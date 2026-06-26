# FuelSplit

FuelSplit is a decentralized expense-splitting app for groups — think Splitwise on-chain. Users create groups, log shared expenses, and settle debts via ERC-20 transfers recorded in a Solidity smart contract. A friend-code system lets users discover each other without exposing wallet addresses.

## Repository layout

| Folder | What it is |
|---|---|
| `app/` | Android app (Kotlin + Jetpack Compose). Connects to the contracts via Web3j and handles the full user experience: groups, trips, expense ledger, balances, and settlement flows. |
| `contracts/` | Solidity smart contracts (Hardhat + TypeScript). Contains the core `ExpenseLedger` contract, deployment scripts, and Ignition modules. Targets a local/testnet EVM chain. |
| `faucet/` | Serverless backend (Node.js, Vercel). Two API routes: a token faucet that drips test ERC-20 tokens to new wallets, and a friend-code registration/lookup service. |

## Quick start

Each subfolder has its own `package.json` or Gradle build. See the README or source in each folder for setup instructions.

```
app/        →  Android Studio / Gradle
contracts/  →  npm install && npx hardhat compile
faucet/     →  npm install && vercel dev
```
