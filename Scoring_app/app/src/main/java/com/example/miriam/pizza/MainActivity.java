package com.example.miriam.pizza;

import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.provider.MediaStore;


import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.UploadNotificationConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;
import java.io.InputStream;

import android.support.v4.content.FileProvider;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.os.Environment;
import android.util.Log;
import android.graphics.Matrix;

import java.io.ByteArrayOutputStream;

import android.os.AsyncTask;


import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends Activity {

    private static final int CAMERA_PIC_REQUEST = 22;

    public static final String TAG = "MY MESSAGE";

    //Uri to store the image uri
    private Uri photoURI;

    private int numberOfCalls;

    private Button BtnSelectImage;
    private TextView TxtSubject;

    private ImageView ImgPhoto;
    private Bitmap photo;
    private Button BtnNext;
    private Button BtnSatisfied;
    private Button BtnNeutral;
    private Button BtnDisssatisfied;
    private TextView TxtWait;
    private ProgressBar ProgressBar;
    private TextView TxtEmotion;
    private int emotion = 0;
    private int score;

    private Button BtnRestart;

    private String currentImagePath;
    private String currentImageName;

    private File image;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        numberOfCalls = 0;

        TakePicture();
    }

    public void TakePicture() {
        score = 0;
        emotion = 0;
        numberOfCalls += 1;
        setContentView(R.layout.start);

        TxtSubject = findViewById(R.id.TxtSubject);
        BtnSelectImage = findViewById(R.id.BtnSelectImg);

        if ((numberOfCalls % 2) == 1)
            TxtSubject.setText("How did you like the food?");
        else
            TxtSubject.setText("How did you like the environment?");

        BtnSelectImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                try {
                    //Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    //startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
                    dispatchTakePictureIntent();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Couldn't load photo", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void Rate() {
        setContentView(R.layout.picture_taken);

        ImgPhoto = findViewById(R.id.ImgPhoto);
        TxtWait = findViewById(R.id.TxtWait);
        ProgressBar = findViewById(R.id.progressBar);
        TxtEmotion = findViewById(R.id.TxtEmotion);
        BtnSatisfied = findViewById(R.id.BtnSatisfied);
        BtnNeutral = findViewById(R.id.BtnNeutral);
        BtnDisssatisfied = findViewById(R.id.BtnDisssatisfied);
        BtnNext = findViewById(R.id.BtnNext);

        ImgPhoto.setImageBitmap(photo);

        TxtWait.setVisibility(View.VISIBLE);
        ProgressBar.setVisibility(View.VISIBLE);
        TxtEmotion.setVisibility(View.INVISIBLE);

        receiveRecognitionFromServer();

        BtnSatisfied.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                score = 1;
                BtnSatisfied.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.AED581)));
                BtnNeutral.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.Mat_Grey)));
                BtnDisssatisfied.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.Mat_Grey)));
            }
        });
        BtnNeutral.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                score = 2;
                BtnSatisfied.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.Mat_Grey)));
                BtnNeutral.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.AED581)));
                BtnDisssatisfied.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.Mat_Grey)));
            }
        });
        BtnDisssatisfied.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                score = 3;
                BtnSatisfied.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.Mat_Grey)));
                BtnNeutral.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.Mat_Grey)));
                BtnDisssatisfied.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.AED581)));

            }
        });

        if ((numberOfCalls % 2) == 1)
            BtnNext.setText("Next");
        else
            BtnNext.setText("Done");

        BtnNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                // show warning if not rated or no emotion from server received
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                if ((score == 0) && (emotion == 0)) {
                    builder.setMessage("Please rate and wait for emotion recognition")
                            .setTitle("Warning")
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                } else if ((score == 0) && (emotion != 0)) {
                    builder.setMessage("Please rate first")
                            .setTitle("Warning")
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                } else if ((score != 0) && (emotion == 0)) {
                    builder.setMessage("Please wait for emotion recognition")
                            .setTitle("Warning")
                            .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            })
                            .show();
                } else {
                    //image.delete();
                    sendRatingToServer();
                    if ((numberOfCalls % 2) == 1)
                        TakePicture();
                    else
                        Restart();
                }
            }
        });
    }

    public void Restart() {
        setContentView(R.layout.thanks);
        BtnRestart = findViewById(R.id.BtnRestart);

        BtnRestart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                TakePicture();
            }
        });
    }

    private void dispatchTakePictureIntent() {

        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created

            if (photoFile != null) {
                    photoURI = FileProvider.getUriForFile(MainActivity.this,
                            "com.example.miriam.pizza.fileprovider",
                            photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, MainActivity.CAMERA_PIC_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // File image = File.createTempFile(
        image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
               storageDir      /* directory */
        );

        currentImagePath = image.getAbsolutePath();
        currentImageName = image.getName();
        return image;
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "ACTIVITY REQUEST CODE " + requestCode);
        try {
            switch (requestCode) {
                case CAMERA_PIC_REQUEST:
                    Log.i(TAG, "RESULT CODE " + resultCode);
                    if (resultCode == RESULT_OK) {
                        //photo = (Bitmap) data.getExtras().get("data");

                        File file = new File(currentImagePath);
                        photo = MediaStore.Images.Media
                                .getBitmap(getApplicationContext().getContentResolver(), Uri.fromFile(file));
                        photo = RotateBitmap(photo, -90);

                        if (photo != null) {

                            if (currentImagePath.length() > 0)
                                sendImageToServer(currentImagePath); //sendImageToServer("/storage/extSdCard/DCIM/Camera");
                            // sendImageToServer(photoURI.getPath());

                            Rate();
                        }
                    } else {
                        currentImagePath = "";
                        Toast.makeText(MainActivity.this, "Erroruring image", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
                    break;
            }
        }
        catch (Exception error) {
            error.printStackTrace();
        }
    }


    /*public static File savebitmap(Bitmap bmp) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
        File f = new File(image.getPath(), image.getName()); ;//new File(Environment.getExternalStorageDirectory()
                //+ File.separator + "testimage.jpg");
        f.createNewFile();
        FileOutputStream fo = new FileOutputStream(f);
        fo.write(bytes.toByteArray());
        fo.close();
        return f;
    }*/

    /*
     * This is the method responsible for image upload
     * We need the full image path and the name for the image in this method
     * */
    public void sendImageToServer(String currentImagePath) {
        //final File image = new File(currentImagePath);

        /*try {
            File newfile = savebitmap(photo);
        } catch (Exception e) {}*/


        /*
        // Assume block needs to be inside a Try/Catch block.
        //String path = Environment.getExternalStorageDirectory().toString();
        OutputStream fOut = null;
        Integer counter = 0;
        File file = new File(image.getPath(), image.getName()); // the File to save , append increasing numeric counter to prevent files from getting overwritten.
        try {
            fOut = new FileOutputStream(file);
        } catch (Exception e) {}

        //Bitmap pictureBitmap = getImageBitmap(myurl); // obtaining the Bitmap
        photo.compress(Bitmap.CompressFormat.JPEG, 85, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
        try {
            fOut.flush(); // Not really required
        } catch (Exception e) {}
        try {
            fOut.close(); // do not forget to close the stream
        } catch (Exception e) {}

        /*try {
            MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
        } catch (Exception e) {}*/


        //getting name for the image
        name = image.getName();//currentImageName;

        //getting the actual path of the image
        String path = image.getPath();//currentImagePath;
        Log.i(TAG, name);

        //Uploading code
        try {
            String uploadId = UUID.randomUUID().toString();

            //Creating a multi part request
            new MultipartUploadRequest(this, uploadId, Constants.UPLOAD_URL)
                    .addFileToUpload(path, "image") //Adding file
                    .addParameter("name", name) //Adding text parameter to the request
                    .setNotificationConfig(new UploadNotificationConfig())
                    .setMaxRetries(2)
                    .setAutoDeleteFilesAfterSuccessfulUpload(true)
                    .startUpload(); //Starting the upload

        } catch (Exception exc) {
            Toast.makeText(this, exc.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    public void sendRatingToServer() {
        SendHttpRequestTask t = new SendHttpRequestTask();

        String[] params = new String[]{Constants.UPLOAD_URL, name, String.valueOf(score)};
        t.execute(params);
    }

    public void receiveRecognitionFromServer() {
        String url = Constants.IMAGES_URL+name;

        DownloadFilesTask process = new DownloadFilesTask();
        process.execute(url);
    }


    private class SendHttpRequestTask extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {
            String url = params[0];
            String param1 = params[1];
            String param2 = params[2];
            //Bitmap b = BitmapFactory.decodeResource(UploadActivity.this.getResources(), R.drawable.logo);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //b.compress(CompressFormat.PNG, 0, baos);

            try {
                HttpClient client = new HttpClient(url);
                client.connectForMultipart();
                client.addFormPart("name", param1);
                client.addFormPart("cu_rate", param2);
                client.finishMultipart();
                String data = client.getResponse();
            }
            catch(Throwable t) {
                t.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String data) {
            //item.setActionView(null);

        }
    }

    private class DownloadFilesTask extends AsyncTask<String, Void, Void> {

        private String data_parsed;
        @Override
        protected Void doInBackground(String... params) {
            while (emotion == 0) {
                try {
                    URL url = new URL(params[0]);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    InputStream inputStream = httpURLConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String data = bufferedReader.readLine();

                    JSONArray JA = new JSONArray(data);
                    JSONObject JO = (JSONObject) JA.get(0);
                    data_parsed = "" + JO.get("ai_rate");
                    emotion = Integer.valueOf(data_parsed);

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.i("Florian",this.data_parsed);

            // show status
            if (emotion == 0) {
                TxtWait.setVisibility(View.VISIBLE);
                ProgressBar.setVisibility(View.VISIBLE);
                TxtEmotion.setVisibility(View.INVISIBLE);
            }
            else {
                TxtWait.setVisibility(View.INVISIBLE);
                ProgressBar.setVisibility(View.INVISIBLE);
                TxtEmotion.setVisibility(View.VISIBLE);
                switch (emotion) {
                    case 1:
                        TxtEmotion.setBackgroundResource(R.drawable.satisfied);
                        break;
                    case 2:
                        TxtEmotion.setBackgroundResource(R.drawable.neutral);
                        break;
                    case 3:
                        TxtEmotion.setBackgroundResource(R.drawable.disssatisfied);
                        break;
                    default:
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("Recognition failed! Please skip this page")
                                .setTitle("Warning")
                                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                })
                                .show();
                        break;
                }
            }
        }
    }
}
