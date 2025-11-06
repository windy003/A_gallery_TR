package com.example.photogallery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private RecyclerView recyclerViewFolders;
    private FolderAdapter folderAdapter;
    private List<Folder> folders;
    private PhotoManager photoManager;
    private DateFolderManager dateFolderManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerViewFolders = findViewById(R.id.recyclerViewFolders);
        recyclerViewFolders.setLayoutManager(new LinearLayoutManager(this));

        photoManager = new PhotoManager(this);
        dateFolderManager = new DateFolderManager(this);

        if (checkPermissions()) {
            loadFolders();
        } else {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFolders();
            } else {
                Toast.makeText(this, "需要存储权限才能查看照片", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadFolders() {
        folders = new ArrayList<>();

        // 添加"所有图片"文件夹
        List<Photo> allPhotos = photoManager.getAllPhotos();
        Folder allPhotosFolder = new Folder("all_photos", "所有图片");
        for (Photo photo : allPhotos) {
            allPhotosFolder.addPhoto(photo);
        }
        folders.add(allPhotosFolder);

        // 添加今天的日期文件夹
        String todayDate = DateFolderManager.getTodayDate();
        List<String> todayPhotoPaths = dateFolderManager.getPhotosForDate(todayDate);
        if (!todayPhotoPaths.isEmpty()) {
            Folder todayFolder = new Folder(todayDate, todayDate);
            todayFolder.setDateFolder(true);
            for (String photoPath : todayPhotoPaths) {
                // 从allPhotos中找到对应的Photo对象
                for (Photo photo : allPhotos) {
                    if (photo.getPath().equals(photoPath)) {
                        todayFolder.addPhoto(photo);
                        break;
                    }
                }
            }
            folders.add(todayFolder);
        }

        // 添加其他日期文件夹
        List<String> dateFolders = dateFolderManager.getAllDateFolders();
        for (String date : dateFolders) {
            if (!date.equals(todayDate)) {
                List<String> photoPaths = dateFolderManager.getPhotosForDate(date);
                if (!photoPaths.isEmpty()) {
                    Folder folder = new Folder(date, date);
                    folder.setDateFolder(true);
                    for (String photoPath : photoPaths) {
                        for (Photo photo : allPhotos) {
                            if (photo.getPath().equals(photoPath)) {
                                folder.addPhoto(photo);
                                break;
                            }
                        }
                    }
                    folders.add(folder);
                }
            }
        }

        folderAdapter = new FolderAdapter(this, folders, folder -> {
            Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
            intent.putExtra("folder_name", folder.getName());
            intent.putExtra("folder_display_name", folder.getDisplayName());
            intent.putExtra("is_date_folder", folder.isDateFolder());
            startActivity(intent);
        });

        recyclerViewFolders.setAdapter(folderAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions()) {
            loadFolders();
        }
    }
}
