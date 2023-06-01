package com.hcmute.edu.vn.machinelearningapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements ImageAnalysis.Analyzer {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private static final int PICK_IMAGE_REQUEST = 1;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider = null;
    private ImageView view_image;
    private Bitmap view_image_bitmap = null;
    private Button btn_camera;
    private Button btn_image;
    private TextView tv_suggest;
    // model FaceDetection
    private FirebaseVisionFaceDetectorOptions options;
    private FirebaseVisionFaceDetector faceDetector;
    private FirebaseVisionImage image;
    private int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException(
                        "Rotation must be 0, 90, 180, or 270.");
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
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
        }, getExecutor());
        //
        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                }else{
                    if(cameraProvider != null) {
                        startCameraX(cameraProvider);
                        view_image.setVisibility(View.VISIBLE);
                        tv_suggest.setVisibility(View.GONE);
                    }
                    else{
                        Toast.makeText(v.getContext(), "camera start error", Toast.LENGTH_LONG).show();
                        view_image.setVisibility(View.GONE);
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
                tv_suggest.setVisibility(View.GONE);
                if(cameraProvider != null){
                    cameraProvider.unbindAll();
                }
                openImageSelection();
            }
        });

        //setup FaceDetector
        options = new FirebaseVisionFaceDetectorOptions.Builder()
                .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                .build();
        faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options);
    }
    Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
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
                if(cameraProvider != null) {
                    startCameraX(cameraProvider);
                    view_image.setVisibility(View.VISIBLE);
                    tv_suggest.setVisibility(View.GONE);
                }
                else{
                    Toast.makeText(this, "camera start error", Toast.LENGTH_LONG).show();
                    view_image.setVisibility(View.GONE);
                    tv_suggest.setVisibility(View.VISIBLE);
                }
            } else {
                view_image.setVisibility(View.GONE);
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

        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
        imageAnalysis.setAnalyzer(getExecutor(), this);


        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis);
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (imageProxy == null || imageProxy.getImage() == null) {
            Log.e("FaceAnalyzer", "Because: ImageProxy = null || ImageProxy.getImage() = null");
            return;
        }
        int rotation = degreesToFirebaseRotation(0);
        Image mediaImage = imageProxy.getImage();
        //
        image = FirebaseVisionImage.fromMediaImage(mediaImage, rotation);
        //
        final Bitmap bitmap = imageProxy.toBitmap();

        //
        Task<List<FirebaseVisionFace>> result = faceDetector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> faces) {
                        Log.e("FaceAnalyzer", "Found face: " + faces.size());
                        Bitmap tempBitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
                        Canvas canvas = new Canvas(tempBitmap);
                        Paint paint = new Paint();
                        paint.setColor(Color.GREEN);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(5f);

                        for (FirebaseVisionFace face : faces) {
                            Rect bounds = face.getBoundingBox();
                            RectF mappedBounds = new RectF();
                            mappedBounds.left = bounds.left;
                            mappedBounds.top = bounds.top ;
                            mappedBounds.right = bounds.right;
                            mappedBounds.bottom = bounds.bottom;

                            canvas.drawRect(mappedBounds, paint);
                        }
                        view_image.setImageBitmap(tempBitmap);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("FaceAnalyzer", "Face detection failed: "+ e);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<FirebaseVisionFace>> task) {
                        imageProxy.close();
                    }
                });
    }
}