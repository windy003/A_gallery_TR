package com.example.photogallery;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 回收站全屏查看器
 * 支持查看回收站中的照片/视频，以及恢复和永久删除操作
 */
public class RecycleBinViewerActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout layoutControls;
    private TextView textViewPageInfo;
    private TextView textViewRemainingTime;
    private Button buttonRestore;
    private Button buttonDelete;

    private List<DeletedPhoto> deletedPhotos;
    private int currentPosition;
    private boolean controlsVisible = true;
    private RecycleBinPagerAdapter adapter;
    private RecycleBinManager recycleBinManager;

    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;
    private DeletedPhoto pendingDeleteItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle_bin_viewer);

        // 初始化视图
        viewPager = findViewById(R.id.viewPager);
        layoutControls = findViewById(R.id.layoutControls);
        textViewPageInfo = findViewById(R.id.textViewPageInfo);
        textViewRemainingTime = findViewById(R.id.textViewRemainingTime);
        buttonRestore = findViewById(R.id.buttonRestore);
        buttonDelete = findViewById(R.id.buttonDelete);

        // 获取数据
        deletedPhotos = (ArrayList<DeletedPhoto>) getIntent().getSerializableExtra("deleted_photos");
        currentPosition = getIntent().getIntExtra("position", 0);

        if (deletedPhotos == null || deletedPhotos.isEmpty()) {
            Toast.makeText(this, "没有可查看的项目", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recycleBinManager = new RecycleBinManager(this);

        // 设置删除请求启动器
        deleteRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && pendingDeleteItem != null) {
                        recycleBinManager.removeFromDatabase(pendingDeleteItem, new RecycleBinManager.Callback() {
                            @Override
                            public void onSuccess() {
                                performDeleteCleanup();
                                Toast.makeText(RecycleBinViewerActivity.this, "已永久删除", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(RecycleBinViewerActivity.this, "删除失败: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
        );

        setupViewPager();
        setupControls();
    }

    private void setupViewPager() {
        adapter = new RecycleBinPagerAdapter(this, deletedPhotos);
        adapter.setOnImageClickListener(this::toggleControls);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition, false);
        viewPager.setOffscreenPageLimit(1);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                updatePageInfo();
                updateRemainingTime();
            }
        });

        updatePageInfo();
        updateRemainingTime();
    }

    private void setupControls() {
        buttonRestore.setOnClickListener(v -> restoreCurrentItem());
        buttonDelete.setOnClickListener(v -> deleteCurrentItem());
    }

    private void updatePageInfo() {
        textViewPageInfo.setText((currentPosition + 1) + " / " + deletedPhotos.size());
    }

    private void updateRemainingTime() {
        if (currentPosition < deletedPhotos.size()) {
            DeletedPhoto item = deletedPhotos.get(currentPosition);
            long remainingTime = item.getRemainingTime();
            textViewRemainingTime.setText(formatRemainingTime(remainingTime));
        }
    }

    private String formatRemainingTime(long milliseconds) {
        if (milliseconds <= 0) {
            return "已过期，将被删除";
        }

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "剩余 %d小时%d分钟后永久删除", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "剩余 %d分钟后永久删除", minutes);
        } else {
            return String.format(Locale.getDefault(), "剩余 %d秒后永久删除", seconds);
        }
    }

    private void toggleControls() {
        if (controlsVisible) {
            layoutControls.setVisibility(View.GONE);
        } else {
            layoutControls.setVisibility(View.VISIBLE);
        }
        controlsVisible = !controlsVisible;
    }

    private void restoreCurrentItem() {
        if (currentPosition >= deletedPhotos.size()) {
            return;
        }

        DeletedPhoto item = deletedPhotos.get(currentPosition);

        new AlertDialog.Builder(this)
                .setTitle("恢复确认")
                .setMessage("确定要恢复 " + item.getName() + " 吗？")
                .setPositiveButton("恢复", (dialog, which) -> {
                    recycleBinManager.restoreFromRecycleBin(item, new RecycleBinManager.Callback() {
                        @Override
                        public void onSuccess() {
                            performDeleteCleanup();
                            setResult(RESULT_OK);
                            Toast.makeText(RecycleBinViewerActivity.this, "已恢复", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(RecycleBinViewerActivity.this, "恢复失败: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteCurrentItem() {
        if (currentPosition >= deletedPhotos.size()) {
            return;
        }

        DeletedPhoto item = deletedPhotos.get(currentPosition);

        new AlertDialog.Builder(this)
                .setTitle("永久删除")
                .setMessage("确定要永久删除 " + item.getName() + " 吗？\n此操作无法撤销！")
                .setPositiveButton("删除", (dialog, which) -> {
                    pendingDeleteItem = item;

                    recycleBinManager.permanentlyDelete(item, deleteRequestLauncher, new RecycleBinManager.Callback() {
                        @Override
                        public void onSuccess() {
                            performDeleteCleanup();
                            Toast.makeText(RecycleBinViewerActivity.this, "已永久删除", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(RecycleBinViewerActivity.this, "删除失败: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void performDeleteCleanup() {
        if (currentPosition >= 0 && currentPosition < deletedPhotos.size()) {
            adapter.removeItem(currentPosition);
        }

        if (deletedPhotos.isEmpty()) {
            setResult(RESULT_OK);
            finish();
            return;
        }

        if (currentPosition >= deletedPhotos.size()) {
            currentPosition = deletedPhotos.size() - 1;
        }

        updatePageInfo();
        updateRemainingTime();
        setResult(RESULT_OK);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
