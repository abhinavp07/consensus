package com.abhinavpinisetti.consensus.ui.trips;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.abhinavpinisetti.consensus.data.ErrorMessages;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.data.repo.AuthRepository;
import com.abhinavpinisetti.consensus.data.repo.TripRepository;
import com.abhinavpinisetti.consensus.data.repo.UserRepository;
import com.abhinavpinisetti.consensus.util.Event;
import com.abhinavpinisetti.consensus.util.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TripsViewModel extends ViewModel {

    private LiveData<Result<List<Trip>>> trips;
    private final MutableLiveData<Event<Result<String>>> joinResult = new MutableLiveData<>();

    /** Every trip the user belongs to, soonest start date first, updating in real time. */
    public LiveData<Result<List<Trip>>> trips(String uid) {
        if (trips == null) {
            trips = Transformations.map(TripRepository.get().tripsForUser(uid), result -> {
                if (!result.isSuccess()) return result;
                List<Trip> sorted = new ArrayList<>(result.data);
                Collections.sort(sorted, Comparator.comparing((Trip t) -> t.startDate == null ? "" : t.startDate)
                        .thenComparing(t -> t.name == null ? "" : t.name));
                return Result.success(sorted);
            });
        }
        return trips;
    }

    public LiveData<Event<Result<String>>> joinResult() {
        return joinResult;
    }

    public void join(String code) {
        joinResult.setValue(new Event<>(Result.loading()));
        String uid = AuthRepository.get().uid();
        TripRepository.get().joinTrip(code, uid, UserRepository.get().currentProfile())
                .addOnCompleteListener(task -> joinResult.setValue(new Event<>(task.isSuccessful()
                        ? Result.success(task.getResult())
                        : Result.error(ErrorMessages.from(task.getException())))));
    }
}
