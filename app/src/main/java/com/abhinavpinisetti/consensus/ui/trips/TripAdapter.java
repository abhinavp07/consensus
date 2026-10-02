package com.abhinavpinisetti.consensus.ui.trips;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.databinding.ItemTripBinding;
import com.abhinavpinisetti.consensus.util.DateUtils;

import java.util.Objects;

class TripAdapter extends ListAdapter<Trip, TripAdapter.Holder> {

    interface OnTripClick {
        void onClick(Trip trip);
    }

    private final OnTripClick listener;

    TripAdapter(OnTripClick listener) {
        super(new DiffUtil.ItemCallback<Trip>() {
            @Override
            public boolean areItemsTheSame(@NonNull Trip a, @NonNull Trip b) {
                return Objects.equals(a.id, b.id);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Trip a, @NonNull Trip b) {
                return Objects.equals(a.name, b.name) && Objects.equals(a.destination, b.destination)
                        && Objects.equals(a.startDate, b.startDate) && Objects.equals(a.endDate, b.endDate)
                        && Objects.equals(a.status, b.status) && a.memberIds.size() == b.memberIds.size();
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemTripBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Trip trip = getItem(position);
        ItemTripBinding b = holder.binding;
        b.name.setText(trip.name);
        b.destination.setText(trip.destination);
        b.dates.setText(DateUtils.formatRange(trip.startDate, trip.endDate));
        b.members.setText(b.getRoot().getContext().getString(R.string.members_count, trip.memberIds.size()));
        b.status.setText(Trip.STATUS_PLANNED.equals(trip.status) ? R.string.status_planned : R.string.status_collecting);
        b.getRoot().setOnClickListener(v -> listener.onClick(trip));
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemTripBinding binding;

        Holder(ItemTripBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
