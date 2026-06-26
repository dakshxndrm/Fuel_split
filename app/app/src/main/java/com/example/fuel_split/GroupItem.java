package com.example.fuel_split;

public class GroupItem {
    public String name;
    public String contractAddress;
    public int memberCount;

    public GroupItem(String name, String contractAddress, int memberCount) {
        this.name = name;
        this.contractAddress = contractAddress;
        this.memberCount = memberCount;
    }
}