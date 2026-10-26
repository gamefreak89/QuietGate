package org.open.quietgate;

import android.view.View;
import android.view.WindowInsets;

/** Keep content and controls clear of system bars, cutouts, and gesture navigation. */
final class UiInsets {
    private UiInsets() {}
    static void apply(View root) {
        final int left=root.getPaddingLeft(), top=root.getPaddingTop();
        final int right=root.getPaddingRight(), bottom=root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(left+bars.left, top+bars.top, right+bars.right, bottom+bars.bottom);
            } else {
                view.setPadding(left+insets.getSystemWindowInsetLeft(), top+insets.getSystemWindowInsetTop(),
                    right+insets.getSystemWindowInsetRight(), bottom+insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        root.requestApplyInsets();
    }
}
