package com.warm.downloaddemo;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;

/**
 * 作者: 51hs_android
 * 时间: 2017/5/25
 * 简介:
 */

public class DownLoadService extends Service {

    public static final String PATH = "path";

    public static final long DOWN_ID = 1001;

    private DownloadManager manager;

    private String filePath = Environment.getExternalStorageDirectory().getAbsolutePath();

    private String fileName = "cc.apk";

    private SuccessReceiver receiver;


    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Uri uri = Uri.parse(intent.getStringExtra(PATH));
        manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        //设置是否显示Notification和相关信息
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle(getString(R.string.app_name));
        request.setDescription("正在下载！");
        request.setVisibleInDownloadsUi(true);


        request.setDestinationInExternalPublicDir(getPackageName() + "/apk", fileName);
        request.allowScanningByMediaScanner();
        manager.enqueue(request);

        receiver = new SuccessReceiver();

        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 返回当前下载进度
     *
     * @param downloadId
     * @return {已下载，总和，下载进度}
     */
    public int[] getBytesAndStatus(long downloadId) {
        int[] bytesAndStatus = new int[]{-1, -1, 0};
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor c = null;
        try {
            c = manager.query(query);
            if (c != null && c.moveToFirst()) {
                bytesAndStatus[0] = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                bytesAndStatus[1] = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                bytesAndStatus[2] = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return bytesAndStatus;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public class SuccessReceiver extends BroadcastReceiver {
        private static final String TAG = "SuccessReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            long completeDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            Log.d(TAG, "onReceive: ");
            checkStatus(completeDownloadId);
        }

    }


    //检查下载状态
    private void checkStatus(long downloadId) {
        DownloadManager.Query query = new DownloadManager.Query();
        //通过下载的id查找
        query.setFilterById(downloadId);
        Cursor c = manager.query(query);
        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            switch (status) {
                //下载暂停
                case DownloadManager.STATUS_PAUSED:
                    break;
                //下载延迟
                case DownloadManager.STATUS_PENDING:
                    break;
                //正在下载
                case DownloadManager.STATUS_RUNNING:
                    break;
                //下载完成
                case DownloadManager.STATUS_SUCCESSFUL:
                    //下载完成安装APK
                    installApk(downloadId);
                    break;
                //下载失败
                case DownloadManager.STATUS_FAILED:

                    break;
            }
        }
    }

    /**
     * 安装软件
     */
    private void installApk(long downloadApkId) {
        Intent install = new Intent(Intent.ACTION_VIEW);
        Uri downloadFileUri = null;
        downloadFileUri = manager.getUriForDownloadedFile(downloadApkId);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadApkId);
        final Cursor c = manager.query(query);

        if (c.moveToFirst()) {
            downloadFileUri = Uri.parse(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
        }
        if (downloadFileUri != null) {
            install.setDataAndType(downloadFileUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
        } else {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);

    }
}
