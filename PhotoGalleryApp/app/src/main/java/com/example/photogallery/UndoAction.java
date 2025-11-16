package com.example.photogallery;

/**
 * 撤销操作的数据模型
 * 用于记录可以撤销的删除或移到3天后操作
 */
public class UndoAction {
    public static final int TYPE_DELETE = 1;        // 删除操作
    public static final int TYPE_DELAY = 2;         // 移到3天后操作

    private int actionType;                         // 操作类型
    private Photo originalPhoto;                    // 原始照片信息
    private int originalPosition;                   // 原始位置
    private long newPhotoId;                        // 新创建的文件ID（仅用于DELAY操作）

    /**
     * 创建删除操作的撤销记录
     */
    public static UndoAction createDeleteAction(Photo photo, int position) {
        UndoAction action = new UndoAction();
        action.actionType = TYPE_DELETE;
        action.originalPhoto = photo;
        action.originalPosition = position;
        action.newPhotoId = -1;
        return action;
    }

    /**
     * 创建移到3天后操作的撤销记录
     */
    public static UndoAction createDelayAction(Photo photo, int position, long newPhotoId) {
        UndoAction action = new UndoAction();
        action.actionType = TYPE_DELAY;
        action.originalPhoto = photo;
        action.originalPosition = position;
        action.newPhotoId = newPhotoId;
        return action;
    }

    public int getActionType() {
        return actionType;
    }

    public Photo getOriginalPhoto() {
        return originalPhoto;
    }

    public int getOriginalPosition() {
        return originalPosition;
    }

    public long getNewPhotoId() {
        return newPhotoId;
    }

    public String getActionName() {
        return actionType == TYPE_DELETE ? "删除" : "移到3天后";
    }
}
