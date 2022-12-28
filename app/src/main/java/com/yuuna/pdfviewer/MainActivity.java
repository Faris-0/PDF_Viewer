package com.yuuna.pdfviewer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.viewpager2.widget.ViewPager2;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends Activity {

    private ViewPager2 vpPDF;
    private SliderAdapter sliderAdapter;
    private ArrayList<SliderItems> sliderItemsArrayList;

    private Uri uri;

    private final Integer TAG_PDF = 35;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vpPDF = findViewById(R.id.slide_pdf);

        uri = getIntent().getData();
        if (uri!=null) new AsyncCaller().execute();

        findViewById(R.id.open_doc).setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= 29) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/pdf");
                try {
                    startActivityForResult(intent, TAG_PDF);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        TAG_PDF
                );
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == TAG_PDF) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/pdf");
                try {
                    startActivityForResult(intent, TAG_PDF);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Tidak ada perizinan untuk mengakses gambar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == TAG_PDF) {
            uri = data.getData();
            new AsyncCaller().execute();
        }
    }

    private class AsyncCaller extends AsyncTask<Void, Void, Void> {
        private ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //this method will be running on UI thread
            pdLoading.setMessage("Please wait..");
            pdLoading.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            //this method will be running on background thread so don't update UI frome here
            //do your long running http tasks here,you dont want to pass argument and u can access the parent class' variable url over here
            try {
                ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
                renderAllPages(parcelFileDescriptor);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                updateUI(e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            //this method will be running on UI thread
            pdLoading.dismiss();
        }
    }

    private void renderAllPages(ParcelFileDescriptor parcelFileDescriptor) {
        // create a new renderer
        try {
            PdfRenderer renderer = new PdfRenderer(parcelFileDescriptor);

            if (renderer != null) {
                // let us just render all pages
                final Integer pageCount = renderer.getPageCount();
                sliderItemsArrayList = new ArrayList<>();
                for (int i = 0; i < pageCount; i++) {
                    PdfRenderer.Page page = renderer.openPage(i);

                    // Important: the destination bitmap must be ARGB (not RGB).
                    Bitmap bitmap = Bitmap.createBitmap(
                            getResources().getDisplayMetrics().densityDpi * page.getWidth() / 72,
                            getResources().getDisplayMetrics().densityDpi * page.getHeight() / 72,
                            Bitmap.Config.ARGB_8888
                    );

                    // Paint bitmap before rendering
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawColor(Color.WHITE);
                    canvas.drawBitmap(bitmap, 0, 0, null);

                    // say we render for showing on the screen
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                    // do stuff with the bitmap
                    sliderItemsArrayList.add(new SliderItems(i + ".jpeg"));
                    try {
                        FileOutputStream fileOutputStream = openFileOutput(i + ".jpeg", Context.MODE_PRIVATE);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, fileOutputStream);
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        updateUI(e.getMessage());
                    }

                    // close the page
                    page.close();
                }

                // close the renderer
                renderer.close();

                final String show = "update";
                updateUI(show);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            updateUI(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            updateUI(e.getMessage());
        }
    }

    private void updateUI(String e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Stuff that updates the UI
                if (e.equals("update")) {
                    sliderAdapter = new SliderAdapter(sliderItemsArrayList, MainActivity.this);
                    vpPDF.setAdapter(sliderAdapter);
                } else {
                    Toast.makeText(MainActivity.this, e, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Yakin keluar?")
                .setPositiveButton("Ya", (dialog, which) -> {
                    int pid = android.os.Process.myPid();
                    android.os.Process.killProcess(pid);
                })
                .setNegativeButton("Tidak", (dialog, which) -> dialog.dismiss())
                .show();
    }
}