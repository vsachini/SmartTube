package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.leanback.widget.Presenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.presenter.base.LongClickPresenter;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/**
 * Featured card for the first Home row: big thumbnail, title, channel and a Play pill.
 */
public class HeroCardPresenter extends LongClickPresenter {
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.hero_card, parent, false);

        View frame = view.findViewById(R.id.hero_frame);
        float radius = context.getResources().getDimension(R.dimen.hero_card_corner_radius);
        float glowElevation = context.getResources().getDimension(R.dimen.card_glow_elevation);

        if (VERSION.SDK_INT >= 21) {
            frame.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View v, Outline outline) {
                    outline.setRoundRect(0, 0, v.getWidth(), v.getHeight(), radius);
                }
            });
            frame.setClipToOutline(true);
        }

        View ring = view.findViewById(R.id.hero_focus_ring);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            ring.setVisibility(hasFocus ? View.VISIBLE : View.GONE);

            if (VERSION.SDK_INT >= 28) {
                frame.setElevation(hasFocus ? glowElevation : 0);
            }
        });

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        super.onBindViewHolder(viewHolder, item);

        Video video = (Video) item;
        View view = viewHolder.view;
        Context context = view.getContext();

        ((TextView) view.findViewById(R.id.hero_title)).setText(video.getTitle());
        ((TextView) view.findViewById(R.id.hero_channel)).setText(video.getAuthor() != null ? video.getAuthor() : video.getSecondTitle());

        View frame = view.findViewById(R.id.hero_frame);
        setGlowColor(frame, Color.WHITE);

        if (context instanceof Activity && ((Activity) context).isDestroyed()) {
            return;
        }

        ImageView image = view.findViewById(R.id.hero_image);
        // bgImageUrl is the high resolution thumbnail when available
        String url = video.bgImageUrl != null ? video.bgImageUrl : video.cardImageUrl;

        Glide.with(context)
                .load(url)
                .apply(ViewUtil.glideOptions())
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        if (resource instanceof BitmapDrawable) {
                            setGlowColor(frame, VideoCardPresenter.getGlowColor(((BitmapDrawable) resource).getBitmap()));
                        }
                        return false;
                    }
                })
                .error(Glide.with(context).load(video.cardImageUrl).apply(ViewUtil.glideOptions()))
                .into(image);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        super.onUnbindViewHolder(viewHolder);

        ImageView image = viewHolder.view.findViewById(R.id.hero_image);
        Glide.with(viewHolder.view.getContext().getApplicationContext()).clear(image);
        image.setImageDrawable(null);
    }

    private static void setGlowColor(View frame, int color) {
        if (VERSION.SDK_INT >= 28) {
            frame.setOutlineAmbientShadowColor(color);
            frame.setOutlineSpotShadowColor(Color.TRANSPARENT);
        }
    }
}
