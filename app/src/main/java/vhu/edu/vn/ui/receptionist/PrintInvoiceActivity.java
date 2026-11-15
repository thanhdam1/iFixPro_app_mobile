package vhu.edu.vn.ui.receptionist;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import vhu.edu.vn.data.repository.InvoiceRepository;
import vhu.edu.vn.data.repository.RepairTicketRepository;
import vhu.edu.vn.databinding.ActivityPrintInvoiceBinding;
import vhu.edu.vn.utils.Constants;
import vhu.edu.vn.utils.QRCodeUtils;
import vhu.edu.vn.utils.SessionManager;
import vhu.edu.vn.utils.UiFormatters;

public class PrintInvoiceActivity extends AppCompatActivity {
    private ActivityPrintInvoiceBinding binding;
    private RepairTicketRepository ticketRepository;
    private InvoiceRepository invoiceRepository;
    private SessionManager sessionManager;

    private int ticketId;
    private TicketWithDetails currentTicket;
    private double currentSubTotal;
    private boolean actionPending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPrintInvoiceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.printInvoiceLayout, (v, insets) -> {
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
        invoiceRepository = new InvoiceRepository(this);

        binding.edtDiscount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilDiscount.setError(null);
                updateLiveDiscountTotal();
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        binding.btnPrintInvoice.setOnClickListener(v -> confirmAndIssueInvoice());
        binding.btnCancelTicket.setOnClickListener(v -> confirmAndCancelTicket());

        ticketRepository.getTicketById(ticketId).observe(this, item -> {
            if (item == null || item.ticket == null) {
                binding.tvInvoiceTicketCode.setText(R.string.ticket_not_found);
                binding.cardBillingControls.setVisibility(View.GONE);
                binding.btnCancelTicket.setVisibility(View.GONE);
                return;
            }
            currentTicket = item;
            bindTicket(item);
        });
    }

