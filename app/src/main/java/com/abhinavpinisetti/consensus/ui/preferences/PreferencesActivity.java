package com.abhinavpinisetti.consensus.ui.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.model.Preferences;
import com.abhinavpinisetti.consensus.databinding.ActivityPreferencesBinding;
import com.abhinavpinisetti.consensus.ui.common.BaseActivity;
import com.abhinavpinisetti.consensus.ui.common.TextWatchers;
import com.abhinavpinisetti.consensus.util.Result;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/** The preference survey: budget, interests, pace, must-dos and things to avoid. */
public class PreferencesActivity extends BaseActivity {

    private static final String EXTRA_TRIP_ID = "tripId";

    private ActivityPreferencesBinding binding;
    private PreferencesViewModel viewModel;

    public static Intent intent(Context context, String tripId) {
        return new Intent(context, PreferencesActivity.class).putExtra(EXTRA_TRIP_ID, tripId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (requireUid() == null) return;
        String tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);

        binding = ActivityPreferencesBinding.inflate(getLayoutInflater());
        setContentViewWithInsets(binding.getRoot());
        setupToolbar(binding.toolbar, true);

        for (String interest : Preferences.INTERESTS) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filter, binding.interests, false);
            chip.setText(interest);
            chip.setTag(interest);
            chip.setId(View.generateViewId());
            binding.interests.addView(chip);
        }
        binding.interests.setOnCheckedStateChangeListener((group, ids) -> binding.interestsError.setVisibility(View.GONE));
        binding.budgetGroup.addOnButtonCheckedListener((g, id, checked) -> binding.budgetError.setVisibility(View.GONE));
        binding.paceGroup.addOnButtonCheckedListener((g, id, checked) -> binding.paceError.setVisibility(View.GONE));

        viewModel = new ViewModelProvider(this).get(PreferencesViewModel.class);
        viewModel.init(tripId);
        viewModel.existing().observe(this, this::onExisting);
        binding.retry.setOnClickListener(v -> viewModel.load());
        binding.save.setOnClickListener(v -> save());

        viewModel.saveResult().observe(this, event -> {
            Result<Void> result = event.peek();
            binding.progress.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            binding.save.setEnabled(!result.isLoading());
            Result<Void> fresh = event.consume();
            if (fresh == null) return;
            if (fresh.isSuccess()) {
                Toast.makeText(this, R.string.preferences_saved, Toast.LENGTH_SHORT).show();
                finish();
            } else if (fresh.isError()) {
                Toast.makeText(this, fresh.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onExisting(Result<Preferences> result) {
        binding.progress.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
        binding.loadError.setVisibility(result.isError() ? View.VISIBLE : View.GONE);
        binding.form.setVisibility(result.isSuccess() ? View.VISIBLE : View.GONE);
        if (result.isError()) binding.loadErrorText.setText(result.message);
        if (!result.isSuccess() || viewModel.prefilled) return;
        viewModel.prefilled = true;

        Preferences p = result.data;
        if (p == null) return;
        int budget = Preferences.BUDGETS.indexOf(p.budget);
        if (budget >= 0) binding.budgetGroup.check(budgetIds()[budget]);
        int pace = Preferences.PACES.indexOf(p.pace);
        if (pace >= 0) binding.paceGroup.check(paceIds()[pace]);
        for (int i = 0; i < binding.interests.getChildCount(); i++) {
            Chip chip = (Chip) binding.interests.getChildAt(i);
            chip.setChecked(p.interests != null && p.interests.contains((String) chip.getTag()));
        }
        binding.mustDos.setText(p.mustDos);
        binding.avoid.setText(p.avoid);
    }

    private int[] budgetIds() {
        return new int[]{R.id.budget_low, R.id.budget_medium, R.id.budget_high};
    }

    private int[] paceIds() {
        return new int[]{R.id.pace_relaxed, R.id.pace_balanced, R.id.pace_packed};
    }

    private static int indexOf(int[] ids, int id) {
        for (int i = 0; i < ids.length; i++) if (ids[i] == id) return i;
        return -1;
    }

    private void save() {
        int budget = indexOf(budgetIds(), binding.budgetGroup.getCheckedButtonId());
        int pace = indexOf(paceIds(), binding.paceGroup.getCheckedButtonId());
        List<String> interests = new ArrayList<>();
        for (int i = 0; i < binding.interests.getChildCount(); i++) {
            Chip chip = (Chip) binding.interests.getChildAt(i);
            if (chip.isChecked()) interests.add((String) chip.getTag());
        }

        boolean valid = true;
        if (budget < 0) {
            binding.budgetError.setVisibility(View.VISIBLE);
            valid = false;
        }
        if (interests.isEmpty()) {
            binding.interestsError.setVisibility(View.VISIBLE);
            valid = false;
        }
        if (pace < 0) {
            binding.paceError.setVisibility(View.VISIBLE);
            valid = false;
        }
        if (!valid) return;

        Preferences prefs = new Preferences();
        prefs.budget = Preferences.BUDGETS.get(budget);
        prefs.pace = Preferences.PACES.get(pace);
        prefs.interests = interests;
        prefs.mustDos = TextWatchers.text(binding.mustDosLayout);
        prefs.avoid = TextWatchers.text(binding.avoidLayout);
        viewModel.save(prefs);
    }
}
