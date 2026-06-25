package com.example.fuel_split;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import org.web3j.crypto.Credentials;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    // ── Trips ──────────────────────────────────────────────────────────────
    private List<Trip>    tripList;
    private TripAdapter   tripAdapter;

    // ── Groups ─────────────────────────────────────────────────────────────
    private List<GroupItem>  groupList = new ArrayList<>();
    private GroupAdapter     groupAdapter;
    private WalletManager    wm;
    private BlockchainManager bm;
    private ContractManager  cm;
    private Credentials      creds;
    private final Map<String, String> pendingMembers = new HashMap<>();

    // ── Views ──────────────────────────────────────────────────────────────
    private LinearLayout tripsLayout, groupsLayout, balancesLayout,
                         balancesContainer, emptyState, emptyGroups;
    private RecyclerView rvGroups;
    private TextView     tvSubtitle, tvIdentity, tvWalletAddress;
    private ExtendedFloatingActionButton fabAddTrip;
    private BottomNavigationView         bottomNav;

    private int currentTab = R.id.nav_trips; // default tab

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("fuelsplit", MODE_PRIVATE);
        if (!prefs.contains("username")) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        View rootMain   = findViewById(R.id.rootMain);
        View headerView = findViewById(R.id.appHeader);
        View navView    = findViewById(R.id.bottom_nav);
        final int headerSidePad = headerView.getPaddingLeft();
        final int navBaseHeight  = dp(72);

        ViewCompat.setOnApplyWindowInsetsListener(rootMain, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            headerView.setPadding(headerSidePad, bars.top + dp(20), headerSidePad, dp(16));
            ViewGroup.LayoutParams lp = navView.getLayoutParams();
            lp.height = navBaseHeight + bars.bottom;
            navView.setLayoutParams(lp);
            navView.setPadding(navView.getPaddingLeft(), 0, navView.getPaddingRight(), bars.bottom);
            return insets;
        });

        tripsLayout       = findViewById(R.id.tripsLayout);
        groupsLayout      = findViewById(R.id.groupsLayout);
        balancesLayout    = findViewById(R.id.balancesLayout);
        balancesContainer = findViewById(R.id.balancesContainer);
        emptyState        = findViewById(R.id.emptyState);
        emptyGroups       = findViewById(R.id.emptyGroups);
        rvGroups          = findViewById(R.id.rvGroups);
        tvSubtitle        = findViewById(R.id.tvSubtitle);
        fabAddTrip        = findViewById(R.id.fabAddTrip);
        bottomNav         = findViewById(R.id.bottom_nav);

        // Identity row
        LinearLayout header = findViewById(R.id.appHeader);
        tvIdentity = new TextView(this);
        tvIdentity.setTextColor(0xFFFFFFFF);
        tvIdentity.setTextSize(15);
        tvIdentity.setPadding(0, dp(6), 0, dp(2));
        header.addView(tvIdentity, 1);

        tvWalletAddress = new TextView(this);
        tvWalletAddress.setTextColor(0xFF7B8AA0);
        tvWalletAddress.setTextSize(11);
        tvWalletAddress.setPadding(0, 0, 0, dp(4));
        header.addView(tvWalletAddress, 2);

        SharedPreferences profilePrefs = getSharedPreferences("FuelSplitProfile", MODE_PRIVATE);
        String displayName = profilePrefs.getString("displayName", "");
        String friendCode  = profilePrefs.getString("code", "");
        if (!displayName.isEmpty()) {
            tvIdentity.setText(displayName + "  —  Code: " + friendCode);
            tvIdentity.setOnClickListener(v -> {
                ClipboardManager cm2 = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm2.setPrimaryClip(ClipData.newPlainText("Friend code", friendCode));
                Toast.makeText(this, "Code copied!", Toast.LENGTH_SHORT).show();
            });
        }

        // Pre-warm NameResolver from saved contacts (no network needed)
        ContactStore.seedNameResolver(this);

        // ── Trips RecyclerView ─────────────────────────────────────────────
        tripList = TripStore.loadTrips(this);
        RecyclerView rvTrips = findViewById(R.id.rvTrips);
        rvTrips.setLayoutManager(new LinearLayoutManager(this));

        tripAdapter = new TripAdapter(tripList,
                pos -> {
                    Trip t = tripList.get(pos);
                    new AlertDialog.Builder(this)
                            .setTitle("Delete Trip")
                            .setMessage("Remove \"" + t.getTripName() + "\"?")
                            .setPositiveButton("Delete", (d, w) -> {
                                tripList.remove(pos);
                                TripStore.saveTrips(this, tripList);
                                tripAdapter.notifyDataSetChanged();
                                updateEmptyState();
                                updateSubtitle();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                },
                pos -> {
                    Intent intent = new Intent(this, TripDetailActivity.class);
                    intent.putExtra(TripDetailActivity.EXTRA_TRIP_INDEX, pos);
                    startActivity(intent);
                });
        rvTrips.setAdapter(tripAdapter);

        // ── Groups RecyclerView ────────────────────────────────────────────
        groupAdapter = new GroupAdapter(groupList, group -> {
            Intent intent = new Intent(this, GroupDetailActivity.class);
            intent.putExtra(GroupDetailActivity.EXTRA_GROUP_ADDRESS, group.contractAddress);
            startActivity(intent);
        });
        rvGroups.setLayoutManager(new LinearLayoutManager(this));
        rvGroups.setAdapter(groupAdapter);

        updateEmptyState();
        updateSubtitle();

        // ── Bottom nav ─────────────────────────────────────────────────────
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            currentTab = id;
            tripsLayout.setVisibility(View.GONE);
            groupsLayout.setVisibility(View.GONE);
            balancesLayout.setVisibility(View.GONE);

            if (id == R.id.nav_trips) {
                tripsLayout.setVisibility(View.VISIBLE);
                fabAddTrip.setText("New Trip");
                fabAddTrip.show();
            } else if (id == R.id.nav_groups) {
                groupsLayout.setVisibility(View.VISIBLE);
                fabAddTrip.setText("New Group");
                fabAddTrip.show();
                if (cm != null) loadGroups();
            } else if (id == R.id.nav_balances) {
                balancesLayout.setVisibility(View.VISIBLE);
                fabAddTrip.hide();
                showSettlements();
            }
            return true;
        });

        // ── FAB ────────────────────────────────────────────────────────────
        fabAddTrip.setOnClickListener(v -> {
            if (currentTab == R.id.nav_groups) showCreateGroupDialog();
            else startActivity(new Intent(this, AddTripActivity.class));
        });

        // ── Wallet + auto-register ─────────────────────────────────────────
        new Thread(() -> {
            try {
                wm    = new WalletManager(this);
                creds = wm.getOrCreateWallet();
                String myAddress = creds.getAddress();
                bm    = new BlockchainManager();
                cm    = new ContractManager(bm.getWeb3(), creds);

                String shortAddr = myAddress.substring(0, 8) + "…"
                        + myAddress.substring(myAddress.length() - 6);
                runOnUiThread(() -> tvWalletAddress.setText(shortAddr));

                boolean already = cm.isRegistered(myAddress);
                if (!already) {
                    String savedCode = getSharedPreferences("FuelSplitProfile", MODE_PRIVATE)
                            .getString("code", "");
                    if (!savedCode.isEmpty()) cm.register(savedCode, "");
                }
                // Load groups in background once cm is ready
                runOnUiThread(() -> { if (currentTab == R.id.nav_groups) loadGroups(); });
            } catch (Exception e) {
                android.util.Log.e("FUELSPLIT", "Init error", e);
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload trips whenever we return from AddTripActivity or TripDetailActivity
        tripList.clear();
        tripList.addAll(TripStore.loadTrips(this));
        tripAdapter.notifyDataSetChanged();
        updateEmptyState();
        updateSubtitle();
        // Refresh the active tab
        if (currentTab == R.id.nav_groups && cm != null) loadGroups();
        if (currentTab == R.id.nav_balances) showSettlements();
    }

    // ── Groups loading ─────────────────────────────────────────────────────
    private void loadGroups() {
        new Thread(() -> {
            try {
                List<String> addrs = cm.getUserGroups();
                groupList.clear();
                for (String addr : addrs) {
                    String       name    = cm.getGroupName(addr);
                    List<String> members = cm.getGroupMembers(addr);
                    groupList.add(new GroupItem(name, addr, members.size()));
                }
                runOnUiThread(() -> {
                    groupAdapter.notifyDataSetChanged();
                    boolean empty = groupList.isEmpty();
                    rvGroups.setVisibility(empty ? View.GONE : View.VISIBLE);
                    emptyGroups.setVisibility(empty ? View.VISIBLE : View.GONE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Groups load error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ── Create Group dialog (mirrored from GroupsActivity) ─────────────────
    private void showCreateGroupDialog() {
        if (cm == null) {
            Toast.makeText(this, "Still connecting, please wait…", Toast.LENGTH_SHORT).show();
            return;
        }
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_create_group);
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        android.widget.EditText etGroupName   = dialog.findViewById(R.id.etGroupName);
        android.widget.EditText etMemberCode  = dialog.findViewById(R.id.etMemberUsername);
        MaterialButton btnAdd    = dialog.findViewById(R.id.btnAddMember);
        ChipGroup chipGroup      = dialog.findViewById(R.id.chipGroupMembers);
        MaterialButton btnCreate = dialog.findViewById(R.id.btnCreateGroup);
        TextView tvStatus        = dialog.findViewById(R.id.tvDialogStatus);

        pendingMembers.clear();
        etMemberCode.setHint("Friend code");

        btnAdd.setOnClickListener(v -> {
            String code = etMemberCode.getText().toString().trim();
            if (code.isEmpty()) return;
            tvStatus.setText("Looking up " + code + "...");
            new Thread(() -> {
                try {
                    String[] result = ProfileClient.lookupByCode(code);
                    String addr      = result[0];
                    String chipLabel = result[1].isEmpty() ? code : result[1];
                    ContactStore.addContact(MainActivity.this, code, chipLabel, addr);
                    runOnUiThread(() -> {
                        pendingMembers.put(code, addr);
                        Chip chip = new Chip(this);
                        chip.setText(chipLabel);
                        chip.setCloseIconVisible(true);
                        chip.setOnCloseIconClickListener(c -> {
                            chipGroup.removeView(chip);
                            pendingMembers.remove(code);
                        });
                        chipGroup.addView(chip);
                        etMemberCode.setText("");
                        tvStatus.setText("");
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> tvStatus.setText(e.getMessage()));
                }
            }).start();
        });

        btnCreate.setOnClickListener(v -> {
            String groupName = etGroupName.getText().toString().trim();
            if (groupName.isEmpty()) { etGroupName.setError("Enter group name"); return; }
            if (pendingMembers.isEmpty()) { tvStatus.setText("Add at least one member"); return; }

            tvStatus.setText("Creating group on blockchain...");
            btnCreate.setEnabled(false);
            new Thread(() -> {
                try {
                    List<String> addrs = new ArrayList<>(pendingMembers.values());
                    String hash = cm.createGroup(groupName, addrs);
                    cm.waitForReceipt(hash);
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        loadGroups();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        tvStatus.setText("Error: " + e.getMessage());
                        btnCreate.setEnabled(true);
                    });
                }
            }).start();
        });

        dialog.show();
    }

    // ── Empty state ────────────────────────────────────────────────────────
    private void updateEmptyState() {
        boolean isEmpty = tripList.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        findViewById(R.id.rvTrips).setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateSubtitle() {
        int count = tripList.size();
        if (count == 0) tvSubtitle.setText("No trips logged yet");
        else tvSubtitle.setText(count + (count == 1 ? " trip" : " trips") + " logged");
    }

    // ── Balances tab (only unsettled trips) ───────────────────────────────
    private void showSettlements() {
        balancesContainer.removeAllViews();
        List<String> settlements = calculateSettlements();

        if (settlements.isEmpty()) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(android.view.Gravity.CENTER);
            card.setBackgroundResource(R.drawable.bg_all_settled);
            card.setPadding(48, 56, 48, 56);

            TextView icon = new TextView(this);
            icon.setText("✓");
            icon.setTextColor(0xFF00C9A7);
            icon.setTextSize(40);
            icon.setGravity(android.view.Gravity.CENTER);
            card.addView(icon);

            TextView msg = new TextView(this);
            msg.setText("All settled up!");
            msg.setTextColor(0xFF00C9A7);
            msg.setTextSize(20);
            msg.setTypeface(null, android.graphics.Typeface.BOLD);
            msg.setGravity(android.view.Gravity.CENTER);
            msg.setPadding(0, 12, 0, 0);
            card.addView(msg);

            TextView sub = new TextView(this);
            sub.setText("No pending payments between anyone");
            sub.setTextColor(0xFF7B8AA0);
            sub.setTextSize(14);
            sub.setGravity(android.view.Gravity.CENTER);
            sub.setPadding(0, 8, 0, 0);
            card.addView(sub);

            balancesContainer.addView(card);
            return;
        }

        for (String s : settlements) {
            View item = getLayoutInflater()
                    .inflate(R.layout.item_settlement, balancesContainer, false);

            String[] byPays   = s.split(" pays ");
            String debtorName = byPays[0].trim();
            String[] byTo     = byPays[1].split(" to ");
            String amount     = byTo[0].trim();
            String creditor   = byTo[1].trim();

            ((TextView) item.findViewById(R.id.tvDebtor)).setText(debtorName);
            ((TextView) item.findViewById(R.id.tvAmount)).setText(amount);
            ((TextView) item.findViewById(R.id.tvCreditor)).setText(creditor);

            balancesContainer.addView(item);
        }
    }

    private List<String> calculateSettlements() {
        HashMap<String, Double> netBalance = new HashMap<>();

        for (Trip trip : tripList) {
            if (trip.isSettled()) continue;

            double total  = Double.parseDouble(trip.getTotal());
            String paidBy = trip.getPaidBy();
            Map<String, Double> shares = trip.getAllShares();

            netBalance.put(paidBy, netBalance.getOrDefault(paidBy, 0.0) + total);
            for (Map.Entry<String, Double> entry : shares.entrySet()) {
                String person = entry.getKey();
                double amount = entry.getValue();
                netBalance.put(person, netBalance.getOrDefault(person, 0.0) - amount);
            }
        }

        return simplifyDebts(netBalance);
    }

    private List<String> simplifyDebts(HashMap<String, Double> netBalance) {
        List<String> result = new ArrayList<>();
        List<Map.Entry<String, Double>> creditors = new ArrayList<>();
        List<Map.Entry<String, Double>> debtors   = new ArrayList<>();

        for (Map.Entry<String, Double> entry : netBalance.entrySet()) {
            if (entry.getValue() >  0.5) creditors.add(entry);
            if (entry.getValue() < -0.5) debtors.add(entry);
        }

        int i = 0, j = 0;
        while (i < debtors.size() && j < creditors.size()) {
            Map.Entry<String, Double> debtor   = debtors.get(i);
            Map.Entry<String, Double> creditor = creditors.get(j);
            double amount = Math.min(-debtor.getValue(), creditor.getValue());
            result.add(debtor.getKey() + " pays ₹"
                    + String.format("%.2f", amount) + " to " + creditor.getKey());
            debtor.setValue(debtor.getValue() + amount);
            creditor.setValue(creditor.getValue() - amount);
            if (Math.abs(debtor.getValue())   < 0.5) i++;
            if (Math.abs(creditor.getValue()) < 0.5) j++;
        }

        return result;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
