package com.example.photogallery;

import android.content.Intent;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

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

        if (isDateFolder) {
            // 加载日期文件夹中的照片
            DateFolderManager dateFolderManager = new DateFolderManager(this);
            List<String> photoPaths = dateFolderManager.getPhotosForDate(folderName);
            PhotoManager photoManager = new PhotoManager(this);
            List<Photo> allPhotos = photoManager.getAllPhotos();

            for (String photoPath : photoPaths) {
                for (Photo photo : allPhotos) {
                    if (photo.getPath().equals(photoPath)) {
                        photos.add(photo);
                        break;
                    }
                }
            }
        } else {
            // 加载所有照片
            PhotoManager photoManager = new PhotoManager(this);
            photos = photoManager.getAllPhotos();
        }

        photoAdapter = new PhotoAdapter(this, photos, position -> {
            Intent intent = new Intent(GalleryActivity.this, ImageViewerActivity.class);
            intent.putExtra("photos", (ArrayList<Photo>) photos);
            intent.putExtra("position", position);
            intent.putExtra("folder_name", folderName);
            intent.putExtra("is_date_folder", isDateFolder);
            startActivity(intent);
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
