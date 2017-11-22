package comgreedyai.github.instagramclone;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CaptureActivity extends AppCompatActivity {
    @BindView(R.id.description_text)
    EditText description;
    @BindView(R.id.picture_view)
    ImageView image;
    @BindView(R.id.retake_photo)
    Button retake;
    @BindView(R.id.cancel)
    Button cancel;
    @BindView(R.id.save_photo)
    Button save;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_CANCELLED = 2;
    private String mCurrentPhotoPath;
    private String mCurrentPhotoName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        ButterKnife.bind(this);
        final Activity captureActivity = this;
        String url = getIntent().getStringExtra(getString(R.string.photos));
        String name = getIntent().getStringExtra(getString(R.string.photo_name));
        mCurrentPhotoPath = url;
        mCurrentPhotoName = name;
        File imgFile = new File(url);
        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            image.setImageBitmap(myBitmap);
        }
        retake.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((ContextCompat.checkSelfPermission(captureActivity,
                        Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(captureActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(captureActivity,
                        Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED)) {
                    dispatchTakePictureIntent();
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // uploads both the image and the photo object to the Redis server
        // the image is for displaying in the app and the photo object
        // is for retrieval purposes on app restart
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Date date = Calendar.getInstance().getTime();
                final Photo newPhoto = new Photo(date.toString(), date, description.getText().toString());
                final String name = "VERSIX" + date.toString();
                final File imgFile = new File(mCurrentPhotoPath);
                RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), imgFile);
                RedisService.getService().makePost(name, newPhoto).enqueue(new Callback<RedisService.SetResponse>() {
                    @Override
                    public void onResponse(Call<RedisService.SetResponse> call, Response<RedisService.SetResponse> response) {
                    }

                    @Override
                    public void onFailure(Call<RedisService.SetResponse> call, Throwable t) {
                        Toast.makeText(captureActivity, t.toString(), Toast.LENGTH_LONG).show();
                    }
                });
                RedisService.getService().postImage(date.toString(), reqFile).enqueue(new Callback<RedisService.SetResponse>() {
                    @Override
                    public void onResponse(Call<RedisService.SetResponse> call, Response<RedisService.SetResponse> response) {
                        Intent intent = new Intent();
                        intent.putExtra(getString(R.string.url), mCurrentPhotoName);
                        intent.putExtra(getString(R.string.photos), mCurrentPhotoPath);
                        intent.putExtra(getString(R.string.timestamp), date);
                        intent.putExtra(getString(R.string.description), newPhoto.getDescription());
                        setResult(4, intent);
                        finish();
                    }

                    @Override
                    public void onFailure(Call<RedisService.SetResponse> call, Throwable t) {
                        Toast.makeText(captureActivity, t.toString(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                mCurrentPhotoName = photoURI.toString();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    // returns back to photo editing activity after retaking photo
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == REQUEST_CANCELLED) {
                Toast.makeText(this, "Cancelled!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
                File imgFile = new File(mCurrentPhotoPath);
                if (imgFile.exists()) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    image.setImageBitmap(myBitmap);
                }
            }
        }
    }


}
