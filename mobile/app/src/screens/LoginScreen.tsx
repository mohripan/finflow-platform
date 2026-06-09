import { StatusBar } from "expo-status-bar";
import * as Linking from "expo-linking";
import { Pressable, SafeAreaView, Text, View } from "react-native";
import { styles } from "../styles";

type LoginScreenProps = {
  registrationUrl: string;
  canSignIn: boolean;
  onSignIn: () => void;
};

export function LoginScreen({ registrationUrl, canSignIn, onSignIn }: LoginScreenProps) {
  return (
    <SafeAreaView style={styles.screen}>
      <StatusBar style="dark" />
      <View style={styles.hero}>
        <Text style={styles.brand}>FinFlow</Text>
        <Text style={styles.title}>Wallet onboarding</Text>
        <Text style={styles.copy}>Sign in or create a Keycloak account before submitting customer KYC.</Text>
      </View>
      <View style={styles.actions}>
        <Pressable accessibilityRole="button" disabled={!canSignIn} onPress={onSignIn} style={styles.primary}>
          <Text style={styles.primaryText}>Sign in</Text>
        </Pressable>
        <Pressable accessibilityRole="button" onPress={() => Linking.openURL(registrationUrl)} style={styles.secondary}>
          <Text style={styles.secondaryText}>Create account</Text>
        </Pressable>
      </View>
    </SafeAreaView>
  );
}
