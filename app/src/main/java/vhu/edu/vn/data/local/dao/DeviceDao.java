package vhu.edu.vn.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import vhu.edu.vn.data.local.entity.DeviceEntity;

@Dao
public interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertDevice(DeviceEntity device);

    @Update
    void updateDevice(DeviceEntity device);

    @Delete
    void deleteDevice(DeviceEntity device);

    @Query("SELECT * FROM devices WHERE customerId = :customerId")
    LiveData<List<DeviceEntity>> getDevicesByCustomerId(int customerId);

    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    DeviceEntity getDeviceByIdSync(int id);

    @Query("SELECT * FROM devices WHERE imeiSerial = :imeiSerial LIMIT 1")
    DeviceEntity getDeviceByImei(String imeiSerial);
}
