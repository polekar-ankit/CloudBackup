package com.example.cloudbackup;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.util.Collections;
//
//***********************note: ref links
// https://github.com/gsuitedevs/android-samples/blob/master/drive/deprecation/app/src/main/java/com/google/android/gms/drive/sample/driveapimigration/MainActivity.java
//https://developers.google.com/drive/api/v3/manage-uploads
//https://developers.google.com/drive/api/v3/ref-search-terms
//
//
//If the format of the content can't be detected, then the MIME type will be set to 'application/octet-stream'.
//
//
//*******file browser**************
//https://android-arsenal.com/details/1/6982

public class MainActivity extends AppCompatActivity {


    private static final int GOOGLE_SIGN_REQ = 121;
    private static final int FILE_PICKER = 122;
    GDriveDataManager GDriveDataManager;
    TextView tvMessage, tvFileList;
    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvMessage = findViewById(R.id.tv_msg);
        tvFileList = findViewById(R.id.tv_file_list);
        requestSignin();

        findViewById(R.id.btn_create_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GDriveDataManager == null) {
                    requestSignin();
                    return;
                }
                GDriveDataManager.createFile().addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        tvMessage.setText(String.format("%s has been created successfully", s));
                    }
                });
            }
        });

        findViewById(R.id.btn_check_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (GDriveDataManager == null) {
                    requestSignin();
                    return;
                }
                GDriveDataManager.getDriveFile().addOnSuccessListener(fileList -> {
                    StringBuilder sfileList = new StringBuilder();
                    for (File file :
                            fileList.getFiles()) {
                        MainActivity.this.file = file;
                        sfileList.append(file.getName()).append(",\n");
                    }
                    tvFileList.setText(sfileList.toString());
                });
            }
        });

        findViewById(R.id.btn_app_folder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (GDriveDataManager == null) {
                    requestSignin();
                    return;
                }
                GDriveDataManager.createAppFolder().addOnSuccessListener(s -> tvMessage.setText(String.format("%s has been created successfully", s)));
            }
        });

        findViewById(R.id.btn_get_app_folder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (GDriveDataManager == null) {
                    requestSignin();
                    return;
                }
                GDriveDataManager.getAppBackUpCouldFolder().addOnSuccessListener(new OnSuccessListener<FileList>() {
                    @Override
                    public void onSuccess(FileList fileList) {
                        File appDir = null;
                        for (File file : fileList.getFiles()) {
                            if (file.getName().equals("CloudBackup")) {
                                appDir = file;
                            }
                        }
                        tvMessage.setText("App Dir found status :" + (appDir != null));
                    }
                });
            }
        });
        tvFileList.setOnClickListener(view -> {
            if (GDriveDataManager != null)
                GDriveDataManager.downloadFile(file)
                        .addOnSuccessListener(outputStream -> {
                            if (outputStream != null)
                                tvMessage.setText(String.format("%s has been successfully downloaded", file.getName()));
                        });
        });

        findViewById(R.id.btn_pick).setOnClickListener(view -> new ChooserDialog(MainActivity.this)
                .withFilter(false, true)
                .withStartFile(Environment.getExternalStorageDirectory().getAbsolutePath())
                // to handle the result(s)
                .withChosenListener((path, pathFile) -> GDriveDataManager.uploadFile(path)
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(String s) {
                                tvMessage.setText(s);
                            }
                        }))
                .build()
                .show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_SIGN_REQ) {
//            if (resultCode == RESULT_OK && data != null) {
            handleIntent(data);
//            }
        }

    }

    private void handleIntent(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                        tvMessage.setText(e.getLocalizedMessage());
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                    @Override
                    public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                        GoogleAccountCredential googleAccountCredential = GoogleAccountCredential.
                                usingOAuth2(MainActivity.this, Collections.singleton(DriveScopes.DRIVE_FILE));
                        googleAccountCredential.setSelectedAccount(googleSignInAccount.getAccount());
                        NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();


                        Drive drive = new Drive.Builder(HTTP_TRANSPORT
                                , new GsonFactory(), googleAccountCredential)
//                                    .setApplicationName("PosBilling")
                                .setApplicationName("CloudBackup")
                                .build();
                        GDriveDataManager = new GDriveDataManager(drive);

                    }
                });
    }

    void requestSignin() {
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
        startActivityForResult(googleSignInClient.getSignInIntent(), GOOGLE_SIGN_REQ);
    }
}
