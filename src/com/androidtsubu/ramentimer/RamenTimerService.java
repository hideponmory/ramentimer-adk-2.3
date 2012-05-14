package com.androidtsubu.ramentimer;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class RamenTimerService extends Service {
	private NotificationManager notificationManager = null;
	/** Notification */
	private Notification notification = null;
	private PendingIntent contentIntent = null;
	/** 待ち時間 */
	private long waitTime = 0;

	class RamenTimerBinder extends Binder {

		RamenTimerService getService() {
			return RamenTimerService.this;
		}

	}

	public static final String ACTION = "Ramen Timer Service";
	private Timer timer;

	@Override
	public void onCreate() {
		super.onCreate();
		// Toast toast = Toast.makeText(getApplicationContext(), "onCreate()",
		// Toast.LENGTH_SHORT);
		// toast.show();
		System.out.println("####### service onCreate() process:"
				+ android.os.Process.myPid() + " task:"
				+ android.os.Process.myTid());
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Toast toast = Toast.makeText(getApplicationContext(), "onDestroy()",
		// Toast.LENGTH_SHORT);
		// toast.show();
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		// Toast toast = Toast.makeText(getApplicationContext(), "onStart()",
		// Toast.LENGTH_SHORT);
		// toast.show();
	}

	@Override
	public void onRebind(Intent intent) {
		// Toast toast = Toast.makeText(getApplicationContext(), "onRebind()",
		// Toast.LENGTH_SHORT);
		// toast.show();
	}

	@Override
	public IBinder onBind(Intent intent) {
		// Toast toast = Toast.makeText(getApplicationContext(), "onBind()",
		// Toast.LENGTH_SHORT);
		// toast.show();
		return new RamenTimerBinder();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// Toast toast = Toast.makeText(getApplicationContext(), "onUnbind()",
		// Toast.LENGTH_SHORT);
		// toast.show();
		return true; // 再度クライアントから接続された際に onRebind を呼び出させる場合は true を返す
	}

	/**
	 * クライアントから呼び出されるメソッド
	 * 
	 * @param delay
	 * @param showText
	 */
	public void schedule(long delay, long waitTime) {
		this.waitTime = waitTime;
		showNotification(getUpdateTimerString());
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		TimerTask timerTask = new TimerTask() {

			public void run() {
				Log.d("ramentimer", "timer thread comming");
				showNotification(getUpdateTimerString());
				sendBroadcast(new Intent(ACTION));
			}

		};
		timer.schedule(timerTask, delay, delay);
	}

	public void stop() {
		if (timer != null) {

			timer.cancel();
			timer.purge();
		}
		if (notificationManager == null) {
			return;
		}
		notificationManager.cancel(1);
		notificationManager = null;
		notification = null;
	}

	/**
	 * Notification表示を行う
	 * 
	 * @param showText
	 */
	private void showNotification(String showText) {
		if (notificationManager == null) {
			notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		}
		if (notification == null) {
			notification = new Notification(R.drawable.ic_stat_notify, "",
					System.currentTimeMillis());
		}
		if (contentIntent == null) {
			contentIntent = PendingIntent.getActivity(this, 0, new Intent(this,
					TimerActivity.class), 0);
		}
		notification.setLatestEventInfo(getApplicationContext(),
				getString(R.string.timer_notification_message), showText,
				contentIntent);
		notificationManager.notify(1, notification);

	}

	/**
	 * 更新時間を文字列で返す
	 * 
	 * @return
	 */
	private String getUpdateTimerString() {
		long currentTime = new Date().getTime();
		long time = (waitTime - currentTime) / 1000 + 1;
		if (time < 0) {
			return "0" + getString(R.string.min_unit) + "00"
					+ getString(R.string.sec_unit);
		}
		int min = (int) time / 60;
		int sec = (int) time % 60;
		DecimalFormat format = new DecimalFormat("00");
		StringBuilder buf = new StringBuilder();
		buf.append(min);
		buf.append(getString(R.string.min_unit));
		buf.append(format.format(sec));
		buf.append(getString(R.string.sec_unit));
		return buf.toString();
	}

}