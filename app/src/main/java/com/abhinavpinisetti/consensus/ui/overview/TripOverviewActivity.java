package com.abhinavpinisetti.consensus.ui.overview;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.model.Preferences;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.databinding.ActivityTripOverviewBinding;
import com.abhinavpinisetti.consensus.ui.chat.ChatActivity;
import com.abhinavpinisetti.consensus.ui.common.BaseActivity;
import com.abhinavpinisetti.consensus.ui.expenses.ExpensesActivity;
import com.abhinavpinisetti.consensus.ui.itinerary.ItineraryActivity;
import com.abhinavpinisetti.consensus.ui.preferences.PreferencesActivity;
import com.abhinavpinisetti.consensus.util.DateUtils;
import com.abhinavpinisetti.consensus.util.Result;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Destination, dates, members and who's ready, plus entry points to every trip feature. */
public class TripOverviewActivity extends BaseActivity {

    private static final String EXTRA_TRIP_ID = "tripId";

    private ActivityTripOverviewBinding binding;
    private TripOverviewViewModel viewModel;
    private final MemberAdapter memberAdapter = new MemberAdapter();
    private String uid;
    private String tripId;
    private Trip trip;
    private Set<String> readyIds = new HashSet<>();
    private AlertDialog errorDialog;
    /** True once the user chose to leave/delete, so the resulting "trip gone" update is expected. */
    private boolean leaving;

