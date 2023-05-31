package com.hcmute.edu.vn.machinelearningapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider = null;
    private PreviewView view_camera;
    private ImageView view_image;
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
                view_image.setVisibility(View.VISIBLE);
                view_camera.setVisibility(View.GONE);
                tv_suggest.setVisibility(View.GONE);
                if(cameraProvider != null){
                    cameraProvider.unbindAll();
                }

            }
        });
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