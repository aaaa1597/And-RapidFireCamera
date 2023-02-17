package com.tks.rapidfirecamera;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventCallback;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.KeyEvent;
import android.view.Surface;
import android.widget.Toast;

import java.io.File;

import kotlin.jvm.functions.Function1;

public class MainActivity extends AppCompatActivity {
    private MainViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mViewModel.setSharedPreferences(getSharedPreferences(ConfigFragment.PREF_APPSETTING, Context.MODE_PRIVATE));

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
        Sensor accel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(mSensorEventCallback, accel, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(mSensorEventCallback);
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

    /* ***********
     * 縦横切替え
     * ************/
    private SensorManager mSensorManager;
    private int mRotation             = -1;
    private int mNextRotation         = -1;
    private int mRotationTransCounter = 0;

    final private SensorEventCallback mSensorEventCallback = new SensorEventCallback() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            super.onSensorChanged(event);

            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
                return;

            float sensorX = event.values[0];
            float sensorY = event.values[1];
            float sensorZ = event.values[2];

            int rotetion = -1;
            if(sensorY>5)       rotetion = Surface.ROTATION_0;
            else if(sensorY<-5) rotetion = Surface.ROTATION_180;
            else if(sensorX> 5) rotetion = Surface.ROTATION_270;
            else if(sensorX<-5) rotetion = Surface.ROTATION_90;

            /* 起動直後、何もしない */
            if(mNextRotation == -1 || mRotation == -1) {
                mRotation     = rotetion;
                mNextRotation = rotetion;
                return;
            }

            /* 回転状態が変更前に、変化した時はカウンタリセット */
            if(mNextRotation != rotetion) {
                mNextRotation = rotetion;
                mRotationTransCounter = 0;
                return;
            }
            /* 回転状態に変化がない時は、何もしない */
            else if(mNextRotation == mRotation) {
                mRotationTransCounter = 0;
                return;
            }
            /* 回転状態に変化あり(mNextRotation!=mRotation)、かつ、回転状態変更準備中(mNextRotation==rotetion)の時、回転状態変更準備中カウンタをインクリメント */
            else {
                mRotationTransCounter++;
                if(mRotationTransCounter >= 5) {
                    /* 回転角度(from -> to)を求める関数 */
                    Function1<Pair<Integer,Integer>, Pair<Integer,Integer>> getRotDegreesFromTo = (rots) -> {
                        if(     rots.first == Surface.ROTATION_0  && rots.second == Surface.ROTATION_90 )  return new Pair<>(360, 270);/*"上向き"->"横↓向き"*/
                        else if(rots.first == Surface.ROTATION_0  && rots.second == Surface.ROTATION_180)  return new Pair<>(  0, 180);/*"上向き"->  "↓向き"*/
                        else if(rots.first == Surface.ROTATION_0  && rots.second == Surface.ROTATION_270)  return new Pair<>(  0,  90);/*"上向き"->"横↑向き"*/

                        else if(rots.first == Surface.ROTATION_90  && rots.second == Surface.ROTATION_0 )  return new Pair<>( 270,360);   /*"横↓向き"->  "上向き"*/
                        else if(rots.first == Surface.ROTATION_90  && rots.second == Surface.ROTATION_180) return new Pair<>( 270,180);   /*"横↓向き"->  "↓向き"*/
                        else if(rots.first == Surface.ROTATION_90  && rots.second == Surface.ROTATION_270) return new Pair<>( 270, 90);   /*"横↓向き"->"横↑向き"*/

                        else if(rots.first == Surface.ROTATION_180 && rots.second == Surface.ROTATION_0 )  return new Pair<>( 180, 0);   /*"↓向き"->  "上向き"*/
                        else if(rots.first == Surface.ROTATION_180 && rots.second == Surface.ROTATION_90)  return new Pair<>( 180,270);   /*"↓向き"->"横↓向き"*/
                        else if(rots.first == Surface.ROTATION_180 && rots.second == Surface.ROTATION_270) return new Pair<>( 180, 90);   /*"↓向き"->"横↑向き"*/

                        else if(rots.first == Surface.ROTATION_270 && rots.second == Surface.ROTATION_0 )  return new Pair<>( 90,  0);   /*"横↑向き"->  "上向き"*/
                        else if(rots.first == Surface.ROTATION_270 && rots.second == Surface.ROTATION_90)  return new Pair<>( 90,270);   /*"横↑向き"->"横↓向き"*/
                        else if(rots.first == Surface.ROTATION_270 && rots.second == Surface.ROTATION_180) return new Pair<>( 90,180);   /*"横↑向き"->  "↓向き"*/

                        return new Pair<>(0,0);
                    };
                    /* 上記関数を呼ぶ */
                    mViewModel.setRotation(getRotDegreesFromTo.invoke(new Pair<>(mRotation, mNextRotation)));
                    mRotation = mNextRotation;
                }
            }
        }
    };
}