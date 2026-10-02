package com.abhinavpinisetti.consensus.ui.itinerary;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.abhinavpinisetti.consensus.data.ErrorMessages;
import com.abhinavpinisetti.consensus.data.model.PlannedActivity;
import com.abhinavpinisetti.consensus.data.repo.ItineraryRepository;
import com.abhinavpinisetti.consensus.util.Event;
import com.abhinavpinisetti.consensus.util.Result;
import com.google.android.gms.tasks.Task;

import java.util.Objects;

public class ActivityEditViewModel extends ViewModel {

    private String tripId;
    private String dayId;
    private String activityId;
    private PlannedActivity original;
    private final MutableLiveData<Result<PlannedActivity>> existing = new MutableLiveData<>();
    private final MutableLiveData<Event<Result<Void>>> done = new MutableLiveData<>();
    public final MutableLiveData<String> time = new MutableLiveData<>();
    public boolean prefilled;

    public void init(String tripId, String dayId, String activityId) {
        if (this.tripId != null) return;
        this.tripId = tripId;
        this.dayId = dayId;
        this.activityId = activityId;
        if (activityId == null) {
            existing.setValue(Result.success(null));
        } else {
            load();
        }
    }

    public void load() {
        existing.setValue(Result.loading());
        ItineraryRepository.get().fetchActivity(tripId, dayId, activityId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                original = task.getResult();
                existing.setValue(Result.success(original));
            } else {
                existing.setValue(Result.error(ErrorMessages.from(task.getException())));
            }
        });
    }

    public boolean isEditing() {
        return activityId != null;
    }

    public LiveData<Result<PlannedActivity>> existing() {
        return existing;
    }

    public LiveData<Event<Result<Void>>> done() {
        return done;
    }

    public void save(String title, String description, double cost, String placeName) {
        String place = placeName.isEmpty() ? null : placeName;
        Task<?> task;
        if (isEditing()) {
            boolean placeChanged = original == null || !Objects.equals(original.placeName, place);
            task = ItineraryRepository.get().updateActivity(tripId, dayId, activityId,
                    title, time.getValue(), description, cost, place, placeChanged);
        } else {
            PlannedActivity a = new PlannedActivity();
            a.title = title;
            a.time = time.getValue();
            a.description = description;
            a.estimatedCost = cost;
            a.placeName = place;
            a.source = PlannedActivity.SOURCE_MANUAL;
            task = ItineraryRepository.get().addActivity(tripId, dayId, a);
        }
        report(task);
    }

    public void delete() {
        report(ItineraryRepository.get().deleteActivity(tripId, dayId, activityId));
    }

    private void report(Task<?> task) {
        done.setValue(new Event<>(Result.loading()));
        task.addOnCompleteListener(t -> done.setValue(new Event<>(t.isSuccessful()
                ? Result.success(null)
                : Result.error(ErrorMessages.from(t.getException())))));
    }
}
