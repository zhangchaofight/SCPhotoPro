package com.example.scphoto;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

@SuppressWarnings("unused")
public class SCControlView extends View {

    private static final int DEFAULT_DENY = 300;
    private static final int DEFAULT_MAX_PRESS_TIME = 5 * 1000;

    private int clickDeny = DEFAULT_DENY;
    private int maxPressTime = DEFAULT_MAX_PRESS_TIME;

    private Handler mainHandler;
    private long startTime = 0;
    private boolean isClick = false;
    private boolean isCanceled = false;

    private SCControlViewTouchListener scControlViewTouchListener;

    public SCControlView(Context context) {
        super(context);
        init();
    }

    public SCControlView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SCControlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mainHandler = new Handler(getContext().getMainLooper());
        setClickable(false);
    }

    public void setClickDeny(int clickDeny) {
        this.clickDeny = clickDeny;
    }

    public void setMaxPressTime(int maxPressTime) {
        this.maxPressTime = maxPressTime;
    }

    public void setScControlViewTouchListener(SCControlViewTouchListener scControlViewTouchListener) {
        this.scControlViewTouchListener = scControlViewTouchListener;
    }

    public void removeScControlViewTouchListener() {
        this.scControlViewTouchListener = null;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public void setClickable(boolean clickable) {

    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {

    }

    @Override
    public void setOnLongClickListener(@Nullable OnLongClickListener l) {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startTime = System.currentTimeMillis();
                isClick = false;
                isCanceled = false;
                mainHandler.postDelayed(new LongClickRunnable(), clickDeny);
                mainHandler.postDelayed(new CancelRunnable(), clickDeny + maxPressTime);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (System.currentTimeMillis() - startTime < clickDeny) {
                    onClick();
                    isClick = true;
                } else {
                    mainHandler.post(new CancelRunnable());
                }
                performClick();
                break;
        }
        return true;
    }

    private class LongClickRunnable implements Runnable {

        @Override
        public void run() {
            if (!isClick) {
                startLongClick();
            }
        }
    }

    private class CancelRunnable implements Runnable {

        @Override
        public void run() {
            if (!isCanceled) {
                cancelLongClick();
                isCanceled = true;
            }
        }
    }

    private void onClick() {
        if (scControlViewTouchListener != null) {
            scControlViewTouchListener.click();
        }
    }

    private void startLongClick() {
        if (scControlViewTouchListener != null) {
            scControlViewTouchListener.startLongPress();
        }
    }

    private void cancelLongClick() {
        if (scControlViewTouchListener != null) {
            scControlViewTouchListener.cancelLongPress();
        }
    }

    public interface SCControlViewTouchListener{

        void click();

        void startLongPress();

        void cancelLongPress();
    }
}
