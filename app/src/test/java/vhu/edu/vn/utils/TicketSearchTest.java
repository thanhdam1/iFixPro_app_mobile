package vhu.edu.vn.utils;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TicketSearchTest {
    @Test
    public void matches_normalizesVietnameseDiacriticsAndCase() {
        assertTrue(TicketSearch.matches("man hinh", "Màn hình iPhone 14 Pro Max"));
        assertTrue(TicketSearch.matches("điện", "Dien thoai Samsung"));
        assertTrue(TicketSearch.matches("fix-2026", "FIX-202611-001", "Dell XPS"));
        assertTrue(TicketSearch.matches("", "anything"));
        assertFalse(TicketSearch.matches("macbook", "iPhone 14 Pro Max", "358912345678901"));
    }

    @Test
    public void isActive_excludesTerminalAndReadyStates() {
        assertTrue(TicketSearch.isActive(Constants.TICKET_RECEIVED));
        assertTrue(TicketSearch.isActive(Constants.TICKET_DIAGNOSING));
        assertTrue(TicketSearch.isActive(Constants.TICKET_WAITING_APPROVAL));
        assertTrue(TicketSearch.isActive(Constants.TICKET_APPROVED));
        assertTrue(TicketSearch.isActive(Constants.TICKET_REPAIRING));

        assertFalse(TicketSearch.isActive(Constants.TICKET_COMPLETED));
        assertFalse(TicketSearch.isActive(Constants.TICKET_DELIVERED));
        assertFalse(TicketSearch.isActive(Constants.TICKET_REJECTED));
        assertFalse(TicketSearch.isActive(Constants.TICKET_CANCELLED));
    }
}
