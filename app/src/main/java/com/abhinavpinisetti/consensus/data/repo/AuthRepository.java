package com.abhinavpinisetti.consensus.data.repo;

import android.app.Activity;
import android.content.Context;
import android.os.CancellationSignal;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.abhinavpinisetti.consensus.R;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/** Google sign-in (via Credential Manager) and Firebase Authentication. */
public class AuthRepository {

    private static final String TAG = "AuthRepository";
    private static AuthRepository instance;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public static synchronized AuthRepository get() {
        if (instance == null) instance = new AuthRepository();
        return instance;
    }

    public interface GoogleTokenCallback {
        void onToken(String idToken);

        void onCancelled();

        /** No Google account is signed in on this device. */
        void onNoAccount();

        void onError(Exception e);
    }

    public FirebaseUser currentUser() {
        return auth.getCurrentUser();
    }

    public String uid() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? null : user.getUid();
    }

    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    /** Shows the "Sign in with Google" sheet and returns a Google ID token. */
    public void requestGoogleIdToken(Activity activity, GoogleTokenCallback callback) {
        GetSignInWithGoogleOption option =
                new GetSignInWithGoogleOption.Builder(activity.getString(R.string.default_web_client_id)).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();

        CredentialManager.create(activity).getCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        Credential credential = response.getCredential();
                        if (credential instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                            try {
                                callback.onToken(GoogleIdTokenCredential.createFrom(credential.getData()).getIdToken());
                            } catch (Exception e) {
                                Log.e(TAG, "Could not parse Google ID token", e);
                                callback.onError(e);
                            }
                        } else {
                            callback.onError(new IllegalStateException("Unexpected credential type " + credential.getType()));
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        if (e instanceof GetCredentialCancellationException) {
                            callback.onCancelled();
                        } else if (e instanceof NoCredentialException) {
                            callback.onNoAccount();
                        } else {
                            Log.e(TAG, "Credential Manager error", e);
                            callback.onError(e);
                        }
                    }
                });
    }

    public Task<FirebaseUser> signInWithGoogle(String idToken) {
        return auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .continueWith(task -> task.getResult().getUser());
    }

    /** Used when sign-in half-completed (e.g. the profile couldn't be saved). */
    public void signOutFirebaseOnly() {
        auth.signOut();
    }

    /** Signs out of Firebase and clears the saved Google credential so the account picker shows next time. */
    public void signOut(Context context) {
        auth.signOut();
        CredentialManager.create(context).clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                new CancellationSignal(),
                ContextCompat.getMainExecutor(context),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void unused) {
                    }

                    @Override
                    public void onError(ClearCredentialException e) {
                        Log.w(TAG, "Could not clear credential state", e);
                    }
                });
    }
}
