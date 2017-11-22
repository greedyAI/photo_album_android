package comgreedyai.github.instagramclone;

import android.graphics.Bitmap;
import android.widget.ImageView;

import java.util.Date;

/**
 * Created by Waley on 2017/11/20.
 */

public class Photo {

    private String url;
    private Date date;
    private String description;

    public Photo(String url, Date date, String description) {
        this.url = url;
        this.date = date;
        this.description = description;
    }

    public String getURL() {
        return url;
    }

    public Date getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }
}
