package edu.umd.realsafensoundandroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private AudioRecord audioRecord;
    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private Thread recordingThread = null;
    private boolean isRecording = false;

    private int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private boolean permissionToRecordAccepted = false;
    private boolean permissionToWriteAccepted = false;
    private String[] permissions = {"android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE"};

    int arrays = 0;

    //initialize buffer creation time variable to time just before creation of first buffer
    long bufferCreationTime;

    List<ArrayList<byte[]>> blockedBuffer;
    List<Long> time;

    ArrayList<byte[]> byteBuffer;
    ArrayList<byte[]> byteBufferSecondary;


    private List<Notification> notifications;
    private RecyclerView rv;

    public LocationManager locationManager;
    public static Location location;

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            MainActivity.location = location;
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recyclerview_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        //getActionBar().show();

        ActionBar actionBar = getActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        new DrawerBuilder().withActivity(this).build();

        //if you want to update the items at a later time it is recommended to keep it in a variable
        PrimaryDrawerItem item1 = new PrimaryDrawerItem().withIdentifier(1).withName("Home");
        SecondaryDrawerItem item2 = new SecondaryDrawerItem().withIdentifier(2).withName("Statistics");

        //startActivity(new Intent(this, LoginActivity.class));

//create the drawer and remember the `Drawer` result object
        Drawer result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .addDrawerItems(
                        item1,
                        new DividerDrawerItem(),
                        item2,
                        new SecondaryDrawerItem().withName("Settings")
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        // do something with the clicked item :D
                        return false;
                    }
                })
                .build();

        //modify an item of the drawer
        item1.withName("A new name for this drawerItem").withBadge("19").withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.md_red_700));
//notify the drawer about the updated element. it will take care about everything else
        result.updateItem(item1);

//to update only the name, badge, icon you can also use one of the quick methods
        result.updateName(1, new StringHolder("Name"));

//the result object also allows you to add new items, remove items, add footer, sticky footer, ..
        result.addItem(new DividerDrawerItem());
        result.addStickyFooterItem(new PrimaryDrawerItem().withName("StickyFooterItem"));

//remove items with an identifier
        result.removeItem(2);

//open / close the drawer
        result.openDrawer();
        result.closeDrawer();

//get the reference to the `DrawerLayout` itself
        result.getDrawerLayout();

//        // Assume thisActivity is the current activity
//        for(String s : permissions)
//        int permissionCheck = ContextCompat.checkSelfPermission(this,
//                Manifest.permission.WRITE_CALENDAR);
//

        try {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000,
                    1, mLocationListener);
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        catch(Exception e){
            e.printStackTrace();
        }
//
            rv = (RecyclerView) findViewById(R.id.rv);

            LinearLayoutManager llm = new LinearLayoutManager(this);
            rv.setLayoutManager(llm);
            rv.setHasFixedSize(true);

            initializeData();

            RetrieveFeedTask json = new RetrieveFeedTask();
            json.execute();

            while(!json.isCancelled()){

            }

            System.out.println("UI PREP");
            initializeAdapter();
            System.out.println("UI FINISHED");

