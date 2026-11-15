package vhu.edu.vn.data.repository;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import vhu.edu.vn.data.local.AppDatabase;
import vhu.edu.vn.data.local.AppExecutors;
import vhu.edu.vn.data.local.entity.DeviceEntity;
import vhu.edu.vn.data.local.entity.InventoryPartEntity;
import vhu.edu.vn.data.local.entity.RepairTicketEntity;
import vhu.edu.vn.data.local.entity.TicketPartDetailEntity;
import vhu.edu.vn.data.local.entity.UserEntity;
import vhu.edu.vn.utils.Constants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class RepairTicketRepositoryTest {
    private AppDatabase db;
    private RepairTicketRepository ticketRepo;
    private InvoiceRepository invoiceRepo;

    private int receptionistId;
    private int techId;
    private int customerId;
    private int deviceId;
    private int partAId;
    private int partBId;

    @Before
    public void setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        AppExecutors syncExecutors = new AppExecutors(Runnable::run, Runnable::run);
        ticketRepo = new RepairTicketRepository(db, syncExecutors);
        invoiceRepo = new InvoiceRepository(db, syncExecutors);

        long now = System.currentTimeMillis();
        receptionistId = (int) db.userDao().insertUser(
                new UserEntity("letan", "123456", "Le Tan", "0900000001", Constants.ROLE_RECEPTIONIST, now));
        techId = (int) db.userDao().insertUser(
                new UserEntity("kythuat", "123456", "Ky Thuat", "0900000002", Constants.ROLE_TECHNICIAN, now));
        customerId = (int) db.userDao().insertUser(
                new UserEntity("khach", "123456", "Khach Hang", "0900000003", Constants.ROLE_CUSTOMER, now));
        deviceId = (int) db.deviceDao().insertDevice(
                new DeviceEntity(customerId, Constants.DEVICE_TYPE_PHONE, "iPhone 14", "IMEI-001", "", "", ""));

        partAId = (int) db.inventoryDao().insertPart(
                new InventoryPartEntity("PART-A", "Màn hình", "Màn hình", 5, 1000.0, 2000.0, 2));
        partBId = (int) db.inventoryDao().insertPart(
                new InventoryPartEntity("PART-B", "Pin", "Pin", 1, 300.0, 600.0, 2));
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void approveTicket_deductsStockOnceAndRejectsSecondApproval() {
        int ticketId = seedWaitingTicket(partAId, 2);

        AtomicReference<String> firstError = new AtomicReference<>();
        ticketRepo.approveTicketAndDeductStock(ticketId, new RepairTicketRepository.ActionCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) { firstError.set(message); }
        });
        assertNull(firstError.get());
        assertEquals(3, db.inventoryDao().getPartByIdSync(partAId).getQuantity());
        assertEquals(Constants.TICKET_APPROVED, db.repairTicketDao().getTicketByIdSync(ticketId).getStatus());

        AtomicReference<String> secondError = new AtomicReference<>();
        ticketRepo.approveTicketAndDeductStock(ticketId, new RepairTicketRepository.ActionCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) { secondError.set(message); }
        });
        assertNotNull(secondError.get());
        assertEquals(3, db.inventoryDao().getPartByIdSync(partAId).getQuantity());
    }

    @Test
    public void approveTicket_rollsBackAllPartsWhenOnePartLacksStock() {
        long now = System.currentTimeMillis();
        int ticketId = (int) db.repairTicketDao().insertTicket(new RepairTicketEntity(
                "FIX-ROLLBACK", deviceId, receptionistId, techId, customerId,
                Constants.TICKET_WAITING_APPROVAL, "Err", "Diag", 100.0, 4600.0, now, now));
        db.ticketPartDetailDao().insertDetails(Arrays.asList(
                new TicketPartDetailEntity(ticketId, partAId, 2, 2000.0),
                new TicketPartDetailEntity(ticketId, partBId, 3, 600.0) // Only 1 in stock!
        ));

        AtomicReference<String> err = new AtomicReference<>();
        ticketRepo.approveTicketAndDeductStock(ticketId, new RepairTicketRepository.ActionCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) { err.set(message); }
        });

        assertNotNull(err.get());
        // Neither partA nor partB was deducted; ticket stays WAITING_APPROVAL
        assertEquals(5, db.inventoryDao().getPartByIdSync(partAId).getQuantity());
        assertEquals(1, db.inventoryDao().getPartByIdSync(partBId).getQuantity());
        assertEquals(Constants.TICKET_WAITING_APPROVAL, db.repairTicketDao().getTicketByIdSync(ticketId).getStatus());
    }

    @Test
    public void rejectOrCancelBeforeApproval_doesNotInflateInventory() {
        int ticketId = seedWaitingTicket(partAId, 2);

        AtomicReference<String> err = new AtomicReference<>();
        ticketRepo.cancelTicketAndRestock(ticketId, Constants.TICKET_REJECTED, new RepairTicketRepository.ActionCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) { err.set(message); }
        });

        assertNull(err.get());
        assertEquals(5, db.inventoryDao().getPartByIdSync(partAId).getQuantity());
        assertEquals(Constants.TICKET_REJECTED, db.repairTicketDao().getTicketByIdSync(ticketId).getStatus());
    }

    @Test
    public void cancelAfterApproval_restocksDeductedInventory() {
        int ticketId = seedWaitingTicket(partAId, 2);
        ticketRepo.approveTicketAndDeductStock(ticketId, new RepairTicketRepository.ActionCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) { throw new AssertionError(message); }
        });
        assertEquals(3, db.inventoryDao().getPartByIdSync(partAId).getQuantity());

        ticketRepo.cancelTicketAndRestock(ticketId, Constants.TICKET_CANCELLED, new RepairTicketRepository.ActionCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) { throw new AssertionError(message); }
        });

        assertEquals(5, db.inventoryDao().getPartByIdSync(partAId).getQuantity());
        assertEquals(Constants.TICKET_CANCELLED, db.repairTicketDao().getTicketByIdSync(ticketId).getStatus());
    }

    @Test
    public void issueInvoice_requiresCompletedStatusAndPreventsDuplicates() {
        int ticketId = seedWaitingTicket(partAId, 1);

        AtomicReference<String> earlyErr = new AtomicReference<>();
        invoiceRepo.issueInvoiceAndDeliver(ticketId, receptionistId, 0.0, Constants.PAYMENT_CASH,
                new InvoiceRepository.InvoiceCreateCallback() {
                    @Override public void onSuccess(String code) { }
                    @Override public void onError(String message) { earlyErr.set(message); }
                });
        assertNotNull(earlyErr.get());

        // Move through workflow properly to COMPLETED
        ticketRepo.approveTicketAndDeductStock(ticketId, noOpCallback());
        ticketRepo.transitionTicketStatus(ticketId, Constants.TICKET_REPAIRING, techId, noOpCallback());
        ticketRepo.transitionTicketStatus(ticketId, Constants.TICKET_COMPLETED, techId, noOpCallback());

        AtomicReference<String> invoiceCode = new AtomicReference<>();
        invoiceRepo.issueInvoiceAndDeliver(ticketId, receptionistId, 500.0, Constants.PAYMENT_QR,
                new InvoiceRepository.InvoiceCreateCallback() {
                    @Override public void onSuccess(String code) { invoiceCode.set(code); }
                    @Override public void onError(String message) { throw new AssertionError(message); }
                });

        assertTrue(invoiceCode.get() != null && invoiceCode.get().startsWith("INV-"));
        assertEquals(Constants.TICKET_DELIVERED, db.repairTicketDao().getTicketByIdSync(ticketId).getStatus());
        assertEquals(1800.0, db.invoiceDao().getInvoiceByTicketIdSync(ticketId).getFinalTotal(), 0.01);
    }

    private int seedWaitingTicket(int partId, int qty) {
        long now = System.currentTimeMillis();
        int ticketId = (int) db.repairTicketDao().insertTicket(new RepairTicketEntity(
                "FIX-" + System.nanoTime(),
                deviceId,
                receptionistId,
                techId,
                customerId,
                Constants.TICKET_WAITING_APPROVAL,
                "Fault",
                "Checked",
                300.0,
                300.0 + (2000.0 * qty),
                now,
                now
        ));
        db.ticketPartDetailDao().insertDetails(Collections.singletonList(
                new TicketPartDetailEntity(ticketId, partId, qty, 2000.0)
        ));
        return ticketId;
    }

    private RepairTicketRepository.ActionCallback noOpCallback() {
        return new RepairTicketRepository.ActionCallback() {
            @Override public void onSuccess() { }
            @Override public void onError(String message) { throw new AssertionError(message); }
        };
    }
}
