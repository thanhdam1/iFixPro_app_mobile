package vhu.edu.vn.ui.technician;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import vhu.edu.vn.data.local.AppExecutors;
import vhu.edu.vn.data.local.model.TicketWithDetails;
import vhu.edu.vn.data.repository.RepairTicketRepository;
import vhu.edu.vn.databinding.ActivityQrScannerBinding;
import vhu.edu.vn.utils.SessionManager;

public class QRScannerActivity extends AppCompatActivity {
    private ActivityQrScannerBinding binding;
    private RepairTicketRepository repository;
    private boolean lookupPending;
    private boolean hasCameraPermission;
    private boolean permissionRequestedOnce;
    private long lastFailedScanAt;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                permissionRequestedOnce = true;
                hasCameraPermission = Boolean.TRUE.equals(granted);
                updateCameraUi();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityQrScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.qrScannerLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            binding.toolbar.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            binding.cardManualLookup.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn() || !sessionManager.isStaff()) {
            finish();
            return;
        }

        repository = new RepairTicketRepository(this);
        binding.barcodeScanner.setStatusText("");

        binding.btnGrantCamera.setOnClickListener(v -> requestOrOpenCameraSettings());
        binding.btnOpenManualCode.setOnClickListener(v -> submitManualLookup());
        binding.edtManualCode.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilManualCode.setError(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        binding.edtManualCode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitManualLookup();
                return true;
            }
            return false;
        });

        hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        if (!hasCameraPermission) {
            permissionRequestedOnce = true;
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startBarcodeDecode();
        }
        updateCameraUi();
    }

    private void requestOrOpenCameraSettings() {
        boolean shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);
        if (!permissionRequestedOnce || shouldShowRationale) {
            permissionRequestedOnce = true;
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
        }
    }

    private void updateCameraUi() {
        binding.cardCameraPermission.setVisibility(hasCameraPermission ? View.GONE : View.VISIBLE);
        binding.scanReticle.setVisibility(hasCameraPermission ? View.VISIBLE : View.GONE);
        if (hasCameraPermission) {
            startBarcodeDecode();
            binding.barcodeScanner.resume();
        } else {
            boolean permanentlyDenied = permissionRequestedOnce
                    && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);
            binding.btnGrantCamera.setText(permanentlyDenied ? "Mở Cài đặt quyền" : "Cấp quyền Camera");
        }
    }

    private void startBarcodeDecode() {
        binding.barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (lookupPending || result == null || result.getText() == null) return;
                long now = System.currentTimeMillis();
                if (now - lastFailedScanAt < 1800L) return;
                lookupTicketAndOpen(result.getText().trim(), false);
            }
        });
    }

    private void submitManualLookup() {
        String code = binding.edtManualCode.getText() != null
                ? binding.edtManualCode.getText().toString().trim()
                : "";
        if (code.isEmpty()) {
            binding.tilManualCode.setError("Vui lòng nhập mã phiếu cần mở");
            return;
        }
        lookupTicketAndOpen(code, true);
    }

    private void lookupTicketAndOpen(String ticketCode, boolean manualInput) {
        if (lookupPending || ticketCode.isEmpty()) return;
        lookupPending = true;
        binding.btnOpenManualCode.setEnabled(false);

        AppExecutors.getInstance().diskIO().execute(() -> {
            TicketWithDetails found = repository.getTicketByCodeSync(ticketCode);
            AppExecutors.getInstance().mainThread().execute(() -> {
                lookupPending = false;
                binding.btnOpenManualCode.setEnabled(true);
                if (found != null && found.ticket != null) {
                    Intent intent = new Intent(QRScannerActivity.this, DiagnoseActivity.class);
                    intent.putExtra("ticket_id", found.ticket.getId());
                    startActivity(intent);
                    finish();
                } else {
                    lastFailedScanAt = System.currentTimeMillis();
                    String msg = "Không tìm thấy phiếu với mã \"" + ticketCode + "\".";
                    if (manualInput) {
                        binding.tilManualCode.setError(msg);
                    } else {
                        Snackbar.make(binding.qrScannerLayout, msg, Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        updateCameraUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.barcodeScanner.pause();
    }
}
