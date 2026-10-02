package com.abhinavpinisetti.consensus.ui.itinerary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.ai.GeneratedActivity;
import com.abhinavpinisetti.consensus.databinding.ItemAlternativeBinding;
import com.abhinavpinisetti.consensus.databinding.SheetAlternativesBinding;
import com.abhinavpinisetti.consensus.util.Money;
import com.abhinavpinisetti.consensus.util.Result;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

/** Shows three AI replacement options. Picking one swaps the activity; cancelling changes nothing. */
public class AlternativesBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TRIP_ID = "tripId";
    private static final String ARG_DAY_ID = "dayId";
    private static final String ARG_DATE = "date";
    private static final String ARG_ACTIVITY_ID = "activityId";

    private SheetAlternativesBinding binding;
    private AlternativesViewModel viewModel;

    public static AlternativesBottomSheet newInstance(String tripId, String dayId, String date, String activityId) {
        Bundle args = new Bundle();
        args.putString(ARG_TRIP_ID, tripId);
        args.putString(ARG_DAY_ID, dayId);
        args.putString(ARG_DATE, date);
        args.putString(ARG_ACTIVITY_ID, activityId);
        AlternativesBottomSheet sheet = new AlternativesBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetAlternativesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        viewModel = new ViewModelProvider(this).get(AlternativesViewModel.class);
        viewModel.init(args.getString(ARG_TRIP_ID), args.getString(ARG_DAY_ID),
                args.getString(ARG_DATE), args.getString(ARG_ACTIVITY_ID));

        binding.cancel.setOnClickListener(v -> dismiss());
        binding.retry.setOnClickListener(v -> viewModel.load());
        viewModel.options().observe(getViewLifecycleOwner(), this::render);
        viewModel.picked().observe(getViewLifecycleOwner(), event -> {
            Result<Void> result = event.peek();
            binding.options.setEnabled(!result.isLoading());
            binding.savingProgress.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            Result<Void> fresh = event.consume();
            if (fresh == null) return;
            if (fresh.isSuccess()) {
                Toast.makeText(requireContext(), R.string.alternative_replaced, Toast.LENGTH_SHORT).show();
                dismiss();
            } else if (fresh.isError()) {
                Toast.makeText(requireContext(), fresh.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void render(Result<List<GeneratedActivity>> result) {
        binding.loading.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(result.isError() ? View.VISIBLE : View.GONE);
        if (result.isError()) binding.error.setText(result.message);
        binding.options.removeAllViews();
        if (!result.isSuccess()) return;

        for (GeneratedActivity option : result.data) {
            ItemAlternativeBinding item = ItemAlternativeBinding.inflate(getLayoutInflater(), binding.options, false);
            item.time.setText(option.time);
            item.title.setText(option.title);
            item.description.setText(option.description);
            item.cost.setText(Money.formatEstimate(option.estimatedCost));
            item.place.setText(option.placeName);
            item.getRoot().setOnClickListener(v -> viewModel.pick(option));
            binding.options.addView(item.getRoot());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
