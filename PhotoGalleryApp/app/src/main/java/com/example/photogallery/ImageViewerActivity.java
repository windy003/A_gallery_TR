package com.example.photogallery;

import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageViewerActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private LinearLayout layoutControls;
    private Button buttonAddToThreeDaysLater;
    private Button buttonDelete;
    private TextView textViewPageInfo;
    private List<Photo> photos;
    private int currentPosition;
    private boolean controlsVisible = true;
    private ImagePagerAdapter adapter;
    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;
    private ActivityResultLauncher<IntentSenderRequest> delayDeleteRequestLauncher;
    private String folderName;
    private boolean isDateFolder;
    private FileOperationHelper fileOperationHelper;
    private Photo photoToDelay; // 临时存储待延迟的图片
    private RecycleBinManager recycleBinManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        // 初始化删除请求启动器
        deleteRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // 用户确认删除，执行删除后的操作
                        performDeleteCleanup();
                    } else {
                        Toast.makeText(this, "删除已取消", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // 初始化延迟操作中的删除请求启动器
        delayDeleteRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // 用户确认删除旧文件，延迟操作完成
                        performDelayCleanup();
                    } else {
                        Toast.makeText(this, "延迟操作已取消", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        viewPager = findViewById(R.id.viewPager);
        layoutControls = findViewById(R.id.layoutControls);
        buttonAddToThreeDaysLater = findViewById(R.id.buttonAddToThreeDaysLater);
        buttonDelete = findViewById(R.id.buttonDelete);
        textViewPageInfo = findViewById(R.id.textViewPageInfo);

        photos = (ArrayList<Photo>) getIntent().getSerializableExtra("photos");
        currentPosition = getIntent().getIntExtra("position", 0);
        folderName = getIntent().getStringExtra("folder_name");
        isDateFolder = getIntent().getBooleanExtra("is_date_folder", false);

        fileOperationHelper = new FileOperationHelper(this);
        recycleBinManager = new RecycleBinManager(this);

        setupViewPager();
        setupControls();
    }

    private void setupViewPager() {
        adapter = new ImagePagerAdapter(this, photos);
        adapter.setOnImageClickListener(() -> toggleControls());
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition, false);
        viewPager.setOffscreenPageLimit(1); // 预加载前后各一张图片

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                updatePageInfo();
            }
        });

        updatePageInfo();
    }

    private void setupControls() {
        buttonAddToThreeDaysLater.setOnClickListener(v -> addToThreeDaysLater());
        buttonDelete.setOnClickListener(v -> deleteCurrentPhoto());
    }

    private void addToThreeDaysLater() {
        if (currentPosition >= photos.size()) {
            return;
        }

        Photo currentPhoto = photos.get(currentPosition);
        photoToDelay = currentPhoto; // 保存引用，供删除回调使用

        Toast.makeText(this, "正在复制文件...", Toast.LENGTH_SHORT).show();

        // 第一步：复制文件（新文件会有新的DATE_ADDED）
        new Thread(() -> {
            long newPhotoId = fileOperationHelper.copyImageFile(currentPhoto);

            runOnUiThread(() -> {
                if (newPhotoId == -1) {
                    Toast.makeText(this, "复制文件失败", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 第二步：使用软删除（移到回收站），避免系统权限对话框
                recycleBinManager.moveToRecycleBin(currentPhoto, new RecycleBinManager.Callback() {
                    @Override
                    public void onSuccess() {
                        performDelayCleanup();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(ImageViewerActivity.this,
                                "移除旧文件失败: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }).start();
    }

    /**
     * 延迟操作完成后的清理工作
     */
    private void performDelayCleanup() {
        // 从适配器中移除当前照片
        adapter.removePhoto(currentPosition);

        // 如果列表为空，关闭Activity
        if (photos.isEmpty()) {
            Toast.makeText(this, "延迟操作完成", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
            return;
        }

        // 调整当前位置
        if (currentPosition >= photos.size()) {
            currentPosition = photos.size() - 1;
        }

        // 更新页面信息
        updatePageInfo();

        // 设置result，通知上级Activity刷新
        setResult(RESULT_OK);

        Toast.makeText(this, "延迟操作完成，文件将在3天后显示", Toast.LENGTH_SHORT).show();
    }

    private void updatePageInfo() {
        textViewPageInfo.setText((currentPosition + 1) + " / " + photos.size());
    }

    private void toggleControls() {
        if (controlsVisible) {
            layoutControls.setVisibility(View.GONE);
        } else {
            layoutControls.setVisibility(View.VISIBLE);
        }
        controlsVisible = !controlsVisible;
    }

    private void deleteCurrentPhoto() {
        if (currentPosition < photos.size()) {
            Photo photoToDelete = photos.get(currentPosition);

            // 使用软删除：移动到回收站
            recycleBinManager.moveToRecycleBin(photoToDelete, new RecycleBinManager.Callback() {
                @Override
                public void onSuccess() {
                    performDeleteCleanup();
                    Toast.makeText(ImageViewerActivity.this, "已移至回收站，24小时后自动删除", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(ImageViewerActivity.this, "移至回收站失败: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void performDeleteCleanup() {
        if (currentPosition >= 0 && currentPosition < photos.size()) {
            // 从适配器中移除
            adapter.removePhoto(currentPosition);
        }

        // 如果删除后列表为空，关闭Activity
        if (photos.isEmpty()) {
            setResult(RESULT_OK);
            finish();
            return;
        }

        // 调整当前位置
        if (currentPosition >= photos.size()) {
            currentPosition = photos.size() - 1;
        }

        // 更新页面信息
        updatePageInfo();

        // 设置result，通知上级Activity刷新
        setResult(RESULT_OK);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
