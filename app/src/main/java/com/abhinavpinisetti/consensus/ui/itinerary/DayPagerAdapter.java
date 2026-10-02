package com.abhinavpinisetti.consensus.ui.itinerary;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.abhinavpinisetti.consensus.data.model.Day;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class DayPagerAdapter extends FragmentStateAdapter {

    private final List<Day> days = new ArrayList<>();

    DayPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    /** Only rebuilds tabs when the set of days changes, not on every note update. */
    void setDays(List<Day> newDays) {
        boolean changed = newDays.size() != days.size();
        for (int i = 0; !changed && i < newDays.size(); i++) {
            Day a = newDays.get(i);
            Day b = days.get(i);
            changed = !Objects.equals(a.id, b.id) || !Objects.equals(a.date, b.date);
        }
        if (!changed) return;
        days.clear();
        days.addAll(newDays);
        notifyDataSetChanged();
    }

    Day dayAt(int position) {
        return position >= 0 && position < days.size() ? days.get(position) : null;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Day day = days.get(position);
        return DayFragment.newInstance(day.id, day.date, day.dayNumber);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    @Override
    public long getItemId(int position) {
        return days.get(position).dayNumber;
    }

    @Override
    public boolean containsItem(long itemId) {
        for (Day d : days) if (d.dayNumber == itemId) return true;
        return false;
    }
}
