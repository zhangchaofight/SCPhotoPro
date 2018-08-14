package com.example.zhangchao.scphotopro;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.scphoto.SCPhotoActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File file = new File(getExternalFilesDir(null).getAbsolutePath());
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public void openPhoto(View view) {
        Intent intent = new Intent(MainActivity.this, SCPhotoActivity.class);
        startActivity(intent);
    }
}
