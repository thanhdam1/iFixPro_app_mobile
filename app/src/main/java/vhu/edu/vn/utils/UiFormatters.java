package vhu.edu.vn.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import vhu.edu.vn.R;

/** Shared formatting and semantic badge presentation helpers. */
public final class UiFormatters {
    private static final Locale VI_LOCALE = new Locale("vi", "VN");

    private UiFormatters() { }

    public static String formatCurrency(double amount) {
        if (!Double.isFinite(amount)) return "0 ₫";
        NumberFormat format = NumberFormat.getCurrencyInstance(VI_LOCALE);
        return format.format(amount);
    }

    public static String formatDateTime(long epochMillis) {
        if (epochMillis <= 0L) return "—";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(epochMillis));
    }

    public static String formatShortDate(long epochMillis) {
        if (epochMillis <= 0L) return "—";
        return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date(epochMillis));
    }

    public static String roleDisplayName(Context context, String role) {
        if (role == null) return context.getString(R.string.role_unknown);
        switch (role) {
            case Constants.ROLE_MANAGER: return context.getString(R.string.role_manager);
            case Constants.ROLE_RECEPTIONIST: return context.getString(R.string.role_receptionist);
            case Constants.ROLE_TECHNICIAN: return context.getString(R.string.role_technician);
            case Constants.ROLE_CUSTOMER: return context.getString(R.string.role_customer);
            default: return context.getString(R.string.role_unknown);
        }
    }

    public static String deviceTypeLabel(String deviceType) {
        if (Constants.DEVICE_TYPE_LAPTOP.equals(deviceType)) return "Laptop";
        if (Constants.DEVICE_TYPE_TABLET.equals(deviceType)) return "Máy tính bảng";
        return "Điện thoại";
    }

    @DrawableRes
    public static int deviceTypeIcon(String deviceType) {
        if (Constants.DEVICE_TYPE_LAPTOP.equals(deviceType)) return R.drawable.ic_device_laptop;
        if (Constants.DEVICE_TYPE_TABLET.equals(deviceType)) return R.drawable.ic_device_tablet;
        return R.drawable.ic_device_phone;
    }

    public static String paymentMethodLabel(String method) {
        if (Constants.PAYMENT_BANK_TRANSFER.equals(method)) return "Chuyển khoản";
        if (Constants.PAYMENT_QR.equals(method)) return "Quét mã QR";
        return "Tiền mặt";
    }

    public static String initials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "IF";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase(Locale.ROOT);
    }

    public static boolean isDebugBuild(Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    public static StatusStyle statusStyle(String status) {
        if (status == null) {
            return new StatusStyle("Chưa xác định", R.color.status_cancelled, R.color.status_cancelled_bg, R.drawable.ic_warning);
        }
        switch (status) {
            case Constants.TICKET_RECEIVED:
                return new StatusStyle("Mới tiếp nhận", R.color.status_received, R.color.status_received_bg, R.drawable.ic_ticket);
            case Constants.TICKET_DIAGNOSING:
                return new StatusStyle("Đang chẩn đoán", R.color.status_diagnosing, R.color.status_diagnosing_bg, R.drawable.ic_search);
            case Constants.TICKET_WAITING_APPROVAL:
                return new StatusStyle("Chờ duyệt giá", R.color.status_waiting, R.color.status_waiting_bg, R.drawable.ic_schedule);
            case Constants.TICKET_APPROVED:
                return new StatusStyle("Đã duyệt · Chờ sửa", R.color.status_approved, R.color.status_approved_bg, R.drawable.ic_check_circle);
            case Constants.TICKET_REPAIRING:
                return new StatusStyle("Đang sửa chữa", R.color.status_repairing, R.color.status_repairing_bg, R.drawable.ic_build);
            case Constants.TICKET_COMPLETED:
                return new StatusStyle("Sửa xong · Chờ giao", R.color.status_completed, R.color.status_completed_bg, R.drawable.ic_check_circle);
            case Constants.TICKET_DELIVERED:
                return new StatusStyle("Đã bàn giao", R.color.status_delivered, R.color.status_delivered_bg, R.drawable.ic_invoice);
            case Constants.TICKET_REJECTED:
                return new StatusStyle("Từ chối báo giá", R.color.status_rejected, R.color.status_rejected_bg, R.drawable.ic_cancel);
            case Constants.TICKET_CANCELLED:
                return new StatusStyle("Đã hủy phiếu", R.color.status_cancelled, R.color.status_cancelled_bg, R.drawable.ic_cancel);
            default:
                return new StatusStyle("Chưa xác định", R.color.status_cancelled, R.color.status_cancelled_bg, R.drawable.ic_warning);
        }
    }

    public static final class StatusStyle {
        public final String label;
        @ColorRes public final int textColor;
        @ColorRes public final int bgColor;
        @DrawableRes public final int iconRes;

        public StatusStyle(String label, @ColorRes int textColor, @ColorRes int bgColor, @DrawableRes int iconRes) {
            this.label = label;
            this.textColor = textColor;
            this.bgColor = bgColor;
            this.iconRes = iconRes;
        }
    }
}
