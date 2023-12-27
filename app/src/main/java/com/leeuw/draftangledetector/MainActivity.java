package com.leeuw.draftangledetector;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private ExtendedFloatingActionButton upload_image, capture_image;
    private TextView web;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2; // Choose any number you like


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        upload_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImagePicker();
            }
        });

        capture_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureImage();
            }
        });

        web.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "https://depouille.vercel.app/";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });

    }

    private void captureImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    // uplading image form gallery
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            // Code for handling gallery image selection
            Uri imageUri = data.getData();
            startNextActivity(imageUri);
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            // Code for handling captured image from the camera
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            Uri imageUri = null;
            try {
                imageUri = getImageUri(imageBitmap);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            startNextActivity(imageUri);
        }
    }

    private Uri getImageUri(Bitmap imageBitmap) throws IOException {
        // Save the bitmap to a file and get the Uri
        // You can use a FileProvider to get a content URI if needed
        // For simplicity, this example uses a temp file (make sure to handle file cleanup)
        File tempFile = createTempImageFile();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Uri.fromFile(tempFile);
    }

    private File createTempImageFile() throws IOException {
        // Create a temporary file to save the captured image
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
//            Uri imageUri = data.getData();
//            try {
////                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
////                imageView.setImageBitmap(bitmap);
//
//                // Start the next activity with the image URI
//                startNextActivity(imageUri);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

    private void startNextActivity(Uri imageUri) {
        Intent intent = new Intent(this, CActivity.class);
        intent.putExtra("IMAGE_URI", imageUri.toString());
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void init(){
        web = findViewById(R.id.web);
        upload_image = findViewById(R.id.upload_image);
        capture_image = findViewById(R.id.capture_image);
    }

}