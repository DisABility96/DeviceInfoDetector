package com.example.deviceinfodetector.ui.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deviceinfodetector.R; // 必须导入项目的R类
import com.example.deviceinfodetector.data.db.entity.AppBgRecord;
import com.example.deviceinfodetector.ui.fragment.AppBgMonitorFragment;

import java.util.List;
import java.util.Locale;

public class AppBgRecordAdapter extends RecyclerView.Adapter<AppBgRecordAdapter.ViewHolder> {
    private List<AppBgRecord> dataList;
    private AppBgMonitorFragment fragment;

    public AppBgRecordAdapter() {}

    public void setData(List<AppBgRecord> dataList) {
        this.dataList = dataList;
        notifyDataSetChanged();
    }

    public void setFragment(AppBgMonitorFragment fragment) {
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_bg_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppBgRecord record = dataList.get(position);
        Context context = holder.itemView.getContext(); // 从itemView获取上下文

        holder.tvAppName.setText(record.appName);
        // 修复：通过context调用getString
        holder.tvLaunchCount.setText(context.getString(R.string.label_launch_count, record.launchCount));
        holder.tvWakeReason.setText(context.getString(R.string.label_wake_reason, record.wakeReason));

        // 格式化启动时间
        String time = DateFormat.format("yyyy-MM-dd HH:mm:ss", record.startTime).toString();
        holder.tvLaunchTime.setText(context.getString(R.string.label_launch_time, time));

        // 条目点击事件
        holder.itemView.setOnClickListener(v -> {
            if (fragment != null) {
                fragment.onItemClick(record.packageName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppName, tvLaunchCount, tvWakeReason, tvLaunchTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
            tvLaunchCount = itemView.findViewById(R.id.tv_launch_count);
            tvWakeReason = itemView.findViewById(R.id.tv_wake_reason);
            tvLaunchTime = itemView.findViewById(R.id.tv_launch_time);
        }
    }
}