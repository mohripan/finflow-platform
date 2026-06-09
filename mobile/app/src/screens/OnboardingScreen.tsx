import { StatusBar } from "expo-status-bar";
import { CameraView, useCameraPermissions } from "expo-camera";
import * as Crypto from "expo-crypto";
import * as FileSystem from "expo-file-system";
import React from "react";
import { ActivityIndicator, Platform, Pressable, SafeAreaView, ScrollView, Text, View } from "react-native";
import { Field } from "../components/Field";
import { styles } from "../styles";
import type { ApiProfile, CapturedEvidence, KycDocumentType, KycForm, KycState } from "../types";

type OnboardingScreenProps = {
  profile: ApiProfile | null;
  kyc: KycState | null;
  form: KycForm;
  loading: boolean;
  message: string;
  onChangeForm: (form: KycForm) => void;
  onSubmit: (evidence: Record<KycDocumentType, CapturedEvidence | null>) => void;
};

export function OnboardingScreen({ profile, kyc, form, loading, message, onChangeForm, onSubmit }: OnboardingScreenProps) {
  const [step, setStep] = React.useState(0);
  const [captureTarget, setCaptureTarget] = React.useState<KycDocumentType | null>(null);
  const [cameraPermission, requestCameraPermission] = useCameraPermissions();
  const cameraRef = React.useRef<CameraView>(null);
  const [evidence, setEvidence] = React.useState<Record<KycDocumentType, CapturedEvidence | null>>({
    IDENTITY_DOCUMENT: null,
    SELFIE: null
  });
  const steps = ["Status", "Details", "Evidence", "Review"];
  const canGoBack = step > 0;
  const canGoNext = step < steps.length - 1;

  async function startCapture(documentType: KycDocumentType) {
    if (Platform.OS === "web") {
      const blob = new Blob([`${documentType}:playwright-evidence`], { type: "image/jpeg" });
      const checksum = await Crypto.digestStringAsync(Crypto.CryptoDigestAlgorithm.SHA256, await blob.text());
      setEvidence((current) => ({
        ...current,
        [documentType]: {
          documentType,
          uri: URL.createObjectURL(blob),
          contentType: "image/jpeg",
          sizeBytes: blob.size,
          checksum
        }
      }));
      return;
    }
    if (!cameraPermission?.granted) {
      const nextPermission = await requestCameraPermission();
      if (!nextPermission.granted) return;
    }
    setCaptureTarget(documentType);
  }

  async function capturePhoto() {
    if (!captureTarget) return;
    const photo = await cameraRef.current?.takePictureAsync({ quality: 0.82, base64: false });
    if (!photo?.uri) return;
    const info = await FileSystem.getInfoAsync(photo.uri, { size: true });
    const base64 = await FileSystem.readAsStringAsync(photo.uri, { encoding: FileSystem.EncodingType.Base64 });
    const checksum = await Crypto.digestStringAsync(Crypto.CryptoDigestAlgorithm.SHA256, base64);
    setEvidence((current) => ({
      ...current,
      [captureTarget]: {
        documentType: captureTarget,
        uri: photo.uri,
        contentType: "image/jpeg",
        sizeBytes: info.exists ? info.size : 0,
        checksum
      }
    }));
    setCaptureTarget(null);
  }

  if (captureTarget) {
    return (
      <SafeAreaView style={styles.screen}>
        <CameraView ref={cameraRef} style={styles.camera} facing={captureTarget === "SELFIE" ? "front" : "back"} />
        <View style={styles.cameraActions}>
          <Pressable accessibilityRole="button" onPress={() => setCaptureTarget(null)} style={styles.secondary}>
            <Text style={styles.secondaryText}>Cancel</Text>
          </Pressable>
          <Pressable accessibilityRole="button" onPress={capturePhoto} style={styles.primary}>
            <Text style={styles.primaryText}>Capture photo</Text>
          </Pressable>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.screen}>
      <StatusBar style="dark" />
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.brand}>FinFlow</Text>
        <View style={styles.stepPanel}>
          <Text style={styles.muted}>Step {step + 1} of {steps.length}</Text>
          <Text style={styles.sectionTitle}>{steps[step]}</Text>
        </View>
        <View style={styles.panel}>
          <Text style={styles.sectionTitle}>{profile?.displayName || profile?.email || "Profile"}</Text>
          <Text style={styles.muted}>Customer: {profile?.customerAccount.status ?? "loading"}</Text>
          <Text style={styles.muted}>KYC: {kyc?.status ?? "loading"}</Text>
          {kyc?.status === "APPROVED" ? (
            <Text style={styles.success}>KYC approved. Wallet activation will continue when the wallet service slice is available.</Text>
          ) : null}
        </View>
        {step === 0 ? (
          <View style={styles.panel}>
            <Text style={styles.sectionTitle}>Before you start</Text>
            <Text style={styles.copy}>Prepare your legal name, date of birth, identity number, phone number, and current address.</Text>
            <Text style={styles.muted}>Admin approval is required before wallet access becomes available.</Text>
          </View>
        ) : null}
        {step === 1 ? (
          <View style={styles.panel}>
            <Text style={styles.sectionTitle}>Personal details</Text>
            <Field label="Legal name" value={form.legalName} onChangeText={(legalName) => onChangeForm({ ...form, legalName })} />
            <Field label="Date of birth YYYY-MM-DD" value={form.dateOfBirth} onChangeText={(dateOfBirth) => onChangeForm({ ...form, dateOfBirth })} />
            <Field label="National identity number" value={form.nationalIdentityNumber} onChangeText={(nationalIdentityNumber) => onChangeForm({ ...form, nationalIdentityNumber })} />
            <Field label="Phone number" value={form.phoneNumber} onChangeText={(phoneNumber) => onChangeForm({ ...form, phoneNumber })} />
            <Field label="Address" value={form.address} onChangeText={(address) => onChangeForm({ ...form, address })} multiline />
          </View>
        ) : null}
        {step === 2 ? (
          <View style={styles.panel}>
            <Text style={styles.sectionTitle}>Identity evidence</Text>
            <EvidenceRow
              label="KTP or SIM"
              value={evidence.IDENTITY_DOCUMENT}
              onCapture={() => startCapture("IDENTITY_DOCUMENT")}
            />
            <EvidenceRow
              label="Selfie"
              value={evidence.SELFIE}
              onCapture={() => startCapture("SELFIE")}
            />
            <Text style={styles.muted}>Images upload only after final submission, using short-lived object storage URLs from the KYC service.</Text>
          </View>
        ) : null}
        {step === 3 ? (
          <View style={styles.panel}>
            <Text style={styles.sectionTitle}>Review submission</Text>
            <Text style={styles.muted}>Legal name: {form.legalName || "-"}</Text>
            <Text style={styles.muted}>Date of birth: {form.dateOfBirth || "-"}</Text>
            <Text style={styles.muted}>Phone number: {form.phoneNumber || "-"}</Text>
            <Text style={styles.muted}>Address: {form.address || "-"}</Text>
          <Text style={styles.muted}>Identity evidence: {evidence.IDENTITY_DOCUMENT ? "captured" : "missing"}</Text>
          <Text style={styles.muted}>Selfie: {evidence.SELFIE ? "captured" : "missing"}</Text>
          <Pressable accessibilityRole="button" onPress={() => onSubmit(evidence)} disabled={loading} style={styles.primary}>
            <Text style={styles.primaryText}>{loading ? "Submitting..." : "Submit KYC"}</Text>
          </Pressable>
          {loading ? <ActivityIndicator /> : null}
          {message ? <Text style={styles.message}>{message}</Text> : null}
          </View>
        ) : null}
        <View style={styles.stepActions}>
          <Pressable accessibilityRole="button" disabled={!canGoBack} onPress={() => setStep(step - 1)} style={[styles.secondary, styles.stepButton]}>
            <Text style={styles.secondaryText}>Back</Text>
          </Pressable>
          {canGoNext ? (
            <Pressable accessibilityRole="button" onPress={() => setStep(step + 1)} style={[styles.primary, styles.stepButton]}>
              <Text style={styles.primaryText}>Continue</Text>
            </Pressable>
          ) : null}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

function EvidenceRow({ label, value, onCapture }: { label: string; value: CapturedEvidence | null; onCapture: () => void }) {
  return (
    <View style={styles.evidenceRow}>
      <View style={styles.evidenceText}>
        <Text style={styles.label}>{label}</Text>
        <Text style={styles.muted}>{value ? `${Math.ceil(value.sizeBytes / 1024)} KB ready` : "Not captured"}</Text>
      </View>
      <Pressable accessibilityRole="button" onPress={onCapture} style={value ? styles.secondary : styles.primary}>
        <Text style={value ? styles.secondaryText : styles.primaryText}>{value ? "Retake" : "Capture"}</Text>
      </Pressable>
    </View>
  );
}
