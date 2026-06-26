import { network } from "hardhat";

async function main() {
  const { ethers } = await network.connect();
  const signers = await ethers.getSigners();
  
  const reg = await ethers.getContractAt(
    "UserRegistry", 
    "0x5FbDB2315678afecb367f032d93F642f64180aa3"
  );

  await reg.connect(signers[1]).register("Rahul", "");
  const isReg = await reg.isRegistered(signers[1].address);
  console.log("Rahul registered:", isReg);
  console.log("Rahul address:", signers[1].address);
}

main().catch((e) => { console.error(e); process.exitCode = 1; });