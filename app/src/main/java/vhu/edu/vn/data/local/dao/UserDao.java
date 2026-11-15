package vhu.edu.vn.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import vhu.edu.vn.data.local.entity.UserEntity;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertUser(UserEntity user);

    @Update
    void updateUser(UserEntity user);

    @Delete
    void deleteUser(UserEntity user);

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    UserEntity login(String username, String password);

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    LiveData<UserEntity> getUserById(int userId);

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    UserEntity getUserByIdSync(int userId);

    @Query("SELECT * FROM users WHERE role = :role ORDER BY fullName ASC")
    LiveData<List<UserEntity>> getUsersByRole(String role);

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    LiveData<List<UserEntity>> getAllUsers();

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    UserEntity getUserByPhone(String phone);

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    UserEntity getUserByUsername(String username);
}
