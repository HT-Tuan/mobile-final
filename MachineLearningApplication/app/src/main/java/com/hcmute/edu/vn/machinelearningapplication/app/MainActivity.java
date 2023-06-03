package com.hcmute.edu.vn.machinelearningapplication.app;

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
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
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
import com.hcmute.edu.vn.machinelearningapplication.ml.AgeModelUtil;
import com.hcmute.edu.vn.machinelearningapplication.R;
import com.hcmute.edu.vn.machinelearningapplication.ml.EmotionModelUtil;

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
    private Button btn_mood;
    private Button btn_exit;
    private TextView tv_suggest;
    // model FaceDetection
    private FirebaseVisionFaceDetectorOptions options;
    private FirebaseVisionFaceDetector faceDetector;
    private FirebaseVisionImage image;
    private AgeModelUtil ageModelUtil;
    private EmotionModelUtil emotionModelUtil;
    // visualize
    private Canvas canvas = null;
    private String result = "";
    private Paint textPaint = null;
    private Paint paintBox = new Paint();
    private Rect bounds;
    private  RectF mappedBounds;
    private Bitmap detected_face = null;

    private int degreesToFirebaseRotation(int degrees) {
        Log.d("MainActivity", "degreesToFirebaseRotation");
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
        Log.d("MainActivity", "onCreate");
        ageModelUtil = AgeModelUtil.getInstance(getApplicationContext());
        emotionModelUtil = new EmotionModelUtil(getApplicationContext());
        view_image = findViewById(R.id.view_image);
        btn_camera = findViewById(R.id.btn_camera);
        btn_image = findViewById(R.id.btn_image);
        btn_age = findViewById(R.id.btn_age);
        btn_mood = findViewById(R.id.btn_mood);
        btn_exit = findViewById(R.id.btn_exit);
        tv_suggest = findViewById(R.id.tv_suggest);
        // visualize
        canvas = new Canvas();
        textPaint = new Paint();
        textPaint.setTextSize(60);
        textPaint.setColor(Color.YELLOW);
        paintBox = new Paint();
        paintBox.setColor(Color.GREEN);
        paintBox.setStyle(Paint.Style.STROKE);
        paintBox.setStrokeWidth(10f);
        mappedBounds = new RectF();
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
                action = 0;
                btn_age.setBackgroundColor(getResources().getColor(R.color.bottom_sheet_button_color));
                btn_mood.setBackgroundColor(getResources().getColor(R.color.bottom_sheet_button_color));
                if(isCamera == true){
                    isCamera = false;
                    cameraProvider.unbindAll();
                    btn_camera.setText("Camera");
                    btn_camera.setBackgroundColor(getResources().getColor(R.color.bottom_sheet_button_color));
                    view_image.setVisibility(View.GONE);
                    tv_suggest.setVisibility(View.VISIBLE);
                    return;
                }
                if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                }else{
                    if(cameraProvider != null) {
                        view_image.setImageDrawable(null);
                        startCameraX(cameraProvider);
                        view_image.setVisibility(View.VISIBLE);
                        tv_suggest.setVisibility(View.GONE);
                        btn_camera.setText("  Stop  ");
                        btn_camera.setBackgroundColor(getResources().getColor(R.color.bounding_box_color));
                        btn_image.setBackgroundColor(getResources().getColor(R.color.bottom_sheet_button_color));
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
                btn_image.setBackgroundColor(getResources().getColor(R.color.bounding_box_color));
                btn_age.setBackgroundColor(getResources().getColor(R.color.bottom_sheet_button_color));
                btn_mood.setBackgroundColor(getResources().getColor(R.color.bottom_sheet_button_color));
                if(cameraProvider != null){
                    isCamera = false;
                    cameraProvider.unbindAll();
                    btn_camera.setText("Camera");
                    btn_camera.setBackgroundColor(getResources().getColor(R.color.bottom_sheet_button_color));
                }
                openImageSelection();
            }
        });
        btn_mood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isCamera && view_image_bitmap == null){
                    Toast.makeText(v.getContext(), "Open camera or select image", Toast.LENGTH_LONG).show();
                    return;
                }
                if(action == 2){
                    btn_mood.setBackgroundColor(getResources().getColor(R.color.bottom_sheet_button_color));
                    action = 0;
                    return;
                }
                action = 2;
                btn_mood.setBackgroundColor(getResources().getColor(R.color.bounding_box_color));
                btn_age.setBackgroundColor(getResources().getColor(R.color.bottom_sheet_button_color));
                if(!isCamera){
                    analyzeImage(view_image_bitmap);
                }
            }
        });
        btn_age.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isCamera && view_image_bitmap == null){
                    Toast.makeText(v.getContext(), "Open camera or select image", Toast.LENGTH_LONG).show();
                    return;
                }
                if(action == 1){
                    btn_age.setBackgroundColor(getResources().getColor(R.color.bottom_sheet_button_color));
                    action = 0;
                    return;
                }
                action = 1;
                btn_age.setBackgroundColor(getResources().getColor(R.color.bounding_box_color));
                btn_mood.setBackgroundColor(getResources().getColor(R.color.bottom_sheet_button_color));
                if(!isCamera) {
                    analyzeImage(view_image_bitmap);
                }
            }
        });
        btn_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.finish();
                System.exit(0);
            }
        });

        //setup FaceDetector
        options = new FirebaseVisionFaceDetectorOptions.Builder()
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                 .build();
        faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options);
    }

    Executor getExecutor() {
        Log.d("MainActivity", "getExecutor");
        return ContextCompat.getMainExecutor(this);
    }

    private void openImageSelection() {
        Log.d("MainActivity", "openImageSelection");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("MainActivity", "onActivityResult");
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                view_image.setVisibility(View.VISIBLE);
                tv_suggest.setVisibility(View.GONE);
                action = 0;
                view_image_bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                analyzeImage(view_image_bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            view_image.setVisibility(View.GONE);
            tv_suggest.setVisibility(View.VISIBLE);
            btn_image.setBackgroundColor(getResources().getColor(R.color.bottom_sheet_button_color));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("MainActivity", "onRequestPermissionsResult");
        if(requestCode == CAMERA_PERMISSION_REQUEST_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if(cameraProvider != null) {
                    startCameraX(cameraProvider);
                    view_image.setVisibility(View.VISIBLE);
                    tv_suggest.setVisibility(View.GONE);
                    btn_camera.setText("  STOP  ");
                    btn_camera.setBackgroundColor(getResources().getColor(R.color.bounding_box_color));
                    btn_image.setBackgroundColor(getResources().getColor(R.color.bottom_sheet_button_color));
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
        Log.d("MainActivity", "startCameraX");
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
        Log.d("MainActivity", "analyze");
        if (imageProxy == null || imageProxy.getImage() == null) {
            Log.e("FaceAnalyzer", "Because: ImageProxy = null || ImageProxy.getImage() = null");
            return;
        }
        int rotation = degreesToFirebaseRotation(0);
        Image mediaImage = imageProxy.getImage();
        //
        image = FirebaseVisionImage.fromMediaImage(mediaImage, rotation);

        Task<List<FirebaseVisionFace>> result = faceDetector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> faces) {
//                        Log.e("FaceAnalyzer", "Found face: " + faces.size());
                        Bitmap tempBitmap = imageProxy.toBitmap().copy(Bitmap.Config.ARGB_8888, true);
                        visualize(tempBitmap, faces);
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

    public void analyzeImage(Bitmap bitmap){
        Log.d("MainActivity", "analyzeImage");
        if (bitmap == null) {
            Log.e("FaceAnalyzer", "Because: bitmap = null");
            return;
        }
        image = FirebaseVisionImage.fromBitmap(bitmap);
        Task<List<FirebaseVisionFace>> result = faceDetector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> faces) {
                        Bitmap tempBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        visualize(tempBitmap, faces);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("FaceAnalyzer", "Face detection failed: "+ e);
                    }
                });
    }
    private void visualize(Bitmap input, List<FirebaseVisionFace> faces){
        Log.d("MainActivity", "start visualize");
        try {
            canvas.setBitmap(input);
            //
            for (FirebaseVisionFace face: faces){
                bounds = face.getBoundingBox();
                mappedBounds.left = bounds.left;
                mappedBounds.top = bounds.top ;
                mappedBounds.right = bounds.right;
                mappedBounds.bottom = bounds.bottom;
                canvas.drawRect(mappedBounds, paintBox);
                detected_face = Bitmap.createBitmap(input, bounds.left, bounds.top, bounds.width(), bounds.height());
                result = "";
                switch (action){
                    case 1:
                        Log.d("MainActivity", "visualize Age");
                        result = String.valueOf(ageModelUtil.predictAge(detected_face));
                        break;
                    case 2:
                        //Emotion
                        Log.d("MainActivity", "visualize Emotion");
                        detected_face = toGrayscale(detected_face);
                        detected_face = Bitmap.createScaledBitmap(detected_face, 48, 48, true);
                        result = emotionModelUtil.predictEmotion(detected_face);
                        break;
                }
                canvas.drawText(result, mappedBounds.left, mappedBounds.top - 20, textPaint);
            }
            canvas.drawText("Detected: " + faces.size(), 1, 60, textPaint);
        }
        catch (Exception ex){
            Log.e("MainActivity", "Error visualize: " + ex.getMessage());
        }
        view_image.setImageBitmap(input);
        Log.d("MainActivity", "finish visualize");
    }
    private Bitmap toGrayscale(Bitmap bmpOriginal) {
        Log.d("MainActivity", "toGrayscale");
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
}