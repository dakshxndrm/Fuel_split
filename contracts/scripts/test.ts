import { network } from "hardhat";

async function main() {
  const { ethers } = await network.connect();
  const [daksh, rahul] = await ethers.getSigners();

  console.log("=== FuelSplit Full Flow Test ===\n");
  console.log("Daksh:", daksh.address);
  console.log("Rahul:", rahul.address, "\n");

  // 1. Deploy
  const userRegistry = await ethers.deployContract("UserRegistry");
  await userRegistry.waitForDeployment();
  const groupFactory = await ethers.deployContract("GroupFactory");
  await groupFactory.waitForDeployment();
  console.log("✓ Contracts deployed\n");

  // 2. Register both users
  await userRegistry.connect(daksh).register("Daksh", "");
  await userRegistry.connect(rahul).register("Rahul", "");
  const dakshUser = await userRegistry.getUser(daksh.address);
  console.log("✓ Daksh registered. Referral code:", dakshUser.referralCode);
  console.log("✓ Rahul registered\n");

  // 3. Daksh creates a group with Rahul
  const tx = await groupFactory.connect(daksh).createGroup("Goa Trip", [rahul.address]);
  const receipt = await tx.wait();
  const userGroups = await groupFactory.getUserGroups(daksh.address);
  const groupAddr = userGroups[0];
  console.log("✓ Group 'Goa Trip' created at:", groupAddr, "\n");

  // 4. Attach to the group contract
  const group = await ethers.getContractAt("ExpenseLedger", groupAddr);
  const members = await group.getMembers();
  console.log("✓ Group members count:", members.length);

  // 5. Daksh adds expense: 240 rupees = 24000 paise, split 50/50
  await group.connect(daksh).addExpense(
    "Petrol",
    24000,
    [daksh.address, rahul.address],
    [50, 50]
  );
  console.log("✓ Expense added: Petrol ₹240, split 50/50\n");

  // 6. Check balance — Rahul should owe Daksh 12000 paise (₹120)
  const rahulOwes = await group.getBalance(rahul.address, daksh.address);
  console.log("Rahul owes Daksh:", Number(rahulOwes) / 100, "rupees");

  // 7. Rahul marks settled (paid ₹120 via UPI outside app)
  await group.connect(rahul).markSettled(daksh.address, 12000);
  console.log("✓ Rahul marked ₹120 as settled\n");

  // 8. Daksh confirms received
  await group.connect(daksh).confirmSettlement(0);
  console.log("✓ Daksh confirmed receipt");

  // 9. Final balance should be 0
  const finalBalance = await group.getBalance(rahul.address, daksh.address);
  console.log("\nFinal balance (Rahul → Daksh):", Number(finalBalance) / 100, "rupees");

  if (Number(finalBalance) === 0) {
    console.log("\n🎉 ALL TESTS PASSED — full flow works end to end!");
  } else {
    console.log("\n❌ Balance not zero — bug somewhere.");
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});