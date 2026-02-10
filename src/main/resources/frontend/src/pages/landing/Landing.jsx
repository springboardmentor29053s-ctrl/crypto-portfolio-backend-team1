import { useState } from "react";
import "./Landing.css";

function Landing({ onLogin, onSignup }) {
    const [email, setEmail] = useState("");

    const handleHeroSignup = () => {
        if (!email) {
            alert("Please enter your email");
            return;
        }
        onSignup(email);
    };

    return (
        <div className="landing">
            {/* NAVBAR */}
            <nav className="navbar">
                <div className="logo">
                    <span className="logo-icon">₿</span> Cryptexa
                </div>

                <ul className="nav-links">
                    <li>Explore</li>
                    <li>Price</li>
                    <li>Why Cryptexa?</li>
                    <li>Learn</li>
                    <li>Support</li>
                </ul>

                <div className="nav-actions">
                    <button className="btn-outline" onClick={onLogin}>
                        Login
                    </button>
                    <button className="btn-fill" onClick={() => onSignup("")}>
                        Sign Up
                    </button>
                </div>
            </nav>

            {/* HERO */}
            <div className="hero">
                <div className="hero-left">
                    <h1>
                        Buy, <span>Sell</span> and Trade <br />
                        Crypto For Future
                    </h1>

                    <p>
                        Cryptexa is the easiest, safest, and fastest way to buy,
                        sell and trade crypto assets.
                    </p>

                    <div className="hero-cta">
                        <input
                            type="email"
                            placeholder="Enter Your Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                        />
                        <button onClick={handleHeroSignup}>
                            Sign Up
                        </button>
                    </div>
                </div>

                {/* RIGHT SIDE DESIGN */}
                <div className="hero-right">
                    {/* Floating coins */}
                    <div className="coin coin-btc">₿</div>
                    <div className="coin coin-eth">◇</div>
                    <div className="coin coin-ltc">Ł</div>

                    {/* Market preview box */}
                    <div className="market-box">
                        <div className="market-header">
                            <span>BTC / USDT</span>
                            <span className="positive">+4.32%</span>
                        </div>

                        <div className="chart">
                            <span style={{ height: "40%" }}></span>
                            <span style={{ height: "55%" }}></span>
                            <span style={{ height: "35%" }}></span>
                            <span style={{ height: "65%" }}></span>
                            <span style={{ height: "80%" }}></span>
                            <span style={{ height: "70%" }}></span>
                            <span style={{ height: "90%" }}></span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Landing;
