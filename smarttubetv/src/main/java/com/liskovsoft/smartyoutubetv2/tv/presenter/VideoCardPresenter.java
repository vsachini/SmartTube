package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.ClickbaitRemover;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.presenter.base.LongClickPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.video.GridFragmentHelper;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.complexcardview.ComplexImageCardView;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class VideoCardPresenter extends LongClickPresenter {
    private static final String TAG = VideoCardPresenter.class.getSimpleName();
    private int mDefaultTextColor = -1;
    private int mSelectedTextColor = -1;
    private int mContentTextColor = -1;
    private int mCardPreviewType;
    private int mThumbQuality;
    private int mWidth;
    private int mHeight;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();

        mDefaultTextColor =
                ContextCompat.getColor(context, R.color.card_title_text);
        mSelectedTextColor =
                ContextCompat.getColor(context, R.color.card_title_text_selected);
        mContentTextColor =
                ContextCompat.getColor(context, R.color.card_content_text);

        mCardPreviewType = getCardPreviewType(context);
        mThumbQuality = getThumbQuality(context);

        boolean isCardMultilineTitleEnabled = isCardMultilineTitleEnabled(context);
        boolean isCardMultilineSubtitleEnabled = isCardMultilineSubtitleEnabled(context);
        boolean isCardTextAutoScrollEnabled = isCardTextAutoScrollEnabled(context);
        float cardTextScrollSpeed = getCardTextScrollSpeed(context);

        updateDimensions(context);

        ComplexImageCardView cardView = new ComplexImageCardView(context) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };

        cardView.setTitleLinesNum(isCardMultilineTitleEnabled ? 2 : 1);
        cardView.setContentLinesNum(isCardMultilineSubtitleEnabled ? 2 : 1);
        cardView.enableTextAutoScroll(isCardTextAutoScrollEnabled);
        cardView.setTextScrollSpeed(cardTextScrollSpeed);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.enableBadge(isBadgeEnabled());
        cardView.enableTitle(isTitleEnabled());
        cardView.enableContent(isContentEnabled());
        // Text sits under the rounded thumbnail, so the card itself has no background
        cardView.setBackgroundColor(Color.TRANSPARENT);
        alignInfoFieldWithImage(cardView);
        //if (VERSION.SDK_INT >= 23 && MainUIData.instance(context).isUiTweakEnabled(MainUIData.UI_TWEAK_ROUNDED_CORNERS)) {
        //    cardView.setForeground(ContextCompat.getDrawable(context, R.drawable.lb_card_outline));
        //}
        updateCardBackgroundColor(cardView, false);
        return new ViewHolder(cardView);
    }

    private void updateCardBackgroundColor(ComplexImageCardView view, boolean selected) {
        View infoField = view.findViewById(R.id.info_field);
        if (infoField != null) {
            infoField.setBackgroundColor(Color.TRANSPARENT);
        }

        TextView titleText = view.findViewById(R.id.title_text);
        if (titleText != null) {
            titleText.setTextColor(selected ? mSelectedTextColor : mDefaultTextColor);
        }
        TextView contentText = view.findViewById(R.id.content_text);
        if (contentText != null) {
            contentText.setTextColor(mContentTextColor);
        }
    }

    /**
     * Removes horizontal padding so the text lines up with the thumbnail edge
     */
    private void alignInfoFieldWithImage(ComplexImageCardView view) {
        View infoField = view.findViewById(R.id.info_field);
        if (infoField != null) {
            infoField.setPaddingRelative(0, infoField.getPaddingTop(), 0, infoField.getPaddingBottom());
        }
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        super.onBindViewHolder(viewHolder, item);

        Video video = (Video) item;

        ComplexImageCardView cardView = (ComplexImageCardView) viewHolder.view;
        Context context = cardView.getContext();

        cardView.setTitleText(video.getTitle());
        cardView.setContentText(video.getSecondTitle());
        // Count progress that very close to zero. E.g. when user closed video immediately.
        cardView.setProgress(video.percentWatched > 0 && video.percentWatched < 1 ? 1 : Math.round(video.percentWatched));
        cardView.setBadgeText(
                video.hasNewContent ? context.getString(R.string.badge_new_content) :
                video.isLive ? context.getString(R.string.badge_live) :
                video.isShorts ? context.getString(R.string.header_shorts).toUpperCase() :
                video.badge
        );
        cardView.setBadgeColor(video.hasNewContent || video.isLive || video.isUpcoming ?
                ContextCompat.getColor(context, R.color.dark_red) : ContextCompat.getColor(context, R.color.card_badge_background));

        if (mCardPreviewType != MainUIData.CARD_PREVIEW_DISABLED) {
            cardView.setPreview(video);
            cardView.setMute(mCardPreviewType == MainUIData.CARD_PREVIEW_MUTED);
        }

        cardView.setMainImageDimensions(mWidth, mHeight);
        cardView.setGlowColor(Color.WHITE); // until the thumbnail is loaded

        if (context instanceof Activity && ((Activity) context).isDestroyed()) {
            // Glide.with(context): IllegalArgumentException: You cannot start a load for a destroyed activity
            return;
        }

        Glide.with(context)
                //.asBitmap() // disable animation (webp, gif)
                .load(ClickbaitRemover.updateThumbnail(video, mThumbQuality))
                //.placeholder(mDefaultCardImage)
                .apply(ViewUtil.glideOptions())
                // improve image compression on low end devices
                .override(mWidth, mHeight)
                // com.liskovsoft.smartyoutubetv2.tv.util.CacheGlideModule
                // Cache makes app crashing on old android versions
                .diskCacheStrategy(VERSION.SDK_INT > 21 ? DiskCacheStrategy.ALL : DiskCacheStrategy.NONE)
                .listener(mErrorListener)
                .addListener(new GlowColorListener(cardView))
                .error(
                    // Updated thumbnail url not found
                    Glide.with(context)
                        .load(video.cardImageUrl) // always working
                        //.placeholder(mDefaultCardImage)
                        .apply(ViewUtil.glideOptions())
                        .listener(mErrorListener)
                        .error(R.drawable.card_placeholder) // R.color.lb_grey
                )
                .into(cardView.getMainImageView());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        super.onUnbindViewHolder(viewHolder);

        ComplexImageCardView cardView = (ComplexImageCardView) viewHolder.view;

        // Remove references to images so that the garbage collector can free up memory.
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);

        // Cleanup Glide resources. https://chatgpt.com/share/682120c5-e428-8010-b848-371b2dec0cd5
        Glide.with(cardView.getContext().getApplicationContext()).clear(cardView.getMainImageView());
    }

    private void updateDimensions(Context context) {
        Pair<Integer, Integer> dimens = getCardDimensPx(context);

        mWidth = dimens.first;
        mHeight = dimens.second;
    }
    
    protected Pair<Integer, Integer> getCardDimensPx(Context context) {
        return GridFragmentHelper.getCardDimensPx(context, R.dimen.card_width, R.dimen.card_height, MainUIData.instance(context).getVideoGridScale());
    }

    protected boolean isCardTextAutoScrollEnabled(Context context) {
        return MainUIData.instance(context).isCardTextAutoScrollEnabled();
    }

    protected int getCardPreviewType(Context context) {
        return MainUIData.instance(context).getCardPreviewType();
    }

    protected boolean isCardMultilineTitleEnabled(Context context) {
        return MainUIData.instance(context).isCardMultilineTitleEnabled();
    }

    protected boolean isCardMultilineSubtitleEnabled(Context context) {
        return MainUIData.instance(context).isCardMultilineSubtitleEnabled();
    }

    protected float getCardTextScrollSpeed(Context context) {
        return MainUIData.instance(context).getCardTextScrollSpeed();
    }

    protected int getThumbQuality(Context context) {
        return MainUIData.instance(context).getThumbQuality();
    }

    protected boolean isContentEnabled() {
        return true;
    }

    protected boolean isTitleEnabled() {
        return true;
    }

    protected boolean isBadgeEnabled() {
        return true;
    }

    private final RequestListener<Drawable> mErrorListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            Log.e(TAG, "Glide load failed: " + e);
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };

    /**
     * Picks the focus glow colour from the loaded thumbnail
     */
    private static class GlowColorListener implements RequestListener<Drawable> {
        private final ComplexImageCardView mCardView;

        GlowColorListener(ComplexImageCardView cardView) {
            mCardView = cardView;
        }

        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            if (resource instanceof BitmapDrawable) {
                mCardView.setGlowColor(getGlowColor(((BitmapDrawable) resource).getBitmap()));
            }
            return false;
        }
    }

    /**
     * Average colour of the image, made vivid and bright enough to read as light
     */
    static int getGlowColor(Bitmap bitmap) {
        try {
            if (VERSION.SDK_INT >= 26 && bitmap.getConfig() == Bitmap.Config.HARDWARE) {
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
            }

            int color = Bitmap.createScaledBitmap(bitmap, 1, 1, true).getPixel(0, 0);
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[1] = Math.max(hsv[1], 0.55f);
            hsv[2] = Math.max(hsv[2], 0.9f);
            return Color.HSVToColor(hsv);
        } catch (RuntimeException e) {
            return Color.WHITE;
        }
    }

    private final RequestListener<Bitmap> mErrorListener2 = new RequestListener<Bitmap>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
            Log.e(TAG, "Glide load failed: " + e);
            return false;
        }

        @Override
        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };
}