    private void bindTicket(TicketWithDetails item) {
        String status = item.ticket.getStatus();
        UiFormatters.StatusStyle style = UiFormatters.statusStyle(status);

        binding.tvInvoiceTicketCode.setText(item.ticket.getTicketCode());
        int textColor = ContextCompat.getColor(this, style.textColor);
        int bgColor = ContextCompat.getColor(this, style.bgColor);
        binding.tvStatusDetail.setText(style.label);
        binding.tvStatusDetail.setTextColor(textColor);
        binding.imgStatusIcon.setImageResource(style.iconRes);
        binding.imgStatusIcon.setColorFilter(textColor);
        binding.badgeStatusContainer.getBackground().mutate();
        binding.badgeStatusContainer.getBackground().setTint(bgColor);

        Bitmap qrBitmap = QRCodeUtils.generateQRCodeBitmap(item.ticket.getTicketCode(), 420, 420);
        if (qrBitmap != null) {
            binding.imgQrCode.setImageBitmap(qrBitmap);
            binding.imgQrCode.setVisibility(View.VISIBLE);
        } else {
            binding.imgQrCode.setVisibility(View.GONE);
        }

        if (item.customer != null) {
            binding.tvCustomerDetail.setText("Khách hàng: " + item.customer.getFullName() + " · " + item.customer.getPhone());
        } else {
            binding.tvCustomerDetail.setText(R.string.unknown_customer);
        }

        if (item.device != null) {
            binding.tvDeviceDetail.setText("Thiết bị: " + item.device.getBrandModel()
                    + " (" + UiFormatters.deviceTypeLabel(item.device.getDeviceType())
                    + " · IMEI/SN: " + item.device.getImeiSerial() + ")");
        } else {
            binding.tvDeviceDetail.setText(R.string.unknown_device);
        }

        String fault = item.ticket.getCustomerNote();
        binding.tvFaultDetail.setText("Lỗi tiếp nhận: " + (fault != null && !fault.trim().isEmpty() ? fault.trim() : "Chưa ghi nhận"));

        // Parts breakdown
        binding.containerInvoiceParts.removeAllViews();
        Map<Integer, String> partNames = new HashMap<>();
        if (item.parts != null) {
            for (InventoryPartEntity part : item.parts) {
                partNames.put(part.getId(), part.getPartName());
            }
        }

        double partsTotal = 0.0;
        if (item.partDetails != null) {
            for (TicketPartDetailEntity detail : item.partDetails) {
                String name = partNames.get(detail.getPartId());
                if (name == null) {
                    name = "Linh kiện #" + detail.getPartId();
                }
                double lineTotal = detail.getUnitPrice() * detail.getQuantity();
                partsTotal += lineTotal;
                addPartLine(name + " (×" + detail.getQuantity() + ")", UiFormatters.formatCurrency(lineTotal));
            }
        }

        double laborFee = item.ticket.getLaborFee();
        currentSubTotal = partsTotal + laborFee;
        if (currentSubTotal <= 0.0 && item.ticket.getTotalCost() > 0.0) {
            currentSubTotal = item.ticket.getTotalCost();
        }

        binding.tvPartsSubtotal.setText(UiFormatters.formatCurrency(partsTotal));
        binding.tvLaborSubtotal.setText(UiFormatters.formatCurrency(laborFee));

        boolean canBill = Constants.TICKET_COMPLETED.equals(status) && item.invoice == null;
        boolean hasInvoice = item.invoice != null;
        boolean canCancel = Constants.TICKET_RECEIVED.equals(status)
                || Constants.TICKET_DIAGNOSING.equals(status)
                || Constants.TICKET_WAITING_APPROVAL.equals(status)
                || Constants.TICKET_APPROVED.equals(status)
                || Constants.TICKET_REPAIRING.equals(status);

        binding.cardBillingControls.setVisibility(canBill ? View.VISIBLE : View.GONE);
        binding.btnCancelTicket.setVisibility(canCancel ? View.VISIBLE : View.GONE);

        if (hasInvoice) {
            binding.rowIssuedDiscount.setVisibility(View.VISIBLE);
            binding.tvIssuedDiscount.setText("-" + UiFormatters.formatCurrency(item.invoice.getDiscount()));
            binding.tvTotalCostDetail.setText("Tổng thanh toán: " + UiFormatters.formatCurrency(item.invoice.getFinalTotal()));
            binding.cardInvoiceNotice.setVisibility(View.VISIBLE);
            binding.tvInvoiceNotice.setText(
                    "Đã lập hóa đơn: " + item.invoice.getInvoiceCode()
                            + "\nPhương thức: " + UiFormatters.paymentMethodLabel(item.invoice.getPaymentMethod())
                            + "\nThời gian lập: " + UiFormatters.formatDateTime(item.invoice.getCreatedAt())
            );
        } else if (canBill) {
            binding.rowIssuedDiscount.setVisibility(View.GONE);
            binding.cardInvoiceNotice.setVisibility(View.GONE);
            updateLiveDiscountTotal();
        } else {
            binding.rowIssuedDiscount.setVisibility(View.GONE);
            binding.cardInvoiceNotice.setVisibility(View.VISIBLE);
            if (item.ticket.getTotalCost() > 0.0) {
                binding.tvTotalCostDetail.setText("Tạm tính: " + UiFormatters.formatCurrency(currentSubTotal));
            } else {
                binding.tvTotalCostDetail.setText(R.string.quote_pending_label);
            }
            binding.tvInvoiceNotice.setText(resolveNonBillableNotice(status));
        }

        updateButtonsEnabled();
    }

    private String resolveNonBillableNotice(String status) {
        if (Constants.TICKET_REJECTED.equals(status)) {
            return "Khách hàng đã từ chối báo giá. Chỉ bàn giao lại máy nguyên trạng, không lập hóa đơn sửa chữa.";
        }
        if (Constants.TICKET_CANCELLED.equals(status)) {
            return "Phiếu sửa chữa đã bị hủy. Vui lòng hoàn trả thiết bị cho khách hàng.";
        }
        return "Chỉ có thể lập hóa đơn & bàn giao sau khi kỹ thuật viên hoàn tất sửa chữa (trạng thái hiện tại: "
                + UiFormatters.statusStyle(status).label + ").";
    }

    private void addPartLine(String label, String price) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = dp(6);
        row.setLayoutParams(params);

