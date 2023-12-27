package com.leeuw.draftangledetector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
import java.util.Locale;

public class ResultActivity extends AppCompatActivity { //implements ResultTouchImageView.OnFourPointsPlacedListener {
    private View overlayView;
    private ImageView resultImageView;
    private ArrayList<PointF> points = new ArrayList<>();
    private Canvas canvas;
    private ArrayList<Point> cornerCoordinates = new ArrayList<>();
    private List<Double> angles = new ArrayList<>();
    private ArrayList<List<Double>> storAngles = new ArrayList<>();
    private Button nextButton, restButton;
    private int comp = 0;
    private ArrayList<ArrayList<PointF>> storPoints = new ArrayList<>();
    private ImageView zoomOut, zoomIn, hideResultsButton;
    private CardView cardView;

    private ScaleGestureDetector scaleGestureDetector;
    private float scale = 1.0f;

    private TextView resultContainer, etape;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        overlayView = findViewById(R.id.overlayView);

        cardView = findViewById(R.id.cardView);
        resultImageView = findViewById(R.id.resultImage);
//        resultContainer = findViewById(R.id.resultContainer);
        etape = findViewById(R.id.etape);
        nextButton = findViewById(R.id.nextButton);
        restButton = findViewById(R.id.restButton);
        zoomOut = findViewById(R.id.zoomOut);
        zoomIn = findViewById(R.id.zoomIn);

        etape.setText("Étape 1. Versants internes (G-D)");

        Uri imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
        Log.d("imageUri", String.valueOf(imageUri));

        // Convert byte array to Bitmap
        Bitmap processedBitmap = processImage(imageUri);

        canvas = new Canvas(processedBitmap);

        resultImageView.setImageBitmap(processedBitmap);


        resultImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    handleCanvasClick(event, cornerCoordinates);
                    return true;
                }
                return false;
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Process image again and reset points for the next step
                resetForNextStep();
            }
        });

        restButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cIntent = new Intent(ResultActivity.this, MainActivity.class);
                cIntent .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(cIntent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        hideResultsButton = findViewById(R.id.hideCard);
        hideResultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(cardView.getVisibility() == View.GONE ){
                    cardView.setVisibility(View.VISIBLE);
                    overlayView.setVisibility(View.VISIBLE);
                }else{
                    cardView.setVisibility(View.GONE);
                    overlayView.setVisibility(View.GONE);
                }

            }
        });

        zoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomImage(0.8f);
            }
        });

        zoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomImage(1.2f);
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Pass touch events to ScaleGestureDetector
        scaleGestureDetector.onTouchEvent(event);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // Get the scale factor from the detector
            float scaleFactor = detector.getScaleFactor();

            // Apply the scale factor incrementally
            scale *= scaleFactor;

            // Set limits to prevent extreme scaling
            scale = Math.max(0.1f, Math.min(scale, 5.0f));

            // Apply the scale to the ImageView
            resultImageView.setScaleX(scale);
            resultImageView.setScaleY(scale);

            Log.d("Scale", String.valueOf(scale));

            return true;
        }
    }


    private void zoomImage(Float scaleFactor) {

        scale *= scaleFactor;

        // Set limits to prevent extreme scaling
        scale = Math.max(0.1f, Math.min(scale, 5.0f));

        // Apply the scale to the ImageView
        resultImageView.setScaleX(scale);
        resultImageView.setScaleY(scale);

        Log.d("Scale", String.valueOf(scale));

        // Get the current matrix
//        Matrix matrix = new Matrix(resultImageView.getImageMatrix());
//        // Scale the matrix
////        scale *= scaleFactor;
//        matrix.postScale(scaleFactor, scaleFactor);

        // Apply the matrix to the ImageView
//        resultImageView.setImageMatrix(matrix);
        resultImageView.invalidate();
    }

    private void handleCanvasClick(MotionEvent event, ArrayList<Point> cornerCoordinates) {
        // Find the closest point considering both x and y axes
        PointF touchPoint = new PointF(event.getX(), event.getY());

        // Transform touch event coordinates to canvas coordinates
        float[] touchPointCanvas = new float[]{touchPoint.x, touchPoint.y};
        Matrix inverse = new Matrix();
        resultImageView.getImageMatrix().invert(inverse);
        inverse.mapPoints(touchPointCanvas);

        Point closestPoint = findClosestPoint(new PointF(touchPointCanvas[0], touchPointCanvas[1]), cornerCoordinates);

        if (closestPoint != null) {
            points.add(new PointF((float) closestPoint.x, (float) closestPoint.y));

            // Draw a point on the canvas
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle((float) closestPoint.x, (float) closestPoint.y, 5, paint);

            // Update the ImageView to reflect the changes
            resultImageView.invalidate();

            Log.d("size", String.valueOf(points.size()));
            if (points.size() == 4) {
                calculateAndDisplayTaperAngles();
            }
        }
    }

    private void calculateAndDisplayTaperAngles() {
        // Calculate distances for taper angles
        float deltaY = points.get(1).y - points.get(0).y;
        float deltaX = points.get(1).x - points.get(0).x;
        float taperAngleRad = (float) Math.atan2(deltaY, deltaX);
        float taperAngleDeg = 0;

        if (storPoints.size() == 0)
            taperAngleDeg = (float) Math.toDegrees(taperAngleRad);
        else if (comp == 0)
            taperAngleDeg = 90 + (float) Math.toDegrees(taperAngleRad);
        else if (comp == 1)
            taperAngleDeg = 90 + (float) Math.toDegrees(taperAngleRad);

        float deltaY2 = points.get(3).y - points.get(2).y;
        float deltaX2 = points.get(3).x - points.get(2).x;
        float taperAngleRad2 = (float) Math.atan2(deltaY2, deltaX2);

        float taperAngleDeg2 = 0;
        if (storPoints.size() == 0)
            taperAngleDeg2 = -(float) Math.toDegrees(taperAngleRad2);
        else if (comp == 0)
            taperAngleDeg2 = -(((float) Math.toDegrees(taperAngleRad2)) + 90);
        else if (comp == 1)
            taperAngleDeg2 = -(((float) Math.toDegrees(taperAngleRad2)) + 90);

        // Calculate the coordinates of the point of intersection
        float slope1 = (points.get(1).y - points.get(0).y) / (points.get(1).x - points.get(0).x);
        float yIntercept1 = points.get(0).y - slope1 * points.get(0).x;

        float slope2 = (points.get(3).y - points.get(2).y) / (points.get(3).x - points.get(2).x);
        float yIntercept2 = points.get(2).y - slope2 * points.get(2).x;

        float intersectionX = (yIntercept2 - yIntercept1) / (slope1 - slope2);
        float intersectionY = slope1 * intersectionX + yIntercept1;

        // Draw the lines on the canvas
        Paint linePaint = new Paint();
        linePaint.setColor(Color.GREEN);
        linePaint.setStrokeWidth(2);

        // Draw line1
        canvas.drawLine(points.get(0).x, points.get(0).y, intersectionX, intersectionY, linePaint);

        // Draw line2
        canvas.drawLine(points.get(2).x, points.get(2).y, intersectionX, intersectionY, linePaint);

        // Draw the angle text
        float centerX = (points.get(0).x + points.get(1).x) / 2;
        float centerY = (points.get(0).y + points.get(1).y) / 2;

        Paint textPaint = new Paint();
        textPaint.setColor(Color.YELLOW);
        textPaint.setTextSize(14);

        canvas.drawText(String.format("%s °", taperAngleDeg), centerX - 40, centerY - 20, textPaint);

        float centerX2 = (points.get(2).x + points.get(3).x) / 2;
        float centerY2 = (points.get(2).y + points.get(3).y) / 2;

        canvas.drawText(String.format("%s °", taperAngleDeg2), centerX2 - 40, centerY2 - 20, textPaint);

        // Calculate the angle between the two lines
        float angleBetweenLines = calculateAngleBetweenLines(
                points.get(0).x, points.get(0).y, intersectionX, intersectionY,
                points.get(2).x, points.get(2).y, intersectionX, intersectionY
        );

        // Draw the convergence angle text
        float centerX3 = (intersectionX + centerX2) / 2;
        float centerY3 = (intersectionY + centerY2) / 2;

        Paint convergencePaint = new Paint();
        convergencePaint.setColor(Color.BLUE);
        convergencePaint.setTextSize(14);

        if (comp != 0)
            canvas.drawText(String.format("Convergence Angle: %s °", angleBetweenLines), centerX3 - 40, centerY3 + 20, convergencePaint);

        float sumOfFirstAndLastAngles = taperAngleDeg + taperAngleDeg2;

        // Display the results
        // Adjust color based on the criteria
        // Set default class to 'green-text'
        String className = "green";

        // Adjust class based on the criteria
        if (sumOfFirstAndLastAngles > 16 && sumOfFirstAndLastAngles <= 21) {
            className = "orange";
        } else if (sumOfFirstAndLastAngles > 21 || (sumOfFirstAndLastAngles >= 4 && sumOfFirstAndLastAngles < 6) || sumOfFirstAndLastAngles < 4) {
            className = "red";
        }

        // Add the results to the resultContainer
        if (points.size() == 4) {
            storPoints.add(points);
            storAngles.add(angles);

            if (storPoints.size() == 1) {
                // Display the results for "Les versants internes"
                displayResults("Les versants internes", className, taperAngleDeg, taperAngleDeg2);
//                comp++;
                // Move to the next step
                showNextStep();
            } else{
                if (comp == 0) {
                        // Display the results for "Les versants externes"
                        displayResults("Les versants externes", className, taperAngleDeg, taperAngleDeg2);
                        comp++;
                        // Move to the next step
                        showNextStep();
                    } else {
                        // Display the results for "Contre dépouille" and symmetry
    //                displayResults("Contre dépouille", className, taperAngleDeg, taperAngleDeg2);
    //                displaySymmetry();
                        // Hide the next button as it's the last step
                        displayFinalResult(taperAngleDeg, taperAngleDeg2, sumOfFirstAndLastAngles, angleBetweenLines);
    //                hideNextButton();
                    }
            }

            cardView.setVisibility(View.VISIBLE);
            hideResultsButton.setVisibility(View.VISIBLE);
            overlayView.setVisibility(View.VISIBLE);
        }
    }

    // Calculate the angle between two lines
    private float calculateAngleBetweenLines(float startX1, float startY1, float endX1, float endY1,
                                             float startX2, float startY2, float endX2, float endY2) {
        // Calculate vectors
        float vector1X = endX1 - startX1;
        float vector1Y = endY1 - startY1;
        float vector2X = endX2 - startX2;
        float vector2Y = endY2 - startY2;

        // Log the vectors for debugging
        Log.d("AngleDebug", "Vector1X: " + vector1X + " Vector1Y: " + vector1Y);
        Log.d("AngleDebug", "Vector2X: " + vector2X + " Vector2Y: " + vector2Y);

        // Calculate dot product
        float dotProduct = dotProduct(vector1X, vector1Y, vector2X, vector2Y);

        // Calculate magnitudes
        float mag1 = magnitude(vector1X, vector1Y);
        float mag2 = magnitude(vector2X, vector2Y);

        // Ensure the dot product is within the valid range for acos
        float dotClamped = Math.max(-1, Math.min(dotProduct / (mag1 * mag2), 1));

        // Calculate angle in radians
        float angleRad = (float) Math.acos(dotClamped);

        // Log the calculated angle in radians
        Log.d("AngleDebug", "AngleRad: " + angleRad);

        // Determine the sign of the angle based on the cross product of the vectors
        float crossProduct = vector1X * vector2Y - vector1Y * vector2X;
        float angleDeg = (crossProduct >= 0) ? (float) Math.toDegrees(angleRad) : -(float) Math.toDegrees(angleRad);

        // Log the calculated angle in degrees
        Log.d("AngleDebug", "AngleDeg: " + angleDeg);

        return Math.abs(angleDeg);
    }

    // Calculate the dot product between two vectors
    private float dotProduct(float x1, float y1, float x2, float y2) {
        return x1 * x2 + y1 * y2;
    }

    // Calculate the magnitude (norm) of a vector
    private float magnitude(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    private void resetForNextStep() {
        if(comp == 0){
            etape.setText("Étape 2.Versants externes (G-D)");
        }else{
            etape.setText("Étape 3. Angles de dépouille (G-D)");
        }

        cardView.setVisibility(View.GONE);
        overlayView.setVisibility(View.GONE);

        // Reset points and any other necessary variables
        points.clear();
        cornerCoordinates.clear();

        // Process image again
        Uri imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
        Bitmap processedBitmap = processImage(imageUri);
        canvas = new Canvas(processedBitmap);
        resultImageView.setImageBitmap(processedBitmap);

    }

//    private void displayResults(String category, String className, float taperAngleDeg, float taperAngleDeg2) {
//        // Create a div element
//        TextView textView = new TextView(this);
//        textView.setText(category);
//        textView.setTextSize(18);
//        textView.setTextColor(Color.parseColor(className));
//
//        // Set the inner HTML content with the dynamically determined class
//        TextView angle1TextView = new TextView(this);
//        angle1TextView.setText(String.format("Lingual 15°/à l'horizontal : %.2f degrés", taperAngleDeg));
//        angle1TextView.setTextSize(14);
//        angle1TextView.setTextColor(Color.parseColor(className));
//
//        TextView angle2TextView = new TextView(this);
//        angle2TextView.setText(String.format("Vestibulaire à 45 °/à l'horizontal : %.2f degrés", taperAngleDeg2));
//        angle2TextView.setTextSize(14);
//        angle2TextView.setTextColor(Color.parseColor(className));
//
//        // Append the dynamically created TextViews to a container (assuming resultContainer is the container)
//        resultContainer.append(textView.getText() + ", " + angle1TextView.getText() + ", " + angle2TextView.getText() + "\n");
//    }
//
//    private void displayFinalResult(float taperAngleDeg, float taperAngleDeg2, float sumOfAngles, float angleBetweenLines) {
//        // Calculate the sum of the first and last angles
//        float sumOfFirstAndLastAngles = taperAngleDeg + taperAngleDeg2;
//
//        // Evaluate the performance based on the sum of angles
//        String performance = evaluateSumOfAngles(sumOfFirstAndLastAngles);
//
//        // Create a div element for the final result
//        TextView finalResultTextView = new TextView(this);
//        finalResultTextView.setText(String.format("Contre dépouille: %s", performance));
//        finalResultTextView.setTextSize(18);
//        finalResultTextView.setTextColor(Color.RED);
//
//        // Create div elements for individual results
//        TextView result1TextView = new TextView(this);
//        result1TextView.setText(String.format("Dépouille des parois vestibulo-linguales : %.2f degrés", taperAngleDeg));
//        result1TextView.setTextSize(14);
//
//        TextView result2TextView = new TextView(this);
//        result2TextView.setText(String.format("Dépouille des parois mésio-distale : %.2f degrés", taperAngleDeg2));
//        result2TextView.setTextSize(14);
//
//        TextView result3TextView = new TextView(this);
//        result3TextView.setText(String.format("Somme des angles : %.2f degrés", sumOfFirstAndLastAngles));
//        result3TextView.setTextSize(14);
//
//        TextView result4TextView = new TextView(this);
//
//        // Check if there is convergence
//        if (angleBetweenLines != 0) {
//            result4TextView.setText(String.format("Angle de convergence : %.2f degrés", angleBetweenLines));
//        } else {
//            result4TextView.setText("Angle de convergence : Pas de convergence");
//        }
//
//        result4TextView.setTextSize(14);
//
//        resultContainer.append(finalResultTextView.getText() + ", " + result1TextView.getText() + ", "
//                + result2TextView.getText() + ", " + result3TextView.getText() + ", "+
//                result4TextView.getText() +"\n");
//
//        // Check symmetry and display the result
//        displaySymmetry();
//
//        // Hide the next button as it's the final step
//        hideNextButton();
//    }




    // =================

    private void displayResults(String category, String className, float taperAngleDeg, float taperAngleDeg2) {
        // Create a TextView for the category
        TextView categoryTextView = new TextView(this);
        categoryTextView.setText(category);
        categoryTextView.setTextSize(18);
        categoryTextView.setTextColor(Color.parseColor(className));

        // Create TextViews for individual angles
        TextView angle1TextView = new TextView(this);
        angle1TextView.setText(String.format("Lingual 15°/à l'horizontal : %.2f degrés", taperAngleDeg));
        angle1TextView.setTextSize(14);
        angle1TextView.setTextColor(Color.parseColor(className));

        TextView angle2TextView = new TextView(this);
        angle2TextView.setText(String.format("Vestibulaire à 45 °/à l'horizontal : %.2f degrés", taperAngleDeg2));
        angle2TextView.setTextSize(14);
        angle2TextView.setTextColor(Color.parseColor(className));

        // Add TextViews to the resultDetailsContainer
        LinearLayout resultDetailsContainer = findViewById(R.id.resultDetailsContainer);
        resultDetailsContainer.addView(categoryTextView);
        resultDetailsContainer.addView(angle1TextView);
        resultDetailsContainer.addView(angle2TextView);

        addDivider();
    }
    private void displayFinalResult(float taperAngleDeg, float taperAngleDeg2, float sumOfAngles, float angleBetweenLines) {
        // Create a TextView for the final result
        TextView finalResultTextView = new TextView(this);
        finalResultTextView.setText(String.format("Contre dépouille: %s", evaluateSumOfAngles(taperAngleDeg + taperAngleDeg2)));
        finalResultTextView.setTextSize(18);
        finalResultTextView.setTextColor(Color.RED);

        // Create TextViews for individual results
        TextView result1TextView = new TextView(this);
        result1TextView.setText(String.format("Dépouille des parois vestibulo-linguales : %.2f degrés", taperAngleDeg));
        result1TextView.setTextSize(14);

        TextView result2TextView = new TextView(this);
        result2TextView.setText(String.format("Dépouille des parois mésio-distale : %.2f degrés", taperAngleDeg2));
        result2TextView.setTextSize(14);

        TextView result3TextView = new TextView(this);
        result3TextView.setText(String.format("Somme des angles : %.2f degrés", taperAngleDeg + taperAngleDeg2));
        result3TextView.setTextSize(14);

        TextView result4TextView = new TextView(this);

        // Check if there is convergence
        if (angleBetweenLines != 0) {
            result4TextView.setText(String.format("Angle de convergence : %.2f degrés", angleBetweenLines));
        } else {
            result4TextView.setText("Angle de convergence : Pas de convergence");
        }

        result4TextView.setTextSize(14);

        // Add TextViews to the resultDetailsContainer
        LinearLayout resultDetailsContainer = findViewById(R.id.resultDetailsContainer);
        resultDetailsContainer.addView(finalResultTextView);
        resultDetailsContainer.addView(result1TextView);
        resultDetailsContainer.addView(result2TextView);
        resultDetailsContainer.addView(result3TextView);
        resultDetailsContainer.addView(result4TextView);

        // Check symmetry and display the result
        displaySymmetry();

        // Hide the next button as it's the final step
        hideNextButton();
    }
    private void displaySymmetry() {
        // Check symmetry based on the four points
        boolean isSymmetric = checkSymmetry(points.get(0).x, points.get(0).y, points.get(1).x, points.get(1).y,
                points.get(2).x, points.get(2).y, points.get(3).x, points.get(3).y);

        // Create a TextView for the symmetry result
        TextView symmetryResultTextView = new TextView(this);
        symmetryResultTextView.setTextSize(14);

        if (isSymmetric) {
            symmetryResultTextView.setText("Symétrie: Symétrique");
            symmetryResultTextView.setTextColor(Color.GREEN);
        } else {
            symmetryResultTextView.setText("Symétrie: Non Symétrique");
            symmetryResultTextView.setTextColor(Color.RED);
        }

        // Add the TextView to the resultDetailsContainer
        LinearLayout resultDetailsContainer = findViewById(R.id.resultDetailsContainer);
        resultDetailsContainer.addView(symmetryResultTextView);
    }

    private void addDivider() {
        // Add a divider line to the resultDetailsContainer
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                2
        );
        // Set vertical margins
        params.setMargins(0, 8, 0, 8);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(Color.GRAY);
        addViewToContainer(divider);
    }

    // Add this method to add a view to the resultDetailsContainer
    private void addViewToContainer(View view) {
        LinearLayout resultDetailsContainer = findViewById(R.id.resultDetailsContainer);
        resultDetailsContainer.addView(view);
    }


