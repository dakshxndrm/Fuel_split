// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

contract UserRegistry {

    struct User {
        string username;
        address walletAddress;
        string referralCode;
        address referredBy;
        bool isRegistered;
    }

    mapping(address => User) private users;
    mapping(string => address) private referralCodeToAddress;
    mapping(string => address) private usernameToAddress;

    event UserRegistered(address indexed wallet, string username, string referralCode);

    function register(string memory _username, string memory _referralCode) external {
        require(!users[msg.sender].isRegistered, "Already registered");
        require(usernameToAddress[_username] == address(0), "Username taken");

        string memory newCode = _generateCode(msg.sender);

        address referrer = address(0);
        if (bytes(_referralCode).length > 0) {
            referrer = referralCodeToAddress[_referralCode];
        }

        users[msg.sender] = User({
            username: _username,
            walletAddress: msg.sender,
            referralCode: newCode,
            referredBy: referrer,
            isRegistered: true
        });

        referralCodeToAddress[newCode] = msg.sender;
        usernameToAddress[_username] = msg.sender;

        emit UserRegistered(msg.sender, _username, newCode);
    }

    function getUser(address _wallet) external view returns (User memory) {
        require(users[_wallet].isRegistered, "User not found");
        return users[_wallet];
    }

    function isRegistered(address _wallet) external view returns (bool) {
        return users[_wallet].isRegistered;
    }

    function getReferralCode(address _wallet) external view returns (string memory) {
        require(users[_wallet].isRegistered, "User not found");
        return users[_wallet].referralCode;
    }

    function getAddressByUsername(string memory _username) external view returns (address) {
        return usernameToAddress[_username];
    }

    function _generateCode(address _wallet) internal view returns (string memory) {
        bytes32 hash = keccak256(abi.encodePacked(_wallet, block.timestamp));
        bytes memory code = new bytes(8);
        bytes16 hexChars = "0123456789ABCDEF";
        for (uint i = 0; i < 8; i++) {
            code[i] = hexChars[uint8(hash[i]) % 16];
        }
        return string(code);
    }
}