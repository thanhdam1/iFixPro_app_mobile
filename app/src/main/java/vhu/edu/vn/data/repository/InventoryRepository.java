package vhu.edu.vn.data.repository;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import androidx.lifecycle.LiveData;
import java.util.List;
import vhu.edu.vn.data.local.AppDatabase;
import vhu.edu.vn.data.local.AppExecutors;
import vhu.edu.vn.data.local.dao.InventoryDao;
import vhu.edu.vn.data.local.dao.TicketPartDetailDao;
import vhu.edu.vn.data.local.entity.InventoryPartEntity;

public class InventoryRepository {
    private final AppDatabase database;
    private final InventoryDao inventoryDao;
    private final TicketPartDetailDao detailDao;
    private final AppExecutors executors;

    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public InventoryRepository(Context context) {
        this(AppDatabase.getInstance(context), AppExecutors.getInstance());
    }

    public InventoryRepository(AppDatabase database, AppExecutors executors) {
        this.database = database;
        this.inventoryDao = database.inventoryDao();
        this.detailDao = database.ticketPartDetailDao();
        this.executors = executors;
    }

    public LiveData<List<InventoryPartEntity>> getAllParts() {
        return inventoryDao.getAllParts();
    }

    public LiveData<List<InventoryPartEntity>> getLowStockParts() {
        return inventoryDao.getLowStockParts();
    }

    public void insertPart(InventoryPartEntity part, ActionCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                validatePart(part);
                if (inventoryDao.getPartByCode(part.getPartCode().trim()) != null) {
                    throw new BusinessException("Mã linh kiện \"" + part.getPartCode().trim() + "\" đã tồn tại.");
                }
                part.setPartCode(part.getPartCode().trim());
                part.setPartName(part.getPartName().trim());
                part.setCategory(part.getCategory() != null ? part.getCategory().trim() : "");
                inventoryDao.insertPart(part);
                executors.mainThread().execute(callback::onSuccess);
            } catch (Exception e) {
                String msg = toFriendlyMessage(e, "Không thể thêm linh kiện");
                executors.mainThread().execute(() -> callback.onError(msg));
            }
        });
    }

    public void updatePart(InventoryPartEntity part, ActionCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                validatePart(part);
                InventoryPartEntity existingCode = inventoryDao.getPartByCode(part.getPartCode().trim());
                if (existingCode != null && existingCode.getId() != part.getId()) {
                    throw new BusinessException("Mã linh kiện \"" + part.getPartCode().trim() + "\" đã được dùng cho linh kiện khác.");
                }
                part.setPartCode(part.getPartCode().trim());
                part.setPartName(part.getPartName().trim());
                part.setCategory(part.getCategory() != null ? part.getCategory().trim() : "");
                inventoryDao.updatePart(part);
                executors.mainThread().execute(callback::onSuccess);
            } catch (Exception e) {
                String msg = toFriendlyMessage(e, "Không thể cập nhật linh kiện");
                executors.mainThread().execute(() -> callback.onError(msg));
            }
        });
    }

    public void deletePart(InventoryPartEntity part, ActionCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                database.runInTransaction(() -> {
                    int usage = detailDao.countUsageByPartId(part.getId());
                    if (usage > 0) {
                        throw new BusinessException("Không thể xóa linh kiện này vì đã được sử dụng trong " + usage + " phiếu sửa chữa.");
                    }
                    inventoryDao.deletePart(part);
                });
                executors.mainThread().execute(callback::onSuccess);
            } catch (Exception e) {
                String msg = toFriendlyMessage(e, "Không thể xóa linh kiện");
                executors.mainThread().execute(() -> callback.onError(msg));
            }
        });
    }

    private void validatePart(InventoryPartEntity part) {
        if (part == null) throw new BusinessException("Dữ liệu linh kiện không hợp lệ.");
        if (part.getPartCode() == null || part.getPartCode().trim().isEmpty()) {
            throw new BusinessException("Vui lòng nhập mã linh kiện.");
        }
        if (part.getPartName() == null || part.getPartName().trim().isEmpty()) {
            throw new BusinessException("Vui lòng nhập tên linh kiện.");
        }
        if (part.getQuantity() < 0) {
            throw new BusinessException("Số lượng tồn kho không được âm.");
        }
        if (part.getSellingPrice() < 0 || part.getImportPrice() < 0) {
            throw new BusinessException("Đơn giá không được âm.");
        }
    }

    private String toFriendlyMessage(Exception e, String fallback) {
        if (e instanceof BusinessException && e.getMessage() != null) {
            return e.getMessage();
        }
        if (e instanceof SQLiteConstraintException) {
            return "Linh kiện đang được tham chiếu hoặc trùng mã linh kiện.";
        }
        return fallback + (e.getMessage() != null ? ": " + e.getMessage() : "");
    }
}
