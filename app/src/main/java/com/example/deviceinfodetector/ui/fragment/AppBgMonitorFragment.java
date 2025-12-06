package com.example.deviceinfodetector.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.deviceinfodetector.R; // 导入项目的R类
import com.example.deviceinfodetector.databinding.FragmentAppBgMonitorBinding;
import com.example.deviceinfodetector.data.util.AppBgMonitorUtils;
import com.example.deviceinfodetector.ui.adapter.AppBgRecordAdapter;

import java.util.List;

public class AppBgMonitorFragment extends Fragment {
    private FragmentAppBgMonitorBinding binding;
    private AppBgRecordAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAppBgMonitorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 初始化RecyclerView
        adapter = new AppBgRecordAdapter();
        adapter.setFragment(this); // 设置Fragment引用
        binding.rvAppBgRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAppBgRecords.setAdapter(adapter);

        // 加载后台记录
        loadBgRecords();

        // 刷新按钮
        binding.btnRefresh.setOnClickListener(v -> loadBgRecords());
    }

    private void loadBgRecords() {
        new Thread(() -> {
            List<com.example.deviceinfodetector.data.db.entity.AppBgRecord> records = AppBgMonitorUtils.getTodayBgRecords(getContext());
            getActivity().runOnUiThread(() -> {
                adapter.setData(records);
                // 修复：直接调用getString（Fragment内置方法）
                binding.tvRecordCount.setText(getString(R.string.label_record_count, records.size()));
            });
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // 条目点击监听（跳转到应用信息页）
    public void onItemClick(String packageName) {
        try {
            Intent intent = AppBgMonitorUtils.getAppInfoIntent(packageName);
            startActivity(intent);
        } catch (Exception e) {
            // 修复：直接调用getString
            Toast.makeText(getContext(), getString(R.string.toast_open_app_info_fail), Toast.LENGTH_SHORT).show();
        }
    }
}