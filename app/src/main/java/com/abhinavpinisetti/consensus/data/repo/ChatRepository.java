package com.abhinavpinisetti.consensus.data.repo;

import com.abhinavpinisetti.consensus.data.QueryLiveData;
import com.abhinavpinisetti.consensus.data.model.MemberProfile;
import com.abhinavpinisetti.consensus.data.model.Message;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/** trips/{tripId}/messages/{messageId} */
public class ChatRepository {

    public static final int PAGE_SIZE = 50;
    private static ChatRepository instance;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static synchronized ChatRepository get() {
        if (instance == null) instance = new ChatRepository();
        return instance;
    }

    /** The newest {@code limit} messages, oldest first, updating live. */
    public QueryLiveData<Message> latestMessages(String tripId, int limit) {
        return new QueryLiveData<>(db.collection("trips").document(tripId).collection("messages")
                .orderBy("timestamp")
                .limitToLast(limit), Message.class);
    }

    public Task<DocumentReference> send(String tripId, String uid, MemberProfile me, String text) {
        Map<String, Object> data = new HashMap<>();
        data.put("senderId", uid);
        data.put("senderName", me.name);
        data.put("senderPhotoUrl", me.photoUrl);
        data.put("text", text);
        data.put("timestamp", FieldValue.serverTimestamp());
        return db.collection("trips").document(tripId).collection("messages").add(data);
    }
}
