package com.fm.touchwarun.guitartuner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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

    int N = 512;
    int sampleRate = 4000;
    public double frequency;

    FFT fft = new FFT(N); // -------------------------

    boolean started = false;//when application is launched, the recorder is not starting yet.

    TextView tv;
    TextView tvFrequency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // starting write our own code
        tv = (TextView) findViewById(R.id.sample_text);
        tvFrequency = (TextView) findViewById(R.id.frequency);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() { //รอรับคลิ๊ก
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Start/Stop Recording", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                startRecording();
            }
        });

        // Example of a call to a native method
    }

    private void startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

            tv.setText("" + started);
            if (started) {
                started = false;
                return;
            } else {
                started = true;
            }
            getRecord();
        } else {
            requestRecordAudioPermission();
        }

    }

    void getRecord() {
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding);//ถ้าจะใช้AudioRecord ต้องใช้ getMin
        AudioRecord audioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, audioEncoding, bufferSize);//AudioRecord คือobjectที่เทำให้ราใช้เครื่องมือ .startrec .read .stop ...
//        Log.d("bufferSize", bufferSize + "");
        short[] buffer = new short[N];

        double[] re = new double[N]; //ในการโยนค่าเข้าฟูเรียร์เราต้องโยนไปเป็นทั้งค่าจริงและค่าอิมเมจิ้น เราเลยต้องสร้างอารเรย์มาเก็บค่าที่ได้จากบัฟเฟอร์แล้วค่อยส่งให้ฟูเรียร์
        double[] im = new double[N]; //Why not using short? : for precise calculation in FFT.
        double[] singleSpectrum = new double[N / 2];

        try {
            audioRecord.startRecording();  //Start
        } catch (Throwable t) {
        }

        long startTime = System.currentTimeMillis(); //fetch starting time

        double avgFrequency = 0;
        int n = 0;

        while (((System.currentTimeMillis() - startTime) < 5000) && started) {
            int bufferReadResult = audioRecord.read(buffer, 0, N); //audiorecord.read return values of how many data has been collected into buffer

            for (int i = 0; i < N && i < bufferReadResult; i++) {
                re[i] = (double) buffer[i] / 32768.0;
                im[i] = 0;
            } //convert buffer into real part and imagination part

// ----------------------for FFT -------------------------------------
            fft.fft(re, im); //send real and img part to fft
// -------------------------------------------------------------------
            for (int i = 0; i < N / 2; i++) {
                singleSpectrum[i] = Math.sqrt((re[i] * re[i]) + (im[i] * im[i])) / N;

            } //get singleSpectrum(single side spectrum of buffer)

            double peak = -1.0;
            int index = -1;
            // Get the largest magnitude peak
            for (int i = 0; i < N / 2; i++) {
                if (peak < singleSpectrum[i]) {
                    peak = singleSpectrum[i];
                    index = i;
                }
            } //use loop to find maximum value in singleSpectrum and index of that value
//            peak = 20
//                    index = 7
//            r= 12 14 16 1 2 3 4 20

            // calculated the frequency

            frequency = sampleRate / N * index; //
            Log.d("freq", "" + frequency);
            n++;
            avgFrequency += frequency;
        }
        avgFrequency /= n;
        tv.setText("avg frequency: " + avgFrequency);
//        if(frequency == 440) {
//            tv.setText("A4");
//        }

        try {
            audioRecord.stop();  //Start
        } catch (Throwable t) {
//            Log.e("AudioRecord", "Recording Failed");
        }
        audioRecord.release();
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
}