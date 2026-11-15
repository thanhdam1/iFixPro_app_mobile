package vhu.edu.vn.utils;

/** Current workflow stage only; does not invent a historical audit trail. */
public final class RepairProgress {
    private RepairProgress() { }

    public static int stage(String status) {
        if (status == null) return -1;
        switch (status) {
            case Constants.TICKET_RECEIVED: return 0;
            case Constants.TICKET_DIAGNOSING: return 1;
            case Constants.TICKET_WAITING_APPROVAL:
            case Constants.TICKET_APPROVED: return 2;
            case Constants.TICKET_REPAIRING: return 3;
            case Constants.TICKET_COMPLETED: return 4;
            case Constants.TICKET_DELIVERED: return 5;
            default: return -1;
        }
    }

    public static boolean isStopped(String status) {
        return Constants.TICKET_REJECTED.equals(status) || Constants.TICKET_CANCELLED.equals(status);
    }
}
