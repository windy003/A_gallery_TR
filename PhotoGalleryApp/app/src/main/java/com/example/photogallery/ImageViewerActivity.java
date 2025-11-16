package com.example.photogallery;

import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ImageViewerActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private LinearLayout layoutControls;
    private LinearLayout layoutFloatingButtons;
    private View dragHandle;
    private Button buttonAddToThreeDaysLater;
    private Button buttonDelete;
    private Button buttonUndo;
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
    private int positionToDelay; // 临时存储待延迟的位置
    private RecycleBinManager recycleBinManager;

    // 悬浮按钮拖动相关变量
    private float dX, dY;

    // 撤销相关变量
    private LinkedList<UndoAction> undoStack = new LinkedList<>();
    private static final int MAX_UNDO_COUNT = 5;

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
        layoutFloatingButtons = findViewById(R.id.layoutFloatingButtons);
        dragHandle = findViewById(R.id.dragHandle);
        buttonAddToThreeDaysLater = findViewById(R.id.buttonAddToThreeDaysLater);
        buttonDelete = findViewById(R.id.buttonDelete);
        buttonUndo = findViewById(R.id.buttonUndo);
        textViewPageInfo = findViewById(R.id.textViewPageInfo);

        photos = (ArrayList<Photo>) getIntent().getSerializableExtra("photos");
        currentPosition = getIntent().getIntExtra("position", 0);
        folderName = getIntent().getStringExtra("folder_name");
        isDateFolder = getIntent().getBooleanExtra("is_date_folder", false);

        fileOperationHelper = new FileOperationHelper(this);
        recycleBinManager = new RecycleBinManager(this);

        setupViewPager();
        setupControls();
        setupFloatingButtonsDrag();
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
        buttonUndo.setOnClickListener(v -> performUndo());
        updateUndoButton();
    }

    private void setupFloatingButtonsDrag() {
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = layoutFloatingButtons.getX() - event.getRawX();
                        dY = layoutFloatingButtons.getY() - event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // 限制在屏幕范围内
                        View parent = (View) layoutFloatingButtons.getParent();
                        int maxX = parent.getWidth() - layoutFloatingButtons.getWidth();
                        int maxY = parent.getHeight() - layoutFloatingButtons.getHeight();

                        newX = Math.max(0, Math.min(newX, maxX));
                        newY = Math.max(0, Math.min(newY, maxY));

                        layoutFloatingButtons.setX(newX);
                        layoutFloatingButtons.setY(newY);
                        return true;

                    case MotionEvent.ACTION_UP:
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    private void addToThreeDaysLater() {
        if (currentPosition >= photos.size()) {
            return;
        }

        Photo currentPhoto = photos.get(currentPosition);
        photoToDelay = currentPhoto; // 保存引用，供删除回调使用
        positionToDelay = currentPosition; // 保存位置，供撤销使用

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
                        // 记录撤销操作
                        addUndoAction(UndoAction.createDelayAction(photoToDelay, positionToDelay, newPhotoId));
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
            layoutFloatingButtons.setVisibility(View.GONE);
        } else {
            layoutControls.setVisibility(View.VISIBLE);
            layoutFloatingButtons.setVisibility(View.VISIBLE);
        }
        controlsVisible = !controlsVisible;
    }

    private void deleteCurrentPhoto() {
        if (currentPosition < photos.size()) {
            Photo photoToDelete = photos.get(currentPosition);
            int positionToDelete = currentPosition;

            // 使用软删除：移动到回收站
            recycleBinManager.moveToRecycleBin(photoToDelete, new RecycleBinManager.Callback() {
                @Override
                public void onSuccess() {
                    // 记录撤销操作
                    addUndoAction(UndoAction.createDeleteAction(photoToDelete, positionToDelete));
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

    /**
     * 添加撤销操作到栈中
     */
    private void addUndoAction(UndoAction action) {
        undoStack.addFirst(action);
        // 保持栈大小不超过最大值
        while (undoStack.size() > MAX_UNDO_COUNT) {
            undoStack.removeLast();
        }
        updateUndoButton();
    }

    /**
     * 更新撤销按钮状态
     */
    private void updateUndoButton() {
        if (undoStack.isEmpty()) {
            buttonUndo.setEnabled(false);
            buttonUndo.setText("撤销");
        } else {
            buttonUndo.setEnabled(true);
            buttonUndo.setText("撤销(" + undoStack.size() + ")");
        }
    }

    /**
     * 执行撤销操作
     */
    private void performUndo() {
        if (undoStack.isEmpty()) {
            Toast.makeText(this, "没有可撤销的操作", Toast.LENGTH_SHORT).show();
            return;
        }

        UndoAction action = undoStack.removeFirst();

        if (action.getActionType() == UndoAction.TYPE_DELETE) {
            undoDelete(action);
        } else if (action.getActionType() == UndoAction.TYPE_DELAY) {
            undoDelay(action);
        }
    }

    /**
     * 撤销删除操作
     */
    private void undoDelete(UndoAction action) {
        Photo photo = action.getOriginalPhoto();

        // 创建DeletedPhoto对象用于从回收站恢复
        DeletedPhoto deletedPhoto = DeletedPhoto.fromPhoto(photo);

        recycleBinManager.restoreFromRecycleBin(deletedPhoto, new RecycleBinManager.Callback() {
            @Override
            public void onSuccess() {
                // 将照片恢复到列表中
                int position = Math.min(action.getOriginalPosition(), photos.size());
                photos.add(position, photo);
                adapter.notifyItemInserted(position);

                // 更新当前位置
                if (position <= currentPosition) {
                    currentPosition = position;
                    viewPager.setCurrentItem(currentPosition, false);
                }

                updatePageInfo();
                updateUndoButton();
                setResult(RESULT_OK);

                Toast.makeText(ImageViewerActivity.this,
                    "已撤销删除: " + photo.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                // 恢复失败，将操作放回栈中
                undoStack.addFirst(action);
                updateUndoButton();
                Toast.makeText(ImageViewerActivity.this,
                    "撤销失败: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 撤销移到3天后操作
     */
    private void undoDelay(UndoAction action) {
        Photo originalPhoto = action.getOriginalPhoto();
        long newPhotoId = action.getNewPhotoId();

        Toast.makeText(this, "正在撤销...", Toast.LENGTH_SHORT).show();

        // 第一步：删除新创建的副本
        new Thread(() -> {
            Uri newPhotoUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    newPhotoId
            );

            ContentResolver resolver = getContentResolver();
            try {
                int deletedRows = resolver.delete(newPhotoUri, null, null);

                runOnUiThread(() -> {
                    if (deletedRows > 0 || true) { // 即使删除失败也继续恢复原文件
                        // 第二步：从回收站恢复原文件
                        DeletedPhoto deletedPhoto = DeletedPhoto.fromPhoto(originalPhoto);

                        recycleBinManager.restoreFromRecycleBin(deletedPhoto, new RecycleBinManager.Callback() {
                            @Override
                            public void onSuccess() {
                                // 将照片恢复到列表中
                                int position = Math.min(action.getOriginalPosition(), photos.size());
                                photos.add(position, originalPhoto);
                                adapter.notifyItemInserted(position);

                                // 更新当前位置
                                if (position <= currentPosition) {
                                    currentPosition = position;
                                    viewPager.setCurrentItem(currentPosition, false);
                                }

                                updatePageInfo();
                                updateUndoButton();
                                setResult(RESULT_OK);

                                Toast.makeText(ImageViewerActivity.this,
                                    "已撤销移到3天后: " + originalPhoto.getName(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                // 恢复失败，将操作放回栈中
                                undoStack.addFirst(action);
                                updateUndoButton();
                                Toast.makeText(ImageViewerActivity.this,
                                    "撤销失败: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    // 即使删除新文件失败，也尝试恢复原文件
                    DeletedPhoto deletedPhoto = DeletedPhoto.fromPhoto(originalPhoto);
                    recycleBinManager.restoreFromRecycleBin(deletedPhoto, new RecycleBinManager.Callback() {
                        @Override
                        public void onSuccess() {
                            int position = Math.min(action.getOriginalPosition(), photos.size());
                            photos.add(position, originalPhoto);
                            adapter.notifyItemInserted(position);

                            if (position <= currentPosition) {
                                currentPosition = position;
                                viewPager.setCurrentItem(currentPosition, false);
                            }

                            updatePageInfo();
                            updateUndoButton();
                            setResult(RESULT_OK);

                            Toast.makeText(ImageViewerActivity.this,
                                "已撤销移到3天后(部分): " + originalPhoto.getName(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            undoStack.addFirst(action);
                            updateUndoButton();
                            Toast.makeText(ImageViewerActivity.this,
                                "撤销失败: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
