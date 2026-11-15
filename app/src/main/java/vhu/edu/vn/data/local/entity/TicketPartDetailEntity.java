package vhu.edu.vn.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "ticket_part_details",
    foreignKeys = {
        @ForeignKey(
            entity = RepairTicketEntity.class,
            parentColumns = "id",
            childColumns = "ticketId",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = InventoryPartEntity.class,
            parentColumns = "id",
            childColumns = "partId"
        )
    },
    indices = {
        @Index(value = {"ticketId"}),
        @Index(value = {"partId"})
    }
)
public class TicketPartDetailEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int ticketId;
    private int partId;
    private int quantity;
    private double unitPrice;

    public TicketPartDetailEntity(int ticketId, int partId, int quantity, double unitPrice) {
        this.ticketId = ticketId;
        this.partId = partId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; }
    public int getPartId() { return partId; }
    public void setPartId(int partId) { this.partId = partId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}
