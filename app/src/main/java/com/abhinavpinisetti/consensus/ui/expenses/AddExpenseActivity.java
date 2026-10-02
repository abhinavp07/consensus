package com.abhinavpinisetti.consensus.ui.expenses;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.databinding.ActivityAddExpenseBinding;
import com.abhinavpinisetti.consensus.ui.common.BaseActivity;
import com.abhinavpinisetti.consensus.ui.common.TextWatchers;
import com.abhinavpinisetti.consensus.util.Money;
import com.abhinavpinisetti.consensus.util.Result;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/** Description, amount, who paid (defaults to me) and who it's split among (defaults to everyone). */
public class AddExpenseActivity extends BaseActivity {

    private static final String EXTRA_TRIP_ID = "tripId";
    private static final String STATE_PAYER = "payer";

    private ActivityAddExpenseBinding binding;
    private ExpensesViewModel viewModel;
    private Trip trip;
    private String payerId;
    private boolean membersBuilt;

    public static Intent intent(Context context, String tripId) {
        return new Intent(context, AddExpenseActivity.class).putExtra(EXTRA_TRIP_ID, tripId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String uid = requireUid();
        if (uid == null) return;
        payerId = savedInstanceState != null ? savedInstanceState.getString(STATE_PAYER, uid) : uid;

        binding = ActivityAddExpenseBinding.inflate(getLayoutInflater());
        setContentViewWithInsets(binding.getRoot());
        setupToolbar(binding.toolbar, true);
        TextWatchers.clearErrorOnEdit(binding.descriptionLayout);
        TextWatchers.clearErrorOnEdit(binding.amountLayout);

        viewModel = new ViewModelProvider(this).get(ExpensesViewModel.class);
        viewModel.init(getIntent().getStringExtra(EXTRA_TRIP_ID));
        viewModel.trip().observe(this, result -> {
            if (!result.isSuccess() || result.data == null) return;
            trip = result.data;
            if (!membersBuilt) buildMemberPickers();
        });

        binding.save.setOnClickListener(v -> save());
        viewModel.writeResult().observe(this, event -> {
            Result<Void> result = event.peek();
            binding.progress.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            binding.save.setEnabled(!result.isLoading());
            Result<Void> fresh = event.consume();
            if (fresh == null) return;
            if (fresh.isSuccess()) finish();
            else if (fresh.isError()) Toast.makeText(this, fresh.message, Toast.LENGTH_LONG).show();
        });
    }

    private void buildMemberPickers() {
        membersBuilt = true;
        List<String> names = new ArrayList<>();
        for (String id : trip.memberIds) names.add(trip.nameOf(id));
        binding.paidBy.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));
        binding.paidBy.setText(trip.nameOf(payerId), false);
        binding.paidBy.setOnItemClickListener((parent, view, position, id) -> payerId = trip.memberIds.get(position));

        for (String id : trip.memberIds) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filter, binding.splitAmong, false);
            chip.setText(trip.nameOf(id));
            chip.setTag(id);
            chip.setId(View.generateViewId());
            chip.setChecked(true);
            binding.splitAmong.addView(chip);
        }
        binding.splitAmong.setOnCheckedStateChangeListener((g, ids) -> binding.splitError.setVisibility(View.GONE));
    }

    private void save() {
        if (trip == null) return;
        String description = TextWatchers.text(binding.descriptionLayout);
        long cents = Money.parseToCents(TextWatchers.text(binding.amountLayout));
        List<String> split = new ArrayList<>();
        for (int i = 0; i < binding.splitAmong.getChildCount(); i++) {
            Chip chip = (Chip) binding.splitAmong.getChildAt(i);
            if (chip.isChecked()) split.add((String) chip.getTag());
        }

        boolean valid = true;
        if (description.isEmpty()) {
            binding.descriptionLayout.setError(getString(R.string.required));
            valid = false;
        }
        if (cents <= 0) {
            binding.amountLayout.setError(getString(R.string.invalid_cost));
            valid = false;
        }
        if (split.isEmpty()) {
            binding.splitError.setVisibility(View.VISIBLE);
            valid = false;
        }
        if (valid) viewModel.add(description, cents, payerId, split);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_PAYER, payerId);
    }
}
