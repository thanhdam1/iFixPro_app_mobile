package vhu.edu.vn.data.repository;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import androidx.lifecycle.LiveData;
import java.util.List;
import vhu.edu.vn.data.local.AppDatabase;
import vhu.edu.vn.data.local.AppExecutors;
import vhu.edu.vn.data.local.dao.UserDao;
import vhu.edu.vn.data.local.entity.UserEntity;

public class UserRepository {
    private final UserDao userDao;
    private final AppExecutors executors;

    public interface DataCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public UserRepository(Context context) {
        this(AppDatabase.getInstance(context), AppExecutors.getInstance());
    }

    public UserRepository(AppDatabase database, AppExecutors executors) {
        this.userDao = database.userDao();
        this.executors = executors;
    }

    public void login(String username, String password, DataCallback<UserEntity> callback) {
        executors.diskIO().execute(() -> {
            try {
                UserEntity user = userDao.login(username, password);
                executors.mainThread().execute(() -> {
                    if (user != null) {
                        callback.onSuccess(user);
                    } else {
                        callback.onError("Tên đăng nhập hoặc mật khẩu không chính xác.");
                    }
                });
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError("Không thể đăng nhập: " + e.getMessage()));
            }
        });
    }

    public void verifyUserExists(int userId, DataCallback<UserEntity> callback) {
        executors.diskIO().execute(() -> {
            UserEntity user = userDao.getUserByIdSync(userId);
            executors.mainThread().execute(() -> {
                if (user != null) {
                    callback.onSuccess(user);
                } else {
                    callback.onError("Tài khoản không còn tồn tại trong hệ thống.");
                }
            });
        });
    }

    public LiveData<UserEntity> getUserById(int userId) {
        return userDao.getUserById(userId);
    }

    public void insertUser(UserEntity user, DataCallback<Long> callback) {
        executors.diskIO().execute(() -> {
            try {
                if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                    throw new BusinessException("Vui lòng nhập tên đăng nhập.");
                }
                if (user.getPassword() == null || user.getPassword().trim().length() < 4) {
                    throw new BusinessException("Mật khẩu cần tối thiểu 4 ký tự.");
                }
                if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
                    throw new BusinessException("Vui lòng nhập họ và tên.");
                }
                String cleanUsername = user.getUsername().trim();
                if (userDao.getUserByUsername(cleanUsername) != null) {
                    throw new BusinessException("Tên đăng nhập \"" + cleanUsername + "\" đã tồn tại.");
                }
                user.setUsername(cleanUsername);
                user.setFullName(user.getFullName().trim());
                user.setPhone(user.getPhone() != null ? user.getPhone().trim() : "");
                long userId = userDao.insertUser(user);
                executors.mainThread().execute(() -> callback.onSuccess(userId));
            } catch (Exception e) {
                String msg;
                if (e instanceof BusinessException && e.getMessage() != null) {
                    msg = e.getMessage();
                } else if (e instanceof SQLiteConstraintException) {
                    msg = "Tên đăng nhập đã tồn tại trong hệ thống.";
                } else {
                    msg = "Không thể tạo tài khoản: " + e.getMessage();
                }
                executors.mainThread().execute(() -> callback.onError(msg));
            }
        });
    }

    public LiveData<List<UserEntity>> getUsersByRole(String role) {
        return userDao.getUsersByRole(role);
    }

    public LiveData<List<UserEntity>> getAllUsers() {
        return userDao.getAllUsers();
    }
}
