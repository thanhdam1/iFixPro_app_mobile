package vhu.edu.vn.ui.manager;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import vhu.edu.vn.R;
import vhu.edu.vn.data.local.AppDatabase;
import vhu.edu.vn.data.local.model.InvoiceWithTicket;
import vhu.edu.vn.data.local.model.TicketWithDetails;
import vhu.edu.vn.databinding.FragmentManagerHomeBinding;
import vhu.edu.vn.utils.Constants;
import vhu.edu.vn.utils.DailyInvoiceTotals;
import vhu.edu.vn.utils.TicketSearch;
import vhu.edu.vn.utils.UiFormatters;

public class ManagerHomeFragment extends Fragment {
    private FragmentManagerHomeBinding binding;
    private List<InvoiceWithTicket> allInvoices = Collections.emptyList();
    private int selectedDays = 7;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentManagerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnNavInventory.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), InventoryManagementActivity.class))
        );
        binding.cardLowStockKpi.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), InventoryManagementActivity.class);
            intent.putExtra(InventoryManagementActivity.EXTRA_LOW_STOCK_ONLY, true);
            startActivity(intent);
        });
        binding.btnNavUsers.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), UserManagementActivity.class))
        );

        binding.chipReportPeriod.setOnCheckedStateChangeListener((group, checkedIds) -> {
            selectedDays = binding.chipThirtyDays.isChecked() ? 30 : 7;
            renderBarChart();
        });

        setupChartsAppearance();

        AppDatabase db = AppDatabase.getInstance(requireContext());
        db.invoiceDao().getAllInvoicesWithTicket().observe(getViewLifecycleOwner(), invoices -> {
            allInvoices = invoices != null ? invoices : Collections.emptyList();
            renderInvoiceHero();
            renderBarChart();
        });

        db.repairTicketDao().getAllTicketsWithDetails().observe(getViewLifecycleOwner(), this::renderOperationsAndPie);

        db.inventoryDao().getLowStockParts().observe(getViewLifecycleOwner(), parts -> {
            if (binding == null) return;
            int lowCount = parts != null ? parts.size() : 0;
            binding.tvMgrKpiLowStock.setText(String.valueOf(lowCount));
        });
    }

    private void setupChartsAppearance() {
        int textColor = ContextCompat.getColor(requireContext(), R.color.on_surface_variant);
        int gridColor = ContextCompat.getColor(requireContext(), R.color.outline);

        binding.barChartRevenue.getDescription().setEnabled(false);
        binding.barChartRevenue.getLegend().setEnabled(false);
        binding.barChartRevenue.getAxisRight().setEnabled(false);
        binding.barChartRevenue.setFitBars(true);
        binding.barChartRevenue.setExtraBottomOffset(6f);
        binding.barChartRevenue.setNoDataText(getString(R.string.no_invoice_period));
        binding.barChartRevenue.setNoDataTextColor(textColor);

        XAxis xAxis = binding.barChartRevenue.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(textColor);

        binding.barChartRevenue.getAxisLeft().setAxisMinimum(0f);
        binding.barChartRevenue.getAxisLeft().setTextColor(textColor);
        binding.barChartRevenue.getAxisLeft().setGridColor(gridColor);
        binding.barChartRevenue.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1_000_000f) {
                    return String.format(java.util.Locale.ROOT, "%.1ftr", value / 1_000_000f);
                }
                if (value >= 1_000f) {
                    return Math.round(value / 1_000f) + "k";
                }
                return String.valueOf(Math.round(value));
            }
        });

        binding.pieChartStatus.getDescription().setEnabled(false);
        binding.pieChartStatus.setUsePercentValues(false);
        binding.pieChartStatus.setDrawEntryLabels(false);
        binding.pieChartStatus.setHoleRadius(56f);
        binding.pieChartStatus.setTransparentCircleRadius(62f);
        binding.pieChartStatus.setHoleColor(ContextCompat.getColor(requireContext(), R.color.surface));
        binding.pieChartStatus.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface));
        binding.pieChartStatus.setCenterTextSize(13f);
        binding.pieChartStatus.getLegend().setTextColor(textColor);
        binding.pieChartStatus.getLegend().setWordWrapEnabled(true);
        binding.pieChartStatus.setNoDataText("Chưa có phiếu sửa chữa.");
        binding.pieChartStatus.setNoDataTextColor(textColor);
    }

    private void renderInvoiceHero() {
        if (binding == null) return;
        double allTimeSum = 0.0;
        for (InvoiceWithTicket row : allInvoices) {
            if (row.invoice != null && Double.isFinite(row.invoice.getFinalTotal())) {
                allTimeSum += row.invoice.getFinalTotal();
            }
        }
        binding.tvTotalRevenueValue.setText(UiFormatters.formatCurrency(allTimeSum));
        binding.tvInvoiceCountAllTime.setText(allInvoices.size()
                + " hóa đơn đã lập (không bao gồm dữ liệu giá vốn)");
    }

    private void renderBarChart() {
        if (binding == null) return;
        long now = System.currentTimeMillis();
        DailyInvoiceTotals totals = new DailyInvoiceTotals(now, selectedDays, TimeZone.getDefault());
        for (InvoiceWithTicket row : allInvoices) {
            if (row.invoice != null) {
                totals.add(row.invoice.getCreatedAt(), row.invoice.getFinalTotal());
            }
        }

        binding.tvPeriodSummary.setText(getString(
                R.string.invoice_period_summary,
                selectedDays,
                totals.invoiceCount(),
                UiFormatters.formatCurrency(totals.total())
        ));

        if (totals.invoiceCount() == 0) {
            binding.barChartRevenue.clear();
            binding.barChartRevenue.invalidate();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < totals.size(); i++) {
            entries.add(new BarEntry(i, (float) totals.value(i)));
            labels.add(UiFormatters.formatShortDate(totals.dayStart(i)));
        }

        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.invoice_value));
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary));
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(selectedDays <= 7 ? 0.55f : 0.65f);

        binding.barChartRevenue.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.barChartRevenue.getXAxis().setLabelCount(selectedDays <= 7 ? 7 : 6);
        binding.barChartRevenue.setData(barData);
        binding.barChartRevenue.animateY(350);
        binding.barChartRevenue.invalidate();
    }

    private void renderOperationsAndPie(List<TicketWithDetails> tickets) {
        if (binding == null) return;
        List<TicketWithDetails> list = tickets != null ? tickets : Collections.emptyList();

        int total = 0;
        int active = 0;
        int waiting = 0;
        int ready = 0;
        int receivedOrDiag = 0;
        int approvedOrRepairing = 0;
        int delivered = 0;
        int closedNegative = 0;

        for (TicketWithDetails row : list) {
            if (row.ticket == null) continue;
            total++;
            String status = row.ticket.getStatus();
            if (TicketSearch.isActive(status)) active++;

            if (Constants.TICKET_RECEIVED.equals(status) || Constants.TICKET_DIAGNOSING.equals(status)) {
                receivedOrDiag++;
            } else if (Constants.TICKET_WAITING_APPROVAL.equals(status)) {
                waiting++;
            } else if (Constants.TICKET_APPROVED.equals(status) || Constants.TICKET_REPAIRING.equals(status)) {
                approvedOrRepairing++;
            } else if (Constants.TICKET_COMPLETED.equals(status)) {
                ready++;
            } else if (Constants.TICKET_DELIVERED.equals(status)) {
                delivered++;
            } else {
                closedNegative++;
            }
        }

        binding.tvMgrKpiTotalActive.setText(String.valueOf(active));
        binding.tvMgrKpiTotalLabel.setText("Đang xử lý / " + total + " phiếu");
        binding.tvMgrKpiWaiting.setText(String.valueOf(waiting));
        binding.tvMgrKpiReady.setText(String.valueOf(ready));
        binding.tvOperationsSummary.setText(getString(R.string.operations_summary, total, active, waiting, ready));

        if (total == 0) {
            binding.pieChartStatus.clear();
            binding.pieChartStatus.invalidate();
            binding.tvStatusTextBreakdown.setText("Chưa có phiếu sửa chữa để thống kê.");
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        StringBuilder breakdown = new StringBuilder();

        addPieSlice(entries, colors, breakdown, "Tiếp nhận & chẩn đoán", receivedOrDiag, R.color.status_received);
        addPieSlice(entries, colors, breakdown, "Chờ duyệt báo giá", waiting, R.color.status_waiting);
        addPieSlice(entries, colors, breakdown, "Đã duyệt & đang sửa", approvedOrRepairing, R.color.status_repairing);
        addPieSlice(entries, colors, breakdown, "Sửa xong · Chờ giao", ready, R.color.status_completed);
        addPieSlice(entries, colors, breakdown, "Đã bàn giao", delivered, R.color.status_delivered);
        addPieSlice(entries, colors, breakdown, "Từ chối / Đã hủy", closedNegative, R.color.status_rejected);

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        dataSet.setValueTextSize(11f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf(Math.round(value));
            }
        });

        binding.pieChartStatus.setCenterText(getString(R.string.ticket_total, total));
        binding.pieChartStatus.setData(new PieData(dataSet));
        binding.pieChartStatus.animateY(350);
        binding.pieChartStatus.invalidate();
        binding.tvStatusTextBreakdown.setText(breakdown.toString().trim());
    }

    private void addPieSlice(
            List<PieEntry> entries,
            List<Integer> colors,
            StringBuilder breakdown,
            String label,
            int count,
            int colorRes
    ) {
        if (count <= 0) return;
        entries.add(new PieEntry(count, label));
        colors.add(ContextCompat.getColor(requireContext(), colorRes));
        if (breakdown.length() > 0) breakdown.append("\n");
        breakdown.append("• ").append(label).append(": ").append(count).append(" phiếu");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
