package com.example.zhangchao.scphotopro;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.scphoto.SCPhotoActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openPhoto(View view) {
        Intent intent = new Intent(MainActivity.this, SCPhotoActivity.class);
        startActivity(intent);
    }
}
