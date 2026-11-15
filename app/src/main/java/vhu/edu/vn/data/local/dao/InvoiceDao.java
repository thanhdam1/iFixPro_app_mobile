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
import vhu.edu.vn.data.local.entity.InvoiceEntity;
import vhu.edu.vn.data.local.model.InvoiceWithTicket;

@Dao
public interface InvoiceDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertInvoice(InvoiceEntity invoice);

    @Update
    void updateInvoice(InvoiceEntity invoice);

    @Delete
    void deleteInvoice(InvoiceEntity invoice);

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :invoiceId LIMIT 1")
    LiveData<InvoiceWithTicket> getInvoiceWithTicketById(int invoiceId);

    @Transaction
    @Query("SELECT * FROM invoices WHERE ticketId = :ticketId LIMIT 1")
    LiveData<InvoiceWithTicket> getInvoiceByTicketId(int ticketId);

    @Query("SELECT * FROM invoices WHERE ticketId = :ticketId LIMIT 1")
    InvoiceEntity getInvoiceByTicketIdSync(int ticketId);

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    LiveData<List<InvoiceWithTicket>> getAllInvoicesWithTicket();

    @Query("SELECT SUM(finalTotal) FROM invoices")
    LiveData<Double> getTotalRevenue();

    @Query("SELECT SUM(finalTotal) FROM invoices WHERE createdAt >= :startTime AND createdAt <= :endTime")
    LiveData<Double> getRevenueBetweenDates(long startTime, long endTime);
}
