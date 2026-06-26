import { network } from "hardhat";

async function main() {
  const { ethers } = await network.connect();
  const [deployer] = await ethers.getSigners();

  const balance = await ethers.provider.getBalance(deployer.address);
  console.log("Deployer:", deployer.address);
  console.log("Balance:", ethers.formatEther(balance), "POL");

  const groupFactory = await ethers.deployContract("GroupFactory");
  await groupFactory.waitForDeployment();
  console.log("GroupFactory:", await groupFactory.getAddress());
}

main().catch((e) => { console.error(e); process.exitCode = 1; });