package com.example.photogallery;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.List;

public class ImageViewerActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private LinearLayout layoutControls;
    private Button buttonAddToThreeDaysLater;
    private TextView textViewPageInfo;
    private List<Photo> photos;
    private int currentPosition;
    private DateFolderManager dateFolderManager;
    private boolean controlsVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        viewPager = findViewById(R.id.viewPager);
        layoutControls = findViewById(R.id.layoutControls);
        buttonAddToThreeDaysLater = findViewById(R.id.buttonAddToThreeDaysLater);
        textViewPageInfo = findViewById(R.id.textViewPageInfo);

        photos = (ArrayList<Photo>) getIntent().getSerializableExtra("photos");
        currentPosition = getIntent().getIntExtra("position", 0);

        dateFolderManager = new DateFolderManager(this);

        setupViewPager();
        setupControls();
    }

    private void setupViewPager() {
        ImagePagerAdapter adapter = new ImagePagerAdapter(this, photos);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition, false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPosition = position;
                updatePageInfo();
            }
        });

        updatePageInfo();

        // 点击图片切换控制栏显示/隐藏
        viewPager.setOnClickListener(v -> toggleControls());
    }

    private void setupControls() {
        buttonAddToThreeDaysLater.setOnClickListener(v -> addToThreeDaysLater());
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
