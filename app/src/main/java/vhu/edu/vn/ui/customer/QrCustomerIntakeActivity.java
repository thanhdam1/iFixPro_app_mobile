package vhu.edu.vn.ui.customer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import vhu.edu.vn.R;
import vhu.edu.vn.data.repository.InvoiceRepository;
import vhu.edu.vn.data.repository.RepairTicketRepository;
import vhu.edu.vn.databinding.ActivityQrCustomerIntakeBinding;
import vhu.edu.vn.ui.receptionist.PrintInvoiceActivity;
import vhu.edu.vn.utils.Constants;
import vhu.edu.vn.utils.QRCodeUtils;

public class QrCustomerIntakeActivity extends AppCompatActivity {
    private ActivityQrCustomerIntakeBinding binding;
    private RepairTicketRepository repository;
    private InvoiceRepository invoiceRepository;
    private int currentTicketId = 0;
    private String currentTicketCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityQrCustomerIntakeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.qrIntakeRoot, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = new RepairTicketRepository(this);
        invoiceRepository = new InvoiceRepository(this);

        setupTextWatchers();
        setupClickListeners();

        // Pre-fill sample values for quick QR intake
        binding.edtCustomerName.setText("Lê Trần Tuấn Minh");
        binding.edtCustomerPhone.setText("0988776655");
        binding.edtDeviceName.setText("Dell Latitude");
        binding.edtImeiSerial.setText("C02X9082JG5H");
        binding.edtFaultNote.setText("Lỗi CPU");
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tvFormError.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) { }
        };
        binding.edtCustomerName.addTextChangedListener(watcher);
        binding.edtCustomerPhone.addTextChangedListener(watcher);
        binding.edtDeviceName.addTextChangedListener(watcher);
        binding.edtFaultNote.addTextChangedListener(watcher);
    }

    private void setupClickListeners() {
        binding.btnTopSearchGo.setOnClickListener(v -> {
            String input = binding.edtTopSearchCode.getText() != null ? binding.edtTopSearchCode.getText().toString().trim() : "";
            if (!input.isEmpty()) {
                searchAndLoadTicketByCode(input);
            } else {
                Toast.makeText(this, "Vui lòng nhập mã phiếu QR (VD: FIX-261123-2344)", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnTopScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(this, vhu.edu.vn.ui.technician.QRScannerActivity.class);
            startActivity(intent);
        });

        binding.btnSubmitQrTicket.setOnClickListener(v -> submitIntakeForm());
        binding.btnSelectPhoto.setOnClickListener(v ->
            Toast.makeText(this, "Đã sẵn sàng tải ảnh hiện trạng thiết bị", Toast.LENGTH_SHORT).show()
        );

        binding.btnCopyCode.setOnClickListener(v -> {
            if (!currentTicketCode.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Mã phiếu iFixPro", currentTicketCode);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Đã sao chép mã: " + currentTicketCode, Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.btnNewSearch.setOnClickListener(v -> showQuickSearchDialog());

        binding.btnPrintTicket.setOnClickListener(v -> {
            if (currentTicketId > 0) {
                Intent intent = new Intent(this, PrintInvoiceActivity.class);
                intent.putExtra("ticket_id", currentTicketId);
                startActivity(intent);
            }
        });

        binding.btnViewProgressNow.setOnClickListener(v -> showTrackingView());

        binding.tabProgress.setOnClickListener(v -> switchTrackingTab(1));
        binding.tabPayment.setOnClickListener(v -> switchTrackingTab(2));
        binding.tabWarranty.setOnClickListener(v -> switchTrackingTab(3));

        binding.btnPrintPaymentInvoice.setOnClickListener(v -> {
            if (currentTicketId > 0) {
                invoiceRepository.issueCashInvoiceAtCounter(currentTicketId, 1, new InvoiceRepository.InvoiceCreateCallback() {
                    @Override
                    public void onSuccess(String invoiceCode) {
                        Toast.makeText(QrCustomerIntakeActivity.this, "Đã tạo biên nhận thu tiền mặt: " + invoiceCode, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(QrCustomerIntakeActivity.this, PrintInvoiceActivity.class);
                        intent.putExtra("ticket_id", currentTicketId);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(String message) {
                        Intent intent = new Intent(QrCustomerIntakeActivity.this, PrintInvoiceActivity.class);
                        intent.putExtra("ticket_id", currentTicketId);
                        startActivity(intent);
                    }
                });
            } else {
                Toast.makeText(this, "Chưa có thông tin phiếu để in biên nhận", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnAddEvidencePhoto.setOnClickListener(v -> {
            if (currentTicketId > 0) {
                repository.updateDeviceEvidencePhotos(currentTicketId, "evidence_photo_sample.jpg", new RepairTicketRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        binding.tvEvidencePhotoTitle.setText("📷 Hình Ảnh Minh Chứng Lúc Tiếp Nhận (1 ảnh)");
                        Toast.makeText(QrCustomerIntakeActivity.this, "Đã cập nhật 1 ảnh minh chứng tiếp nhận", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(QrCustomerIntakeActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Hình ảnh minh chứng lúc tiếp nhận: 0 ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        binding.fabHotline.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:02838604999"));
            startActivity(intent);
        });

        binding.btnMenuPortal.setOnClickListener(v ->
            Toast.makeText(this, "Cổng tiếp nhận CSKH iFixPro Lục Giác", Toast.LENGTH_SHORT).show()
        );
    }

    private void switchTrackingTab(int tabIndex) {
        binding.viewTabProgress.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        binding.viewTabPayment.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        binding.viewTabWarranty.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);

        binding.tabProgress.setBackgroundTintList(ColorStateList.valueOf(tabIndex == 1 ? 0xFF0284C7 : 0xFFFFFFFF));
        binding.tabProgress.setTextColor(tabIndex == 1 ? 0xFFFFFFFF : 0xFF475569);

        binding.tabPayment.setBackgroundTintList(ColorStateList.valueOf(tabIndex == 2 ? 0xFF0284C7 : 0xFFFFFFFF));
        binding.tabPayment.setTextColor(tabIndex == 2 ? 0xFFFFFFFF : 0xFF475569);

        binding.tabWarranty.setBackgroundTintList(ColorStateList.valueOf(tabIndex == 3 ? 0xFF0284C7 : 0xFFFFFFFF));
        binding.tabWarranty.setTextColor(tabIndex == 3 ? 0xFFFFFFFF : 0xFF475569);

        if (tabIndex == 2 && !currentTicketCode.isEmpty()) {
            binding.tvCashStep1.setText("1. Quý khách xuất trình Mã phiếu QR #" + currentTicketCode + " cho nhân viên Lễ tân tại quầy.");
        }
    }

    private void submitIntakeForm() {
        String name = textOf(binding.edtCustomerName);
        String phone = textOf(binding.edtCustomerPhone);
        String deviceName = textOf(binding.edtDeviceName);
        String serialTag = textOf(binding.edtImeiSerial);
        String faultNote = textOf(binding.edtFaultNote);

        if (name.isEmpty()) {
            showError("Vui lòng nhập họ và tên khách hàng");
            return;
        }
        if (phone.isEmpty() || phone.length() < 8) {
            showError("Vui lòng nhập số điện thoại liên hệ hợp lệ");
            return;
        }
        if (deviceName.isEmpty()) {
            showError("Vui lòng nhập tên thiết bị hoặc dịch vụ cần hỗ trợ");
            return;
        }
        if (faultNote.isEmpty()) {
            showError("Vui lòng mô tả chi tiết sự cố phần cứng/phần mềm");
            return;
        }

        binding.btnSubmitQrTicket.setEnabled(false);
        binding.btnSubmitQrTicket.setText("Đang khởi tạo phiếu...");

        // Create ticket intake with No Login required (uses System Reception ID 1)
        repository.createTicketIntake(
                name,
                phone,
                Constants.DEVICE_TYPE_PHONE,
                deviceName,
                serialTag.isEmpty() ? "Không có" : serialTag,
                "",
                "Tiếp nhận qua mã QR Khách hàng",
                faultNote,
                1, // System staff ID
                new RepairTicketRepository.TicketCreateCallback() {
                    @Override
                    public void onSuccess(int ticketId, String ticketCode) {
                        binding.btnSubmitQrTicket.setEnabled(true);
                        binding.btnSubmitQrTicket.setText("Tạo Phiếu & Nhận Mã QR ➔");
                        currentTicketId = ticketId;
                        currentTicketCode = ticketCode;
                        showResultView(name, phone, deviceName, faultNote, ticketCode);
                        updateStepProgressUI(Constants.TICKET_RECEIVED);
                    }

                    @Override
                    public void onError(String message) {
                        binding.btnSubmitQrTicket.setEnabled(true);
                        binding.btnSubmitQrTicket.setText("Tạo Phiếu & Nhận Mã QR ➔");
                        showError(message);
                    }
                }
        );
    }

    private void showResultView(String name, String phone, String deviceName, String faultNote, String ticketCode) {
        binding.containerIntakeForm.setVisibility(View.GONE);
        binding.containerTracking.setVisibility(View.GONE);
        binding.containerTicketResult.setVisibility(View.VISIBLE);

        binding.tvDeviceTitle.setText(deviceName);
        binding.tvTicketCodeBadge.setText("#" + ticketCode);
        binding.tvResultCustomerName.setText(name);
        binding.tvResultPhone.setText(phone);
        binding.tvTrackingFaultNote.setText(faultNote);

        Bitmap qrBitmap = QRCodeUtils.generateQRCodeBitmap(ticketCode, 480, 480);
        if (qrBitmap != null) {
            binding.imgResultQrCode.setImageBitmap(qrBitmap);
        }

        String now = new SimpleDateFormat("HH:mm:ss, dd/MM/yyyy", Locale.getDefault()).format(new Date());
        binding.tvLogTime.setText(now);
    }

    private void updateStepProgressUI(String status) {
        int currentStep = 1;
        String statusLabel = "Mới tiếp nhận";

        if (Constants.TICKET_RECEIVED.equals(status)) {
            currentStep = 1;
            statusLabel = "Mới tiếp nhận";
        } else if (Constants.TICKET_DIAGNOSING.equals(status)
                || Constants.TICKET_WAITING_APPROVAL.equals(status)
                || Constants.TICKET_APPROVED.equals(status)) {
            currentStep = 2;
            statusLabel = "Đang kiểm tra & chẩn đoán";
        } else if (Constants.TICKET_REPAIRING.equals(status)) {
            currentStep = 3;
            statusLabel = "Đang sửa chữa & cài đặt";
        } else if (Constants.TICKET_COMPLETED.equals(status) || Constants.TICKET_DELIVERED.equals(status)) {
            currentStep = 4;
            statusLabel = "Hoàn tất & bảo hành QR";
        }

        binding.tvStepProgressTitle.setText("🌀 TIẾN TRÌNH XỬ LÝ HỒ SƠ (BƯỚC " + currentStep + "/4) - " + statusLabel.toUpperCase(Locale.getDefault()));

        if (currentStep > 1) {
            binding.stepCard1.setBackgroundResource(R.drawable.bg_step_card_completed);
            binding.imgStep1Icon.setColorFilter(0xFF10B981);
            binding.tvStep1Text.setTextColor(0xFF047857);
        } else {
            binding.stepCard1.setBackgroundResource(R.drawable.bg_step_card_active);
            binding.imgStep1Icon.setColorFilter(0xFFD97706);
            binding.tvStep1Text.setTextColor(0xFF92400E);
        }

        if (currentStep > 2) {
            binding.stepCard2.setBackgroundResource(R.drawable.bg_step_card_completed);
            binding.imgStep2Icon.setColorFilter(0xFF10B981);
            binding.tvStep2Text.setTextColor(0xFF047857);
        } else if (currentStep == 2) {
            binding.stepCard2.setBackgroundResource(R.drawable.bg_step_card_active);
            binding.imgStep2Icon.setColorFilter(0xFFD97706);
            binding.tvStep2Text.setTextColor(0xFF92400E);
        } else {
            binding.stepCard2.setBackgroundResource(R.drawable.bg_step_card_inactive);
            binding.imgStep2Icon.setColorFilter(0xFF94A3B8);
            binding.tvStep2Text.setTextColor(0xFF64748B);
        }

        if (currentStep > 3) {
            binding.stepCard3.setBackgroundResource(R.drawable.bg_step_card_completed);
            binding.imgStep3Icon.setColorFilter(0xFF10B981);
            binding.tvStep3Text.setTextColor(0xFF047857);
        } else if (currentStep == 3) {
            binding.stepCard3.setBackgroundResource(R.drawable.bg_step_card_active);
            binding.imgStep3Icon.setColorFilter(0xFFD97706);
            binding.tvStep3Text.setTextColor(0xFF92400E);
        } else {
            binding.stepCard3.setBackgroundResource(R.drawable.bg_step_card_inactive);
            binding.imgStep3Icon.setColorFilter(0xFF94A3B8);
            binding.tvStep3Text.setTextColor(0xFF64748B);
        }

        if (currentStep == 4) {
            binding.stepCard4.setBackgroundResource(R.drawable.bg_step_card_completed);
            binding.imgStep4Icon.setColorFilter(0xFF10B981);
            binding.tvStep4Text.setTextColor(0xFF047857);
        } else {
            binding.stepCard4.setBackgroundResource(R.drawable.bg_step_card_inactive);
            binding.imgStep4Icon.setColorFilter(0xFF94A3B8);
            binding.tvStep4Text.setTextColor(0xFF64748B);
        }
    }

    private void showTrackingView() {
        binding.containerIntakeForm.setVisibility(View.GONE);
        binding.containerTicketResult.setVisibility(View.GONE);
        binding.containerTracking.setVisibility(View.VISIBLE);
        switchTrackingTab(1);
    }

    private void resetToFormState() {
        binding.containerTicketResult.setVisibility(View.GONE);
        binding.containerTracking.setVisibility(View.GONE);
        binding.containerIntakeForm.setVisibility(View.VISIBLE);

        binding.edtCustomerName.setText("");
        binding.edtCustomerPhone.setText("");
        binding.edtDeviceName.setText("");
        binding.edtImeiSerial.setText("");
        binding.edtFaultNote.setText("");
        binding.tvFormError.setVisibility(View.GONE);
    }

    private void showQuickSearchDialog() {
        com.google.android.material.textfield.TextInputLayout inputLayout = new com.google.android.material.textfield.TextInputLayout(this);
        inputLayout.setHint("Nhập mã phiếu QR (Ví dụ: FIX-261123-2344 / RP-569689)");
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        inputLayout.setPadding(pad, pad, pad, 0);

        com.google.android.material.textfield.TextInputEditText edtCode = new com.google.android.material.textfield.TextInputEditText(this);
        edtCode.setSingleLine(true);
        inputLayout.addView(edtCode);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("🔍 Tra Cứu Hồ Sơ Phiếu QR")
                .setMessage("Nhập mã phiếu hoặc chọn quét camera để tra cứu hồ sơ sửa chữa & in phiếu.")
                .setView(inputLayout)
                .setNeutralButton("📷 Quét Camera", (d, w) -> {
                    Intent intent = new Intent(this, vhu.edu.vn.ui.technician.QRScannerActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Tạo phiếu mới", (d, w) -> resetToFormState())
                .setPositiveButton("Tra cứu ngay", (d, w) -> {
                    String input = edtCode.getText() != null ? edtCode.getText().toString().trim() : "";
                    if (!input.isEmpty()) {
                        searchAndLoadTicketByCode(input);
                    }
                })
                .show();
    }

    private void searchAndLoadTicketByCode(String code) {
        repository.findTicketByCodeAndLoadDetails(code, new RepairTicketRepository.TicketDetailsCallback() {
            @Override
            public void onSuccess(vhu.edu.vn.data.local.model.TicketWithDetails details) {
                if (details == null || details.ticket == null) return;
                currentTicketId = details.ticket.getId();
                currentTicketCode = details.ticket.getTicketCode();

                String devName = details.device != null ? details.device.getBrandModel() : "Thiết bị sửa chữa";
                String custName = details.customer != null ? details.customer.getFullName() : "Khách hàng";
                String custPhone = details.customer != null ? details.customer.getPhone() : "";
                String note = details.ticket.getCustomerNote() != null ? details.ticket.getCustomerNote() : "";

                showResultView(custName, custPhone, devName, note, currentTicketCode);
                updateStepProgressUI(details.ticket.getStatus());
                Toast.makeText(QrCustomerIntakeActivity.this, "Đã tải xong hồ sơ phiếu #" + currentTicketCode, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(QrCustomerIntakeActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showError(String message) {
        binding.tvFormError.setVisibility(View.VISIBLE);
        binding.tvFormError.setText("⚠️ " + message);
    }

    private String textOf(com.google.android.material.textfield.TextInputEditText edt) {
        return edt.getText() != null ? edt.getText().toString().trim() : "";
    }
}
