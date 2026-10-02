package com.abhinavpinisetti.consensus.ui.expenses;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.model.Expense;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.databinding.ActivitySettleUpBinding;
import com.abhinavpinisetti.consensus.databinding.ItemBalanceBinding;
import com.abhinavpinisetti.consensus.ui.common.Avatars;
import com.abhinavpinisetti.consensus.ui.common.BaseActivity;
import com.abhinavpinisetti.consensus.util.Money;
import com.abhinavpinisetti.consensus.util.SettleUp;
import com.google.android.material.color.MaterialColors;

import java.util.List;
import java.util.Map;

/** Each person's balance (paid minus share) and the fewest payments to settle up. */
public class SettleUpActivity extends BaseActivity {

    private static final String EXTRA_TRIP_ID = "tripId";

    private ActivitySettleUpBinding binding;
    private Trip trip;
    private List<Expense> expenses;

    public static Intent intent(Context context, String tripId) {
        return new Intent(context, SettleUpActivity.class).putExtra(EXTRA_TRIP_ID, tripId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (requireUid() == null) return;

        binding = ActivitySettleUpBinding.inflate(getLayoutInflater());
        setContentViewWithInsets(binding.getRoot());
        setupToolbar(binding.toolbar, true);

        ExpensesViewModel viewModel = new ViewModelProvider(this).get(ExpensesViewModel.class);
        viewModel.init(getIntent().getStringExtra(EXTRA_TRIP_ID));
        viewModel.trip().observe(this, r -> {
            if (r.isSuccess() && r.data != null) {
                trip = r.data;
                render();
            }
        });
        viewModel.expenses().observe(this, r -> {
            binding.progress.setVisibility(r.isLoading() ? View.VISIBLE : View.GONE);
            if (r.isSuccess()) {
                expenses = r.data;
                render();
            }
        });
    }

    private String nameOf(String id) {
        return trip.memberIds.contains(id) ? trip.nameOf(id) : "Former member";
    }

    private void render() {
        if (trip == null || expenses == null) return;
        Map<String, Long> balances = ExpensesViewModel.balances(trip, expenses);

        binding.balances.removeAllViews();
        int positive = ContextCompat.getColor(this, R.color.ready_green);
        int negative = MaterialColors.getColor(binding.getRoot(), androidx.appcompat.R.attr.colorError);
        for (Map.Entry<String, Long> entry : balances.entrySet()) {
            ItemBalanceBinding row = ItemBalanceBinding.inflate(getLayoutInflater(), binding.balances, false);
            Avatars.load(row.avatar, trip.photoOf(entry.getKey()));
            row.name.setText(nameOf(entry.getKey()));
            long cents = entry.getValue();
            row.amount.setText(cents > 0 ? "+" + Money.formatCents(cents) : Money.formatCents(cents));
            if (cents > 0) row.amount.setTextColor(positive);
            else if (cents < 0) row.amount.setTextColor(negative);
            binding.balances.addView(row.getRoot());
        }

        List<SettleUp.Payment> payments = SettleUp.payments(balances);
        binding.payments.removeAllViews();
        binding.allSettled.setVisibility(payments.isEmpty() ? View.VISIBLE : View.GONE);
        for (SettleUp.Payment p : payments) {
            ItemBalanceBinding row = ItemBalanceBinding.inflate(getLayoutInflater(), binding.payments, false);
            Avatars.load(row.avatar, trip.photoOf(p.from));
            row.name.setText(getString(R.string.payment_line, firstName(nameOf(p.from)), firstName(nameOf(p.to))));
            row.amount.setText(Money.formatCents(p.amountCents));
            binding.payments.addView(row.getRoot());
        }
    }

    private static String firstName(String name) {
        int space = name.indexOf(' ');
        return space > 0 ? name.substring(0, space) : name;
    }
}
