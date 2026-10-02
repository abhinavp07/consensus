package com.abhinavpinisetti.consensus.ui.itinerary;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.model.Day;
import com.abhinavpinisetti.consensus.databinding.ActivityItineraryBinding;
import com.abhinavpinisetti.consensus.ui.common.BaseActivity;
import com.abhinavpinisetti.consensus.util.DateUtils;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

/** One tab per trip day ("Day 1 · Fri Oct 9"), each showing that day's activities. */
public class ItineraryActivity extends BaseActivity {

    private static final String EXTRA_TRIP_ID = "tripId";

    private ActivityItineraryBinding binding;
    private DayPagerAdapter adapter;

    public static Intent intent(Context context, String tripId) {
        return new Intent(context, ItineraryActivity.class).putExtra(EXTRA_TRIP_ID, tripId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (requireUid() == null) return;
        String tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);

        binding = ActivityItineraryBinding.inflate(getLayoutInflater());
        setContentViewWithInsets(binding.getRoot());
        setupToolbar(binding.toolbar, true);

        ItineraryViewModel viewModel = new ViewModelProvider(this).get(ItineraryViewModel.class);
        viewModel.init(tripId);

        adapter = new DayPagerAdapter(this);
        binding.pager.setAdapter(adapter);
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> {
            Day day = adapter.dayAt(position);
            tab.setText(DateUtils.dayTabLabel(day.dayNumber, day.date));
        }).attach();

        binding.fab.setOnClickListener(v -> {
            Day day = adapter.dayAt(binding.pager.getCurrentItem());
            if (day != null) startActivity(ActivityEditActivity.intent(this, tripId, day.id, null));
        });

        viewModel.trip().observe(this, result -> {
            if (result.isSuccess() && result.data != null) {
                binding.toolbar.setSubtitle(result.data.name);
            } else if (result.isSuccess() || result.isError()) {
                Toast.makeText(this, R.string.trip_deleted, Toast.LENGTH_LONG).show();
                finish();
            }
        });
        viewModel.days().observe(this, result -> {
            binding.progress.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            binding.error.setVisibility(result.isError() ? View.VISIBLE : View.GONE);
            if (result.isError()) binding.error.setText(result.message);
            if (!result.isSuccess()) return;
            List<Day> days = result.data;
            adapter.setDays(days);
            binding.empty.setVisibility(days.isEmpty() ? View.VISIBLE : View.GONE);
            binding.fab.setVisibility(days.isEmpty() ? View.GONE : View.VISIBLE);
        });
        viewModel.messages().observe(this, event -> {
            String message = event.consume();
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }
}
