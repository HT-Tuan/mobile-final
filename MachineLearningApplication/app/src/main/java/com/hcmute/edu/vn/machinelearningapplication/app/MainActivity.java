package com.hcmute.edu.vn.machinelearningapplication.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.hcmute.edu.vn.machinelearningapplication.R;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private static final int PICK_IMAGE_REQUEST = 1;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider = null;
    private PreviewView view_camera;
    private ImageView view_image;
    private Bitmap view_image_bitmap = null;
    private Button btn_camera;
    private Button btn_image;
    private TextView tv_suggest;
    private Preview preview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        view_camera = findViewById(R.id.view_camera);
        view_image = findViewById(R.id.view_image);
        btn_camera = findViewById(R.id.btn_camera);
        btn_image = findViewById(R.id.btn_image);
        tv_suggest = findViewById(R.id.tv_suggest);
        //
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() ->{
            try{
                cameraProvider = cameraProviderFuture.get();
            }catch (ExecutionException | InterruptedException e){
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
        //
        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view_camera.setVisibility(View.VISIBLE);
                view_image.setVisibility(View.GONE);
                if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                }else{
                    if(cameraProvider != null) {
                        startCameraX(cameraProvider);
                        tv_suggest.setVisibility(View.GONE);
                    }
                    else{
                        Toast.makeText(v.getContext(), "camera start error", Toast.LENGTH_LONG).show();
                        view_camera.setVisibility(View.GONE);
                        tv_suggest.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
        btn_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view_image_bitmap = null;
                view_image.setImageDrawable(null);
                view_image.setVisibility(View.VISIBLE);
                view_camera.setVisibility(View.GONE);
                tv_suggest.setVisibility(View.GONE);
                if(cameraProvider != null){
                    cameraProvider.unbindAll();
                }
                openImageSelection();
            }
        });
    }

    private void openImageSelection() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            displaySelectedImage(imageUri);
        }
    }

    private void displaySelectedImage(Uri imageUri) {
        try {
            view_image_bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            view_image.setImageBitmap(view_image_bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == CAMERA_PERMISSION_REQUEST_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                view_camera.setVisibility(View.VISIBLE);
                view_image.setVisibility(View.GONE);
                if(cameraProvider != null) {
                    startCameraX(cameraProvider);
                    tv_suggest.setVisibility(View.GONE);
                }
                else{
                    Toast.makeText(this, "camera start error", Toast.LENGTH_LONG).show();
                    view_camera.setVisibility(View.GONE);
                    tv_suggest.setVisibility(View.VISIBLE);
                }
            } else {
                view_camera.setVisibility(View.GONE);
                tv_suggest.setVisibility(View.VISIBLE);
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview = new Preview.Builder().build();
        preview.setSurfaceProvider(view_camera.getSurfaceProvider());



        cameraProvider.bindToLifecycle(this, cameraSelector, preview);
    }
}