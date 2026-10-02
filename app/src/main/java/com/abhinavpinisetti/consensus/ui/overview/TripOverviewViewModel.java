package com.abhinavpinisetti.consensus.ui.overview;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.abhinavpinisetti.consensus.data.ErrorMessages;
import com.abhinavpinisetti.consensus.data.model.Preferences;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.data.repo.AiRepository;
import com.abhinavpinisetti.consensus.data.repo.ItineraryRepository;
import com.abhinavpinisetti.consensus.data.repo.PlacesRepository;
import com.abhinavpinisetti.consensus.data.repo.PreferencesRepository;
import com.abhinavpinisetti.consensus.data.repo.TripRepository;
import com.abhinavpinisetti.consensus.util.Event;
import com.abhinavpinisetti.consensus.util.Result;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;

public class TripOverviewViewModel extends ViewModel {

    private static final String TAG = "TripOverviewViewModel";

    private String tripId;
    private LiveData<Result<Trip>> trip;
    private LiveData<Result<List<Preferences>>> preferences;
    private final MutableLiveData<Result<Void>> generation = new MutableLiveData<>(Result.success(null));
    private final MutableLiveData<Event<String>> generationEvents = new MutableLiveData<>();
    private final MutableLiveData<Event<Result<Void>>> membershipResult = new MutableLiveData<>();

    public void init(String tripId) {
        if (this.tripId != null) return;
        this.tripId = tripId;
        trip = TripRepository.get().trip(tripId);
        preferences = PreferencesRepository.get().preferences(tripId);
    }

    public LiveData<Result<Trip>> trip() {
        return trip;
    }

    public LiveData<Result<List<Preferences>>> preferences() {
        return preferences;
    }

    /** LOADING while Gemini is planning; ERROR carries the message for the retry dialog. */
    public LiveData<Result<Void>> generation() {
        return generation;
    }

    /** One-off "itinerary ready" signal. */
    public LiveData<Event<String>> generationEvents() {
        return generationEvents;
    }

    public LiveData<Event<Result<Void>>> membershipResult() {
        return membershipResult;
    }

    /**
     * Reads everyone's answers, asks Gemini for a plan, saves it in one batch, then matches each
     * activity to a real place. Nothing is saved unless the whole plan is valid.
     */
    public void generate(Trip current) {
        if (generation.getValue() != null && generation.getValue().isLoading()) return;
        generation.setValue(Result.loading());

        PreferencesRepository.get().fetchAll(current.id)
                .continueWithTask(prefs -> {
                    if (!prefs.isSuccessful()) return Tasks.forException(prefs.getException());
                    return AiRepository.get().generatePlan(current, prefs.getResult());
                })
                .continueWithTask(plan -> {
                    if (!plan.isSuccessful()) return Tasks.forException(plan.getException());
                    return ItineraryRepository.get().saveGeneratedPlan(current, plan.getResult());
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        generation.setValue(Result.success(null));
                        generationEvents.setValue(new Event<>(current.id));
                        PlacesRepository.get().matchActivities(current, task.getResult());
                    } else {
                        Log.e(TAG, "Itinerary generation failed for trip " + current.id, task.getException());
                        ErrorMessages.FriendlyException friendly =
                                ErrorMessages.find(task.getException(), ErrorMessages.FriendlyException.class);
                        generation.setValue(Result.error(friendly != null
                                ? friendly.getMessage() : AiRepository.FAILURE_MESSAGE));
                    }
                });
    }

    public void dismissGenerationError() {
        generation.setValue(Result.success(null));
    }

    public void leave(String uid) {
        report(TripRepository.get().leaveTrip(tripId, uid));
    }

    public void delete(Trip current) {
        report(TripRepository.get().deleteTrip(current));
    }

    private void report(Task<Void> task) {
        membershipResult.setValue(new Event<>(Result.loading()));
        task.addOnCompleteListener(t -> membershipResult.setValue(new Event<>(t.isSuccessful()
                ? Result.success(null)
                : Result.error(ErrorMessages.from(t.getException())))));
    }
}
