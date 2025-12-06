package com.example.deviceinfodetector.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deviceinfodetector.R; // 导入项目的R类
import com.example.deviceinfodetector.data.util.CacheCleanUtils;

import java.util.List;

public class CacheAppAdapter extends RecyclerView.Adapter<CacheAppAdapter.ViewHolder> {
    private List<CacheCleanUtils.AppCacheInfo> dataList;
    private final List<String> selectedPackages;

    public CacheAppAdapter(List<String> selectedPackages) {
        this.selectedPackages = selectedPackages;
    }

    public void setData(List<CacheCleanUtils.AppCacheInfo> dataList) {
        this.dataList = dataList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_cache, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CacheCleanUtils.AppCacheInfo info = dataList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvAppName.setText(info.appName);
        holder.tvCacheSize.setText(context.getString(R.string.label_cache_size, CacheCleanUtils.formatFileSize(info.cacheSize)));
        holder.cbSelect.setChecked(selectedPackages.contains(info.packageName));

        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedPackages.add(info.packageName);
            } else {
                selectedPackages.remove(info.packageName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAppName, tvCacheSize;
        CheckBox cbSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAppName = itemView.findViewById(R.id.tv_app_name);
            tvCacheSize = itemView.findViewById(R.id.tv_cache_size);
            cbSelect = itemView.findViewById(R.id.cb_select);
        }
    }
}