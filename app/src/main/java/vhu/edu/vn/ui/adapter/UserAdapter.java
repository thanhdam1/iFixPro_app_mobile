package vhu.edu.vn.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import vhu.edu.vn.R;
import vhu.edu.vn.data.local.entity.UserEntity;
import vhu.edu.vn.databinding.ItemUserBinding;
import vhu.edu.vn.utils.Constants;
import vhu.edu.vn.utils.UiFormatters;

public class UserAdapter extends ListAdapter<UserEntity, UserAdapter.UserViewHolder> {

    public UserAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setUsers(List<UserEntity> users) {
        submitList(users != null ? Collections.unmodifiableList(new ArrayList<>(users)) : null);
    }

    private static final DiffUtil.ItemCallback<UserEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<UserEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull UserEntity oldItem, @NonNull UserEntity newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull UserEntity oldItem, @NonNull UserEntity newItem) {
            return Objects.equals(oldItem.getUsername(), newItem.getUsername())
                    && Objects.equals(oldItem.getFullName(), newItem.getFullName())
                    && Objects.equals(oldItem.getPhone(), newItem.getPhone())
                    && Objects.equals(oldItem.getRole(), newItem.getRole());
        }
    };

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding binding = ItemUserBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemUserBinding binding;

        UserViewHolder(@NonNull ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(UserEntity user) {
            Context context = itemView.getContext();
            binding.tvUserFullName.setText(user.getFullName());
            binding.tvUserInitials.setText(UiFormatters.initials(user.getFullName()));

            String phone = user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "Chưa có SĐT";
            binding.tvUserUsernamePhone.setText("@" + user.getUsername() + " · " + phone);

            String role = user.getRole();
            binding.tvUserRoleBadge.setText(UiFormatters.roleDisplayName(context, role));
            binding.tvUserRoleBadge.getBackground().mutate();

            if (Constants.ROLE_MANAGER.equals(role)) {
                binding.tvUserRoleBadge.getBackground().setTint(ContextCompat.getColor(context, R.color.primary_container));
                binding.tvUserRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.on_primary_container));
            } else if (Constants.ROLE_RECEPTIONIST.equals(role)) {
                binding.tvUserRoleBadge.getBackground().setTint(ContextCompat.getColor(context, R.color.secondary_container));
                binding.tvUserRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.on_secondary_container));
            } else if (Constants.ROLE_TECHNICIAN.equals(role)) {
                binding.tvUserRoleBadge.getBackground().setTint(ContextCompat.getColor(context, R.color.warning_container));
                binding.tvUserRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.on_warning_container));
            } else {
                binding.tvUserRoleBadge.getBackground().setTint(ContextCompat.getColor(context, R.color.surface_variant));
                binding.tvUserRoleBadge.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant));
            }
        }
    }
}
