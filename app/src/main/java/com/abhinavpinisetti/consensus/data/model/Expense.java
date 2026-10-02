package com.abhinavpinisetti.consensus.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.List;

/** trips/{tripId}/expenses/{expenseId}. Money is stored as integer cents. */
public class Expense {
    @DocumentId public String id;
    public String description;
    public long amountCents;
    public String paidBy;
    public List<String> splitAmong = new ArrayList<>();
    @ServerTimestamp public Timestamp createdAt;

    public Expense() {}
}
