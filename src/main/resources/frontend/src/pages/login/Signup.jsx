import { useState, useEffect } from "react";
import axios from "axios";
import "./Login.css";

function Signup({ onBackToLogin, prefilledEmail }) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState(prefilledEmail || "");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (prefilledEmail) {
      setEmail(prefilledEmail);
    }
  }, [prefilledEmail]);

  const handleSignup = async (e) => {
    e.preventDefault();
    setMessage("");

    try {
      const res = await axios.post(
        "http://localhost:8080/api/signup",
        { name, email, password }
      );

      setMessage(res.data);

      setTimeout(() => {
        onBackToLogin();
      }, 800);

    } catch (err) {
      setMessage(err.response?.data || "Signup failed");
    }
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <h2>Crypto Portfolio</h2>
        <p className="subtitle">Create your account</p>

        <form onSubmit={handleSignup}>
          <input
            type="text"
            placeholder="Full Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

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

          <button type="submit">Sign Up</button>
        </form>

        {message && <p className="message">{message}</p>}

        <p className="switch-text">
          Already have an account?{" "}
          <span className="switch-link" onClick={onBackToLogin}>
            Login
          </span>
        </p>
      </div>
    </div>
  );
}

export default Signup;
