import { StyleSheet } from "react-native";

export const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: "#f7f8f5" },
  content: { padding: 20, gap: 16 },
  hero: { padding: 28, gap: 10 },
  brand: { fontSize: 20, fontWeight: "800", color: "#0f3d36" },
  title: { fontSize: 34, fontWeight: "800", color: "#17201d" },
  copy: { fontSize: 16, lineHeight: 24, color: "#52605a" },
  actions: { padding: 24, gap: 12 },
  panel: { backgroundColor: "#ffffff", borderRadius: 8, padding: 16, gap: 12, borderWidth: 1, borderColor: "#dce2de" },
  sectionTitle: { fontSize: 18, fontWeight: "700", color: "#17201d" },
  muted: { color: "#52605a" },
  field: { gap: 6 },
  label: { color: "#2f3d38", fontWeight: "600" },
  input: { borderWidth: 1, borderColor: "#c9d2cd", borderRadius: 6, paddingHorizontal: 12, paddingVertical: 10, backgroundColor: "#fff" },
  multi: { minHeight: 76, textAlignVertical: "top" },
  primary: { minHeight: 46, borderRadius: 6, backgroundColor: "#0f6b57", alignItems: "center", justifyContent: "center", paddingHorizontal: 16 },
  primaryText: { color: "#fff", fontWeight: "800" },
  secondary: { minHeight: 46, borderRadius: 6, borderWidth: 1, borderColor: "#0f6b57", alignItems: "center", justifyContent: "center", paddingHorizontal: 16 },
  secondaryText: { color: "#0f6b57", fontWeight: "800" },
  message: { color: "#0f3d36", fontWeight: "600" },
  success: { color: "#0f6b57", fontWeight: "700", lineHeight: 20 }
});
