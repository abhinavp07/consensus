package com.abhinavpinisetti.consensus.ui.itinerary;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.abhinavpinisetti.consensus.ai.GeneratedActivity;
import com.abhinavpinisetti.consensus.data.ErrorMessages;
import com.abhinavpinisetti.consensus.data.model.Day;
import com.abhinavpinisetti.consensus.data.model.PlannedActivity;
import com.abhinavpinisetti.consensus.data.model.Preferences;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.data.repo.AiRepository;
import com.abhinavpinisetti.consensus.data.repo.ItineraryRepository;
import com.abhinavpinisetti.consensus.data.repo.PreferencesRepository;
import com.abhinavpinisetti.consensus.data.repo.TripRepository;
import com.abhinavpinisetti.consensus.util.Event;
import com.abhinavpinisetti.consensus.util.Result;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;

public class AlternativesViewModel extends ViewModel {

    private String tripId;
    private String dayId;
    private String date;
    private String activityId;
    private final MutableLiveData<Result<List<GeneratedActivity>>> options = new MutableLiveData<>();
    private final MutableLiveData<Event<Result<Void>>> picked = new MutableLiveData<>();

    public void init(String tripId, String dayId, String date, String activityId) {
        if (this.tripId != null) return;
        this.tripId = tripId;
        this.dayId = dayId;
        this.date = date;
        this.activityId = activityId;
        load();
    }

    public LiveData<Result<List<GeneratedActivity>>> options() {
        return options;
    }

    public LiveData<Event<Result<Void>>> picked() {
        return picked;
    }

    /** Sends the activity, the rest of its day, and the group's preferences to Gemini. */
    public void load() {
        options.setValue(Result.loading());
        Task<Trip> trip = TripRepository.get().fetchTrip(tripId);
        Task<List<PlannedActivity>> dayActivities = ItineraryRepository.get().fetchActivities(tripId, dayId);
        Task<List<Preferences>> prefs = PreferencesRepository.get().fetchAll(tripId);

        Tasks.whenAll(trip, dayActivities, prefs)
                .continueWithTask(all -> {
                    if (!all.isSuccessful()) return Tasks.forException(all.getException());
                    PlannedActivity target = null;
                    for (PlannedActivity a : dayActivities.getResult()) {
                        if (activityId.equals(a.id)) target = a;
                    }
                    if (target == null) {
                        return Tasks.forException(new ErrorMessages.FriendlyException("This activity was deleted."));
                    }
                    Day day = new Day();
                    day.id = dayId;
                    day.date = date;
                    return AiRepository.get().findAlternatives(trip.getResult(), day, target,
                            dayActivities.getResult(), prefs.getResult());
                })
                .addOnCompleteListener(task -> options.setValue(task.isSuccessful()
                        ? Result.success(task.getResult())
                        : Result.error(ErrorMessages.from(task.getException()))));
    }

    /** Replaces the activity with the chosen option and clears its votes. */
    public void pick(GeneratedActivity option) {
        picked.setValue(new Event<>(Result.loading()));
        ItineraryRepository.get().replaceWithAlternative(tripId, dayId, activityId, option)
                .addOnCompleteListener(task -> picked.setValue(new Event<>(task.isSuccessful()
                        ? Result.success(null)
                        : Result.error(ErrorMessages.from(task.getException())))));
    }
}
