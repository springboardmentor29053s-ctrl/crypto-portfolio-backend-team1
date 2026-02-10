import { useState } from "react";
import axios from "axios";
import "./Login.css";

function Signup({ onBackToLogin }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleSignup = async (e) => {
    e.preventDefault();
    try {
      const res = await axios.post(
        "http://localhost:8080/api/register",
        { email, password }
      );
      alert(res.data);
      onBackToLogin();
    } catch (err) {
      alert(err.response?.data || "Signup failed");
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h2>Create Account</h2>
        <p className="subtitle">Sign up to continue</p>

        <form onSubmit={handleSignup}>
          <input
            type="email"
            placeholder="Email"
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <input
            type="password"
            placeholder="Password"
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          <button type="submit">Sign Up</button>
        </form>

        <p style={{ marginTop: "15px" }}>
          Already have an account?{" "}
          <span
            style={{ color: "#2c5364", cursor: "pointer" }}
            onClick={onBackToLogin}
          >
            Login
          </span>
        </p>
      </div>
    </div>
  );
}

export default Signup;
