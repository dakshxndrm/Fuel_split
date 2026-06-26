import { Redis } from "@upstash/redis";
import { ethers } from "ethers";
import { randomInt } from "node:crypto";

// The Vercel Upstash integration injects KV_REST_API_URL / KV_REST_API_TOKEN.
// (Some setups use UPSTASH_REDIS_REST_* — we accept either.)
const url   = process.env.KV_REST_API_URL   || process.env.UPSTASH_REDIS_REST_URL;
const token = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;

// Friend-code charset: no 0/O/1/I/L so codes are easy to read and type.
const CHARSET  = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
const CODE_LEN = 6;

function randomCode() {
  let c = "";
  for (let i = 0; i < CODE_LEN; i++) c += CHARSET[randomInt(CHARSET.length)];
  return c;
}

export default async function handler(req, res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");
  if (req.method === "OPTIONS") return res.status(200).end();
  if (req.method !== "POST") return res.status(405).json({ error: "Use POST." });

  if (!url || !token) {
    return res.status(500).json({ error: "Storage is not connected. Add the Redis integration in Vercel." });
  }

  let body = req.body;
  if (typeof body === "string") { try { body = JSON.parse(body); } catch { body = {}; } }
  const address = body?.address;
  const displayName = (body?.displayName || "").toString().trim();

  if (!address || !ethers.isAddress(address)) {
    return res.status(400).json({ error: "Provide a valid 'address'." });
  }
  if (!displayName) {
    return res.status(400).json({ error: "Provide a 'displayName'." });
  }

  const redis = new Redis({ url, token });
  const addrKey = "addr:" + address.toLowerCase();

  try {
    // Idempotent: if this wallet already has a code, return it.
    const existing = await redis.get(addrKey);
    if (existing) {
      return res.status(200).json({ ...existing, address, created: false });
    }

    // Generate a code that nobody else has yet.
    let code = null;
    for (let i = 0; i < 10; i++) {
      const candidate = randomCode();
      const ok = await redis.set("code:" + candidate, { address, displayName }, { nx: true });
      if (ok) { code = candidate; break; }
    }
    if (!code) {
      return res.status(500).json({ error: "Could not generate a unique code. Try again." });
    }

    await redis.set(addrKey, { code, displayName });
    return res.status(200).json({ code, displayName, address, created: true });
  } catch (err) {
    return res.status(500).json({ error: String(err?.message || err) });
  }
}
