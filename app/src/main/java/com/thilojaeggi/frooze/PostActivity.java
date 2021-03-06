package com.thilojaeggi.frooze;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sandrios.sandriosCamera.internal.SandriosCamera;
import com.sandrios.sandriosCamera.internal.configuration.CameraConfiguration;
import com.sandrios.sandriosCamera.internal.ui.model.Media;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import life.knowledge4.videotrimmer.utils.FileUtils;
import petrov.kristiyan.colorpicker.ColorPicker;

public class PostActivity extends AppCompatActivity {
    TextView post;
    private static final int Gallery = 0x01;
    private static final int Camera = 2;
    private static final int VIDEO_TRIMMER = 3;
    private AppCompatActivity activity;

    static final String EXTRA_VIDEO_PATH = "EXTRA_VIDEO_PATH";
    public static final int MULTIPLE_PERMISSIONS = 10;
    Button textcolor;
    String textcolorvalue;
    EditText description;
    private static ProgressDialog mProgressDialog;
    String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        setContentView(R.layout.activity_post);
        description = findViewById(R.id.description);
        try {
            Map config = new HashMap();
            config.put("cloud_name", "froozecdn");
            MediaManager.init(this, config);
        } catch (Exception e){

        }
        post = findViewById(R.id.post);
        textcolorvalue ="white";
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getApplicationContext(), "Sign in first", Toast.LENGTH_SHORT).show();
            finish();
        }
        textcolor = findViewById(R.id.textcolor);
        textcolor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ColorPicker colorPicker = new ColorPicker(PostActivity.this);
                ArrayList<String> colors = new ArrayList<>();
                colors.add("#ffffff");
                colors.add("#000000");
                colorPicker
                        .setDefaultColorButton(Color.parseColor("#f84c44"))
                        .setColors(colors)
                        .setColumns(5)
                        .setRoundColorButton(true)
                        .setOnChooseColorListener(new ColorPicker.OnChooseColorListener() {
                            @Override
                            public void onChooseColor(int position, int color) {
                                textcolor.setBackgroundColor(color);
                                if (position == 1){
                                    textcolor.setTextColor(Color.WHITE);
                                    textcolorvalue = "black";
                                }
                                description.setTextColor(color);
                                textcolor.setBackgroundColor(color);
                                if (position == 0){
                                    textcolor.setTextColor(Color.BLACK);

                                    textcolorvalue = "white";
                                }
                            }

                            @Override
                            public void onCancel() {

                            }
                        }).show();
            }
            });

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.uploading));
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMax(100);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      //  pickFromGallery();
        ImageButton close = findViewById(R.id.close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        if (checkPermissions()){
            showDialog();
        }
    }
    private  boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p:permissions) {
            result = ContextCompat.checkSelfPermission(getApplicationContext(),p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),MULTIPLE_PERMISSIONS );
            return false;
        }
        return true;
    }

    private void showDialog(){
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle(getString(R.string.source));
        String[] pictureDialogItems = {
                getString(R.string.pickvideo),
                getString(R.string.selectcamera) };
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                pickFromGallery();
                                dialog.dismiss();
                                break;
                            case 1:
                                takeVideoFromCamera();
                                dialog.dismiss();
                                break;
                        }
                    }
                });
        pictureDialog.show();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            final Uri selectedUri = data.getData();
            if (requestCode == VIDEO_TRIMMER) {
                if (selectedUri != null) {
                    VideoView preview = findViewById(R.id.video_added);
                    preview.setVideoURI(selectedUri);
                    preview.start();
                    post.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            uploadVideoCloudinary(selectedUri);
                        }
                    });
                }
            }
            if (requestCode == Gallery) {
                if (selectedUri != null) {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(getApplicationContext(), selectedUri);
                    String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    long timeInMillisec = Long.parseLong(time);
                    retriever.release();
                    if (timeInMillisec <= 17500) {
                        VideoView preview = findViewById(R.id.video_added);
                        preview.setVideoURI(selectedUri);
                        preview.start();
                        post.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                uploadVideoCloudinary(selectedUri);
                            }
                        });
                    } else {
                        Toast.makeText(PostActivity.this, getString(R.string.toolong), Toast.LENGTH_SHORT).show();
                        startTrimActivity(selectedUri);
                    }
                } else {
                    Toast.makeText(PostActivity.this, getString(R.string.novideoselected), Toast.LENGTH_SHORT).show();
                }
            }

            if (requestCode == Camera) {
            if (selectedUri != null) {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//use one of overloaded setDataSource() functions to set your data source
                retriever.setDataSource(getApplicationContext(), selectedUri);
                String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long timeInMillisec = Long.parseLong(time);
                retriever.release();
                if (timeInMillisec <= 17000) {
                    VideoView preview = findViewById(R.id.video_added);
                    preview.setVideoURI(selectedUri);
                    preview.start();
                    TextView post = findViewById(R.id.post);
                    post.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            uploadVideoCloudinary(selectedUri);
                        }
                    });
                } else {
                        Toast.makeText(PostActivity.this, getString(R.string.toolong), Toast.LENGTH_SHORT).show();
                        startTrimActivity(selectedUri);

                }
            }else {
                Toast.makeText(PostActivity.this, getString(R.string.novideoselected), Toast.LENGTH_SHORT).show();
            }
        }
        }
        if (resultCode == Activity.RESULT_OK
                && requestCode == SandriosCamera.RESULT_CODE
                && data != null) {
            if (data.getSerializableExtra(SandriosCamera.MEDIA) instanceof Media) {
                Media media = (Media) data.getSerializableExtra(SandriosCamera.MEDIA);
                VideoView preview = findViewById(R.id.video_added);
                preview.setVideoURI(Uri.parse(media.getPath()));
                preview.start();
                preview.start();
                TextView post = findViewById(R.id.post);
                post.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        uploadVideoCloudinary(Uri.parse(media.getPath()));
                    }
                });
            }
        }
}
    private void startTrimActivity(@NonNull Uri uri) {
            Intent intent = new Intent(this, TrimVideoActivity.class);
            intent.putExtra(EXTRA_VIDEO_PATH, FileUtils.getPath(this, uri));
            startActivityForResult(intent, VIDEO_TRIMMER);

    }

    private void pickFromGallery() {

            Intent intent = new Intent();
            intent.setTypeAndNormalize("video/*");
            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Video"), Gallery);


    }
    private void takeVideoFromCamera() {

        SandriosCamera
                .with()
                .setShowPicker(true)
                .setVideoFileSize(30)
                .setMediaAction(CameraConfiguration.MEDIA_ACTION_VIDEO)
                .launchCamera(activity);

    }
    private void uploadVideoCloudinary(Uri videoUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user.getUid();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        mProgressDialog.show();
        String postid = reference.push().getKey();
        MediaManager.get().upload(videoUri)
                .maxFileSize(62914560)
                .option("public_id", "frooze/posts/" + uid + "/" + postid)
                .option("resource_type", "video")
                .unsigned("frooze")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        //ringProgressBar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        double progress = (double) bytes/totalBytes;
                        progress = progress * 100;
                        Log.i("progress", String.valueOf(progress));
                        //ringProgressBar.setProgress((int)progress);
                        mProgressDialog.setProgress((int)progress);
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String text = description.getText().toString();
                        if (!text.isEmpty()){
                            String[] hashtags = text.split(" ");
                            List<String> tags = new ArrayList<String>();
                            for ( String hashtag : hashtags) {
                                if (hashtag.substring(0, 1).equals("#")) {
                                    tags.add(hashtag);
                                    String hashtagwithouthash = hashtag.replace("#","").toLowerCase();
                                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Hashtags").child(hashtagwithouthash);
                                    reference.child("posts").child(postid).setValue(true);
                                }
                            }
                        }
                        String hls = resultData.get("secure_url").toString().replace("mp4", "m3u8");
                        String lowquality = hls.replace("/video/upload/", "/video/upload/q_40/");
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        String uid = user.getUid();
                        Switch dangerousswitch = findViewById(R.id.dangerousswitch);
                        Boolean switchState = dangerousswitch.isChecked();
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("dangerous", switchState.toString());
                        hashMap.put("postid", postid);
                        hashMap.put("postvideo", lowquality);
                        hashMap.put("description", description.getText().toString());
                        hashMap.put("trendingviews", 0);
                        hashMap.put("textcolor", textcolorvalue);
                        hashMap.put("publisher", uid);
                        reference.child(postid).setValue(hashMap);
                        Toast.makeText(getApplicationContext(), getString(R.string.success),Toast.LENGTH_LONG).show();
                        mProgressDialog.dismiss();
                        finish();
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(getApplicationContext(), getString(R.string.fail),Toast.LENGTH_LONG).show();
                        //ringProgressBar.setVisibility(View.INVISIBLE);
                        mProgressDialog.dismiss();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {

                    }
                })
                .dispatch(getApplicationContext());
    }

    /**
     * Requests given permission.
     * If the permission has been denied previously, a Dialog will prompt the user to grant the
     * permission, otherwise it is requested directly.
     */
    private void requestPermission(final String permission, String rationale, final int requestCode, String title, String description) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(title);
            builder.setMessage(description);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(PostActivity.this, new String[]{permission}, requestCode);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissionsList[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS:{
                if (grantResults.length > 0) {
                    String permissionsDenied = "";
                    for (String per : permissionsList) {
                        if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                            permissionsDenied += "\n" + per;
                            Toast.makeText(getApplicationContext(), "These permissions are necessary", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                    showDialog();
                }
                return;
            }
        }
    }
}