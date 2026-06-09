import { StatusBar } from "expo-status-bar";
import * as Linking from "expo-linking";
import { Animated, Easing, Image, Pressable, SafeAreaView, Text, View } from "react-native";
import { useEffect, useRef } from "react";
import { styles } from "../styles";

type LoginScreenProps = {
  registrationUrl: string;
  canSignIn: boolean;
  onSignIn: () => void;
};

export function LoginScreen({ registrationUrl, canSignIn, onSignIn }: LoginScreenProps) {
  const intro = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.timing(intro, {
      toValue: 1,
      duration: 900,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: true
    }).start();
  }, [intro]);

  const heroStyle = {
    opacity: intro,
    transform: [
      {
        translateY: intro.interpolate({
          inputRange: [0, 1],
          outputRange: [24, 0]
        })
      },
      {
        scale: intro.interpolate({
          inputRange: [0, 1],
          outputRange: [0.96, 1]
        })
      }
    ]
  };

  return (
    <SafeAreaView style={styles.screen}>
      <StatusBar style="dark" />
      <Animated.View style={[styles.hero, heroStyle]}>
        <Image source={require("../assets/images/icon.png")} style={styles.icon} />
        <Text style={styles.brand}>FinFlow</Text>
        <Text style={styles.title}>Wallet onboarding</Text>
        <Text style={styles.copy}>Verify your identity, wait for review, and unlock wallet access after approval.</Text>
      </Animated.View>
      <View style={styles.actions}>
        <Pressable accessibilityRole="button" disabled={!canSignIn} onPress={onSignIn} style={styles.primary}>
          <Text style={styles.primaryText}>Log in</Text>
        </Pressable>
        <Pressable accessibilityRole="button" onPress={() => Linking.openURL(registrationUrl)} style={styles.secondary}>
          <Text style={styles.secondaryText}>Sign up</Text>
        </Pressable>
      </View>
    </SafeAreaView>
  );
}
