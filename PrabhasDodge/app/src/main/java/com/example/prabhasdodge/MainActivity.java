package com.example.prabhasdodge;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    Bitmap hand, vball;
    SensorManager sensorManager;
    GameView gameView;
    float shift, accel, recenter, down;
    MediaPlayer music;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if(sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() != 0){
            Sensor sensor = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        music = MediaPlayer.create(getApplicationContext(), R.raw.backgroundmusic);
        music.start();

        hand = BitmapFactory.decodeResource(getResources(), R.drawable.hand);
        vball = BitmapFactory.decodeResource(getResources(), R.drawable.vb);

        shift = 0;
        accel = 0;


        gameView = new GameView(this);
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        accel = sensorEvent.values[0];

        if(accel > -1 && accel < 1){
            accel = 0;
        }
        else if(accel >= 1 && accel < 5){
            accel = -3;
        }
        else if(accel <= -1 && accel > -5){
            accel = 3;
        }
        else if(accel >= 5){
            accel = -9;
        }
        else if(accel <= -5){
            accel = 9;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public class GameView extends SurfaceView implements Runnable{

        SurfaceHolder surfaceHolder;
        Thread myThread = null;
        volatile boolean running = true;
        float screenWidth, screenHeight;
        Paint text, collide;
        String timeLeft;
        int points = 0;
        Rect vbRect, handRect;


        public GameView(Context context) {
            super(context);

            surfaceHolder = getHolder();

            Display screenDisplay = getWindowManager().getDefaultDisplay();
            Point sizeOfScreen = new Point();
            screenDisplay.getSize(sizeOfScreen);

            screenWidth = sizeOfScreen.x;
            screenHeight = sizeOfScreen.y;

            text = new Paint();
            text.setColor(Color.BLACK);
            text.setTextSize(80);

            collide = new Paint();
            collide.setAlpha(0);

            recenter = (float)(Math.random()*(screenWidth-vball.getWidth()+1));
            down = -vball.getHeight();

            new CountDownTimer(30000, 1000) {

                public void onTick(long millisUntilFinished) {
                    timeLeft = "Time Left: " + millisUntilFinished / 1000 + "s";
                }

                public void onFinish() {
                    running = false;
                    music.stop();
                }
            }.start();
        }

        public void pause() {
            running = false;
            while(true) {
                try {
                    myThread.join();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                break;
            }
            myThread = null;
        }

        public void resume() {
            running = true;
            myThread = new Thread(this);
            myThread.start();
        }

        @Override
        public void run() {
            while(running){
                if(!surfaceHolder.getSurface().isValid()){
                    continue;
                }

                vbRect = new Rect((int) recenter, (int) down, (int) (recenter + vball.getWidth()), (int) (down + vball.getHeight()));
                handRect = new Rect((int)((screenWidth-hand.getWidth())/2 + shift), (int) (screenHeight - 2*hand.getHeight()), (int)((screenWidth + hand.getWidth())/2 + shift), (int)(screenHeight - hand.getHeight()));

                Canvas canvas = surfaceHolder.lockCanvas();
                canvas.drawColor(Color.MAGENTA);
                canvas.drawText(timeLeft, 40, 100, text);
                canvas.drawText("Score: "+points, 40, 200, text);

                if(shift < (-(screenWidth-hand.getWidth()))/2){
                    shift = (hand.getWidth() - screenWidth)/2;
                    canvas.drawBitmap(hand, 0, screenHeight-2*hand.getHeight(), null);

                }
                else if(shift > (screenWidth-hand.getWidth())/2){
                    shift = (screenWidth-hand.getWidth())/2;
                    canvas.drawBitmap(hand, (screenWidth - hand.getWidth()), screenHeight-2*hand.getHeight(), null);
                }
                else{
                    canvas.drawBitmap(hand, (screenWidth - hand.getWidth())/2 + shift, screenHeight-2*hand.getHeight(), null);
                }

                canvas.drawRect(vbRect, collide);
                canvas.drawRect(handRect, collide);

                if(vbRect.intersect(handRect)){
                    hand = BitmapFactory.decodeResource(getResources(), R.drawable.vbspike);
                }

                if(down > screenHeight){
                    recenter = (float)(Math.random()*(screenWidth-vball.getWidth()+1));
                    down = -vball.getHeight();
                    hand = BitmapFactory.decodeResource(getResources(), R.drawable.hand);
                    points++;
                }
                else{
                    down += 10;
                    canvas.drawBitmap(vball, recenter, down, null);
                }

                shift += accel;
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }
}