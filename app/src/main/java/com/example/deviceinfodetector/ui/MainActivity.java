package com.example.deviceinfodetector.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import android.os.Bundle;

import com.example.deviceinfodetector.R;
import com.example.deviceinfodetector.databinding.ActivityMainBinding;
import com.example.deviceinfodetector.ui.fragment.DeviceInfoFragment;
import com.example.deviceinfodetector.ui.fragment.AppBgMonitorFragment;
import com.example.deviceinfodetector.ui.fragment.CacheCleanFragment;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化ViewPager
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new DeviceInfoFragment(), getString(R.string.tab_device_info));
        adapter.addFragment(new AppBgMonitorFragment(), getString(R.string.tab_bg_monitor));
        adapter.addFragment(new CacheCleanFragment(), getString(R.string.tab_cache_clean));
        binding.viewPager.setAdapter(adapter);
        binding.tabLayout.setupWithViewPager(binding.viewPager);
    }

    // ViewPager适配器
    static class ViewPagerAdapter extends FragmentPagerAdapter {
        private final java.util.List<Fragment> fragmentList = new java.util.ArrayList<>();
        private final java.util.List<String> titleList = new java.util.ArrayList<>();

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        public void addFragment(Fragment fragment, String title) {
            fragmentList.add(fragment);
            titleList.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titleList.get(position);
        }
    }
}