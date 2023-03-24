package com.tks.rapidfirecamera;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventCallback;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.Surface;

import kotlin.jvm.functions.Function1;

public class MainActivity extends AppCompatActivity {
    private MainViewModel mViewModel;
    private OrientationEventListener mOrientationEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mViewModel.setSharedPreferences(getSharedPreferences(ConfigFragment.PREF_APPSETTING, Context.MODE_PRIVATE));
        mOrientationEventListener = new OrientationEventListener(getApplicationContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                int orientationdegree = ((orientation + 45) / 90) * 90;    /* 0:↑ 90:→ 180:↓ 270:← */
//                Log.d("aaaaa", String.format("端末の角度=%d", orientationdegree));
                mViewModel.setOrientation(orientationdegree);
            }
        };

        /* 全画面アプリ設定 */
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarContrastEnforced(false);
        getWindow().setNavigationBarContrastEnforced(false);

        /* get camera permission */
        ActivityResultLauncher<String> launcher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                (isGranted) -> {
                    if (isGranted) {
                        /* 権限取得 OK時 -> Fragment追加 */
                        if (null == savedInstanceState) {
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.container, MainFragment.newInstance())
                                    .commit();
                        }
                    }
                    else {
                        /* 権限取得 拒否時 -> ErrorダイアグOpenでアプリ終了!! */
                        MainFragment.ErrorDialog.newInstance(getString(R.string.request_permission)).show(getSupportFragmentManager(), "Error!!");
                    }
                });

        /* request camera permission */
        launcher.launch(android.Manifest.permission.CAMERA);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mOrientationEventListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientationEventListener.disable();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            /* コンフィグ画面時のBackイベントは、撮像画面に戻る。 */
            if(getSupportFragmentManager().getFragments().get(0) instanceof ConfigFragment) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, MainFragment.newInstance())
                        .commit();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }
}