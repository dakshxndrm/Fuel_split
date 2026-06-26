import { ethers } from "ethers";

// ── Config ────────────────────────────────────────────────────────────────────
const LOCAL_RPC      = "http://127.0.0.1:8545";
const PROFILE_URL    = "https://fuelsplit-faucet.vercel.app/api/profile";
const USER_REGISTRY  = "0xc3e53F4d16Ae77Db1c982e75a937B9f60FE63690";
const FUND_AMOUNT    = "1.0"; // 1 fake POL each — more than enough locally

// Hardhat account #0 — publicly known key, local only
const FUNDER_KEY = "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";

// Test users to create
const USERS = [
  { displayName: "Arjun" },
  { displayName: "Priya" },
  { displayName: "Ravi"  },
  { displayName: "Neha"  },
  { displayName: "Rohit" },
];

// register(string username, string referralCode)
const ABI = ["function register(string username, string referralCode) external"];

const provider = new ethers.JsonRpcProvider(LOCAL_RPC);
const funder   = new ethers.Wallet(FUNDER_KEY, provider);

async function createProfile(address, displayName) {
  const res = await fetch(PROFILE_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ address, displayName }),
  });
  const j = await res.json();
  if (j.error) throw new Error("Profile API: " + j.error);
  return j.code; // 6-char code
}

async function main() {
  // Sanity check: make sure local node is reachable
  try {
    await provider.getBlockNumber();
  } catch {
    console.error("❌  Cannot reach local node at", LOCAL_RPC);
    console.error("    Make sure `npx hardhat node` is running in Window 1.");
    process.exit(1);
  }

  const registry = new ethers.Contract(USER_REGISTRY, ABI, funder);
  const results  = [];

  for (const { displayName } of USERS) {
    console.log(`\n=== ${displayName} ===`);
    const wallet = ethers.Wallet.createRandom().connect(provider);

    // Fund from Hardhat account #0
    const ftx = await funder.sendTransaction({
      to: wallet.address,
      value: ethers.parseEther(FUND_AMOUNT),
    });
    await ftx.wait();
    console.log("funded:", wallet.address);

    // Get friend code from Vercel profile API
    let code;
    try {
      code = await createProfile(wallet.address, displayName);
      console.log("code:", code);
    } catch (e) {
      console.log("profile failed:", e.message, "— using displayName as fallback username");
      code = displayName.toLowerCase();
    }

    // Register on-chain (username = their friend code)
    try {
      const rtx = await registry.connect(wallet).register(code, "");
      await rtx.wait();
      console.log("registered on-chain ✓");
      results.push({ displayName, code, address: wallet.address });
    } catch (e) {
      console.log("register failed:", e.shortMessage || e.message);
    }
  }

  // ── Summary table ──────────────────────────────────────────────────────────
  console.log("\n\n╔══════════════════════════════════════════════════════════════════════╗");
  console.log(  "║              FAKE USERS READY — add them by their CODE               ║");
  console.log(  "╠══════════╦══════════╦══════════════════════════════════════════════╣");
  console.log(  "║ NAME     ║ CODE     ║ WALLET ADDRESS                               ║");
  console.log(  "╠══════════╬══════════╬══════════════════════════════════════════════╣");
  for (const u of results) {
    const name = u.displayName.padEnd(8);
    const code = u.code.padEnd(8);
    console.log(`║ ${name} ║ ${code} ║ ${u.address} ║`);
  }
  console.log(  "╚══════════╩══════════╩══════════════════════════════════════════════╝");
  console.log("\nIn the app → add group member → type the CODE from above.");
}

main().catch(console.error);
