package vhu.edu.vn.ui.technician;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import vhu.edu.vn.R;
import vhu.edu.vn.data.local.entity.InventoryPartEntity;
import vhu.edu.vn.data.local.entity.TicketPartDetailEntity;
import vhu.edu.vn.data.local.model.TicketWithDetails;
import vhu.edu.vn.data.repository.InventoryRepository;
import vhu.edu.vn.data.repository.RepairTicketRepository;
import vhu.edu.vn.databinding.ActivityDiagnoseBinding;
import vhu.edu.vn.utils.Constants;
import vhu.edu.vn.utils.SessionManager;
import vhu.edu.vn.utils.UiFormatters;

public class DiagnoseActivity extends AppCompatActivity {
    private static final class SelectedPartLine {
        final int partId;
        final String partName;
        final double unitPrice;
        int quantity;

        SelectedPartLine(int partId, String partName, double unitPrice, int quantity) {
            this.partId = partId;
            this.partName = partName;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }
    }

    private ActivityDiagnoseBinding binding;
    private RepairTicketRepository ticketRepository;
    private InventoryRepository inventoryRepository;
    private SessionManager sessionManager;

    private int ticketId;
    private TicketWithDetails currentTicket;
    private List<InventoryPartEntity> inventoryParts = Collections.emptyList();
    private InventoryPartEntity selectedDropdownPart;
    private int pickerQuantity = 1;
    private final List<SelectedPartLine> selectedParts = new ArrayList<>();
    private long hydratedVersion = -1L;
    private boolean actionPending;
    private boolean quoteEditable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityDiagnoseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.diagnoseLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn() || !sessionManager.isStaff()) {
            finish();
            return;
        }

        ticketId = getIntent().getIntExtra("ticket_id", -1);
        if (ticketId <= 0) {
            finish();
            return;
        }

        ticketRepository = new RepairTicketRepository(this);
        inventoryRepository = new InventoryRepository(this);

        setupInputs();

        inventoryRepository.getAllParts().observe(this, parts -> {
            inventoryParts = parts != null ? parts : Collections.emptyList();
            setupPartDropdown();
        });

        ticketRepository.getTicketById(ticketId).observe(this, item -> {
            if (item == null || item.ticket == null) {
                binding.tvTicketSummary.setText(R.string.ticket_not_found);
                binding.btnWorkflowTransition.setVisibility(View.GONE);
                binding.btnSubmitDiagnosis.setVisibility(View.GONE);
                return;
            }
            currentTicket = item;
            bindTicket(item);
        });
    }

    private void setupInputs() {
        binding.edtTechnicianNote.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilTechnicianNote.setError(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        binding.edtLaborFee.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilLaborFee.setError(null);
                updateCostEstimate();
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        binding.btnQtyMinus.setOnClickListener(v -> {
            if (pickerQuantity > 1) {
                pickerQuantity--;
                binding.tvPartQtyValue.setText(String.valueOf(pickerQuantity));
            }
        });

        binding.btnQtyPlus.setOnClickListener(v -> {
            int maxStock = selectedDropdownPart != null ? selectedDropdownPart.getQuantity() : 99;
            if (maxStock <= 0) {
                Snackbar.make(binding.diagnoseLayout, "Linh kiện này hiện đã hết hàng trong kho.", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (pickerQuantity < maxStock) {
                pickerQuantity++;
                binding.tvPartQtyValue.setText(String.valueOf(pickerQuantity));
            } else {
                Snackbar.make(binding.diagnoseLayout, "Đã đạt tối đa tồn kho (" + maxStock + ").", Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.btnAddPart.setOnClickListener(v -> addSelectedPartToQuote());
        binding.btnSubmitDiagnosis.setOnClickListener(v -> submitDiagnosis());
        binding.btnWorkflowTransition.setOnClickListener(v -> performWorkflowTransition());
    }

    private void setupPartDropdown() {
        List<String> labels = new ArrayList<>();
        for (InventoryPartEntity part : inventoryParts) {
            labels.add(part.getPartName() + " · Tồn: " + part.getQuantity()
                    + " · " + UiFormatters.formatCurrency(part.getSellingPrice()));
        }
        ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                labels
        );
        binding.dropdownParts.setAdapter(dropdownAdapter);
        binding.dropdownParts.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < inventoryParts.size()) {
                selectedDropdownPart = inventoryParts.get(position);
                pickerQuantity = 1;
                binding.tvPartQtyValue.setText("1");
                updateSelectedPartStockLabel();
            }
        });

        if (selectedDropdownPart == null && !inventoryParts.isEmpty()) {
            selectedDropdownPart = inventoryParts.get(0);
            binding.dropdownParts.setText(labels.get(0), false);
            updateSelectedPartStockLabel();
        } else if (selectedDropdownPart != null) {
            for (int i = 0; i < inventoryParts.size(); i++) {
                if (inventoryParts.get(i).getId() == selectedDropdownPart.getId()) {
                    selectedDropdownPart = inventoryParts.get(i);
                    binding.dropdownParts.setText(labels.get(i), false);
                    updateSelectedPartStockLabel();
                    break;
                }
            }
        }
    }

    private void updateSelectedPartStockLabel() {
        if (selectedDropdownPart == null) {
            binding.tvSelectedPartStock.setText("");
            return;
        }
        int stock = selectedDropdownPart.getQuantity();
        if (stock <= 0) {
            binding.tvSelectedPartStock.setText("Hết hàng trong kho · Không thể thêm");
            binding.tvSelectedPartStock.setTextColor(ContextCompat.getColor(this, R.color.error));
        } else if (stock <= selectedDropdownPart.getMinThreshold()) {
            binding.tvSelectedPartStock.setText("Còn " + stock + " cái (sắp hết) · Đơn giá "
                    + UiFormatters.formatCurrency(selectedDropdownPart.getSellingPrice()));
            binding.tvSelectedPartStock.setTextColor(ContextCompat.getColor(this, R.color.warning));
        } else {
            binding.tvSelectedPartStock.setText("Tồn kho sẵn có: " + stock + " · Đơn giá "
                    + UiFormatters.formatCurrency(selectedDropdownPart.getSellingPrice()));
            binding.tvSelectedPartStock.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
        }
    }

    private void bindTicket(TicketWithDetails item) {
        String status = item.ticket.getStatus();
        UiFormatters.StatusStyle style = UiFormatters.statusStyle(status);

        binding.tvTicketCodeHeader.setText(item.ticket.getTicketCode());
        binding.tvTicketSummary.setText(item.device != null ? item.device.getBrandModel() : getString(R.string.unknown_device));

        int textColor = ContextCompat.getColor(this, style.textColor);
        int bgColor = ContextCompat.getColor(this, style.bgColor);
        binding.tvStatusBadge.setText(style.label);
        binding.tvStatusBadge.setTextColor(textColor);
        binding.imgStatusIcon.setImageResource(style.iconRes);
        binding.imgStatusIcon.setColorFilter(textColor);
        binding.badgeStatusContainer.getBackground().mutate();
        binding.badgeStatusContainer.getBackground().setTint(bgColor);

        StringBuilder meta = new StringBuilder();
        if (item.device != null) {
            meta.append(UiFormatters.deviceTypeLabel(item.device.getDeviceType()))
                    .append(" · IMEI/SN: ").append(item.device.getImeiSerial());
            String pass = item.device.getScreenPassword();
            if (pass != null && !pass.trim().isEmpty()) {
                meta.append("\nMật khẩu màn hình: ").append(pass.trim());
            }
            String cond = item.device.getInitialCondition();
            if (cond != null && !cond.trim().isEmpty()) {
                meta.append("\nNgoại quan tiếp nhận: ").append(cond.trim());
            }
        }
        if (item.customer != null) {
            meta.append("\nKhách hàng: ").append(item.customer.getFullName())
                    .append(" · ").append(item.customer.getPhone());
        }
        binding.tvDeviceMetadata.setText(meta.toString());

        String fault = item.ticket.getCustomerNote();
        binding.tvCustomerFault.setText("Lỗi khách phản ánh: " + (fault != null && !fault.trim().isEmpty() ? fault.trim() : "Chưa ghi nhận"));

        // Workflow button
        if (Constants.TICKET_RECEIVED.equals(status)) {
            binding.btnWorkflowTransition.setVisibility(View.VISIBLE);
            binding.btnWorkflowTransition.setText("Bắt đầu kiểm tra (Chẩn đoán)");
        } else if (Constants.TICKET_APPROVED.equals(status)) {
            binding.btnWorkflowTransition.setVisibility(View.VISIBLE);
            binding.btnWorkflowTransition.setText("Bắt đầu sửa chữa");
        } else if (Constants.TICKET_REPAIRING.equals(status)) {
            binding.btnWorkflowTransition.setVisibility(View.VISIBLE);
            binding.btnWorkflowTransition.setText("Hoàn tất sửa chữa");
        } else {
            binding.btnWorkflowTransition.setVisibility(View.GONE);
        }

        // Lock quotation editing once customer approved or workflow moved past quote stage
        quoteEditable = Constants.TICKET_RECEIVED.equals(status)
                || Constants.TICKET_DIAGNOSING.equals(status)
                || Constants.TICKET_WAITING_APPROVAL.equals(status);

        binding.layoutPartPicker.setVisibility(quoteEditable ? View.VISIBLE : View.GONE);
        binding.btnSubmitDiagnosis.setVisibility(quoteEditable ? View.VISIBLE : View.GONE);
        binding.edtTechnicianNote.setEnabled(quoteEditable);
        binding.edtLaborFee.setEnabled(quoteEditable);

        if (!quoteEditable) {
            binding.cardQuoteLockBanner.setVisibility(View.VISIBLE);
            binding.tvQuoteLockReason.setText("Báo giá đang ở trạng thái khóa (" + style.label
                    + ") để đảm bảo khớp linh kiện đã giữ kho và số tiền khách hàng đã xác nhận.");
        } else {
            binding.cardQuoteLockBanner.setVisibility(View.GONE);
            binding.btnSubmitDiagnosis.setText(Constants.TICKET_WAITING_APPROVAL.equals(status)
                    ? "Cập nhật báo giá gửi khách"
                    : "Gửi báo giá cho khách hàng");
        }

        long versionKey = item.ticket.getUpdatedAt();
        if (hydratedVersion != versionKey) {
            hydratedVersion = versionKey;
            hydrateFormFromTicket(item);
        }
        updateButtonsEnabled();
    }

    private void hydrateFormFromTicket(TicketWithDetails item) {
        String note = item.ticket.getTechnicianNote();
        binding.edtTechnicianNote.setText(note != null ? note : "");

        double labor = item.ticket.getLaborFee();
        binding.edtLaborFee.setText(labor > 0 ? String.valueOf((long) labor) : "0");

        selectedParts.clear();
        Map<Integer, InventoryPartEntity> partMap = new HashMap<>();
        if (item.parts != null) {
            for (InventoryPartEntity p : item.parts) {
                partMap.put(p.getId(), p);
            }
        }
        if (item.partDetails != null) {
            for (TicketPartDetailEntity detail : item.partDetails) {
                InventoryPartEntity p = partMap.get(detail.getPartId());
                String name = p != null ? p.getPartName() : ("Linh kiện #" + detail.getPartId());
                selectedParts.add(new SelectedPartLine(detail.getPartId(), name, detail.getUnitPrice(), detail.getQuantity()));
            }
        }
        renderSelectedParts();
        updateCostEstimate();
    }

    private void addSelectedPartToQuote() {
        if (!quoteEditable || selectedDropdownPart == null) return;
        int stock = selectedDropdownPart.getQuantity();
        if (stock <= 0) {
            Snackbar.make(binding.diagnoseLayout, "Linh kiện đã chọn hiện hết hàng trong kho.", Snackbar.LENGTH_SHORT).show();
            return;
        }

        SelectedPartLine existing = null;
        for (SelectedPartLine line : selectedParts) {
            if (line.partId == selectedDropdownPart.getId()) {
                existing = line;
                break;
            }
        }

        int currentQty = existing != null ? existing.quantity : 0;
        int nextQty = currentQty + pickerQuantity;
        if (nextQty > stock) {
            Snackbar.make(binding.diagnoseLayout,
                    "Tổng số lượng (" + nextQty + ") vượt quá tồn kho (" + stock + ").",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        if (existing != null) {
            existing.quantity = nextQty;
        } else {
            selectedParts.add(new SelectedPartLine(
                    selectedDropdownPart.getId(),
                    selectedDropdownPart.getPartName(),
                    selectedDropdownPart.getSellingPrice(),
                    pickerQuantity
            ));
        }

        pickerQuantity = 1;
        binding.tvPartQtyValue.setText("1");
        renderSelectedParts();
        updateCostEstimate();
    }

    private void renderSelectedParts() {
        binding.containerSelectedParts.removeAllViews();
        binding.tvNoPartsSelected.setVisibility(selectedParts.isEmpty() ? View.VISIBLE : View.GONE);

        for (int i = 0; i < selectedParts.size(); i++) {
            final int index = i;
            SelectedPartLine line = selectedParts.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = dp(8);
            row.setLayoutParams(params);
            row.setPadding(dp(12), dp(8), dp(8), dp(8));
            row.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_variant));

            TextView tvInfo = new TextView(this);
            tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tvInfo.setText(line.partName + " (×" + line.quantity + ")\n"
                    + UiFormatters.formatCurrency(line.unitPrice * line.quantity));
            tvInfo.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
            tvInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            row.addView(tvInfo);

            if (quoteEditable) {
                ImageButton btnRemove = new ImageButton(this);
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dp(48), dp(48));
                btnRemove.setLayoutParams(btnParams);
                btnRemove.setBackground(null);
                btnRemove.setImageResource(R.drawable.ic_delete);
                btnRemove.setColorFilter(ContextCompat.getColor(this, R.color.error));
                btnRemove.setContentDescription("Xóa linh kiện " + line.partName);
                btnRemove.setOnClickListener(v -> {
                    selectedParts.remove(index);
                    renderSelectedParts();
                    updateCostEstimate();
                });
                row.addView(btnRemove);
            }

            binding.containerSelectedParts.addView(row);
        }
    }

    private void updateCostEstimate() {
        double partsTotal = 0.0;
        for (SelectedPartLine line : selectedParts) {
            partsTotal += line.unitPrice * line.quantity;
        }
        double labor = parseLaborFee();
        double validLabor = labor >= 0.0 ? labor : 0.0;
        double total = partsTotal + validLabor;

        binding.tvEstimateParts.setText("Tiền linh kiện: " + UiFormatters.formatCurrency(partsTotal));
        binding.tvEstimateLabor.setText("Tiền công: " + UiFormatters.formatCurrency(validLabor));
        binding.tvEstimateTotal.setText("Tổng báo giá dự kiến: " + UiFormatters.formatCurrency(total));
    }

    private double parseLaborFee() {
        String raw = binding.edtLaborFee.getText() != null ? binding.edtLaborFee.getText().toString().trim() : "";
        if (raw.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private void submitDiagnosis() {
        if (actionPending || !quoteEditable) return;

        String note = binding.edtTechnicianNote.getText() != null
                ? binding.edtTechnicianNote.getText().toString().trim()
                : "";
        if (note.isEmpty()) {
            binding.tilTechnicianNote.setError("Vui lòng nhập kết quả kiểm tra kỹ thuật");
            binding.edtTechnicianNote.requestFocus();
            return;
        }

        double labor = parseLaborFee();
        if (!Double.isFinite(labor) || labor < 0.0) {
            binding.tilLaborFee.setError("Tiền công không hợp lệ");
            binding.edtLaborFee.requestFocus();
            return;
        }

        List<TicketPartDetailEntity> details = new ArrayList<>();
        for (SelectedPartLine line : selectedParts) {
            details.add(new TicketPartDetailEntity(ticketId, line.partId, line.quantity, line.unitPrice));
        }

        actionPending = true;
        updateButtonsEnabled();

        ticketRepository.saveDiagnosisAndParts(
                ticketId,
                sessionManager.getUserId(),
                labor,
                note,
                details,
                new RepairTicketRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        actionPending = false;
                        updateButtonsEnabled();
                        Snackbar.make(binding.diagnoseLayout, "Đã lưu chẩn đoán và gửi báo giá cho khách.", Snackbar.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String message) {
                        actionPending = false;
                        updateButtonsEnabled();
                        new MaterialAlertDialogBuilder(DiagnoseActivity.this)
                                .setTitle("Không thể lưu báo giá")
                                .setMessage(message)
                                .setPositiveButton(R.string.action_close, null)
                                .show();
                    }
                }
        );
    }

    private void performWorkflowTransition() {
        if (actionPending || currentTicket == null || currentTicket.ticket == null) return;
        String current = currentTicket.ticket.getStatus();
        final String targetStatus;
        if (Constants.TICKET_RECEIVED.equals(current)) {
            targetStatus = Constants.TICKET_DIAGNOSING;
        } else if (Constants.TICKET_APPROVED.equals(current)) {
            targetStatus = Constants.TICKET_REPAIRING;
        } else if (Constants.TICKET_REPAIRING.equals(current)) {
            targetStatus = Constants.TICKET_COMPLETED;
        } else {
            return;
        }

        actionPending = true;
        updateButtonsEnabled();
        ticketRepository.transitionTicketStatus(
                ticketId,
                targetStatus,
                sessionManager.getUserId(),
                new RepairTicketRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        actionPending = false;
                        updateButtonsEnabled();
                        Snackbar.make(binding.diagnoseLayout,
                                "Đã cập nhật trạng thái: " + UiFormatters.statusStyle(targetStatus).label,
                                Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        actionPending = false;
                        updateButtonsEnabled();
                        Snackbar.make(binding.diagnoseLayout, message, Snackbar.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void updateButtonsEnabled() {
        binding.btnSubmitDiagnosis.setEnabled(!actionPending && quoteEditable);
        binding.btnWorkflowTransition.setEnabled(!actionPending);
        binding.btnAddPart.setEnabled(!actionPending && quoteEditable);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
