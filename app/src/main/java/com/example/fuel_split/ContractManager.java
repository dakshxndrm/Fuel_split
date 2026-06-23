package com.example.fuel_split;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Numeric;

import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ContractManager {

    public static final String GROUP_FACTORY_ADDRESS = "0x97CC2151b535fC1E13D51903D3E4c18D93eF825f";
    private static final long EXPENSE_GAS_LIMIT = 3000000;
    // CHANGE to the UserRegistry address from setup.ts
    public static final String USER_REGISTRY_ADDRESS = "0xD81528FFA49c8BA0d725B4bFd3F27C3b63f983Ea";
    private static final long CHAIN_ID = 80002; // Hardhat local

    private final Web3j web3;
    private final Credentials credentials;

    public ContractManager(Web3j web3, Credentials credentials) {
        this.web3 = web3;
        this.credentials = credentials;
    }

    public boolean isRegistered(String address) throws Exception {
        Function function = new Function(
                "isRegistered",
                Collections.singletonList(new Address(address)),
                Collections.singletonList(new TypeReference<Bool>() {})
        );
        String encoded = FunctionEncoder.encode(function);
        EthCall response = web3.ethCall(
                Transaction.createEthCallTransaction(address, USER_REGISTRY_ADDRESS, encoded),
                DefaultBlockParameterName.LATEST
        ).send();
        List<Type> result = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        if (result.isEmpty()) return false;
        return (Boolean) result.get(0).getValue();
    }

    public String register(String username, String referralCode) throws Exception {
        Function function = new Function(
                "register",
                Arrays.asList(new Utf8String(username), new Utf8String(referralCode)),
                Collections.emptyList()
        );
        String encoded = FunctionEncoder.encode(function);

        BigInteger nonce = web3.ethGetTransactionCount(
                        credentials.getAddress(), DefaultBlockParameterName.LATEST)
                .send().getTransactionCount();

        BigInteger gasPrice = web3.ethGasPrice().send().getGasPrice();
        BigInteger gasLimit = BigInteger.valueOf(500000);

        RawTransaction rawTx = RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit, USER_REGISTRY_ADDRESS, encoded);

        byte[] signed = TransactionEncoder.signMessage(rawTx, CHAIN_ID, credentials);
        String hexValue = Numeric.toHexString(signed);

        EthSendTransaction tx = web3.ethSendRawTransaction(hexValue).send();
        if (tx.hasError()) {
            throw new Exception(tx.getError().getMessage());
        }
        return tx.getTransactionHash();
    }
    public String getUsernameAddress(String username) throws Exception {
        Function function = new Function(
                "getAddressByUsername",
                Collections.singletonList(new Utf8String(username)),
                Collections.singletonList(new TypeReference<Address>() {})
        );
        String encoded = FunctionEncoder.encode(function);
        EthCall response = web3.ethCall(
                Transaction.createEthCallTransaction(
                        credentials.getAddress(), USER_REGISTRY_ADDRESS, encoded),
                DefaultBlockParameterName.LATEST
        ).send();
        List<Type> result = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        if (result.isEmpty()) return null;
        String addr = result.get(0).getValue().toString();
        if (addr.equals("0x0000000000000000000000000000000000000000")) return null;
        return addr;
    }

    public String createGroup(String groupName, List<String> memberAddresses) throws Exception {
        org.web3j.abi.datatypes.DynamicArray<Address> membersArray =
                new org.web3j.abi.datatypes.DynamicArray<>(
                        Address.class,
                        memberAddresses.stream()
                                .map(Address::new)
                                .collect(java.util.stream.Collectors.toList())
                );

        Function function = new Function(
                "createGroup",
                Arrays.asList(new Utf8String(groupName), membersArray),
                Collections.singletonList(new TypeReference<Address>() {})
        );
        String encoded = FunctionEncoder.encode(function);

        BigInteger nonce = web3.ethGetTransactionCount(
                        credentials.getAddress(), DefaultBlockParameterName.LATEST)
                .send().getTransactionCount();
        BigInteger gasPrice = web3.ethGasPrice().send().getGasPrice();
        BigInteger gasLimit = BigInteger.valueOf(3000000);

        RawTransaction rawTx = RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit, GROUP_FACTORY_ADDRESS, encoded);
        byte[] signed = TransactionEncoder.signMessage(rawTx, CHAIN_ID, credentials);
        String hexValue = Numeric.toHexString(signed);

        EthSendTransaction tx = web3.ethSendRawTransaction(hexValue).send();
        if (tx.hasError()) throw new Exception(tx.getError().getMessage());
        return tx.getTransactionHash();
    }

    public List<String> getUserGroups() throws Exception {
        Function function = new Function(
                "getUserGroups",
                Collections.singletonList(new Address(credentials.getAddress())),
                Collections.singletonList(new TypeReference<DynamicArray<Address>>() {})
        );
        String encoded = FunctionEncoder.encode(function);
        EthCall response = web3.ethCall(
                Transaction.createEthCallTransaction(
                        credentials.getAddress(), GROUP_FACTORY_ADDRESS, encoded),
                DefaultBlockParameterName.LATEST
        ).send();
        List<Type> result = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        if (result.isEmpty()) return new java.util.ArrayList<>();
        List<Address> addrs = (List<Address>) result.get(0).getValue();
        List<String> out = new java.util.ArrayList<>();
        for (Address a : addrs) out.add(a.getValue());
        return out;
    }

    public String getGroupName(String groupAddress) throws Exception {
        Function function = new Function("groupName",
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<Utf8String>() {}));
        String encoded = FunctionEncoder.encode(function);
        EthCall response = web3.ethCall(
                Transaction.createEthCallTransaction(
                        credentials.getAddress(), groupAddress, encoded),
                DefaultBlockParameterName.LATEST).send();
        List<Type> result = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        return result.isEmpty() ? "" : result.get(0).getValue().toString();
    }

    public List<String> getGroupMembers(String groupAddress) throws Exception {
        Function function = new Function("getMembers",
                Collections.emptyList(),
                Collections.singletonList(new TypeReference<DynamicArray<Address>>() {}));
        String encoded = FunctionEncoder.encode(function);
        EthCall response = web3.ethCall(
                Transaction.createEthCallTransaction(
                        credentials.getAddress(), groupAddress, encoded),
                DefaultBlockParameterName.LATEST).send();
        List<Type> result = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        if (result.isEmpty()) return new java.util.ArrayList<>();
        List<Address> addrs = (List<Address>) result.get(0).getValue();
        List<String> out = new java.util.ArrayList<>();
        for (Address a : addrs) out.add(a.getValue());
        return out;
    }

    public long getBalance(String groupAddress, String debtor, String creditor) throws Exception {
        Function function = new Function("getBalance",
                Arrays.asList(new Address(debtor), new Address(creditor)),
                Collections.singletonList(new TypeReference<org.web3j.abi.datatypes.generated.Int256>() {}));
        String encoded = FunctionEncoder.encode(function);
        EthCall response = web3.ethCall(
                Transaction.createEthCallTransaction(
                        credentials.getAddress(), groupAddress, encoded),
                DefaultBlockParameterName.LATEST).send();
        List<Type> result = FunctionReturnDecoder.decode(
                response.getValue(), function.getOutputParameters());
        if (result.isEmpty()) return 0;
        return ((java.math.BigInteger) result.get(0).getValue()).longValue();
    }

    private String sendTx(String toAddress, String encodedFunction, long gasLimit) throws Exception {
        BigInteger nonce = web3.ethGetTransactionCount(
                        credentials.getAddress(), DefaultBlockParameterName.LATEST)
                .send().getTransactionCount();
        BigInteger gasPrice = web3.ethGasPrice().send().getGasPrice();
        RawTransaction rawTx = RawTransaction.createTransaction(
                nonce, gasPrice, BigInteger.valueOf(gasLimit), toAddress, encodedFunction);
        byte[] signed = TransactionEncoder.signMessage(rawTx, CHAIN_ID, credentials);
        EthSendTransaction tx = web3.ethSendRawTransaction(
                Numeric.toHexString(signed)).send();
        if (tx.hasError()) throw new Exception(tx.getError().getMessage());
        return tx.getTransactionHash();
    }

    public String renameGroup(String groupAddress, String newName) throws Exception {
        Function function = new Function("renameGroup",
                Collections.singletonList(new Utf8String(newName)),
                Collections.emptyList());
        return sendTx(groupAddress, FunctionEncoder.encode(function), EXPENSE_GAS_LIMIT);
    }

    public String addMemberToGroup(String groupAddress, String memberAddress) throws Exception {
        Function function = new Function("addMember",
                Collections.singletonList(new Address(memberAddress)),
                Collections.emptyList());
        return sendTx(groupAddress, FunctionEncoder.encode(function), EXPENSE_GAS_LIMIT);
    }

    public String removeMemberFromGroup(String groupAddress, String memberAddress) throws Exception {
        Function function = new Function("removeMember",
                Collections.singletonList(new Address(memberAddress)),
                Collections.emptyList());
        return sendTx(groupAddress, FunctionEncoder.encode(function), EXPENSE_GAS_LIMIT);
    }
}