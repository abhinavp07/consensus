package com.abhinavpinisetti.consensus.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.abhinavpinisetti.consensus.data.model.Message;
import com.abhinavpinisetti.consensus.databinding.ActivityChatBinding;
import com.abhinavpinisetti.consensus.ui.common.BaseActivity;
import com.abhinavpinisetti.consensus.util.Result;

import java.util.List;

/** Group chat: oldest at top, newest at bottom, live updates, older pages on scroll up. */
public class ChatActivity extends BaseActivity {

    private static final String EXTRA_TRIP_ID = "tripId";

    private ActivityChatBinding binding;
    private ChatViewModel viewModel;
    private MessageAdapter adapter;
    private String newestId;

    public static Intent intent(Context context, String tripId) {
        return new Intent(context, ChatActivity.class).putExtra(EXTRA_TRIP_ID, tripId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String uid = requireUid();
        if (uid == null) return;

        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentViewWithInsets(binding.getRoot());
        setupToolbar(binding.toolbar, true);

        adapter = new MessageAdapter(uid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.list.setLayoutManager(layoutManager);
        binding.list.setAdapter(adapter);
        binding.list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy < 0 && layoutManager.findFirstVisibleItemPosition() <= 2) viewModel.loadOlder();
            }
        });

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        viewModel.init(getIntent().getStringExtra(EXTRA_TRIP_ID));
        viewModel.messages().observe(this, this::render);
        viewModel.errors().observe(this, event -> {
            String message = event.consume();
            if (message != null) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });

        binding.send.setEnabled(false);
        binding.input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.send.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        binding.send.setOnClickListener(v -> {
            String text = binding.input.getText() == null ? "" : binding.input.getText().toString().trim();
            if (text.isEmpty()) return;
            binding.input.setText("");
            viewModel.send(text);
        });
    }

    private void render(Result<List<Message>> result) {
        binding.progress.setVisibility(result.isLoading() && adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        binding.error.setVisibility(result.isError() ? View.VISIBLE : View.GONE);
        if (result.isError()) binding.error.setText(result.message);
        if (!result.isSuccess()) return;

        List<Message> messages = result.data;
        viewModel.onMessagesLoaded(messages.size());
        binding.empty.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
        String latest = messages.isEmpty() ? null : messages.get(messages.size() - 1).id;
        boolean newAtBottom = latest != null && !latest.equals(newestId);
        newestId = latest;
        adapter.submitList(messages, () -> {
            if (newAtBottom) binding.list.scrollToPosition(adapter.getItemCount() - 1);
        });
    }
}
