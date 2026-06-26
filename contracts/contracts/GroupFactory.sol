// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "./ExpenseLedger.sol";

contract GroupFactory {

    struct GroupInfo {
        address contractAddress;
        string name;
        address creator;
        uint256 createdAt;
    }

    mapping(address => address[]) private userGroups;
    address[] public allGroups;

    event GroupCreated(address indexed creator, address groupContract, string name);

    function createGroup(string memory _name, address[] memory _members) external returns (address) {
        ExpenseLedger newGroup = new ExpenseLedger(_name, msg.sender, _members);
        address groupAddr = address(newGroup);

        allGroups.push(groupAddr);
        userGroups[msg.sender].push(groupAddr);

        for (uint i = 0; i < _members.length; i++) {
            if (_members[i] != msg.sender) {
                userGroups[_members[i]].push(groupAddr);
            }
        }

        emit GroupCreated(msg.sender, groupAddr, _name);
        return groupAddr;
    }

    function getUserGroups(address _user) external view returns (address[] memory) {
        return userGroups[_user];
    }

    function getTotalGroups() external view returns (uint256) {
        return allGroups.length;
    }
}