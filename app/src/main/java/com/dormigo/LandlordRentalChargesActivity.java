package com.dormigo;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LandlordRentalChargesActivity extends AppCompatActivity {

    private int houseId;
    private String houseName;
    private SwipeRefreshLayout swipeRefreshLayout;

    private LinearLayout depositsContainer;
    private LinearLayout feesContainer;
    private LinearLayout utilitiesContainer;

    private JSONArray cachedDeposits = new JSONArray();
    private JSONArray cachedFees = new JSONArray();
    private JSONArray cachedUtilities = new JSONArray();
    private JSONArray cachedRooms = new JSONArray();
    private int selectedRoomIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landlord_rental_charges);

        View mainLayout = findViewById(R.id.mainLayout);
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        if (savedInstanceState != null) {
            houseId = savedInstanceState.getInt("HOUSE_ID", 0);
            houseName = savedInstanceState.getString("PROPERTY_NAME", "");
        } else {
            Intent intent = getIntent();
            if (intent != null) {
                houseId = intent.getIntExtra("HOUSE_ID", 0);
                houseName = intent.getStringExtra("PROPERTY_NAME");
            }
        }

        if (houseId <= 0) {
            Toast.makeText(this, "Unable to load rental charges for this property.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView houseNameLabel = findViewById(R.id.houseNameLabel);
        if (houseNameLabel != null && houseName != null && !houseName.isEmpty()) {
            houseNameLabel.setText(houseName);
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnPreviewBilling = findViewById(R.id.btnPreviewBilling);
        if (btnPreviewBilling != null) {
            btnPreviewBilling.setOnClickListener(v -> showStudentBillingPreviewDialog());
        }

        depositsContainer = findViewById(R.id.depositsContainer);
        feesContainer = findViewById(R.id.feesContainer);
        utilitiesContainer = findViewById(R.id.utilitiesContainer);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadCharges);
        }

        setupAddButtons();
        loadCharges();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("HOUSE_ID", houseId);
        outState.putString("PROPERTY_NAME", houseName);
    }

    private void setupAddButtons() {
        View btnAddDeposit = findViewById(R.id.btnAddDeposit);
        if (btnAddDeposit != null) btnAddDeposit.setOnClickListener(v -> showAddDepositDialog(null));

        View btnAddFee = findViewById(R.id.btnAddFee);
        if (btnAddFee != null) btnAddFee.setOnClickListener(v -> showAddFeeDialog(null));
    }

    private void loadRooms() {
        if (houseId <= 0) return;
        String url = "http://10.209.52.109/Dormigo_Backend/api/rooms.php?house_id=" + houseId;
        Request request = new Request.Builder().url(url).get().build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("success", false)) {
                        cachedRooms = json.optJSONArray("data");
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void loadCharges() {
        if (houseId <= 0) return;
        loadRooms();
        String url = "http://10.209.52.109/Dormigo_Backend/api/property_charges.php?house_id=" + houseId;
        Request request = new Request.Builder().url(url).get().build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(LandlordRentalChargesActivity.this, "Failed to load charges.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                try {
                    JSONObject json = new JSONObject(body);
                    if (json.optBoolean("success", false)) {
                        JSONObject data = json.optJSONObject("data");
                        if (data != null) {
                            cachedDeposits = data.optJSONArray("deposits");
                            cachedFees = data.optJSONArray("additional_fees");
                            cachedUtilities = data.optJSONArray("utilities");

                            runOnUiThread(() -> {
                                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                                renderDeposits(cachedDeposits);
                                renderFees(cachedFees);
                                renderUtilities(cachedUtilities);
                            });
                        }
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    });
                }
            }
        });
    }

    private void renderDeposits(JSONArray arr) {
        if (depositsContainer == null) return;
        depositsContainer.removeAllViews();
        if (arr == null || arr.length() == 0) {
            depositsContainer.addView(createEmptyState("No required deposits configured. (Students pay 0 deposits)"));
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj != null) {
                String name = obj.optString("deposit_name", "");
                double amount = obj.optDouble("amount", 0);
                String desc = obj.optString("description", "");
                boolean hasDesc = !desc.trim().isEmpty() && !"null".equalsIgnoreCase(desc.trim());
                String valStr = formatMoney(amount) + (hasDesc ? " · " + desc.trim() : "");
                depositsContainer.addView(createItemCard(name, valStr, obj, "edit_deposit", "delete_deposit", "deposit_id", true));
            }
        }
    }

    private void renderFees(JSONArray arr) {
        if (feesContainer == null) return;
        feesContainer.removeAllViews();
        if (arr == null || arr.length() == 0) {
            feesContainer.addView(createEmptyState("No additional fees configured."));
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj != null) {
                String name = obj.optString("fee_name", "");
                String type = obj.optString("fee_type", "ONE_TIME");
                double amount = obj.optDouble("amount", 0);
                String desc = obj.optString("description", "");
                boolean hasDesc = !desc.trim().isEmpty() && !"null".equalsIgnoreCase(desc.trim());
                String typeLabel = ("MONTHLY".equalsIgnoreCase(type) ? "Monthly Recurring Fee · " + formatMoney(amount) + "/month" : "One-Time Fee · " + formatMoney(amount))
                        + (hasDesc ? "\n" + desc.trim() : "");
                feesContainer.addView(createItemCard(name, typeLabel, obj, "edit_fee", "delete_fee", "fee_id", true));
            }
        }
    }

    private void renderUtilities(JSONArray arr) {
        if (utilitiesContainer == null) return;
        utilitiesContainer.removeAllViews();
        if (arr == null || arr.length() == 0) {
            utilitiesContainer.addView(createEmptyState("No utilities configured."));
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj != null) {
                String name = obj.optString("utility_name", "");
                String method = obj.optString("charging_method", "");
                String valStr = method;
                if ("FIXED_MONTHLY".equalsIgnoreCase(method)) {
                    valStr = formatMoney(obj.optDouble("fixed_amount", 0)) + "/month";
                } else if ("CONSUMPTION_BASED".equalsIgnoreCase(method)) {
                    valStr = "Charged according to actual meter reading";
                } else if ("FREE".equalsIgnoreCase(method)) {
                    valStr = "Free";
                }
                utilitiesContainer.addView(createItemCard(name, valStr, obj, "edit_utility", "delete_utility", "utility_id", false));
            }
        }
    }

    private View createItemCard(String title, String value, JSONObject itemData, String editAction, String deleteAction, String idKey, boolean allowDelete) {
        LinearLayout card = new LinearLayout(this);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundResource(R.drawable.bg_card_rounded);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
        params.bottomMargin = dp(8);
        card.setLayoutParams(params);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        textLayout.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor("#1A1A1A"));
        tvTitle.setTextSize(15);
        tvTitle.setTypeface(null, Typeface.BOLD);

        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextColor(Color.parseColor("#6E6E73"));
        tvVal.setTextSize(13);

        textLayout.addView(tvTitle);
        textLayout.addView(tvVal);
        card.addView(textLayout);

        TextView btnEdit = new TextView(this);
        btnEdit.setText("Edit");
        btnEdit.setTextColor(Color.parseColor("#1B5E4C"));
        btnEdit.setTextSize(13);
        btnEdit.setTypeface(null, Typeface.BOLD);
        btnEdit.setPadding(dp(8), dp(8), dp(8), dp(8));
        btnEdit.setBackgroundResource(R.drawable.bg_chip_white);
        btnEdit.setOnClickListener(v -> {
            if ("edit_deposit".equals(editAction)) {
                showAddDepositDialog(itemData);
            } else if ("edit_fee".equals(editAction)) {
                showAddFeeDialog(itemData);
            } else if ("edit_utility".equals(editAction)) {
                showAddUtilityDialog(itemData);
            }
        });
        card.addView(btnEdit);

        if (allowDelete) {
            TextView btnDelete = new TextView(this);
            btnDelete.setText("Delete");
            btnDelete.setTextColor(Color.parseColor("#D32F2F"));
            btnDelete.setTextSize(13);
            btnDelete.setTypeface(null, Typeface.BOLD);
            btnDelete.setPadding(dp(8), dp(8), dp(8), dp(8));
            btnDelete.setBackgroundResource(R.drawable.bg_chip_white);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            btnParams.setMarginStart(dp(8));
            btnDelete.setLayoutParams(btnParams);
            btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Delete " + title + "?")
                        .setMessage("Are you sure you want to delete this charge?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            deleteItem(deleteAction, itemData.optInt(idKey, 0));
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
            card.addView(btnDelete);
        }

        return card;
    }

    private TextView createEmptyState(String text) {
        TextView empty = new TextView(this);
        empty.setText(text);
        empty.setTextColor(Color.parseColor("#9A9A9E"));
        empty.setTextSize(13);
        empty.setPadding(0, dp(12), 0, dp(12));
        empty.setGravity(Gravity.CENTER);
        return empty;
    }

    private void showAddDepositDialog(JSONObject existing) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_deposit, null);
        dialog.setContentView(view);

        TextView title = view.findViewById(R.id.dialogDepositTitle);
        Spinner nameSpinner = view.findViewById(R.id.spinnerDepositName);
        EditText customNameInput = view.findViewById(R.id.inputCustomDepositName);
        EditText valueInput = view.findViewById(R.id.inputDepositValue);
        EditText descInput = view.findViewById(R.id.inputDepositDescription);

        if (title != null) title.setText(existing != null ? "Edit Required Deposit" : "Add Required Deposit");

        String[] depositNames = {"Advance Payment", "Security Deposit", "Reservation Deposit", "Utility Deposit", "Cleaning Deposit", "Custom Deposit"};
        ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, depositNames);
        if (nameSpinner != null) nameSpinner.setAdapter(nameAdapter);

        if (nameSpinner != null) {
            nameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                    if (customNameInput != null) {
                        customNameInput.setVisibility(position == 5 ? View.VISIBLE : View.GONE);
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        if (existing != null) {
            String existingName = existing.optString("deposit_name", "");
            double amt = existing.optDouble("amount", 0);
            String desc = existing.optString("description", "");

            int foundIdx = -1;
            for (int i = 0; i < depositNames.length - 1; i++) {
                if (depositNames[i].equalsIgnoreCase(existingName)) {
                    foundIdx = i;
                    break;
                }
            }
            if (foundIdx >= 0 && nameSpinner != null) {
                nameSpinner.setSelection(foundIdx);
            } else if (nameSpinner != null) {
                nameSpinner.setSelection(5);
                if (customNameInput != null) {
                    customNameInput.setVisibility(View.VISIBLE);
                    customNameInput.setText(existingName);
                }
            }
            if (valueInput != null) valueInput.setText(amt > 0 ? String.valueOf(amt) : "");
            if (descInput != null) descInput.setText(desc);
        }

        View btnCancel = view.findViewById(R.id.btnCancelDeposit);
        View btnSave = view.findViewById(R.id.btnSaveDeposit);

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String selectedName = nameSpinner != null ? nameSpinner.getSelectedItem().toString() : "Deposit";
                if ("Custom Deposit".equals(selectedName) && customNameInput != null) {
                    selectedName = customNameInput.getText().toString().trim();
                }
                String valStr = valueInput != null ? valueInput.getText().toString().trim() : "0";
                String desc = descInput != null ? descInput.getText().toString().trim() : "";

                double amt = valStr.isEmpty() ? 0.0 : Double.parseDouble(valStr);

                // Prevent duplicate Advance Payment or Security Deposit
                int targetId = existing != null ? existing.optInt("deposit_id", 0) : 0;
                if (targetId == 0 && cachedDeposits != null) {
                    for (int i = 0; i < cachedDeposits.length(); i++) {
                        JSONObject d = cachedDeposits.optJSONObject(i);
                        if (d != null && selectedName.equalsIgnoreCase(d.optString("deposit_name", ""))) {
                            targetId = d.optInt("deposit_id", 0);
                            break;
                        }
                    }
                }

                saveCharge("add_deposit", targetId, "deposit_id", selectedName, "FIXED", amt, desc);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void showAddFeeDialog(JSONObject existing) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_fee, null);
        dialog.setContentView(view);

        TextView title = view.findViewById(R.id.dialogFeeTitle);
        Spinner typeSpinner = view.findViewById(R.id.spinnerFeeType);
        View nameContainer = view.findViewById(R.id.layoutFeeNameContainer);
        TextView nameLabel = view.findViewById(R.id.labelFeeName);
        EditText nameInput = view.findViewById(R.id.inputFeeName);
        TextView amountLabel = view.findViewById(R.id.labelFeeAmount);
        EditText amountInput = view.findViewById(R.id.inputFeeAmount);
        TextView descLabel = view.findViewById(R.id.labelFeeDescription);
        EditText descInput = view.findViewById(R.id.inputFeeDescription);

        TextView previewName = view.findViewById(R.id.previewFeeName);
        TextView previewAmount = view.findViewById(R.id.previewFeeAmount);
        TextView previewDetails = view.findViewById(R.id.previewFeeDetails);

        TextView btnCancel = view.findViewById(R.id.btnCancelFee);
        TextView btnSave = view.findViewById(R.id.btnSaveFee);

        if (title != null) title.setText(existing != null ? "Edit Fee" : "+ Add Fee");
        if (btnSave != null) btnSave.setText(existing != null ? "Save Changes" : "Add Fee");

        String[] feeTypes = {
            "Advance Payment",
            "Security Deposit",
            "Reservation Fee",
            "Utility Deposit",
            "Cleaning Deposit",
            "One-Time Fee",
            "Monthly Recurring Fee"
        };

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, feeTypes);
        if (typeSpinner != null) typeSpinner.setAdapter(typeAdapter);

        // Prefill existing values
        if (existing != null) {
            String existingName = existing.optString("fee_name", "");
            String existingType = existing.optString("fee_type", "ONE_TIME");
            double existingAmt = existing.optDouble("amount", 0.0);
            String existingDesc = existing.optString("description", "");

            if (amountInput != null) amountInput.setText(existingAmt > 0 ? String.valueOf(existingAmt) : "");
            if (descInput != null) descInput.setText(existingDesc);

            int matchedIndex = -1;
            for (int i = 0; i < feeTypes.length - 2; i++) {
                if (feeTypes[i].equalsIgnoreCase(existingName)) {
                    matchedIndex = i;
                    break;
                }
            }

            if (matchedIndex >= 0 && typeSpinner != null) {
                typeSpinner.setSelection(matchedIndex);
            } else if (typeSpinner != null) {
                typeSpinner.setSelection("MONTHLY".equalsIgnoreCase(existingType) ? 6 : 5);
                if (nameInput != null) nameInput.setText(existingName);
            }
        }

        // Live Preview Updater
        Runnable updateLivePreview = () -> {
            String selectedType = typeSpinner != null ? typeSpinner.getSelectedItem().toString() : "One-Time Fee";
            boolean isCustomNameType = "One-Time Fee".equalsIgnoreCase(selectedType) || "Monthly Recurring Fee".equalsIgnoreCase(selectedType);

            if (nameContainer != null) {
                nameContainer.setVisibility(isCustomNameType ? View.VISIBLE : View.GONE);
            }

            if (descLabel != null) {
                descLabel.setText(isCustomNameType ? "Description (Optional)" : "Payment Details (Optional)");
            }
            if (descInput != null) {
                descInput.setHint(isCustomNameType ? "e.g. Monthly Wi-Fi subscription" : "e.g. Refundable after move-out");
            }

            String finalName = isCustomNameType ? (nameInput != null ? nameInput.getText().toString().trim() : "") : selectedType;
            if (finalName.isEmpty()) finalName = selectedType;

            String amtStr = amountInput != null ? amountInput.getText().toString().trim() : "0";
            double amtVal = amtStr.isEmpty() ? 0.0 : Double.parseDouble(amtStr);

            String userDesc = descInput != null ? descInput.getText().toString().trim() : "";

            if (previewName != null) previewName.setText(finalName);
            if (previewAmount != null) {
                previewAmount.setText(formatMoney(amtVal) + ("Monthly Recurring Fee".equalsIgnoreCase(selectedType) ? " / month" : ""));
            }
            if (previewDetails != null) {
                if (!userDesc.isEmpty() && !"null".equalsIgnoreCase(userDesc)) {
                    previewDetails.setText(userDesc);
                } else if ("Monthly Recurring Fee".equalsIgnoreCase(selectedType)) {
                    previewDetails.setText("Billed monthly after move-in.");
                } else {
                    previewDetails.setText("Collected during move-in only.");
                }
            }
        };

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLivePreview.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        if (nameInput != null) nameInput.addTextChangedListener(watcher);
        if (amountInput != null) amountInput.addTextChangedListener(watcher);
        if (descInput != null) descInput.addTextChangedListener(watcher);

        if (typeSpinner != null) {
            typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                    updateLivePreview.run();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        updateLivePreview.run();

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                String selectedType = typeSpinner != null ? typeSpinner.getSelectedItem().toString() : "One-Time Fee";
                boolean isCustomNameType = "One-Time Fee".equalsIgnoreCase(selectedType) || "Monthly Recurring Fee".equalsIgnoreCase(selectedType);

                String name = isCustomNameType ? (nameInput != null ? nameInput.getText().toString().trim() : "") : selectedType;
                if (name.isEmpty()) name = selectedType;

                String canonicalType = "Monthly Recurring Fee".equalsIgnoreCase(selectedType) ? "MONTHLY" : "ONE_TIME";
                String amtStr = amountInput != null ? amountInput.getText().toString().trim() : "0";
                String desc = descInput != null ? descInput.getText().toString().trim() : "";

                if (amtStr.isEmpty()) {
                    Toast.makeText(this, "Please enter amount.", Toast.LENGTH_SHORT).show();
                    return;
                }

                saveFee("add_fee", existing != null ? existing.optInt("fee_id", 0) : 0, name, canonicalType, Double.parseDouble(amtStr), desc);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void showAddUtilityDialog(JSONObject existing) {
        if (existing == null) return;

        int utilityId = existing.optInt("utility_id", 0);
        String name = existing.optString("utility_name", "Utility");
        String currentMethod = existing.optString("charging_method", "FREE");
        double currentAmount = existing.optDouble("fixed_amount", 0.0);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(16));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(String.format(Locale.US, "Edit %s", name));
        tvTitle.setTextColor(Color.parseColor("#1A1A1A"));
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, Typeface.BOLD);
        layout.addView(tvTitle);

        SwitchCompat toggleFree = new SwitchCompat(this);
        toggleFree.setText(String.format(Locale.US, "Free %s", name));
        toggleFree.setChecked("FREE".equalsIgnoreCase(currentMethod));
        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        toggleLp.topMargin = dp(12);
        toggleFree.setLayoutParams(toggleLp);
        layout.addView(toggleFree);

        TextView labelRate = new TextView(this);
        labelRate.setText(String.format(Locale.US, "%s Rate / Amount (₱)", name));
        labelRate.setTextColor(Color.parseColor("#6E6E73"));
        labelRate.setTextSize(12);
        labelRate.setLayoutParams(toggleLp);
        layout.addView(labelRate);

        EditText inputRate = new EditText(this);
        inputRate.setHint("0.00");
        inputRate.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputRate.setText(currentAmount > 0 ? String.valueOf(currentAmount) : "");
        inputRate.setEnabled(!toggleFree.isChecked());
        inputRate.setAlpha(toggleFree.isChecked() ? 0.4f : 1.0f);
        inputRate.setLayoutParams(toggleLp);
        layout.addView(inputRate);

        toggleFree.setOnCheckedChangeListener((buttonView, isChecked) -> {
            inputRate.setEnabled(!isChecked);
            inputRate.setAlpha(isChecked ? 0.4f : 1.0f);
        });

        new AlertDialog.Builder(this)
                .setTitle("Configure " + name)
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    boolean isFree = toggleFree.isChecked();
                    String method = isFree ? "FREE" : ("Electricity".equalsIgnoreCase(name) ? "CONSUMPTION_BASED" : "FIXED_MONTHLY");
                    String rateStr = inputRate.getText().toString().trim();
                    double rate = rateStr.isEmpty() ? 0.0 : Double.parseDouble(rateStr);

                    saveUtility("edit_utility", utilityId, name, method, rate);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStudentBillingPreviewDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_billing_preview, null);
        dialog.setContentView(view);

        LinearLayout container = view.findViewById(R.id.previewContentContainer);
        TextView tvTotalInitial = view.findViewById(R.id.textPreviewTotalInitial);

        renderBillingPreviewContent(dialog, view, container, tvTotalInitial);

        View btnClose = view.findViewById(R.id.btnClosePreview);
        View btnEdit = view.findViewById(R.id.btnEditChargesFromPreview);

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnEdit != null) btnEdit.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void renderBillingPreviewContent(BottomSheetDialog dialog, View view, LinearLayout container, TextView tvTotalInitial) {
        if (container == null) return;
        container.removeAllViews();

        double baseRent = 0.00;
        String roomNameStr = "Selected Room";

        if (cachedRooms != null && cachedRooms.length() > 0) {
            if (selectedRoomIndex < 0 || selectedRoomIndex >= cachedRooms.length()) {
                selectedRoomIndex = 0;
            }
            JSONObject currentRoom = cachedRooms.optJSONObject(selectedRoomIndex);
            if (currentRoom != null) {
                baseRent = currentRoom.optDouble("monthly_rent", 0.0);
                roomNameStr = "Room " + currentRoom.optString("room_number", "1");
            }

            if (cachedRooms.length() > 1) {
                TextView roomSelectLabel = new TextView(this);
                roomSelectLabel.setText("Select Room for Preview:");
                roomSelectLabel.setTextColor(Color.parseColor("#6E6E73"));
                roomSelectLabel.setTextSize(12);
                roomSelectLabel.setTypeface(null, Typeface.BOLD);
                container.addView(roomSelectLabel);

                Spinner roomSpinner = new Spinner(this);
                String[] roomOptions = new String[cachedRooms.length()];
                for (int i = 0; i < cachedRooms.length(); i++) {
                    JSONObject rm = cachedRooms.optJSONObject(i);
                    String rNum = rm != null ? rm.optString("room_number", "") : "";
                    double rRent = rm != null ? rm.optDouble("monthly_rent", 0.0) : 0.0;
                    roomOptions[i] = "Room " + rNum + " (" + formatMoney(rRent) + "/mo)";
                }

                ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roomOptions);
                roomSpinner.setAdapter(roomAdapter);
                roomSpinner.setSelection(selectedRoomIndex);

                LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                spLp.topMargin = dp(4);
                spLp.bottomMargin = dp(12);
                roomSpinner.setLayoutParams(spLp);

                roomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                        if (selectedRoomIndex != position) {
                            selectedRoomIndex = position;
                            renderBillingPreviewContent(dialog, view, container, tvTotalInitial);
                        }
                    }
                    @Override public void onNothingSelected(AdapterView<?> parent) {}
                });

                container.addView(roomSpinner);
            }
        }

        double totalInitial = baseRent;

        container.addView(createPreviewSectionHeader("Monthly Rent"));
        container.addView(createPreviewRow("Monthly Base Rent (" + roomNameStr + ")", formatMoney(baseRent)));

        if (cachedDeposits != null && cachedDeposits.length() > 0) {
            container.addView(createPreviewSectionHeader("Required Deposits"));
            for (int i = 0; i < cachedDeposits.length(); i++) {
                JSONObject d = cachedDeposits.optJSONObject(i);
                if (d != null) {
                    String name = d.optString("deposit_name", "Deposit");
                    double amt = d.optDouble("amount", 0);
                    String desc = d.optString("description", "");
                    boolean hasDesc = !desc.trim().isEmpty() && !"null".equalsIgnoreCase(desc.trim());
                    totalInitial += amt;
                    container.addView(createPreviewRow(name + (hasDesc ? " (" + desc.trim() + ")" : ""), formatMoney(amt)));
                }
            }
        }

        if (cachedFees != null && cachedFees.length() > 0) {
            boolean addedOneTimeHeader = false;
            boolean addedMonthlyHeader = false;

            for (int i = 0; i < cachedFees.length(); i++) {
                JSONObject f = cachedFees.optJSONObject(i);
                if (f != null) {
                    String fType = f.optString("fee_type", "ONE_TIME");
                    String name = f.optString("fee_name", "Fee");
                    double amt = f.optDouble("amount", 0);

                    if ("ONE_TIME".equalsIgnoreCase(fType)) {
                        if (!addedOneTimeHeader) {
                            container.addView(createPreviewSectionHeader("One-Time Fees (Included in Initial Payment)"));
                            addedOneTimeHeader = true;
                        }
                        totalInitial += amt;
                        container.addView(createPreviewRow(name, formatMoney(amt)));
                    }
                }
            }

            for (int i = 0; i < cachedFees.length(); i++) {
                JSONObject f = cachedFees.optJSONObject(i);
                if (f != null) {
                    String fType = f.optString("fee_type", "ONE_TIME");
                    String name = f.optString("fee_name", "Fee");
                    double amt = f.optDouble("amount", 0);

                    if ("MONTHLY".equalsIgnoreCase(fType)) {
                        if (!addedMonthlyHeader) {
                            container.addView(createPreviewSectionHeader("Monthly Recurring Fees (Billed After Move-in)"));
                            addedMonthlyHeader = true;
                        }
                        container.addView(createPreviewRow(name, formatMoney(amt) + "/month"));
                    }
                }
            }
        }

        if (cachedUtilities != null && cachedUtilities.length() > 0) {
            container.addView(createPreviewSectionHeader("Utilities"));
            for (int i = 0; i < cachedUtilities.length(); i++) {
                JSONObject u = cachedUtilities.optJSONObject(i);
                if (u != null) {
                    String name = u.optString("utility_name", "Utility");
                    String method = u.optString("charging_method", "FREE");
                    double amt = u.optDouble("fixed_amount", 0);

                    String val = "FREE".equalsIgnoreCase(method) ? "Free" : ("CONSUMPTION_BASED".equalsIgnoreCase(method) ? "Meter Reading" : formatMoney(amt) + "/month");
                    container.addView(createPreviewRow(name, val));
                }
            }
        }

        if (tvTotalInitial != null) {
            tvTotalInitial.setText(formatMoney(totalInitial));
        }
    }

    private TextView createPreviewSectionHeader(String title) {
        TextView header = new TextView(this);
        header.setText(title);
        header.setTextColor(Color.parseColor("#1B5E4C"));
        header.setTextSize(13);
        header.setTypeface(null, Typeface.BOLD);
        header.setPadding(0, dp(12), 0, dp(4));
        return header;
    }

    private View createPreviewRow(String name, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        tvName.setText(name);
        tvName.setTextColor(Color.parseColor("#1A1A1A"));
        tvName.setTextSize(14);

        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextColor(Color.parseColor("#6E6E73"));
        tvVal.setTextSize(14);

        row.addView(tvName);
        row.addView(tvVal);
        return row;
    }

    private void saveCharge(String action, int id, String idKey, String name, String method, double amount, String desc) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("action", action);
            payload.put("house_id", houseId);
            if (id > 0) payload.put(idKey, id);
            payload.put("deposit_name", name);
            payload.put("charging_method", method);
            payload.put("amount", amount);
            payload.put("description", desc);

            sendPostCharge(payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveFee(String action, int id, String name, String feeType, double amount, String desc) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("action", action);
            payload.put("house_id", houseId);
            if (id > 0) payload.put("fee_id", id);
            payload.put("fee_name", name);
            payload.put("fee_type", feeType);
            payload.put("amount", amount);
            payload.put("description", desc);

            sendPostCharge(payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveUtility(String action, int id, String name, String method, double amount) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("action", action);
            payload.put("house_id", houseId);
            if (id > 0) payload.put("utility_id", id);
            payload.put("utility_name", name);
            payload.put("charging_method", method);
            payload.put("amount", amount);

            sendPostCharge(payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPostCharge(JSONObject payload) {
        RequestBody body = RequestBody.create(payload.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("http://10.209.52.109/Dormigo_Backend/api/property_charges.php")
                .post(body)
                .build();

        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(LandlordRentalChargesActivity.this, "Failed to save charge.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> loadCharges());
            }
        });
    }

    private void deleteItem(String action, int itemId) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("action", action);
            payload.put("house_id", houseId);
            payload.put("item_id", itemId);

            RequestBody body = RequestBody.create(payload.toString(), MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("http://10.209.52.109/Dormigo_Backend/api/property_charges.php")
                    .delete(body)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(LandlordRentalChargesActivity.this, "Failed to delete.", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    runOnUiThread(() -> loadCharges());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatMoney(double amount) {
        return "₱" + String.format(Locale.US, "%,.2f", amount);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
