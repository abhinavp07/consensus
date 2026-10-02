package com.abhinavpinisetti.consensus.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.abhinavpinisetti.consensus.data.ErrorMessages;
import com.abhinavpinisetti.consensus.data.model.Message;
import com.abhinavpinisetti.consensus.data.repo.AuthRepository;
import com.abhinavpinisetti.consensus.data.repo.ChatRepository;
import com.abhinavpinisetti.consensus.data.repo.UserRepository;
import com.abhinavpinisetti.consensus.util.Event;
import com.abhinavpinisetti.consensus.util.Result;

import java.util.List;

/**
 * Listens to the latest N messages. Scrolling to the top grows N by a page, which re-attaches
 * the listener with a larger window so older messages appear above the current ones.
 */
public class ChatViewModel extends ViewModel {

    private String tripId;
    private final MutableLiveData<Integer> limit = new MutableLiveData<>(ChatRepository.PAGE_SIZE);
    private LiveData<Result<List<Message>>> messages;
    private final MutableLiveData<Event<String>> errors = new MutableLiveData<>();
    private boolean reachedStart;

    public void init(String tripId) {
        if (this.tripId != null) return;
        this.tripId = tripId;
        messages = Transformations.switchMap(limit, l -> ChatRepository.get().latestMessages(tripId, l));
    }

    public LiveData<Result<List<Message>>> messages() {
        return messages;
    }

    public LiveData<Event<String>> errors() {
        return errors;
    }

    /** Called with each result so we know when there's nothing older left to load. */
    public void onMessagesLoaded(int count) {
        Integer current = limit.getValue();
        reachedStart = current != null && count < current;
    }

    public void loadOlder() {
        if (reachedStart) return;
        Integer current = limit.getValue();
        reachedStart = true; // blocks repeat calls until the bigger page arrives
        limit.setValue((current == null ? 0 : current) + ChatRepository.PAGE_SIZE);
    }

    public void send(String text) {
        ChatRepository.get().send(tripId, AuthRepository.get().uid(), UserRepository.get().currentProfile(), text)
                .addOnFailureListener(e -> errors.setValue(new Event<>(ErrorMessages.from(e))));
    }
}
