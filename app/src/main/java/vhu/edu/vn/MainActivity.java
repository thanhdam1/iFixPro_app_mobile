package vhu.edu.vn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import vhu.edu.vn.data.local.entity.UserEntity;
import vhu.edu.vn.data.repository.UserRepository;
import vhu.edu.vn.databinding.ActivityMainBinding;
import vhu.edu.vn.databinding.ViewAccountSheetBinding;
import vhu.edu.vn.ui.auth.LoginActivity;
import vhu.edu.vn.ui.customer.CustomerHomeFragment;
import vhu.edu.vn.ui.manager.InventoryManagementActivity;
import vhu.edu.vn.ui.manager.ManagerHomeFragment;
import vhu.edu.vn.ui.manager.UserManagementActivity;
import vhu.edu.vn.ui.receptionist.CreateTicketActivity;
import vhu.edu.vn.ui.receptionist.ReceptionistHomeFragment;
import vhu.edu.vn.ui.technician.QRScannerActivity;
import vhu.edu.vn.ui.technician.TechnicianHomeFragment;
import vhu.edu.vn.utils.Constants;
import vhu.edu.vn.utils.SessionManager;
import vhu.edu.vn.utils.UiFormatters;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private UserRepository userRepository;
    private UserEntity currentUser;
    private int homeMenuId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        userRepository = new UserRepository(this);

        if (!sessionManager.isLoggedIn()) {
            redirectExpiredSession(false);
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.appBarLayout.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            binding.bottomNavigation.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            if (binding.bottomNavigation.getVisibility() == View.GONE) {
                binding.fragmentContainer.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            } else {
                binding.fragmentContainer.setPadding(systemBars.left, 0, systemBars.right, 0);
            }
            return insets;
        });

        String role = sessionManager.getRole();
        String fullName = sessionManager.getFullName();

        binding.topAppBar.setTitle(R.string.app_name);
        binding.topAppBar.setSubtitle(UiFormatters.roleDisplayName(this, role) + " · " + fullName);
        binding.topAppBar.inflateMenu(R.menu.menu_main_top);

        setupRoleNavigation(role);
        verifySessionWithDatabase();

        binding.topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_account) {
                showAccountBottomSheet();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && homeMenuId != 0 && binding.bottomNavigation.getVisibility() == View.VISIBLE) {
            binding.bottomNavigation.setSelectedItemId(homeMenuId);
        }
    }

    private void verifySessionWithDatabase() {
        int userId = sessionManager.getUserId();
        userRepository.verifyUserExists(userId, new UserRepository.DataCallback<UserEntity>() {
            @Override
            public void onSuccess(UserEntity user) {
                currentUser = user;
                if (binding != null) {
                    binding.topAppBar.setSubtitle(
                            UiFormatters.roleDisplayName(MainActivity.this, user.getRole()) + " · " + user.getFullName()
                    );
                }
            }

            @Override
            public void onError(String message) {
                redirectExpiredSession(true);
            }
        });
    }

    private void setupRoleNavigation(String role) {
        if (Constants.ROLE_RECEPTIONIST.equals(role)) {
            homeMenuId = R.id.nav_receptionist_home;
            binding.bottomNavigation.inflateMenu(R.menu.menu_receptionist);
            loadFragment(new ReceptionistHomeFragment());
        } else if (Constants.ROLE_TECHNICIAN.equals(role)) {
            homeMenuId = R.id.nav_tech_home;
            binding.bottomNavigation.inflateMenu(R.menu.menu_technician);
            loadFragment(new TechnicianHomeFragment());
        } else if (Constants.ROLE_CUSTOMER.equals(role)) {
            homeMenuId = 0;
            binding.bottomNavigation.setVisibility(View.GONE);
            loadFragment(new CustomerHomeFragment());
        } else if (Constants.ROLE_MANAGER.equals(role)) {
            homeMenuId = R.id.nav_manager_dashboard;
            binding.bottomNavigation.inflateMenu(R.menu.menu_manager);
            loadFragment(new ManagerHomeFragment());
        } else {
            redirectExpiredSession(true);
            return;
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent intent = null;
            if (Constants.ROLE_MANAGER.equals(role)) {
                if (id == R.id.nav_manager_inventory) {
                    intent = new Intent(this, InventoryManagementActivity.class);
                } else if (id == R.id.nav_manager_users) {
                    intent = new Intent(this, UserManagementActivity.class);
                } else {
                    return id == R.id.nav_manager_dashboard;
                }
            } else if (Constants.ROLE_RECEPTIONIST.equals(role)) {
                if (id == R.id.nav_receptionist_create) {
                    intent = new Intent(this, CreateTicketActivity.class);
                } else {
                    return id == R.id.nav_receptionist_home;
                }
            } else if (Constants.ROLE_TECHNICIAN.equals(role)) {
                if (id == R.id.nav_tech_scan) {
                    intent = new Intent(this, QRScannerActivity.class);
                } else {
                    return id == R.id.nav_tech_home;
                }
            }
            if (intent != null) {
                startActivity(intent);
            }
            return false;
        });

        binding.bottomNavigation.setOnItemReselectedListener(item -> { });
    }

    private void showAccountBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        ViewAccountSheetBinding sheetBinding = ViewAccountSheetBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());

        String fullName = currentUser != null ? currentUser.getFullName() : sessionManager.getFullName();
        String username = currentUser != null ? currentUser.getUsername() : sessionManager.getUsername();
        String phone = currentUser != null && currentUser.getPhone() != null && !currentUser.getPhone().isEmpty()
                ? currentUser.getPhone()
                : "Chưa cập nhật";
        String role = currentUser != null ? currentUser.getRole() : sessionManager.getRole();

        sheetBinding.tvAccountInitials.setText(UiFormatters.initials(fullName));
        sheetBinding.tvAccountName.setText(fullName);
        sheetBinding.tvAccountRole.setText(UiFormatters.roleDisplayName(this, role));
        sheetBinding.tvAccountUsername.setText("@" + username);
        sheetBinding.tvAccountPhone.setText(phone);

        sheetBinding.btnSheetLogout.setOnClickListener(v -> {
            dialog.dismiss();
            confirmLogout();
        });

        dialog.show();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_logout_title)
                .setMessage(R.string.confirm_logout_body)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_logout, (d, w) -> {
                    sessionManager.logoutUser();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private void loadFragment(Fragment fragment) {
        if (getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) != null) return;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void redirectExpiredSession(boolean showExpiredBanner) {
        sessionManager.logoutUser();
        Intent intent = new Intent(this, LoginActivity.class);
        if (showExpiredBanner) {
            intent.putExtra(LoginActivity.EXTRA_SESSION_EXPIRED, true);
        }
        startActivity(intent);
        finish();
    }
}
