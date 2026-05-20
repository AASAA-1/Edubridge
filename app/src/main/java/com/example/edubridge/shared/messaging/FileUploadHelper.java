package com.example.edubridge.shared.messaging;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.UUID;

public class FileUploadHelper {

    private final FirebaseStorage storage;
    private final Context context;

    public FileUploadHelper() {
        this.storage = FirebaseStorage.getInstance();
        this.context = null;
    }

    public interface UploadCallback {
        void onProgress(double progress);
        void onSuccess(String downloadUrl, String fileName, long fileSize);
        void onFailure(Exception e);
    }

    public void uploadFile(Uri fileUri, UploadCallback callback) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString());
        String fileName = "chat_files/" + UUID.randomUUID().toString() + "." + extension;

        StorageReference fileRef = storage.getReference().child(fileName);

        UploadTask uploadTask = fileRef.putFile(fileUri);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) /
                    taskSnapshot.getTotalByteCount();
            callback.onProgress(progress);
        }).addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                long fileSize = taskSnapshot.getTotalByteCount();
                String originalName = fileUri.getLastPathSegment();
                callback.onSuccess(downloadUri.toString(), originalName, fileSize);
            });
        }).addOnFailureListener(callback::onFailure);
    }

    private String getMimeType(Uri uri) {
        String mimeType = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            mimeType = context.getContentResolver().getType(uri);
        } else {
            String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return mimeType;
    }
}