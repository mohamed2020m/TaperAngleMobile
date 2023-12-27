package com.leeuw.draftangledetector;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class CActivity extends AppCompatActivity {
    private ImageView selectedImage;
    private Button cancel, proceed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_c);

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed.");
        } else {
            Log.d("OpenCV", "OpenCV initialization succeeded.");
        }

        init();

        // Get the image URI from the intent
        Uri imageUri = Uri.parse(getIntent().getStringExtra("IMAGE_URI"));

        Bitmap bitmap = processSelectedImage(imageUri);

        // Set the image in the ImageView
        selectedImage.setImageBitmap(bitmap);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        proceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Process the image
//                processImage(imageUri);

                Intent resultIntent = new Intent(CActivity.this, ResultActivity.class);
                resultIntent.putExtra("imageUri", imageUri.toString());
                startActivity(resultIntent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

//                // Open ResultActivity and pass the processed image Mat
//                Intent intent = new Intent(CActivity.this, ResultActivity.class);
//                intent.putExtra("PROCESSED_IMAGE", inputMat.getNativeObjAddr());
//                startActivity(intent);
            }
        });
    }

    public Bitmap processSelectedImage(Uri imageUri) {
        Mat originalImageMat = new Mat();

        // Convert Uri to Bitmap
        Bitmap bitmap = uriToBitmap(imageUri);

        Utils.bitmapToMat(bitmap, originalImageMat);

        Mat edges = new Mat();
        Mat blurred = new Mat();
        Mat gray = new Mat();

        try {
            // Convert the image to grayscale
            Imgproc.cvtColor(originalImageMat, gray, Imgproc.COLOR_RGBA2GRAY);

            // Apply Gaussian blur
            Imgproc.GaussianBlur(originalImageMat, blurred, new Size(5, 5), 0);

            // Add weighted to enhance edges
            Core.addWeighted(originalImageMat, 1.5, blurred, -0.5, 0, originalImageMat);

            // Apply Canny edge detection
            Imgproc.Canny(blurred, edges, 50, 150);

            // Dilate the image to connect components
            Mat dilated = new Mat();
            int dilateSize = 1;
            Mat dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(dilateSize, dilateSize));
            Imgproc.dilate(edges, dilated, dilateKernel);

            // Highlight corners
            highlightCorners(originalImageMat, dilated);

            // Convert the processed Mat to Bitmap
            Log.d("originalImageMat", String.valueOf(originalImageMat));

            // Convert the processed Mat to Bitmap
            Bitmap.Config config = originalImageMat.channels() == 1 ? Bitmap.Config.ALPHA_8 : Bitmap.Config.ARGB_8888;
            int width = originalImageMat.cols();
            int height = originalImageMat.rows();
            Bitmap res = Bitmap.createBitmap(width, height, config);
            Utils.matToBitmap(originalImageMat, res);
            return res;
        } finally {
            // Release Mats
            edges.release();
            blurred.release();
            gray.release();
            originalImageMat.release();
        }
    }

    public void highlightCorners(Mat originalImage, Mat dilated) {
        MatOfPoint corners = new MatOfPoint();

        // Adjust the parameters based on your requirements
        Imgproc.goodFeaturesToTrack(dilated, corners, 100, 0.01, 10.0);

        Scalar color = new Scalar(0.0, 255.0, 0.0, 255.0); // Green color for corners

        for (Point point : corners.toArray()) {
            Imgproc.circle(originalImage, new Point(point.x, point.y), 1, color, 2);
        }

        corners.release();
    }

    private Bitmap uriToBitmap(Uri uri) {
        try {
            // Convert the stream to a Bitmap
            return MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (Exception e) {
            // Handle the exception if the file is not found
            e.printStackTrace();
            return null;
        }
    }

    private void init(){
        selectedImage = findViewById(R.id.selectedImage);
        proceed = findViewById(R.id.proceed);
        cancel = findViewById(R.id.cancle);
    }

//    private void processImage(Uri imageUri) {
//        inputMat = new Mat();
//
//        // Convert Uri to Bitmap
//        Bitmap bitmap = uriToBitmap(imageUri);
//
//        // Check if the conversion was successful
//        if (bitmap != null) {
//            Utils.bitmapToMat(bitmap, inputMat);
//
//            Mat edges = new Mat();
//            Mat blurred = new Mat();
//            Mat dilated = new Mat();
//            Mat lines = new Mat();
//
//            // Convert the image to grayscale
//            Imgproc.cvtColor(inputMat, edges, Imgproc.COLOR_RGBA2GRAY);
//
//            // Apply Gaussian blur
//            Imgproc.GaussianBlur(edges, blurred, new Size(5, 5), 0);
//
//            // Apply Canny edge detection
//            Imgproc.Canny(blurred, edges, 50, 150);
//
//            // Convert processed Mat back to Bitmap
//            Bitmap processedBitmap = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(edges, processedBitmap);
//
//            // Convert Bitmap to byte array
//            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            processedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
//            byte[] byteArray = stream.toByteArray();
//
//            // Dilate the image to connect components
//            int dilateSize = 3;
//            Mat dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(dilateSize, dilateSize));
//            Imgproc.dilate(edges, dilated, dilateKernel);
//
//            // Use Hough transform to detect lines
//            Imgproc.HoughLines(dilated, lines, 1, Math.PI / 180, 100);
//
//            // Process the detected lines
//            List<Double> angles = new ArrayList<>();
//
//            for (int i = 0; i < lines.rows(); ++i) {
//                double[] data = lines.get(i, 0);
//
//                double rho = data[0];
//                double theta = data[1];
//                double angleDeg = Math.toDegrees(theta);
//
//                // Store the angles for later evaluation
//                angles.add(angleDeg);
//            }
//
//            // Clean up
//            edges.release();
//            dilated.release();
//            lines.release();
//
//            //Start ResultActivity and pass the processed image
//            Intent resultIntent = new Intent(CActivity.this, ResultActivity.class);
//            resultIntent.putExtra("PROCESSED_IMAGE", byteArray);
//            resultIntent.putExtra("ANGLES", angles.toArray(new Double[0]));
//            resultIntent.putExtra("IMAGE_URI", imageUri.toString());
//            startActivity(resultIntent);
//            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
//
//
//        } else {
//            // Handle the case where the conversion failed
//            // This may occur if the image is not accessible or invalid
//            Log.e("OpenCV", "Failed to convert Uri to Bitmap");
//        }
//    }//
}
