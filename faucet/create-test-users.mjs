import { ethers } from "ethers";

// ── Settings ────────────────────────────────────────────────────────────────
const RPC_URL       = "https://rpc-amoy.polygon.technology/";
const FAUCET_URL    = "https://fuelsplit-faucet.vercel.app/api/fund";
const USER_REGISTRY = "0xD81528FFA49c8BA0d725B4bFd3F27C3b63f983Ea";

// The test users to create. Edit this list. Use NEW names each time you re-run,
// because a username can only be registered once.
const USERS = ["alice", "bob"];

// register(string username, string referralCode) — referral left blank ("")
const ABI = ["function register(string username, string referralCode) external"];

const provider = new ethers.JsonRpcProvider(RPC_URL);

async function fund(address) {
  const res = await fetch(FAUCET_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ address }),
  });
  return res.json();
}

async function main() {
  const created = [];

  for (const username of USERS) {
    console.log(`\n=== ${username} ===`);
    const wallet = ethers.Wallet.createRandom().connect(provider);
    console.log("wallet:", wallet.address);

    console.log("funding from faucet...");
    const f = await fund(wallet.address);
    console.log("faucet says:", JSON.stringify(f));
    if (f.error) {
      console.log("!! funding failed, skipping this user.");
      continue;
    }

    // give the funding tx a few seconds to settle
    await new Promise((r) => setTimeout(r, 4000));

    console.log("registering on-chain...");
    try {
      const registry = new ethers.Contract(USER_REGISTRY, ABI, wallet);
      const tx = await registry.register(username, "");
      await tx.wait();
      console.log("registered! tx:", tx.hash);
      created.push({ username, address: wallet.address });
    } catch (e) {
      console.log("!! register failed:", e.shortMessage || e.message);
    }
  }

  console.log("\n\n===== DONE — test users created =====");
  if (created.length === 0) {
    console.log("(none — check the messages above)");
  } else {
    for (const u of created) console.log(`${u.username}   ->   ${u.address}`);
    console.log("\nIn the app, add them to a group by their username.");
  }
}

main().catch(console.error);
