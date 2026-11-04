package com.liskovsoft.smartyoutubetv2.tv.ui.common;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.PlaybackActivity;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class UriBackgroundManager {
    private static final String TAG = UriBackgroundManager.class.getSimpleName();
    private static final int BACKGROUND_UPDATE_DELAY_MS = 300;
    private Uri mBackgroundURI;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Runnable mBackgroundTask;
    private BackgroundManager mBackgroundManager;
    private final Activity mActivity;
    private final Handler mHandler;
    private int mBackgroundColor = -1;
    private static final int AMBIENT_UPDATE_DELAY_MS = 400;
    private static final int AMBIENT_WIDTH = 96;
    private static final int AMBIENT_HEIGHT = 54;
    private static final int AMBIENT_BLUR_RADIUS = 6;
    private String mAmbientUrl;
    private Drawable mAmbientDrawable;
    private final Runnable mAmbientTask = () -> loadAmbientBackground(mAmbientUrl);

    public UriBackgroundManager(Activity activity) {
        mActivity = activity;
        mHandler = new Handler();
        prepareBackgroundManager();
        setDefaultBackground();
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(mActivity);
        mBackgroundManager.attach(mActivity.getWindow());
        mDefaultBackground = ContextCompat.getDrawable(mActivity, Helpers.getThemeAttr(mActivity, R.attr.shelfBackground));
        mBackgroundTask = new UpdateBackgroundTask();
        mMetrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void startBackgroundTimer(Uri backgroundURI) {
        mBackgroundURI = backgroundURI;
        mHandler.removeCallbacks(mBackgroundTask);
        mHandler.postDelayed(mBackgroundTask, BACKGROUND_UPDATE_DELAY_MS);
    }

    public void startBackgroundTimer(String bgImageUrl) {
        if (bgImageUrl != null) {
            startBackgroundTimer(Uri.parse(bgImageUrl));
        }
    }

    /**
     * MOD: ambient backdrop. A blurred, darkened copy of the focused thumbnail fades in behind the content.
     */
    public void setBackgroundFrom(Video item) {
        // The player keeps its plain background
        if (item == null || item.cardImageUrl == null || mActivity instanceof PlaybackActivity) {
            return;
        }

        if (item.cardImageUrl.equals(mAmbientUrl)) {
            return;
        }

        mAmbientUrl = item.cardImageUrl;
        mHandler.removeCallbacks(mAmbientTask);
        mHandler.postDelayed(mAmbientTask, AMBIENT_UPDATE_DELAY_MS);
    }

    private void loadAmbientBackground(String url) {
        if (mBackgroundManager == null || !Utils.checkActivity(mActivity)) {
            return;
        }

        Glide.with(mActivity)
                .asBitmap()
                .load(url)
                .apply(ViewUtil.glideOptions().centerCrop())
                .into(new SimpleTarget<Bitmap>(AMBIENT_WIDTH, AMBIENT_HEIGHT) {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                        // Skip if focus already moved on
                        if (mBackgroundManager == null || !url.equals(mAmbientUrl)) {
                            return;
                        }

                        Bitmap blurred = AmbientBlur.blur(resource, AMBIENT_BLUR_RADIUS);
                        BitmapDrawable image = new BitmapDrawable(mActivity.getResources(), blurred);
                        image.setFilterBitmap(true); // smooth upscaling of the small blurred image
                        mAmbientDrawable = new LayerDrawable(new Drawable[] {
                                image, new ColorDrawable(ContextCompat.getColor(mActivity, R.color.ambient_scrim))
                        });
                        mBackgroundManager.setDrawable(mAmbientDrawable);
                    }
                });
    }

    public void onStart() {
        if (mAmbientDrawable != null) {
            mBackgroundManager.setDrawable(mAmbientDrawable);
        } else if (mBackgroundURI != null) {
            showBackground(mBackgroundURI.toString());
        } else if (mBackgroundColor != -1) {
            setBackgroundColor(mBackgroundColor);
        } else {
            setDefaultBackground();
        }
    }

    public void onStop() {
        mBackgroundManager.release();
    }

    public void onDestroy() {
        mHandler.removeCallbacks(mBackgroundTask);
        mHandler.removeCallbacks(mAmbientTask);
        mBackgroundManager = null;
    }

    public void removeBackground() {
        mBackgroundManager.setDrawable(null);
    }

    public void setDefaultBackground() {
        mBackgroundManager.setDrawable(mDefaultBackground);
    }

    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        mBackgroundManager.setColor(color);
    }

    private class UpdateBackgroundTask implements Runnable {
        @Override
        public void run() {
            if (mBackgroundURI != null) {
                showBackground(mBackgroundURI.toString());
            }
        }
    }

    public BackgroundManager getBackgroundManager() {
        return mBackgroundManager;
    }

    public void showBackground(String uri) {
        View videoView = mActivity.findViewById(R.id.video_surface);

        if (videoView != null) {
            videoView.setVisibility(uri == null ? View.VISIBLE : View.INVISIBLE);
        }

        if (uri == null) {
            removeBackground();
            return;
        }

        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;

        RequestOptions options = ViewUtil.glideOptions()
                .centerCrop()
                .error(mDefaultBackground);

        Glide.with(mActivity)
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(
                            @NonNull Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }
                });
    }

    public void showBackgroundColor(int colorResId) {
        View videoView = mActivity.findViewById(R.id.video_surface);

        if (videoView != null) {
            videoView.setVisibility(colorResId == -1 ? View.VISIBLE : View.INVISIBLE);
        }

        if (colorResId == -1) {
            removeBackground();
            return;
        }

        if (mBackgroundManager != null) {
            mBackgroundManager.setColor(ContextCompat.getColor(mActivity, colorResId));
        }
    }
}
