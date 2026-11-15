package vhu.edu.vn.ui.manager;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vhu.edu.vn.R;
import vhu.edu.vn.data.local.entity.InventoryPartEntity;
import vhu.edu.vn.data.repository.InventoryRepository;
import vhu.edu.vn.databinding.ActivityInventoryManagementBinding;
import vhu.edu.vn.databinding.DialogInventoryPartBinding;
import vhu.edu.vn.ui.adapter.InventoryAdapter;
import vhu.edu.vn.utils.Constants;
import vhu.edu.vn.utils.SessionManager;
import vhu.edu.vn.utils.TicketSearch;

public class InventoryManagementActivity extends AppCompatActivity {
    public static final String EXTRA_LOW_STOCK_ONLY = "extra_low_stock_only";

    private ActivityInventoryManagementBinding binding;
    private InventoryRepository repository;
    private InventoryAdapter adapter;
    private List<InventoryPartEntity> allParts = Collections.emptyList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityInventoryManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.inventoryLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn() || !Constants.ROLE_MANAGER.equals(sessionManager.getRole())) {
            finish();
            return;
        }

        repository = new InventoryRepository(this);
        adapter = new InventoryAdapter();
        adapter.setOnPartClickListener(this::showPartDialog);

        binding.rvInventory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvInventory.setAdapter(adapter);

        if (getIntent().getBooleanExtra(EXTRA_LOW_STOCK_ONLY, false)) {
            binding.chipPartsLowStock.setChecked(true);
        }

        binding.edtSearchParts.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        binding.chipInventoryFilter.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilter());
        binding.fabAddPart.setOnClickListener(v -> showPartDialog(null));

        repository.getAllParts().observe(this, parts -> {
            allParts = parts != null ? parts : Collections.emptyList();
            applyFilter();
        });
    }

    private void applyFilter() {
        String query = binding.edtSearchParts.getText() != null ? binding.edtSearchParts.getText().toString() : "";
        boolean lowStockOnly = binding.chipPartsLowStock.isChecked();

        List<InventoryPartEntity> filtered = new ArrayList<>();
        for (InventoryPartEntity part : allParts) {
            if (lowStockOnly && part.getQuantity() > part.getMinThreshold()) continue;
            if (TicketSearch.matches(query, part.getPartCode(), part.getPartName(), part.getCategory())) {
                filtered.add(part);
            }
        }

        adapter.setParts(filtered);
        binding.tvPartResultCount.setText("Hiển thị " + filtered.size() + " / " + allParts.size() + " linh kiện");
        binding.tvInventoryEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showPartDialog(InventoryPartEntity existingPart) {
        DialogInventoryPartBinding dialogBinding = DialogInventoryPartBinding.inflate(getLayoutInflater());
        boolean isEdit = existingPart != null;

        if (isEdit) {
            dialogBinding.edtPartCode.setText(existingPart.getPartCode());
            dialogBinding.edtPartName.setText(existingPart.getPartName());
            dialogBinding.edtPartCategory.setText(existingPart.getCategory());
            dialogBinding.edtPartQuantity.setText(String.valueOf(existingPart.getQuantity()));
            dialogBinding.edtPartThreshold.setText(String.valueOf(existingPart.getMinThreshold()));
            dialogBinding.edtImportPrice.setText(String.valueOf((long) existingPart.getImportPrice()));
            dialogBinding.edtSellingPrice.setText(String.valueOf((long) existingPart.getSellingPrice()));
        }

        TextWatcher clearErrors = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                dialogBinding.tilPartCode.setError(null);
                dialogBinding.tilPartName.setError(null);
                dialogBinding.tilPartQuantity.setError(null);
                dialogBinding.tilPartThreshold.setError(null);
                dialogBinding.tilImportPrice.setError(null);
                dialogBinding.tilSellingPrice.setError(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        };
        dialogBinding.edtPartCode.addTextChangedListener(clearErrors);
        dialogBinding.edtPartName.addTextChangedListener(clearErrors);
        dialogBinding.edtPartQuantity.addTextChangedListener(clearErrors);
        dialogBinding.edtPartThreshold.addTextChangedListener(clearErrors);
        dialogBinding.edtImportPrice.addTextChangedListener(clearErrors);
        dialogBinding.edtSellingPrice.addTextChangedListener(clearErrors);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(isEdit ? "Cập nhật linh kiện" : "Thêm linh kiện mới")
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(isEdit ? "Lưu thay đổi" : "Thêm mới", null);

        if (isEdit) {
            builder.setNeutralButton("Xóa", null);
        }

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveBtn.setOnClickListener(v -> handleSavePart(dialog, dialogBinding, existingPart));

            if (isEdit) {
                Button deleteBtn = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                deleteBtn.setTextColor(getColor(R.color.error));
                deleteBtn.setOnClickListener(v -> {
                    dialog.dismiss();
                    confirmDeletePart(existingPart);
                });
            }
        });
        dialog.show();
    }

    private void handleSavePart(
            AlertDialog dialog,
            DialogInventoryPartBinding db,
            InventoryPartEntity existingPart
    ) {
        String code = db.edtPartCode.getText() != null ? db.edtPartCode.getText().toString().trim() : "";
        String name = db.edtPartName.getText() != null ? db.edtPartName.getText().toString().trim() : "";
        String category = db.edtPartCategory.getText() != null ? db.edtPartCategory.getText().toString().trim() : "";
        String qtyRaw = db.edtPartQuantity.getText() != null ? db.edtPartQuantity.getText().toString().trim() : "";
        String thresholdRaw = db.edtPartThreshold.getText() != null ? db.edtPartThreshold.getText().toString().trim() : "";
        String importRaw = db.edtImportPrice.getText() != null ? db.edtImportPrice.getText().toString().trim() : "";
        String sellingRaw = db.edtSellingPrice.getText() != null ? db.edtSellingPrice.getText().toString().trim() : "";

        boolean valid = true;
        if (code.isEmpty()) {
            db.tilPartCode.setError("Nhập mã linh kiện");
            valid = false;
        }
        if (name.isEmpty()) {
            db.tilPartName.setError("Nhập tên linh kiện");
            valid = false;
        }
        int qty = parseIntOrInvalid(qtyRaw);
        if (qty < 0) {
            db.tilPartQuantity.setError("Số lượng >= 0");
            valid = false;
        }
        int threshold = thresholdRaw.isEmpty() ? Constants.DEFAULT_MIN_STOCK_THRESHOLD : parseIntOrInvalid(thresholdRaw);
        if (threshold < 0) {
            db.tilPartThreshold.setError("Ngưỡng >= 0");
            valid = false;
        }
        double importPrice = importRaw.isEmpty() ? 0.0 : parseDoubleOrInvalid(importRaw);
        if (importPrice < 0.0) {
            db.tilImportPrice.setError("Giá vốn >= 0");
            valid = false;
        }
        double sellingPrice = parseDoubleOrInvalid(sellingRaw);
        if (sellingPrice < 0.0) {
            db.tilSellingPrice.setError("Nhập giá bán hợp lệ");
            valid = false;
        }
        if (!valid) return;

        Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveBtn.setEnabled(false);

        InventoryRepository.ActionCallback callback = new InventoryRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                dialog.dismiss();
                Snackbar.make(binding.inventoryLayout,
                        existingPart == null ? "Đã thêm linh kiện mới." : "Đã cập nhật linh kiện.",
                        Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                positiveBtn.setEnabled(true);
                new MaterialAlertDialogBuilder(InventoryManagementActivity.this)
                        .setTitle("Không thể lưu linh kiện")
                        .setMessage(message)
                        .setPositiveButton(R.string.action_close, null)
                        .show();
            }
        };

        if (existingPart == null) {
            InventoryPartEntity newPart = new InventoryPartEntity(
                    code, name, category, qty, importPrice, sellingPrice, threshold
            );
            repository.insertPart(newPart, callback);
        } else {
            existingPart.setPartCode(code);
            existingPart.setPartName(name);
            existingPart.setCategory(category);
            existingPart.setQuantity(qty);
            existingPart.setMinThreshold(threshold);
            existingPart.setImportPrice(importPrice);
            existingPart.setSellingPrice(sellingPrice);
            repository.updatePart(existingPart, callback);
        }
    }

    private void confirmDeletePart(InventoryPartEntity part) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa linh kiện?")
                .setMessage("Bạn chắc chắn muốn xóa \"" + part.getPartName() + "\" (" + part.getPartCode() + ") khỏi kho?")
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Xóa linh kiện", (d, w) -> repository.deletePart(part, new InventoryRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        Snackbar.make(binding.inventoryLayout, "Đã xóa linh kiện khỏi kho.", Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        new MaterialAlertDialogBuilder(InventoryManagementActivity.this)
                                .setTitle("Không thể xóa linh kiện")
                                .setMessage(message)
                                .setPositiveButton(R.string.action_close, null)
                                .show();
                    }
                }))
                .show();
    }

    private int parseIntOrInvalid(String raw) {
        if (raw == null || raw.isEmpty()) return -1;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private double parseDoubleOrInvalid(String raw) {
        if (raw == null || raw.isEmpty()) return -1.0;
        try {
            double v = Double.parseDouble(raw);
            return Double.isFinite(v) ? v : -1.0;
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }
}
