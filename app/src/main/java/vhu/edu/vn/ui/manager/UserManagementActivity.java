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
import vhu.edu.vn.data.local.entity.UserEntity;
import vhu.edu.vn.data.repository.UserRepository;
import vhu.edu.vn.databinding.ActivityUserManagementBinding;
import vhu.edu.vn.databinding.DialogAddUserBinding;
import vhu.edu.vn.ui.adapter.UserAdapter;
import vhu.edu.vn.utils.Constants;
import vhu.edu.vn.utils.SessionManager;
import vhu.edu.vn.utils.TicketSearch;

public class UserManagementActivity extends AppCompatActivity {
    private ActivityUserManagementBinding binding;
    private UserRepository repository;
    private UserAdapter adapter;
    private List<UserEntity> allUsers = Collections.emptyList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityUserManagementBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.userManagementLayout, (v, insets) -> {
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

        repository = new UserRepository(this);
        adapter = new UserAdapter();
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUsers.setAdapter(adapter);

        binding.edtSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        binding.chipUserRoleFilter.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilter());
        binding.fabAddUser.setOnClickListener(v -> showAddUserDialog());

        repository.getAllUsers().observe(this, users -> {
            allUsers = users != null ? users : Collections.emptyList();
            applyFilter();
        });
    }

    private void applyFilter() {
        String query = binding.edtSearchUsers.getText() != null ? binding.edtSearchUsers.getText().toString() : "";
        String roleFilter = resolveRoleFilter();

        List<UserEntity> filtered = new ArrayList<>();
        for (UserEntity user : allUsers) {
            if (roleFilter != null && !roleFilter.equals(user.getRole())) continue;
            if (TicketSearch.matches(query, user.getFullName(), user.getUsername(), user.getPhone())) {
                filtered.add(user);
            }
        }

        adapter.setUsers(filtered);
        binding.tvUserResultCount.setText("Hiển thị " + filtered.size() + " / " + allUsers.size() + " tài khoản");
        binding.tvUserEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String resolveRoleFilter() {
        if (binding.chipRoleManager.isChecked()) return Constants.ROLE_MANAGER;
        if (binding.chipRoleReceptionist.isChecked()) return Constants.ROLE_RECEPTIONIST;
        if (binding.chipRoleTech.isChecked()) return Constants.ROLE_TECHNICIAN;
        if (binding.chipRoleCustomer.isChecked()) return Constants.ROLE_CUSTOMER;
        return null;
    }

    private void showAddUserDialog() {
        DialogAddUserBinding db = DialogAddUserBinding.inflate(getLayoutInflater());

        TextWatcher clearWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                db.tilNewUsername.setError(null);
                db.tilNewPassword.setError(null);
                db.tilNewFullName.setError(null);
                db.tilNewPhone.setError(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        };
        db.edtNewUsername.addTextChangedListener(clearWatcher);
        db.edtNewPassword.addTextChangedListener(clearWatcher);
        db.edtNewFullName.addTextChangedListener(clearWatcher);
        db.edtNewPhone.addTextChangedListener(clearWatcher);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Thêm tài khoản nhân sự")
                .setView(db.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton("Tạo tài khoản", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button createBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            createBtn.setOnClickListener(v -> {
                String username = db.edtNewUsername.getText() != null ? db.edtNewUsername.getText().toString().trim() : "";
                String password = db.edtNewPassword.getText() != null ? db.edtNewPassword.getText().toString().trim() : "";
                String fullName = db.edtNewFullName.getText() != null ? db.edtNewFullName.getText().toString().trim() : "";
                String phone = db.edtNewPhone.getText() != null ? db.edtNewPhone.getText().toString().trim() : "";

                boolean valid = true;
                if (username.isEmpty() || username.contains(" ")) {
                    db.tilNewUsername.setError("Tên đăng nhập không chứa khoảng trắng");
                    valid = false;
                }
                if (password.length() < 4) {
                    db.tilNewPassword.setError("Mật khẩu cần tối thiểu 4 ký tự");
                    valid = false;
                }
                if (fullName.isEmpty()) {
                    db.tilNewFullName.setError("Vui lòng nhập họ và tên");
                    valid = false;
                }
                if (phone.isEmpty() || phone.length() < 8) {
                    db.tilNewPhone.setError("Vui lòng nhập số điện thoại hợp lệ");
                    valid = false;
                }
                if (!valid) return;

                String role = Constants.ROLE_RECEPTIONIST;
                if (db.chipSelectTechnician.isChecked()) {
                    role = Constants.ROLE_TECHNICIAN;
                } else if (db.chipSelectManager.isChecked()) {
                    role = Constants.ROLE_MANAGER;
                }

                createBtn.setEnabled(false);
                UserEntity newUser = new UserEntity(username, password, fullName, phone, role, System.currentTimeMillis());
                repository.insertUser(newUser, new UserRepository.DataCallback<Long>() {
                    @Override
                    public void onSuccess(Long result) {
                        dialog.dismiss();
                        Snackbar.make(binding.userManagementLayout,
                                "Đã tạo tài khoản cho " + fullName + ".",
                                Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        createBtn.setEnabled(true);
                        new MaterialAlertDialogBuilder(UserManagementActivity.this)
                                .setTitle("Không thể tạo tài khoản")
                                .setMessage(message)
                                .setPositiveButton(R.string.action_close, null)
                                .show();
                    }
                });
            });
        });

        dialog.show();
    }
}
