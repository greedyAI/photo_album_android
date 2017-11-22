package comgreedyai.github.instagramclone;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.R.attr.permission;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.recycler)
    RecyclerView mainPageView;
    @BindView(R.id.myFAB)
    FloatingActionButton myFab;
    private RecyclerView.ViewHolder mainPageViewHolder;
    private RecyclerView.Adapter myAdapter;
    private static final int MY_PERMISSIONS_REQUEST_APP = 1;
    private ArrayList<Photo> photos = new ArrayList<Photo>();
    private RecyclerView.LayoutManager layoutManager;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private String mCurrentPhotoPath;
    private String mCurrentPhotoName;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_CANCELLED = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        final Activity mainActivity = this;
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET},
                    MY_PERMISSIONS_REQUEST_APP);
        }
        myAdapter = new RecyclerAdapter(photos);
        layoutManager = new LinearLayoutManager(this);
        mainPageView.setLayoutManager(layoutManager);
        mainPageView.setAdapter(myAdapter);

        // on app start, loads all existing photos from the server onto the app, and then notify the adapter to update the app display
        RedisService.getService().allKeys("VERSIX*").enqueue(new Callback<RedisService.KeysResponse>() {
            @Override
            public void onResponse(Call<RedisService.KeysResponse> call, Response<RedisService.KeysResponse> response) {
                for (final String k : response.body().keys) {
                    RedisService.getService().getPost(k).enqueue(new Callback<RedisService.GetResponse>() {
                        @Override
                        public void onResponse(Call<RedisService.GetResponse> call, Response<RedisService.GetResponse> response) {
                            Photo p = response.body().item;
                            photos.add(p);
                            myAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onFailure(Call<RedisService.GetResponse> call, Throwable t) {
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<RedisService.KeysResponse> call, Throwable t) {
            }
        });
        myFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if ((ContextCompat.checkSelfPermission(mainActivity,
                        Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(mainActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(mainActivity,
                        Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED)) {
                    dispatchTakePictureIntent();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_APP: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, R.string.persistent_toast, Toast.LENGTH_LONG).show();
                    return;
                }
                return;
            }
        }
    }

    private File createImageFile() throws IOException {
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

    // starts photo editing activity when photo successfully taken
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == REQUEST_CANCELLED) {
                Toast.makeText(this, "Cancelled!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, CaptureActivity.class);
                intent.putExtra(getString(R.string.photos), mCurrentPhotoPath);
                intent.putExtra(getString(R.string.photo_name), mCurrentPhotoName);
                startActivityForResult(intent, 3);
            }
        } else {
            if (resultCode == 4) {
                Bundle extra = data.getExtras();
                Photo newPhoto = new Photo((String) extra.get(getString(R.string.url)), (Date) extra.get(getString(R.string.timestamp)), (String) extra.get(getString(R.string.description)));
                photos.add(newPhoto);
                myAdapter.notifyDataSetChanged();
            }
        }
    }
}
