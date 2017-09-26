package com.laurenzfiala.stadtbaum;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.view.ViewTreeObserver;

/**
 * Created by Laurenz Fiala on 20/09/2017.
 * Contains various utility methods for easy use.
 */
public class Utils {

    /**
     * Checks whether a network connection is available.
     * @return
     */
    public static boolean isNetworkAvailable(final Activity context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Makes a {@link Snackbar} undismissable, which means it can't be dismissed by swiping it away.
     */
    public static void snackbarUndismissable(final Snackbar searchInfo) {
        searchInfo.getView().getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                searchInfo.getView().getViewTreeObserver().removeOnPreDrawListener(this);
                ((CoordinatorLayout.LayoutParams) searchInfo.getView().getLayoutParams()).setBehavior(null);
                return true;
            }
        });
    }
}
