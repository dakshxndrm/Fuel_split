// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

contract ReferralRegistry {

    mapping(address => address[]) private referrals;
    mapping(address => address) private referredBy;
    mapping(address => uint256) public referralCount;

    event ReferralRecorded(address indexed referrer, address indexed newUser);

    function recordReferral(address _referrer, address _newUser) external {
        require(_referrer != _newUser, "Cannot refer yourself");
        require(referredBy[_newUser] == address(0), "Already referred");

        referredBy[_newUser] = _referrer;
        referrals[_referrer].push(_newUser);
        referralCount[_referrer]++;

        emit ReferralRecorded(_referrer, _newUser);
    }

    function getReferrals(address _referrer) external view returns (address[] memory) {
        return referrals[_referrer];
    }

    function getReferredBy(address _user) external view returns (address) {
        return referredBy[_user];
    }

    function getReferralCount(address _referrer) external view returns (uint256) {
        return referralCount[_referrer];
    }
}