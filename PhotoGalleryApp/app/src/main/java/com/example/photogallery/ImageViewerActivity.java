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
    private DateFolderManager dateFolderManager;
    private boolean controlsVisible = true;
    private ImagePagerAdapter adapter;
    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;

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

        viewPager = findViewById(R.id.viewPager);
        layoutControls = findViewById(R.id.layoutControls);
        buttonAddToThreeDaysLater = findViewById(R.id.buttonAddToThreeDaysLater);
        buttonDelete = findViewById(R.id.buttonDelete);
        textViewPageInfo = findViewById(R.id.textViewPageInfo);

        photos = (ArrayList<Photo>) getIntent().getSerializableExtra("photos");
        currentPosition = getIntent().getIntExtra("position", 0);

        dateFolderManager = new DateFolderManager(this);

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
        if (currentPosition < photos.size()) {
            Photo currentPhoto = photos.get(currentPosition);
            String threeDaysLaterDate = DateFolderManager.getDateAfterDays(3);

            dateFolderManager.addPhotoToDateFolder(currentPhoto.getPath(), threeDaysLaterDate);

            Toast.makeText(this,
                    "已添加到 " + threeDaysLaterDate + " 的文件夹",
                    Toast.LENGTH_SHORT).show();
        }
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

            try {
                // 重新构建 URI（因为 Uri 是 transient 的，序列化后会丢失）
                Uri photoUri = android.content.ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    photoToDelete.getId()
                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+ (API 30+): 使用 createDeleteRequest
                    List<Uri> urisToDelete = Collections.singletonList(photoUri);
                    PendingIntent pendingIntent = MediaStore.createDeleteRequest(getContentResolver(), urisToDelete);

                    IntentSenderRequest request = new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build();
                    deleteRequestLauncher.launch(request);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10 (API 29): 使用 RecoverableSecurityException
                    try {
                        int deletedRows = getContentResolver().delete(photoUri, null, null);
                        if (deletedRows > 0) {
                            performDeleteCleanup();
                        } else {
                            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                        }
                    } catch (RecoverableSecurityException e) {
                        // 需要用户授权
                        IntentSenderRequest request = new IntentSenderRequest.Builder(
                            e.getUserAction().getActionIntent().getIntentSender()
                        ).build();
                        deleteRequestLauncher.launch(request);
                    }
                } else {
                    // Android 9 及以下：直接删除
                    int deletedRows = getContentResolver().delete(photoUri, null, null);
                    if (deletedRows > 0) {
                        performDeleteCleanup();
                    } else {
                        Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Toast.makeText(this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private void performDeleteCleanup() {
        // 从适配器中移除
        adapter.removePhoto(currentPosition);

        // 如果删除后列表为空，关闭Activity
        if (photos.isEmpty()) {
            Toast.makeText(this, "照片已删除", Toast.LENGTH_SHORT).show();
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
        Toast.makeText(this, "照片已删除", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
