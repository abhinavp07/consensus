package com.abhinavpinisetti.consensus.data;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.abhinavpinisetti.consensus.util.Result;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Real-time single document. Emits success(null) when the document doesn't exist (e.g. the trip
 * was deleted). Listener lifecycle matches {@link QueryLiveData}.
 */
public class DocumentLiveData<T> extends LiveData<Result<T>> {

    private static final String TAG = "DocumentLiveData";

    private final DocumentReference ref;
    private final Class<T> type;
    private ListenerRegistration registration;

    public DocumentLiveData(DocumentReference ref, Class<T> type) {
        super(Result.loading());
        this.ref = ref;
        this.type = type;
    }

    @Override
    protected void onActive() {
        registration = ref.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed for " + ref.getPath(), error);
                setValue(Result.error(ErrorMessages.from(error)));
                return;
            }
            if (snapshot == null || !snapshot.exists()) {
                setValue(Result.success(null));
            } else {
                setValue(Result.success(snapshot.toObject(type, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)));
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