    public static Intent intent(Context context, String tripId) {
        return new Intent(context, TripOverviewActivity.class).putExtra(EXTRA_TRIP_ID, tripId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uid = requireUid();
        tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        if (uid == null) return;
        if (tripId == null) {
            finish();
            return;
        }

        binding = ActivityTripOverviewBinding.inflate(getLayoutInflater());
        setContentViewWithInsets(binding.getRoot());
        setupToolbar(binding.toolbar, true);

        binding.members.setLayoutManager(new LinearLayoutManager(this));
        binding.members.setAdapter(memberAdapter);
        binding.members.setNestedScrollingEnabled(false);

        binding.copyCode.setOnClickListener(v -> copyCode());
        binding.shareCode.setOnClickListener(v -> shareCode());
        binding.preferencesButton.setOnClickListener(v -> startActivity(PreferencesActivity.intent(this, tripId)));
        binding.itineraryButton.setOnClickListener(v -> startActivity(ItineraryActivity.intent(this, tripId)));
        binding.expensesButton.setOnClickListener(v -> startActivity(ExpensesActivity.intent(this, tripId)));
        binding.chatButton.setOnClickListener(v -> startActivity(ChatActivity.intent(this, tripId)));
        binding.generateButton.setOnClickListener(v -> onGenerateClicked());

        viewModel = new ViewModelProvider(this).get(TripOverviewViewModel.class);
        viewModel.init(tripId);
        viewModel.trip().observe(this, this::onTrip);
        viewModel.preferences().observe(this, this::onPreferences);
        viewModel.generation().observe(this, this::renderGeneration);
        viewModel.generationEvents().observe(this, event -> {
            if (event.consume() == null) return;
            Snackbar.make(binding.getRoot(), R.string.itinerary_ready, Snackbar.LENGTH_LONG)
                    .setAction(R.string.itinerary, v -> startActivity(ItineraryActivity.intent(this, tripId)))
                    .show();
        });
        viewModel.membershipResult().observe(this, event -> {
            Result<Void> result = event.peek();
            binding.progress.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            Result<Void> fresh = event.consume();
            if (fresh == null) return;
            if (fresh.isSuccess()) {
                finish();
            } else if (fresh.isError()) {
                leaving = false;
                Toast.makeText(this, fresh.message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onTrip(Result<Trip> result) {
        binding.progress.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
        if (result.isLoading()) return;
        if (result.isError() || result.data == null || !result.data.memberIds.contains(uid)) {
            // Deleted, or we left / lost access.
            if (!leaving) Toast.makeText(this, R.string.trip_deleted, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        trip = result.data;
        binding.content.setVisibility(View.VISIBLE);
        binding.toolbar.setTitle(trip.name);
        binding.destination.setText(trip.destination);
        binding.dates.setText(DateUtils.formatRange(trip.startDate, trip.endDate));
        binding.status.setText(Trip.STATUS_PLANNED.equals(trip.status) ? R.string.status_planned : R.string.status_collecting);
        binding.inviteCode.setText(trip.inviteCode);
        invalidateOptionsMenu();
        renderMembers();
    }

    private void onPreferences(Result<List<Preferences>> result) {
        if (!result.isSuccess()) return;
        Set<String> ids = new HashSet<>();
        for (Preferences p : result.data) ids.add(p.userId);
        readyIds = ids;
        renderMembers();
    }

    private void renderMembers() {
        if (trip == null) return;
        List<MemberAdapter.Row> rows = new ArrayList<>();
        int ready = 0;
        for (String memberId : trip.memberIds) {
            boolean isReady = readyIds.contains(memberId);
            if (isReady) ready++;
            rows.add(new MemberAdapter.Row(memberId, trip.nameOf(memberId), trip.photoOf(memberId),
                    trip.isOwnedBy(memberId), isReady));
        }
        memberAdapter.submitList(rows);
        binding.readyCount.setText(getString(R.string.ready_count, ready, trip.memberIds.size()));

        boolean owner = trip.isOwnedBy(uid);
        binding.generateCard.setVisibility(owner ? View.VISIBLE : View.GONE);
        binding.generateButton.setText(Trip.STATUS_PLANNED.equals(trip.status)
                ? R.string.regenerate_itinerary : R.string.generate_itinerary);
        binding.generateButton.setEnabled(ready > 0);
        binding.generateHint.setVisibility(ready > 0 ? View.GONE : View.VISIBLE);
    }

    private int readyMemberCount() {
        int ready = 0;
        for (String id : trip.memberIds) if (readyIds.contains(id)) ready++;
        return ready;
    }

    private void onGenerateClicked() {
        if (trip == null) return;
        if (Trip.STATUS_PLANNED.equals(trip.status)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.regenerate_title)
                    .setMessage(R.string.regenerate_message)
                    .setPositiveButton(R.string.regenerate, (d, w) -> confirmMissingAnswers())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            confirmMissingAnswers();
        }
    }

    private void confirmMissingAnswers() {
        int missing = trip.memberIds.size() - readyMemberCount();
        if (missing <= 0) {
            viewModel.generate(trip);
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.generate_anyway_title)
                .setMessage(getResources().getQuantityString(R.plurals.generate_anyway_message, missing, missing))
                .setPositiveButton(R.string.generate_anyway, (d, w) -> viewModel.generate(trip))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void renderGeneration(Result<Void> result) {
        binding.generatingOverlay.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
        if (result.isError() && (errorDialog == null || !errorDialog.isShowing())) {
            errorDialog = new MaterialAlertDialogBuilder(this)
                    .setMessage(result.message)
                    .setPositiveButton(R.string.retry, (d, w) -> {
                        viewModel.dismissGenerationError();
                        if (trip != null) viewModel.generate(trip);
                    })
                    .setNegativeButton(R.string.cancel, (d, w) -> viewModel.dismissGenerationError())
                    .setOnCancelListener(d -> viewModel.dismissGenerationError())
                    .show();
        }
    }

    private void copyCode() {
        if (trip == null) return;
        ClipboardManager clipboard = getSystemService(ClipboardManager.class);
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.invite_code), trip.inviteCode));
        Toast.makeText(this, R.string.code_copied, Toast.LENGTH_SHORT).show();
    }

    private void shareCode() {
        if (trip == null) return;
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message, trip.destination, trip.inviteCode));
        startActivity(Intent.createChooser(send, getString(R.string.share_via)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_trip_overview, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean owner = trip != null && trip.isOwnedBy(uid);
        menu.findItem(R.id.action_delete).setVisible(trip != null && owner);
        menu.findItem(R.id.action_leave).setVisible(trip != null && !owner);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_leave) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.leave_trip)
                    .setMessage(R.string.leave_trip_message)
                    .setPositiveButton(R.string.leave, (d, w) -> {
                        leaving = true;
                        viewModel.leave(uid);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        } else if (item.getItemId() == R.id.action_delete) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete_trip)
                    .setMessage(R.string.delete_trip_message)
                    .setPositiveButton(R.string.delete, (d, w) -> {
                        leaving = true;
                        viewModel.delete(trip);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
