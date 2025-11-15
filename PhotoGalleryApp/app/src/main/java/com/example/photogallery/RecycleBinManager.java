package com.example.photogallery;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 回收站管理器
 * 处理软删除、恢复和永久删除操作
 */
public class RecycleBinManager {
    private static final String TAG = "RecycleBinManager";
    private Context context;
    private DeletedPhotoDao deletedPhotoDao;
    private ExecutorService executor;

    public interface Callback {
        void onSuccess();
        void onError(String error);
    }

    public interface CountCallback {
        void onResult(int count);
    }

    public interface ListCallback {
        void onResult(List<DeletedPhoto> items);
    }

    public RecycleBinManager(Context context) {
        this.context = context;
        this.deletedPhotoDao = AppDatabase.getInstance(context).deletedPhotoDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * 将照片/视频移动到回收站（软删除）
     * 只是在数据库中记录，不真正删除文件
     */
    public void moveToRecycleBin(Photo photo, Callback callback) {
        executor.execute(() -> {
            try {
                DeletedPhoto deletedPhoto = DeletedPhoto.fromPhoto(photo);
                deletedPhotoDao.insert(deletedPhoto);
                Log.d(TAG, "Moved to recycle bin: " + photo.getName());

                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error moving to recycle bin", e);
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * 从回收站恢复照片/视频
     * 只是从数据库中删除记录
     */
    public void restoreFromRecycleBin(DeletedPhoto deletedPhoto, Callback callback) {
        executor.execute(() -> {
            try {
                deletedPhotoDao.delete(deletedPhoto);
                Log.d(TAG, "Restored from recycle bin: " + deletedPhoto.getName());

                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error restoring from recycle bin", e);
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * 获取回收站中的所有项目
     */
    public void getAllDeletedItems(ListCallback callback) {
        executor.execute(() -> {
            try {
                List<DeletedPhoto> items = deletedPhotoDao.getAll();
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onResult(items));
            } catch (Exception e) {
                Log.e(TAG, "Error getting deleted items", e);
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onResult(null));
            }
        });
    }

    /**
     * 获取回收站中的项目数量
     */
    public void getDeletedCount(CountCallback callback) {
        executor.execute(() -> {
            try {
                int count = deletedPhotoDao.getCount();
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onResult(count));
            } catch (Exception e) {
                Log.e(TAG, "Error getting deleted count", e);
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onResult(0));
            }
        });
    }

    /**
     * 永久删除单个项目
     * 从MediaStore中删除文件，然后从数据库中删除记录
     */
    public void permanentlyDelete(DeletedPhoto deletedPhoto,
                                  androidx.activity.result.ActivityResultLauncher<androidx.activity.result.IntentSenderRequest> launcher,
                                  Callback callback) {
        ContentResolver resolver = context.getContentResolver();

        Uri mediaUri;
        if (deletedPhoto.isVideo()) {
            mediaUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    deletedPhoto.getMediaStoreId()
            );
        } else {
            mediaUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    deletedPhoto.getMediaStoreId()
            );
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+
                try {
                    int deletedRows = resolver.delete(mediaUri, null, null);
                    if (deletedRows > 0) {
                        // 从数据库中删除记录
                        executor.execute(() -> {
                            deletedPhotoDao.delete(deletedPhoto);
                            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                            mainHandler.post(callback::onSuccess);
                        });
                    } else {
                        // 文件可能已经不存在，直接从数据库删除
                        executor.execute(() -> {
                            deletedPhotoDao.delete(deletedPhoto);
                            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                            mainHandler.post(callback::onSuccess);
                        });
                    }
                } catch (SecurityException e) {
                    // 需要用户授权
                    android.app.PendingIntent pendingIntent =
                            MediaStore.createDeleteRequest(resolver,
                                    java.util.Collections.singletonList(mediaUri));
                    androidx.activity.result.IntentSenderRequest request =
                            new androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build();
                    launcher.launch(request);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10
                try {
                    int deletedRows = resolver.delete(mediaUri, null, null);
                    if (deletedRows > 0) {
                        executor.execute(() -> {
                            deletedPhotoDao.delete(deletedPhoto);
                            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                            mainHandler.post(callback::onSuccess);
                        });
                    } else {
                        executor.execute(() -> {
                            deletedPhotoDao.delete(deletedPhoto);
                            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                            mainHandler.post(callback::onSuccess);
                        });
                    }
                } catch (SecurityException e) {
                    if (e instanceof android.app.RecoverableSecurityException) {
                        android.app.RecoverableSecurityException recoverableException =
                                (android.app.RecoverableSecurityException) e;
                        androidx.activity.result.IntentSenderRequest request =
                                new androidx.activity.result.IntentSenderRequest.Builder(
                                        recoverableException.getUserAction().getActionIntent().getIntentSender()).build();
                        launcher.launch(request);
                    }
                }
            } else {
                // Android 9及以下
                int deletedRows = resolver.delete(mediaUri, null, null);
                executor.execute(() -> {
                    deletedPhotoDao.delete(deletedPhoto);
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(callback::onSuccess);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error permanently deleting", e);
            // 即使MediaStore删除失败，也从数据库中删除记录
            executor.execute(() -> {
                deletedPhotoDao.delete(deletedPhoto);
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(callback::onSuccess);
            });
        }
    }

    /**
     * 从数据库中删除记录（用于权限请求成功后）
     */
    public void removeFromDatabase(DeletedPhoto deletedPhoto, Callback callback) {
        executor.execute(() -> {
            try {
                deletedPhotoDao.delete(deletedPhoto);
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(callback::onSuccess);
            } catch (Exception e) {
                Log.e(TAG, "Error removing from database", e);
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * 清理过期项目（24小时后自动删除）
     * 在应用启动时调用
     */
    public void cleanupExpiredItems(Callback callback) {
        executor.execute(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                List<DeletedPhoto> expiredItems = deletedPhotoDao.getExpired(currentTime);

                Log.d(TAG, "Found " + expiredItems.size() + " expired items to delete");

                ContentResolver resolver = context.getContentResolver();

                for (DeletedPhoto item : expiredItems) {
                    Uri mediaUri;
                    if (item.isVideo()) {
                        mediaUri = ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                item.getMediaStoreId()
                        );
                    } else {
                        mediaUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                item.getMediaStoreId()
                        );
                    }

                    try {
                        // 尝试删除MediaStore中的文件
                        resolver.delete(mediaUri, null, null);
                        Log.d(TAG, "Permanently deleted: " + item.getName());
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting from MediaStore: " + item.getName(), e);
                        // 继续删除数据库记录
                    }
                }

                // 从数据库中删除所有过期记录
                int deletedCount = deletedPhotoDao.deleteExpired(currentTime);
                Log.d(TAG, "Cleaned up " + deletedCount + " expired items from database");

                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up expired items", e);
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * 清空回收站
     */
    public void emptyRecycleBin(Callback callback) {
        executor.execute(() -> {
            try {
                List<DeletedPhoto> allItems = deletedPhotoDao.getAll();
                ContentResolver resolver = context.getContentResolver();

                for (DeletedPhoto item : allItems) {
                    Uri mediaUri;
                    if (item.isVideo()) {
                        mediaUri = ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                item.getMediaStoreId()
                        );
                    } else {
                        mediaUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                item.getMediaStoreId()
                        );
                    }

                    try {
                        resolver.delete(mediaUri, null, null);
                    } catch (Exception e) {
                        Log.e(TAG, "Error deleting: " + item.getName(), e);
                    }
                }

                deletedPhotoDao.deleteAll();
                Log.d(TAG, "Emptied recycle bin");

                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error emptying recycle bin", e);
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }
}
