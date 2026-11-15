package vhu.edu.vn.utils;

import java.util.Calendar;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DailyInvoiceTotalsTest {
    @Test
    public void add_bucketsInvoicesByCalendarDayAndIgnoresOutOfRangeOrNonFinite() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        Calendar cal = Calendar.getInstance(utc);
        cal.set(2026, Calendar.NOVEMBER, 14, 15, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long now = cal.getTimeInMillis();

        DailyInvoiceTotals totals = new DailyInvoiceTotals(now, 7, utc);
        assertEquals(7, totals.size());

        // Today
        totals.add(now - 60_000L, 500_000.0);
        // 6 days ago (earliest included day)
        totals.add(totals.dayStart(0) + 1_000L, 250_000.0);
        // 7 days ago (excluded)
        totals.add(totals.dayStart(0) - 1_000L, 999_000.0);
        // Non-finite (ignored)
        totals.add(now, Double.NaN);

        assertEquals(2, totals.invoiceCount());
        assertEquals(750_000.0, totals.total(), 0.01);
        assertEquals(250_000.0, totals.value(0), 0.01);
        assertEquals(500_000.0, totals.value(6), 0.01);
    }
}
