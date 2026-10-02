package com.abhinavpinisetti.consensus.ui.chat;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.abhinavpinisetti.consensus.data.model.Message;
import com.abhinavpinisetti.consensus.databinding.ItemMessageMineBinding;
import com.abhinavpinisetti.consensus.databinding.ItemMessageOtherBinding;
import com.abhinavpinisetti.consensus.ui.common.Avatars;
import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.Objects;

class MessageAdapter extends ListAdapter<Message, RecyclerView.ViewHolder> {

    private static final int MINE = 1;
    private static final int OTHER = 2;

    private final String uid;

    MessageAdapter(String uid) {
        super(new DiffUtil.ItemCallback<Message>() {
            @Override
            public boolean areItemsTheSame(@NonNull Message a, @NonNull Message b) {
                return Objects.equals(a.id, b.id);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Message a, @NonNull Message b) {
                return Objects.equals(a.text, b.text) && Objects.equals(a.timestamp, b.timestamp)
                        && Objects.equals(a.senderName, b.senderName);
            }
        });
        this.uid = uid;
    }

    @Override
    public int getItemViewType(int position) {
        return Objects.equals(getItem(position).senderId, uid) ? MINE : OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == MINE) {
            ItemMessageMineBinding b = ItemMessageMineBinding.inflate(inflater, parent, false);
            return new MineHolder(b);
        }
        return new OtherHolder(ItemMessageOtherBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message m = getItem(position);
        if (holder instanceof MineHolder) {
            ItemMessageMineBinding b = ((MineHolder) holder).binding;
            b.text.setText(m.text);
            b.time.setText(formatTime(b.getRoot().getContext(), m.timestamp));
        } else {
            ItemMessageOtherBinding b = ((OtherHolder) holder).binding;
            b.text.setText(m.text);
            b.name.setText(m.senderName);
            b.time.setText(formatTime(b.getRoot().getContext(), m.timestamp));
            // Group consecutive messages from the same sender under one avatar/name.
            boolean sameAsPrevious = position > 0 && Objects.equals(getItem(position - 1).senderId, m.senderId);
            b.name.setVisibility(sameAsPrevious ? View.GONE : View.VISIBLE);
            b.avatar.setVisibility(sameAsPrevious ? View.INVISIBLE : View.VISIBLE);
            if (!sameAsPrevious) Avatars.load(b.avatar, m.senderPhotoUrl);
        }
    }

    private static String formatTime(Context context, Timestamp timestamp) {
        if (timestamp == null) return "";
        Date date = timestamp.toDate();
        String time = DateFormat.getTimeFormat(context).format(date);
        if (DateUtils.isToday(date.getTime())) return time;
        return DateFormat.getMediumDateFormat(context).format(date) + ", " + time;
    }

    static class MineHolder extends RecyclerView.ViewHolder {
        final ItemMessageMineBinding binding;

        MineHolder(ItemMessageMineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class OtherHolder extends RecyclerView.ViewHolder {
        final ItemMessageOtherBinding binding;

        OtherHolder(ItemMessageOtherBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
