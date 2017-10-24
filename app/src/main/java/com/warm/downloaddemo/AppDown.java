package com.warm.downloaddemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;

/**
 * 作者: 51hs_android
 * 时间: 2017/5/26
 * 简介:
 */

public class AppDown {
    private Context mContext;
    private String notifiTitle;
    private String notifiDescription;
    private Uri downUri;
    private String parentDir;
    private String apkName;

    private DownLoadService.OnProgressListener mPListener;

    /**
     * {@link android.app.DownloadManager.Request#NETWORK_WIFI
     *
     * @link android.app.DownloadManager.Request#NETWORK_MOBILE}
     */
    private int netType;

    private AppDown(Builder builder) {
        this.mContext = builder.context;
        this.notifiTitle = builder.notifiTitle;
        this.notifiDescription = builder.notifiDescription;
        this.downUri = builder.downUri;
        this.parentDir = builder.parentDir;
        this.apkName = builder.apkName;
        this.netType = builder.netType;
        this.mPListener = builder.listener;

    }


    public void start() {
        Intent intent = new Intent(mContext, DownLoadService.class);
        intent.putExtra(DownLoadService.NOTIFI_TITLE, notifiTitle);
        intent.putExtra(DownLoadService.NOTIFI_DESCRIPTION, notifiDescription);
        intent.putExtra(DownLoadService.URI, downUri);
        intent.putExtra(DownLoadService.PARENT_DIR, parentDir);
        intent.putExtra(DownLoadService.APK_NAME, apkName);
        intent.putExtra(DownLoadService.NET_TYPE, netType);
        if (mPListener == null) {
            mContext.startService(intent);
        } else {
            mContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    public void end(){
        if (mPListener != null) {
            mContext.unbindService(connection);
        }
    }





    public static class Builder {
        private String notifiTitle;
        private String notifiDescription;
        private Uri downUri;
        private String parentDir;
        private String apkName;
        private int netType;
        private Context context;
        private DownLoadService.OnProgressListener listener;


        public Builder(Context context) {
            this.context = context;
        }


        public Builder setNotifiTitle(String notifiTitle) {
            this.notifiTitle = notifiTitle;
            return this;
        }

        public Builder setNotifiDescription(String notifiDescription) {
            this.notifiDescription = notifiDescription;
            return this;
        }

        public Builder setDownPath(String path) {
            this.downUri = Uri.parse(path);
            return this;
        }

        public Builder setDownUri(Uri downUri) {
            this.downUri = downUri;
            return this;
        }

        public Builder setParentDir(String parentDir) {
            this.parentDir = parentDir;
            return this;
        }


        public Builder setApkName(String apkName) {
            this.apkName = apkName;
            return this;

        }

        public Builder setNetType(int netType) {
            this.netType = netType;
            return this;
        }

        public Builder setOnProgressListener(DownLoadService.OnProgressListener listener) {
            this.listener = listener;
            return this;
        }

        public AppDown build() {
            return new AppDown(this);
        }
    }


    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DownLoadService downLoadService = ((DownLoadService.DownLoadBinder) service).getDownLoadService();
            downLoadService.setOnProgressListener(mPListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

}
