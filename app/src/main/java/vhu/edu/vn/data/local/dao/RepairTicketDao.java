package vhu.edu.vn.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import java.util.List;
import vhu.edu.vn.data.local.entity.RepairTicketEntity;
import vhu.edu.vn.data.local.model.TicketWithDetails;

@Dao
public interface RepairTicketDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertTicket(RepairTicketEntity ticket);

    @Update
    void updateTicket(RepairTicketEntity ticket);

    @Delete
    void deleteTicket(RepairTicketEntity ticket);

    @Query("SELECT * FROM repair_tickets WHERE id = :ticketId LIMIT 1")
    RepairTicketEntity getTicketByIdSync(int ticketId);

    @Transaction
    @Query("SELECT * FROM repair_tickets WHERE id = :ticketId LIMIT 1")
    LiveData<TicketWithDetails> getTicketWithDetailsById(int ticketId);

    @Transaction
    @Query("SELECT * FROM repair_tickets WHERE ticketCode = :ticketCode LIMIT 1")
    TicketWithDetails getTicketWithDetailsByCodeSync(String ticketCode);

    @Transaction
    @Query("SELECT * FROM repair_tickets WHERE customerId = :customerId ORDER BY updatedAt DESC, createdAt DESC")
    LiveData<List<TicketWithDetails>> getTicketsByCustomerId(int customerId);

    @Transaction
    @Query("SELECT * FROM repair_tickets WHERE technicianId = :technicianId ORDER BY updatedAt DESC, createdAt DESC")
    LiveData<List<TicketWithDetails>> getTicketsByTechnicianId(int technicianId);

    @Transaction
    @Query("SELECT * FROM repair_tickets WHERE status = :status ORDER BY updatedAt DESC, createdAt DESC")
    LiveData<List<TicketWithDetails>> getTicketsByStatus(String status);

    @Transaction
    @Query("SELECT * FROM repair_tickets ORDER BY updatedAt DESC, createdAt DESC")
    LiveData<List<TicketWithDetails>> getAllTicketsWithDetails();

    @Query("UPDATE repair_tickets SET status = :status, updatedAt = :updatedAt WHERE id = :ticketId")
    void updateTicketStatus(int ticketId, String status, long updatedAt);

    @Query("UPDATE repair_tickets SET technicianId = :technicianId, status = :status, updatedAt = :updatedAt WHERE id = :ticketId")
    void assignTechnician(int ticketId, Integer technicianId, String status, long updatedAt);

    @Query("UPDATE repair_tickets SET technicianId = :technicianId, laborFee = :laborFee, totalCost = :totalCost, technicianNote = :technicianNote, status = :status, updatedAt = :updatedAt WHERE id = :ticketId")
    void updateDiagnosis(int ticketId, Integer technicianId, double laborFee, double totalCost, String technicianNote, String status, long updatedAt);
}
