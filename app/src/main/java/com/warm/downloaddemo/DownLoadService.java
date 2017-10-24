package com.warm.downloaddemo;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 作者: 51hs_android
 * 时间: 2017/5/25
 * 简介:
 */

public class DownLoadService extends Service  {

    private static final String TAG = "DownLoadService--";

    public static final String URI = "uri";
    public static final String APK_NAME = "apkName";
    public static final String PARENT_DIR = "parent";
    public static final String NOTIFI_TITLE = "notifiTitle";
    public static final String NOTIFI_DESCRIPTION = "notifiDescription";
    public static final String NET_TYPE = "netType";


    public long downloadId;

    private DownloadManager manager;

    private String notifiTitle;
    private String notifiDescription;
    private Uri downUri;
    private String parentDir;
    private String apkName;
    private int netType;
    private Handler mHandler=new Handler(Looper.getMainLooper());

    private SuccessReceiver mReceiver;


    //    private Timer mTimer;
    private ScheduledExecutorService mService;

    private OnProgressListener mListener;

    private DownLoadContentObserver mObserver;




    public void setOnProgressListener(OnProgressListener mListener) {
        this.mListener = mListener;
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        initData(intent);

        checkOrDownload();
        return super.onStartCommand(intent, flags, startId);
    }

    private void initData(Intent intent) {
        notifiTitle = intent.getStringExtra(NOTIFI_TITLE);
        notifiDescription = intent.getStringExtra(NOTIFI_DESCRIPTION);
        downUri = intent.getParcelableExtra(URI);
        apkName = intent.getStringExtra(APK_NAME);
        parentDir = intent.getStringExtra(PARENT_DIR);
        netType = intent.getIntExtra(NET_TYPE, DownloadManager.Request.NETWORK_WIFI);
        manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
    }

    private void checkOrDownload() {

//        File file = getApkFile();
//        if (file.exists()) {
//            installApkByFile(file);
//            stopSelf();
//        } else {
        if (isDownloadManagerAvailable()) {
            initDownLoadManager();
        } else {
            Intent chrome = new Intent(Intent.ACTION_VIEW, downUri);
            chrome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(chrome);
            stopSelf();
        }
//        }

    }


    private void initDownLoadManager() {

        mObserver=new DownLoadContentObserver(mHandler);
        mObserver.registerContentObserver();

        //初始化DownloadManager，传入下载Uri
        DownloadManager.Request request = new DownloadManager.Request(downUri);

        //设置是否显示Notification和相关信息
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);


//        request.setAllowedNetworkTypes(netType);

        //设置Notification标题
        if (notifiTitle != null)
            request.setTitle(notifiTitle);

        //设置Notification描述
        if (notifiDescription != null)
            request.setDescription(notifiTitle);

        request.setVisibleInDownloadsUi(true);


        if (parentDir == null) {
            /**
             *传入文件夹类型，系统类型
             * The type of files directory to return. May be {@code null}
             *            for the root of the files directory or one of the following
             *            constants for a subdirectory:
             *            {@link Environment#DIRECTORY_MUSIC},
             *            {@link Environment#DIRECTORY_PODCASTS},
             *            {@link Environment#DIRECTORY_RINGTONES},
             *            {@link Environment#DIRECTORY_ALARMS},
             *            {@link Environment#DIRECTORY_NOTIFICATIONS},
             *            {@link Environment#DIRECTORY_PICTURES}, or
             *            {@link Environment#DIRECTORY_MOVIES}.
             */
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, apkName);
        } else {
            //只需要传入文件夹名称,和文件名称,可以放置到外部任意文件夹
            request.setDestinationUri(Uri.fromFile(new File(parentDir, apkName)));
        }

