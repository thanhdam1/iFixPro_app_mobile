package vhu.edu.vn.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import vhu.edu.vn.data.local.entity.TicketPartDetailEntity;

@Dao
public interface TicketPartDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertDetail(TicketPartDetailEntity detail);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDetails(List<TicketPartDetailEntity> details);

    @Update
    void updateDetail(TicketPartDetailEntity detail);

    @Delete
    void deleteDetail(TicketPartDetailEntity detail);

    @Query("DELETE FROM ticket_part_details WHERE ticketId = :ticketId")
    void deleteDetailsByTicketId(int ticketId);

    @Query("SELECT * FROM ticket_part_details WHERE ticketId = :ticketId")
    LiveData<List<TicketPartDetailEntity>> getDetailsByTicketId(int ticketId);

    @Query("SELECT * FROM ticket_part_details WHERE ticketId = :ticketId")
    List<TicketPartDetailEntity> getDetailsByTicketIdSync(int ticketId);

    @Query("SELECT COUNT(*) FROM ticket_part_details WHERE partId = :partId")
    int countUsageByPartId(int partId);
}
