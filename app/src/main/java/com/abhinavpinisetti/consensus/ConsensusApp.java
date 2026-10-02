package com.abhinavpinisetti.consensus;

import android.app.Application;

import com.abhinavpinisetti.consensus.data.repo.PlacesRepository;
import com.abhinavpinisetti.consensus.notifications.Notifications;

public class ConsensusApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PlacesRepository.get().init(this);
        Notifications.createChannel(this);
    }
}
