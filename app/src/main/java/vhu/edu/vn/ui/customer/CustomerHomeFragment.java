package vhu.edu.vn.ui.customer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import vhu.edu.vn.data.repository.InvoiceRepository;
import vhu.edu.vn.data.repository.RepairTicketRepository;
import vhu.edu.vn.databinding.FragmentCustomerHomeBinding;
import vhu.edu.vn.ui.receptionist.PrintInvoiceActivity;
import vhu.edu.vn.utils.Constants;
import vhu.edu.vn.utils.QRCodeUtils;
import vhu.edu.vn.utils.SessionManager;

public class CustomerHomeFragment extends Fragment {
    private FragmentCustomerHomeBinding binding;
    private RepairTicketRepository repository;
    private InvoiceRepository invoiceRepository;
    private SessionManager sessionManager;
    private int currentTicketId = 0;
    private String currentTicketCode = "";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentCustomerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        repository = new RepairTicketRepository(requireContext());
        invoiceRepository = new InvoiceRepository(requireContext());

        if (sessionManager.isLoggedIn() && sessionManager.getFullName() != null) {
            binding.edtCustomerName.setText(sessionManager.getFullName());
        }

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnTopSearchGo.setOnClickListener(v -> {
            String input = binding.edtTopSearchCode.getText() != null ? binding.edtTopSearchCode.getText().toString().trim() : "";
            if (!input.isEmpty()) {
                searchAndLoadTicketByCode(input);
            } else {
                Toast.makeText(requireContext(), "Vui lòng nhập mã phiếu QR (VD: FIX-261123-2344)", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnTopScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), vhu.edu.vn.ui.technician.QRScannerActivity.class);
            startActivity(intent);
        });

        binding.btnSubmitQrTicket.setOnClickListener(v -> submitIntakeForm());

        binding.btnSelectPhoto.setOnClickListener(v ->
            Toast.makeText(requireContext(), "Đã sẵn sàng chọn ảnh hiện trạng", Toast.LENGTH_SHORT).show()
        );

        binding.btnCopyCode.setOnClickListener(v -> {
            if (!currentTicketCode.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Mã phiếu iFixPro", currentTicketCode);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(requireContext(), "Đã sao chép mã: " + currentTicketCode, Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.btnNewSearch.setOnClickListener(v -> showQuickSearchDialog());

        binding.btnPrintTicket.setOnClickListener(v -> {
            if (currentTicketId > 0) {
                Intent intent = new Intent(requireContext(), PrintInvoiceActivity.class);
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
                invoiceRepository.issueCashInvoiceAtCounter(currentTicketId, sessionManager.getUserId(), new InvoiceRepository.InvoiceCreateCallback() {
                    @Override
                    public void onSuccess(String invoiceCode) {
                        Toast.makeText(requireContext(), "Đã tạo biên nhận thu tiền mặt: " + invoiceCode, Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(requireContext(), PrintInvoiceActivity.class);
                        intent.putExtra("ticket_id", currentTicketId);
                        startActivity(intent);
                    }

                    @Override
                    public void onError(String message) {
                        Intent intent = new Intent(requireContext(), PrintInvoiceActivity.class);
                        intent.putExtra("ticket_id", currentTicketId);
                        startActivity(intent);
                    }
                });
            } else {
                Toast.makeText(requireContext(), "Chưa có phiếu để in biên nhận thanh toán", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnAddEvidencePhoto.setOnClickListener(v -> {
            if (currentTicketId > 0) {
                repository.updateDeviceEvidencePhotos(currentTicketId, "evidence_sample.jpg", new RepairTicketRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        binding.tvEvidencePhotoTitle.setText("📷 Hình Ảnh Minh Chứng Lúc Tiếp Nhận (1 ảnh)");
                        Toast.makeText(requireContext(), "Đã cập nhật ảnh minh chứng tiếp nhận", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(requireContext(), "Hình ảnh minh chứng lúc tiếp nhận: 0 ảnh", Toast.LENGTH_SHORT).show();
            }
        });
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
            showError("Vui lòng nhập số điện thoại hợp lệ");
            return;
        }
        if (deviceName.isEmpty()) {
            showError("Vui lòng nhập tên thiết bị / dịch vụ");
            return;
        }
        if (faultNote.isEmpty()) {
            showError("Vui lòng mô tả chi tiết sự cố phần cứng/phần mềm");
            return;
        }

        binding.btnSubmitQrTicket.setEnabled(false);
        binding.btnSubmitQrTicket.setText("Đang khởi tạo phiếu...");

        int userId = sessionManager.isLoggedIn() ? sessionManager.getUserId() : 1;

        repository.createTicketIntake(
                name,
                phone,
                Constants.DEVICE_TYPE_PHONE,
                deviceName,
                serialTag.isEmpty() ? "Không có" : serialTag,
                "",
                "Tiếp nhận qua mã QR Khách hàng",
                faultNote,
                userId,
                new RepairTicketRepository.TicketCreateCallback() {
                    @Override
                    public void onSuccess(int ticketId, String ticketCode) {
                        binding.btnSubmitQrTicket.setEnabled(true);
                        binding.btnSubmitQrTicket.setText("Tạo Phiếu & Nhận Mã QR ➔");
                        currentTicketId = ticketId;
                        currentTicketCode = ticketCode;
                        showResultView(name, phone, deviceName, faultNote, ticketCode);
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

        binding.edtDeviceName.setText("");
        binding.edtImeiSerial.setText("");
        binding.edtFaultNote.setText("");
        binding.tvFormError.setVisibility(View.GONE);
    }

    private void showQuickSearchDialog() {
        com.google.android.material.textfield.TextInputLayout inputLayout = new com.google.android.material.textfield.TextInputLayout(requireContext());
        inputLayout.setHint("Nhập mã phiếu QR (Ví dụ: FIX-261123-2344 / RP-569689)");
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        inputLayout.setPadding(pad, pad, pad, 0);

        com.google.android.material.textfield.TextInputEditText edtCode = new com.google.android.material.textfield.TextInputEditText(requireContext());
        edtCode.setSingleLine(true);
        inputLayout.addView(edtCode);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("🔍 Tra Cứu Hồ Sơ Phiếu QR")
                .setMessage("Nhập mã phiếu hoặc chọn quét camera để tra cứu hồ sơ sửa chữa & in phiếu.")
                .setView(inputLayout)
                .setNeutralButton("📷 Quét Camera", (d, w) -> {
                    Intent intent = new Intent(requireContext(), vhu.edu.vn.ui.technician.QRScannerActivity.class);
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
                Toast.makeText(requireContext(), "Đã tải xong hồ sơ phiếu #" + currentTicketCode, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
