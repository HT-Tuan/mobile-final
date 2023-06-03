package com.hcmute.edu.vn.machinelearningapplication.ml;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;

import com.hcmute.edu.vn.machinelearningapplication.ml.EmotionModel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class EmotionModelUtil {
    private static final String[] emotion_classes = {"TucGian", "ChanNan", "SoHai", "HanhPhuc", "BinhThuong", "Buon", "NgacNhien"};
    private static final int IMAGE_SIZE = 48;
    private EmotionModel emotionModel = null;;
    private TensorBuffer inputFeature0 = null;
    private TensorBuffer outputAgeBuffer = null;
    public EmotionModelUtil(Context context){
        try {
            emotionModel = EmotionModel.newInstance(context);
            inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 48, 48, 1}, DataType.FLOAT32);
        }catch (Exception e){
            Log.e("Emotion", "Error initializing EmotionModelUtil: " + e.getMessage());
        }
    }
    public String predictEmotion(Bitmap detected_face){
        try {
            ByteBuffer input = ByteBuffer.allocateDirect(48 * 48 * 4).order(ByteOrder.nativeOrder());
            for (int y = 0; y < detected_face.getHeight(); y++){
                for (int x = 0; x < detected_face.getWidth(); x++){
                    int px = detected_face.getPixel(x, y);
                    float pxValue = (float) Color.red(px);
                    input.putFloat(pxValue);
                }
            }
            inputFeature0.loadBuffer(input);
            EmotionModel.Outputs outputs = emotionModel.process(inputFeature0);
            outputAgeBuffer = outputs.getOutputFeature0AsTensorBuffer();
            float[] outputValues = outputAgeBuffer.getFloatArray();
            int maxPos = 0;
            float maxValue = 0;
            for (int i = 0; i < outputValues.length; i++){
                if(outputValues[i] > maxValue){
                    maxValue = outputValues[i];
                    maxPos = i;
                }
            }
            return emotion_classes[maxPos];
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return "";
    }
}
