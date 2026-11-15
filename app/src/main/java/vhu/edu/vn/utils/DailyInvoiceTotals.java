package vhu.edu.vn.utils;

import java.util.Calendar;
import java.util.TimeZone;

/** Calendar-day buckets, including today; the next midnight is excluded. */
public final class DailyInvoiceTotals {
    private final long[] boundaries;
    private final double[] totals;
    private int count;

    public DailyInvoiceTotals(long now, int days, TimeZone zone) {
        if (days < 1) throw new IllegalArgumentException("days must be positive");
        totals = new double[days];
        boundaries = new long[days + 1];
        Calendar day = Calendar.getInstance(zone);
        day.setTimeInMillis(now);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        day.add(Calendar.DATE, 1 - days);
        for (int i = 0; i <= days; i++) {
            boundaries[i] = day.getTimeInMillis();
            day.add(Calendar.DATE, 1);
        }
    }

    public void add(long timestamp, double amount) {
        if (!Double.isFinite(amount)) return;
        for (int i = 0; i < totals.length; i++) {
            if (timestamp >= boundaries[i] && timestamp < boundaries[i + 1]) {
                totals[i] += amount;
                count++;
                return;
            }
        }
    }

    public int size() { return totals.length; }
    public long dayStart(int day) { return boundaries[day]; }
    public double value(int day) { return totals[day]; }
    public int invoiceCount() { return count; }

    public double total() {
        double sum = 0;
        for (double value : totals) sum += value;
        return sum;
    }
}
