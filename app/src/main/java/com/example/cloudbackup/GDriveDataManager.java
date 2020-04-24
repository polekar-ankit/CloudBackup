package com.example.cloudbackup;

import android.os.Environment;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Ankit on 07-Nov-19.
 */
public class GDriveDataManager {
    private final Drive mDriveService;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private File appCludDir;
    GDriveDataManager(Drive mDriveService) {
        this.mDriveService = mDriveService;
        getAppBackUpCouldFolder().addOnSuccessListener(new OnSuccessListener<FileList>() {
            @Override
            public void onSuccess(FileList fileList) {
                for (File file : fileList.getFiles()) {
                    if (file.getName().equals("CloudBackup")) {
                        appCludDir = file;
                    }
                }
            }
        });
    }

    private static java.io.File createBackupDirectory(String fileName) throws IOException {
        java.io.File file = new java.io.File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/CloudBackup");
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return null;
            }
        }
        java.io.File backDir = new java.io.File(file.getAbsolutePath() + "/" + fileName);
        if (backDir.createNewFile())
            return backDir;
        else
            return backDir;

    }

    Task<String> createAppFolder() {
        return Tasks.call(mExecutor, new Callable<String>() {
            @Override
            public String call() throws Exception {
                File fileMetaData = new File();
                fileMetaData.setName("CloudBackup");
                fileMetaData.setMimeType("application/vnd.google-apps.folder");
                File file = mDriveService.files().create(fileMetaData).execute();
                return file.getId();
            }
        });
    }

    Task<String> createFile() {
        return Tasks.call(mExecutor, () -> {
            File file = new File().setParents(Collections.singletonList("root"))
                    .setMimeType("text/plain")
                    .setParents(Collections.singletonList(appCludDir.getId()))
                    .setName("Test File");
            File dFile = mDriveService.files().create(file).execute();
            return dFile.getId();
        });
    }

    Task<String> uploadFile(final String sfilePath) {
        return Tasks.call(mExecutor, () -> {

            java.io.File filePath = new java.io.File(sfilePath);
            File fileMetadata = new File();
            fileMetadata.setName(filePath.getName());
            fileMetadata.setParents(Collections.singletonList(appCludDir.getId()));
//                FileContent mediaContent = new FileContent("image/jpeg", filePath);
            FileContent mediaContent = new FileContent("application/octet-stream", filePath);
            File file = mDriveService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();

            return file.getId();
        });
    }

    Task<FileList> getAppBackUpCouldFolder() {
        return Tasks.call(mExecutor, new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {
                return mDriveService.files().list()
                        .setQ("name='CloudBackup' and mimeType = 'application/vnd.google-apps.folder'")
                        .setSpaces("drive").execute();
            }
        });
    }

    Task<FileList> getDriveFile() {
        return Tasks.call(mExecutor, () -> mDriveService.files().list()
                .setQ("name contains 'PADB'")
                .setSpaces("drive").execute());
    }

    Task<OutputStream> downloadFile(File downloadingFile) {
        return Tasks.call(mExecutor, () -> {
            java.io.File file = createBackupDirectory(downloadingFile.getName());
            if (file != null) {
                FileOutputStream outputStream = new FileOutputStream(file);
                mDriveService.files().get(downloadingFile.getId())
                        .executeMediaAndDownloadTo(outputStream);
                return outputStream;
            } else {
                return null;
            }
        });
    }

}
