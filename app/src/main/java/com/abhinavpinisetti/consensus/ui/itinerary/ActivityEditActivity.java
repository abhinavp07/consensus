package com.abhinavpinisetti.consensus.ui.itinerary;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.model.PlannedActivity;
import com.abhinavpinisetti.consensus.databinding.ActivityActivityEditBinding;
import com.abhinavpinisetti.consensus.ui.common.BaseActivity;
import com.abhinavpinisetti.consensus.ui.common.TextWatchers;
import com.abhinavpinisetti.consensus.util.Result;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Locale;

/** Adds a new activity by hand, or edits/deletes an existing one. */
public class ActivityEditActivity extends BaseActivity {

    private static final String EXTRA_TRIP_ID = "tripId";
    private static final String EXTRA_DAY_ID = "dayId";
    private static final String EXTRA_ACTIVITY_ID = "activityId";

    private ActivityActivityEditBinding binding;
    private ActivityEditViewModel viewModel;

    public static Intent intent(Context context, String tripId, String dayId, @Nullable String activityId) {
        return new Intent(context, ActivityEditActivity.class)
                .putExtra(EXTRA_TRIP_ID, tripId)
                .putExtra(EXTRA_DAY_ID, dayId)
                .putExtra(EXTRA_ACTIVITY_ID, activityId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (requireUid() == null) return;

        binding = ActivityActivityEditBinding.inflate(getLayoutInflater());
        setContentViewWithInsets(binding.getRoot());
        setupToolbar(binding.toolbar, true);

        viewModel = new ViewModelProvider(this).get(ActivityEditViewModel.class);
        viewModel.init(getIntent().getStringExtra(EXTRA_TRIP_ID), getIntent().getStringExtra(EXTRA_DAY_ID),
                getIntent().getStringExtra(EXTRA_ACTIVITY_ID));
        binding.toolbar.setTitle(viewModel.isEditing() ? R.string.edit_activity : R.string.add_activity);

        TextWatchers.clearErrorOnEdit(binding.titleLayout);
        TextWatchers.clearErrorOnEdit(binding.costLayout);
        binding.time.setOnClickListener(v -> pickTime());
        viewModel.time.observe(this, t -> {
            binding.time.setText(t);
            binding.timeLayout.setError(null);
        });
        binding.save.setOnClickListener(v -> save());

        viewModel.existing().observe(this, this::onExisting);
        viewModel.done().observe(this, event -> {
            Result<Void> result = event.peek();
            binding.progress.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            binding.save.setEnabled(!result.isLoading());
            Result<Void> fresh = event.consume();
            if (fresh == null) return;
            if (fresh.isSuccess()) finish();
            else if (fresh.isError()) Toast.makeText(this, fresh.message, Toast.LENGTH_LONG).show();
        });
    }

    private void onExisting(Result<PlannedActivity> result) {
        binding.progress.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
        binding.form.setVisibility(result.isSuccess() ? View.VISIBLE : View.INVISIBLE);
        if (result.isError()) {
            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!result.isSuccess() || viewModel.prefilled) return;
        viewModel.prefilled = true;
        PlannedActivity a = result.data;
        if (a == null) return;
        binding.title.setText(a.title);
        binding.description.setText(a.description);
        binding.cost.setText(a.estimatedCost == 0 ? "" : formatCost(a.estimatedCost));
        binding.placeName.setText(a.placeName);
        viewModel.time.setValue(a.time);
    }

    private static String formatCost(double cost) {
        return cost == Math.rint(cost) ? String.format(Locale.US, "%.0f", cost) : String.format(Locale.US, "%.2f", cost);
    }

    private void pickTime() {
        int hour = 9;
        int minute = 0;
        String current = viewModel.time.getValue();
        if (current != null && current.length() == 5) {
            hour = Integer.parseInt(current.substring(0, 2));
            minute = Integer.parseInt(current.substring(3));
        }
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText(R.string.time)
                .build();
        picker.addOnPositiveButtonClickListener(v -> viewModel.time.setValue(
                String.format(Locale.US, "%02d:%02d", picker.getHour(), picker.getMinute())));
        picker.show(getSupportFragmentManager(), "time");
    }

    private void save() {
        String title = TextWatchers.text(binding.titleLayout);
        String costText = TextWatchers.text(binding.costLayout).replace("$", "");
        boolean valid = true;
        if (title.isEmpty()) {
            binding.titleLayout.setError(getString(R.string.required));
            valid = false;
        }
        if (viewModel.time.getValue() == null) {
            binding.timeLayout.setError(getString(R.string.required));
            valid = false;
        }
        double cost = 0;
        if (!costText.isEmpty()) {
            try {
                cost = Double.parseDouble(costText);
                if (cost < 0 || Double.isNaN(cost) || Double.isInfinite(cost)) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                binding.costLayout.setError(getString(R.string.invalid_cost));
                valid = false;
            }
        }
        if (!valid) return;
        viewModel.save(title, TextWatchers.text(binding.descriptionLayout), cost, TextWatchers.text(binding.placeNameLayout));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (viewModel != null && viewModel.isEditing()) {
            getMenuInflater().inflate(R.menu.menu_delete, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete_activity_title)
                    .setMessage(R.string.delete_activity_message)
                    .setPositiveButton(R.string.delete, (d, w) -> viewModel.delete())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