        TextView tvLabel = new TextView(this);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvLabel.setText(label);
        tvLabel.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        TextView tvPrice = new TextView(this);
        tvPrice.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        tvPrice.setText(price);
        tvPrice.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
        tvPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        row.addView(tvLabel);
        row.addView(tvPrice);
        binding.containerInvoiceParts.addView(row);
    }

    private void updateLiveDiscountTotal() {
        if (currentTicket == null || currentTicket.ticket == null) return;
        if (!Constants.TICKET_COMPLETED.equals(currentTicket.ticket.getStatus()) || currentTicket.invoice != null) {
            return;
        }
        double discount = parseDiscountOrZero();
        if (discount < 0.0 || discount > currentSubTotal) {
            binding.tilDiscount.setError("Giảm giá phải từ 0 đến " + UiFormatters.formatCurrency(currentSubTotal));
            binding.tvTotalCostDetail.setText("Tổng thanh toán: " + UiFormatters.formatCurrency(currentSubTotal));
        } else {
            double finalTotal = Math.max(0.0, currentSubTotal - discount);
            binding.tvTotalCostDetail.setText("Tổng thanh toán: " + UiFormatters.formatCurrency(finalTotal));
        }
    }

    private double parseDiscountOrZero() {
        String raw = binding.edtDiscount.getText() != null ? binding.edtDiscount.getText().toString().trim() : "";
        if (raw.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    private String resolvePaymentMethod() {
        if (binding.chipPayBank.isChecked()) return Constants.PAYMENT_BANK_TRANSFER;
        if (binding.chipPayQr.isChecked()) return Constants.PAYMENT_QR;
        return Constants.PAYMENT_CASH;
    }

    private void confirmAndIssueInvoice() {
        if (actionPending || currentTicket == null || currentTicket.ticket == null) return;

        double discount = parseDiscountOrZero();
        if (discount < 0.0 || discount > currentSubTotal) {
            binding.tilDiscount.setError("Giảm giá phải từ 0 đến " + UiFormatters.formatCurrency(currentSubTotal));
            return;
        }

        double finalTotal = Math.max(0.0, currentSubTotal - discount);
        String method = resolvePaymentMethod();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận lập hóa đơn & bàn giao?")
                .setMessage("Tổng thanh toán: " + UiFormatters.formatCurrency(finalTotal)
                        + "\nPhương thức: " + UiFormatters.paymentMethodLabel(method)
                        + "\nPhiếu sẽ chuyển sang trạng thái Đã bàn giao.")
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Xác nhận bàn giao", (d, w) -> {
                    actionPending = true;
                    updateButtonsEnabled();
                    invoiceRepository.issueInvoiceAndDeliver(
                            ticketId,
                            sessionManager.getUserId(),
                            discount,
                            method,
                            new InvoiceRepository.InvoiceCreateCallback() {
                                @Override
                                public void onSuccess(String invoiceCode) {
                                    actionPending = false;
                                    updateButtonsEnabled();
                                    Snackbar.make(binding.printInvoiceLayout,
                                            "Đã lập hóa đơn " + invoiceCode + " và bàn giao máy.",
                                            Snackbar.LENGTH_LONG).show();
                                }

                                @Override
                                public void onError(String message) {
                                    actionPending = false;
                                    updateButtonsEnabled();
                                    new MaterialAlertDialogBuilder(PrintInvoiceActivity.this)
                                            .setTitle("Không thể lập hóa đơn")
                                            .setMessage(message)
                                            .setPositiveButton(R.string.action_close, null)
                                            .show();
                                }
                            }
                    );
                })
                .show();
    }

    private void confirmAndCancelTicket() {
        if (actionPending || currentTicket == null || currentTicket.ticket == null) return;
        String status = currentTicket.ticket.getStatus();
        boolean willRestock = Constants.TICKET_APPROVED.equals(status) || Constants.TICKET_REPAIRING.equals(status);
        String detail = willRestock
                ? "Linh kiện đã giữ kho của phiếu này sẽ được hoàn trả lại kho."
                : "Phiếu sẽ được đánh dấu Đã hủy phiếu.";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Hủy phiếu sửa chữa?")
                .setMessage(detail)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Xác nhận hủy", (d, w) -> {
                    actionPending = true;
                    updateButtonsEnabled();
                    ticketRepository.cancelTicketAndRestock(ticketId, Constants.TICKET_CANCELLED, new RepairTicketRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            actionPending = false;
                            updateButtonsEnabled();
                            Snackbar.make(binding.printInvoiceLayout, "Đã hủy phiếu sửa chữa.", Snackbar.LENGTH_LONG).show();
                        }

                        @Override
                        public void onError(String message) {
                            actionPending = false;
                            updateButtonsEnabled();
                            new MaterialAlertDialogBuilder(PrintInvoiceActivity.this)
                                    .setTitle("Không thể hủy phiếu")
                                    .setMessage(message)
                                    .setPositiveButton(R.string.action_close, null)
                                    .show();
                        }
                    });
                })
                .show();
    }

    private void updateButtonsEnabled() {
        binding.btnPrintInvoice.setEnabled(!actionPending);
        binding.btnCancelTicket.setEnabled(!actionPending);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
