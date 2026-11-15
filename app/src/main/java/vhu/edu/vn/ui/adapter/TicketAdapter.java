package vhu.edu.vn.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Objects;
import vhu.edu.vn.R;
import vhu.edu.vn.data.local.model.TicketWithDetails;
import vhu.edu.vn.databinding.ItemRepairTicketBinding;
import vhu.edu.vn.utils.Constants;
import vhu.edu.vn.utils.UiFormatters;

public class TicketAdapter extends ListAdapter<TicketWithDetails, TicketAdapter.TicketViewHolder> {

    private OnTicketClickListener clickListener;
    private boolean showCustomerInfo = true;
    private boolean staffMode = false;

    public interface OnTicketClickListener {
        void onTicketClick(TicketWithDetails ticket);
    }

    public TicketAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setShowCustomerInfo(boolean showCustomerInfo) {
        this.showCustomerInfo = showCustomerInfo;
    }

    public void setStaffMode(boolean staffMode) {
        this.staffMode = staffMode;
    }

    public void setOnTicketClickListener(OnTicketClickListener listener) {
        this.clickListener = listener;
    }

    public void setTickets(List<TicketWithDetails> tickets) {
        submitList(tickets != null ? java.util.Collections.unmodifiableList(new java.util.ArrayList<>(tickets)) : null);
    }

    private static final DiffUtil.ItemCallback<TicketWithDetails> DIFF_CALLBACK = new DiffUtil.ItemCallback<TicketWithDetails>() {
        @Override
        public boolean areItemsTheSame(@NonNull TicketWithDetails oldItem, @NonNull TicketWithDetails newItem) {
            if (oldItem.ticket == null || newItem.ticket == null) return false;
            return oldItem.ticket.getId() == newItem.ticket.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull TicketWithDetails oldItem, @NonNull TicketWithDetails newItem) {
            if (oldItem.ticket == null || newItem.ticket == null) return false;
            return oldItem.ticket.getUpdatedAt() == newItem.ticket.getUpdatedAt()
                    && Objects.equals(oldItem.ticket.getStatus(), newItem.ticket.getStatus())
                    && Double.compare(oldItem.ticket.getTotalCost(), newItem.ticket.getTotalCost()) == 0
                    && Objects.equals(oldItem.device != null ? oldItem.device.getBrandModel() : null,
                                      newItem.device != null ? newItem.device.getBrandModel() : null);
        }
    };

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRepairTicketBinding binding = ItemRepairTicketBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new TicketViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class TicketViewHolder extends RecyclerView.ViewHolder {
        private final ItemRepairTicketBinding binding;

        TicketViewHolder(@NonNull ItemRepairTicketBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TicketWithDetails item) {
            Context context = itemView.getContext();

            // Reset reused ViewHolder fields cleanly
            binding.tvTicketCode.setText("");
            binding.tvDeviceName.setText(R.string.unknown_device);
            binding.tvDeviceImei.setText("");
            binding.tvCustomerInfo.setText(R.string.unknown_customer);
            binding.tvCustomerInfo.setVisibility(showCustomerInfo ? View.VISIBLE : View.GONE);
            binding.tvFaultNote.setText("");
            binding.tvDate.setText("");
            binding.tvTotalCost.setText(R.string.quote_pending_label);
            binding.tvTotalCost.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
            binding.tvActionLabel.setText(R.string.cta_view_progress);
            binding.imgDeviceType.setImageResource(R.drawable.ic_device_phone);

            String status = item != null && item.ticket != null ? item.ticket.getStatus() : null;
            applyStatusBadge(context, status);

            if (item != null && item.device != null) {
                binding.tvDeviceName.setText(item.device.getBrandModel());
                binding.tvDeviceImei.setText(UiFormatters.deviceTypeLabel(item.device.getDeviceType())
                        + " · IMEI/SN: " + item.device.getImeiSerial());
                binding.imgDeviceType.setImageResource(UiFormatters.deviceTypeIcon(item.device.getDeviceType()));
            }

            if (item != null && item.customer != null && showCustomerInfo) {
                binding.tvCustomerInfo.setText("Khách: " + item.customer.getFullName() + " · " + item.customer.getPhone());
            }

            if (item != null && item.ticket != null) {
                binding.tvTicketCode.setText(item.ticket.getTicketCode());
                String fault = item.ticket.getCustomerNote();
                binding.tvFaultNote.setText(fault != null && !fault.trim().isEmpty()
                        ? "Mô tả lỗi: " + fault.trim()
                        : "Mô tả lỗi: Chưa ghi nhận");

                long displayTime = item.ticket.getUpdatedAt() > 0
                        ? item.ticket.getUpdatedAt()
                        : item.ticket.getCreatedAt();
                binding.tvDate.setText("Cập nhật: " + UiFormatters.formatDateTime(displayTime));

                boolean hasQuote = item.ticket.getTotalCost() > 0.0
                        || Constants.TICKET_WAITING_APPROVAL.equals(status)
                        || Constants.TICKET_APPROVED.equals(status)
                        || Constants.TICKET_REPAIRING.equals(status)
                        || Constants.TICKET_COMPLETED.equals(status)
                        || Constants.TICKET_DELIVERED.equals(status);

                if (hasQuote && item.ticket.getTotalCost() > 0.0) {
                    binding.tvTotalCost.setText(UiFormatters.formatCurrency(item.ticket.getTotalCost()));
                    binding.tvTotalCost.setTextColor(ContextCompat.getColor(context, R.color.on_surface));
                } else {
                    binding.tvTotalCost.setText(R.string.quote_pending_label);
                    binding.tvTotalCost.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
                }

                if (staffMode) {
                    binding.tvActionLabel.setText(Constants.TICKET_COMPLETED.equals(status) ? "Bàn giao / Hóa đơn" : "Xử lý phiếu");
                } else if (Constants.TICKET_WAITING_APPROVAL.equals(status)) {
                    binding.tvActionLabel.setText(R.string.cta_view_quote);
                } else {
                    binding.tvActionLabel.setText(R.string.cta_view_progress);
                }
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null && item != null) {
                    clickListener.onTicketClick(item);
                }
            });
        }

        private void applyStatusBadge(Context context, String status) {
            UiFormatters.StatusStyle style = UiFormatters.statusStyle(status);
            int textColor = ContextCompat.getColor(context, style.textColor);
            int bgColor = ContextCompat.getColor(context, style.bgColor);
            binding.tvStatusBadge.setText(style.label);
            binding.tvStatusBadge.setTextColor(textColor);
            binding.imgStatusIcon.setImageResource(style.iconRes);
            binding.imgStatusIcon.setColorFilter(textColor);
            binding.badgeStatusContainer.getBackground().mutate();
            binding.badgeStatusContainer.getBackground().setTint(bgColor);
        }
    }
}