//        catch(Exception e){
//            e.printStackTrace();
//        }

        //startRecording();
    }

    private static JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {
        HttpURLConnection urlConnection = null;
        URL url = new URL(urlString);
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000 /* milliseconds */ );
        urlConnection.setConnectTimeout(15000 /* milliseconds */ );
        urlConnection.setDoOutput(true);
        urlConnection.connect();

        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }
        br.close();

        String jsonString = sb.toString();
        System.out.println("JSON: " + jsonString);

        return new JSONObject(jsonString);
    }

    private void initializeData(){
        notifications = new ArrayList<>();

        //notifications.add(new Notification("A", "B", "C", "D", 100, 100));

        /*
        persons.add(new Notification());

        persons.add(new Notification("Emma Wilson", "23 years old", R.drawable.emma));
        persons.add(new Notification("Lavery Maiss", "25 years old", R.drawable.lavery));
        persons.add(new Notification("Lillie Watts", "35 years old", R.drawable.lillie));
        */
    }

    class RetrieveFeedTask extends AsyncTask<Void, Void, Integer> {

        private Exception exception;

        protected Integer doInBackground(Void... urls) {
            try {

                JSONObject obj = null;

                try {
                    obj = getJSONObjectFromURL("https://fe37fbf9.ngrok.io/api/v1/receive");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {

                    Iterator x = obj.keys();
                    JSONArray jsonArray = new JSONArray();

                    while (x.hasNext()){
                        String key = (String) x.next();
                        jsonArray.put(obj.get(key));
                    }

                    System.out.println(jsonArray);

                        for (int i = 0; i < jsonArray.length(); i++) {
                            Notification notification = null;
                            long id = 0;
                            String title = "";
                            String coords[] = new String[3];
                            String place = "";
                            String photo = "";
                            String suggestedAction = "";
                            long time = 0;

                            //time of request
                            if (i == 0) {
                                id = (long) jsonArray.get(i);
                                System.out.println(id);
                            }
                            else {
                                JSONArray event = (JSONArray) jsonArray.get(i);
                                System.out.println("EVENT: " + event);
                                System.out.println(event.length());
                                for(int j = 0; j < 6; j++) {
                                    //title
                                    if(j == 0) {
                                        System.out.println(event.get(j));
                                        title = (String) event.getJSONObject(j).getString("title");
                                    }
                                    //location
                                    if(j == 1) {
                                        JSONObject location = event.getJSONObject(0).getJSONObject("location");
                                        System.out.println("LO" + location);
                                        for(int l = 0; l < location.length(); l++) {
                                            if(l == 0)
                                                coords[0] = location.getDouble("lat") + " ";
                                            if(l == 1)
                                                coords[1] = location.getDouble("lng") + " ";
                                            if(l == 2)
                                                coords[2] = location.getDouble("elev") + " ";
                                        }
                                    }
                                    if(j == 2) {
                                        place = (String) event.getJSONObject(0).getString("place");
                                    }
                                    if(j == 3) {
                                        photo = (String) event.getJSONObject(0).getString("photo");
                                    }
                                    if(j == 4) {
                                        suggestedAction = (String) event.getJSONObject(0).getString("suggested_action");
                                    }
                                    if(j == 5) {
                                        time = Long.parseLong((String) event.getJSONObject(0).getString("time"));
                                    }
                                }
                            }

                            System.out.println(id + " " + title + " " + coords + " " + place + " " + photo + " " + suggestedAction + " " + time);

                            notification = new Notification(title, coords, place, photo, id, time);

                            if(title != "" && place != null && photo != null && time != 0) {
                                notifications.add(notification);
                                System.out.println("ADDED");
                            }
                        }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {
                this.exception = e;

                return null;
            }

            cancel(true);

            return 1;
        }

        protected void onPostExecute(Integer... fields) {
            // TODO: check this.exception
            // TODO: do something with the feed
            System.out.println("ESCAPIST");
        }
    }

    private void initializeAdapter(){
        RVAdapter adapter = new RVAdapter(notifications);
        System.out.println("ADAPTER");
        try {
            rv.setAdapter(adapter);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("FINISH");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public AudioRecord getAudioRecord() {
        try {
            int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);
            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);
            if (recorder.getState() == AudioRecord.STATE_INITIALIZED){
                return recorder;
            }
        } catch (Exception e) {
            Log.i("AudioRecording", "Error in Audio Record");
        }
        return null;
    }

    private void startRecording() {

        for (int rate : new int[] {44100, 22050, 11025, 16000, 8000}) {  // add the rates you wish to check against
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_CONFIGURATION_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                // buffer size is valid, Sample rate supported
                RECORDER_SAMPLERATE = rate;
                break;
            }
        }

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, 2048);
        Log.d("TAG", "" + audioRecord);

        audioRecord.startRecording();
        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }



    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }


    public void sendPost(final long time, final Byte[] adjacentWaveform, final int index, final double lat, final double lng, final double ele) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://fe37fbf9.ngrok.io/api/v1/send");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept","application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("time", time);
                    //jsonParam.put("surrounding_waveform", adjacentWaveform);
                    //jsonParam.put("index_of_loudest", index);
                    jsonParam.put("latitude", lat);
                    jsonParam.put("longitude", lng);
                    jsonParam.put("elevation", ele);


                    Log.i("JSON", jsonParam.toString());
                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG" , conn.getResponseMessage());

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    private double calculateAverage(List <Integer> marks) {
        Integer sum = 0;
        if(!marks.isEmpty()) {
            for (Integer mark : marks) {
                sum += mark;
            }
            return sum.doubleValue() / marks.size();
        }
        return sum;
    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte

        String filePath = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator + "file.mp3";
        String filePathTime = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM + File.separator + "time.mp3";
        short sData[] = new short[BufferElements2Rec];

        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(filePath);
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        long startTime = time();
        long elapsed = 0;
        final long BUFFER_SIZE = 2000; //in millis
        final long BLOCK_SIZE = 50; //in millis
        final long BLOCKS_PER_BUFFER = BUFFER_SIZE / BLOCK_SIZE;
        arrays = 0;
        //initialize buffer creation time variable to time just before creation of first buffer
        long bufferCreationTime = time();

        blockedBuffer = new ArrayList<ArrayList<byte[]>>();
        for(int i = 0; i < BLOCKS_PER_BUFFER; i++)
            blockedBuffer.add(i, new ArrayList<byte[]>());

        time = new ArrayList<Long>();

        byteBuffer = new ArrayList<byte[]>();
        byteBufferSecondary = new ArrayList<byte[]>();

        while (isRecording) {
            // gets the voice output from microphone to byte format

            //TODO: absolute value averages of block data

            final int BYTE_ARRAY_SIZE = 1024;
            final int BUFFER_STACK = 50;
            final int TIME_BYTE_ARRAY_SIZE = 8;

            //read the next short array of audio data
            audioRecord.read(sData, 0, BufferElements2Rec);
            System.out.println("" + elapsed + " " + " " + BUFFER_SIZE);
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                final byte bData[] = short2byte(sData);
                //os.write(bData, 0, BufferElements2Rec * BytesPerElement);
                System.out.println("Byte writing to file" + " " + elapsed + " " + Arrays.toString(bData));
                byteBuffer.add(bData);
                arrays++;
                time.add(time());
                elapsed = time() - startTime;

                if(arrays > BUFFER_STACK){
                    Thread thread = new Thread() {
                        @Override
                        public void run(){
                            List<Byte> unifiedBuffer = new ArrayList<Byte>();

                            List<Integer> averages = new ArrayList<>();
                            int localAverage;

                            for(int i = 0; i < byteBuffer.size(); i++){
                                localAverage = 0;
                                for(int j = 0; j < byteBuffer.get(i).length; j++){
                                    localAverage += Math.abs(byteBuffer.get(i)[j] & 0xFF);
                                }
                                localAverage /= byteBuffer.get(0).length;
                                averages.add(localAverage);
                            }

                            @SuppressLint("MissingPermission") Location position = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            double latitude = position.getLatitude();
                            double longitude = position.getLongitude();
                            double elevation = position.getAltitude();

                            int maxAverage = Collections.max(averages);
                            double meanAverage = calculateAverage(averages);
                            int maxIndex = averages.indexOf(maxAverage);
                            System.out.println("MAX: " + maxAverage + " " + time.get(maxIndex) + " " + meanAverage);

                            if(maxAverage > 1000){

                                sendPost(time.get(maxIndex), byteBuffer.toArray(new Byte[byteBuffer.size()]), maxIndex, latitude, longitude, elevation);
                            }

                            byteBuffer = new ArrayList<byte[]>();

                            arrays %= BUFFER_STACK;
                        }
                    };
                    thread.start();
                }

                /*
                if(elapsed > BUFFER_SIZE) {

                    System.out.println(time);

                    for (int i = 0; i < time.size(); i++)
                        time.set(i, time.get(i) - bufferCreationTime);

                    System.out.println(time);

                    System.out.println("TAG" + blockedBuffer);
                }

                    int k = 0;

                    while(k < byteBuffer.size()) {
                        long blockIncrementor = BLOCK_SIZE;
                        long timeStamp = time.get(0);
                        int blockNumber = 0;
                        byte[] currentByteArray;
                        ArrayList<byte[]> currentBlock;

                        for (int i = 0; i < time.size(); i++) {

                            currentBlock = blockedBuffer.get(blockNumber);

                            while (timeStamp < blockIncrementor) {
                                currentByteArray = new byte[BYTE_ARRAY_SIZE];
                                currentByteArray = byteBuffer.get(k);
                                k++;

                                //is.read(currentByteArray);
                                currentBlock.add(currentByteArray);
                                arrays--;
                                i++;
                                timeStamp = time.get(i);
                            }

                            blockIncrementor++;
                            blockNumber++;
                        }
                        */

                    /*
                    byte[][] buffer = new byte[0][0];
                    byte[] rawBuffer = new byte[0];
                    byte[] block = new byte[0];
                    List<Byte> arrayAssembly;
                    byte[] byteArrayAssembly;
                    byte[] nextAudioByteArray;
                    byte[] nextTimeByteArray;
                    long relativeTime;
                    long currentBlockStartTime;

                    if()
                    while(arrays > 0){
                        nextAudioByteArray = new byte[BYTE_ARRAY_SIZE];
                        is.read(nextAudioByteArray);

                        nextTimeByteArray = new byte[TIME_BYTE_ARRAY_SIZE];
                        isTime.read(nextTimeByteArray);
                        long relativeTime = ByteBuffer.allocate(8).getLong();

                        mergeByteArrays(rawBuffer, nextAudioByteArray);
                        arrays--;
                    }
                }
                */

                //byteBufferSecondary = byteBuffer;
                //byteBuffer = new ArrayList<byte[]>();

                //set new buffer creation time
                bufferCreationTime = time();



            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        /*
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != audioRecord) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            recordingThread = null;
        }
    }

    private static long time() {
        return SystemClock.elapsedRealtime();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 200:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToWriteAccepted  = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) MainActivity.super.finish();
        if (!permissionToWriteAccepted ) MainActivity.super.finish();

    }
}
