package vhu.edu.vn.utils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RepairProgressTest {
    @Test
    public void stageMappingAndStoppedFlags_matchWorkflowOrder() {
        assertEquals(0, RepairProgress.stage(Constants.TICKET_RECEIVED));
        assertEquals(1, RepairProgress.stage(Constants.TICKET_DIAGNOSING));
        assertEquals(2, RepairProgress.stage(Constants.TICKET_WAITING_APPROVAL));
        assertEquals(2, RepairProgress.stage(Constants.TICKET_APPROVED));
        assertEquals(3, RepairProgress.stage(Constants.TICKET_REPAIRING));
        assertEquals(4, RepairProgress.stage(Constants.TICKET_COMPLETED));
        assertEquals(5, RepairProgress.stage(Constants.TICKET_DELIVERED));

        assertTrue(RepairProgress.isStopped(Constants.TICKET_REJECTED));
        assertTrue(RepairProgress.isStopped(Constants.TICKET_CANCELLED));
        assertFalse(RepairProgress.isStopped(Constants.TICKET_WAITING_APPROVAL));
    }
}
