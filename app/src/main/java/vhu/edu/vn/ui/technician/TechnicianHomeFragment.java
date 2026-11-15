package vhu.edu.vn.ui.technician;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.List;
import vhu.edu.vn.R;
import vhu.edu.vn.data.local.model.TicketWithDetails;
import vhu.edu.vn.data.repository.RepairTicketRepository;
import vhu.edu.vn.databinding.FragmentTechnicianHomeBinding;
import vhu.edu.vn.ui.adapter.StaffTicketList;
import vhu.edu.vn.ui.adapter.TicketAdapter;
import vhu.edu.vn.utils.Constants;

public class TechnicianHomeFragment extends Fragment {
    private FragmentTechnicianHomeBinding binding;
    private StaffTicketList ticketList;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentTechnicianHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TicketAdapter adapter = new TicketAdapter();
        adapter.setShowCustomerInfo(true);
        adapter.setStaffMode(true);
        adapter.setOnTicketClickListener(item -> {
            if (item == null || item.ticket == null) return;
            Intent intent = new Intent(requireContext(), DiagnoseActivity.class);
            intent.putExtra("ticket_id", item.ticket.getId());
            startActivity(intent);
        });

        binding.rvTechTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTechTickets.setAdapter(adapter);

        ticketList = new StaffTicketList(binding.getRoot(), adapter);
        ticketList.configureSecondaryChips(
                getString(R.string.kpi_approved_queue),
                Constants.TICKET_APPROVED::equals,
                getString(R.string.filter_diagnose),
                status -> Constants.TICKET_RECEIVED.equals(status) || Constants.TICKET_DIAGNOSING.equals(status)
        );

        binding.btnScanQr.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), QRScannerActivity.class))
        );

        RepairTicketRepository repository = new RepairTicketRepository(requireContext());
        repository.getAllTickets().observe(getViewLifecycleOwner(), tickets -> {
            updateKpis(tickets);
            ticketList.submit(tickets);
        });
    }

    private void updateKpis(List<TicketWithDetails> tickets) {
        if (binding == null) return;
        int diagnoseCount = 0;
        int approvedCount = 0;
        int repairingCount = 0;
        int completedCount = 0;

        if (tickets != null) {
            for (TicketWithDetails item : tickets) {
                if (item.ticket == null) continue;
                String status = item.ticket.getStatus();
                if (Constants.TICKET_RECEIVED.equals(status) || Constants.TICKET_DIAGNOSING.equals(status)) {
                    diagnoseCount++;
                } else if (Constants.TICKET_APPROVED.equals(status)) {
                    approvedCount++;
                } else if (Constants.TICKET_REPAIRING.equals(status)) {
                    repairingCount++;
                } else if (Constants.TICKET_COMPLETED.equals(status)) {
                    completedCount++;
                }
            }
        }

        binding.tvTechKpiDiagnose.setText(String.valueOf(diagnoseCount));
        binding.tvTechKpiApproved.setText(String.valueOf(approvedCount));
        binding.tvTechKpiRepairing.setText(String.valueOf(repairingCount));
        binding.tvTechKpiCompleted.setText(String.valueOf(completedCount));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
