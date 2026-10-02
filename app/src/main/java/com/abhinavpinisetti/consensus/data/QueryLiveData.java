package com.abhinavpinisetti.consensus.data;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.abhinavpinisetti.consensus.util.Result;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.List;

/**
 * Real-time query results. The snapshot listener is attached only while the LiveData has an
 * active observer and removed as soon as the screen stops, so closed screens don't leak
 * listeners or keep paying for reads.
 */
public class QueryLiveData<T> extends LiveData<Result<List<T>>> {

    private static final String TAG = "QueryLiveData";

    private final Query query;
    private final Class<T> type;
    private ListenerRegistration registration;

    public QueryLiveData(Query query, Class<T> type) {
        super(Result.loading());
        this.query = query;
        this.type = type;
    }

    @Override
    protected void onActive() {
        registration = query.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed for " + type.getSimpleName(), error);
                setValue(Result.error(ErrorMessages.from(error)));
                return;
            }
            if (snapshot != null) {
                setValue(Result.success(snapshot.toObjects(type, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)));
            }
        });
    }

    @Override
    protected void onInactive() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }
}
