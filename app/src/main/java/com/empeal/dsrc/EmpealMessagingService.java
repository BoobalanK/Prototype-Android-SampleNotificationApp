package com.empeal.dsrc;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

public class EmpealMessagingService extends FirebaseMessagingService {

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.e("remoteMessage",remoteMessage.getData().toString());
        try {
            if(!isAppRunning())
            {
                sendNotification(remoteMessage);
            }
        } catch (Exception e) {
            Log.e("onMessageReceived error", e.getMessage() + "\n" + e.toString());
        }
    }
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.e("newToken", s);

        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
    }

    private Spannable getActionText(String title, @ColorRes int colorRes) {
        Spannable spannable = new SpannableString(title);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            spannable.setSpan(
                    new ForegroundColorSpan(this.getColor(colorRes)), 0, spannable.length(), 0);
        }
        return spannable;
    }

    /**
     * Create and show a custom notification containing the received FCM message.
     *
     * @param remoteMessage FCM notification payload received.
     */
    private void sendNotification(RemoteMessage remoteMessage) {
        int oneTimeID = (int) SystemClock.uptimeMillis();
        String channelId = "fcm_call_channel";
        String channelName = "Incoming Call";
        Uri uri= Uri.parse("empealmobileapp://");
        String IS_CALL_ACCEPTED = "IS_CALL_ACCEPTED";

        Uri notification_sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        String notification_data= new JSONObject(remoteMessage.getData()).toString();
        String notification_channelId= remoteMessage.getData().get("channel_id");
        String notification_title= remoteMessage.getData().get("title");
        String notification_body= remoteMessage.getData().get("body");
        String notification_hc_name= remoteMessage.getData().get("hc_name");
        String notification_hc_profile_pic= remoteMessage.getData().get("hc_profile_pic");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("NOTIFICATION_ID",oneTimeID);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);



        // notification action buttons start
        PendingIntent acptIntent = MainActivity.getActionIntent(oneTimeID,uri,"Accept",notification_data,this);
        PendingIntent rjctIntent = MainActivity.getActionIntent(oneTimeID,uri,"Decline",notification_data, this);

        NotificationCompat.Action rejectCall=new NotificationCompat.Action.Builder(R.drawable.rjt_btn,getActionText("Decline",android.R.color.holo_red_light),rjctIntent).build();
        NotificationCompat.Action acceptCall=new NotificationCompat.Action.Builder(R.drawable.acpt_btn,getActionText("Answer",android.R.color.holo_green_light),acptIntent).build();
        //end

        //when device locked show fullscreen notification start
        Intent i = new Intent(getApplicationContext(), LockScreenActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        i.putExtra("APP_STATE",isAppRunning());
        i.putExtra("FALL_BACK",true);
        i.putExtra("NOTIFICATION_ID",oneTimeID);
        i.putExtra("notification_data",notification_data);
        i.putExtra("notification_title",notification_title);
        i.putExtra("notification_body",notification_body);
        i.putExtra("notification_hc_name",notification_hc_name);
        i.putExtra("notification_hc_profile_pic",notification_hc_profile_pic);

        PendingIntent fullScreenIntent = PendingIntent.getActivity(this, 0 /* Request code */, i,
                PendingIntent.FLAG_UPDATE_CURRENT);
        //end

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(notification_title)
                .setContentText(notification_body)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setSound(notification_sound)
                .addAction(acceptCall)
                .addAction(rejectCall)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setFullScreenIntent(fullScreenIntent, true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();


        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //channel creation start
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    notification_channelId, notification_body, NotificationManager.IMPORTANCE_HIGH);
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            mChannel.setSound(notification_sound,attributes);
            mChannel.setDescription(notification_body);
            mChannel.enableLights(true);
            mChannel.enableVibration(true);
            notificationManager.createNotificationChannel(mChannel);
        }
        //end

        notificationManager.notify(oneTimeID, notification);

        Handler handlers = new Handler(Looper.getMainLooper());
        handlers.post(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(),"Local notification triggered with nid"+oneTimeID ,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isAppRunning() {
        ActivityManager m = (ActivityManager) this.getSystemService( ACTIVITY_SERVICE );
        List<ActivityManager.RunningTaskInfo> runningTaskInfoList =  m.getRunningTasks(10);
        Iterator<ActivityManager.RunningTaskInfo> itr = runningTaskInfoList.iterator();
        int n=0;
        while(itr.hasNext()){
            n++;
            itr.next();
        }
        if(n==1){ // App is killed
            return false;
        }
        return true; // App is in background or foreground
    }
}
