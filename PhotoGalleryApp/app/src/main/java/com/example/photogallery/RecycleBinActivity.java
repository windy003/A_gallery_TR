package com.example.photogallery;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 回收站Activity
 * 显示已删除的照片/视频，支持恢复和永久删除
 */
public class RecycleBinActivity extends AppCompatActivity {

    private RecyclerView recyclerViewDeletedPhotos;
    private TextView textViewEmpty;
    private TextView textViewTitle;
    private Button buttonEmptyAll;

    private DeletedPhotoAdapter adapter;
    private List<DeletedPhoto> deletedPhotos;
    private RecycleBinManager recycleBinManager;

    private ActivityResultLauncher<IntentSenderRequest> deleteRequestLauncher;
    private ActivityResultLauncher<Intent> viewerLauncher;
    private DeletedPhoto pendingDeleteItem;
    private int pendingDeletePosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycle_bin);

        // 初始化视图
        recyclerViewDeletedPhotos = findViewById(R.id.recyclerViewDeletedPhotos);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        textViewTitle = findViewById(R.id.textViewTitle);
        buttonEmptyAll = findViewById(R.id.buttonEmptyAll);

        recycleBinManager = new RecycleBinManager(this);
        deletedPhotos = new ArrayList<>();

        // 设置删除请求启动器（用于永久删除需要权限时）
        deleteRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && pendingDeleteItem != null) {
                        // 用户授权删除，从数据库中移除记录
                        recycleBinManager.removeFromDatabase(pendingDeleteItem, new RecycleBinManager.Callback() {
                            @Override
                            public void onSuccess() {
                                adapter.removeItem(pendingDeletePosition);
                                updateEmptyView();
                                Toast.makeText(RecycleBinActivity.this, "已永久删除", Toast.LENGTH_SHORT).show();
                                pendingDeleteItem = null;
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(RecycleBinActivity.this, "删除失败: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
        );

        // 设置查看器启动器
        viewerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // 查看器有变化，刷新列表
                        setResult(RESULT_OK);
                        loadDeletedPhotos();
                    }
                }
        );

        // 设置RecyclerView
        recyclerViewDeletedPhotos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeletedPhotoAdapter(this, deletedPhotos, new DeletedPhotoAdapter.OnItemActionListener() {
            @Override
            public void onRestore(DeletedPhoto item, int position) {
                restoreItem(item, position);
            }

            @Override
            public void onDelete(DeletedPhoto item, int position) {
                confirmPermanentDelete(item, position);
            }
        });

        // 设置点击查看全屏
        adapter.setOnItemClickListener(position -> {
            if (!deletedPhotos.isEmpty()) {
                Intent intent = new Intent(RecycleBinActivity.this, RecycleBinViewerActivity.class);
                intent.putExtra("deleted_photos", new ArrayList<>(deletedPhotos));
                intent.putExtra("position", position);
                viewerLauncher.launch(intent);
            }
        });

        recyclerViewDeletedPhotos.setAdapter(adapter);

        // 清空回收站按钮
        buttonEmptyAll.setOnClickListener(v -> confirmEmptyAll());

        // 加载数据
        loadDeletedPhotos();
    }

    private void loadDeletedPhotos() {
        recycleBinManager.getAllDeletedItems(items -> {
            if (items != null) {
                deletedPhotos.clear();
                deletedPhotos.addAll(items);
                adapter.notifyDataSetChanged();
                updateEmptyView();
                updateTitle();
            }
        });
    }

    private void updateEmptyView() {
        if (deletedPhotos.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
            recyclerViewDeletedPhotos.setVisibility(View.GONE);
            buttonEmptyAll.setEnabled(false);
        } else {
            textViewEmpty.setVisibility(View.GONE);
            recyclerViewDeletedPhotos.setVisibility(View.VISIBLE);
            buttonEmptyAll.setEnabled(true);
        }
    }

    private void updateTitle() {
        textViewTitle.setText("回收站 (" + deletedPhotos.size() + ")");
    }

    private void restoreItem(DeletedPhoto item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("恢复确认")
                .setMessage("确定要恢复 " + item.getName() + " 吗？")
                .setPositiveButton("恢复", (dialog, which) -> {
                    recycleBinManager.restoreFromRecycleBin(item, new RecycleBinManager.Callback() {
                        @Override
                        public void onSuccess() {
                            adapter.removeItem(position);
                            updateEmptyView();
                            updateTitle();
                            setResult(RESULT_OK); // 通知MainActivity刷新
                            Toast.makeText(RecycleBinActivity.this, "已恢复", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(RecycleBinActivity.this, "恢复失败: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmPermanentDelete(DeletedPhoto item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("永久删除")
                .setMessage("确定要永久删除 " + item.getName() + " 吗？\n此操作无法撤销！")
                .setPositiveButton("删除", (dialog, which) -> {
                    pendingDeleteItem = item;
                    pendingDeletePosition = position;

                    recycleBinManager.permanentlyDelete(item, deleteRequestLauncher, new RecycleBinManager.Callback() {
                        @Override
                        public void onSuccess() {
                            adapter.removeItem(position);
                            updateEmptyView();
                            updateTitle();
                            Toast.makeText(RecycleBinActivity.this, "已永久删除", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(RecycleBinActivity.this, "删除失败: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmEmptyAll() {
        new AlertDialog.Builder(this)
                .setTitle("清空回收站")
                .setMessage("确定要永久删除回收站中的所有项目吗？\n此操作无法撤销！")
                .setPositiveButton("清空", (dialog, which) -> {
                    recycleBinManager.emptyRecycleBin(new RecycleBinManager.Callback() {
                        @Override
                        public void onSuccess() {
                            deletedPhotos.clear();
                            adapter.notifyDataSetChanged();
                            updateEmptyView();
                            updateTitle();
                            Toast.makeText(RecycleBinActivity.this, "回收站已清空", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(RecycleBinActivity.this, "清空失败: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 刷新数据以更新剩余时间
        loadDeletedPhotos();
    }
}
