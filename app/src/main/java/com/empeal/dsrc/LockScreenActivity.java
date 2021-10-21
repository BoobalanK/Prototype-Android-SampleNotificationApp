package com.empeal.dsrc;


import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.InputStream;
import java.net.URL;


public class LockScreenActivity extends AppCompatActivity {
    public static final String CALL_ACTION = "CALL_ACTION";
    private static final String TAG = "MessagingService";
    public static final String NOTIFICATION_DATA = "NOTIFICATION_DATA";
    private Ringtone ringtone;
    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("com.lockscreenactivity.action.close")){
                finish();
            }
        }
    };
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("com.lockscreenactivity.action.close");
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        setContentView(R.layout.incomingcall_activity);

        Intent intent = getIntent();
        final Integer notifID=intent.getIntExtra("NOTIFICATION_ID", -1);
        String notification_channelId=intent.getStringExtra("notification_channelId");
        String notification_data=intent.getStringExtra("notification_data");
        String notification_title=intent.getStringExtra("notification_title");
        String notification_body=intent.getStringExtra("notification_body");
        String notification_hc_name=intent.getStringExtra("notification_hc_name");
        String notification_hc_profile_pic=intent.getStringExtra("notification_hc_profile_pic");

        //ringtoneManager start
        Uri incoming_call_notif = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        this.ringtone= RingtoneManager.getRingtone(getApplicationContext(), incoming_call_notif);
        //ringtoneManager end

        final Boolean fallBack = intent.getBooleanExtra("FALL_BACK",true);
            if(!fallBack) {
                ringtone.setLooping(true);
                ringtone.play();
            }
        //final String host_name = "Alex";
        final Boolean isAppRuning=intent.getBooleanExtra("APP_STATE",false);

        TextView tvName = (TextView)findViewById(R.id.callerName);
        tvName.setText(notification_hc_name);

        ImageView imgView = (ImageView) findViewById(R.id.icon_profile_pic);
        new DownloadImageFromInternet(imgView).execute(notification_hc_profile_pic);
        //imgView.setImageDrawable(getDrawable(R.drawable.rjt_btn));

        ImageButton acceptCallBtn = (ImageButton) findViewById(R.id.accept_call_btn);
        acceptCallBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                removeNotification(fallBack,notifID);
                
                    String deeplinkUri="empealmobileapp://";
                    Uri uri = Uri.parse(deeplinkUri);
                    Log.e("deeplinkUri", uri.toString());
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.putExtra(NOTIFICATION_DATA,notification_data);
                    finish();
                    startActivity(intent);
            }
        });

        ImageButton rejectCallBtn = (ImageButton) findViewById(R.id.reject_call_btn);
        rejectCallBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                removeNotification(fallBack,notifID);
                if(isAppRuning){
                    Intent intent = new Intent(LockScreenActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY );
                    //intent.putExtra(NOTIFICATION_DATA,notification_body);
                    finish();
                    startActivity(intent);
                }
                else{
                    finish();
                }
            }
        });

        Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            public void run() {
                finish();
            }
        }, 45000);

    }

    private class DownloadImageFromInternet extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;
        public DownloadImageFromInternet(ImageView imageView) {
            this.imageView=imageView;
            //Toast.makeText(getApplicationContext(), "Please wait, it may take a few minute...",Toast.LENGTH_SHORT).show();
        }
        protected Bitmap doInBackground(String... urls) {
            String imageURL=urls[0];
            Bitmap bimage=null;
            try {
                InputStream in=new URL(imageURL).openStream();
                bimage=BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error Message", e.getMessage());
                e.printStackTrace();
            }
            return bimage;
        }
        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        ringtone.stop();
    }

    private void removeNotification(Boolean fallBack,Integer notifID){
        if(fallBack) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.cancel(notifID);
        }
    }
}
