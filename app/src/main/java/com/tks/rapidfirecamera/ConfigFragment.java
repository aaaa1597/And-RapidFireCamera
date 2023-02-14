package com.tks.rapidfirecamera;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ConfigFragment extends Fragment {
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

        Activity activity = getActivity();
        if(activity==null)
            throw new RuntimeException("Error occurred!! illigal state in this app. activity is null!!");

        RecyclerView configRvw = activity.findViewById(R.id.rvw_config);
        /* BLEデバイスリストに区切り線を表示 */
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(configRvw.getContext(), new LinearLayoutManager(getActivity().getApplicationContext()).getOrientation());
        configRvw.addItemDecoration(dividerItemDecoration);
        configRvw.setHasFixedSize(true);
        configRvw.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        configRvw.setAdapter(new ConfigAdapter());

        /* ダミーデータ */
        mConfigItems.add(new ConfigItem(activity.getResources().getString(R.string.str_resolution), "aaaa002"));
        mConfigItems.add(new ConfigItem(activity.getResources().getString(R.string.str_filelocation), "aaaa002"));
    }

    /* メンバ変数 */
    private final List<ConfigItem> mConfigItems = new ArrayList<>();
    private static class ConfigItem {
        public String			mItemName;
        public String			mExplain;
        ConfigItem(String itemname, String explain) {
            mItemName = itemname;
            mExplain = explain;
        }
    }

    class ConfigAdapter extends RecyclerView.Adapter<ConfigAdapter.ViewHolder> {
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView mTxtItemName;
            TextView mTxtExplain;
            ViewHolder(View view) {
                super(view);
                mTxtItemName   = view.findViewById(R.id.txt_itemname);
                mTxtExplain    = view.findViewById(R.id.txt_explain);
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
            holder.mTxtItemName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(v.getContext(), "aaaaa", Toast.LENGTH_SHORT).show();
                }
            });
            holder.mTxtExplain.setText(item.mExplain);
            holder.mTxtExplain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(v.getContext(), "bbbbb", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mConfigItems.size();
        }
    }
}