//        request.setDestinationUri()

        request.allowScanningByMediaScanner();

        downloadId = manager.enqueue(request);

        mService = Executors.newSingleThreadScheduledExecutor();


        mReceiver = new SuccessReceiver();

        registerReceiver(mReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

    }
    /**
     * 检查一遍
     */
    private void updateProgress() {
        final int[] progress = getBytesAndStatus(downloadId);
//        mRunnable.setProgress();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener!=null){
                    mListener.onProgress(progress[0],progress[1],progress[2]);
                }
            }
        });

    }


    /**
     * 返回当前下载进度
     *
     * @param downloadId
     * @return {已下载，总和，下载进度}
     */
    public int[] getBytesAndStatus(long downloadId) {
        int[] bytesAndStatus = new int[]{0, 100, 0};
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        Cursor cursor = null;
        try {
            cursor = manager.query(query);
            if (cursor != null && cursor.moveToFirst()) {

                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                switch (status) {
                    //下载暂停
                    case DownloadManager.STATUS_PAUSED:
                        cancel();
                        break;
                    //下载延迟
                    case DownloadManager.STATUS_PENDING:

                        break;
                    //正在下载
                    case DownloadManager.STATUS_RUNNING:
                        bytesAndStatus[0] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        bytesAndStatus[1] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        break;
                    //下载完成
                    case DownloadManager.STATUS_SUCCESSFUL:
                        cancel();
                        //下载完成安装APK
                        installApkById(downloadId);
                        stopSelf();
                        break;
                    //下载失败
                    case DownloadManager.STATUS_FAILED:
                        cancel();
                        break;
                }
                bytesAndStatus[2] = status;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }


        return bytesAndStatus;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        initData(intent);

        checkOrDownload();

        return new DownLoadBinder();
    }




    /**
     * 根据DownLoadId 获取Uri
     *
     * @param downloadApkId
     * @return
     */
    private Uri getUriByDownLoadId(long downloadApkId) {
        Uri downloadFileUri = manager.getUriForDownloadedFile(downloadApkId);

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadApkId);
        Cursor c = manager.query(query);
        if (c != null && c.moveToFirst()) {
            downloadFileUri = Uri.parse(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
            c.close();
        }
        return downloadFileUri;
    }

    /**
     * 安装软件
     */
    private void installApkById(long downloadApkId) {
        Uri downloadFileUri = getUriByDownLoadId(downloadApkId);
        install(downloadFileUri);
    }

    private File getApkFile() {
        File file;
        if (parentDir == null) {

            file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), apkName);
        } else {
            file = new File(parentDir, apkName);
        }
        return file;
    }


    private void installApkByFile(File apkFile) {
        Uri apkFileUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            apkFileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", apkFile);
        } else {
            apkFileUri = Uri.fromFile(apkFile);
        }
        install(apkFileUri);
    }

    private void install(Uri apkFileUri) {
        if (apkFileUri != null) {
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkFileUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
        } else {

        }

    }


    private boolean isDownloadManagerAvailable() {
        try {
            if (getPackageManager().getApplicationEnabledSetting("com.android.providers.downloads") == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    || getPackageManager().getApplicationEnabledSetting("com.android.providers.downloads") == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    || getPackageManager().getApplicationEnabledSetting("com.android.providers.downloads") == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mObserver!=null) {
            mObserver.unregisterContentObserver();
        }
        if (mHandler!=null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        cancel();

    }

    class DownLoadBinder extends Binder {

        DownLoadService getDownLoadService() {
            return DownLoadService.this;
        }

    }



    private void schedule() {
        if (mService != null) {

            mService.scheduleWithFixedDelay(new TimerTask() {
                @Override
                public void run() {
                    updateProgress();
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
    }

    private void cancel() {
        if (mService != null) {
            mService.shutdown();
        }
    }



    public class SuccessReceiver extends BroadcastReceiver {
        private static final String TAG = "SuccessReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            long completeDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            Log.d(TAG, "onReceive: ");
            updateProgress();
        }

    }

    private class DownLoadContentObserver extends ContentObserver {
        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        public DownLoadContentObserver(Handler handler) {
            super(handler);
        }

        public void registerContentObserver(){
            getContentResolver().registerContentObserver(Uri.parse("content://downloads/my_downloads/"),false,this);
        }

        public void unregisterContentObserver(){
            getContentResolver().unregisterContentObserver(this);


        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            schedule();
        }
    }


    /**
     * 监听下载进度的回调
     */
    public interface OnProgressListener {
        /**
         * @param downed   已下载
         * @param total    总和
         * @param state 下载进度
         */
        void onProgress(int downed, int total, int state);

    }


}