//    private void displaySymmetry() {
//        // Check symmetry based on the four points
//        boolean isSymmetric = checkSymmetry(points.get(0).x, points.get(0).y, points.get(1).x, points.get(1).y,
//                points.get(2).x, points.get(2).y, points.get(3).x, points.get(3).y);
//
//        // Create a TextView to display symmetry result
//        TextView symmetryTextView = new TextView(this);
//
//        if (isSymmetric) {
//            symmetryTextView.setText("Symétrie: Symétrique");
//        } else {
//            symmetryTextView.setText("Symétrie: Non Symétrique");
//        }
//
//        symmetryTextView.setTextSize(14);
//
//        // Display the symmetry result
//        resultContainer.append(symmetryTextView.getText());
//    }

    private void showNextStep() {
        // Show the next button
        nextButton.setVisibility(View.VISIBLE);
    }

    private void hideNextButton() {
        // Hide the next button
        nextButton.setVisibility(View.GONE);
    }

    // Evaluate the sum of angles and provide performance feedback
    private String evaluateSumOfAngles(float sumOfAngles) {
        String result;
        if (sumOfAngles >= 6 && sumOfAngles <= 16) {
            result = "Excellente performance";
        } else if (sumOfAngles > 0 && sumOfAngles < 6) {
            result = "Performance insatisfaisante";
        } else if (sumOfAngles > 16) {
            result = "Performance insatisfaisante : Dépouille excessive";
        } else {
            result = "Médiocre";
        }
        return result;
    }

    // Check symmetry and return the result
    private boolean checkSymmetry(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        float deltaX1 = Math.abs(x1 - x2);
        float deltaX2 = Math.abs(x3 - x4);
        float deltaY1 = Math.abs(y1 - y2);
        float deltaY2 = Math.abs(y3 - y4);

        boolean isSymmetrical = Math.abs(deltaX1 - deltaX2) < 5 && Math.abs(deltaY1 - deltaY2) < 5;

        return isSymmetrical;
    }

    private Point findClosestPoint(PointF touchPoint, ArrayList<Point> cornerCoordinates ) {
        Point closestPoint = null;
        double minDistance = Double.MAX_VALUE;

        for (Point point : cornerCoordinates) {
            double distance = Math.sqrt(Math.pow(point.x - touchPoint.x, 2) + Math.pow(point.y - touchPoint.y, 2));

            if (distance < minDistance) {
                minDistance = distance;
                closestPoint = point;
            }
        }

        return closestPoint;
    }

    private Bitmap processImage(Uri imageUri) {
        Mat inputMat = new Mat();

        // Convert Uri to Bitmap
        Bitmap bitmap = uriToBitmap(imageUri);

        Mat dilated = new Mat();
        Mat edges = new Mat();
        Mat blurred = new Mat();
        Mat lines = new Mat();
        Mat gray = new Mat();
        Mat dilateKernel = new Mat();

        try{
            Utils.bitmapToMat(bitmap, inputMat);

            // Convert the image to grayscale
            Imgproc.cvtColor(inputMat, gray, Imgproc.COLOR_RGBA2GRAY);

            // Apply Gaussian blur
            Imgproc.GaussianBlur(inputMat, blurred, new Size(5, 5), 0);

            // Add weighted to enhance edges
            Core.addWeighted(inputMat, 1.5, blurred, -0.5, 0, inputMat);

            // Apply Canny edge detection
            Imgproc.Canny(blurred, edges, 50, 150);

            // Dilate the image to connect components
            int dilateSize = 1;
            dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(dilateSize, dilateSize));
            Imgproc.dilate(edges, dilated, dilateKernel);

            highlightCorners2(dilated);

            // Use Hough transform to detect lines
            Imgproc.HoughLines(dilated, lines, 1, Math.PI / 180, 100);

            // Process the detected lines
            angles = new ArrayList<>();

            for (int i = 0; i < lines.rows(); ++i) {
                double[] data = lines.get(i, 0);

                double rho = data[0];
                double theta = data[1];
                double angleDeg = Math.toDegrees(theta);

                // Store the angles for later evaluation
                angles.add(angleDeg);
            }

            Bitmap.Config config = Bitmap.Config.ARGB_8888;
            int width = dilated.cols();
            int height = dilated.rows();
            Bitmap res = Bitmap.createBitmap(width, height, config);
            Utils.matToBitmap(dilated, res);
            return res;

        } finally {
            edges.release();
            dilated.release();
            dilateKernel.release();
            lines.release();
            gray.release();
            inputMat.release();
        }
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

    public void highlightCorners2(Mat dilated) {
        MatOfPoint corners = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(dilated, corners, 100, 0.01, 10.0);

        Scalar color = new Scalar(255.0, 0.0, 0.0, 255.0); // Red color for corners

        // Check if corners were detected
        if (corners.rows() <= 0) {
            System.out.println("No corners detected.");
            return;
        }

        System.out.println("corners.rows(): " + corners.rows());

        for (Point point : corners.toArray()) {
            // Increase the size of the points
            Imgproc.circle(dilated, point, 2, color, -1);

            // Store coordinates in the array (assuming cornerCoordinates is a class-level ArrayList<Point>)
            cornerCoordinates.add(point);
        }
        corners.release();
    }
}


// Format the results
//            String resultText = String.format(Locale.US,
//                "The dental taper angle left is: %.2f degrees\n" +
//                        "The dental taper angle right is: %.2f degrees\n" +
//                        "Symmetry: %s",
//                taperAngleDeg, taperAngleDeg2, isSymmetrical ? "Symmetrical" : "Not Symmetrical");

// Show the results in a MaterialAlertDialog
//            new MaterialAlertDialogBuilder(ResultActivity.this)
//                .setTitle("Symmetry Evaluation Results")
//                .setMessage(resultText)
//                .setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        Intent cIntent = new Intent(ResultActivity.this, CActivity.class);
//                        cIntent.putExtra("IMAGE_URI", imageUri);
//                        startActivity(cIntent);
//                        finish();
//                    }
//                })
//                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        Intent mainIntent = new Intent(ResultActivity.this, MainActivity.class);
//                        startActivity(mainIntent);
//                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
//                        finish();
//                    }
//                })
//                .show();
