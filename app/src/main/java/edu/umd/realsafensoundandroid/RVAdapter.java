package edu.umd.realsafensoundandroid;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.List;

import static edu.umd.realsafensoundandroid.MainActivity.location;

public class RVAdapter extends RecyclerView.Adapter<RVAdapter.PersonViewHolder> {

    public static class PersonViewHolder extends RecyclerView.ViewHolder {

        CardView cv;
        TextView title;
        TextView details;
        MLRoundedImageView photo;

        PersonViewHolder(View itemView) {
            super(itemView);
            cv = (CardView)itemView.findViewById(R.id.cardView);
            title = (TextView)itemView.findViewById(R.id.heading);
            details = (TextView)itemView.findViewById(R.id.description);
            photo = (MLRoundedImageView) itemView.findViewById(R.id.picture);
        }
    }

    List<Notification> notifications;

    RVAdapter(List<Notification> notifications){
        this.notifications = notifications;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public PersonViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item, viewGroup, false);
        PersonViewHolder pvh = new PersonViewHolder(v);
        return pvh;
    }

    @Override
    public void onBindViewHolder(PersonViewHolder personViewHolder, int i) {
        try {

            personViewHolder.title.setText(notifications.get(i).name);

                String url = notifications.get(i).photoId;
                Bitmap icon = null;

                DownloadImageTask task = new DownloadImageTask(personViewHolder.photo);
                task.execute(url);

                //personViewHolder.photo.setImageBitmap();
                System.out.println("INDEX: " + i);

                personViewHolder.details.setText(notifications.get(i).place + "\n" + "348 ft from your location\n" + "Estimated altitude of " +  notifications.get(i).location[2] + "ft\n");

                System.out.println("AX");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            try {
                bmImage.setImageBitmap(result);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            System.out.println("POSTEXECUTE");
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }
}