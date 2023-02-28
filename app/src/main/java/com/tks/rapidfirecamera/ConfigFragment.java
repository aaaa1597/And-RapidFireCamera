package com.tks.rapidfirecamera;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import kotlin.jvm.functions.Function2;

public class ConfigFragment extends Fragment {
    private MainViewModel mViewModel;
    public static final String PREF_APPSETTING = "APPSETTING";
    public static final String PREF_KEY_SAVEPATH     = "Key_SavePath";
    public static final String PREF_KEY_RESOLUTION_H = "Key_Resolution_H";
    public static final String PREF_KEY_RESOLUTION_W = "Key_Resolution_W";
    public static ConfigFragment newInstance() {
        return new ConfigFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        Activity activity = getActivity();
        if(activity==null)
            throw new RuntimeException("Error occurred!! illigal state in this app. activity is null!!");

        final RecyclerView configRvw = activity.findViewById(R.id.rvw_config);
        /* 区切り線を表示 */
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(configRvw.getContext(), new LinearLayoutManager(getActivity().getApplicationContext()).getOrientation());
        configRvw.addItemDecoration(dividerItemDecoration);
        configRvw.setHasFixedSize(true);
        configRvw.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        configRvw.setAdapter(new ConfigAdapter());

        /* 現在値設定 */
        Resources res = activity.getResources();
        Size ressize  = mViewModel.getTakePictureSize();
        mConfigItems.add(new ConfigItem(res.getString(R.string.str_resolution)  , String.format(Locale.JAPAN, "%d x %d", ressize.getWidth(), ressize.getHeight())));
        mConfigItems.add(new ConfigItem(res.getString(R.string.str_filelocation), mViewModel.getSaveFullPath()));

        /* 撮像解像度を変更した時の動作 */
        mViewModel.setOnChageTakePictureSizeListner().observe(getViewLifecycleOwner(), size -> {
            for(ConfigItem item : mConfigItems) {
                /* 撮像解像度の文字列を設定(値は設定済) */
                if(item.mItemName.equals(res.getString(R.string.str_resolution))) {
                    item.mExplain = String.format(Locale.JAPAN, "%d x %d", size.getWidth(), size.getHeight());
                    configRvw.getAdapter().notifyItemChanged(mConfigItems.indexOf(item));
                    break;
                }
            }
        });

    }

    /* 設定アイテムのリスト */
    private final List<ConfigItem> mConfigItems = new ArrayList<>();
    private static class ConfigItem {
        public String			mItemName;
        public String			mExplain;
        ConfigItem(String itemname, String explain) {
            mItemName = itemname;
            mExplain = explain;
        }
    }

