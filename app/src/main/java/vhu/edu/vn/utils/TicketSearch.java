package vhu.edu.vn.utils;

import java.text.Normalizer;
import java.util.Locale;

/** Pure search rules shared with tests; independent of Android and Room. */
public final class TicketSearch {
    private TicketSearch() { }

    public static boolean matches(String query, String... fields) {
        String needle = normalize(query);
        if (needle.isEmpty()) return true;
        if (fields == null) return false;
        for (String field : fields) {
            if (normalize(field).contains(needle)) return true;
        }
        return false;
    }

    public static boolean isActive(String status) {
        return Constants.TICKET_RECEIVED.equals(status)
                || Constants.TICKET_DIAGNOSING.equals(status)
                || Constants.TICKET_WAITING_APPROVAL.equals(status)
                || Constants.TICKET_APPROVED.equals(status)
                || Constants.TICKET_REPAIRING.equals(status);
    }

    public static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd');
    }
}
