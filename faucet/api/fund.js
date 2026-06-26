import { ethers } from "ethers";
import { Redis } from "@upstash/redis";

// ── Config (secrets come from Vercel Environment Variables) ──────────────────
const RPC_URL = process.env.AMOY_RPC_URL || "https://rpc-amoy.polygon.technology/";
const TREASURY_PRIVATE_KEY = process.env.TREASURY_PRIVATE_KEY;

const url   = process.env.KV_REST_API_URL   || process.env.UPSTASH_REDIS_REST_URL;
const token = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;

const FUND_AMOUNT   = "0.05"; // POL per new wallet — enough to create a group on Amoy
const SKIP_IF_ABOVE = "0.02"; // don't re-fund a wallet that already has gas
const DAILY_CAP     = 1.0;    // max total POL this faucet will hand out per day

export default async function handler(req, res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") return res.status(200).end();
  if (req.method === "GET") {
    return res.status(200).json({ ok: true, message: "FuelSplit faucet is running." });
  }
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Use POST." });
  }

  if (!TREASURY_PRIVATE_KEY) {
    console.error("Faucet misconfigured: TREASURY_PRIVATE_KEY missing");
    return res.status(500).json({ error: "Faucet temporarily unavailable." });
  }

  let body = req.body;
  if (typeof body === "string") { try { body = JSON.parse(body); } catch { body = {}; } }
  const address = body?.address;

  if (!address || !ethers.isAddress(address)) {
    return res.status(400).json({ error: "Provide a valid 'address'." });
  }

  try {
    const provider = new ethers.JsonRpcProvider(RPC_URL);
    const treasury = new ethers.Wallet(TREASURY_PRIVATE_KEY, provider);

    // 1) Don't re-fund a wallet that already has gas.
    const balance = await provider.getBalance(address);
    if (balance > ethers.parseEther(SKIP_IF_ABOVE)) {
      return res.status(200).json({ funded: false, reason: "Wallet already has gas." });
    }

    // 2) Daily spend cap — protects against many-fresh-wallets drain attacks.
    if (url && token) {
      const redis = new Redis({ url, token });
      const day = new Date().toISOString().slice(0, 10);     // YYYY-MM-DD (UTC)
      const key = "faucet:spent:" + day;
      const spent = parseFloat((await redis.get(key)) || "0");
      if (spent + parseFloat(FUND_AMOUNT) > DAILY_CAP) {
        return res.status(429).json({ error: "Faucet daily limit reached. Try again tomorrow." });
      }
      // reserve this fund against today's budget (expires after ~2 days)
      await redis.incrbyfloat(key, parseFloat(FUND_AMOUNT));
      await redis.expire(key, 60 * 60 * 48);
    }

    // 3) Make sure the treasury can cover it.
    const treasuryBalance = await provider.getBalance(treasury.address);
    if (treasuryBalance < ethers.parseEther(FUND_AMOUNT)) {
      return res.status(503).json({ error: "Faucet treasury is empty right now." });
    }

    const tx = await treasury.sendTransaction({
      to: address,
      value: ethers.parseEther(FUND_AMOUNT),
    });
    await tx.wait();

    return res.status(200).json({ funded: true, txHash: tx.hash, amount: FUND_AMOUNT });
  } catch (err) {
    console.error("Faucet error:", err); // full detail stays server-side only
    return res.status(500).json({ error: "Could not fund wallet. Try again shortly." });
  }
}
