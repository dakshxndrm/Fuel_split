package com.example.fuel_split;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.*;


public class AddTripActivity extends AppCompatActivity {

    private ViewFlipper viewFlipper;
    private EditText etTripName, etNewPersonName, etDistance, etMileage, etFuelPrice;
    private ChipGroup chipGroup;
    private LinearLayout percentageContainer, splitModeContainer;
    private Button btnNext, btnSplitEqual, btnSplitPercent, btnSplitKm;
    private Spinner spinnerPaidBy;
    private TextView tvStepLabel, tvStepTitle, tvSplitModeHint;
    private LinearProgressIndicator stepProgress;

    private final List<String> members = new ArrayList<>();
    private int step = 0;
    private String splitMode = "equal"; // "equal", "percentage", "km"

    private static final String[] STEP_TITLES = {
            "Who was there?",
            "Trip Details",
            "Split the Cost"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trip);

        viewFlipper         = findViewById(R.id.viewFlipper);
        etTripName          = findViewById(R.id.etTripName);
        etNewPersonName     = findViewById(R.id.etNewPersonName);
        etDistance          = findViewById(R.id.etDistance);
        etMileage           = findViewById(R.id.etMileage);
        etFuelPrice         = findViewById(R.id.etFuelPrice);
        chipGroup           = findViewById(R.id.dynamicChipGroup);
        percentageContainer = findViewById(R.id.percentageContainer);
        spinnerPaidBy       = findViewById(R.id.spinnerPaidBy);
        btnNext             = findViewById(R.id.btnNext);
        tvStepLabel         = findViewById(R.id.tvStepLabel);
        tvStepTitle         = findViewById(R.id.tvStepTitle);
        stepProgress        = findViewById(R.id.stepProgress);

        updateHeader();

