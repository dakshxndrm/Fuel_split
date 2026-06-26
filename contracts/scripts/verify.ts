import { network } from "hardhat";

async function main() {
  const { ethers } = await network.connect();

  const REGISTRY = "0x5FbDB2315678afecb367f032d93F642f64180aa3";
  const APP_WALLET = "0x46be0b54db02a2603972d1918909cc71d8da3ca8";

  const registry = await ethers.getContractAt("UserRegistry", REGISTRY);

  const isReg = await registry.isRegistered(APP_WALLET);
  console.log("Is app wallet registered on-chain?:", isReg);

  if (isReg) {
    const user = await registry.getUser(APP_WALLET);
    console.log("Username:", user.username);
    console.log("Referral code:", user.referralCode);
    console.log("Wallet:", user.walletAddress);
  }
}

main().catch((e) => { console.error(e); process.exitCode = 1; });