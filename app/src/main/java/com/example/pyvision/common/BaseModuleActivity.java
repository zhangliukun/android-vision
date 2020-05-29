package com.example.pyvision.common;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pyvision.R;

/**
 * Created by zale.zhang on 2020/5/16.
 *
 * @author zale.zhang
 */
public class BaseModuleActivity extends AppCompatActivity {

    private static final int UNSET = 0;

    protected HandlerThread mBackgroundThread;
    protected Handler mBackgroundHandler;
    protected Handler mUIHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUIHandler = new Handler(getMainLooper());
    }

    // 指的是onCreate方法彻底执行完毕的回调，onPostResume类似。
    // 一般不会去重写，只有在使用ActionBarDrawerToggle的使用在
    // onPostCreate需要在屏幕旋转时候等同步下状态
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        startBackgroundThread();
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("ModuleActivity");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @Override
    protected void onDestroy() {
        stopBackgroundThread();
        super.onDestroy();
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(Constants.TAG, "Error on stopping background thread", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_model, menu);
        menu.findItem(R.id.action_info).setVisible(getInfoViewCode() != UNSET);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_info) {
            onMenuItemInfoSelected();
        }
        return super.onOptionsItemSelected(item);
    }

    protected int getInfoViewCode() {
        return UNSET;
    }

    protected String getInfoViewAdditionalText() {
        return null;
    }

    private void onMenuItemInfoSelected() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setCancelable(true)
                .setView(InfoViewFactory.newInfoView(this, getInfoViewCode(), getInfoViewAdditionalText()));

        builder.show();
    }

    @UiThread
    protected void showErrorDialog(View.OnClickListener clickListener) {
        final View view = InfoViewFactory.newErrorDialogView(this);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog)
                .setCancelable(false)
                .setView(view);
        final AlertDialog alertDialog = builder.show();
        view.setOnClickListener(v -> {
            clickListener.onClick(v);
            alertDialog.dismiss();
        });
    }
}
