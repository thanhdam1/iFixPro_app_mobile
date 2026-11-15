package vhu.edu.vn.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vhu.edu.vn.R;
import vhu.edu.vn.data.local.model.TicketWithDetails;
import vhu.edu.vn.data.repository.RepairTicketRepository;
import vhu.edu.vn.databinding.FragmentCustomerHomeBinding;
import vhu.edu.vn.ui.adapter.TicketAdapter;
import vhu.edu.vn.ui.auth.LoginActivity;
import vhu.edu.vn.utils.Constants;
import vhu.edu.vn.utils.SessionManager;
import vhu.edu.vn.utils.TicketSearch;

public class CustomerHomeFragment extends Fragment {
    private FragmentCustomerHomeBinding binding;
    private TicketAdapter adapter;
    private List<TicketWithDetails> allTickets = Collections.emptyList();
    private boolean hasLoaded;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentCustomerHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SessionManager sessionManager = new SessionManager(requireContext());
        int customerId = sessionManager.getUserId();
        if (!sessionManager.isLoggedIn() || customerId <= 0) {
            sessionManager.logoutUser();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.putExtra(LoginActivity.EXTRA_SESSION_EXPIRED, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
            return;
        }

        String displayName = sessionManager.getFullName();
        if (displayName.trim().isEmpty()) {
            displayName = getString(R.string.role_customer);
        }
        binding.tvCustomerGreeting.setText(getString(R.string.greeting_hello, displayName));

        adapter = new TicketAdapter();
        adapter.setShowCustomerInfo(false);
        adapter.setStaffMode(false);
        adapter.setOnTicketClickListener(item -> {
            if (item == null || item.ticket == null) return;
            Intent intent = new Intent(requireContext(), TicketTimelineActivity.class);
            intent.putExtra("ticket_id", item.ticket.getId());
            startActivity(intent);
        });

        binding.rvCustomerTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCustomerTickets.setAdapter(adapter);

        setupFilterControls();

        RepairTicketRepository repository = new RepairTicketRepository(requireContext());
        repository.getTicketsByCustomer(customerId).observe(getViewLifecycleOwner(), tickets -> {
            hasLoaded = true;
            binding.customerLoading.setVisibility(View.GONE);
            allTickets = tickets != null ? tickets : Collections.emptyList();
            updateKpis();
            applyFilters();
        });
    }

    private void setupFilterControls() {
        binding.edtTicketSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        binding.chipTicketFilters.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilters());
        View.OnClickListener resetListener = v -> resetFilters();
        binding.btnCustomerClearFilter.setOnClickListener(resetListener);
        binding.btnEmptyReset.setOnClickListener(resetListener);
    }

    private void resetFilters() {
        binding.edtTicketSearch.setText("");
        binding.chipAll.setChecked(true);
        applyFilters();
    }

    private void updateKpis() {
        int activeCount = 0;
        int waitingApprovalCount = 0;
        int readyCount = 0;

        for (TicketWithDetails item : allTickets) {
            if (item.ticket == null) continue;
            String status = item.ticket.getStatus();
            if (TicketSearch.isActive(status)) {
                activeCount++;
            }
            if (Constants.TICKET_WAITING_APPROVAL.equals(status)) {
                waitingApprovalCount++;
            }
            if (Constants.TICKET_COMPLETED.equals(status)) {
                readyCount++;
            }
        }

        binding.tvCustomerKpiActive.setText(String.valueOf(activeCount));
        binding.tvCustomerKpiApproval.setText(String.valueOf(waitingApprovalCount));
        binding.tvCustomerKpiReady.setText(String.valueOf(readyCount));
    }

    private void applyFilters() {
        if (binding == null || !hasLoaded) return;

        String query = binding.edtTicketSearch.getText() != null
                ? binding.edtTicketSearch.getText().toString()
                : "";
        boolean hasFilter = !query.trim().isEmpty() || !binding.chipAll.isChecked();
        binding.btnCustomerClearFilter.setVisibility(hasFilter ? View.VISIBLE : View.GONE);

        List<TicketWithDetails> filtered = new ArrayList<>();
        for (TicketWithDetails item : allTickets) {
            if (item.ticket == null) continue;
            String status = item.ticket.getStatus();
            if (!matchesSelectedChip(status)) continue;

            if (TicketSearch.matches(
                    query,
                    item.ticket.getTicketCode(),
                    item.device != null ? item.device.getBrandModel() : null,
                    item.device != null ? item.device.getImeiSerial() : null,
                    item.ticket.getCustomerNote()
            )) {
                filtered.add(item);
            }
        }

        adapter.setTickets(filtered);
        binding.tvTicketCount.setText(getString(R.string.ticket_result_count, filtered.size(), allTickets.size()));

        if (allTickets.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.rvCustomerTickets.setVisibility(View.GONE);
            binding.tvEmptyTitle.setText(R.string.no_tickets_title);
            binding.tvEmptyDescription.setText(R.string.no_tickets_description);
            binding.btnEmptyReset.setVisibility(View.GONE);
        } else if (filtered.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.rvCustomerTickets.setVisibility(View.GONE);
            binding.tvEmptyTitle.setText(R.string.no_results_title);
            binding.tvEmptyDescription.setText(R.string.no_results_description);
            binding.btnEmptyReset.setVisibility(View.VISIBLE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.rvCustomerTickets.setVisibility(View.VISIBLE);
        }
    }

    private boolean matchesSelectedChip(String status) {
        if (binding.chipActive.isChecked()) {
            return TicketSearch.isActive(status);
        }
        if (binding.chipApproval.isChecked()) {
            return Constants.TICKET_WAITING_APPROVAL.equals(status);
        }
        if (binding.chipReady.isChecked()) {
            return Constants.TICKET_COMPLETED.equals(status);
        }
        if (binding.chipClosed.isChecked()) {
            return Constants.TICKET_DELIVERED.equals(status)
                    || Constants.TICKET_REJECTED.equals(status)
                    || Constants.TICKET_CANCELLED.equals(status);
        }
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
