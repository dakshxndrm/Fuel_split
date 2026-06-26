import type { HardhatUserConfig } from "hardhat/config";
import hardhatEthers from "@nomicfoundation/hardhat-ethers";

const PRIVATE_KEY = "82917ef4b24f65be01eb03397d450890b6043efd1cf7d0ef88ea4b82f3b22db9";

const config: HardhatUserConfig = {
  plugins: [hardhatEthers],
  solidity: "0.8.24",
  networks: {
    localhost: {
      type: "http",
      url: "http://127.0.0.1:8545",
      chainId: 31337
    },
    amoy: {
      type: "http",
      url: "https://rpc-amoy.polygon.technology/",
      chainId: 80002,
      accounts: [PRIVATE_KEY],
      gasPrice: 30000000000
    }
  }
};

export default config;