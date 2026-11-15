package vhu.edu.vn.data.local.model;

import androidx.room.Embedded;
import androidx.room.Relation;
import vhu.edu.vn.data.local.entity.InvoiceEntity;
import vhu.edu.vn.data.local.entity.RepairTicketEntity;
import vhu.edu.vn.data.local.entity.UserEntity;

public class InvoiceWithTicket {
    @Embedded
    public InvoiceEntity invoice;

    @Relation(
        parentColumn = "ticketId",
        entityColumn = "id"
    )
    public RepairTicketEntity ticket;

    @Relation(
        parentColumn = "receptionistId",
        entityColumn = "id"
    )
    public UserEntity receptionist;
}
