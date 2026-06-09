import { AppShell } from "./components/AppShell";
import { useAdminSession } from "./auth/useAdminSession";
import { KycReviewScreen } from "./screens/KycReviewScreen";
import { LoadingPanel } from "./screens/LoadingPanel";
import { LoginPanel } from "./screens/LoginPanel";

export default function App() {
  const session = useAdminSession();

  return (
    <AppShell authState={session.authState} onSignOut={session.signOut}>
      {session.authState === "loading" ? <LoadingPanel /> : null}
      {session.authState === "anonymous" ? (
        <LoginPanel error={session.authError} onSignIn={session.signIn} />
      ) : null}
      {session.authState === "authenticated" ? (
        <KycReviewScreen accessToken={session.accessToken} profileEmail={session.profileEmail} />
      ) : null}
    </AppShell>
  );
}
