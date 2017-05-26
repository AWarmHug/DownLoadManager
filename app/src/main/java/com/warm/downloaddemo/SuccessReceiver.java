package com.warm.downloaddemo;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 作者: 51hs_android
 * 时间: 2017/5/25
 * 简介:
 */

public class SuccessReceiver extends BroadcastReceiver {
    private static final String TAG = "SuccessReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        long completeDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        Log.d(TAG, "onReceive: ");


    }

}
