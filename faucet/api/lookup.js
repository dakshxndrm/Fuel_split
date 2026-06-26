import { Redis } from "@upstash/redis";
import { ethers } from "ethers";

const url   = process.env.KV_REST_API_URL   || process.env.UPSTASH_REDIS_REST_URL;
const token = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;

export default async function handler(req, res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");
  if (req.method === "OPTIONS") return res.status(200).end();

  if (!url || !token) {
    return res.status(500).json({ error: "Storage is not connected. Add the Redis integration in Vercel." });
  }

  let code, address;
  if (req.method === "GET") {
    code = req.query?.code;
    address = req.query?.address;
  } else if (req.method === "POST") {
    let body = req.body;
    if (typeof body === "string") { try { body = JSON.parse(body); } catch { body = {}; } }
    code = body?.code;
    address = body?.address;
  } else {
    return res.status(405).json({ error: "Use GET or POST." });
  }

  const redis = new Redis({ url, token });

  try {
    // Look up by friend code -> who is this?
    if (code) {
      const rec = await redis.get("code:" + String(code).toUpperCase());
      if (!rec) return res.status(404).json({ error: "No user with that code." });
      return res.status(200).json({
        code: String(code).toUpperCase(),
        address: rec.address,
        displayName: rec.displayName,
      });
    }

    // Look up by wallet address -> what's their code/name?
    if (address) {
      if (!ethers.isAddress(address)) return res.status(400).json({ error: "Invalid address." });
      const rec = await redis.get("addr:" + address.toLowerCase());
      if (!rec) return res.status(404).json({ error: "No profile for that address." });
      return res.status(200).json({ code: rec.code, address, displayName: rec.displayName });
    }

    return res.status(400).json({ error: "Provide 'code' or 'address'." });
  } catch (err) {
    return res.status(500).json({ error: String(err?.message || err) });
  }
}
