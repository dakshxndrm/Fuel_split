package com.example.fuel_split;

public final class Config {
    static final boolean USE_LOCAL = true;

    static final String RPC_URL = USE_LOCAL
            ? "http://127.0.0.1:8545"
            : "https://rpc-amoy.polygon.technology/";

    static final String FAUCET_URL = USE_LOCAL
            ? "http://127.0.0.1:3000"
            : "https://fuelsplit-faucet.vercel.app/api/fund";

    static final String USER_REGISTRY = USE_LOCAL
            ? "0x4ed7c70F96B99c776995fB64377f0d4aB3B0e1C1"
            : "0xD81528FFA49c8BA0d725B4bFd3F27C3b63f983Ea";

    static final String GROUP_FACTORY = USE_LOCAL
            ? "0xa85233C63b9Ee964Add6F2cffe00Fd84eb32338f"
            : "0x97CC2151b535fC1E13D51903D3E4c18D93eF825f";

    static final long   CHAIN_ID     = USE_LOCAL ? 31337L : 80002L;

    static final String PROFILE_BASE = "https://fuelsplit-faucet.vercel.app";

    private Config() {}
}
