package com.abhinavpinisetti.consensus.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.repo.AuthRepository;
import com.abhinavpinisetti.consensus.databinding.ActivityLoginBinding;
import com.abhinavpinisetti.consensus.ui.common.BaseActivity;
import com.abhinavpinisetti.consensus.ui.trips.TripsActivity;

public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AuthRepository.get().isSignedIn()) {
            openTrips();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentViewWithInsets(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        binding.signInButton.setOnClickListener(v -> startSignIn());
        viewModel.state().observe(this, this::render);
    }

    private void startSignIn() {
        viewModel.setState(LoginViewModel.State.LOADING);
        AuthRepository.get().requestGoogleIdToken(this, new AuthRepository.GoogleTokenCallback() {
            @Override
            public void onToken(String idToken) {
                viewModel.signIn(idToken);
            }

            @Override
            public void onCancelled() {
                viewModel.setState(LoginViewModel.State.CANCELLED);
            }

            @Override
            public void onNoAccount() {
                viewModel.setState(LoginViewModel.State.NO_ACCOUNT);
            }

            @Override
            public void onError(Exception e) {
                viewModel.setState(LoginViewModel.State.FAILED);
            }
        });
    }

    private void render(LoginViewModel.State state) {
        boolean loading = state == LoginViewModel.State.LOADING;
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.signInButton.setEnabled(!loading);

        switch (state) {
            case SIGNED_IN:
                openTrips();
                break;
            case CANCELLED:
                showMessage(R.string.sign_in_cancelled);
                break;
            case NO_ACCOUNT:
                showMessage(R.string.sign_in_no_account);
                break;
            case FAILED:
                showMessage(R.string.sign_in_failed);
                break;
            default:
                binding.message.setVisibility(View.GONE);
        }
    }

    private void showMessage(int resId) {
        binding.message.setText(resId);
        binding.message.setVisibility(View.VISIBLE);
    }

    private void openTrips() {
        startActivity(new Intent(this, TripsActivity.class));
        finish();
    }
}
