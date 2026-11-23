package vhu.edu.vn.data.repository;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import androidx.lifecycle.LiveData;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import vhu.edu.vn.data.local.AppDatabase;
import vhu.edu.vn.data.local.AppExecutors;
import vhu.edu.vn.data.local.dao.InvoiceDao;
import vhu.edu.vn.data.local.dao.RepairTicketDao;
import vhu.edu.vn.data.local.dao.TicketPartDetailDao;
import vhu.edu.vn.data.local.dao.UserDao;
import vhu.edu.vn.data.local.entity.InvoiceEntity;
import vhu.edu.vn.data.local.entity.RepairTicketEntity;
import vhu.edu.vn.data.local.entity.TicketPartDetailEntity;
import vhu.edu.vn.data.local.model.InvoiceWithTicket;
import vhu.edu.vn.utils.Constants;

public class InvoiceRepository {
    private final AppDatabase database;
    private final InvoiceDao invoiceDao;
    private final RepairTicketDao ticketDao;
    private final TicketPartDetailDao detailDao;
    private final UserDao userDao;
    private final AppExecutors executors;

    public interface InvoiceCreateCallback {
        void onSuccess(String invoiceCode);
        void onError(String message);
    }

    public InvoiceRepository(Context context) {
        this(AppDatabase.getInstance(context), AppExecutors.getInstance());
    }

    public InvoiceRepository(AppDatabase database, AppExecutors executors) {
        this.database = database;
        this.invoiceDao = database.invoiceDao();
        this.ticketDao = database.repairTicketDao();
        this.detailDao = database.ticketPartDetailDao();
        this.userDao = database.userDao();
        this.executors = executors;
    }

    public LiveData<Double> getTotalRevenue() {
        return invoiceDao.getTotalRevenue();
    }

    public LiveData<List<InvoiceWithTicket>> getAllInvoices() {
        return invoiceDao.getAllInvoicesWithTicket();
    }

    /** Atomic invoice creation + ticket delivery transition. Prevents duplicate invoices and invalid-state billing. */
    public void issueInvoiceAndDeliver(
            int ticketId,
            int receptionistId,
            double discount,
            String paymentMethod,
            InvoiceCreateCallback callback
    ) {
        executors.diskIO().execute(() -> {
            final String[] createdCode = new String[1];
            try {
                database.runInTransaction(() -> {
                    RepairTicketEntity ticket = ticketDao.getTicketByIdSync(ticketId);
                    if (ticket == null) {
                        throw new BusinessException("Không tìm thấy phiếu sửa chữa.");
                    }
                    if (!Constants.TICKET_COMPLETED.equals(ticket.getStatus())) {
                        throw new BusinessException("Chỉ có thể lập hóa đơn & bàn giao khi phiếu đã hoàn tất sửa chữa.");
                    }
                    if (invoiceDao.getInvoiceByTicketIdSync(ticketId) != null) {
                        throw new BusinessException("Phiếu này đã được lập hóa đơn trước đó.");
                    }
                    if (receptionistId <= 0 || userDao.getUserByIdSync(receptionistId) == null) {
                        throw new BusinessException("Phiên lễ tân không hợp lệ. Vui lòng đăng nhập lại.");
                    }

                    List<TicketPartDetailEntity> details = detailDao.getDetailsByTicketIdSync(ticketId);
                    double partsTotal = 0.0;
                    for (TicketPartDetailEntity d : details) {
                        partsTotal += d.getUnitPrice() * d.getQuantity();
                    }
                    double subTotal = partsTotal + ticket.getLaborFee();
                    if (subTotal <= 0.0 && ticket.getTotalCost() > 0.0) {
                        subTotal = ticket.getTotalCost();
                    }

                    if (!Double.isFinite(discount) || discount < 0.0) {
                        throw new BusinessException("Số tiền giảm giá không hợp lệ.");
                    }
                    if (discount > subTotal) {
                        throw new BusinessException("Giảm giá không được vượt quá tổng chi phí phiếu.");
                    }

                    String method = (paymentMethod != null && !paymentMethod.trim().isEmpty())
                            ? paymentMethod
                            : Constants.PAYMENT_CASH;
                    double finalTotal = Math.max(0.0, subTotal - discount);
                    long now = System.currentTimeMillis();
                    String datePart = new SimpleDateFormat("yyMMdd", Locale.ROOT).format(new Date(now));
                    String randPart = String.format(Locale.ROOT, "%04d", ThreadLocalRandom.current().nextInt(1000, 10000));
                    String invoiceCode = "INV-" + datePart + "-" + randPart;

                    InvoiceEntity invoice = new InvoiceEntity(
                            invoiceCode,
                            ticketId,
                            receptionistId,
                            subTotal,
                            discount,
                            finalTotal,
                            method,
                            now
                    );
                    invoiceDao.insertInvoice(invoice);
                    ticketDao.updateTicketStatus(ticketId, Constants.TICKET_DELIVERED, now);
                    createdCode[0] = invoiceCode;
                });
                executors.mainThread().execute(() -> callback.onSuccess(createdCode[0]));
            } catch (Exception e) {
                String msg;
                if (e instanceof BusinessException && e.getMessage() != null) {
                    msg = e.getMessage();
                } else if (e instanceof SQLiteConstraintException) {
                    msg = "Mã hóa đơn bị trùng hoặc dữ liệu không hợp lệ.";
                } else {
                    msg = "Không thể lập hóa đơn: " + (e.getMessage() != null ? e.getMessage() : "Lỗi không xác định");
                }
                executors.mainThread().execute(() -> callback.onError(msg));
            }
        });
    }

    public void issueCashInvoiceAtCounter(int ticketId, int receptionistId, InvoiceCreateCallback callback) {
        int staffId = receptionistId > 0 ? receptionistId : 1;
        issueInvoiceAndDeliver(ticketId, staffId, 0.0, Constants.PAYMENT_CASH, callback);
    }
}
