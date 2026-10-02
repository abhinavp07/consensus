package com.abhinavpinisetti.consensus.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SettleUpTest {

    private static SettleUp.ExpenseInput expense(String paidBy, long cents, String... split) {
        return new SettleUp.ExpenseInput(paidBy, cents, Arrays.asList(split));
    }

    private static final List<String> GROUP = Arrays.asList("alex", "jordan", "sam");

    @Test
    public void noExpensesMeansZeroBalancesAndNoPayments() {
        Map<String, Long> balances = SettleUp.balances(Collections.emptyList(), GROUP);
        assertEquals(3, balances.size());
        for (long b : balances.values()) assertEquals(0, b);
        assertTrue(SettleUp.payments(balances).isEmpty());
    }

    @Test
    public void balanceIsPaidMinusShare() {
        Map<String, Long> balances = SettleUp.balances(
                Collections.singletonList(expense("sam", 9000, "alex", "jordan", "sam")), GROUP);
        assertEquals(6000, (long) balances.get("sam"));
        assertEquals(-3000, (long) balances.get("alex"));
        assertEquals(-3000, (long) balances.get("jordan"));
    }

    @Test
    public void unevenSplitsStillSumToZero() {
        Map<String, Long> balances = SettleUp.balances(
                Collections.singletonList(expense("alex", 1000, "alex", "jordan", "sam")), GROUP);
        long sum = 0;
        for (long b : balances.values()) sum += b;
        assertEquals(0, sum);
        // 1000 / 3 = 333 r1; the extra cent goes to the first id in sorted order ("alex").
        assertEquals(1000 - 334, (long) balances.get("alex"));
        assertEquals(-333, (long) balances.get("jordan"));
        assertEquals(-333, (long) balances.get("sam"));
    }

    @Test
    public void singlePaymentWhenOnePersonOwes() {
        Map<String, Long> balances = SettleUp.balances(
                Collections.singletonList(expense("sam", 8500, "alex", "sam")), GROUP);
        List<SettleUp.Payment> payments = SettleUp.payments(balances);
        assertEquals(Collections.singletonList(new SettleUp.Payment("alex", "sam", 4250)), payments);
    }

    @Test
    public void mixedExpensesSettleWithFewPayments() {
        List<SettleUp.ExpenseInput> expenses = Arrays.asList(
                expense("alex", 12000, "alex", "jordan", "sam"),  // dinner
                expense("jordan", 3000, "alex", "jordan", "sam"), // taxi
                expense("sam", 6000, "jordan", "sam"));           // tickets
        Map<String, Long> balances = SettleUp.balances(expenses, GROUP);
        // alex: +12000 - 4000 - 1000 = 7000; jordan: +3000 - 4000 - 1000 - 3000 = -5000;
        // sam: +6000 - 4000 - 1000 - 3000 = -2000
        assertEquals(7000, (long) balances.get("alex"));
        assertEquals(-5000, (long) balances.get("jordan"));
        assertEquals(-2000, (long) balances.get("sam"));

        List<SettleUp.Payment> payments = SettleUp.payments(balances);
        assertEquals(2, payments.size());
        assertEquals(new SettleUp.Payment("jordan", "alex", 5000), payments.get(0));
        assertEquals(new SettleUp.Payment("sam", "alex", 2000), payments.get(1));
    }

    @Test
    public void paymentsSettleEveryBalance() {
        List<String> group = Arrays.asList("a", "b", "c", "d", "e");
        List<SettleUp.ExpenseInput> expenses = new ArrayList<>();
        expenses.add(expense("a", 10001, "a", "b", "c", "d", "e"));
        expenses.add(expense("b", 2550, "c", "d"));
        expenses.add(expense("e", 799, "a", "e"));
        expenses.add(expense("c", 43210, "a", "b", "c"));
        Map<String, Long> balances = SettleUp.balances(expenses, group);

        List<SettleUp.Payment> payments = SettleUp.payments(balances);
        assertTrue("At most n-1 payments", payments.size() <= group.size() - 1);
        for (SettleUp.Payment p : payments) {
            assertTrue(p.amountCents > 0);
            balances.put(p.from, balances.get(p.from) + p.amountCents);
            balances.put(p.to, balances.get(p.to) - p.amountCents);
        }
        for (long b : balances.values()) assertEquals(0, b);
    }

    @Test
    public void formerMembersStillAppearInBalances() {
        Map<String, Long> balances = SettleUp.balances(
                Collections.singletonList(expense("gone", 2000, "alex", "gone")), GROUP);
        assertEquals(1000, (long) balances.get("gone"));
        assertEquals(-1000, (long) balances.get("alex"));
    }

    @Test
    public void ignoresInvalidExpenses() {
        Map<String, Long> balances = SettleUp.balances(Arrays.asList(
                expense("alex", 0, "alex", "sam"),
                new SettleUp.ExpenseInput("alex", 500, Collections.emptyList())), GROUP);
        for (long b : balances.values()) assertEquals(0, b);
    }
}
