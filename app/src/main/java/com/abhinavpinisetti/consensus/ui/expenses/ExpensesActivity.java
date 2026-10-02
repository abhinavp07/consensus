package com.abhinavpinisetti.consensus.ui.expenses;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.model.Expense;
import com.abhinavpinisetti.consensus.databinding.ActivityExpensesBinding;
import com.abhinavpinisetti.consensus.ui.common.BaseActivity;
import com.abhinavpinisetti.consensus.util.Money;
import com.abhinavpinisetti.consensus.util.Result;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** Expenses newest first with a running total. Long-press to delete. */
public class ExpensesActivity extends BaseActivity {

    private static final String EXTRA_TRIP_ID = "tripId";

    private ActivityExpensesBinding binding;
    private ExpensesViewModel viewModel;
    private String tripId;

    public static Intent intent(Context context, String tripId) {
        return new Intent(context, ExpensesActivity.class).putExtra(EXTRA_TRIP_ID, tripId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (requireUid() == null) return;
        tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);

        binding = ActivityExpensesBinding.inflate(getLayoutInflater());
        setContentViewWithInsets(binding.getRoot());
        setupToolbar(binding.toolbar, true);

        ExpenseAdapter adapter = new ExpenseAdapter(this::confirmDelete);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        binding.fab.setOnClickListener(v -> startActivity(AddExpenseActivity.intent(this, tripId)));

        viewModel = new ViewModelProvider(this).get(ExpensesViewModel.class);
        viewModel.init(tripId);
        viewModel.trip().observe(this, result -> {
            if (result.isSuccess() && result.data != null) adapter.setTrip(result.data);
        });
        viewModel.expenses().observe(this, (Result<List<Expense>> result) -> {
            binding.progress.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            binding.error.setVisibility(result.isError() ? View.VISIBLE : View.GONE);
            if (result.isError()) binding.error.setText(result.message);
            if (!result.isSuccess()) return;
            adapter.submitList(result.data);
            binding.total.setText(Money.formatCents(ExpensesViewModel.total(result.data)));
            binding.empty.setVisibility(result.data.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.writeResult().observe(this, event -> {
            Result<Void> r = event.consume();
            if (r != null && r.isError()) Toast.makeText(this, r.message, Toast.LENGTH_LONG).show();
        });
    }

    private void confirmDelete(Expense expense) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_expense_title)
                .setMessage(expense.description + " · " + Money.formatCents(expense.amountCents))
                .setPositiveButton(R.string.delete, (d, w) -> viewModel.delete(expense))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_expenses, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settle_up) {
            startActivity(SettleUpActivity.intent(this, tripId));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
