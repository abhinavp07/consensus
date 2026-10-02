package com.abhinavpinisetti.consensus.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Pure settle-up math. All amounts are integer cents.
 *
 * Balance = amount paid - fair share. Positive means the group owes you.
 * Payments are found greedily (largest debtor pays largest creditor), which produces at most
 * n-1 payments and in practice the fewest for typical groups.
 */
public final class SettleUp {

    private SettleUp() {}

    public static final class ExpenseInput {
        public final String paidBy;
        public final long amountCents;
        public final List<String> splitAmong;

        public ExpenseInput(String paidBy, long amountCents, List<String> splitAmong) {
            this.paidBy = paidBy;
            this.amountCents = amountCents;
            this.splitAmong = splitAmong;
        }
    }

    public static final class Payment {
        public final String from;
        public final String to;
        public final long amountCents;

        public Payment(String from, String to, long amountCents) {
            this.from = from;
            this.to = to;
            this.amountCents = amountCents;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Payment)) return false;
            Payment p = (Payment) o;
            return amountCents == p.amountCents && from.equals(p.from) && to.equals(p.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to, amountCents);
        }

        @Override
        public String toString() {
            return from + " pays " + to + " " + amountCents;
        }
    }

    /**
     * Computes each person's balance. Every member appears in the result, even with a zero balance.
     * When an amount doesn't divide evenly, the leftover cents go to participants in sorted-id
     * order so results are deterministic and always sum to zero.
     */
    public static Map<String, Long> balances(List<ExpenseInput> expenses, Collection<String> members) {
        Map<String, Long> balances = new TreeMap<>();
        for (String m : members) balances.put(m, 0L);

        for (ExpenseInput e : expenses) {
            if (e.amountCents <= 0 || e.splitAmong == null || e.splitAmong.isEmpty()) continue;
            List<String> split = new ArrayList<>(e.splitAmong);
            Collections.sort(split);
            long share = e.amountCents / split.size();
            long remainder = e.amountCents % split.size();

            add(balances, e.paidBy, e.amountCents);
            for (int i = 0; i < split.size(); i++) {
                long owed = share + (i < remainder ? 1 : 0);
                add(balances, split.get(i), -owed);
            }
        }
        return balances;
    }

    public static List<Payment> payments(Map<String, Long> balances) {
        List<Entry> creditors = new ArrayList<>();
        List<Entry> debtors = new ArrayList<>();
        for (Map.Entry<String, Long> b : balances.entrySet()) {
            if (b.getValue() > 0) creditors.add(new Entry(b.getKey(), b.getValue()));
            else if (b.getValue() < 0) debtors.add(new Entry(b.getKey(), -b.getValue()));
        }

        List<Payment> result = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Collections.sort(creditors);
            Collections.sort(debtors);
            Entry creditor = creditors.get(0);
            Entry debtor = debtors.get(0);
            long amount = Math.min(creditor.amount, debtor.amount);
            result.add(new Payment(debtor.id, creditor.id, amount));
            creditor.amount -= amount;
            debtor.amount -= amount;
            if (creditor.amount == 0) creditors.remove(0);
            if (debtor.amount == 0) debtors.remove(0);
        }
        return result;
    }

    private static void add(Map<String, Long> balances, String id, long delta) {
        Long current = balances.get(id);
        balances.put(id, (current == null ? 0L : current) + delta);
    }

    /** Sorted largest amount first, ties broken by id for deterministic output. */
    private static final class Entry implements Comparable<Entry> {
        final String id;
        long amount;

        Entry(String id, long amount) {
            this.id = id;
            this.amount = amount;
        }

        @Override
        public int compareTo(Entry o) {
            int byAmount = Long.compare(o.amount, amount);
            return byAmount != 0 ? byAmount : id.compareTo(o.id);
        }
    }
}
