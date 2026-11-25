package vhu.edu.vn.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "repair_tickets",
    foreignKeys = {
        @ForeignKey(entity = DeviceEntity.class, parentColumns = "id", childColumns = "deviceId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "receptionistId"),
        @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "technicianId"),
        @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "customerId")
    },
    indices = {
        @Index(value = {"ticketCode"}, unique = true),
        @Index(value = {"deviceId"}),
        @Index(value = {"receptionistId"}),
        @Index(value = {"technicianId"}),
        @Index(value = {"customerId"})
    }
)
public class RepairTicketEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String ticketCode;
    private int deviceId;
    private int receptionistId;
    private Integer technicianId;
    private int customerId;
    private String status;
    private String customerNote;
    private String technicianNote;
    private double laborFee;
    private double totalCost;
    private float rating;
    private String customerFeedback;
    private long createdAt;
    private long updatedAt;

    public RepairTicketEntity(String ticketCode, int deviceId, int receptionistId, Integer technicianId, int customerId, String status, String customerNote, String technicianNote, double laborFee, double totalCost, long createdAt, long updatedAt) {
        this.ticketCode = ticketCode;
        this.deviceId = deviceId;
        this.receptionistId = receptionistId;
        this.technicianId = technicianId;
        this.customerId = customerId;
        this.status = status;
        this.customerNote = customerNote;
        this.technicianNote = technicianNote;
        this.laborFee = laborFee;
        this.totalCost = totalCost;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }
    public int getDeviceId() { return deviceId; }
    public void setDeviceId(int deviceId) { this.deviceId = deviceId; }
    public int getReceptionistId() { return receptionistId; }
    public void setReceptionistId(int receptionistId) { this.receptionistId = receptionistId; }
    public Integer getTechnicianId() { return technicianId; }
    public void setTechnicianId(Integer technicianId) { this.technicianId = technicianId; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCustomerNote() { return customerNote; }
    public void setCustomerNote(String customerNote) { this.customerNote = customerNote; }
    public String getTechnicianNote() { return technicianNote; }
    public void setTechnicianNote(String technicianNote) { this.technicianNote = technicianNote; }
    public double getLaborFee() { return laborFee; }
    public void setLaborFee(double laborFee) { this.laborFee = laborFee; }
    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
    public String getCustomerFeedback() { return customerFeedback; }
    public void setCustomerFeedback(String customerFeedback) { this.customerFeedback = customerFeedback; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
