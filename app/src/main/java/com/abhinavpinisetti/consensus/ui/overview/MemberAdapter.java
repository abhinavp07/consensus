package com.abhinavpinisetti.consensus.ui.overview;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.databinding.ItemMemberBinding;
import com.abhinavpinisetti.consensus.ui.common.Avatars;
import com.google.android.material.color.MaterialColors;

import java.util.Objects;

class MemberAdapter extends ListAdapter<MemberAdapter.Row, MemberAdapter.Holder> {

    static class Row {
        final String uid;
        final String name;
        final String photoUrl;
        final boolean owner;
        final boolean ready;

        Row(String uid, String name, String photoUrl, boolean owner, boolean ready) {
            this.uid = uid;
            this.name = name;
            this.photoUrl = photoUrl;
            this.owner = owner;
            this.ready = ready;
        }
    }

    MemberAdapter() {
        super(new DiffUtil.ItemCallback<Row>() {
            @Override
            public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
                return a.uid.equals(b.uid);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
                return Objects.equals(a.name, b.name) && Objects.equals(a.photoUrl, b.photoUrl)
                        && a.owner == b.owner && a.ready == b.ready;
            }
        });
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemMemberBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Row row = getItem(position);
        ItemMemberBinding b = holder.binding;
        Avatars.load(b.avatar, row.photoUrl);
        b.name.setText(row.name);

        String status = b.getRoot().getContext().getString(
                row.ready ? R.string.preferences_submitted : R.string.waiting_for_answers);
        if (row.owner) status = b.getRoot().getContext().getString(R.string.owner) + " · " + status;
        b.subtitle.setText(status);

        b.status.setImageResource(row.ready ? R.drawable.ic_check_circle : R.drawable.ic_pending);
        int color = row.ready
                ? ContextCompat.getColor(b.getRoot().getContext(), R.color.ready_green)
                : MaterialColors.getColor(b.status, com.google.android.material.R.attr.colorOutline);
        b.status.setImageTintList(ColorStateList.valueOf(color));
        b.status.setContentDescription(status);
        b.getRoot().setAlpha(row.ready ? 1f : 0.75f);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemMemberBinding binding;

        Holder(ItemMemberBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
