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
import android.graphics.drawable.Drawable;
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
    private static int action = 0;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider = null;
    private boolean isCamera = false;
    private ImageView view_image;
    private Bitmap view_image_bitmap = null;
    private Button btn_camera;
    private Button btn_image;
    private Button btn_age;
    private TextView tv_suggest;
    // model FaceDetection
    private FirebaseVisionFaceDetectorOptions options;
    private FirebaseVisionFaceDetector faceDetector;
    private FirebaseVisionImage image;
    private AgeModelUtil ageModelUtil;
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
        ageModelUtil = new AgeModelUtil(getApplicationContext());
        view_image = findViewById(R.id.view_image);
        btn_camera = findViewById(R.id.btn_camera);
        btn_image = findViewById(R.id.btn_image);
        btn_age = findViewById(R.id.btn_age);
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
                if(isCamera == true){
                    isCamera = false;
                    cameraProvider.unbindAll();
                    btn_camera.setText("Camera");
                    view_image.setVisibility(View.GONE);
                    tv_suggest.setVisibility(View.VISIBLE);
                    return;
                }
                if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                }else{
                    if(cameraProvider != null) {
                        action = 0;
                        view_image.setImageDrawable(null);
                        startCameraX(cameraProvider);
                        view_image.setVisibility(View.VISIBLE);
                        tv_suggest.setVisibility(View.GONE);
                        btn_camera.setText("  Stop  ");
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
                action = 0;
                view_image_bitmap = null;
                view_image.setImageDrawable(null);
                view_image.setVisibility(View.VISIBLE);
                tv_suggest.setVisibility(View.GONE);
                if(cameraProvider != null){
                    isCamera = false;
                    cameraProvider.unbindAll();
                    btn_camera.setText("Camera");
                }
                openImageSelection();
            }
        });
        btn_age.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action = 1;
                detectAge(view_image_bitmap);
            }
        });

        //setup FaceDetector
        options = new FirebaseVisionFaceDetectorOptions.Builder()
                .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                .build();
        faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options);
    }

    private void detectAge(Bitmap bitmap) {
        if(bitmap == null)
            return;
        detectFaces(bitmap, action);
        view_image.setVisibility(View.VISIBLE);
        tv_suggest.setVisibility(View.GONE);
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
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                view_image.setImageBitmap(bitmap);
                view_image_bitmap = bitmap;
                detectFaces(bitmap, 0);
                view_image.setVisibility(View.VISIBLE);
                tv_suggest.setVisibility(View.GONE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void detectFaces(Bitmap bitmap, int action) {
        if (bitmap != null) {
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
            FirebaseVisionFaceDetectorOptions options =
                    new FirebaseVisionFaceDetectorOptions.Builder()
                            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.NO_LANDMARKS)
                            .setClassificationMode(FirebaseVisionFaceDetectorOptions.NO_CLASSIFICATIONS)
                            .setMinFaceSize(0.1f)
                            .build();

            FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                    .getVisionFaceDetector(options);

            detector.detectInImage(image)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionFace> faces) {
                            if (action == 0)
                                processFaces(faces, bitmap);
                            else if (action == 1)
                                processAgeFaces(faces, bitmap);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "Failed to detect faces", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
    private void processFaces(List<FirebaseVisionFace> faces, Bitmap bitmap) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);

        for (FirebaseVisionFace face : faces) {
            RectF boundingBox = new RectF(face.getBoundingBox());
            canvas.drawRect(boundingBox, paint);
        }
        view_image.setImageBitmap(mutableBitmap);
    }
    private void processAgeFaces(List<FirebaseVisionFace> faces, Bitmap bitmap) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setTextSize(36f);

        for (FirebaseVisionFace face : faces) {
            RectF boundingBox = new RectF(face.getBoundingBox());
            canvas.drawRect(boundingBox, paint);

            // Crop the image inside the bounding box
            int left = (int) boundingBox.left;
            int top = (int) boundingBox.top;
            int right = (int) boundingBox.right;
            int bottom = (int) boundingBox.bottom;
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
            int apparentAge = ageModelUtil.predictAge(croppedBitmap);

            // Display the predicted age label on top of the bounding box
            String ageLabel = "Age: " + apparentAge;
            float textWidth = paint.measureText(ageLabel);
            float x = boundingBox.left;
            float y = boundingBox.top - 10f; // Adjust the y-coordinate for the desired position
            canvas.drawText(ageLabel, x, y, paint);
        }

        view_image.setImageBitmap(mutableBitmap);
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
                    btn_camera.setText("  STOP  ");
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
        isCamera = true;
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
                        Bitmap tempBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        if(action == 0)
                            processFaces(faces, tempBitmap);
                        else if (action == 1)
                            processAgeFaces(faces, tempBitmap);
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