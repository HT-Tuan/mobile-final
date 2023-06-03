package com.hcmute.edu.vn.machinelearningapplication.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class AgeModelUtil {
    private static final String TAG = "AgeModelUtil";
    private static final String MODEL_FILE_PATH = "age_model.tflite";
    private static final int IMAGE_SIZE = 224;
    private static final int NUM_CLASSES = 101;

    private static AgeModelUtil instance;  // Singleton instance

    private Interpreter interpreter;
    private TensorImage inputImageBuffer;
    private TensorBuffer outputAgeBuffer;

    // Private constructor to prevent direct instantiation
    private AgeModelUtil(Context context) {
        try {
            // Initialize the TensorFlow Lite Interpreter
            Interpreter.Options options = new Interpreter.Options();
            MappedByteBuffer modelFileBuffer = loadModelFile(context);
            interpreter = new Interpreter(modelFileBuffer, options);

            // Initialize input and output tensors
            DataType inputDataType = interpreter.getInputTensor(0).dataType();
            inputImageBuffer = new TensorImage(inputDataType);
            DataType outputDataType = interpreter.getOutputTensor(0).dataType();
            outputAgeBuffer = TensorBuffer.createFixedSize(new int[]{1, NUM_CLASSES}, outputDataType);
        } catch (IOException e) {
            Log.e(TAG, "Error initializing AgeModelUtil: " + e.getMessage());
        }
    }

    // Method to get the singleton instance
    public static AgeModelUtil getInstance(Context context) {
        if (instance == null) {
            instance = new AgeModelUtil(context.getApplicationContext());
        }
        return instance;
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILE_PATH);

        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());

        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public int predictAge(Bitmap inputBitmap) {
        // Preprocess the input image
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(inputBitmap, IMAGE_SIZE, IMAGE_SIZE, true);
        inputImageBuffer.load(resizedBitmap);

        // Run inference
        interpreter.run(inputImageBuffer.getBuffer(), outputAgeBuffer.getBuffer());

        // Process the output to determine the apparent age
        float[] ageProbabilities = outputAgeBuffer.getFloatArray();
        int apparentAge = 0;
        float maxProbability = 0f;
        for (int i = 0; i < NUM_CLASSES; i++) {
            if (ageProbabilities[i] > maxProbability) {
                maxProbability = ageProbabilities[i];
                apparentAge = i;
            }
        }

        return apparentAge;
    }
}
