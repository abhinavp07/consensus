package com.abhinavpinisetti.consensus.ui.common;

import android.widget.ImageView;

import com.abhinavpinisetti.consensus.R;
import com.bumptech.glide.Glide;

public final class Avatars {

    private Avatars() {}

    public static void load(ImageView view, String url) {
        Glide.with(view)
                .load(url)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(view);
    }
}
