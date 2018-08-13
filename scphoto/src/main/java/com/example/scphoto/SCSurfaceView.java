package com.example.scphoto;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class SCSurfaceView extends TextureView {

    public SCSurfaceView(Context context) {
        super(context);
    }

    public SCSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SCSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void resize(int width, int height) {

    }
}
