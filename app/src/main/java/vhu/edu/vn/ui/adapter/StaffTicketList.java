package vhu.edu.vn.ui.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import vhu.edu.vn.R;
import vhu.edu.vn.data.local.model.TicketWithDetails;
import vhu.edu.vn.utils.Constants;
import vhu.edu.vn.utils.TicketSearch;

/** View-scoped search and status filter controls shared by receptionist and technician lists. */
public final class StaffTicketList {
    public interface FilterMode {
        boolean matches(String status);
    }

    private final View root;
    private final TicketAdapter adapter;
    private final TextInputEditText search;
    private final ChipGroup filterGroup;
    private final Chip chipAll;
    private final Chip chipActive;
    private final Chip chipSecondary;
    private final Chip chipThird;
    private final TextView count;
    private final TextView empty;
    private final MaterialCardView emptyCard;
    private final MaterialButton btnClearFilter;
    private final LinearProgressIndicator loading;

    private List<TicketWithDetails> tickets = Collections.emptyList();
    private boolean loaded;

    private FilterMode secondaryFilter = Constants.TICKET_COMPLETED::equals;
    private FilterMode thirdFilter = Constants.TICKET_WAITING_APPROVAL::equals;

    public StaffTicketList(View root, TicketAdapter adapter) {
        this.root = root;
        this.adapter = adapter;
        search = root.findViewById(R.id.staffSearch);
        filterGroup = root.findViewById(R.id.staffFilterGroup);
        chipAll = root.findViewById(R.id.chipStaffAll);
        chipActive = root.findViewById(R.id.chipStaffActive);
        chipSecondary = root.findViewById(R.id.chipStaffSecondary);
        chipThird = root.findViewById(R.id.chipStaffThird);
        count = root.findViewById(R.id.staffCount);
        empty = root.findViewById(R.id.staffEmpty);
        emptyCard = root.findViewById(R.id.staffEmptyCard);
        btnClearFilter = root.findViewById(R.id.btnStaffClearFilter);
        loading = root.findViewById(R.id.staffLoading);

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> render());
        btnClearFilter.setOnClickListener(v -> resetFilters());
    }

    public void configureSecondaryChips(String secondaryLabel, FilterMode secondaryMode, String thirdLabel, FilterMode thirdMode) {
        chipSecondary.setText(secondaryLabel);
        this.secondaryFilter = secondaryMode;
        chipThird.setText(thirdLabel);
        this.thirdFilter = thirdMode;
    }

    public void resetFilters() {
        search.setText("");
        chipAll.setChecked(true);
        render();
    }

    public void submit(List<TicketWithDetails> rows) {
        tickets = rows != null ? rows : Collections.emptyList();
        loaded = true;
        loading.setVisibility(View.GONE);
        render();
    }

    private void render() {
        if (!loaded) return;
        String query = search.getText() != null ? search.getText().toString() : "";
        boolean hasFilter = !query.trim().isEmpty() || !chipAll.isChecked();
        btnClearFilter.setVisibility(hasFilter ? View.VISIBLE : View.GONE);

        List<TicketWithDetails> result = new ArrayList<>();
        for (TicketWithDetails row : tickets) {
            if (row.ticket == null) continue;
            String status = row.ticket.getStatus();
            if (chipActive.isChecked() && !TicketSearch.isActive(status)) continue;
            if (chipSecondary.isChecked() && !secondaryFilter.matches(status)) continue;
            if (chipThird.isChecked() && !thirdFilter.matches(status)) continue;

            if (TicketSearch.matches(
                    query,
                    row.ticket.getTicketCode(),
                    row.device != null ? row.device.getBrandModel() : null,
                    row.device != null ? row.device.getImeiSerial() : null,
                    row.customer != null ? row.customer.getFullName() : null,
                    row.customer != null ? row.customer.getPhone() : null
            )) {
                result.add(row);
            }
        }
        adapter.setTickets(result);
        count.setText(root.getContext().getString(R.string.ticket_result_count, result.size(), tickets.size()));
        emptyCard.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText(tickets.isEmpty() ? R.string.staff_empty : R.string.staff_no_results);
    }
}
