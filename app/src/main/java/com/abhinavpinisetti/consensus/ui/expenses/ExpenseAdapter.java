package com.abhinavpinisetti.consensus.ui.expenses;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.model.Expense;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.databinding.ItemExpenseBinding;
import com.abhinavpinisetti.consensus.util.Money;

import java.util.Objects;

class ExpenseAdapter extends ListAdapter<Expense, ExpenseAdapter.Holder> {

    interface OnLongPress {
        void onLongPress(Expense expense);
    }

    private final OnLongPress listener;
    private Trip trip;

    ExpenseAdapter(OnLongPress listener) {
        super(new DiffUtil.ItemCallback<Expense>() {
            @Override
            public boolean areItemsTheSame(@NonNull Expense a, @NonNull Expense b) {
                return Objects.equals(a.id, b.id);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Expense a, @NonNull Expense b) {
                return Objects.equals(a.description, b.description) && a.amountCents == b.amountCents
                        && Objects.equals(a.paidBy, b.paidBy) && Objects.equals(a.splitAmong, b.splitAmong)
                        && Objects.equals(a.createdAt, b.createdAt);
            }
        });
        this.listener = listener;
    }

    void setTrip(Trip trip) {
        this.trip = trip;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemExpenseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Expense e = getItem(position);
        ItemExpenseBinding b = holder.binding;
        b.description.setText(e.description);
        b.amount.setText(Money.formatCents(e.amountCents));
        String payer = trip == null ? "" : trip.firstNameOf(e.paidBy);
        int ways = e.splitAmong.size();
        String only = ways == 1 && trip != null ? trip.firstNameOf(e.splitAmong.get(0)) : "";
        String line = b.getRoot().getContext().getResources()
                .getQuantityString(R.plurals.paid_by_line, ways, payer, ways, only);
        if (e.createdAt != null) {
            line += " · " + DateUtils.getRelativeTimeSpanString(e.createdAt.toDate().getTime());
        }
        b.subtitle.setText(line);
        b.getRoot().setOnLongClickListener(v -> {
            listener.onLongPress(e);
            return true;
        });
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemExpenseBinding binding;

        Holder(ItemExpenseBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
