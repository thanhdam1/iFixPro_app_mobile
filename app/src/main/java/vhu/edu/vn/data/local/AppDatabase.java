package vhu.edu.vn.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import vhu.edu.vn.data.local.dao.DeviceDao;
import vhu.edu.vn.data.local.dao.InventoryDao;
import vhu.edu.vn.data.local.dao.InvoiceDao;
import vhu.edu.vn.data.local.dao.RepairTicketDao;
import vhu.edu.vn.data.local.dao.TicketPartDetailDao;
import vhu.edu.vn.data.local.dao.UserDao;
import vhu.edu.vn.data.local.entity.DeviceEntity;
import vhu.edu.vn.data.local.entity.InventoryPartEntity;
import vhu.edu.vn.data.local.entity.InvoiceEntity;
import vhu.edu.vn.data.local.entity.RepairTicketEntity;
import vhu.edu.vn.data.local.entity.TicketPartDetailEntity;
import vhu.edu.vn.data.local.entity.UserEntity;
import vhu.edu.vn.utils.Constants;

@Database(
    entities = {
        UserEntity.class,
        DeviceEntity.class,
        RepairTicketEntity.class,
        InventoryPartEntity.class,
        TicketPartDetailEntity.class,
        InvoiceEntity.class
    },
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "ifixpro_db";
    private static volatile AppDatabase sInstance;

    public abstract UserDao userDao();
    public abstract DeviceDao deviceDao();
    public abstract RepairTicketDao repairTicketDao();
    public abstract InventoryDao inventoryDao();
    public abstract TicketPartDetailDao ticketPartDetailDao();
    public abstract InvoiceDao invoiceDao();

    public static AppDatabase getInstance(final Context context) {
        if (sInstance == null) {
            synchronized (AppDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            AppExecutors.getInstance().diskIO().execute(() -> populateInitialData(getInstance(context)));
                        }
                    })
                    .build();
                }
            }
        }
        return sInstance;
    }

    private static void populateInitialData(AppDatabase database) {
        long now = System.currentTimeMillis();
        long oneHour = 60L * 60L * 1000L;
        long oneDay = 24L * oneHour;

        // 1. Seed Users
        UserEntity manager = new UserEntity("manager", "123456", "Nguyễn Văn Quản Lý", "0901234567", Constants.ROLE_MANAGER, now - 30 * oneDay);
        UserEntity receptionist = new UserEntity("letan", "123456", "Trần Thị Lễ Tân", "0912345678", Constants.ROLE_RECEPTIONIST, now - 30 * oneDay);
        UserEntity technician = new UserEntity("kythuat", "123456", "Lê Văn Kỹ Thuật", "0923456789", Constants.ROLE_TECHNICIAN, now - 30 * oneDay);
        UserEntity customer = new UserEntity("khachhang", "123456", "Phạm Hoàng Khách", "0934567890", Constants.ROLE_CUSTOMER, now - 10 * oneDay);

        int managerId = (int) database.userDao().insertUser(manager);
        int receptionistId = (int) database.userDao().insertUser(receptionist);
        int techId = (int) database.userDao().insertUser(technician);
        int customerId = (int) database.userDao().insertUser(customer);

        // 2. Seed Inventory Parts
        int screenId = (int) database.inventoryDao().insertPart(new InventoryPartEntity("PART-001", "Màn hình iPhone 14 Pro Max", "Màn hình", 10, 2500000.0, 3200000.0, 5));
        int batteryPhoneId = (int) database.inventoryDao().insertPart(new InventoryPartEntity("PART-002", "Pin iPhone 13 Pro", "Pin", 15, 450000.0, 750000.0, 5));
        int batteryLaptopId = (int) database.inventoryDao().insertPart(new InventoryPartEntity("PART-003", "Pin Laptop Dell XPS 15", "Pin", 3, 850000.0, 1200000.0, 5));
        database.inventoryDao().insertPart(new InventoryPartEntity("PART-004", "RAM Laptop DDR4 8GB Kingston", "RAM", 20, 350000.0, 550000.0, 5));
        int ssdId = (int) database.inventoryDao().insertPart(new InventoryPartEntity("PART-005", "SSD NVMe 512GB Samsung", "Ổ cứng", 2, 900000.0, 1350000.0, 5));

        // 3. Seed Sample Devices & Tickets across workflow stages
        int device1Id = (int) database.deviceDao().insertDevice(
                new DeviceEntity(customerId, Constants.DEVICE_TYPE_PHONE, "iPhone 14 Pro Max", "358912345678901", "1234", "Trầy nhẹ viền khung thép", "")
        );
        int device2Id = (int) database.deviceDao().insertDevice(
                new DeviceEntity(customerId, Constants.DEVICE_TYPE_LAPTOP, "Dell XPS 15 9520", "DLXPS95209988", "", "Kèm sạc USB-C 130W, vỏ nhôm sạch", "")
        );
        int device3Id = (int) database.deviceDao().insertDevice(
                new DeviceEntity(customerId, Constants.DEVICE_TYPE_PHONE, "iPhone 13 Pro", "358912345678999", "0000", "Mặt lưng không nứt vỡ", "")
        );

        // Ticket 1: Waiting for customer quote approval (with itemized part + labor)
        int ticketWaitingId = (int) database.repairTicketDao().insertTicket(new RepairTicketEntity(
                "FIX-202611-001",
                device1Id,
                receptionistId,
                techId,
                customerId,
                Constants.TICKET_WAITING_APPROVAL,
                "Máy vỡ màn hình, xuất hiện sọc dọc khi bật sáng",
                "Kiểm tra socket màn hình ổn định, cần thay cụm màn hình zin mới và vệ sinh ron chống bụi.",
                300000.0,
                3500000.0,
                now - 2 * oneDay,
                now - 3 * oneHour
        ));
        database.ticketPartDetailDao().insertDetail(new TicketPartDetailEntity(ticketWaitingId, screenId, 1, 3200000.0));

        // Ticket 2: Newly received, awaiting technician diagnosis
        database.repairTicketDao().insertTicket(new RepairTicketEntity(
                "FIX-202611-002",
                device2Id,
                receptionistId,
                null,
                customerId,
                Constants.TICKET_RECEIVED,
                "Pin tụt nhanh và máy báo ổ cứng chậm",
                "",
                0.0,
                0.0,
                now - 5 * oneHour,
                now - 5 * oneHour
        ));

        // Ticket 3: Completed repair awaiting receptionist delivery & invoice
        int ticketCompletedId = (int) database.repairTicketDao().insertTicket(new RepairTicketEntity(
                "FIX-202611-003",
                device3Id,
                receptionistId,
                techId,
                customerId,
                Constants.TICKET_COMPLETED,
                "Pin phù nhẹ, chai còn 71%",
                "Đã thay pin mới, hiệu chuẩn chu kỳ sạc và kiểm tra dòng sạc ổn định.",
                150000.0,
                900000.0,
                now - 4 * oneDay,
                now - oneHour
        ));
        database.ticketPartDetailDao().insertDetail(new TicketPartDetailEntity(ticketCompletedId, batteryPhoneId, 1, 750000.0));

        // Keep managerId referenced
        if (managerId <= 0) {
            throw new IllegalStateException("Seed manager failed");
        }
        // Prevent unused warning on ssdId / batteryLaptopId
        if (batteryLaptopId <= 0 || ssdId <= 0) {
            throw new IllegalStateException("Seed inventory failed");
        }
    }
}
