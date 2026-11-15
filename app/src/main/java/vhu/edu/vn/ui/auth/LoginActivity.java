package vhu.edu.vn.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import vhu.edu.vn.MainActivity;
import vhu.edu.vn.R;
import vhu.edu.vn.databinding.ActivityLoginBinding;
import vhu.edu.vn.utils.SessionManager;
import vhu.edu.vn.utils.UiFormatters;

public class LoginActivity extends AppCompatActivity {
    public static final String EXTRA_SESSION_EXPIRED = "extra_session_expired";

    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.loginLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        setupObservers();
        setupInputListeners();
        setupDebugDemoChips();

        if (getIntent().getBooleanExtra(EXTRA_SESSION_EXPIRED, false)) {
            binding.cardLoginError.setVisibility(View.VISIBLE);
            binding.tvLoginError.setText(R.string.session_expired);
        }

        binding.btnLogin.setOnClickListener(v -> submitLogin());
        binding.edtPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (binding.btnLogin.isEnabled()) submitLogin();
                return true;
            }
            return false;
        });
    }

    private void setupInputListeners() {
        TextWatcher clearWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.inputLayoutUsername.setError(null);
                binding.inputLayoutPassword.setError(null);
                viewModel.clearError();
            }
            @Override public void afterTextChanged(Editable s) { }
        };
        binding.edtUsername.addTextChangedListener(clearWatcher);
        binding.edtPassword.addTextChangedListener(clearWatcher);
    }

    private void submitLogin() {
        String username = binding.edtUsername.getText() == null ? "" : binding.edtUsername.getText().toString().trim();
        String password = binding.edtPassword.getText() == null ? "" : binding.edtPassword.getText().toString();
        binding.inputLayoutUsername.setError(username.isEmpty() ? getString(R.string.username_required) : null);
        binding.inputLayoutPassword.setError(password.isEmpty() ? getString(R.string.password_required) : null);

        if (username.isEmpty()) {
            binding.edtUsername.requestFocus();
        } else if (password.isEmpty()) {
            binding.edtPassword.requestFocus();
        } else {
            viewModel.login(username, password);
        }
    }

    private void setupDebugDemoChips() {
        if (!UiFormatters.isDebugBuild(this)) {
            binding.cardDebugDemo.setVisibility(View.GONE);
            return;
        }
        binding.cardDebugDemo.setVisibility(View.VISIBLE);
        binding.chipManager.setOnClickListener(v -> populateCredentials("manager", "123456"));
        binding.chipReceptionist.setOnClickListener(v -> populateCredentials("letan", "123456"));
        binding.chipTechnician.setOnClickListener(v -> populateCredentials("kythuat", "123456"));
        binding.chipCustomer.setOnClickListener(v -> populateCredentials("khachhang", "123456"));
    }

    private void populateCredentials(String username, String password) {
        binding.edtUsername.setText(username);
        binding.edtPassword.setText(password);
        binding.inputLayoutUsername.setError(null);
        binding.inputLayoutPassword.setError(null);
        binding.cardLoginError.setVisibility(View.GONE);
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnLogin.setEnabled(!loading);
            binding.btnLogin.setText(loading ? R.string.login_loading : R.string.login_action);
            binding.edtUsername.setEnabled(!loading);
            binding.edtPassword.setEnabled(!loading);
            binding.chipManager.setEnabled(!loading);
            binding.chipReceptionist.setEnabled(!loading);
            binding.chipTechnician.setEnabled(!loading);
            binding.chipCustomer.setEnabled(!loading);
        });

        viewModel.getLoginResult().observe(this, user -> {
            if (user != null) {
                sessionManager.createLoginSession(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getRole()
                );
                navigateToMain();
            }
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                binding.cardLoginError.setVisibility(View.VISIBLE);
                binding.tvLoginError.setText(msg);
            } else {
                binding.cardLoginError.setVisibility(View.GONE);
            }
        });
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
