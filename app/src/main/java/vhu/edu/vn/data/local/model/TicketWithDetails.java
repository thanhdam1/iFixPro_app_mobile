package vhu.edu.vn.data.local.model;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;
import java.util.List;
import vhu.edu.vn.data.local.entity.DeviceEntity;
import vhu.edu.vn.data.local.entity.InventoryPartEntity;
import vhu.edu.vn.data.local.entity.InvoiceEntity;
import vhu.edu.vn.data.local.entity.RepairTicketEntity;
import vhu.edu.vn.data.local.entity.TicketPartDetailEntity;
import vhu.edu.vn.data.local.entity.UserEntity;

public class TicketWithDetails {
    @Embedded
    public RepairTicketEntity ticket;

    @Relation(
        parentColumn = "deviceId",
        entityColumn = "id"
    )
    public DeviceEntity device;

    @Relation(
        parentColumn = "customerId",
        entityColumn = "id"
    )
    public UserEntity customer;

    @Relation(
        parentColumn = "receptionistId",
        entityColumn = "id"
    )
    public UserEntity receptionist;

    @Relation(
        parentColumn = "technicianId",
        entityColumn = "id"
    )
    public UserEntity technician;

    @Relation(
        parentColumn = "id",
        entityColumn = "ticketId"
    )
    public List<TicketPartDetailEntity> partDetails;

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = @Junction(
            value = TicketPartDetailEntity.class,
            parentColumn = "ticketId",
            entityColumn = "partId"
        )
    )
    public List<InventoryPartEntity> parts;

    @Relation(
        parentColumn = "id",
        entityColumn = "ticketId"
    )
    public InvoiceEntity invoice;
}
