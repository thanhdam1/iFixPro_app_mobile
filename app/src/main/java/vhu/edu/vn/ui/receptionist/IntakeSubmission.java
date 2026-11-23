package vhu.edu.vn.ui.receptionist;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import vhu.edu.vn.data.repository.RepairTicketRepository;

/** Retains the pending Room operation and its result across Activity recreation. */
public class IntakeSubmission extends ViewModel implements RepairTicketRepository.TicketCreateCallback {
    final MutableLiveData<String> createdCode = new MutableLiveData<>();
    final MutableLiveData<String> error = new MutableLiveData<>();
    boolean pending;
    int ticketId;

    @Override
    public void onSuccess(int ticketId, String ticketCode) {
        this.ticketId = ticketId;
        pending = false;
        createdCode.setValue(ticketCode);
    }

    @Override
    public void onError(String message) {
        pending = false;
        error.setValue(message);
    }
}
