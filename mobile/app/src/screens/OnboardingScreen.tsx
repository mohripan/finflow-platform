import { StatusBar } from "expo-status-bar";
import { ActivityIndicator, Pressable, SafeAreaView, ScrollView, Text, View } from "react-native";
import { Field } from "../components/Field";
import { styles } from "../styles";
import type { ApiProfile, KycForm, KycState } from "../types";

type OnboardingScreenProps = {
  profile: ApiProfile | null;
  kyc: KycState | null;
  form: KycForm;
  loading: boolean;
  message: string;
  onChangeForm: (form: KycForm) => void;
  onSubmit: () => void;
};

export function OnboardingScreen({ profile, kyc, form, loading, message, onChangeForm, onSubmit }: OnboardingScreenProps) {
  return (
    <SafeAreaView style={styles.screen}>
      <StatusBar style="dark" />
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.brand}>FinFlow</Text>
        <View style={styles.panel}>
          <Text style={styles.sectionTitle}>{profile?.displayName || profile?.email || "Profile"}</Text>
          <Text style={styles.muted}>Customer: {profile?.customerAccount.status ?? "loading"}</Text>
          <Text style={styles.muted}>KYC: {kyc?.status ?? "loading"}</Text>
          {kyc?.status === "APPROVED" ? (
            <Text style={styles.success}>KYC approved. Wallet activation will continue when the wallet service slice is available.</Text>
          ) : null}
        </View>
        <View style={styles.panel}>
          <Text style={styles.sectionTitle}>KYC onboarding</Text>
          <Field label="Legal name" value={form.legalName} onChangeText={(legalName) => onChangeForm({ ...form, legalName })} />
          <Field label="Date of birth YYYY-MM-DD" value={form.dateOfBirth} onChangeText={(dateOfBirth) => onChangeForm({ ...form, dateOfBirth })} />
          <Field label="National identity number" value={form.nationalIdentityNumber} onChangeText={(nationalIdentityNumber) => onChangeForm({ ...form, nationalIdentityNumber })} />
          <Field label="Phone number" value={form.phoneNumber} onChangeText={(phoneNumber) => onChangeForm({ ...form, phoneNumber })} />
          <Field label="Address" value={form.address} onChangeText={(address) => onChangeForm({ ...form, address })} multiline />
          <Pressable accessibilityRole="button" onPress={onSubmit} disabled={loading} style={styles.primary}>
            <Text style={styles.primaryText}>{loading ? "Submitting..." : "Submit KYC"}</Text>
          </Pressable>
          {loading ? <ActivityIndicator /> : null}
          {message ? <Text style={styles.message}>{message}</Text> : null}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
