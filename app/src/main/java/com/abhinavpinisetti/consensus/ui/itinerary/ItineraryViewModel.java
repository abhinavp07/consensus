package com.abhinavpinisetti.consensus.ui.itinerary;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.abhinavpinisetti.consensus.data.ErrorMessages;
import com.abhinavpinisetti.consensus.data.model.Day;
import com.abhinavpinisetti.consensus.data.model.PlannedActivity;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.data.repo.AuthRepository;
import com.abhinavpinisetti.consensus.data.repo.ItineraryRepository;
import com.abhinavpinisetti.consensus.data.repo.PlacesRepository;
import com.abhinavpinisetti.consensus.data.repo.TripRepository;
import com.abhinavpinisetti.consensus.util.Event;
import com.abhinavpinisetti.consensus.util.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared by the itinerary screen and its day tabs. All data is live via snapshot listeners. */
public class ItineraryViewModel extends ViewModel {

    private String tripId;
    private LiveData<Result<Trip>> trip;
    private LiveData<Result<List<Day>>> days;
    private final Map<String, LiveData<Result<List<PlannedActivity>>>> activities = new HashMap<>();
    /** Activity+placeName pairs already looked up this session, so failures don't retry in a loop. */
    private final Set<String> lookupsAttempted = new HashSet<>();
    private final MutableLiveData<Event<String>> messages = new MutableLiveData<>();

    public void init(String tripId) {
        if (this.tripId != null) return;
        this.tripId = tripId;
        trip = TripRepository.get().trip(tripId);
        days = ItineraryRepository.get().days(tripId);
    }

    public String tripId() {
        return tripId;
    }

    public LiveData<Result<Trip>> trip() {
        return trip;
    }

    public LiveData<Result<List<Day>>> days() {
        return days;
    }

    public LiveData<Result<List<PlannedActivity>>> activitiesFor(String dayId) {
        LiveData<Result<List<PlannedActivity>>> live = activities.get(dayId);
        if (live == null) {
            live = ItineraryRepository.get().activitiesLive(tripId, dayId);
            activities.put(dayId, live);
        }
        return live;
    }

    public LiveData<Event<String>> messages() {
        return messages;
    }

    /** Tapping the same thumb again removes the vote; tapping the other one switches it. */
    public void vote(String dayId, PlannedActivity activity, int clicked) {
        String uid = AuthRepository.get().uid();
        int next = activity.voteOf(uid) == clicked ? 0 : clicked;
        ItineraryRepository.get().vote(tripId, dayId, activity.id, uid, next)
                .addOnFailureListener(e -> messages.setValue(new Event<>(ErrorMessages.from(e))));
    }

    /** Matches activities that have a place name but no lookup yet (e.g. after an alternative swap). */
    public void lookUpMissingPlaces(Trip current, String dayId, List<PlannedActivity> list) {
        if (current == null || !PlacesRepository.get().isAvailable()) return;
        List<ItineraryRepository.ActivityRef> refs = new ArrayList<>();
        for (PlannedActivity a : list) {
            if (a.placeLookupDone || a.placeName == null || a.placeName.trim().isEmpty()) continue;
            if (lookupsAttempted.add(dayId + "/" + a.id + "|" + a.placeName)) {
                refs.add(new ItineraryRepository.ActivityRef(dayId, a.id, a.placeName));
            }
        }
        PlacesRepository.get().matchActivities(current, refs);
    }
}
