package vhu.edu.vn.ui.receptionist;

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
import vhu.edu.vn.databinding.FragmentReceptionistHomeBinding;
import vhu.edu.vn.ui.adapter.StaffTicketList;
import vhu.edu.vn.ui.adapter.TicketAdapter;
import vhu.edu.vn.utils.Constants;

public class ReceptionistHomeFragment extends Fragment {
    private FragmentReceptionistHomeBinding binding;
    private StaffTicketList ticketList;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentReceptionistHomeBinding.inflate(inflater, container, false);
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
            Intent intent = new Intent(requireContext(), PrintInvoiceActivity.class);
            intent.putExtra("ticket_id", item.ticket.getId());
            startActivity(intent);
        });

        binding.rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTickets.setAdapter(adapter);

        ticketList = new StaffTicketList(binding.getRoot(), adapter);
        ticketList.configureSecondaryChips(
                getString(R.string.kpi_ready_delivery),
                Constants.TICKET_COMPLETED::equals,
                getString(R.string.filter_approval),
                Constants.TICKET_WAITING_APPROVAL::equals
        );

        binding.fabCreateTicket.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateTicketActivity.class))
        );

        RepairTicketRepository repository = new RepairTicketRepository(requireContext());
        repository.getAllTickets().observe(getViewLifecycleOwner(), tickets -> {
            updateKpis(tickets);
            ticketList.submit(tickets);
        });
    }

    private void updateKpis(List<TicketWithDetails> tickets) {
        if (binding == null) return;
        int received = 0;
        int active = 0;
        int ready = 0;
        int attention = 0;

        if (tickets != null) {
            for (TicketWithDetails item : tickets) {
                if (item.ticket == null) continue;
                String status = item.ticket.getStatus();
                if (Constants.TICKET_RECEIVED.equals(status)) {
                    received++;
                } else if (Constants.TICKET_DIAGNOSING.equals(status)
                        || Constants.TICKET_APPROVED.equals(status)
                        || Constants.TICKET_REPAIRING.equals(status)) {
                    active++;
                } else if (Constants.TICKET_COMPLETED.equals(status)) {
                    ready++;
                } else if (Constants.TICKET_WAITING_APPROVAL.equals(status)) {
                    attention++;
                }
            }
        }

        binding.tvRecKpiReceived.setText(String.valueOf(received));
        binding.tvRecKpiActive.setText(String.valueOf(active));
        binding.tvRecKpiReady.setText(String.valueOf(ready));
        binding.tvRecKpiAttention.setText(String.valueOf(attention));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
