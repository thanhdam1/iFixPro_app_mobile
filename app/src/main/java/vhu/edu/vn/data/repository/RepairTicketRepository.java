package vhu.edu.vn.data.repository;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import androidx.lifecycle.LiveData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import vhu.edu.vn.data.local.AppDatabase;
import vhu.edu.vn.data.local.AppExecutors;
import vhu.edu.vn.data.local.dao.DeviceDao;
import vhu.edu.vn.data.local.dao.InventoryDao;
import vhu.edu.vn.data.local.dao.RepairTicketDao;
import vhu.edu.vn.data.local.dao.TicketPartDetailDao;
import vhu.edu.vn.data.local.dao.UserDao;
import vhu.edu.vn.data.local.entity.DeviceEntity;
import vhu.edu.vn.data.local.entity.InventoryPartEntity;
import vhu.edu.vn.data.local.entity.RepairTicketEntity;
import vhu.edu.vn.data.local.entity.TicketPartDetailEntity;
import vhu.edu.vn.data.local.entity.UserEntity;
import vhu.edu.vn.data.local.model.TicketWithDetails;
import vhu.edu.vn.utils.Constants;

public class RepairTicketRepository {
    private final AppDatabase database;
    private final RepairTicketDao ticketDao;
    private final DeviceDao deviceDao;
    private final UserDao userDao;
    private final InventoryDao inventoryDao;
    private final TicketPartDetailDao detailDao;
    private final AppExecutors executors;

    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface TicketCreateCallback {
        void onSuccess(int ticketId, String ticketCode);
        void onError(String message);
    }

    public interface TicketDetailsCallback {
        void onSuccess(TicketWithDetails ticketWithDetails);
        void onError(String message);
    }

    public RepairTicketRepository(Context context) {
        this(AppDatabase.getInstance(context), AppExecutors.getInstance());
    }

    public RepairTicketRepository(AppDatabase database, AppExecutors executors) {
        this.database = database;
        this.ticketDao = database.repairTicketDao();
        this.deviceDao = database.deviceDao();
        this.userDao = database.userDao();
        this.inventoryDao = database.inventoryDao();
        this.detailDao = database.ticketPartDetailDao();
        this.executors = executors;
    }

    public LiveData<List<TicketWithDetails>> getAllTickets() {
        return ticketDao.getAllTicketsWithDetails();
    }

    public LiveData<List<TicketWithDetails>> getTicketsByStatus(String status) {
        return ticketDao.getTicketsByStatus(status);
    }

    public LiveData<List<TicketWithDetails>> getTicketsByCustomer(int customerId) {
        return ticketDao.getTicketsByCustomerId(customerId);
    }

    public LiveData<List<TicketWithDetails>> getTicketsByTechnician(int technicianId) {
        return ticketDao.getTicketsByTechnicianId(technicianId);
    }

    public LiveData<TicketWithDetails> getTicketById(int ticketId) {
        return ticketDao.getTicketWithDetailsById(ticketId);
    }

    public TicketWithDetails getTicketByCodeSync(String ticketCode) {
        return ticketDao.getTicketWithDetailsByCodeSync(ticketCode);
    }

