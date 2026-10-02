package com.abhinavpinisetti.consensus.ui.preferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.abhinavpinisetti.consensus.data.ErrorMessages;
import com.abhinavpinisetti.consensus.data.model.Preferences;
import com.abhinavpinisetti.consensus.data.repo.AuthRepository;
import com.abhinavpinisetti.consensus.data.repo.PreferencesRepository;
import com.abhinavpinisetti.consensus.util.Event;
import com.abhinavpinisetti.consensus.util.Result;

public class PreferencesViewModel extends ViewModel {

    private String tripId;
    /** Saved answers to pre-fill (data is null when the user hasn't answered yet). */
    private final MutableLiveData<Result<Preferences>> existing = new MutableLiveData<>();
    private final MutableLiveData<Event<Result<Void>>> saveResult = new MutableLiveData<>();
    public boolean prefilled;

    public void init(String tripId) {
        if (this.tripId != null) return;
        this.tripId = tripId;
        load();
    }

    public void load() {
        existing.setValue(Result.loading());
        PreferencesRepository.get().fetchMine(tripId, AuthRepository.get().uid())
                .addOnCompleteListener(task -> existing.setValue(task.isSuccessful()
                        ? Result.success(task.getResult())
                        : Result.error(ErrorMessages.from(task.getException()))));
    }

    public LiveData<Result<Preferences>> existing() {
        return existing;
    }

    public LiveData<Event<Result<Void>>> saveResult() {
        return saveResult;
    }

    public void save(Preferences prefs) {
        saveResult.setValue(new Event<>(Result.loading()));
        PreferencesRepository.get().save(tripId, AuthRepository.get().uid(), prefs)
                .addOnCompleteListener(task -> saveResult.setValue(new Event<>(task.isSuccessful()
                        ? Result.success(null)
                        : Result.error(ErrorMessages.from(task.getException())))));
    }
}
