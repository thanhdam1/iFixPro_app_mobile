package vhu.edu.vn.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "invoices",
    foreignKeys = {
        @ForeignKey(
            entity = RepairTicketEntity.class,
            parentColumns = "id",
            childColumns = "ticketId"
        ),
        @ForeignKey(
            entity = UserEntity.class,
            parentColumns = "id",
            childColumns = "receptionistId"
        )
    },
    indices = {
        @Index(value = {"invoiceCode"}, unique = true),
        @Index(value = {"ticketId"}),
        @Index(value = {"receptionistId"})
    }
)
public class InvoiceEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String invoiceCode;
    private int ticketId;
    private int receptionistId;
    private double subTotal;
    private double discount;
    private double finalTotal;
    private String paymentMethod;
    private long createdAt;

    public InvoiceEntity(String invoiceCode, int ticketId, int receptionistId, double subTotal, double discount, double finalTotal, String paymentMethod, long createdAt) {
        this.invoiceCode = invoiceCode;
        this.ticketId = ticketId;
        this.receptionistId = receptionistId;
        this.subTotal = subTotal;
        this.discount = discount;
        this.finalTotal = finalTotal;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getInvoiceCode() { return invoiceCode; }
    public void setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; }
    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }
    public int getReceptionistId() { return receptionistId; }
    public void setReceptionistId(int receptionistId) { this.receptionistId = receptionistId; }
    public double getSubTotal() { return subTotal; }
    public void setSubTotal(double subTotal) { this.subTotal = subTotal; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
    public double getFinalTotal() { return finalTotal; }
    public void setFinalTotal(double finalTotal) { this.finalTotal = finalTotal; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
