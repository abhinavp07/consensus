package com.abhinavpinisetti.consensus.data.model;

import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.List;

/** users/{userId} */
public class User {
    @DocumentId public String id;
    public String name;
    public String email;
    public String photoUrl;
    public List<String> fcmTokens = new ArrayList<>();

    public User() {}
}
