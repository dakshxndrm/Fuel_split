import { createServer } from "node:http";
import { ethers } from "ethers";

// ── Local-only faucet. Funds new app wallets from a rich Hardhat account. ─────
const RPC         = "http://127.0.0.1:8545";
const PORT        = 3000;
const FUND_AMOUNT = "10"; // 10 POL per wallet — gas is free locally, so be generous

// Hardhat's default account #0. This private key is PUBLICLY KNOWN and is safe to
// use ONLY on a local test chain. Never use it on Amoy or any real network.
const FUNDER_KEY = "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";

const provider = new ethers.JsonRpcProvider(RPC);
const funder   = new ethers.Wallet(FUNDER_KEY, provider);

const server = createServer((req, res) => {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") { res.writeHead(200); return res.end(); }
  if (req.method === "GET") {
    res.writeHead(200, { "Content-Type": "application/json" });
    return res.end(JSON.stringify({ ok: true, message: "Local faucet running." }));
  }
  if (req.method !== "POST") {
    res.writeHead(405, { "Content-Type": "application/json" });
    return res.end(JSON.stringify({ error: "Use POST." }));
  }

  let data = "";
  req.on("data", (c) => (data += c));
  req.on("end", async () => {
    try {
      const { address } = JSON.parse(data || "{}");
      if (!address || !ethers.isAddress(address)) {
        res.writeHead(400, { "Content-Type": "application/json" });
        return res.end(JSON.stringify({ error: "Provide a valid 'address'." }));
      }
      const tx = await funder.sendTransaction({
        to: address,
        value: ethers.parseEther(FUND_AMOUNT),
      });
      await tx.wait();
      res.writeHead(200, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ funded: true, txHash: tx.hash, amount: FUND_AMOUNT }));
    } catch (err) {
      res.writeHead(500, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: String(err?.message || err) }));
    }
  });
});

server.listen(PORT, "127.0.0.1", () =>
  console.log(`Local faucet running on http://127.0.0.1:${PORT}  (POST an {"address"} to fund it)`)
);
