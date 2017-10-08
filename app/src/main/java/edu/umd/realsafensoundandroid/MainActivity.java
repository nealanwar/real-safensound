package edu.umd.realsafensoundandroid;

import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
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
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AudioRecord audioRecord;
    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private Thread recordingThread = null;
    private boolean isRecording = false;

    private int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private boolean permissionToRecordAccepted = false;
    private boolean permissionToWriteAccepted = false;
    private String [] permissions = {"android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE"};

    private List<Notification> persons;
    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recyclerview_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        int requestCode = 200;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, requestCode);
        }

        rv=(RecyclerView)findViewById(R.id.rv);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        rv.setHasFixedSize(true);

        RetrieveFeedTask json = new RetrieveFeedTask();
        json.execute();

        initializeData();
        initializeAdapter();

        startRecording();
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
        persons = new ArrayList<>();

        /*
        persons.add(new Notification());

        persons.add(new Notification("Emma Wilson", "23 years old", R.drawable.emma));
        persons.add(new Notification("Lavery Maiss", "25 years old", R.drawable.lavery));
        persons.add(new Notification("Lillie Watts", "35 years old", R.drawable.lillie));
        */
    }

    class RetrieveFeedTask extends AsyncTask<Void, Void, Void> {

        private Exception exception;

        protected Void doInBackground(Void... urls) {
            try {

                JSONObject obj = null;

                try {
                    obj = getJSONObjectFromURL("http://safensound.tech/api/receive");
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    JSONArray array = obj.getJSONArray("GET");
                    for (int i = 0; i < array.length(); i++)
                        System.out.println(array.get(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {
                this.exception = e;

                return null;
            }

            return null;
        }

        protected void onPostExecute(Void... fields) {
            // TODO: check this.exception
            // TODO: do something with the feed
        }
    }

    private void initializeAdapter(){
        RVAdapter adapter = new RVAdapter(persons);
        rv.setAdapter(adapter);
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

        audioRecord = getAudioRecord();
        Log.d("TAG", "" + audioRecord);

        //audioRecord.startRecording();
        isRecording = true;
        /*
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
        */
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
        int arrays = 0;
        //initialize buffer creation time variable to time just before creation of first buffer
        long bufferCreationTime = time();

        List<ArrayList<byte[]>> blockedBuffer = new ArrayList<ArrayList<byte[]>>();
        for(int i = 0; i < BLOCKS_PER_BUFFER; i++)
            blockedBuffer.add(i, new ArrayList<byte[]>());

        List<Long> time = new ArrayList<Long>();

        ArrayList<byte[]> byteBuffer = new ArrayList<byte[]>();
        ArrayList<byte[]> byteBufferSecondary = new ArrayList<byte[]>();

        while (isRecording) {
            // gets the voice output from microphone to byte format

            //TODO: absolute value averages of block data

            final int BYTE_ARRAY_SIZE = 1024000000;
            final int TIME_BYTE_ARRAY_SIZE = 8;

            //read the next short array of audio data
            audioRecord.read(sData, 0, BufferElements2Rec);
            System.out.println("Short writing to file" + " " + elapsed + " " + Arrays.toString(sData));
            System.out.println("" + elapsed + " " + " " + BUFFER_SIZE);
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                final byte bData[] = short2byte(sData);
                //os.write(bData, 0, BufferElements2Rec * BytesPerElement);
                byteBuffer.add(bData);
                arrays++;
                time.add(time());

                elapsed = time() - startTime;

                if(elapsed > BUFFER_SIZE) {

                    System.out.println(time);

                    for (int i = 0; i < time.size(); i++)
                        time.set(i, time.get(i) - bufferCreationTime);

                    System.out.println(time);

                    System.out.println("TAG" + blockedBuffer);
                }

                    /*
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

                byteBufferSecondary = byteBuffer;
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
