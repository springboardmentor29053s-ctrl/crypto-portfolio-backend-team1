import { useState } from "react";
import Landing from "./pages/landing/Landing";
import Login from "./pages/login/Login";
import Signup from "./pages/login/Signup";

function App() {
    const [page, setPage] = useState("landing");
    const [signupEmail, setSignupEmail] = useState("");

    return (
        <>
            {page === "landing" && (
                <Landing
                    onLogin={() => setPage("login")}
                    onSignup={(email) => {
                        setSignupEmail(email);
                        setPage("signup");
                    }}
                />
            )}

            {page === "signup" && (
                <Signup
                    prefilledEmail={signupEmail}
                    onBackToLogin={() => setPage("login")}
                />
            )}

            {page === "login" && (
                <Login onSignup={() => setPage("signup")} />
            )}
        </>
    );
}

export default App;

