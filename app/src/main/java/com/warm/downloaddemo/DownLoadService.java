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

/**
 * 作者: 51hs_android
 * 时间: 2017/5/25
 * 简介:
 */

public class DownLoadService extends Service {

    public static final String URI = "uri";
    public static final String APK_NAME="apkName";
    public static final String PARENT_DIR="parent";
    public static final String NOTIFI_TITLE="notifiTitle";
    public static final String NOTIFI_DESCRIPTION="notifiDescription";
    public static final String NET_TYPE="netType";


    public static final long DOWN_ID = 1001;

    private DownloadManager manager;

    private String notifiTitle;
    private String notifiDescription;
    private Uri downUri;
    private String parentDir;
    private String apkName;
    private int netType;

    private SuccessReceiver receiver;


    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        notifiTitle=intent.getStringExtra(NOTIFI_TITLE);
        notifiDescription=intent.getStringExtra(NOTIFI_DESCRIPTION);
        downUri=intent.getParcelableExtra(URI);
        apkName=intent.getStringExtra(APK_NAME);
        parentDir=intent.getStringExtra(PARENT_DIR);
        netType=intent.getIntExtra(NET_TYPE, DownloadManager.Request.NETWORK_WIFI);
        manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        //初始化DownloadManager，传入下载Uri
        DownloadManager.Request request = new DownloadManager.Request(downUri);

        //设置是否显示Notification和相关信息
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);


//        request.setAllowedNetworkTypes(netType);

        //设置Notification标题
        if (notifiTitle!=null)
        request.setTitle(notifiTitle);

        //设置Notification描述
        if (notifiDescription!=null)
        request.setDescription(notifiTitle);

        request.setVisibleInDownloadsUi(true);


        if (parentDir==null) {
            /**
             *传入文件夹类型，系统类型
             * The type of files directory to return. May be {@code null}
             *            for the root of the files directory or one of the following
             *            constants for a subdirectory:
             *            {@link android.os.Environment#DIRECTORY_MUSIC},
             *            {@link android.os.Environment#DIRECTORY_PODCASTS},
             *            {@link android.os.Environment#DIRECTORY_RINGTONES},
             *            {@link android.os.Environment#DIRECTORY_ALARMS},
             *            {@link android.os.Environment#DIRECTORY_NOTIFICATIONS},
             *            {@link android.os.Environment#DIRECTORY_PICTURES}, or
             *            {@link android.os.Environment#DIRECTORY_MOVIES}.
             */
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS,apkName);
        }else {
            //只需要传入文件夹名称,和文件名称,可以放置到外部任意文件夹
            request.setDestinationInExternalPublicDir(parentDir, apkName);
        }

//        request.setDestinationUri()

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
        Cursor cursor = manager.query(query);
        if (cursor.moveToFirst()) {
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
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
                    stopSelf();
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
