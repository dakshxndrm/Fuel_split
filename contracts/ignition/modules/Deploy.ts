import { buildModule } from "@nomicfoundation/hardhat-ignition/modules";

const FuelSplitModule = buildModule("FuelSplitModule", (m) => {
  const userRegistry = m.contract("UserRegistry");
  const referralRegistry = m.contract("ReferralRegistry");
  const groupFactory = m.contract("GroupFactory");

  return { userRegistry, referralRegistry, groupFactory };
});

export default FuelSplitModule;