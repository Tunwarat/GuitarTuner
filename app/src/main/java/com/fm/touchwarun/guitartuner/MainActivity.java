package com.fm.touchwarun.guitartuner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.fm.touchwarun.guitartuner.fft.FFT;


public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private static final int PERMISSION_REQUEST_RECORDAUDIO = 0;

    int audioSource = MediaRecorder.AudioSource.MIC;
    int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    int blockSize = 256;
    int sampleRate = 8000;
    public double frequency = 0.0;

    FFT fft = new FFT(blockSize); // -------------------------

    boolean started = false;

    TextView tv;
    TextView tvFrequency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tv = (TextView) findViewById(R.id.sample_text);
        tvFrequency = (TextView) findViewById(R.id.frequency);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Start/Stop Recording", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                startRecording();
            }
        });

        // Example of a call to a native method
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_RECORDAUDIO) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(R.id.main_layout), "Recording Audio permission was granted, start recording",
                        Snackbar.LENGTH_SHORT)
                        .show();
                startRecording();
            } else {
                Snackbar.make(findViewById(R.id.main_layout), "Recording Audio permission request was denied.",
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
    }


    private void startRecording() {
        tv.setText("" + started);
        if (started) {
            started = false;
            return;
        } else {
            started = true;
        }
        getRecord();
    }


    private void requestRecordAudioPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            Snackbar.make(findViewById(R.id.main_layout), "Camera access is required to display the camera preview.",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            PERMISSION_REQUEST_RECORDAUDIO);
                }
            }).show();
        } else {
            Snackbar.make(findViewById(R.id.main_layout), "Permission is not available. Requesting Recording audio permission,",
                    Snackbar.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_RECORDAUDIO);
        }
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

    void getRecord() {
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding);
        AudioRecord audioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, audioEncoding, bufferSize);
        Log.d("bufferSize", bufferSize + "");
        short[] buffer = new short[blockSize];

        double[] magnitude = new double[blockSize];
        double[] re = new double[blockSize];
        double[] im = new double[blockSize];
        double[] singleSpectrum = new double[blockSize / 2];

        try {
            audioRecord.startRecording();  //Start
        } catch (Throwable t) {
//            Log.e("AudioRecord", "Recording Failed");
        }
        long startTime = System.currentTimeMillis(); //fetch starting time

        double avgFrequency = 0;
        int n = 0;

        while ((false || (System.currentTimeMillis() - startTime) < 5000) && started) {


            int bufferReadResult = audioRecord.read(buffer, 0, blockSize);
            for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                re[i] = (double) buffer[i] / 32767.0;
                im[i] = 0;
            }

// ----------------------for FTT --------------------------------

            fft.fft(re, im);
// -------------------------------------------------------------------


            for (int i = 0; i < blockSize / 2; i++) {
                singleSpectrum[i] = Math.sqrt((re[i] * re[i]) + (im[i] * im[i])) / blockSize;
            }

            double peak = -1.0;
            int index = -1;
            // Get the largest magnitude peak
            for (int i = 0; i < blockSize / 2; i++) {
                if (peak < singleSpectrum[i]) {
                    peak = singleSpectrum[i];
                    index = i;
                }
            }

            // calculated the frequency

            frequency =  sampleRate/blockSize*index;
            Log.d("freq", "" + frequency);
            n++;
            avgFrequency += frequency;
        }
        avgFrequency /= n;
        tv.setText("avg frequency: " + avgFrequency);

        try {
            audioRecord.stop();  //Start
        } catch (Throwable t) {
//            Log.e("AudioRecord", "Recording Failed");
        }
        audioRecord.release();
    }



    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
