package vhu.edu.vn.ui.customer;

import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.HashMap;
import java.util.Map;
import vhu.edu.vn.R;
import vhu.edu.vn.data.local.entity.InventoryPartEntity;
import vhu.edu.vn.data.local.entity.TicketPartDetailEntity;
import vhu.edu.vn.data.local.model.TicketWithDetails;
import vhu.edu.vn.data.repository.RepairTicketRepository;
import vhu.edu.vn.databinding.ActivityTicketTimelineBinding;
import vhu.edu.vn.utils.Constants;
import vhu.edu.vn.utils.QRCodeUtils;
import vhu.edu.vn.utils.RepairProgress;
import vhu.edu.vn.utils.UiFormatters;

public class TicketTimelineActivity extends AppCompatActivity {
    private ActivityTicketTimelineBinding binding;
    private RepairTicketRepository repository;
    private int ticketId;
    private TicketWithDetails currentTicket;
    private long displayedUpdatedAt = -1L;
    private boolean actionPending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityTicketTimelineBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.timelineLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        ticketId = getIntent().getIntExtra("ticket_id", -1);
        if (ticketId <= 0) {
            showMissingTicketState();
            return;
        }

        repository = new RepairTicketRepository(this);
        repository.getTicketById(ticketId).observe(this, item -> {
            binding.progressLoading.setVisibility(View.GONE);
            if (item == null || item.ticket == null) {
                showMissingTicketState();
                return;
            }
            currentTicket = item;
            displayedUpdatedAt = item.ticket.getUpdatedAt();
            bindTicket(item);
        });

