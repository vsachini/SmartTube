package com.liskovsoft.smartyoutubetv2.tv.ui.browse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import androidx.fragment.app.Fragment;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.LeanbackActivity;

public class BrowseActivity extends LeanbackActivity {
    private static final String TAG = BrowseActivity.class.getSimpleName();
    private static final long INTRO_DURATION_MS = 1_100;
    private static final long INTRO_MAX_DURATION_MS = 5_000;
    private static boolean sIntroShown;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.fragment_main);
        } catch (NoClassDefFoundError e) {
            // Failed resolution of: Landroidx/lifecycle/ViewTreeLifecycleOwner;
            MessageHelpers.showMessage(this, e.getMessage());
        }

        if (savedInstanceState == null && !sIntroShown) {
            sIntroShown = true;
            showIntro();
        }
    }

    /**
     * MOD: Lumen intro. The mark pops in, the sparkle twinkles, then the overlay fades into Home.
     */
    private void showIntro() {
        ViewGroup content = findViewById(android.R.id.content);

        if (content == null) {
            return;
        }

        View overlay = LayoutInflater.from(this).inflate(R.layout.intro_overlay, content, false);
        content.addView(overlay);

        View mark = overlay.findViewById(R.id.intro_mark_frame);
        View sparkle = overlay.findViewById(R.id.intro_sparkle);
        View title = overlay.findViewById(R.id.intro_title);

        mark.setScaleX(0.6f);
        mark.setScaleY(0.6f);
        mark.setAlpha(0f);
        mark.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(450)
                .setInterpolator(new OvershootInterpolator(1.4f)).start();

        sparkle.setScaleX(0f);
        sparkle.setScaleY(0f);
        sparkle.animate().scaleX(1.3f).scaleY(1.3f).rotation(90f).setStartDelay(350).setDuration(350)
                .withEndAction(() -> sparkle.animate().scaleX(1f).scaleY(1f).setDuration(200).start()).start();

        title.setAlpha(0f);
        title.setTranslationY(20f);
        title.animate().alpha(1f).translationY(0f).setStartDelay(250).setDuration(400).start();

        // Covers the first load: fades out once Home has rows (within a min/max time)
        long startMs = System.currentTimeMillis();
        overlay.postDelayed(new Runnable() {
            @Override
            public void run() {
                long elapsedMs = System.currentTimeMillis() - startMs;

                if (elapsedMs < INTRO_MAX_DURATION_MS && !isHomeLoaded()) {
                    overlay.postDelayed(this, 200);
                    return;
                }

                overlay.animate().alpha(0f).setDuration(400)
                        .withEndAction(() -> content.removeView(overlay)).start();
            }
        }, INTRO_DURATION_MS);
    }

    private boolean isHomeLoaded() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_frame);
        return fragment instanceof BrowseFragment && !((BrowseFragment) fragment).isEmpty();
    }

    @Override
    protected void initTheme() {
        int browseThemeResId = MainUIData.instance(this).getColorScheme().browseThemeResId;
        if (browseThemeResId > 0) {
            setTheme(browseThemeResId);
        }
    }
}
