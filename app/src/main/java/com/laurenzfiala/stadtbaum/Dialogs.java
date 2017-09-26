package com.laurenzfiala.stadtbaum;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.lang.annotation.Target;

/**
 * Created by Laurenz Fiala on 20/09/2017.
 * Provides functionality for showing errors and checking permissions.
 */
public class Dialogs {

    /**
     * Shows the permission request explanation and lets the user choose between closing the app
     * and allowing the permissions.
     * @param context The calling activity.
     */
    @TargetApi(23)
    public static void permissionsDialog(final Activity context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false)
                .setMessage(R.string.permreq_explanation)
                .setPositiveButton(R.string.permreq_btn_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        context.requestPermissions(IntroActivity.NEEDED_PERMISSIONS, IntroActivity.REQUEST_PERMISSIONS);
                    }
                })
                .setNegativeButton(R.string.close_app, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        context.finish();
                    }
                });
        builder.create().show();
    }

    /**
     * Prompt to show to the user when a critical error occurs.
     * @param context calling activity
     * @param message Message to show to the user.
     */
    public static void errorPrompt(final Activity context, final String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false)
                .setMessage(message)
                .setNegativeButton(R.string.close_app, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        context.finish();
                    }
                });
        builder.create().show();
    }

    /**
     * Shows a dialog regarding internet connectivity to the user. The caller can decide what
     * happens when the user wants to retry; if he doesn't, he may close the app.
     * @param context calling activity
     */
    public static void noInternetDialog(final MainActivity context, final DialogInterface.OnClickListener onPositiveClick) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false)
                .setMessage(R.string.error_no_internet)
                .setPositiveButton(R.string.retry, onPositiveClick)
                .setNegativeButton(R.string.close_app, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        context.finish();
                    }
                });
        builder.create().show();
    }

}
