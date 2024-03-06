package com.example.mlseriesdemonstrator.object;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;

import com.example.mlseriesdemonstrator.R;
import com.example.mlseriesdemonstrator.helpers.MLVideoHelperActivity;
import com.example.mlseriesdemonstrator.helpers.vision.VisionBaseProcessor;
import com.example.mlseriesdemonstrator.helpers.vision.recogniser.FaceRecognitionProcessor;
import com.google.mlkit.vision.face.Face;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.io.InputStream;

public class FaceRecognitionActivity extends MLVideoHelperActivity implements FaceRecognitionProcessor.FaceRecognitionCallback {

    private Interpreter faceNetInterpreter;
    private FaceRecognitionProcessor faceRecognitionProcessor;

    private Face face;
    private Bitmap faceBitmap;
    private float[] faceVector;
    private boolean isButtonAddFace = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // next line I want modified!
//        makeAddFaceVisible();
        // Enable the process static image button
        enableProcessStaticImageButton();
    }

    @Override
    protected VisionBaseProcessor setProcessor() {
        try {
            faceNetInterpreter = new Interpreter(FileUtil.loadMappedFile(this, "mobile_face_net.tflite"), new Interpreter.Options());
        } catch (IOException e) {
            e.printStackTrace();
        }

        faceRecognitionProcessor = new FaceRecognitionProcessor(
                faceNetInterpreter,
                graphicOverlay,
                this
        );
        faceRecognitionProcessor.activity = this;
        return faceRecognitionProcessor;
    }

    public void setTestImage(Bitmap cropToBBox) {
        if (cropToBBox == null) {
            return;
        }
        runOnUiThread(() -> ((ImageView) findViewById(R.id.testImageView)).setImageBitmap(cropToBBox));
    }

    // this is called if detectInImage was successful (a face was returned)
    @Override
    public void onFaceDetected(Face face, Bitmap faceBitmap, float[] faceVector) {
        this.face = face;
        this.faceBitmap = faceBitmap;
        this.faceVector = faceVector;
        makeAddFaceVisible();

//        if (!isButtonAddFace) {
//            makeAddFaceVisible();
//            this.isButtonAddFace = true;
//        }
    }

    @Override
    public void onFaceRecognised(Face face, float probability, String name) {

    }

    @Override
    public void onAddFaceClicked(View view) {
        super.onAddFaceClicked(view);

        // checks if there is a face stored
        if (face == null || faceBitmap == null) {
            Log.e("FaceRecognitionActivity", "addFace button clicked, but no face found");
            return;
        }

        Face tempFace = face;
        Bitmap tempBitmap = faceBitmap;
        float[] tempVector = faceVector;

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.add_face_dialog, null);
        ((ImageView) dialogView.findViewById(R.id.dlg_image)).setImageBitmap(tempBitmap);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Editable input  = ((EditText) dialogView.findViewById(R.id.dlg_input)).getEditableText();
                if (input.length() > 0) {
                    faceRecognitionProcessor.registerFace(input, tempVector);
                }
            }
        });
        builder.show();
    }

    public void processStaticImage(String assetImagePath) {
        try {
            // Load the bitmap from the assets folder
            InputStream is = getAssets().open(assetImagePath);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close(); // Remember to close the InputStream

            if (bitmap != null) {
                // Proceed with processing the bitmap as before
                FaceRecognitionProcessor processor = faceRecognitionProcessor;
                processor.detectAndProcessStaticImage(bitmap, new FaceRecognitionProcessor.FaceRecognitionCallback() {
                    @Override
                    public void onFaceRecognised(Face face, float probability, String name) {
                        // Handle recognized face, if needed
                    }

                    @Override
                    public void onFaceDetected(Face face, Bitmap faceBitmap, float[] vector) {
                        // Here you can display the face or save it
                        setTestImage(faceBitmap); // Display the detected face image
                        FaceRecognitionActivity.this.onFaceDetected(face, faceBitmap, vector);
                    }
                });
            } else {
                Log.e("FaceRecognitionActivity", "Failed to decode asset into bitmap.");
            }
        } catch (IOException e) {
            Log.e("FaceRecognitionActivity", "Error loading image from assets", e);
        }
    }

}
