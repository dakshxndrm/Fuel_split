package com.example.fuel_split;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.web3j.crypto.Credentials;

import java.util.ArrayList;
import java.util.List;

public class GroupDetailActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ADDRESS = "group_address";

    private String groupAddress;
    private TextView tvGroupName, tvMemberCount, tvNoExpenses;
    private LinearLayout membersContainer, balancesContainer, expensesContainer;

    private WalletManager wm;
    private BlockchainManager bm;
    private ContractManager cm;
    private Credentials creds;

    private List<String> currentMembers = new ArrayList<>();
    private String currentGroupName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        groupAddress = getIntent().getStringExtra(EXTRA_GROUP_ADDRESS);

        tvGroupName       = findViewById(R.id.tvDetailGroupName);
        tvMemberCount     = findViewById(R.id.tvDetailMemberCount);
        tvNoExpenses      = findViewById(R.id.tvNoExpenses);
        membersContainer  = findViewById(R.id.membersContainer);
        balancesContainer = findViewById(R.id.balancesContainer);
        expensesContainer = findViewById(R.id.expensesContainer);

        findViewById(R.id.btnEditGroup).setOnClickListener(v -> showEditDialog());
        findViewById(R.id.btnAddExpense).setOnClickListener(v -> {
            // P3 Screen 3 — wired up next session
            android.widget.Toast.makeText(this, "Add expense — coming next!", android.widget.Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnSettle).setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Settle — coming next!", android.widget.Toast.LENGTH_SHORT).show();
        });

        initBlockchain();
    }

    private void initBlockchain() {
        new Thread(() -> {
            try {
                wm    = new WalletManager(this);
                creds = wm.getOrCreateWallet();
                bm    = new BlockchainManager();
                cm    = new ContractManager(bm.getWeb3(), creds);
                runOnUiThread(this::loadGroupData);
            } catch (Exception e) {
                runOnUiThread(() -> toast("Init error: " + e.getMessage()));
            }
        }).start();
    }

    private void loadGroupData() {
        new Thread(() -> {
            try {
                String name       = cm.getGroupName(groupAddress);
                List<String> members = cm.getGroupMembers(groupAddress);
                currentGroupName  = name;
                currentMembers    = members;

                runOnUiThread(() -> {
                    tvGroupName.setText(name);
                    tvMemberCount.setText(members.size() + " members");
                    renderMembers(members);
                    renderBalances(members);
                    tvNoExpenses.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> toast("Load error: " + e.getMessage()));
            }
        }).start();
    }

    private void renderMembers(List<String> members) {
        membersContainer.removeAllViews();
        for (String addr : members) {
            View row = getLayoutInflater().inflate(R.layout.item_trip, membersContainer, false);
            TextView tv = new TextView(this);
            tv.setText("• " + addr.substring(0, 10) + "...");
            tv.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tv.setTextSize(14);
            tv.setPadding(0, 8, 0, 8);
            membersContainer.addView(tv);
        }
    }

    private void renderBalances(List<String> members) {
        balancesContainer.removeAllViews();
        String myAddr = creds.getAddress().toLowerCase();

        new Thread(() -> {
            for (String other : members) {
                if (other.toLowerCase().equals(myAddr)) continue;
                try {
                    long bal = cm.getBalance(groupAddress, myAddr, other);
                    String label;
                    int color;
                    if (bal > 0) {
                        label = "You owe " + other.substring(0, 8) + "... ₹" + (bal / 100);
                        color = R.color.money_negative;
                    } else if (bal < 0) {
                        label = other.substring(0, 8) + "... owes you ₹" + (Math.abs(bal) / 100);
                        color = R.color.money_positive;
                    } else {
                        label = "Settled with " + other.substring(0, 8) + "...";
                        color = R.color.text_secondary;
                    }
                    String finalLabel = label;
                    int finalColor = color;
                    runOnUiThread(() -> {
                        TextView tv = new TextView(this);
                        tv.setText(finalLabel);
                        tv.setTextColor(ContextCompat.getColor(this, finalColor));
                        tv.setTextSize(15);
                        tv.setPadding(0, 10, 0, 10);
                        balancesContainer.addView(tv);
                    });
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void showEditDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_group);
        dialog.getWindow().setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText etName     = dialog.findViewById(R.id.etEditGroupName);
        EditText etUsername = dialog.findViewById(R.id.etEditMemberUsername);
        MaterialButton btnAdd  = dialog.findViewById(R.id.btnEditAddMember);
        ChipGroup chipGroup    = dialog.findViewById(R.id.chipEditMembers);
        MaterialButton btnSave = dialog.findViewById(R.id.btnSaveGroup);
        TextView tvStatus      = dialog.findViewById(R.id.tvEditStatus);

        etName.setText(currentGroupName);

        // Show current members as removable chips
        for (String addr : currentMembers) {
            addMemberChip(chipGroup, addr, addr.substring(0, 8) + "...", dialog, tvStatus);
        }

        btnAdd.setOnClickListener(v -> {
            String uname = etUsername.getText().toString().trim();
            if (uname.isEmpty()) return;
            tvStatus.setText("Looking up " + uname + "...");
            new Thread(() -> {
                try {
                    String addr = cm.getUsernameAddress(uname);
                    runOnUiThread(() -> {
                        if (addr == null) {
                            tvStatus.setText(uname + " not found");
                        } else {
                            tvStatus.setText("Adding " + uname + " on-chain...");
                            new Thread(() -> {
                                try {
                                    cm.addMemberToGroup(groupAddress, addr);
                                    runOnUiThread(() -> {
                                        addMemberChip(chipGroup, addr, uname, dialog, tvStatus);
                                        etUsername.setText("");
                                        tvStatus.setText(uname + " added!");
                                        currentMembers.add(addr);
                                    });
                                } catch (Exception e) {
                                    runOnUiThread(() -> tvStatus.setText("Error: " + e.getMessage()));
                                }
                            }).start();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> tvStatus.setText("Error: " + e.getMessage()));
                }
            }).start();
        });

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            if (newName.isEmpty()) { etName.setError("Enter name"); return; }
            if (newName.equals(currentGroupName)) { dialog.dismiss(); return; }

            tvStatus.setText("Renaming group...");
            btnSave.setEnabled(false);
            new Thread(() -> {
                try {
                    cm.renameGroup(groupAddress, newName);
                    currentGroupName = newName;
                    runOnUiThread(() -> {
                        tvGroupName.setText(newName);
                        dialog.dismiss();
                        toast("Group updated!");
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        tvStatus.setText("Error: " + e.getMessage());
                        btnSave.setEnabled(true);
                    });
                }
            }).start();
        });

        dialog.show();
    }

    private void addMemberChip(ChipGroup cg, String address, String label,
                               Dialog dialog, TextView tvStatus) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            tvStatus.setText("Removing " + label + "...");
            new Thread(() -> {
                try {
                    cm.removeMemberFromGroup(groupAddress, address);
                    currentMembers.remove(address);
                    runOnUiThread(() -> {
                        cg.removeView(chip);
                        tvStatus.setText(label + " removed");
                        tvMemberCount.setText(currentMembers.size() + " members");
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> tvStatus.setText("Error: " + e.getMessage()));
                }
            }).start();
        });
        cg.addView(chip);
    }

    private void toast(String msg) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();
    }
}