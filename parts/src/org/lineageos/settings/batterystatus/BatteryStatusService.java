/*
 * Copyright (C) 2025 Julival Bittencourt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.batterystatus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

public class BatteryStatusService extends Service {

    private static final String CHANNEL_ID = "stoneparts_battery_status";
    private BroadcastReceiver batteryReceiver;
    private NotificationManager nm;

    @Override
    public void onCreate() {
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel();
        registerReceiver();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Battery Status", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Battery status notifications for StoneParts");
            nm.createNotificationChannel(channel);
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                    int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                    int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    int currentNow = intent.getIntExtra("current_now", 0);

                    float batteryPct = scale > 0 ? level * 100f / scale : 0f;
                    float tempC = temp / 10f;
                    float current_mA = currentNow;
                    float power = (voltage / 1000f) * (current_mA/1000f);

                    String content = String.format("Agora: %d%% · %.1f°C", (int)batteryPct, tempC);

                    Notification n = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.ic_menu_info_details)
                            .setContentTitle("Battery Status")
                            .setContentText(content)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(buildDetails(intent)))
                            .setOngoing(true)
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .build();

                    startForeground(1001, n);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        registerReceiver(batteryReceiver, filter);
    }

    private String buildDetails(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        int voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int currentNow = intent.getIntExtra("current_now", 0);
        float batteryPct = scale > 0 ? level * 100f / scale : 0f;
        float tempC = temp / 10f;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Agora: %d%% · %.1f°C\n", (int)batteryPct, tempC));
        sb.append(String.format("Voltagem: %dmV\n", voltage));
        sb.append(String.format("Corrente: %dmA\n", currentNow));
        sb.append(String.format("Potência: %.2fW\n", (voltage/1000f)* (currentNow/1000f)));
        return sb.toString();
    }

    @Override
    public void onDestroy() {
        try {
            if (batteryReceiver != null) unregisterReceiver(batteryReceiver);
        } catch (Exception e) { }
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
