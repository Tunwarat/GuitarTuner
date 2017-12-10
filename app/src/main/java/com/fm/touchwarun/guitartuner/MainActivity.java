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
    int k = 0;

    FFT fft = new FFT(blockSize);
//    FFT fft = new FFT(8);

    RecordAudio recordAudio;
    boolean started = false;

    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tv = (TextView) findViewById(R.id.sample_text);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Start/Stop Recording", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                startExecute();
                tv.setText("" + started);
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

    private void startExecute() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startRecording();
        } else {
            requestRecordAudioPermission();
        }
    }

    private void startRecording() {
        if (started) {
            started = false;
            recordAudio.cancel(true);
        } else {
            started = true;
            recordAudio = new RecordAudio();
            recordAudio.execute();
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

    private class RecordAudio extends AsyncTask<Void, Double, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding);
            AudioRecord audioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, audioEncoding, bufferSize);

            short[] buffer = new short[blockSize];

            double[] re = new double[blockSize];
            double[] im = new double[blockSize];
            double[] magnitude = new double[blockSize];
            double[] toTransform = new double[blockSize];
//            double[] toTransform = new double[8];
            double[] zeros = new double[blockSize];

            try {
                audioRecord.startRecording();  //Start
            } catch (Throwable t) {
                Log.e("AudioRecord", "Recording Failed");
            }

            while (started) {
                Log.d("k", "" + k);
                int bufferReadResult = audioRecord.read(buffer, 0, blockSize);

//                Log.d("buffer", "" + buffer[0]);
                Log.d("BufferReadResult", "" + bufferReadResult);

//                Log.d("BufferReadResult_for", "" + bufferReadResult);
                for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                    toTransform[i] = (double) buffer[i] / 32768.0;
                    zeros[i] = 0;
                }
//                Log.d("started", "started: " + started);

// ----------------------for FTT --------------------------------
//                fft.fft(toTransform, zeros);
//                double[] R = {7, 8, 9, 20, 21, 90, 21, 90};
//                double[] I = {0, 0, 0, 0, 0,0, 0, 0};
//
//                fft.fft(R, I);
                fft.fft(toTransform, zeros);
// -------------------------------------------------------------------

//                Log.d("Buffer: ", "" + bufferReadResult);

                for (int i = 0; i < blockSize; i++) {
                    try {
                        re[i] = toTransform[i];
                        im[i] = zeros[i];
//                        magnitude[i] = Math.sqrt((R[i] * R[i]) + (I[i] * I[i]));
                        magnitude[i] = Math.sqrt((re[i] * re[i]) + (im[i] * im[i]));
                    } catch (ArrayIndexOutOfBoundsException e) {
//                        Log.e("ArrayIndexOutOfBounds", "NULL");
                    }
                }

                double peak = -1.0;
                // Get the largest magnitude peak
                for (int i = 0; i < blockSize; i++) {
                    if (peak < magnitude[i])
                        peak = magnitude[i];
                }

                // calculated the frequency
                frequency = (sampleRate * peak) / blockSize;

                Log.e("frequency = ", "" + frequency);
//                Log.e("peak = ", "" + peak);


                Log.d("-------------------", "----------------------------");
                k++;

                publishProgress(frequency);
                try {
                    audioRecord.stop();
                } catch (IllegalStateException e) {
//                    Log.e("Stop failed", e.toString());
                }


            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Double... frequencies) {
            String info = Double.toString(frequencies[0]);
//            Log.d("frequency = ", info);
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
