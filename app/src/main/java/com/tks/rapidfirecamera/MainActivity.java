package com.tks.rapidfirecamera;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Size;
import android.view.KeyEvent;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        /* ViewModelインスタンス生成 */
        MainViewModel viewmodel = new ViewModelProvider(this).get(MainViewModel.class);

        /* 設定読込み */
        SharedPreferences sharedPref = getSharedPreferences(ConfigFragment.PREF_APPSETTING, Context.MODE_PRIVATE);

        /* 保存場所 読込み */
        String savepath = sharedPref.getString(ConfigFragment.PREF_KEY_SAVEPATH, "");
        if(savepath.equals("")) {
            /* 外部保存先を取得する(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)配下しか対象にしない) */
            String defalutsavepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/Rapidfie";
            File defalutsavefile = new File(defalutsavepath);
            defalutsavefile.mkdirs();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(ConfigFragment.PREF_KEY_SAVEPATH, defalutsavefile.getAbsolutePath());
            editor.apply();
            savepath = defalutsavefile.getAbsolutePath();
        }
        viewmodel.setSavePath(savepath);

        /* 解像度 読込み */
        int resolutionw = sharedPref.getInt(ConfigFragment.PREF_KEY_RESOLUTION_W, -1);
        int resolutionh = sharedPref.getInt(ConfigFragment.PREF_KEY_RESOLUTION_H, -1);
        if(resolutionw == -1 || resolutionh == -1) {
            resolutionw = 1920;
            resolutionh = 1080;
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(ConfigFragment.PREF_KEY_RESOLUTION_W, resolutionw);
            editor.putInt(ConfigFragment.PREF_KEY_RESOLUTION_H, resolutionh);
            editor.apply();
        }
        viewmodel.setResolutionSize(new Size(resolutionw, resolutionh));
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