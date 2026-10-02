package com.abhinavpinisetti.consensus.ui.trips;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.data.repo.AuthRepository;
import com.abhinavpinisetti.consensus.data.repo.UserRepository;
import com.abhinavpinisetti.consensus.databinding.ActivityTripsBinding;
import com.abhinavpinisetti.consensus.ui.auth.LoginActivity;
import com.abhinavpinisetti.consensus.ui.common.BaseActivity;
import com.abhinavpinisetti.consensus.ui.overview.TripOverviewActivity;
import com.abhinavpinisetti.consensus.util.Result;

import java.util.List;

/** "My Trips": every trip the user created or joined. */
public class TripsActivity extends BaseActivity {

    private static boolean askedForNotifications;

    private ActivityTripsBinding binding;
    private final TripAdapter adapter = new TripAdapter(this::openTrip);

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String uid = requireUid();
        if (uid == null) return;

        binding = ActivityTripsBinding.inflate(getLayoutInflater());
        setContentViewWithInsets(binding.getRoot());
        setupToolbar(binding.toolbar, false);

        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        binding.fab.setOnClickListener(v -> openCreate());
        binding.emptyCreate.setOnClickListener(v -> openCreate());
        binding.emptyJoin.setOnClickListener(v -> showJoinDialog());

        TripsViewModel viewModel = new ViewModelProvider(this).get(TripsViewModel.class);
        viewModel.trips(uid).observe(this, this::render);

        UserRepository.get().registerFcmToken();
        maybeAskForNotifications();
    }

    private void render(Result<List<Trip>> result) {
        binding.progress.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
        binding.error.setVisibility(result.isError() ? View.VISIBLE : View.GONE);
        if (result.isError()) binding.error.setText(result.message);

        boolean empty = result.isSuccess() && result.data.isEmpty();
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.fab.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (result.isSuccess()) adapter.submitList(result.data);
    }

    private void maybeAskForNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedForNotifications
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            askedForNotifications = true;
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void openCreate() {
        startActivity(new Intent(this, CreateTripActivity.class));
    }

    private void showJoinDialog() {
        new JoinTripDialogFragment().show(getSupportFragmentManager(), "join");
    }

    void openTrip(Trip trip) {
        startActivity(TripOverviewActivity.intent(this, trip.id));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_trips, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_join) {
            showJoinDialog();
            return true;
        } else if (item.getItemId() == R.id.action_sign_out) {
            signOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        binding.progress.setVisibility(View.VISIBLE);
        UserRepository.get().unregisterFcmToken().addOnCompleteListener(task -> {
            AuthRepository.get().signOut(getApplicationContext());
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
