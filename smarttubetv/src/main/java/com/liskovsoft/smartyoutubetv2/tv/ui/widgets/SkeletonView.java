package com.liskovsoft.smartyoutubetv2.tv.ui.widgets;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Placeholder rows of cards with a light sweep, shown while a screen loads its first content
 */
public class SkeletonView extends View {
    private static final int ROWS = 2;
    private static final int SHIMMER_DURATION_MS = 1_400;
    private final Paint mBasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mShimmerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix mShimmerMatrix = new Matrix();
    private final RectF mRect = new RectF();
    private final float mDp;
    private ValueAnimator mAnimator;
    private float mShimmerX;

    public SkeletonView(Context context) {
        super(context);
        mDp = context.getResources().getDisplayMetrics().density;
        mBasePaint.setColor(context.getResources().getColor(R.color.skeleton_base));
        int highlight = context.getResources().getColor(R.color.skeleton_highlight);
        mShimmerPaint.setShader(new LinearGradient(0, 0, 240 * mDp, 0,
                new int[] {0x00FFFFFF & highlight, highlight, 0x00FFFFFF & highlight}, null, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAnimator = ValueAnimator.ofFloat(-240 * mDp, getResources().getDisplayMetrics().widthPixels);
        mAnimator.setDuration(SHIMMER_DURATION_MS);
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.addUpdateListener(animation -> {
            mShimmerX = (float) animation.getAnimatedValue();
            invalidate();
        });
        mAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mShimmerMatrix.setTranslate(mShimmerX, 0);
        mShimmerPaint.getShader().setLocalMatrix(mShimmerMatrix);

        float left = dimen(R.dimen.skeleton_start);
        float top = dimen(R.dimen.skeleton_top);
        float cardWidth = dimen(R.dimen.card_width);
        float cardHeight = dimen(R.dimen.card_height);
        float gap = dimen(R.dimen.grid_item_horizontal_spacing);
        float radius = dimen(R.dimen.card_corner_radius);
        float rowPitch = cardHeight + 120 * mDp;

        for (int row = 0; row < ROWS; row++) {
            float y = top + row * rowPitch;
            // Row title
            drawBlock(canvas, left, y, left + 140 * mDp, y + 14 * mDp, 7 * mDp);

            for (float x = left; x < getWidth(); x += cardWidth + gap) {
                float cardTop = y + 34 * mDp;
                drawBlock(canvas, x, cardTop, x + cardWidth, cardTop + cardHeight, radius);
                // Title and channel lines
                drawBlock(canvas, x, cardTop + cardHeight + 12 * mDp, x + cardWidth * 0.85f, cardTop + cardHeight + 24 * mDp, 6 * mDp);
                drawBlock(canvas, x, cardTop + cardHeight + 32 * mDp, x + cardWidth * 0.5f, cardTop + cardHeight + 42 * mDp, 5 * mDp);
            }
        }
    }

    private void drawBlock(Canvas canvas, float left, float top, float right, float bottom, float radius) {
        mRect.set(left, top, right, bottom);
        canvas.drawRoundRect(mRect, radius, radius, mBasePaint);
        canvas.drawRoundRect(mRect, radius, radius, mShimmerPaint);
    }

    private float dimen(int resId) {
        return getResources().getDimension(resId);
    }
}
