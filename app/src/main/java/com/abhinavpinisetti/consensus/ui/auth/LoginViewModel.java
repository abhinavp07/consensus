package com.abhinavpinisetti.consensus.ui.auth;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.abhinavpinisetti.consensus.data.repo.AuthRepository;
import com.abhinavpinisetti.consensus.data.repo.UserRepository;
import com.google.android.gms.tasks.Tasks;

public class LoginViewModel extends ViewModel {

    public enum State { IDLE, LOADING, SIGNED_IN, CANCELLED, NO_ACCOUNT, FAILED }

    private static final String TAG = "LoginViewModel";
    private final MutableLiveData<State> state = new MutableLiveData<>(State.IDLE);

    public LiveData<State> state() {
        return state;
    }

    public void setState(State s) {
        state.setValue(s);
    }

    /** Signs in to Firebase and creates/updates users/{uid}. */
    public void signIn(String idToken) {
        state.setValue(State.LOADING);
        AuthRepository.get().signInWithGoogle(idToken)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) return Tasks.forException(task.getException());
                    return UserRepository.get().saveProfile(task.getResult());
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        state.setValue(State.SIGNED_IN);
                    } else {
                        Log.e(TAG, "Sign-in failed", task.getException());
                        AuthRepository.get().signOutFirebaseOnly();
                        state.setValue(State.FAILED);
                    }
                });
    }
}