    /* 設定アイテムのアダプタ */
    class ConfigAdapter extends RecyclerView.Adapter<ConfigAdapter.ViewHolder> {
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView mTxtItemName;
            TextView mTxtExplain;
            ViewHolder(View view) {
                super(view);
                mTxtItemName = view.findViewById(R.id.txt_itemname);
                mTxtExplain  = view.findViewById(R.id.txt_explain);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_config, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ConfigItem item = mConfigItems.get(position);
            holder.mTxtItemName.setText(item.mItemName);

            /* 設定項目別処理の呼び分け */
            Function2<Context, String, String> assignExecution = (context, itemname) -> {
                /* 解像度押下 */
                if(itemname.equals(context.getResources().getString(R.string.str_resolution))) {
                    /* すでに表示済なら表示しない */
                    if( SelectResolutionDialog.mIsShowing) return "";

                    /* 撮影解像度のindexを、Cameraデバイスの解像度リストから求める */
                    List<Size> resolutions = Arrays.asList(mViewModel.getSupportedCameraSizes());
                    int index = resolutions.indexOf(mViewModel.getTakePictureSize());
                    /* 選択ダイアログ表示 */
                    SelectResolutionDialog.newInstance(mViewModel.getSupportedCameraSizes(), index).show(getChildFragmentManager(), null);
                }
                /* 保存場所押下 */
                else if(itemname.equals(context.getResources().getString(R.string.str_filelocation))) {
                    Toast.makeText(context.getApplicationContext(), getString(R.string.cant_change_the_path), Toast.LENGTH_SHORT).show();
                }
                return "";
            };

            holder.mTxtItemName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    assignExecution.invoke(v.getContext(), holder.mTxtItemName.getText().toString());
                }
            });
            holder.mTxtExplain.setText(item.mExplain);
            holder.mTxtExplain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    assignExecution.invoke(v.getContext(), holder.mTxtItemName.getText().toString());
                }
            });
        }

        @Override
        public int getItemCount() {
            return mConfigItems.size();
        }
    }

    /*******************************
     * 解像度押下 -> 解像度選択ダイアログ
     *******************************/
    public static class SelectResolutionDialog extends DialogFragment {
        private MainViewModel mViewModel;
        private RecyclerView mRecyclerView;
        private final Size[] mResolutions;
        private int mPos = -1;

        private SelectResolutionDialog(Size[] resolutions, int pos) {
            mResolutions = resolutions;
            mPos         = pos;
        }
        public static SelectResolutionDialog newInstance(Size[] resolutions, int pos) {
            return new SelectResolutionDialog(resolutions, pos);
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view    = inflater.inflate(R.layout.dialogfragment_selectresolution, container, false);
            mRecyclerView= view.findViewById(R.id.rvw_resolution);
            return view;
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
            /* 区切り線を表示 */
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(view.getContext(), new LinearLayoutManager(getActivity().getApplicationContext()).getOrientation());
            mRecyclerView.addItemDecoration(dividerItemDecoration);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
            mRecyclerView.setAdapter(new SelectResolutionAdapter());
            mRecyclerView.scrollToPosition(mPos);
        }

        class SelectResolutionAdapter extends RecyclerView.Adapter<SelectResolutionAdapter.ViewHolder> {
            class ViewHolder extends RecyclerView.ViewHolder {
                RadioButton mRbnResolution;
                ViewHolder(View view) {
                    super(view);
                    mRbnResolution = view.findViewById(R.id.rbn_resolution);
                }
            }

            @NonNull
            @Override
            public SelectResolutionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_resolution, parent, false);
                return new SelectResolutionAdapter.ViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull SelectResolutionAdapter.ViewHolder holder, int position) {
                Size resolution = mResolutions[position];
                int gcd = MainViewModel.getGreatestCommonDivisor(resolution.getWidth(), resolution.getHeight());
                holder.mRbnResolution.setText(String.format(Locale.JAPAN, "%d x %d(%d:%d)", resolution.getWidth(), resolution.getHeight(), resolution.getWidth()/gcd, resolution.getHeight()/gcd) );
                holder.mRbnResolution.setChecked(mPos==position);
                holder.mRbnResolution.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        /* 撮像解像度の値を設定(->文字列の更新は、非同期でonChageCurrentResolutionSize().observe()の処理が動くのでそこで実行) */
                        mViewModel.setTakePictureSize(new Size(resolution.getWidth(), resolution.getHeight()));

                        /* SharedPreferenceにも撮像解像度の値を設定 */
                        SharedPreferences sharedPref = v.getContext().getSharedPreferences(ConfigFragment.PREF_APPSETTING, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putInt(ConfigFragment.PREF_KEY_RESOLUTION_W, resolution.getWidth());
                        editor.putInt(ConfigFragment.PREF_KEY_RESOLUTION_H, resolution.getHeight());
                        editor.apply();

                        /* Toast表示(解像度に 〇 x △ を設定しました。) */
                        String strmsg = getString(R.string.set_the_resolution, resolution.getWidth(), resolution.getHeight());
                        Toast.makeText(v.getContext(), strmsg, Toast.LENGTH_SHORT).show();

                        /* チェック位置を更新 */
                        mPos = holder.getAbsoluteAdapterPosition();
                        notifyItemChanged(mPos);

                        /* ダイアログを消す */
                        getDialog().dismiss();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return mResolutions.length;
            }
        }

        /**　インスタンス制御(一つしか表示しない) */
        private static boolean mIsShowing = false;
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mIsShowing = true;
        }
        @Override
        public void onDestroy() {
            super.onDestroy();
            mIsShowing = false;
        }

    }
}
