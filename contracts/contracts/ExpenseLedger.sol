// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

contract ExpenseLedger {

    struct Expense {
        uint256 id;
        string description;
        uint256 amountPaise;
        address paidBy;
        address[] members;
        uint256[] shares;
        uint256 timestamp;
    }

    // A single, explicit debt line: debtor owes creditor this amount.
    // settled flips true the moment the debtor pays — kept in the list as history.
    struct Debt {
        uint256 id;
        address debtor;
        address creditor;
        uint256 amountPaise;
        string description; // which trip/expense it came from
        bool settled;
        uint256 createdAt;
        uint256 settledAt;
    }

    string public groupName;
    address public creator;
    address[] public members;
    mapping(address => bool) public isMember;

    Expense[] public expenses;
    Debt[] public debts;

    // Live net owed between two people (debtor => creditor => paise still owed)
    mapping(address => mapping(address => uint256)) public owed;

    bool public deleted;

    event ExpenseAdded(uint256 indexed id, string description, uint256 amount, address paidBy);
    event DebtCreated(uint256 indexed id, address indexed debtor, address indexed creditor, uint256 amount);
    event DebtSettled(uint256 indexed id, address indexed debtor, address indexed creditor, uint256 amount);
    event MemberAdded(address indexed member);
    event MemberRemoved(address indexed member);
    event GroupDeleted();

    modifier onlyMember() {
        require(isMember[msg.sender], "Not a group member");
        _;
    }

    modifier onlyCreator() {
        require(msg.sender == creator, "Only creator");
        _;
    }

    modifier notDeleted() {
        require(!deleted, "Group deleted");
        _;
    }

    constructor(string memory _name, address _creator, address[] memory _members) {
        groupName = _name;
        creator = _creator;

        members.push(_creator);
        isMember[_creator] = true;

        for (uint i = 0; i < _members.length; i++) {
            if (!isMember[_members[i]]) {
                members.push(_members[i]);
                isMember[_members[i]] = true;
            }
        }
    }

    // ── Expenses ────────────────────────────────────────────────────────────
    function addExpense(
        string memory _description,
        uint256 _amountPaise,
        address[] memory _splitMembers,
        uint256[] memory _shares
    ) external onlyMember notDeleted {
        require(_amountPaise > 0, "Amount must be positive");
        require(_splitMembers.length == _shares.length, "Members/shares mismatch");
        require(_splitMembers.length > 0, "Need at least one member");

        uint256 totalShares = 0;
        for (uint i = 0; i < _shares.length; i++) {
            require(isMember[_splitMembers[i]], "Split includes a non-member");
            totalShares += _shares[i];
        }
        require(totalShares == 100, "Shares must add to 100");

        expenses.push(Expense({
            id: expenses.length,
            description: _description,
            amountPaise: _amountPaise,
            paidBy: msg.sender,
            members: _splitMembers,
            shares: _shares,
            timestamp: block.timestamp
        }));

        // What everyone except the payer should collectively cover.
        uint256 payerShare = 0;
        for (uint i = 0; i < _splitMembers.length; i++) {
            if (_splitMembers[i] == msg.sender) payerShare = _shares[i];
        }
        uint256 othersTotal = _amountPaise - (_amountPaise * payerShare) / 100;

        // One explicit debt line per person who owes the payer (raw, no netting).
        // Each share rounds down; the LAST debtor absorbs the leftover paise so the
        // debts sum to exactly othersTotal and the payer is fully repaid.
        uint256 assigned = 0;
        uint256 lastDebtIndex = type(uint256).max;
        for (uint i = 0; i < _splitMembers.length; i++) {
            if (_splitMembers[i] != msg.sender) {
                uint256 owedAmount = (_amountPaise * _shares[i]) / 100;
                if (owedAmount > 0) {
                    debts.push(Debt({
                        id: debts.length,
                        debtor: _splitMembers[i],
                        creditor: msg.sender,
                        amountPaise: owedAmount,
                        description: _description,
                        settled: false,
                        createdAt: block.timestamp,
                        settledAt: 0
                    }));
                    owed[_splitMembers[i]][msg.sender] += owedAmount;
                    assigned += owedAmount;
                    lastDebtIndex = debts.length - 1;
                    emit DebtCreated(lastDebtIndex, _splitMembers[i], msg.sender, owedAmount);
                }
            }
        }

        // Hand any rounding remainder to the last debt so totals reconcile exactly.
        if (lastDebtIndex != type(uint256).max && othersTotal > assigned) {
            uint256 remainder = othersTotal - assigned;
            Debt storage last = debts[lastDebtIndex];
            last.amountPaise += remainder;
            owed[last.debtor][last.creditor] += remainder;
        }

        emit ExpenseAdded(expenses.length - 1, _description, _amountPaise, msg.sender);
    }

    // ── Settle: debtor pays a single debt line, drops instantly ──────────────
    function settleDebt(uint256 _debtId) external notDeleted {
        Debt storage d = debts[_debtId];
        require(d.debtor == msg.sender, "Only the debtor can settle");
        require(!d.settled, "Already settled");

        d.settled = true;
        d.settledAt = block.timestamp;
        owed[d.debtor][d.creditor] -= d.amountPaise;

        emit DebtSettled(_debtId, d.debtor, d.creditor, d.amountPaise);
    }

    // ── Membership ───────────────────────────────────────────────────────────
    function addMember(address _newMember) external onlyMember notDeleted {
        require(!isMember[_newMember], "Already a member");
        members.push(_newMember);
        isMember[_newMember] = true;
        emit MemberAdded(_newMember);
    }

    function removeMember(address _member) external onlyMember notDeleted {
        require(isMember[_member], "Not a member");
        require(_member != creator, "Cannot remove creator");

        // Blocked if they still owe or are owed anything (safe rule).
        for (uint i = 0; i < members.length; i++) {
            if (members[i] != _member) {
                require(owed[_member][members[i]] == 0, "Member still owes money");
                require(owed[members[i]][_member] == 0, "Member is still owed money");
            }
        }

        isMember[_member] = false;
        for (uint i = 0; i < members.length; i++) {
            if (members[i] == _member) {
                members[i] = members[members.length - 1];
                members.pop();
                break;
            }
        }
        emit MemberRemoved(_member);
    }

    function renameGroup(string memory _newName) external onlyMember notDeleted {
        groupName = _newName;
    }

    // ── Delete: creator only ─────────────────────────────────────────────────
    function deleteGroup() external onlyCreator notDeleted {
        deleted = true;
        emit GroupDeleted();
    }

    // ── Reads ────────────────────────────────────────────────────────────────
    function getExpenses() external view returns (Expense[] memory) {
        return expenses;
    }

    function getDebts() external view returns (Debt[] memory) {
        return debts;
    }

    function getMembers() external view returns (address[] memory) {
        return members;
    }

    function getOwed(address _debtor, address _creditor) external view returns (uint256) {
        return owed[_debtor][_creditor];
    }

    function getExpenseCount() external view returns (uint256) {
        return expenses.length;
    }

    function getDebtCount() external view returns (uint256) {
        return debts.length;
    }
}