    public void findTicketByCodeAndLoadDetails(String inputCode, TicketDetailsCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                if (inputCode == null || inputCode.trim().isEmpty()) {
                    throw new BusinessException("Vui lòng nhập hoặc quét mã phiếu QR.");
                }
                String cleanCode = inputCode.trim().replaceAll("^#", "");
                TicketWithDetails result = ticketDao.getTicketWithDetailsByCodeSync(cleanCode);
                if (result == null && !cleanCode.startsWith("FIX-")) {
                    result = ticketDao.getTicketWithDetailsByCodeSync("FIX-" + cleanCode);
                }
                if (result == null || result.ticket == null) {
                    throw new BusinessException("Không tìm thấy dữ liệu phiếu QR [" + inputCode + "].");
                }
                final TicketWithDetails finalResult = result;
                executors.mainThread().execute(() -> callback.onSuccess(finalResult));
            } catch (Exception e) {
                String msg = toFriendlyMessage(e, "Không thể tìm kiếm phiếu QR");
                executors.mainThread().execute(() -> callback.onError(msg));
            }
        });
    }

    /** Atomic intake: finds/creates customer, validates/reuses device by IMEI, creates ticket. */
    public void createTicketIntake(
            String customerName,
            String customerPhone,
            String deviceType,
            String brandModel,
            String imeiSerial,
            String screenPassword,
            String initialCondition,
            String customerNote,
            int receptionistId,
            TicketCreateCallback callback
    ) {
        executors.diskIO().execute(() -> {
            final int[] createdTicketId = new int[1];
            final String[] createdCode = new String[1];
            try {
                database.runInTransaction(() -> {
                    String cleanPhone = customerPhone != null ? customerPhone.trim() : "";
                    String cleanName = customerName != null ? customerName.trim() : "";
                    String cleanBrand = brandModel != null ? brandModel.trim() : "";
                    String cleanImei = imeiSerial != null ? imeiSerial.trim() : "";
                    String cleanPass = screenPassword != null ? screenPassword.trim() : "";
                    String cleanCondition = initialCondition != null ? initialCondition.trim() : "";
                    String cleanNote = customerNote != null ? customerNote.trim() : "";

                    if (cleanPhone.isEmpty() || cleanBrand.isEmpty() || cleanImei.isEmpty()) {
                        throw new BusinessException("Vui lòng nhập đầy đủ Số điện thoại, Hãng/Model và IMEI/Serial.");
                    }
                    int staffId = receptionistId;
                    if (staffId <= 0 || userDao.getUserByIdSync(staffId) == null) {
                        staffId = 2; // Default system receptionist
                        if (userDao.getUserByIdSync(staffId) == null) {
                            staffId = 1;
                        }
                    }

                    long now = System.currentTimeMillis();
                    UserEntity customer = userDao.getUserByPhone(cleanPhone);
                    int customerId;
                    if (customer != null) {
                        if (!Constants.ROLE_CUSTOMER.equals(customer.getRole())) {
                            throw new BusinessException("Số điện thoại này thuộc tài khoản nhân sự, không thể gán cho khách hàng.");
                        }
                        customerId = customer.getId();
                    } else {
                        String suffix = String.format(Locale.ROOT, "%04d", ThreadLocalRandom.current().nextInt(10000));
                        String username = "cust_" + cleanPhone.replaceAll("[^0-9]", "") + "_" + suffix;
                        String finalName = cleanName.isEmpty() ? ("Khách hàng " + cleanPhone) : cleanName;
                        UserEntity newCustomer = new UserEntity(username, "123456", finalName, cleanPhone, Constants.ROLE_CUSTOMER, now);
                        customerId = (int) userDao.insertUser(newCustomer);
                    }

                    DeviceEntity existingDevice = deviceDao.getDeviceByImei(cleanImei);
                    int deviceId;
                    if (existingDevice != null) {
                        if (existingDevice.getCustomerId() != customerId) {
                            throw new BusinessException("IMEI/Serial [" + cleanImei + "] đã thuộc về một khách hàng khác trong hệ thống.");
                        }
                        existingDevice.setDeviceType(deviceType);
                        existingDevice.setBrandModel(cleanBrand);
                        existingDevice.setScreenPassword(cleanPass);
                        existingDevice.setInitialCondition(cleanCondition);
                        deviceDao.updateDevice(existingDevice);
                        deviceId = existingDevice.getId();
                    } else {
                        DeviceEntity newDevice = new DeviceEntity(
                                customerId,
                                deviceType,
                                cleanBrand,
                                cleanImei,
                                cleanPass,
                                cleanCondition,
                                ""
                        );
                        deviceId = (int) deviceDao.insertDevice(newDevice);
                    }

                    String datePart = new SimpleDateFormat("yyMMdd", Locale.ROOT).format(new Date(now));
                    String randPart = String.format(Locale.ROOT, "%04d", ThreadLocalRandom.current().nextInt(1000, 10000));
                    String ticketCode = "FIX-" + datePart + "-" + randPart;
                    while (ticketDao.getTicketWithDetailsByCodeSync(ticketCode) != null) {
                        randPart = String.format(Locale.ROOT, "%04d", ThreadLocalRandom.current().nextInt(1000, 10000));
                        ticketCode = "FIX-" + datePart + "-" + randPart;
                    }

                    RepairTicketEntity ticket = new RepairTicketEntity(
                            ticketCode,
                            deviceId,
                            staffId,
                            null,
                            customerId,
                            Constants.TICKET_RECEIVED,
                            cleanNote,
                            "",
                            0.0,
                            0.0,
                            now,
                            now
                    );
                    createdTicketId[0] = (int) ticketDao.insertTicket(ticket);
                    createdCode[0] = ticketCode;
                });
                executors.mainThread().execute(() -> callback.onSuccess(createdTicketId[0], createdCode[0]));
            } catch (Exception e) {
                String msg = toFriendlyMessage(e, "Không thể tạo phiếu sửa chữa");
                executors.mainThread().execute(() -> callback.onError(msg));
            }
        });
    }

    /** Atomic diagnosis & quote update: validates editable status, stock sufficiency, non-negative amounts. */
    public void saveDiagnosisAndParts(
            int ticketId,
            Integer technicianId,
            double laborFee,
            String technicianNote,
            List<TicketPartDetailEntity> parts,
            ActionCallback callback
    ) {
        executors.diskIO().execute(() -> {
            try {
                database.runInTransaction(() -> {
                    RepairTicketEntity ticket = ticketDao.getTicketByIdSync(ticketId);
                    if (ticket == null) {
                        throw new BusinessException("Không tìm thấy phiếu sửa chữa.");
                    }
                    String status = ticket.getStatus();
                    if (!Constants.TICKET_RECEIVED.equals(status)
                            && !Constants.TICKET_DIAGNOSING.equals(status)
                            && !Constants.TICKET_WAITING_APPROVAL.equals(status)) {
                        throw new BusinessException("Không thể sửa báo giá khi phiếu đã ở trạng thái hiện tại.");
                    }
                    String cleanNote = technicianNote != null ? technicianNote.trim() : "";
                    if (cleanNote.isEmpty()) {
                        throw new BusinessException("Vui lòng nhập kết quả kiểm tra / chẩn đoán.");
                    }
                    if (!Double.isFinite(laborFee) || laborFee < 0.0) {
                        throw new BusinessException("Tiền công không hợp lệ.");
                    }

                    // Aggregate duplicate part selections cleanly
                    Map<Integer, TicketPartDetailEntity> merged = new LinkedHashMap<>();
                    if (parts != null) {
                        for (TicketPartDetailEntity raw : parts) {
                            if (raw == null) continue;
                            if (raw.getQuantity() <= 0) {
                                throw new BusinessException("Số lượng linh kiện phải lớn hơn 0.");
                            }
                            TicketPartDetailEntity existing = merged.get(raw.getPartId());
                            if (existing == null) {
                                merged.put(raw.getPartId(), new TicketPartDetailEntity(ticketId, raw.getPartId(), raw.getQuantity(), raw.getUnitPrice()));
                            } else {
                                existing.setQuantity(existing.getQuantity() + raw.getQuantity());
                            }
                        }
                    }

                    double totalPartsCost = 0.0;
                    List<TicketPartDetailEntity> normalizedParts = new ArrayList<>(merged.values());
                    for (TicketPartDetailEntity detail : normalizedParts) {
                        InventoryPartEntity stockPart = inventoryDao.getPartByIdSync(detail.getPartId());
                        if (stockPart == null) {
                            throw new BusinessException("Linh kiện đã chọn không còn tồn tại trong kho.");
                        }
                        if (stockPart.getQuantity() < detail.getQuantity()) {
                            throw new BusinessException("Linh kiện \"" + stockPart.getPartName() + "\" chỉ còn " + stockPart.getQuantity() + " cái trong kho.");
                        }
                        detail.setUnitPrice(stockPart.getSellingPrice());
                        totalPartsCost += stockPart.getSellingPrice() * detail.getQuantity();
                    }

                    double totalCost = laborFee + totalPartsCost;
                    Integer effectiveTechId = (technicianId != null && technicianId > 0) ? technicianId : ticket.getTechnicianId();

                    detailDao.deleteDetailsByTicketId(ticketId);
                    if (!normalizedParts.isEmpty()) {
                        detailDao.insertDetails(normalizedParts);
                    }
                    ticketDao.updateDiagnosis(
                            ticketId,
                            effectiveTechId,
                            laborFee,
                            totalCost,
                            cleanNote,
                            Constants.TICKET_WAITING_APPROVAL,
                            System.currentTimeMillis()
                    );
                });
                executors.mainThread().execute(callback::onSuccess);
            } catch (Exception e) {
                String msg = toFriendlyMessage(e, "Không thể lưu chẩn đoán và báo giá");
                executors.mainThread().execute(() -> callback.onError(msg));
            }
        });
    }

    /** Atomic quote approval & stock deduction. Prevents double-deduction and checks optimistic version. */
    public void approveTicketAndDeductStock(int ticketId, long expectedUpdatedAt, ActionCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                database.runInTransaction(() -> {
                    RepairTicketEntity ticket = ticketDao.getTicketByIdSync(ticketId);
                    if (ticket == null) {
                        throw new BusinessException("Không tìm thấy phiếu sửa chữa.");
                    }
                    if (!Constants.TICKET_WAITING_APPROVAL.equals(ticket.getStatus())) {
                        throw new BusinessException("Phiếu không còn ở trạng thái chờ duyệt báo giá.");
                    }
                    if (expectedUpdatedAt > 0L && ticket.getUpdatedAt() != expectedUpdatedAt) {
                        throw new BusinessException("Báo giá vừa được cập nhật. Vui lòng xem lại trước khi xác nhận.");
                    }

                    List<TicketPartDetailEntity> details = detailDao.getDetailsByTicketIdSync(ticketId);
                    for (TicketPartDetailEntity detail : details) {
                        InventoryPartEntity part = inventoryDao.getPartByIdSync(detail.getPartId());
                        if (part == null || part.getQuantity() < detail.getQuantity()) {
                            String name = part != null ? part.getPartName() : ("#" + detail.getPartId());
                            throw new BusinessException("Linh kiện \"" + name + "\" không đủ tồn kho để duyệt sửa.");
                        }
                    }

                    for (TicketPartDetailEntity detail : details) {
                        int rowsUpdated = inventoryDao.decreaseStock(detail.getPartId(), detail.getQuantity());
                        if (rowsUpdated == 0) {
                            throw new BusinessException("Không thể trừ kho linh kiện, vui lòng kiểm tra lại tồn kho.");
                        }
                    }

                    ticketDao.updateTicketStatus(ticketId, Constants.TICKET_APPROVED, System.currentTimeMillis());
                });
                executors.mainThread().execute(callback::onSuccess);
            } catch (Exception e) {
                String msg = toFriendlyMessage(e, "Không thể duyệt báo giá");
                executors.mainThread().execute(() -> callback.onError(msg));
            }
        });
    }

    public void approveTicketAndDeductStock(int ticketId, ActionCallback callback) {
        approveTicketAndDeductStock(ticketId, 0L, callback);
    }

    /**
     * Atomic reject/cancel. Restocks parts ONLY when previous state had already deducted inventory
     * (APPROVED or REPAIRING). Never restocks from RECEIVED/DIAGNOSING/WAITING_APPROVAL.
     */
    public void cancelTicketAndRestock(int ticketId, String targetStatus, ActionCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                database.runInTransaction(() -> {
                    if (!Constants.TICKET_REJECTED.equals(targetStatus) && !Constants.TICKET_CANCELLED.equals(targetStatus)) {
                        throw new BusinessException("Trạng thái hủy/từ chối không hợp lệ.");
                    }
                    RepairTicketEntity ticket = ticketDao.getTicketByIdSync(ticketId);
                    if (ticket == null) {
                        throw new BusinessException("Không tìm thấy phiếu sửa chữa.");
                    }
                    String prevStatus = ticket.getStatus();
                    if (Constants.TICKET_REJECTED.equals(targetStatus)) {
                        if (!Constants.TICKET_WAITING_APPROVAL.equals(prevStatus)) {
                            throw new BusinessException("Chỉ có thể từ chối báo giá khi phiếu đang chờ khách duyệt.");
                        }
                    } else {
                        if (!Constants.TICKET_RECEIVED.equals(prevStatus)
                                && !Constants.TICKET_DIAGNOSING.equals(prevStatus)
                                && !Constants.TICKET_WAITING_APPROVAL.equals(prevStatus)
                                && !Constants.TICKET_APPROVED.equals(prevStatus)
                                && !Constants.TICKET_REPAIRING.equals(prevStatus)) {
                            throw new BusinessException("Phiếu ở trạng thái hiện tại không thể hủy.");
                        }
                    }

                    boolean stockWasDeducted = Constants.TICKET_APPROVED.equals(prevStatus)
                            || Constants.TICKET_REPAIRING.equals(prevStatus);
                    if (stockWasDeducted) {
                        List<TicketPartDetailEntity> details = detailDao.getDetailsByTicketIdSync(ticketId);
                        for (TicketPartDetailEntity detail : details) {
                            inventoryDao.increaseStock(detail.getPartId(), detail.getQuantity());
                        }
                    }

                    ticketDao.updateTicketStatus(ticketId, targetStatus, System.currentTimeMillis());
                });
                executors.mainThread().execute(callback::onSuccess);
            } catch (Exception e) {
                String msg = toFriendlyMessage(e, "Không thể cập nhật trạng thái phiếu");
                executors.mainThread().execute(() -> callback.onError(msg));
            }
        });
    }

    /** Atomic workflow transition for technician/receptionist progress updates. */
    public void transitionTicketStatus(int ticketId, String targetStatus, Integer actorTechId, ActionCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                database.runInTransaction(() -> {
                    RepairTicketEntity ticket = ticketDao.getTicketByIdSync(ticketId);
                    if (ticket == null) {
                        throw new BusinessException("Không tìm thấy phiếu sửa chữa.");
                    }
                    String current = ticket.getStatus();
                    boolean allowed = false;
                    if (Constants.TICKET_DIAGNOSING.equals(targetStatus) && Constants.TICKET_RECEIVED.equals(current)) {
                        allowed = true;
                    } else if (Constants.TICKET_REPAIRING.equals(targetStatus) && Constants.TICKET_APPROVED.equals(current)) {
                        allowed = true;
                    } else if (Constants.TICKET_COMPLETED.equals(targetStatus) && Constants.TICKET_REPAIRING.equals(current)) {
                        allowed = true;
                    }
                    if (!allowed) {
                        throw new BusinessException("Không thể chuyển từ trạng thái hiện tại sang bước này.");
                    }

                    long now = System.currentTimeMillis();
                    if (actorTechId != null && actorTechId > 0) {
                        ticketDao.assignTechnician(ticketId, actorTechId, targetStatus, now);
                    } else {
                        ticketDao.updateTicketStatus(ticketId, targetStatus, now);
                    }
                });
                executors.mainThread().execute(callback::onSuccess);
            } catch (Exception e) {
                String msg = toFriendlyMessage(e, "Không thể chuyển trạng thái phiếu");
                executors.mainThread().execute(() -> callback.onError(msg));
            }
        });
    }

    public void updateDeviceEvidencePhotos(int ticketId, String newImagePath, ActionCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                database.runInTransaction(() -> {
                    RepairTicketEntity ticket = ticketDao.getTicketByIdSync(ticketId);
                    if (ticket == null) {
                        throw new BusinessException("Không tìm thấy phiếu sửa chữa.");
                    }
                    DeviceEntity device = deviceDao.getDeviceByIdSync(ticket.getDeviceId());
                    if (device != null) {
                        device.setImagePath(newImagePath);
                        deviceDao.updateDevice(device);
                    }
                });
                executors.mainThread().execute(callback::onSuccess);
            } catch (Exception e) {
                String msg = toFriendlyMessage(e, "Không thể cập nhật ảnh minh chứng");
                executors.mainThread().execute(() -> callback.onError(msg));
            }
        });
    }

    public void saveCustomerRatingAndFeedback(int ticketId, float rating, String feedback, ActionCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                if (ticketId <= 0) {
                    throw new BusinessException("Mã phiếu không hợp lệ.");
                }
                if (rating <= 0) {
                    throw new BusinessException("Vui lòng chọn số sao đánh giá dịch vụ.");
                }
                long now = System.currentTimeMillis();
                ticketDao.updateRatingAndFeedback(ticketId, rating, feedback != null ? feedback.trim() : "", now);
                executors.mainThread().execute(callback::onSuccess);
            } catch (Exception e) {
                String msg = toFriendlyMessage(e, "Không thể gửi đánh giá dịch vụ");
                executors.mainThread().execute(() -> callback.onError(msg));
            }
        });
    }

    private String toFriendlyMessage(Exception e, String fallback) {
        if (e instanceof BusinessException && e.getMessage() != null) {
            return e.getMessage();
        }
        if (e instanceof SQLiteConstraintException) {
            return "Dữ liệu bị trùng hoặc vi phạm ràng buộc lưu trữ.";
        }
        return fallback + (e.getMessage() != null ? ": " + e.getMessage() : "");
    }
}
