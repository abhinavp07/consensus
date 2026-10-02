package com.abhinavpinisetti.consensus.ui.expenses;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.abhinavpinisetti.consensus.data.ErrorMessages;
import com.abhinavpinisetti.consensus.data.model.Expense;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.data.repo.ExpenseRepository;
import com.abhinavpinisetti.consensus.data.repo.TripRepository;
import com.abhinavpinisetti.consensus.util.Event;
import com.abhinavpinisetti.consensus.util.Result;
import com.abhinavpinisetti.consensus.util.SettleUp;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Used by the expenses list, add-expense form and settle-up screens. */
public class ExpensesViewModel extends ViewModel {

    private String tripId;
    private LiveData<Result<Trip>> trip;
    private LiveData<Result<List<Expense>>> expenses;
    private final MutableLiveData<Event<Result<Void>>> writeResult = new MutableLiveData<>();

    public void init(String tripId) {
        if (this.tripId != null) return;
        this.tripId = tripId;
        trip = TripRepository.get().trip(tripId);
        expenses = ExpenseRepository.get().expenses(tripId);
    }

    public LiveData<Result<Trip>> trip() {
        return trip;
    }

    public LiveData<Result<List<Expense>>> expenses() {
        return expenses;
    }

    public LiveData<Event<Result<Void>>> writeResult() {
        return writeResult;
    }

    public void add(String description, long amountCents, String paidBy, List<String> splitAmong) {
        writeResult.setValue(new Event<>(Result.loading()));
        ExpenseRepository.get().add(tripId, description, amountCents, paidBy, splitAmong)
                .addOnCompleteListener(t -> writeResult.setValue(new Event<>(t.isSuccessful()
                        ? Result.success(null) : Result.error(ErrorMessages.from(t.getException())))));
    }

    public void delete(Expense expense) {
        ExpenseRepository.get().delete(tripId, expense.id)
                .addOnFailureListener(e -> writeResult.setValue(new Event<>(Result.error(ErrorMessages.from(e)))));
    }

    public static long total(List<Expense> expenses) {
        long total = 0;
        for (Expense e : expenses) total += e.amountCents;
        return total;
    }

    /** Everyone with a stake: current members plus anyone who has since left but appears in an expense. */
    public static Set<String> participants(Trip trip, List<Expense> expenses) {
        Set<String> ids = new LinkedHashSet<>(trip.memberIds);
        for (Expense e : expenses) {
            ids.add(e.paidBy);
            ids.addAll(e.splitAmong);
        }
        return ids;
    }

    public static Map<String, Long> balances(Trip trip, List<Expense> expenses) {
        List<SettleUp.ExpenseInput> inputs = new ArrayList<>();
        for (Expense e : expenses) inputs.add(new SettleUp.ExpenseInput(e.paidBy, e.amountCents, e.splitAmong));
        return SettleUp.balances(inputs, participants(trip, expenses));
    }
}
