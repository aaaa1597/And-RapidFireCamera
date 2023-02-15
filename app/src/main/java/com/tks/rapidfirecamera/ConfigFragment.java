package com.tks.rapidfirecamera;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
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
        mConfigItems.add(new ConfigItem(activity.getResources().getString(R.string.str_resolution), String.format(Locale.JAPAN, "%dx%d", mViewModel.getCurrentResolutionSize().getWidth(), mViewModel.getCurrentResolutionSize().getHeight())));
        mConfigItems.add(new ConfigItem(activity.getResources().getString(R.string.str_filelocation), mViewModel.getSavePath()));

        /* 撮像解像度を変更した時の動作 */
        mViewModel.onChageCurrentResolutionSize().observe(getViewLifecycleOwner(), size -> {
            for(ConfigItem item : mConfigItems) {
                /* 撮像解像度の文字列を設定(値は設定済) */
                if(item.mItemName.equals(activity.getResources().getString(R.string.str_resolution))) {
                    item.mExplain = String.format(Locale.JAPAN, "%dx%d", size.getWidth(), size.getHeight());
                    configRvw.getAdapter().notifyDataSetChanged();
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
                    SelectResolutionDialog.newInstance(mViewModel.getSupportedResolutionSizes(), mViewModel).show(getChildFragmentManager(), null);
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
        private RecyclerView mRecyclerView;
        private final Size[] mResolutions;
        private MainViewModel mViewModel;
        private SelectResolutionDialog(Size[] resolutions, MainViewModel viewModel) {
            mResolutions = resolutions;
            mViewModel = viewModel;
        }
        public static SelectResolutionDialog newInstance(Size[] resolutions, MainViewModel viewModel) {
            return new SelectResolutionDialog(resolutions, viewModel);
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.dialogfragment_selectresolution, container, false);
            mRecyclerView = (RecyclerView) view.findViewById(R.id.rvw_resolution);
            return view;
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            /* 区切り線を表示 */
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(view.getContext(), new LinearLayoutManager(getActivity().getApplicationContext()).getOrientation());
            mRecyclerView.addItemDecoration(dividerItemDecoration);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
            mRecyclerView.setAdapter(new SelectResolutionAdapter());
        }

        class SelectResolutionAdapter extends RecyclerView.Adapter<SelectResolutionAdapter.ViewHolder> {
            class ViewHolder extends RecyclerView.ViewHolder {
                TextView mTxtResolution;
                ViewHolder(View view) {
                    super(view);
                    mTxtResolution = view.findViewById(R.id.txt_resolution);
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
                holder.mTxtResolution.setText(String.format(Locale.JAPAN, "%dx%d", resolution.getWidth(), resolution.getHeight()) );

                holder.mTxtResolution.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        /* 撮像解像度の値を設定(->文字列の更新は、非同期でonChageCurrentResolutionSize().observe()の処理が動くのでそこで実行) */
                        mViewModel.setCurrentResolutionSize(new Size(resolution.getWidth(), resolution.getHeight()));

                        /* SharedPreferenceにも撮像解像度の値を設定 */
                        SharedPreferences sharedPref = v.getContext().getSharedPreferences(ConfigFragment.PREF_APPSETTING, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putInt(ConfigFragment.PREF_KEY_RESOLUTION_W, resolution.getWidth());
                        editor.putInt(ConfigFragment.PREF_KEY_RESOLUTION_H, resolution.getHeight());
                        editor.apply();

                        String strmsg = getString(R.string.set_the_resolution, resolution.getWidth(), resolution.getHeight());
                        Toast.makeText(v.getContext(), strmsg, Toast.LENGTH_SHORT).show();
                        getDialog().dismiss();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return mResolutions.length;
            }
        }
    }
}
