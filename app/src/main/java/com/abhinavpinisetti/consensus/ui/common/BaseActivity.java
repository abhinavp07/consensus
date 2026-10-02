package com.abhinavpinisetti.consensus.ui.common;

import android.content.Intent;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.abhinavpinisetti.consensus.data.repo.AuthRepository;
import com.abhinavpinisetti.consensus.ui.auth.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;

/** Shared setup: edge-to-edge insets, toolbars, and a signed-in guard. */
public abstract class BaseActivity extends AppCompatActivity {

    /** Sets the content view and pads it so nothing hides under system bars or the keyboard. */
    protected void setContentViewWithInsets(View root) {
        EdgeToEdge.enable(this);
        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    protected void setupToolbar(MaterialToolbar toolbar, boolean showUp) {
        setSupportActionBar(toolbar);
        if (showUp && getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }
    }

    /** Returns the signed-in user's ID, or sends them to the login screen and returns null. */
    protected String requireUid() {
        String uid = AuthRepository.get().uid();
        if (uid == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
        return uid;
    }
}
