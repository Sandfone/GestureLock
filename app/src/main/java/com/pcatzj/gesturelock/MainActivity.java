package com.pcatzj.gesturelock;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.pcatzj.gesturelock.view.GestureLockView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, GestureLockView.GestureEvent {

    private GestureLockView mGestureLockView;

    private String mPassword = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_create_password).setOnClickListener(this);
        findViewById(R.id.btn_verify_password).setOnClickListener(this);
        mGestureLockView = (GestureLockView) findViewById(R.id.view_gesture);
        mGestureLockView.setGestureEvent(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_create_password:
                createGesturePassword();
                break;
            case R.id.btn_verify_password:
                verifyGesturePassword();
                break;
        }
    }

    private void createGesturePassword() {
        mGestureLockView.setModeCreator(3);
        mGestureLockView.setVisibility(View.VISIBLE);
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
            case CREATE_SUCCESSFUL:
                Toast.makeText(this, "创建手势密码成功", Toast.LENGTH_SHORT).show();
                mGestureLockView.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onGestureCreateEffective(int leftSteps, String password) {

    }

    @Override
    public void storePassword(String password) {
        mPassword = password;
    }

    @Override
    public boolean verifyPassword(String password) {
        return mPassword.equals(password);
    }
}
