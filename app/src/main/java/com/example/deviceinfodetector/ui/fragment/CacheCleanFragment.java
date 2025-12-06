package com.example.deviceinfodetector.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.deviceinfodetector.R; // 导入项目的R类
import com.example.deviceinfodetector.databinding.FragmentCacheCleanBinding;
import com.example.deviceinfodetector.data.util.CacheCleanUtils;
import com.example.deviceinfodetector.ui.adapter.CacheAppAdapter;
import com.example.deviceinfodetector.viewmodel.CacheCleanViewModel;

import java.util.ArrayList;
import java.util.List;

public class CacheCleanFragment extends Fragment {
    private FragmentCacheCleanBinding binding;
    private CacheCleanViewModel viewModel;
    private CacheAppAdapter adapter;
    private final List<String> selectedPackages = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCacheCleanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(CacheCleanViewModel.class);

        // 初始化RecyclerView
        adapter = new CacheAppAdapter(selectedPackages);
        binding.rvAppCache.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAppCache.setAdapter(adapter);

        // 加载缓存列表
        viewModel.getAppCacheList().observe(getViewLifecycleOwner(), appCacheInfos -> {
            adapter.setData(appCacheInfos);
            // 修复：直接调用getString
            binding.tvTotalCache.setText(getString(R.string.label_total_cache, appCacheInfos.size()));
        });

        // 清理进度监听
        viewModel.getCleanProgress().observe(getViewLifecycleOwner(), progress -> {
            binding.progressBar.setProgress(progress);
            if (progress == 100) {
                long release = viewModel.getTotalReleaseSpace().getValue();
                String releaseStr = CacheCleanUtils.formatFileSize(release);
                // 修复：直接调用getString
                Toast.makeText(getContext(), getString(R.string.toast_clean_complete, releaseStr), Toast.LENGTH_LONG).show();
                binding.progressBar.setProgress(0);
                selectedPackages.clear();
                adapter.notifyDataSetChanged();
            }
        });

        // 一键清理按钮
        binding.btnClean.setOnClickListener(v -> {
            if (selectedPackages.isEmpty()) {
                // 修复：直接调用getString
                Toast.makeText(getContext(), getString(R.string.toast_select_app), Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.cleanSelectedApps(selectedPackages);
        });

        // 刷新按钮
        binding.btnRefresh.setOnClickListener(v -> viewModel.loadAppCacheList());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}