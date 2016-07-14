package com.yuan.house;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.yuan.house.application.DMApplication;

/**
 * Created by Alsor Zhou on 16/7/12.
 */

public class HeartbeatService extends Service {

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d("ClearFromRecentService", "Service Started");
            return START_NOT_STICKY;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.d("ClearFromRecentService", "Service Destroyed");
        }

        public void onTaskRemoved(Intent rootIntent) {
            Log.e("ClearFromRecentService", "END");

            DMApplication.getInstance().stopSelf();
        }

}
