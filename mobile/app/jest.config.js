module.exports = {
  preset: "jest-expo",
  passWithNoTests: true,
  testMatch: ["**/?(*.)+(test).[jt]s?(x)"],
  transformIgnorePatterns: [
    "node_modules/(?!((jest-)?react-native|@react-native|expo(nent)?|@expo(nent)?/.*|@expo-google-fonts/.*|react-navigation|@react-navigation/.*|@sentry/react-native)/)"
  ]
};
