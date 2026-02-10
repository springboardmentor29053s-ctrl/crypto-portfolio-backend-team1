import { useState } from "react";
import axios from "axios";
import "./Login.css";

function Login({ onSignup }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");

  const handleLogin = async (e) => {
    e.preventDefault();
    setMessage("");

    try {
      const res = await axios.post(
        "http://localhost:8080/api/login",
        {
          email,
          password
        }
      );

      // ✅ STORE JWT
      localStorage.setItem("token", res.data.token);

      setMessage("Login successful");

      // OPTIONAL: redirect after login
      // window.location.href = "/dashboard";

    } catch (err) {
      setMessage(err.response?.data || "Invalid credentials");
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h2>Crypto Portfolio</h2>
        <p className="subtitle">Login to your account</p>

        <form onSubmit={handleLogin}>
          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />

          <button type="submit">Login</button>
        </form>

        {message && <p className="message">{message}</p>}

        <p className="switch-text">
          Don’t have an account?{" "}
          <span className="switch-link" onClick={onSignup}>
            Sign Up
          </span>
        </p>
      </div>
    </div>
  );
}

export default Login;
