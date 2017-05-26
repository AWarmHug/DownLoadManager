package com.warm.downloaddemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                down();
            }
        });

    }

    private void down(){
        Intent intent=new Intent(this,DownLoadService.class);
        intent.putExtra(DownLoadService.PATH,"http://imtt.dd.qq.com/16891/CCE1007FAC2B71E4B61CEFDEB8AF1ECA.apk?fsname=flipboard.cn_3.5.7.0_289305.apk&csr=1bbd");
        startService(intent);
    }
}