        findViewById(R.id.btnAddPerson).setOnClickListener(v -> addMember());
        btnNext.setOnClickListener(v -> handleNext());
    }

    // ── Update header (step label, title, progress bar) ──────────────────
    private void updateHeader() {
        tvStepLabel.setText("Step " + (step + 1) + " of 3");
        tvStepTitle.setText(STEP_TITLES[step]);
        stepProgress.setProgress((step + 1) * 33, true);
        btnNext.setText(step == 2 ? "Submit Trip  ✓" : "Next  →");
    }

    // ── Add a member chip ─────────────────────────────────────────────────
    private void addMember() {
        String name = etNewPersonName.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a name first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (members.contains(name)) {
            Toast.makeText(this, name + " is already added", Toast.LENGTH_SHORT).show();
            return;
        }

        members.add(name);

        Chip chip = new Chip(this);
        chip.setText(name);
        chip.setCloseIconVisible(true);

        final String memberName = name;
        chip.setOnCloseIconClickListener(v -> {
            members.remove(memberName);
            chipGroup.removeView(chip);
        });

        chipGroup.addView(chip);
        etNewPersonName.setText("");
    }

    // ── Handle Next / Submit button ───────────────────────────────────────
    private void handleNext() {
        if (step == 0) {
            if (etTripName.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Please enter a trip name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (members.size() < 2) {
                Toast.makeText(this, "Add at least 2 members", Toast.LENGTH_SHORT).show();
                return;
            }
            step++;
            viewFlipper.showNext();
            updateHeader();

        } else if (step == 1) {
            String dist  = etDistance.getText().toString().trim();
            String mil   = etMileage.getText().toString().trim();
            String price = etFuelPrice.getText().toString().trim();

            if (dist.isEmpty() || mil.isEmpty() || price.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Double.parseDouble(mil) <= 0) {
                Toast.makeText(this, "Mileage must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            step++;
            buildStep3();
            viewFlipper.showNext();
            updateHeader();

        } else {
            submitTrip();
        }
    }

    // ── Build Step 3: split mode selection + dynamic inputs ───────────────
    private void buildStep3() {
        percentageContainer.removeAllViews();

        // Who paid spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, members);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaidBy.setAdapter(spinnerAdapter);

        // Total cost preview
        double distance  = Double.parseDouble(etDistance.getText().toString());
        double mileage   = Double.parseDouble(etMileage.getText().toString());
        double fuelPrice = Double.parseDouble(etFuelPrice.getText().toString());
        double totalCost = (distance / mileage) * fuelPrice;

        TextView tvTotal = new TextView(this);
        tvTotal.setText("Total fuel cost:  ₹" + String.format("%.2f", totalCost));
        tvTotal.setTextColor(0xFF00C9A7);
        tvTotal.setTextSize(17);
        tvTotal.setPadding(0, 0, 0, 8);
        percentageContainer.addView(tvTotal);

        // Split mode selection section
        TextView tvSplitLabel = new TextView(this);
        tvSplitLabel.setText("How do you want to split?");
        tvSplitLabel.setTextColor(0xFF7B8AA0);
        tvSplitLabel.setTextSize(13);
        tvSplitLabel.setPadding(0, 20, 0, 10);
        percentageContainer.addView(tvSplitLabel);

        splitModeContainer = new LinearLayout(this);
        splitModeContainer.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        modeParams.setMargins(0, 0, 0, 20);
        splitModeContainer.setLayoutParams(modeParams);

        // Create three mode buttons
        btnSplitEqual = createModeButton("Equal", "equal");
        btnSplitPercent = createModeButton("By %", "percentage");
        btnSplitKm = createModeButton("By KM", "km");

        splitModeContainer.addView(btnSplitEqual);
        splitModeContainer.addView(btnSplitPercent);
        splitModeContainer.addView(btnSplitKm);
        percentageContainer.addView(splitModeContainer);

        // Hint text
        tvSplitModeHint = new TextView(this);
        tvSplitModeHint.setTextColor(0xFF7B8AA0);
        tvSplitModeHint.setTextSize(13);
        tvSplitModeHint.setPadding(0, 0, 0, 16);
        percentageContainer.addView(tvSplitModeHint);

        // Build initial split UI (default: equal)
        updateSplitMode("equal");
    }

    // ── Create mode selection button ──────────────────────────────────────
    private Button createModeButton(String label, String mode) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextSize(14);
        btn.setAllCaps(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, 52, 1f);
        params.setMargins(0, 0, 8, 0);
        btn.setLayoutParams(params);
        btn.setStateListAnimator(null);

        btn.setOnClickListener(v -> updateSplitMode(mode));
        return btn;
    }

    // ── Update split mode and rebuild UI ──────────────────────────────────
    private void updateSplitMode(String mode) {
        splitMode = mode;

        // Update button states
        updateButtonState(btnSplitEqual, mode.equals("equal"));
        updateButtonState(btnSplitPercent, mode.equals("percentage"));
        updateButtonState(btnSplitKm, mode.equals("km"));

        // Remove old input rows
        int childCount = percentageContainer.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = percentageContainer.getChildAt(i);
            if (child.getTag() != null && child.getTag().equals("inputRow")) {
                percentageContainer.removeView(child);
            }
        }

        // Update hint and build new inputs
        if (mode.equals("equal")) {
            tvSplitModeHint.setText("Cost will be divided equally among all members");
            buildEqualSplitUI();
        } else if (mode.equals("percentage")) {
            tvSplitModeHint.setText("Enter each person's % — must total 100");
            buildPercentageSplitUI();
        } else {
            tvSplitModeHint.setText("Enter kilometers driven by each person");
            buildKmSplitUI();
        }
    }

    // ── Update button visual state ────────────────────────────────────────
    private void updateButtonState(Button btn, boolean selected) {
        if (selected) {
            btn.setBackgroundResource(R.drawable.bg_segment_selected);
            btn.setTextColor(0xFF0A0E1A);
            btn.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            btn.setBackgroundResource(R.drawable.bg_segment_unselected);
            btn.setTextColor(0xFF8B98AE);
            btn.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    // ── Build equal split UI (just shows the split) ───────────────────────
    private void buildEqualSplitUI() {
        double totalCost = calculateTotalCost();
        double equalShare = totalCost / members.size();

        for (String name : members) {
            TextView tv = new TextView(this);
            tv.setText(name + "  •  ₹" + String.format("%.2f", equalShare));
            tv.setTextColor(0xFFF0F0FF);
            tv.setTextSize(15);
            tv.setPadding(16, 12, 16, 12);
            tv.setBackgroundResource(R.drawable.bg_input);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 8);
            tv.setLayoutParams(params);
            tv.setTag("inputRow");
            
            percentageContainer.addView(tv);
        }
    }

    // ── Build percentage split UI ─────────────────────────────────────────
    private void buildPercentageSplitUI() {
        for (String name : members) {
            LinearLayout row = createInputRow(name, "%", "0");
            percentageContainer.addView(row);
        }
    }

    // ── Build km-based split UI ───────────────────────────────────────────
    private void buildKmSplitUI() {
        for (String name : members) {
            LinearLayout row = createInputRow(name, "km", "0");
            percentageContainer.addView(row);
        }
    }

    // ── Create a reusable input row ───────────────────────────────────────
    private LinearLayout createInputRow(String name, String unit, String hint) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_input);
        row.setPadding(20, 12, 20, 12);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, 8);
        row.setLayoutParams(rowParams);
        row.setTag("inputRow");

        TextView tv = new TextView(this);
        tv.setText(name);
        tv.setTextColor(0xFFF0F0FF);
        tv.setTextSize(15);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tvParams);

        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(0xFF3A4A60);
        et.setTextColor(0xFF00C9A7);
        et.setTextSize(16);
        et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        et.setGravity(Gravity.CENTER);
        et.setBackgroundResource(R.drawable.bg_input);
        et.setPadding(12, 8, 12, 8);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(100, 
                LinearLayout.LayoutParams.WRAP_CONTENT);
        etParams.setMargins(8, 0, 8, 0);
        et.setLayoutParams(etParams);

        TextView unitTv = new TextView(this);
        unitTv.setText(unit);
        unitTv.setTextColor(0xFF7B8AA0);
        unitTv.setTextSize(14);

        row.addView(tv);
        row.addView(et);
        row.addView(unitTv);
        
        return row;
    }

    // ── Calculate total cost ──────────────────────────────────────────────
    private double calculateTotalCost() {
        double distance  = Double.parseDouble(etDistance.getText().toString());
        double mileage   = Double.parseDouble(etMileage.getText().toString());
        double fuelPrice = Double.parseDouble(etFuelPrice.getText().toString());
        return (distance / mileage) * fuelPrice;
    }

    // ── Submit trip and return to MainActivity ────────────────────────────
    private void submitTrip() {
        double totalCost = calculateTotalCost();
        double distance  = Double.parseDouble(etDistance.getText().toString());
        double mileage   = Double.parseDouble(etMileage.getText().toString());
        double fuelUsed  = distance / mileage;

        HashMap<String, Double> shares = new HashMap<>();

        if (splitMode.equals("equal")) {
            // Equal split - simple division
            double equalShare = totalCost / members.size();
            for (String name : members) {
                shares.put(name, equalShare);
            }

        } else if (splitMode.equals("percentage")) {
            // Percentage split - validate and calculate
            double totalPercent = 0;

            for (int i = 0; i < members.size(); i++) {
                View row = findInputRowByIndex(i);
                if (row instanceof LinearLayout) {
                    EditText et = (EditText) ((LinearLayout) row).getChildAt(1);
                    String percentStr = et.getText().toString().trim();

                    if (percentStr.isEmpty()) {
                        Toast.makeText(this, members.get(i) + "'s % is empty", 
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double percent = Double.parseDouble(percentStr);
                    if (percent < 0 || percent > 100) {
                        Toast.makeText(this, members.get(i) + "'s % must be between 0 and 100",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    totalPercent += percent;
                    shares.put(members.get(i), (percent / 100.0) * totalCost);
                }
            }

            if (Math.abs(totalPercent - 100) > 0.01) {
                Toast.makeText(this,
                        "Percentages must total 100%  (currently " + 
                        String.format("%.1f", totalPercent) + "%)",
                        Toast.LENGTH_LONG).show();
                return;
            }

        } else {
            // KM-based split
            double totalKm = 0;
            HashMap<String, Double> kmMap = new HashMap<>();

            for (int i = 0; i < members.size(); i++) {
                View row = findInputRowByIndex(i);
                if (row instanceof LinearLayout) {
                    EditText et = (EditText) ((LinearLayout) row).getChildAt(1);
                    String kmStr = et.getText().toString().trim();

                    if (kmStr.isEmpty()) {
                        Toast.makeText(this, members.get(i) + "'s km is empty", 
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double km = Double.parseDouble(kmStr);
                    if (km < 0) {
                        Toast.makeText(this, members.get(i) + "'s km cannot be negative",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    totalKm += km;
                    kmMap.put(members.get(i), km);
                }
            }

            if (totalKm == 0) {
                Toast.makeText(this, "Total km cannot be 0", Toast.LENGTH_SHORT).show();
                return;
            }

            // Calculate shares based on km proportion
            for (String name : members) {
                double km = kmMap.get(name);
                shares.put(name, (km / totalKm) * totalCost);
            }
        }

        String paidBy = spinnerPaidBy.getSelectedItem().toString();

        Intent intent = new Intent();
        intent.putExtra("TRIP_NAME",  etTripName.getText().toString().trim());
        intent.putExtra("DISTANCE",   String.format("%.1f", distance));
        intent.putExtra("FUEL_USED",  String.format("%.2f", fuelUsed));
        intent.putExtra("TOTAL_COST", String.format("%.2f", totalCost));
        intent.putExtra("MEMBERS",    members.toString());
        intent.putExtra("SHARES_MAP", shares);
        intent.putExtra("PAID_BY",    paidBy);
        intent.putExtra("SPLIT_MODE", splitMode);

        setResult(RESULT_OK, intent);
        finish();
    }

    // ── Helper to find input row by index ─────────────────────────────────
    private View findInputRowByIndex(int index) {
        int rowCount = 0;
        for (int i = 0; i < percentageContainer.getChildCount(); i++) {
            View child = percentageContainer.getChildAt(i);
            if (child.getTag() != null && child.getTag().equals("inputRow")) {
                if (rowCount == index) {
                    return child;
                }
                rowCount++;
            }
        }
        return null;
    }
}