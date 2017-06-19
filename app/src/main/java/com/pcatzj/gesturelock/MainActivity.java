package com.pcatzj.gesturelock;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.pcatzj.gesturelock.view.GestureLockView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, GestureLockView.GestureEvent {

    private GestureLockView mGestureLockView;
    private GestureLockView mGestureLockPreviewView;

    private String mPassword = "";

    private final int authorityTimes = 2;
    private Button mBtnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_create_password).setOnClickListener(this);
        findViewById(R.id.btn_verify_password).setOnClickListener(this);
        mBtnReset = (Button) findViewById(R.id.btn_reset);
        mBtnReset.setOnClickListener(this);
        mGestureLockView = (GestureLockView) findViewById(R.id.view_gesture);
        mGestureLockPreviewView = (GestureLockView) findViewById(R.id.view_gesture_preview);
        mGestureLockPreviewView.setModePreview("");
        mGestureLockView.setGestureEvent(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_create_password:
                createGesturePassword();
                break;
            case R.id.btn_verify_password:
                if (mBtnReset.getVisibility() == View.VISIBLE) {
                    mBtnReset.setVisibility(View.GONE);
                }
                verifyGesturePassword();
                break;
            case R.id.btn_reset:
                mGestureLockView.resetCreatorState();
                mBtnReset.setVisibility(View.GONE);
                mGestureLockPreviewView.setModePreview("");
                break;
        }
    }

    private void createGesturePassword() {
        mGestureLockView.setModeCreator(authorityTimes);
        mGestureLockView.setVisibility(View.VISIBLE);
        mGestureLockPreviewView.setVisibility(View.VISIBLE);
    }

    private void verifyGesturePassword() {
        mGestureLockView.setModeTraverser();
        mGestureLockView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onGestureAuthority(int authority) {
        switch (authority) {
            case AUTHORITY_EXACTLY:
                Toast.makeText(this, "手势密码验证成功", Toast.LENGTH_SHORT).show();
                mGestureLockView.setVisibility(View.GONE);
                break;
            case AUTHORITY_NOT_EXACTLY:
                Toast.makeText(this, "手势密码不正确", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onGestureCreate(int create) {
        switch (create) {
            case CREATE_CHECK_POINT_NOT_ENOUGH:
                Toast.makeText(this, "连接点数过少", Toast.LENGTH_SHORT).show();
                break;
            case CREATE_NOT_SAME_AS_FIRST_TIMES:
                Toast.makeText(this, "手势密码和第一次不同", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onGestureCreateEffective(int leftSteps, String password) {
        if (leftSteps < authorityTimes) {
            if (mBtnReset.getVisibility() != View.VISIBLE) {
                mBtnReset.setVisibility(View.VISIBLE);
            }
            if (leftSteps == authorityTimes -1) {
                mGestureLockPreviewView.setModePreview(password);
            }
        }

    }

    @Override
    public void onGestureCreateSuccessful(String password) {
        mPassword = password;

        Toast.makeText(this, "创建手势密码成功", Toast.LENGTH_SHORT).show();
        mGestureLockView.setVisibility(View.GONE);
        mGestureLockPreviewView.setVisibility(View.GONE);
        mGestureLockPreviewView.setModePreview("");
        if (mBtnReset.getVisibility() == View.VISIBLE) {
            mBtnReset.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean verifyPassword(String password) {
        return mPassword.equals(password);
    }
}
