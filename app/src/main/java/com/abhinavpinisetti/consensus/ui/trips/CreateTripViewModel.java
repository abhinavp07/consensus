package com.abhinavpinisetti.consensus.ui.trips;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.abhinavpinisetti.consensus.data.ErrorMessages;
import com.abhinavpinisetti.consensus.data.repo.AuthRepository;
import com.abhinavpinisetti.consensus.data.repo.TripRepository;
import com.abhinavpinisetti.consensus.data.repo.UserRepository;
import com.abhinavpinisetti.consensus.util.Event;
import com.abhinavpinisetti.consensus.util.Result;

import java.time.LocalDate;

/** Holds the form's picked values (dates, place) across rotation and saves the trip. */
public class CreateTripViewModel extends ViewModel {

    public final MutableLiveData<LocalDate> startDate = new MutableLiveData<>();
    public final MutableLiveData<LocalDate> endDate = new MutableLiveData<>();
    public String placeId;
    public Double placeLat;
    public Double placeLng;
    /** The destination text as chosen from autocomplete, so manual edits can clear the place. */
    public String placeName;

    private final MutableLiveData<Event<Result<String>>> saveResult = new MutableLiveData<>();

    public LiveData<Event<Result<String>>> saveResult() {
        return saveResult;
    }

    public void setPlace(String name, String id, Double lat, Double lng) {
        placeName = name;
        placeId = id;
        placeLat = lat;
        placeLng = lng;
    }

    public void save(String name, String destination) {
        TripRepository.TripDraft draft = new TripRepository.TripDraft();
        draft.name = name;
        draft.destination = destination;
        if (destination.equals(placeName)) {
            draft.destinationPlaceId = placeId;
            draft.destinationLat = placeLat;
            draft.destinationLng = placeLng;
        }
        draft.startDate = startDate.getValue();
        draft.endDate = endDate.getValue();

        saveResult.setValue(new Event<>(Result.loading()));
        TripRepository.get()
                .createTrip(draft, AuthRepository.get().uid(), UserRepository.get().currentProfile())
                .addOnCompleteListener(task -> saveResult.setValue(new Event<>(task.isSuccessful()
                        ? Result.success(task.getResult())
                        : Result.error(ErrorMessages.from(task.getException())))));
    }
}
