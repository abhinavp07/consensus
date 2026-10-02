package com.abhinavpinisetti.consensus.data.repo;

import com.abhinavpinisetti.consensus.data.QueryLiveData;
import com.abhinavpinisetti.consensus.data.model.Expense;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** trips/{tripId}/expenses/{expenseId} */
public class ExpenseRepository {

    private static ExpenseRepository instance;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static synchronized ExpenseRepository get() {
        if (instance == null) instance = new ExpenseRepository();
        return instance;
    }

    private CollectionReference collection(String tripId) {
        return db.collection("trips").document(tripId).collection("expenses");
    }

    public QueryLiveData<Expense> expenses(String tripId) {
        return new QueryLiveData<>(collection(tripId).orderBy("createdAt", Query.Direction.DESCENDING), Expense.class);
    }

    public Task<DocumentReference> add(String tripId, String description, long amountCents,
                                       String paidBy, List<String> splitAmong) {
        Map<String, Object> data = new HashMap<>();
        data.put("description", description);
        data.put("amountCents", amountCents);
        data.put("paidBy", paidBy);
        data.put("splitAmong", splitAmong);
        data.put("createdAt", FieldValue.serverTimestamp());
        return collection(tripId).add(data);
    }

    public Task<Void> delete(String tripId, String expenseId) {
        return collection(tripId).document(expenseId).delete();
    }
}
