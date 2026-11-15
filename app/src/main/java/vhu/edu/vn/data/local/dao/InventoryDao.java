package vhu.edu.vn.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import vhu.edu.vn.data.local.entity.InventoryPartEntity;

@Dao
public interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertPart(InventoryPartEntity part);

    @Update
    void updatePart(InventoryPartEntity part);

    @Delete
    void deletePart(InventoryPartEntity part);

    @Query("SELECT * FROM parts_inventory ORDER BY partName ASC")
    LiveData<List<InventoryPartEntity>> getAllParts();

    @Query("SELECT * FROM parts_inventory WHERE id = :partId LIMIT 1")
    InventoryPartEntity getPartByIdSync(int partId);

    @Query("SELECT * FROM parts_inventory WHERE id = :partId LIMIT 1")
    LiveData<InventoryPartEntity> getPartById(int partId);

    @Query("SELECT * FROM parts_inventory WHERE quantity <= minThreshold ORDER BY quantity ASC")
    LiveData<List<InventoryPartEntity>> getLowStockParts();

    @Query("UPDATE parts_inventory SET quantity = quantity - :amount WHERE id = :partId AND quantity >= :amount")
    int decreaseStock(int partId, int amount);

    @Query("UPDATE parts_inventory SET quantity = quantity + :amount WHERE id = :partId")
    void increaseStock(int partId, int amount);

    @Query("SELECT * FROM parts_inventory WHERE partCode = :partCode LIMIT 1")
    InventoryPartEntity getPartByCode(String partCode);
}
