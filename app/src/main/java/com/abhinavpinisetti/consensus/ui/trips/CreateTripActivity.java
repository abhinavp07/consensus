package com.abhinavpinisetti.consensus.ui.trips;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.repo.PlacesRepository;
import com.abhinavpinisetti.consensus.databinding.ActivityCreateTripBinding;
import com.abhinavpinisetti.consensus.ui.common.BaseActivity;
import com.abhinavpinisetti.consensus.ui.common.TextWatchers;
import com.abhinavpinisetti.consensus.ui.overview.TripOverviewActivity;
import com.abhinavpinisetti.consensus.util.DateUtils;
import com.abhinavpinisetti.consensus.util.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceTypes;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

public class CreateTripActivity extends BaseActivity {

    private static final String TAG = "CreateTripActivity";

    private ActivityCreateTripBinding binding;
    private CreateTripViewModel viewModel;

    private final ActivityResultLauncher<Intent> placePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::onPlacePicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (requireUid() == null) return;

        binding = ActivityCreateTripBinding.inflate(getLayoutInflater());
        setContentViewWithInsets(binding.getRoot());
        setupToolbar(binding.toolbar, true);
        viewModel = new ViewModelProvider(this).get(CreateTripViewModel.class);

        TextWatchers.clearErrorOnEdit(binding.nameLayout);
        TextWatchers.clearErrorOnEdit(binding.destinationLayout);

        if (PlacesRepository.get().isAvailable()) {
            // Destination must come from Places autocomplete so it's a real place.
            binding.destination.setFocusable(false);
            binding.destination.setOnClickListener(v -> openPlacePicker());
        }

        binding.startDate.setOnClickListener(v -> pickDate(true));
        binding.endDate.setOnClickListener(v -> pickDate(false));
        viewModel.startDate.observe(this, d -> {
            binding.startDate.setText(d == null ? "" : DateUtils.formatShort(d));
            binding.startDateLayout.setError(null);
        });
        viewModel.endDate.observe(this, d -> {
            binding.endDate.setText(d == null ? "" : DateUtils.formatShort(d));
            binding.endDateLayout.setError(null);
        });

        binding.save.setOnClickListener(v -> save());
        viewModel.saveResult().observe(this, event -> {
            Result<String> result = event.peek();
            binding.progress.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            binding.save.setEnabled(!result.isLoading());
            Result<String> fresh = event.consume();
            if (fresh == null) return;
            if (fresh.isSuccess()) {
                startActivity(TripOverviewActivity.intent(this, fresh.data));
                finish();
            } else if (fresh.isError()) {
                Toast.makeText(this, fresh.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openPlacePicker() {
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY,
                Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION))
                .setTypesFilter(Collections.singletonList(PlaceTypes.REGIONS))
                .build(this);
        placePicker.launch(intent);
    }

    private void onPlacePicked(ActivityResult result) {
        Intent data = result.getData();
        if (result.getResultCode() == Activity.RESULT_OK && data != null) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            String name = place.getDisplayName();
            viewModel.setPlace(name, place.getId(),
                    place.getLocation() == null ? null : place.getLocation().latitude,
                    place.getLocation() == null ? null : place.getLocation().longitude);
            binding.destination.setText(name);
        } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR && data != null) {
            Status status = Autocomplete.getStatusFromIntent(data);
            Log.e(TAG, "Places autocomplete error: " + status.getStatusMessage());
            // Fall back to typing the destination so the user isn't stuck.
            Toast.makeText(this, R.string.places_error, Toast.LENGTH_LONG).show();
            binding.destination.setFocusableInTouchMode(true);
            binding.destination.setOnClickListener(null);
            binding.destination.requestFocus();
        }
    }

    private void pickDate(boolean isStart) {
        LocalDate current = isStart ? viewModel.startDate.getValue() : viewModel.endDate.getValue();
        LocalDate start = viewModel.startDate.getValue();
        CalendarConstraints.Builder constraints = new CalendarConstraints.Builder();
        if (!isStart && start != null) {
            constraints.setValidator(DateValidatorPointForward.from(DateUtils.toUtcMillis(start)));
            constraints.setOpenAt(DateUtils.toUtcMillis(start));
        } else {
            constraints.setValidator(DateValidatorPointForward.now());
        }

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isStart ? R.string.start_date : R.string.end_date)
                .setSelection(current != null ? DateUtils.toUtcMillis(current)
                        : (!isStart && start != null ? DateUtils.toUtcMillis(start) : MaterialDatePicker.todayInUtcMilliseconds()))
                .setCalendarConstraints(constraints.build())
                .build();
        picker.addOnPositiveButtonClickListener(millis -> {
            LocalDate picked = DateUtils.fromUtcMillis(millis);
            if (isStart) {
                viewModel.startDate.setValue(picked);
                LocalDate end = viewModel.endDate.getValue();
                if (end != null && end.isBefore(picked)) viewModel.endDate.setValue(picked);
            } else {
                viewModel.endDate.setValue(picked);
            }
        });
        picker.show(getSupportFragmentManager(), isStart ? "start" : "end");
    }

    private void save() {
        String name = TextWatchers.text(binding.nameLayout);
        String destination = TextWatchers.text(binding.destinationLayout);
        LocalDate start = viewModel.startDate.getValue();
        LocalDate end = viewModel.endDate.getValue();

        boolean valid = true;
        if (name.isEmpty()) {
            binding.nameLayout.setError(getString(R.string.required));
            valid = false;
        }
        if (destination.isEmpty()) {
            binding.destinationLayout.setError(getString(R.string.required));
            valid = false;
        }
        if (start == null) {
            binding.startDateLayout.setError(getString(R.string.required));
            valid = false;
        }
        if (end == null) {
            binding.endDateLayout.setError(getString(R.string.required));
            valid = false;
        }
        if (start != null && end != null) {
            if (end.isBefore(start)) {
                binding.endDateLayout.setError(getString(R.string.end_before_start));
                valid = false;
            } else if (DateUtils.tripLength(DateUtils.toIso(start), DateUtils.toIso(end)) > DateUtils.MAX_TRIP_DAYS) {
                binding.endDateLayout.setError(getString(R.string.trip_too_long, DateUtils.MAX_TRIP_DAYS));
                valid = false;
            }
        }
        if (valid) viewModel.save(name, destination);
    }
}
