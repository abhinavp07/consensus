package com.abhinavpinisetti.consensus.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

/** trips/{tripId}/messages/{messageId}. Sender name/photo are denormalized for display. */
public class Message {
    @DocumentId public String id;
    public String senderId;
    public String senderName;
    public String senderPhotoUrl;
    public String text;
    @ServerTimestamp public Timestamp timestamp;

    public Message() {}
}
