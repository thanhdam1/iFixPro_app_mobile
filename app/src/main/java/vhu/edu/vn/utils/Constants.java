package vhu.edu.vn.utils;

public class Constants {
    // SharedPreferences Keys
    public static final String PREF_NAME = "iFixProSession";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_FULL_NAME = "fullName";
    public static final String KEY_ROLE = "role";

    // User Roles
    public static final String ROLE_RECEPTIONIST = "RECEPTIONIST";
    public static final String ROLE_TECHNICIAN = "TECHNICIAN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_MANAGER = "MANAGER";

    // Repair Ticket Statuses
    public static final String TICKET_RECEIVED = "RECEIVED";
    public static final String TICKET_DIAGNOSING = "DIAGNOSING";
    public static final String TICKET_WAITING_APPROVAL = "WAITING_APPROVAL";
    public static final String TICKET_APPROVED = "APPROVED";
    public static final String TICKET_REJECTED = "REJECTED";
    public static final String TICKET_REPAIRING = "REPAIRING";
    public static final String TICKET_COMPLETED = "COMPLETED";
    public static final String TICKET_DELIVERED = "DELIVERED";
    public static final String TICKET_CANCELLED = "CANCELLED";

    // Device Types
    public static final String DEVICE_TYPE_PHONE = "PHONE";
    public static final String DEVICE_TYPE_LAPTOP = "LAPTOP";
    public static final String DEVICE_TYPE_TABLET = "TABLET";

    // Inventory Warning Threshold
    public static final int DEFAULT_MIN_STOCK_THRESHOLD = 5;

    // Payment Methods
    public static final String PAYMENT_CASH = "CASH";
    public static final String PAYMENT_BANK_TRANSFER = "BANK_TRANSFER";
    public static final String PAYMENT_QR = "QR_PAY";
}
