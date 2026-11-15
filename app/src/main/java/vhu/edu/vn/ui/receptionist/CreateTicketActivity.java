package vhu.edu.vn.ui.receptionist;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import vhu.edu.vn.R;
import vhu.edu.vn.data.repository.RepairTicketRepository;
import vhu.edu.vn.databinding.ActivityCreateTicketBinding;
import vhu.edu.vn.utils.Constants;
import vhu.edu.vn.utils.QRCodeUtils;
import vhu.edu.vn.utils.SessionManager;

public class CreateTicketActivity extends AppCompatActivity {
    private ActivityCreateTicketBinding binding;
    private RepairTicketRepository repository;
    private SessionManager sessionManager;
    private boolean isSubmitting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCreateTicketBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.createTicketLayout, (v, insets) -> {
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

        repository = new RepairTicketRepository(this);
        setupClearErrorWatchers();
        binding.btnSubmitTicket.setOnClickListener(v -> submitTicket());
    }

    private void setupClearErrorWatchers() {
        TextWatcher clearWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilCustomerPhone.setError(null);
                binding.tilBrandModel.setError(null);
                binding.tilImeiSerial.setError(null);
                binding.tilCustomerNote.setError(null);
                binding.cardCreateError.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) { }
        };
        binding.edtCustomerPhone.addTextChangedListener(clearWatcher);
        binding.edtBrandModel.addTextChangedListener(clearWatcher);
        binding.edtImeiSerial.addTextChangedListener(clearWatcher);
        binding.edtCustomerNote.addTextChangedListener(clearWatcher);
    }

    private void submitTicket() {
        if (isSubmitting) return;

        String phone = textOf(binding.edtCustomerPhone);
        String name = textOf(binding.edtCustomerName);
        String brandModel = textOf(binding.edtBrandModel);
        String imei = textOf(binding.edtImeiSerial);
        String screenPassword = textOf(binding.edtScreenPassword);
        String initialCondition = textOf(binding.edtInitialCondition);
        String customerNote = textOf(binding.edtCustomerNote);

        boolean valid = true;
        if (phone.isEmpty() || phone.length() < 8) {
            binding.tilCustomerPhone.setError("Vui lòng nhập số điện thoại hợp lệ");
            valid = false;
        }
        if (brandModel.isEmpty()) {
            binding.tilBrandModel.setError("Vui lòng nhập hãng và tên thiết bị");
            valid = false;
        }
        if (imei.isEmpty()) {
            binding.tilImeiSerial.setError("Vui lòng nhập IMEI hoặc số Serial");
            valid = false;
        }
        if (customerNote.isEmpty()) {
            binding.tilCustomerNote.setError("Vui lòng mô tả lỗi khách hàng phản ánh");
            valid = false;
        }
        if (!valid) return;

        String deviceType = resolveDeviceType();
        setSubmitting(true);

        repository.createTicketIntake(
                name,
                phone,
                deviceType,
                brandModel,
                imei,
                screenPassword,
                initialCondition,
                customerNote,
                sessionManager.getUserId(),
                new RepairTicketRepository.TicketCreateCallback() {
                    @Override
                    public void onSuccess(int ticketId, String ticketCode) {
                        setSubmitting(false);
                        showCreatedTicketDialog(ticketId, ticketCode);
                    }

                    @Override
                    public void onError(String message) {
                        setSubmitting(false);
                        binding.cardCreateError.setVisibility(View.VISIBLE);
                        binding.tvCreateError.setText(message);
                    }
                }
        );
    }

    private void showCreatedTicketDialog(int ticketId, String ticketCode) {
        ImageView qrPreview = new ImageView(this);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        qrPreview.setPadding(pad, pad, pad, 0);
        Bitmap bitmap = QRCodeUtils.generateQRCodeBitmap(ticketCode, 460, 460);
        if (bitmap != null) {
            qrPreview.setImageBitmap(bitmap);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle("Đã tiếp nhận phiếu " + ticketCode)
                .setMessage("Thiết bị đã được lưu vào hệ thống. Bạn có thể in tem QR hoặc mở ngay chi tiết phiếu.")
                .setCancelable(false)
                .setNegativeButton(R.string.action_close, (d, w) -> finish())
                .setPositiveButton("Xem chi tiết", (d, w) -> {
                    Intent intent = new Intent(this, PrintInvoiceActivity.class);
                    intent.putExtra("ticket_id", ticketId);
                    startActivity(intent);
                    finish();
                });
        if (bitmap != null) {
            builder.setView(qrPreview);
        }
        builder.show();
    }

    private String resolveDeviceType() {
        if (binding.chipLaptop.isChecked()) return Constants.DEVICE_TYPE_LAPTOP;
        if (binding.chipTablet.isChecked()) return Constants.DEVICE_TYPE_TABLET;
        return Constants.DEVICE_TYPE_PHONE;
    }

    private void setSubmitting(boolean submitting) {
        isSubmitting = submitting;
        binding.btnSubmitTicket.setEnabled(!submitting);
        binding.btnSubmitTicket.setText(submitting ? R.string.saving_data : R.string.create_ticket_action);
    }

    private String textOf(com.google.android.material.textfield.TextInputEditText edt) {
        return edt.getText() != null ? edt.getText().toString().trim() : "";
    }
}
