package vhu.edu.vn.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import vhu.edu.vn.data.local.entity.InventoryPartEntity;
import vhu.edu.vn.databinding.ItemInventoryPartBinding;
import vhu.edu.vn.utils.UiFormatters;

public class InventoryAdapter extends ListAdapter<InventoryPartEntity, InventoryAdapter.InventoryViewHolder> {

    private OnPartClickListener clickListener;

    public interface OnPartClickListener {
        void onPartClick(InventoryPartEntity part);
    }

    public InventoryAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnPartClickListener(OnPartClickListener listener) {
        this.clickListener = listener;
    }

    public void setParts(List<InventoryPartEntity> parts) {
        submitList(parts != null ? Collections.unmodifiableList(new ArrayList<>(parts)) : null);
    }

    private static final DiffUtil.ItemCallback<InventoryPartEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<InventoryPartEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull InventoryPartEntity oldItem, @NonNull InventoryPartEntity newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull InventoryPartEntity oldItem, @NonNull InventoryPartEntity newItem) {
            return Objects.equals(oldItem.getPartCode(), newItem.getPartCode())
                    && Objects.equals(oldItem.getPartName(), newItem.getPartName())
                    && Objects.equals(oldItem.getCategory(), newItem.getCategory())
                    && oldItem.getQuantity() == newItem.getQuantity()
                    && oldItem.getMinThreshold() == newItem.getMinThreshold()
                    && Double.compare(oldItem.getImportPrice(), newItem.getImportPrice()) == 0
                    && Double.compare(oldItem.getSellingPrice(), newItem.getSellingPrice()) == 0;
        }
    };

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInventoryPartBinding binding = ItemInventoryPartBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new InventoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class InventoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemInventoryPartBinding binding;

        InventoryViewHolder(@NonNull ItemInventoryPartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(InventoryPartEntity part) {
            binding.tvPartName.setText(part.getPartName());
            String category = part.getCategory() != null && !part.getCategory().isEmpty() ? part.getCategory() : "Chưa phân loại";
            binding.tvPartCodeCategory.setText(part.getPartCode() + " · " + category);
            binding.tvStockQuantity.setText("Tồn kho: " + part.getQuantity() + " (cảnh báo ≤ " + part.getMinThreshold() + ")");
            binding.tvImportPrice.setText("Giá vốn: " + UiFormatters.formatCurrency(part.getImportPrice()));
            binding.tvSellingPrice.setText(UiFormatters.formatCurrency(part.getSellingPrice()));

            boolean isLow = part.getQuantity() <= part.getMinThreshold();
            binding.badgeLowStock.setVisibility(isLow ? View.VISIBLE : View.GONE);
            if (isLow) {
                binding.tvLowStockBadge.setText(part.getQuantity() == 0 ? "HẾT HÀNG" : "SẮP HẾT HÀNG");
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onPartClick(part);
                }
            });
        }
    }
}