        binding.btnApprove.setOnClickListener(v -> confirmApprove());
        binding.btnReject.setOnClickListener(v -> confirmReject());
    }

    private void showMissingTicketState() {
        binding.progressLoading.setVisibility(View.GONE);
        binding.cardApprovalBox.setVisibility(View.GONE);
        binding.tvTicketCode.setText("");
        binding.tvDeviceName.setText(R.string.ticket_not_found);
        binding.tvTimelineState.setText(R.string.ticket_not_found);
    }

    private void bindTicket(TicketWithDetails item) {
        String status = item.ticket.getStatus();
        UiFormatters.StatusStyle style = UiFormatters.statusStyle(status);

        binding.tvTicketCode.setText(item.ticket.getTicketCode());
        if (item.device != null) {
            binding.tvDeviceName.setText(item.device.getBrandModel());
            binding.imgDeviceIcon.setImageResource(UiFormatters.deviceTypeIcon(item.device.getDeviceType()));
            binding.tvDeviceImei.setText(UiFormatters.deviceTypeLabel(item.device.getDeviceType())
                    + " · IMEI/SN: " + item.device.getImeiSerial());
        } else {
            binding.tvDeviceName.setText(R.string.unknown_device);
            binding.imgDeviceIcon.setImageResource(R.drawable.ic_device_phone);
            binding.tvDeviceImei.setText("");
        }

        int badgeTextColor = ContextCompat.getColor(this, style.textColor);
        int badgeBgColor = ContextCompat.getColor(this, style.bgColor);
        binding.tvStatusBadge.setText(style.label);
        binding.tvStatusBadge.setTextColor(badgeTextColor);
        binding.imgStatusIcon.setImageResource(style.iconRes);
        binding.imgStatusIcon.setColorFilter(badgeTextColor);
        binding.badgeStatusContainer.getBackground().mutate();
        binding.badgeStatusContainer.getBackground().setTint(badgeBgColor);

        long lastTime = item.ticket.getUpdatedAt() > 0 ? item.ticket.getUpdatedAt() : item.ticket.getCreatedAt();
        binding.tvLastUpdated.setText(getString(R.string.ticket_updated, UiFormatters.formatDateTime(lastTime)));

        // Reception details + QR
        binding.tvReceptionDate.setText(getString(R.string.ticket_created_at, UiFormatters.formatDateTime(item.ticket.getCreatedAt())));
        String customerFault = item.ticket.getCustomerNote();
        binding.tvCustomerFault.setText("Lỗi khách phản ánh: " + (customerFault != null && !customerFault.trim().isEmpty()
                ? customerFault.trim()
                : "Chưa ghi nhận"));
        String initCond = item.device != null ? item.device.getInitialCondition() : "";
        binding.tvInitialCondition.setText("Ngoại quan tiếp nhận: " + (initCond != null && !initCond.trim().isEmpty()
                ? initCond.trim()
                : "Không ghi chú thêm"));

        Bitmap qrBitmap = QRCodeUtils.generateQRCodeBitmap(item.ticket.getTicketCode(), 360, 360);
        if (qrBitmap != null) {
            binding.imgTicketQr.setImageBitmap(qrBitmap);
            binding.imgTicketQr.setVisibility(View.VISIBLE);
        } else {
            binding.imgTicketQr.setVisibility(View.GONE);
        }

        // Diagnosis details
        binding.tvTechnicianName.setText(item.technician != null
                ? "Kỹ thuật viên phụ trách: " + item.technician.getFullName()
                : "Chưa phân công kỹ thuật viên");
        String techNote = item.ticket.getTechnicianNote();
        binding.tvTechNote.setText(techNote != null && !techNote.trim().isEmpty()
                ? techNote.trim()
                : "Kỹ thuật viên đang kiểm tra thiết bị, kết quả chẩn đoán sẽ hiển thị tại đây.");

        // Quotation Breakdown (hide "0 đ" before a quote exists)
        bindQuotationBreakdown(item, status);

        // 6-stage Timeline
        bindProgressTimeline(status);

        // Handover / Invoice summary
        if (item.invoice != null) {
            binding.sectionHandover.setVisibility(View.VISIBLE);
            binding.tvHandoverDetails.setText(
                    "Mã hóa đơn: " + item.invoice.getInvoiceCode()
                            + "\nThanh toán: " + UiFormatters.paymentMethodLabel(item.invoice.getPaymentMethod())
                            + "\nGiảm giá: " + UiFormatters.formatCurrency(item.invoice.getDiscount())
                            + "\nTổng thanh toán: " + UiFormatters.formatCurrency(item.invoice.getFinalTotal())
                            + "\nThời gian bàn giao: " + UiFormatters.formatDateTime(item.invoice.getCreatedAt())
            );
        } else {
            binding.sectionHandover.setVisibility(View.GONE);
        }

        // Fixed Approval Bar ONLY when waiting for customer approval
        boolean waitingApproval = Constants.TICKET_WAITING_APPROVAL.equals(status);
        binding.cardApprovalBox.setVisibility(waitingApproval ? View.VISIBLE : View.GONE);
        if (waitingApproval) {
            binding.tvStickyQuoteSummary.setText(getString(R.string.quotation_total, UiFormatters.formatCurrency(item.ticket.getTotalCost())));
        }
        updateActionButtons();
    }

    private void bindQuotationBreakdown(TicketWithDetails item, String status) {
        binding.containerQuoteParts.removeAllViews();

        Map<Integer, String> partNames = new HashMap<>();
        if (item.parts != null) {
            for (InventoryPartEntity part : item.parts) {
                partNames.put(part.getId(), part.getPartName());
            }
        }

        boolean hasParts = item.partDetails != null && !item.partDetails.isEmpty();
        if (hasParts) {
            for (TicketPartDetailEntity detail : item.partDetails) {
                String name = partNames.get(detail.getPartId());
                if (name == null) {
                    name = "Linh kiện #" + detail.getPartId();
                }
                double lineTotal = detail.getUnitPrice() * detail.getQuantity();
                addCostRow(
                        binding.containerQuoteParts,
                        name + " (×" + detail.getQuantity() + ")",
                        UiFormatters.formatCurrency(lineTotal)
                );
            }
        }

        boolean quoteReady = hasParts
                || item.ticket.getLaborFee() > 0.0
                || item.ticket.getTotalCost() > 0.0
                || Constants.TICKET_WAITING_APPROVAL.equals(status)
                || Constants.TICKET_APPROVED.equals(status)
                || Constants.TICKET_REPAIRING.equals(status)
                || Constants.TICKET_COMPLETED.equals(status)
                || Constants.TICKET_DELIVERED.equals(status);

        if (!quoteReady) {
            binding.rowLaborFee.setVisibility(View.GONE);
            binding.quoteDivider.setVisibility(View.GONE);
            binding.tvQuotationTotal.setText(R.string.quote_pending_label);
            binding.tvQuotationTotal.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
        } else {
            binding.rowLaborFee.setVisibility(View.VISIBLE);
            binding.tvQuoteLaborFee.setText(UiFormatters.formatCurrency(item.ticket.getLaborFee()));
            binding.quoteDivider.setVisibility(View.VISIBLE);
            binding.tvQuotationTotal.setText(getString(R.string.quotation_total, UiFormatters.formatCurrency(item.ticket.getTotalCost())));
            binding.tvQuotationTotal.setTextColor(ContextCompat.getColor(this, R.color.primary));
        }
    }

    private void addCostRow(LinearLayout container, String label, String amountText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(8);
        row.setLayoutParams(params);

        TextView tvLabel = new TextView(this);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvLabel.setText(label);
        tvLabel.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        TextView tvValue = new TextView(this);
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        tvValue.setText(amountText);
        tvValue.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
        tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        row.addView(tvLabel);
        row.addView(tvValue);
        container.addView(row);
    }

    private void bindProgressTimeline(String status) {
        String[] stageLabels = getResources().getStringArray(R.array.repair_stages);
        TextView[] stepViews = new TextView[] {
                binding.step1, binding.step2, binding.step3,
                binding.step4, binding.step5, binding.step6
        };

        int currentStage = RepairProgress.stage(status);
        boolean stopped = RepairProgress.isStopped(status);

        if (Constants.TICKET_REJECTED.equals(status)) {
            binding.tvTimelineState.setText(R.string.repair_rejected);
        } else if (Constants.TICKET_CANCELLED.equals(status)) {
            binding.tvTimelineState.setText(R.string.repair_cancelled);
        } else if (Constants.TICKET_APPROVED.equals(status)) {
            binding.tvTimelineState.setText(R.string.repair_approved);
        } else if (Constants.TICKET_DELIVERED.equals(status)) {
            binding.tvTimelineState.setText(R.string.repair_delivered);
        } else if (currentStage < 0) {
            binding.tvTimelineState.setText(R.string.repair_unknown);
        } else {
            binding.tvTimelineState.setText(R.string.repair_progress_hint);
        }

        for (int i = 0; i < stepViews.length && i < stageLabels.length; i++) {
            String title = stageLabels[i];
            if (i == 2 && currentStage >= 2 && !Constants.TICKET_WAITING_APPROVAL.equals(status) && !stopped) {
                title = getString(R.string.quote_approved_stage);
            }
            TextView stepView = stepViews[i];

            if (stopped) {
                if (Constants.TICKET_REJECTED.equals(status) && i < 2) {
                    applyDoneStep(stepView, title);
                } else if (Constants.TICKET_REJECTED.equals(status) && i == 2) {
                    applyStoppedStep(stepView, "Từ chối báo giá");
                } else if (Constants.TICKET_CANCELLED.equals(status) && i == 0) {
                    applyStoppedStep(stepView, "Phiếu đã hủy");
                } else {
                    applyPendingStep(stepView, title);
                }
            } else if (currentStage > i || (i == 5 && Constants.TICKET_DELIVERED.equals(status))) {
                applyDoneStep(stepView, title);
            } else if (currentStage == i) {
                applyActiveStep(stepView, title);
            } else {
                applyPendingStep(stepView, title);
            }
        }
    }

    private void applyDoneStep(TextView view, String label) {
        view.setBackgroundResource(R.drawable.bg_progress_done);
        view.setText(getString(R.string.stage_done, label));
        view.setTextColor(ContextCompat.getColor(this, R.color.on_success_container));
    }

    private void applyActiveStep(TextView view, String label) {
        view.setBackgroundResource(R.drawable.bg_progress_active);
        view.setText(getString(R.string.stage_current, label));
        view.setTextColor(ContextCompat.getColor(this, R.color.on_primary_container));
    }

    private void applyStoppedStep(TextView view, String label) {
        view.setBackgroundResource(R.drawable.bg_progress_stopped);
        view.setText(label);
        view.setTextColor(ContextCompat.getColor(this, R.color.on_error_container));
    }

    private void applyPendingStep(TextView view, String label) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(12));
        bg.setColor(ContextCompat.getColor(this, R.color.surface_variant));
        view.setBackground(bg);
        view.setText(getString(R.string.stage_pending, label));
        view.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant));
    }

    private void confirmApprove() {
        if (actionPending || currentTicket == null || currentTicket.ticket == null) return;
        String totalText = UiFormatters.formatCurrency(currentTicket.ticket.getTotalCost());
        long versionAtPrompt = displayedUpdatedAt;

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_approve_title)
                .setMessage(getString(R.string.confirm_approve_body, totalText))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.confirm_action, (d, w) -> {
                    actionPending = true;
                    updateActionButtons();
                    repository.approveTicketAndDeductStock(ticketId, versionAtPrompt, new RepairTicketRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            actionPending = false;
                            updateActionButtons();
                            Snackbar.make(binding.timelineLayout, "Đã đồng ý báo giá. Kỹ thuật viên sẽ bắt đầu sửa chữa.", Snackbar.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String message) {
                            actionPending = false;
                            updateActionButtons();
                            new MaterialAlertDialogBuilder(TicketTimelineActivity.this)
                                    .setTitle("Không thể xác nhận báo giá")
                                    .setMessage(message)
                                    .setPositiveButton(R.string.action_close, null)
                                    .show();
                        }
                    });
                })
                .show();
    }

    private void confirmReject() {
        if (actionPending || currentTicket == null || currentTicket.ticket == null) return;
        String totalText = UiFormatters.formatCurrency(currentTicket.ticket.getTotalCost());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_reject_title)
                .setMessage(getString(R.string.confirm_reject_body, totalText))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Từ chối báo giá", (d, w) -> {
                    actionPending = true;
                    updateActionButtons();
                    repository.cancelTicketAndRestock(ticketId, Constants.TICKET_REJECTED, new RepairTicketRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            actionPending = false;
                            updateActionButtons();
                            Snackbar.make(binding.timelineLayout, "Đã ghi nhận từ chối báo giá.", Snackbar.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String message) {
                            actionPending = false;
                            updateActionButtons();
                            new MaterialAlertDialogBuilder(TicketTimelineActivity.this)
                                    .setTitle("Không thể từ chối báo giá")
                                    .setMessage(message)
                                    .setPositiveButton(R.string.action_close, null)
                                    .show();
                        }
                    });
                })
                .show();
    }

    private void updateActionButtons() {
        boolean enabled = !actionPending
                && currentTicket != null
                && currentTicket.ticket != null
                && Constants.TICKET_WAITING_APPROVAL.equals(currentTicket.ticket.getStatus());
        binding.btnApprove.setEnabled(enabled);
        binding.btnReject.setEnabled(enabled);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
