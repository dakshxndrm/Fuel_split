import { network } from "hardhat";

async function main() {
  const { ethers } = await network.connect();

  console.log("Deploying FuelSplit contracts...");

  const userRegistry = await ethers.deployContract("UserRegistry");
  await userRegistry.waitForDeployment();
  console.log("UserRegistry:", await userRegistry.getAddress());

  const referralRegistry = await ethers.deployContract("ReferralRegistry");
  await referralRegistry.waitForDeployment();
  console.log("ReferralRegistry:", await referralRegistry.getAddress());

  const groupFactory = await ethers.deployContract("GroupFactory");
  await groupFactory.waitForDeployment();
  console.log("GroupFactory:", await groupFactory.getAddress());

  console.log("All contracts deployed!");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});