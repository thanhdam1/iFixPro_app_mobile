package vhu.edu.vn.ui.auth;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import vhu.edu.vn.data.local.entity.UserEntity;
import vhu.edu.vn.data.repository.UserRepository;

public class LoginViewModel extends AndroidViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<UserEntity> loginResult = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository(application);
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<UserEntity> getLoginResult() { return loginResult; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void clearError() {
        errorMessage.setValue(null);
    }

    public void login(String username, String password) {
        if (Boolean.TRUE.equals(isLoading.getValue())) return;
        String cleanUser = username == null ? "" : username.trim();
        String cleanPass = password == null ? "" : password.trim();
        if (cleanUser.isEmpty() || cleanPass.isEmpty()) {
            errorMessage.setValue("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }
        errorMessage.setValue(null);
        isLoading.setValue(true);
        userRepository.login(cleanUser, cleanPass, new UserRepository.DataCallback<UserEntity>() {
            @Override
            public void onSuccess(UserEntity user) {
                isLoading.setValue(false);
                loginResult.setValue(user);
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }
}
