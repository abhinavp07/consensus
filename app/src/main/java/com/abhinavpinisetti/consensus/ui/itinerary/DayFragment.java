package com.abhinavpinisetti.consensus.ui.itinerary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.abhinavpinisetti.consensus.BuildConfig;
import com.abhinavpinisetti.consensus.R;
import com.abhinavpinisetti.consensus.data.model.Day;
import com.abhinavpinisetti.consensus.data.model.PlannedActivity;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.data.repo.AuthRepository;
import com.abhinavpinisetti.consensus.databinding.FragmentDayBinding;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

/** One day's activities, as a list or a map. */
public class DayFragment extends Fragment implements ActivityAdapter.Listener {

    private static final String ARG_DAY_ID = "dayId";
    private static final String ARG_DATE = "date";
    private static final String ARG_DAY_NUMBER = "dayNumber";
    private static final String STATE_SHOWING_MAP = "showingMap";
    private static final String MAP_TAG = "map";

    private FragmentDayBinding binding;
    private ItineraryViewModel viewModel;
    private ActivityAdapter adapter;
    private String dayId;
    private String date;
    private Trip trip;
    private List<PlannedActivity> activities = new ArrayList<>();
    private SupportMapFragment mapFragment;
    private GoogleMap map;
    private boolean showingMap;

    public static DayFragment newInstance(String dayId, String date, int dayNumber) {
        Bundle args = new Bundle();
        args.putString(ARG_DAY_ID, dayId);
        args.putString(ARG_DATE, date);
        args.putInt(ARG_DAY_NUMBER, dayNumber);
        DayFragment fragment = new DayFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        dayId = requireArguments().getString(ARG_DAY_ID);
        date = requireArguments().getString(ARG_DATE);
        showingMap = savedInstanceState != null && savedInstanceState.getBoolean(STATE_SHOWING_MAP);
        viewModel = new ViewModelProvider(requireActivity()).get(ItineraryViewModel.class);

        adapter = new ActivityAdapter(this);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        binding.viewToggle.check(showingMap ? R.id.show_map : R.id.show_list);
        binding.viewToggle.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked) return;
            showingMap = id == R.id.show_map;
            renderMode();
        });

        viewModel.trip().observe(getViewLifecycleOwner(), result -> {
            if (!result.isSuccess() || result.data == null) return;
            trip = result.data;
            adapter.setContext(AuthRepository.get().uid(), trip.memberIds.size());
            viewModel.lookUpMissingPlaces(trip, dayId, activities);
        });
        viewModel.days().observe(getViewLifecycleOwner(), result -> {
            if (!result.isSuccess()) return;
            for (Day d : result.data) {
                if (d.id.equals(dayId)) {
                    boolean hasNote = d.note != null && !d.note.trim().isEmpty();
                    binding.noteCard.setVisibility(hasNote ? View.VISIBLE : View.GONE);
                    binding.note.setText(d.note);
                }
            }
        });
        viewModel.activitiesFor(dayId).observe(getViewLifecycleOwner(), result -> {
            binding.progress.setVisibility(result.isLoading() ? View.VISIBLE : View.GONE);
            binding.error.setVisibility(result.isError() ? View.VISIBLE : View.GONE);
            if (result.isError()) binding.error.setText(result.message);
            if (!result.isSuccess()) return;
            activities = result.data;
            adapter.submitList(activities);
            viewModel.lookUpMissingPlaces(trip, dayId, activities);
            renderMode();
        });
        renderMode();
    }

    private void renderMode() {
        if (binding == null) return;
        boolean empty = activities.isEmpty();
        binding.empty.setVisibility(!showingMap && empty ? View.VISIBLE : View.GONE);
        binding.list.setVisibility(showingMap ? View.GONE : View.VISIBLE);
        binding.mapArea.setVisibility(showingMap ? View.VISIBLE : View.GONE);
        if (!showingMap) return;

        if (BuildConfig.MAPS_API_KEY.isEmpty() || "DEFAULT_API_KEY".equals(BuildConfig.MAPS_API_KEY)) {
            binding.mapMessage.setText(R.string.map_unavailable);
            binding.mapMessage.setVisibility(View.VISIBLE);
            return;
        }
        if (mapFragment == null) {
            mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentByTag(MAP_TAG);
            if (mapFragment == null) {
                mapFragment = SupportMapFragment.newInstance();
                getChildFragmentManager().beginTransaction().replace(R.id.map_container, mapFragment, MAP_TAG).commit();
            }
            mapFragment.getMapAsync(googleMap -> {
                map = googleMap;
                map.getUiSettings().setMapToolbarEnabled(false);
                renderMap();
            });
        } else if (map != null) {
            renderMap();
        }
    }

    private void renderMap() {
        if (map == null || binding == null) return;
        int color = MaterialColors.getColor(binding.getRoot(), androidx.appcompat.R.attr.colorPrimary);
        int pins = DayMapRenderer.render(requireContext(), map, binding.mapContainer, activities, color);
        binding.mapMessage.setText(R.string.map_no_locations);
        binding.mapMessage.setVisibility(pins == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_SHOWING_MAP, showingMap);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        mapFragment = null;
        map = null;
    }

    @Override
    public void onEdit(PlannedActivity activity) {
        startActivity(ActivityEditActivity.intent(requireContext(), viewModel.tripId(), dayId, activity.id));
    }

    @Override
    public void onVote(PlannedActivity activity, int value) {
        viewModel.vote(dayId, activity, value);
    }

    @Override
    public void onFindAlternatives(PlannedActivity activity) {
        AlternativesBottomSheet.newInstance(viewModel.tripId(), dayId, date, activity.id)
                .show(getChildFragmentManager(), "alternatives");
    }

    @Override
    public void onDirections(PlannedActivity activity) {
        Directions.open(requireContext(), activity, trip == null ? "" : trip.destination);
    }
}
