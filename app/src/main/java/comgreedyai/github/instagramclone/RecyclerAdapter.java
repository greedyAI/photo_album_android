package comgreedyai.github.instagramclone;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Waley on 2017/11/20.
 */

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.PhotoViewHolder> {

    private ArrayList<Photo> photos;

    public RecyclerAdapter(ArrayList<Photo> photos) {
        this.photos = photos;
    }

    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_item, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final PhotoViewHolder holder, int position) {
        final Photo photo = photos.get(position);
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String reportDate = df.format(photo.getDate());
        String text;
        if (photo.getDescription() == null || photo.getDescription().equals("")) {
            text = reportDate;
        } else {
            text = reportDate + "\r\n" + photo.getDescription();
        }
        holder.tv.setText(text);
        holder.key = "VERSIX" + photo.getDate().toString();
        String url = "http://ec2-54-210-25-34.compute-1.amazonaws.com/94b37c98a64e0a85b8c69a34a7b97e59c0/GET/" + photo.getDate().toString() + ".jpg";
        Picasso.with(holder.iv.getContext()).load(url).into(holder.iv);

        // clicking the floating action button on the top right of every picture deletes it from the server and the app's display
        holder.picFAB.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                RedisService.getService().deletePost("VERSIX" + photo.getDate().toString()).enqueue(new Callback<RedisService.DelResponse>() {
                    @Override
                    public void onResponse(Call<RedisService.DelResponse> call, Response<RedisService.DelResponse> response) {
                    }

                    @Override
                    public void onFailure(Call<RedisService.DelResponse> call, Throwable t) {
                    }
                });
                RedisService.getService().deletePost(photo.getDate().toString()).enqueue(new Callback<RedisService.DelResponse>() {
                    @Override
                    public void onResponse(Call<RedisService.DelResponse> call, Response<RedisService.DelResponse> response) {
                    }

                    @Override
                    public void onFailure(Call<RedisService.DelResponse> call, Throwable t) {
                    }
                });
                photos.remove(photo);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.dateDescription)
        TextView tv;
        public String key;
        @BindView(R.id.image)
        ImageView iv;
        @BindView(R.id.picFAB)
        FloatingActionButton picFAB;

        public PhotoViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }

}
