package com.abhinavpinisetti.consensus.ui.trips;

import android.app.Dialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.databinding.DialogJoinTripBinding;
import com.abhinavpinisetti.consensus.ui.common.TextWatchers;
import com.abhinavpinisetti.consensus.ui.overview.TripOverviewActivity;
import com.abhinavpinisetti.consensus.util.Result;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Accepts an invite code (case-insensitive) and joins the trip. */
public class JoinTripDialogFragment extends DialogFragment {

    private DialogJoinTripBinding binding;
    private TripsViewModel viewModel;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(TripsViewModel.class);
        binding = DialogJoinTripBinding.inflate(getLayoutInflater());
        binding.code.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(8)});
        TextWatchers.clearErrorOnEdit(binding.codeLayout);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.join_trip)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.join, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        // Override the click so the dialog stays open while joining or on error.
        dialog.setOnShowListener(d -> {
            Button join = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            join.setOnClickListener(v -> {
                String code = TextWatchers.text(binding.codeLayout);
                if (code.isEmpty()) {
                    binding.codeLayout.setError(getString(R.string.required));
                } else {
                    viewModel.join(code);
                }
            });
        });

        viewModel.joinResult().observe(this, event -> {
            Result<String> result = event.peek();
            boolean loading = result.isLoading();
            binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            Button join = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (join != null) join.setEnabled(!loading);

            Result<String> fresh = event.consume();
            if (fresh == null) return;
            if (fresh.isSuccess()) {
                startActivity(TripOverviewActivity.intent(requireContext(), fresh.data));
                dismiss();
            } else if (fresh.isError()) {
                binding.codeLayout.setError(fresh.message);
            }
        });
        return dialog;
    }
}
