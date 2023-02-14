package com.tks.rapidfirecamera;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.KeyEvent;

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

        MainViewModel viewmodel = new ViewModelProvider(this).get(MainViewModel.class);
//        model.getUiState().observe(this, uiState -> {
//            // update UI
//        });
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