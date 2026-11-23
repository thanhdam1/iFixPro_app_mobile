package vhu.edu.vn.ui.receptionist;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.util.Patterns;
import androidx.activity.OnBackPressedCallback;
import androidx.lifecycle.ViewModelProvider;
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
    private IntakeSubmission submission;
    private int step;
    private static final int LAST_STEP = 3;
    private static final int[] TITLES = {
            R.string.intake_device_title, R.string.intake_issue_title,
            R.string.intake_visit_title, R.string.intake_contact_title
    };
    private static final int[] HINTS = {
            R.string.intake_device_hint, R.string.intake_issue_hint,
            R.string.intake_visit_hint, R.string.intake_contact_hint
    };

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

        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn() || !sessionManager.isStaff()) {
            finish();
            return;
        }

        repository = new RepairTicketRepository(this);
        setupClearErrorWatchers();
        submission = new ViewModelProvider(this).get(IntakeSubmission.class);
        step = savedInstanceState == null ? 0 : savedInstanceState.getInt("intakeStep", 0);
        showStep();
        binding.btnSubmitTicket.setOnClickListener(v -> advance());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (submission.pending) return;
                if (step == 0) finish();
                else {
                    step--;
                    showStep();
                }
            }
        });
        submission.createdCode.observe(this, code -> showCreatedTicketDialog(submission.ticketId, code));
        submission.error.observe(this, message -> {
            if (message == null) return;
            showStep();
            binding.tvCreateError.setText(message);
            binding.cardCreateError.setVisibility(View.VISIBLE);
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("intakeStep", step);
    }

    private void showStep() {
        View[] pages = { binding.pageDevice, binding.pageIssue, binding.pageVisit, binding.pageContact };
        ImageView[] icons = { binding.iconDevice, binding.iconIssue, binding.iconVisit, binding.iconContact };
        for (int i = 0; i < pages.length; i++) {
            pages[i].setVisibility(i == step ? View.VISIBLE : View.GONE);
            int fill = getColor(i == step ? R.color.intake_violet : R.color.intake_lilac);
            int ink = getColor(i == step ? android.R.color.white : R.color.intake_violet);
            icons[i].setBackgroundTintList(ColorStateList.valueOf(fill));
            icons[i].setImageTintList(ColorStateList.valueOf(ink));
        }
        binding.stepTitle.setText(TITLES[step]);
        binding.stepHint.setText(HINTS[step]);
        binding.cardCreateError.setVisibility(View.GONE);
        binding.btnSubmitTicket.setEnabled(!submission.pending && submission.createdCode.getValue() == null);
        binding.btnSubmitTicket.setText(submission.pending ? R.string.saving_data
                : step == LAST_STEP ? R.string.intake_submit : R.string.intake_next);
        binding.intakeScroll.post(() -> binding.intakeScroll.scrollTo(0, 0));
    }

    private void advance() {
        if (submission.pending || submission.createdCode.getValue() != null) return;
        if (step == 0 && textOf(binding.edtBrandModel).isEmpty()) {
            binding.tilBrandModel.setError("Vui lòng nhập hãng và tên thiết bị");
            binding.edtBrandModel.requestFocus();
            return;
        }
        if (step == 0 && textOf(binding.edtImeiSerial).isEmpty()) {
            binding.tilImeiSerial.setError("Vui lòng nhập IMEI hoặc số Serial");
            binding.edtImeiSerial.requestFocus();
            return;
        }
        if (step == 1 && textOf(binding.edtCustomerNote).isEmpty()) {
            binding.tilCustomerNote.setError("Vui lòng mô tả lỗi khách hàng phản ánh");
            binding.edtCustomerNote.requestFocus();
            return;
        }
        if (step < LAST_STEP) {
            step++;
            showStep();
            return;
        }
        submitTicket();
    }

    private void setupClearErrorWatchers() {
        TextWatcher clearWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilCustomerPhone.setError(null);
                binding.tilEmail.setError(null);
                binding.tilBrandModel.setError(null);
                binding.tilImeiSerial.setError(null);
                binding.tilCustomerNote.setError(null);
                binding.cardCreateError.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) { }
        };
        binding.edtCustomerPhone.addTextChangedListener(clearWatcher);
        binding.edtEmail.addTextChangedListener(clearWatcher);
        binding.edtBrandModel.addTextChangedListener(clearWatcher);
        binding.edtImeiSerial.addTextChangedListener(clearWatcher);
        binding.edtCustomerNote.addTextChangedListener(clearWatcher);
    }

    private void submitTicket() {
        String phone = textOf(binding.edtCustomerPhone);
        String name = textOf(binding.edtCustomerName);
        String brandModel = textOf(binding.edtBrandModel);
        String imei = textOf(binding.edtImeiSerial);
        String screenPassword = textOf(binding.edtScreenPassword);
        String initialCondition = textOf(binding.edtInitialCondition);
        String customerNote = textOf(binding.edtCustomerNote);

        if (phone.length() < 8) {
            binding.tilCustomerPhone.setError("Vui lòng nhập số điện thoại hợp lệ");
            binding.edtCustomerPhone.requestFocus();
            return;
        }
        String email = textOf(binding.edtEmail);
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.intake_invalid_email));
            binding.edtEmail.requestFocus();
            return;
        }
        String visit = textOf(binding.edtVisit);
        if (!visit.isEmpty()) customerNote += "\n" + getString(R.string.intake_note_visit) + ": " + visit;
        if (!email.isEmpty()) customerNote += "\n" + getString(R.string.intake_note_email) + ": " + email;
        String deviceType = resolveDeviceType();
        submission.error.setValue(null);
        submission.pending = true;
        showStep();

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
                submission
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

    private String textOf(com.google.android.material.textfield.TextInputEditText edt) {
        return edt.getText() != null ? edt.getText().toString().trim() : "";
    }
}
