package com.abhinavpinisetti.consensus.ui.itinerary;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.model.PlannedActivity;
import com.abhinavpinisetti.consensus.databinding.ItemActivityBinding;
import com.abhinavpinisetti.consensus.util.Money;
import com.google.android.material.color.MaterialColors;

import java.util.Objects;

/** Activity cards: time, title, description, cost, place, votes and actions. */
class ActivityAdapter extends ListAdapter<PlannedActivity, ActivityAdapter.Holder> {

    interface Listener {
        void onEdit(PlannedActivity activity);

        void onVote(PlannedActivity activity, int value);

        void onFindAlternatives(PlannedActivity activity);

        void onDirections(PlannedActivity activity);
    }

    private final Listener listener;
    private String uid;
    private int memberCount;

    ActivityAdapter(Listener listener) {
        super(new DiffUtil.ItemCallback<PlannedActivity>() {
            @Override
            public boolean areItemsTheSame(@NonNull PlannedActivity a, @NonNull PlannedActivity b) {
                return Objects.equals(a.id, b.id);
            }

            @Override
            public boolean areContentsTheSame(@NonNull PlannedActivity a, @NonNull PlannedActivity b) {
                return Objects.equals(a.title, b.title) && Objects.equals(a.time, b.time)
                        && Objects.equals(a.description, b.description) && a.estimatedCost == b.estimatedCost
                        && Objects.equals(a.votes, b.votes) && Objects.equals(a.address, b.address)
                        && a.placeLookupDone == b.placeLookupDone && Objects.equals(a.location, b.location)
                        && Objects.equals(a.source, b.source) && Objects.equals(a.placeName, b.placeName);
            }
        });
        this.listener = listener;
    }

    /** Votes and the "Needs a swap" badge depend on who's viewing and how many members there are. */
    void setContext(String uid, int memberCount) {
        if (Objects.equals(this.uid, uid) && this.memberCount == memberCount) return;
        this.uid = uid;
        this.memberCount = memberCount;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemActivityBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        PlannedActivity a = getItem(position);
        ItemActivityBinding b = holder.binding;

        b.time.setText(a.time);
        b.title.setText(a.title);
        b.description.setText(a.description);
        b.description.setVisibility(a.description == null || a.description.isEmpty() ? View.GONE : View.VISIBLE);
        b.cost.setText(Money.formatEstimate(a.estimatedCost));
        b.aiBadge.setVisibility(PlannedActivity.SOURCE_AI.equals(a.source) ? View.VISIBLE : View.GONE);
        b.swapBadge.setVisibility(a.needsSwap(memberCount) ? View.VISIBLE : View.GONE);

        if (a.location != null && a.address != null) {
            b.place.setText(a.address);
            b.place.setVisibility(View.VISIBLE);
            b.place.setAlpha(1f);
        } else if (a.isLocationMissing()) {
            b.place.setText(R.string.location_not_found);
            b.place.setVisibility(View.VISIBLE);
            b.place.setAlpha(0.6f);
        } else {
            b.place.setVisibility(View.GONE);
        }

        int myVote = a.voteOf(uid);
        int active = MaterialColors.getColor(b.voteUp, androidx.appcompat.R.attr.colorPrimary);
        int down = MaterialColors.getColor(b.voteDown, androidx.appcompat.R.attr.colorError);
        int idle = MaterialColors.getColor(b.voteUp, com.google.android.material.R.attr.colorOnSurfaceVariant);
        b.voteUp.setIconTint(ColorStateList.valueOf(myVote == 1 ? active : idle));
        b.voteDown.setIconTint(ColorStateList.valueOf(myVote == -1 ? down : idle));
        int score = a.netScore();
        b.score.setText(score > 0 ? "+" + score : String.valueOf(score));

        boolean hasPlace = a.location != null || (a.placeName != null && !a.placeName.isEmpty());
        b.directions.setVisibility(hasPlace ? View.VISIBLE : View.GONE);

        b.getRoot().setOnClickListener(v -> listener.onEdit(a));
        b.voteUp.setOnClickListener(v -> listener.onVote(a, 1));
        b.voteDown.setOnClickListener(v -> listener.onVote(a, -1));
        b.alternatives.setOnClickListener(v -> listener.onFindAlternatives(a));
        b.directions.setOnClickListener(v -> listener.onDirections(a));
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemActivityBinding binding;

        Holder(ItemActivityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
