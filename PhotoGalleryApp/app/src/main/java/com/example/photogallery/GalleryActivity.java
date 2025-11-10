package com.example.photogallery;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    private RecyclerView recyclerViewPhotos;
    private PhotoAdapter photoAdapter;
    private List<Photo> photos;
    private String folderName;
    private String folderDisplayName;
    private boolean isDateFolder;
    private ActivityResultLauncher<Intent> imageViewerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        // 注册ImageViewerActivity的结果监听器
        imageViewerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // ImageViewerActivity返回OK，说明有变化，传递给MainActivity
                        setResult(RESULT_OK);
                        loadPhotos(); // 刷新当前列表
                    }
                }
        );

        folderName = getIntent().getStringExtra("folder_name");
        folderDisplayName = getIntent().getStringExtra("folder_display_name");
        isDateFolder = getIntent().getBooleanExtra("is_date_folder", false);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(folderDisplayName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerViewPhotos = findViewById(R.id.recyclerViewPhotos);
        recyclerViewPhotos.setLayoutManager(new GridLayoutManager(this, 3));

        loadPhotos();
    }

    private void loadPhotos() {
        photos = new ArrayList<>();
        PhotoManager photoManager = new PhotoManager(this);

        if (isDateFolder) {
            // 加载日期文件夹中的照片（基于DATE_ADDED+3天）
            photos = photoManager.getPhotosForDate(folderName);
        } else {
            // 加载所有照片
            photos = photoManager.getAllPhotos();
        }

        photoAdapter = new PhotoAdapter(this, photos, position -> {
            Intent intent = new Intent(GalleryActivity.this, ImageViewerActivity.class);
            intent.putExtra("photos", (ArrayList<Photo>) photos);
            intent.putExtra("position", position);
            intent.putExtra("folder_name", folderName);
            intent.putExtra("is_date_folder", isDateFolder);
            imageViewerLauncher.launch(intent);
        });

        recyclerViewPhotos.setAdapter(photoAdapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPhotos();
    }
}
