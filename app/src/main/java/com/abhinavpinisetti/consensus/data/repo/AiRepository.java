package com.abhinavpinisetti.consensus.data.repo;

import android.util.Log;

import com.abhinavpinisetti.consensus.ai.GeneratedActivity;
import com.abhinavpinisetti.consensus.ai.GeneratedPlan;
import com.abhinavpinisetti.consensus.ai.InvalidPlanException;
import com.abhinavpinisetti.consensus.ai.ItineraryParser;
import com.abhinavpinisetti.consensus.ai.ItinerarySchemas;
import com.abhinavpinisetti.consensus.ai.MemberAnswers;
import com.abhinavpinisetti.consensus.ai.PromptBuilder;
import com.abhinavpinisetti.consensus.data.ErrorMessages;
import com.abhinavpinisetti.consensus.data.model.Day;
import com.abhinavpinisetti.consensus.data.model.PlannedActivity;
import com.abhinavpinisetti.consensus.data.model.Preferences;
import com.abhinavpinisetti.consensus.data.model.Trip;
import com.abhinavpinisetti.consensus.util.DateUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Gemini calls through Firebase AI Logic, using structured JSON output. */
public class AiRepository {

    public static final String FAILURE_MESSAGE = "Couldn't generate a plan. Try again.";
    public static final String ALTERNATIVES_FAILURE_MESSAGE = "Couldn't find alternatives. Try again.";

    private static final String TAG = "AiRepository";
    /** Update this when newer Gemini models become available in Firebase AI Logic. */
    private static final String MODEL_NAME = "gemini-3.8-flash";
    private static final long TIMEOUT_SECONDS = 90;
    private static AiRepository instance;

    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
    private GenerativeModelFutures planModel;
    private GenerativeModelFutures alternativesModel;

    public static synchronized AiRepository get() {
        if (instance == null) instance = new AiRepository();
        return instance;
    }

    private static GenerativeModelFutures jsonModel(Schema schema) {
        GenerationConfig.Builder config = new GenerationConfig.Builder();
        config.responseMimeType = "application/json";
        config.responseSchema = schema;
        GenerativeModel model = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel(MODEL_NAME, config.build());
        return GenerativeModelFutures.from(model);
    }

    private synchronized GenerativeModelFutures planModel() {
        if (planModel == null) planModel = jsonModel(ItinerarySchemas.plan());
        return planModel;
    }

    private synchronized GenerativeModelFutures alternativesModel() {
        if (alternativesModel == null) alternativesModel = jsonModel(ItinerarySchemas.alternatives());
        return alternativesModel;
    }

    /** Drafts a full itinerary from everyone's answers. Fails with a friendly message on any problem. */
    public Task<GeneratedPlan> generatePlan(Trip trip, List<Preferences> prefs) {
        int dayCount = DateUtils.tripLength(trip.startDate, trip.endDate);
        String prompt = PromptBuilder.itineraryPrompt(trip.destination, trip.startDate, trip.endDate,
                dayCount, trip.memberIds.size(), toAnswers(trip, prefs));

        return call(planModel(), prompt).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Plan generation request failed for trip " + trip.id, task.getException());
                throw new ErrorMessages.FriendlyException(FAILURE_MESSAGE, task.getException());
            }
            String json = task.getResult();
            try {
                return ItineraryParser.parsePlan(json, dayCount);
            } catch (InvalidPlanException e) {
                Log.e(TAG, "Invalid plan for trip " + trip.id + ": " + e.getMessage()
                        + "\nResponse: " + truncate(json), e);
                throw new ErrorMessages.FriendlyException(FAILURE_MESSAGE, e);
            }
        });
    }

    /** Returns three replacement options for {@code target} that fit the rest of its day. */
    public Task<List<GeneratedActivity>> findAlternatives(Trip trip, Day day, PlannedActivity target,
                                                          List<PlannedActivity> dayActivities,
                                                          List<Preferences> prefs) {
        List<String> others = new ArrayList<>();
        for (PlannedActivity a : dayActivities) {
            if (a.id != null && a.id.equals(target.id)) continue;
            others.add(a.time + " " + a.title);
        }
        String prompt = PromptBuilder.alternativesPrompt(trip.destination, day.date,
                target.title, target.time, target.description, others, toAnswers(trip, prefs));

        return call(alternativesModel(), prompt).continueWith(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Alternatives request failed for activity " + target.id, task.getException());
                throw new ErrorMessages.FriendlyException(ALTERNATIVES_FAILURE_MESSAGE, task.getException());
            }
            String json = task.getResult();
            try {
                return ItineraryParser.parseAlternatives(json);
            } catch (InvalidPlanException e) {
                Log.e(TAG, "Invalid alternatives: " + e.getMessage() + "\nResponse: " + truncate(json), e);
                throw new ErrorMessages.FriendlyException(ALTERNATIVES_FAILURE_MESSAGE, e);
            }
        });
    }

    private static List<MemberAnswers> toAnswers(Trip trip, List<Preferences> prefs) {
        List<MemberAnswers> answers = new ArrayList<>();
        for (Preferences p : prefs) {
            if (!trip.memberIds.contains(p.userId)) continue; // ignore people who left
            answers.add(new MemberAnswers(trip.firstNameOf(p.userId), p.budget, p.interests,
                    p.pace, p.mustDos, p.avoid));
        }
        return answers;
    }

    /** Sends a prompt and resolves to the raw response text, with a timeout. */
    private Task<String> call(GenerativeModelFutures model, String prompt) {
        TaskCompletionSource<String> result = new TaskCompletionSource<>();
        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> future = Futures.withTimeout(
                model.generateContent(content), TIMEOUT_SECONDS, TimeUnit.SECONDS, timeoutScheduler);

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse response) {
                String text = response.getText();
                if (text == null || text.trim().isEmpty()) {
                    result.setException(new InvalidPlanException("Empty response from model"));
                } else {
                    result.setResult(text);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                result.setException(t instanceof Exception ? (Exception) t : new Exception(t));
            }
        }, MoreExecutors.directExecutor());
        return result.getTask();
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 2000 ? s.substring(0, 2000) + "…" : s;
    }
}
