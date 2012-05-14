package com.androidtsubu.ramentimer;

import java.sql.SQLException;
import java.util.Date;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidtsubu.ramentimer.USBAccessoryManager.RETURN_CODES;

public class TimerActivity extends Activity {

	// 分表示部
	private TextView minTextView = null;
	// 秒表示部
	private TextView secTextView = null;
	// 開始ボタン
	private Button startButton = null;
	// 終了ボタン
	private Button endButton = null;
	// リセットボタン
	private Button resetButton = null;
	// 登録確認ボタン
	private Button yesButton = null;
	// 分+ボタン
	private Button minUpButton = null;
	// 分-ボタン
	private Button minDownButton = null;
	// 秒+ボタン
	private Button secUpButton = null;
	// 秒-ボタン
	private Button secDownButton = null;
	// タイマーイメージ
	private ImageView timerImage = null;
	// ラーメン画像
	private ImageView noodleImage = null;
	// Janコード
	private TextView janCode = null;
	// 商品名
	private TextView name = null;
	// 待ち時間
	private TextView timerLimit = null;
	// 切り替え
	private ViewStub timerInfoViewStub = null;

	private View timerInfoInView = null;
	//
	private LinearLayout timerInfoFrame = null;
	// 確認用ダイアログ
	private AlertDialog verificationDialog = null;

	// Intentに付与している呼び出し元を保持する
	private int requestCode = 0;
	// ラーメン情報
	private NoodleMaster noodleMaster = null;
	// 履歴
	private NoodleHistory noodleHistory = null;
	// 登録フラグ
	private boolean registrationFlg = false;

	// 商品情報(NoodleMaster)のキー
	public static final String KEY_NOODLE_MASTER = "NOODLE_MASTER";
	// 履歴のキー
	public static final String KEY_NOODLE_HISTORY = "NOODLE_HISTORY";
	public static final String KEY_FROMRAMENSEARCH = "FROM_RAMENSEARCH";
	// 秒の増減間隔
	private static final int SEC_INTERVALS = 10;
	// 分の上限値
	private static final int MIN_UPPEL_LIMIT = 59;
	// 分の下限値
	private static final int MIN_LOWER_LIMIT = 0;
	// タイマーの更新時間間隔(ms)
	private static final int TIMER_UPDATE_INTERVALS = 200;
	/** タイマーのデフォルトの設定時間 */
	private static final long DEFAULT_SET_TIME = 180;

	private RamenTimerService ramenTimerService;
	private final RamenTimerReceiver receiver = new RamenTimerReceiver();

	// 開始時刻を保持
	private long startTime = 0;
	// 待ち時間を保持
	private long waitTime = 0;
	/** 茹で時間（履歴で使用する）@hideponm */
	private int boilTime;
	/** タイマーリセット時に設定する時間 */
	private int resetTime = 180;
	/** カウントダウンフラグ */
	private boolean countdown = false;
	/** チャルメラモードの切り替え **/
	private int animationMode = 0;
	/** 音 */
	private MediaPlayer mediaPlayer = null;
	/** 振動 */
	private Vibrator vibrator = null;
	/** 振動パターン */
	private static long[] vibePattern = { 500, 500, 500, 500, 500, 500, 500,
			500 };
	private AlarmManager alarmManager = null;
	private PendingIntent alarmPendingIntent = null;
	/** アラームの音のリソース **/
	private int alarm_sound_resouse_id = R.raw.alarm;
	/** アラームの画像のリソース **/
	private int alarm_img_default_resouse_id = R.drawable.img_alarm_default;
	private int alarm_img_start_resouse_id = R.drawable.img_alarm_start;
	private int alarm_img_end_resouse_id = R.drawable.img_alarm_end;
	/** 手動検索から来たよ */
	private boolean fromRamenSerach = false;
	/** twitter認証がキャンセルされた */
	private boolean twitterauthorizationcancel = false;
	/** WakeLock */
	private WakeLock wakeLock = null;
	/** ADK通信用UsbAccessoryManager */
	private USBAccessoryManager mUsbAccessoryManager;
	private static final int USBACCESSORY_WHAT = 0;
	private static final int MIN_COMMAND = 1;
	private static final int SEC_COMMAND = 2;
	private static final int BUZZON_COMMAND = 3;
	private static final int BUZZOFF_COMMAND = 4;
	private long min = 0;
	private long sec = 0;
	/** ファームプロトコルバージョン */
	private int firmwareProtocol = 0;
	private boolean deviceAttached = false;
	private TextView textViewTitle;

	private static String TAG = "RAMENTIMER";
	private Runnable drawRunnable = new Runnable() {
		@Override
		public void run() {
			if (deviceAttached) {
				updateTimerTextView(min * 60 + sec);
				return;
			}
			drawHandler.postAtTime(drawRunnable, 1000);
		}
	};
	/** 描画用Handler */
	private Handler drawHandler = new Handler();

	/** Handler */
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case USBACCESSORY_WHAT:
				switch (((USBAccessoryManagerMessage) msg.obj).type) {
				case READ:
					// Toast.makeText(TimerActivity.this, "ADK Read",
					// Toast.LENGTH_SHORT).show();
					break;
				case CONNECTED:
					// Toast.makeText(TimerActivity.this, "ADK Connected",
					// Toast.LENGTH_SHORT).show();
					updateTimerTextView(min * 60 + sec);

					break;
				case READY:
					// setTitle("Basic Accessory Demo: Device connected.");
					// Toast.makeText(TimerActivity.this, "ADK Ready",
					// Toast.LENGTH_SHORT).show();
					textViewTitle.setText(getString(R.string.timer_timer)
							+ " ●");

					String version = ((USBAccessoryManagerMessage) msg.obj).accessory
							.getVersion();
					firmwareProtocol = getFirmwareProtocol(version);

					switch (firmwareProtocol) {
					case 1:
						deviceAttached = true;
						break;
					case 2:
						deviceAttached = true;
						mUsbAccessoryManager.write(AdkCommandMaker
								.makeConnectCommand());
						Log.d(TAG, "connect message sent.");
						break;
					default:
						// showErrorPage(ErrorMessageCode.ERROR_FIRMWARE_PROTOCOL);
						break;
					}
					break;
				case DISCONNECTED:
					disconnectAccessory();
					break;
				}

				break;
			case MIN_COMMAND:
				Log.d(TAG, "MIN_COMMAND:" + Long.toString(min));
				mUsbAccessoryManager.write(AdkCommandMaker
						.makeMinCommand((int) min));
				break;
			case SEC_COMMAND:
				Log.d(TAG, "SEC_COMMAND:" + Long.toString(sec));
				mUsbAccessoryManager.write(AdkCommandMaker
						.makeSecCommand((int) sec));
				break;
			case BUZZON_COMMAND:
				Log.d(TAG, "BUZZ_COMMAND:on");
				mUsbAccessoryManager.write(AdkCommandMaker
						.makeBuzzerCommand(true));
				break;
			case BUZZOFF_COMMAND:
				Log.d(TAG, "BUZZ_COMMAND:off");
				mUsbAccessoryManager.write(AdkCommandMaker
						.makeBuzzerCommand(false));
				break;
			default:
				break;
			}
		}

	};

	private void disconnectAccessory() {
		if (deviceAttached == false) {
			return;
		}

		Log.d(TAG, "disconnectAccessory()");

		// this.setTitle("Basic Accessory Demo: Device not connected.");
		textViewTitle.setText(getString(R.string.timer_timer) + " ○");
	}

	private Context getThis() {
		return this;
	}

	private class RamenTimerReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			long currentTime = System.currentTimeMillis();

			// 更新が重なって時間がリセットされないことがあるのでそれを回避
			if (ramenTimerService == null)
				return;

			// 待ち時間を超えてない場合は、表示を更新し処理を終了する。
			if (waitTime > currentTime) {
				updateTimerTextView((waitTime - currentTime) / 1000 + 1);
				return;
			}
			// アンバインドする
			try {
				unbindService(serviceConnection);
			} catch (IllegalArgumentException ex) {
				// Service Not Registered対策。ログだけ残す @hideponm
				Log.e(TimerActivity.class.getName(), ex.getMessage(), ex);
				// debug
				// Toast.makeText(TimerActivity.this, ex.getMessage(),
				// Toast.LENGTH_LONG).show();
			}
			// サービスを停止するs
			ramenTimerService.stop();

			// 0秒TextView、終了ボタンを表示
			updateTimerTextView(0);
			setTimerEndLayout();

			if (isUseVibrate()) {
				// バイブが必要な設定なら振動させる
				new Thread(new Runnable() {
					public void run() {
						vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
						vibrator.vibrate(vibePattern, -1);
					}
				}).start();
			}
			// Mainスレッドでアラーム再生すると遅延が発生するため、スレッドで実行する
			new Thread(new Runnable() {
				public void run() {
					mediaPlayer = MediaPlayer.create(TimerActivity.this,
							alarm_sound_resouse_id);
					// チャルメラ試したい人はこっち
					// mediaPlayer = MediaPlayer.create(TimerActivity.this,
					// R.raw.charumera);
					try {
						mediaPlayer.start();
						sendMessageFromHandler(BUZZON_COMMAND);
					} catch (Exception e) {
						// 例外は発生しない
					}
				}
			}).start();

			// GAEに情報が存在した場合、履歴を登録する
			if (noodleMaster != null && noodleMaster.isCompleteData()) {
				new Thread(new Runnable() {
					public void run() {
						NoodleManager noodleManager = new NoodleManager(
								TimerActivity.this);
						try {
							// 履歴を作成する（茹で時間も入れる）@hideponm
							noodleManager.createNoodleHistory(noodleMaster,
									boilTime, new Date());
						} catch (SQLException e) {
							Toast.makeText(getThis(),
									ExceptionToStringConverter.convert(e),
									Toast.LENGTH_LONG).show();
						}
					}
				}).start();
			}
			setCountdown(false);
		}
	}

	/**
	 * バイブを使用するかどうか返す
	 * 
	 * @return
	 */
	private boolean isUseVibrate() {
		AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		int setting = audioManager
				.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
		if (setting == AudioManager.VIBRATE_SETTING_ON) {
			// バイブ設定ONなら振動させる
			return true;
		}
		if (setting == AudioManager.VIBRATE_SETTING_OFF) {
			// バイブ設定OFFなら振動しない
			return false;
		}
		if (setting == AudioManager.VIBRATE_SETTING_ONLY_SILENT) {
			// サイレント時のみバイブ設定ならば音声モードがサイレントかバイブの時に振動させる
			int ringerMode = audioManager.getRingerMode();
			if (ringerMode == AudioManager.RINGER_MODE_SILENT
					|| ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
				return true;
			}
		}
		return false;
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			ramenTimerService = ((RamenTimerService.RamenTimerBinder) service)
					.getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			ramenTimerService = null;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 端末スリープのロック設定を取得する
		wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
				.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
						"PARTIAL_WAKE_LOCK");
		// ハードウェアキーによる音量設定を有効にする
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		setContentView(R.layout.activity_timer);

		// レイアウトを取得
		minTextView = (TextView) findViewById(R.id.MinTextView);
		secTextView = (TextView) findViewById(R.id.SecTextView);

		minUpButton = (Button) findViewById(R.id.MinUpButton);
		minDownButton = (Button) findViewById(R.id.MinDownButton);
		secUpButton = (Button) findViewById(R.id.SecUpButton);
		secDownButton = (Button) findViewById(R.id.SecDownButton);

		timerInfoViewStub = (ViewStub) findViewById(R.id.NoodleInfoViewStub);

		timerInfoFrame = (LinearLayout) findViewById(R.id.TimerInfoFrame);

		timerImage = (ImageView) findViewById(R.id.TimerImageView);
		textViewTitle = (TextView) findViewById(R.id.textViewTitle);
		textViewTitle.setText(getString(R.string.timer_timer) + " ○");
		// 呼び出し元を保持する
		Intent requestIntent = getIntent();
		requestCode = requestIntent.getIntExtra(RequestCode.KEY_REQUEST_CODE,
				-1);
		// 呼び出し元のラーメン情報を取得する
		noodleMaster = (NoodleMaster) requestIntent
				.getParcelableExtra(KEY_NOODLE_MASTER);
		// 呼び出し元のラーメン履歴を取得する
		noodleHistory = (NoodleHistory) requestIntent
				.getParcelableExtra(KEY_NOODLE_HISTORY);
		// 商品検索から来たかどうかを取得する
		fromRamenSerach = requestIntent.getBooleanExtra(KEY_FROMRAMENSEARCH,
				false);
		// 呼び出し元に応じて表示を切り替える
		displaySetting(requestCode);

		// 分+ボタン
		minUpButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				addTimerCount(60);
			}
		});
		// 分-ボタン
		minDownButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				addTimerCount(-60);
			}
		});
		// 秒+ボタン
		secUpButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				addTimerCount(SEC_INTERVALS);
			}
		});
		// 秒-ボタン
		secDownButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				addTimerCount(SEC_INTERVALS * -1);
			}
		});
		// 開始ボタン
		startButton = (Button) findViewById(R.id.TimerStartButton);
		startButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startTimer();
				// 未登録の商品であれば、登録確認レイアウトを表示する
				if (registrationFlg) {
					showConfirmCreation();
				}

			}
		});
		// 終了ボタン
		endButton = (Button) findViewById(R.id.TimerEndButton);
		endButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// 音がなってたら消す
				if (mediaPlayer != null && mediaPlayer.isPlaying()) {
					mediaPlayer.stop();
				}
				sendMessageFromHandler(BUZZOFF_COMMAND);
				// 振動してたら消す
				if (vibrator != null) {
					vibrator.cancel();
				}
				setResult(RESULT_OK);
				finish();
			}
		});
		// リセットボタン
		resetButton = (Button) findViewById(R.id.TimerResetButton);
		resetButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// ダイアログの表示
				showVerificationDialog();
			}
		});
		startRamenTimerService();
		// Toast.makeText(this, "USBAccessoryManager instance created",
		// Toast.LENGTH_SHORT).show();
		mUsbAccessoryManager = new USBAccessoryManager(mHandler,
				USBACCESSORY_WHAT);

	}

	/**
	 * リセット確認用ダイアログを表示する
	 */
	private void showVerificationDialog() {
		verificationDialog = getVerificationDialog(TimerActivity.this);
		if (null != verificationDialog)
			verificationDialog.show();
	}

	/**
	 * リセットボタンが押されたときの確認ダイアログを
	 * 
	 * @param context
	 * @return
	 */
	private AlertDialog getVerificationDialog(Context context) {

		Resources resources = getResources();
		final String DIALOG_TITLE = resources
				.getString(R.string.dialog_timer_verification_title);
		final String DIALOG_BUTTON_OK = resources
				.getString(R.string.dialog_timer_verification_ok);
		final String DIALOG_BUTTON_CANCEL = resources
				.getString(R.string.dialog_timer_verification_cancel);

		CustomAlertDialog dialog = new CustomAlertDialog(this,
				R.style.CustomDialog);
		dialog.setTitle(DIALOG_TITLE);
		dialog.setButton(DIALOG_BUTTON_OK, onResetOkClick);
		dialog.setButton2(DIALOG_BUTTON_CANCEL, onResetCancelClick);

		return dialog;
	}

	/**
	 * twitter認証確認ダイアログを作成して返す
	 * 
	 * @param context
	 * @return
	 */
	private CustomAlertDialog getAuthorizationTwitterDialog(Context context) {
		Resources resources = getResources();
		final String DIALOG_TITLE = resources
				.getString(R.string.dialog_authorization_twitter_title);
		final String DIALOG_MESSAGE = resources
				.getString(R.string.dialog_authorization_twitter_message);
		final String DIALOG_BUTTON_OK = resources
				.getString(R.string.dialog_authorization_twitter_authorization);
		final String DIALOG_BUTTON_CANCEL = resources
				.getString(R.string.dialog_authorization_twitter_cancel);

		CustomAlertDialog dialog = new CustomAlertDialog(context,
				R.style.CustomDialog);
		dialog.setTitle(DIALOG_TITLE);
		// dialog.setMessage(DIALOG_MESSAGE);
		dialog.setButton(DIALOG_BUTTON_OK, new Dialog.OnClickListener() {
			// twitterに認証するがクリックされた
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent();
				intent.setClass(TimerActivity.this, AuthorizationActivity.class);
				startActivityForResult(intent,
						RequestCode.TIMER2AUTHORIZATION.ordinal());
			}
		});
		dialog.setButton2(DIALOG_BUTTON_CANCEL, new Dialog.OnClickListener() {
			// twitterに認証するがキャンセルされた
			@Override
			public void onClick(DialogInterface dialog, int which) {
				twitterauthorizationcancel = true;
				// もう一度画面設定する
				displaySetting(RequestCode.READER2TIMER.ordinal());
			}
		});

		return dialog;

	}

	/* 確認ダイアログの「はい」が押されたとき */
	Dialog.OnClickListener onResetOkClick = new Dialog.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			resetTimer();
			dialog.dismiss();
		}
	};
	/* 確認ダイアログの「いいえ」が押されたとき */
	Dialog.OnClickListener onResetCancelClick = new Dialog.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			dialog.cancel();
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		// unbindService(serviceConnection); // バインド解除
		unregisterReceiver(receiver); // 登録解除
		ramenTimerService.stopSelf(); // サービスは必要ないので終了させる。
		ramenTimerService = null;

	}

	/**
	 * ラーメン情報をレイアウトにセット、表示する
	 */
	private void setNoodleData() {
		if (noodleMaster == null) {
			// デフォルト3分をいれておく
			updateTimerTextView(DEFAULT_SET_TIME);
			return;
		}

		// 設定されている項目を表示する
		if (noodleImage != null && noodleMaster.getImage() != null) {
			noodleImage.setImageBitmap(noodleMaster.getImage());
			noodleImage.setVisibility(View.VISIBLE);
		}
		if (janCode != null && !noodleMaster.getJanCode().equals("")) {
			janCode.setText(noodleMaster.getJanCode());
			LinearLayout janCodeTableRow = (LinearLayout) findViewById(R.id.JanCodeTableRow);
			janCodeTableRow.setVisibility(View.VISIBLE);
		}
		if (name != null && !noodleMaster.getName().equals("")) {
			name.setText(noodleMaster.getName());
			name.setVisibility(View.VISIBLE);
		}
		if (noodleHistory != null && noodleHistory.getBoilTime() != 0) {
			timerLimit.setText(""
					+ noodleHistory.getNoodleMaster().getTimerLimitString(
							getString(R.string.min_unit),
							getString(R.string.sec_unit)));
			// タイマーの時間をセットする
			// 履歴がある場合は履歴のゆで時間を利用する @hideponm
			updateTimerTextView(Long.valueOf(noodleHistory.getBoilTime()));

		} else if (noodleMaster != null && noodleMaster.getTimerLimit() != 0) {
			timerLimit.setText(""
					+ noodleMaster.getTimerLimitString(
							getString(R.string.min_unit),
							getString(R.string.sec_unit)));
			// タイマーの時間をセットする
			updateTimerTextView(Long.valueOf(noodleMaster.getTimerLimit()));
		}
	}

	/**
	 * リクエストコードで表示を切り替える
	 * 
	 * @param id
	 */
	private void displaySetting(int id) {
		if (id == -1) {
			return;
		}
		switch (RequestCode.values()[id]) {
		case DASHBORAD2TIMER:
			if (!countdown) {
				timerInfoViewStub
						.setLayoutResource(R.layout.activity_timer_only_jancode);
				timerInfoInView = timerInfoViewStub.inflate();
			}
			timerInfoFrame.setVisibility(View.INVISIBLE);
			break;
		case CREATE2TIMER:
		case HISTORY2TIMER:
		case FAVORITE2TIMER:
		case READER2TIMER:
			if (!countdown) {
				if (noodleMaster.isCompleteData()) {
					timerInfoViewStub
							.setLayoutResource(R.layout.activity_timer_full_information);
					timerInfoInView = timerInfoViewStub.inflate();

					noodleImage = (ImageView) timerInfoInView
							.findViewById(R.id.NoodleImageView);
					janCode = (TextView) timerInfoInView
							.findViewById(R.id.JanCodeTextView);
					name = (TextView) timerInfoInView
							.findViewById(R.id.NameTextView);
					timerLimit = (TextView) timerInfoInView
							.findViewById(R.id.TimerLimitTextView);
				} else {
					if (fromRamenSerach || twitterauthorizationcancel) {
						timerInfoViewStub
								.setLayoutResource(R.layout.activity_timer_only_jancode_fromserach);
						timerInfoInView = timerInfoViewStub.inflate();
						janCode = (TextView) timerInfoInView
								.findViewById(R.id.JanCodeTextView);

					} else {
						if (!TwitterManager.getInstance().isAuthorization(this)) {
							// まだtwitter認証されていないのでtwitter認証するように促す
							// getAuthorizationTwitterDialog(this);
							getAuthorizationTwitterDialog(this).show();
							return;
						}
						// 登録フラグをたてる
						registrationFlg = true;
						// ActionBarのバーコードを登録と置き換える
						timerInfoViewStub
								.setLayoutResource(R.layout.activity_timer_only_jancode);
						timerInfoInView = timerInfoViewStub.inflate();

						janCode = (TextView) timerInfoInView
								.findViewById(R.id.JanCodeTextView);

						// はいボタン
						yesButton = (Button) timerInfoInView
								.findViewById(R.id.ConfirmCreationYesButton);
						yesButton
								.setOnClickListener(new View.OnClickListener() {
									public void onClick(View v) {
										onCreateButtonClick(v);
									}
								});
					}
				}
			}
			break;
		}
		setNoodleData();
	}

	/**
	 * 登録確認メッセージの表示
	 */
	private void showConfirmCreation() {
		LinearLayout confirmCreation = (LinearLayout) findViewById(R.id.ConfirmCreationLinearLayout);
		confirmCreation.setVisibility(View.VISIBLE);
	}

	/**
	 * タイマーに時間を足す
	 * 
	 * @param sec
	 */
	private void addTimerCount(int sec) {
		int setTime = (Integer.valueOf(minTextView.getText().toString()) * 60)
				+ Integer.valueOf(secTextView.getText().toString());
		setTime = setTime + sec;
		// 上限値または下限値を超える場合は処理しない
		if (setTime > MIN_UPPEL_LIMIT * 60 || setTime < MIN_LOWER_LIMIT) {
			return;
		}
		updateTimerTextView(setTime);
	}

	/**
	 * タイマーの残り時間を更新する
	 * 
	 * @param time
	 */
	private void updateTimerTextView(long sec) {
		int latestmin = (int) (sec / 60);
		int latestsec = (int) (sec % 60);
		if (latestmin != this.min || latestsec != this.sec) {
			Log.d("ADK from TimerActivity", "mm:sec changed");
			minTextView.setText(String.valueOf(latestmin));
			secTextView.setText(getSecText(latestsec));
			if (latestmin != this.min) {
				this.min = latestmin;
				sendMessageFromHandler(MIN_COMMAND);
			}
			if (latestsec != this.sec) {
				this.sec = latestsec;
				sendMessageFromHandler(SEC_COMMAND);
			}
		}
	}

	/**
	 * 引数が有効値でなければ有効値を戻す。 1桁の場合は前0を付加する。
	 */
	private String getSecText(long sec) {
		if (sec >= 60) {
			sec = sec - 60;
		} else if (sec < 0) {
			sec = sec + 60;
		}
		String secText = String.valueOf(sec);
		if (sec < 10) { // 一桁の場合は前0を表示
			secText = "0" + sec;
		}
		return secText;
	}

	/**
	 * 終了時間をセットし、サービスのタイマーを起動する
	 */
	private void startTimer() {
		setCountdown(true);
		// int min = Integer.valueOf(minTextView.getText().toString());
		// int sec = Integer.valueOf(secTextView.getText().toString());

		alarmPendingIntent = PendingIntent.getActivity(this, 0, new Intent(
				this, TimerActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
		// リセットしたときに戻す時間を保持
		resetTime = (int) min * 60 + (int) sec;
		boilTime = resetTime;
		Date date = new Date();
		date.setSeconds(date.getSeconds() + boilTime);
		alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, date.getTime(),
				alarmPendingIntent);

		// 終了時刻を設定する
		startTime = System.currentTimeMillis();
		waitTime = startTime + (boilTime * 1000);
		ramenTimerService.schedule(TIMER_UPDATE_INTERVALS, waitTime);
		// カウント中のレイアウトを表示する
		setTimerRunningLayout();
		// 時間調整ボタンを非表示にする
		hidePickerButton(true);
	}

	/**
	 * 非カウント中のレイアウトを設定する
	 */
	private void setTimerNotRunningLayout() {
		startButton.setVisibility(View.VISIBLE);
		resetButton.setVisibility(View.GONE);
		endButton.setVisibility(View.GONE);
		timerImage.setImageResource(alarm_img_default_resouse_id);
		// ボタンを有効化（押せるようになる）
		setOnClickEnable(true);
	}

	/**
	 * カウント中のレイアウトを設定する
	 */
	private void setTimerRunningLayout() {
		startButton.setVisibility(View.GONE);
		resetButton.setVisibility(View.VISIBLE);
		// タイマー画像を差し替える(黄色)
		timerImage.setImageResource(alarm_img_start_resouse_id);
		endButton.setVisibility(View.GONE);
		// ボタンを無効化（押せないようになる）
		setOnClickEnable(false);

	}

	/**
	 * タイマー終了後のレイアウトを設定する
	 */
	private void setTimerEndLayout() {
		endButton.setVisibility(View.VISIBLE);
		resetButton.setVisibility(View.GONE);
		// タイマー画像を差し替える(赤)
		timerImage.setImageResource(alarm_img_end_resouse_id);
		Animation timerAnim = AnimationUtils.loadAnimation(this,
				R.anim.timer_icon_action_start);
		timerAnim.setAnimationListener(mAnimLeft2Right);
		timerImage.startAnimation(timerAnim);

		startButton.setVisibility(View.GONE);
		// ボタンを有効化（押せるようになる）
		setOnClickEnable(true);

	}

	/**
	 * ボタンを無効化/有効化　するメソッド
	 * 
	 * @param enabled
	 */
	private void setOnClickEnable(boolean enabled) {
		ImageButton homeButton = (ImageButton) findViewById(R.id.TitleHomeButton);
		ImageButton readerButton = (ImageButton) findViewById(R.id.TitleReaderButton);
		ImageButton searchButton = (ImageButton) findViewById(R.id.TitleSearchButton);
		homeButton.setEnabled(enabled);
		readerButton.setEnabled(enabled);
		searchButton.setEnabled(enabled);
		// チャルメラモードと通常モードの切り替えボタンの無効化
		timerImage.setClickable(enabled);
		// 登録確認のボタンがあるとき
		if (yesButton != null) {
			yesButton.setEnabled(enabled);
			yesButton.setFocusable(enabled);
		}
	}

	AnimationListener mAnimLeft2Right = new AnimationListener() {
		public void onAnimationStart(Animation animation) {
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationEnd(Animation animation) {
			Animation timerAnim = AnimationUtils.loadAnimation(
					TimerActivity.this, R.anim.timer_icon_action);
			timerAnim.setAnimationListener(mAnimRight2Center);
			timerImage.startAnimation(timerAnim);
		}
	};

	AnimationListener mAnimRight2Center = new AnimationListener() {
		public void onAnimationStart(Animation animation) {
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationEnd(Animation animation) {
			Animation timerAnim = AnimationUtils.loadAnimation(
					TimerActivity.this, R.anim.timer_icon_action_end);
			timerImage.startAnimation(timerAnim);
		}
	};

	/**
	 * 時間調整ボタンを非表示にする
	 * 
	 * @param hide
	 *            true：ボタンを隠す false：表示する
	 */
	private void hidePickerButton(boolean hide) {
		if (hide) {
			minUpButton.setVisibility(View.INVISIBLE);
			minDownButton.setVisibility(View.INVISIBLE);
			secUpButton.setVisibility(View.INVISIBLE);
			secDownButton.setVisibility(View.INVISIBLE);
		} else {
			minUpButton.setVisibility(View.VISIBLE);
			minDownButton.setVisibility(View.VISIBLE);
			secUpButton.setVisibility(View.VISIBLE);
			secDownButton.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * 登録機能を起動し、Timerを終了する
	 * 
	 */
	public void onCreateButtonClick(View v) {
		Intent intent = new Intent(TimerActivity.this, CreateActivity.class);
		intent.putExtra(RequestCode.KEY_REQUEST_CODE,
				RequestCode.READER2CREATE.ordinal());
		intent.putExtra(KEY_NOODLE_MASTER, noodleMaster);
		startActivity(intent);
		finish();
	}

	public void onLogoClick(View v) {
		setResult(RESULT_OK);
		finish();
	}

	/**
	 * バーコードリーダーを起動し、Timerを終了する
	 */
	public void onReaderButtonClick(View v) {
		Intent intent = new Intent();
		intent.putExtra(RequestCode.KEY_REQUEST_CODE,
				RequestCode.ACTION_READER.ordinal());
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * 検索を起動し、Timerを終了する
	 */
	public void onSearchButtonClick(View v) {
		Intent intent = new Intent();
		intent.putExtra(RequestCode.KEY_REQUEST_CODE,
				RequestCode.ACTION_SEARCH.ordinal());
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == RequestCode.TIMER2AUTHORIZATION.ordinal()) {
			if (resultCode == RESULT_CANCELED) {
				twitterauthorizationcancel = true;
			}
			displaySetting(RequestCode.READER2TIMER.ordinal());
		}
	}

	/**
	 * チャルメラモードと通常モードの切り替え
	 */
	public void onModeChangeClick(View v) {
		// animationMode = 1 - animationMode;
		// if (animationMode == 1) {
		// alarm_sound_resouse_id = R.raw.charumera;
		// alarm_img_default_resouse_id = R.drawable.img_charumera_default;
		// alarm_img_start_resouse_id = R.drawable.img_charumera_start;
		// alarm_img_end_resouse_id = R.drawable.img_charumera_end;
		// // Toast.makeText(this, "チャルメラモード", Toast.LENGTH_SHORT).show();
		// } else {
		// alarm_sound_resouse_id = R.raw.alarm;
		// alarm_img_default_resouse_id = R.drawable.img_alarm_default;
		// alarm_img_start_resouse_id = R.drawable.img_alarm_start;
		// alarm_img_end_resouse_id = R.drawable.img_alarm_end;
		// // Toast.makeText(this, "ノーマルモード", Toast.LENGTH_SHORT).show();
		// }
		// timerImage.setImageResource(alarm_img_default_resouse_id);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (!countdown) {
			return super.dispatchKeyEvent(event);
		}

		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			// カウントダウン中は戻るキーでリセットダイアログを出す
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK:
				showVerificationDialog();
				// return true;
			}
		}
		return super.dispatchKeyEvent(event);
	}

	/**
	 * カウントダウン中フラグを設定する
	 * 
	 * @param countdown
	 */
	private void setCountdown(boolean countdown) {
		this.countdown = countdown;
		if (countdown) {
			// スリープを無効化する
			getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			wakeLock.acquire();
		} else {
			// スリープを有効化する
			getWindow()
					.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			if (wakeLock.isHeld()) {
				wakeLock.release();
			}
		}
	}

	/**
	 * Timerをキャンセルする
	 */
	private void resetTimer() {
		if (ramenTimerService == null) {
			return;
		}
		if (!countdown) {
			return;
		}
		if (alarmManager != null) {
			alarmManager.cancel(alarmPendingIntent);
		}
		ramenTimerService.stop();
		// サービスをアンバインドする
		try {
			unbindService(serviceConnection);
		} catch (Exception ex) {

		}
		ramenTimerService = null;
		// // カウント前の画面に戻す
		// displaySetting(requestCode);
		updateTimerTextView(resetTime);
		// ボタンを押せるようにする
		setOnClickEnable(true);
		startButton.setVisibility(View.VISIBLE);
		resetButton.setVisibility(View.GONE);
		// 次のカウントダウンに備える
		startRamenTimerService();
		// カウント中フラグを落とす
		countdown = false;
		// 時間調節用のPickerボタンを表示する
		hidePickerButton(false);
	}

	/**
	 * RamenTimerServiceを開始する
	 */
	private void startRamenTimerService() {
		// 開始前の画面表示
		setTimerNotRunningLayout();

		// サービスを開始
		Intent intent = new Intent(this, RamenTimerService.class);
		startService(intent);
		IntentFilter filter = new IntentFilter(RamenTimerService.ACTION);
		registerReceiver(receiver, filter);

		// サービスにバインド
		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
		// いったんアンバインドしてから再度バインド
		unbindService(serviceConnection);
		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPause() {
		drawHandler.removeCallbacks(drawRunnable);
		// TODO Auto-generated method stub
		// Toast.makeText(this, "ADK disconnect command",
		// Toast.LENGTH_SHORT).show();
		switch (firmwareProtocol) {
		case 2:
			mUsbAccessoryManager.write(AdkCommandMaker.makeDisConnectCommand());
			break;
		}

		try {
			while (mUsbAccessoryManager.isClosed() == false) {
				Thread.sleep(2000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		super.onPause();
		// Toast.makeText(this, "ADK disable", Toast.LENGTH_SHORT).show();
		mUsbAccessoryManager.disable(this);
		disconnectAccessory();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		// Toast.makeText(this, "ADK enable", Toast.LENGTH_SHORT).show();
		RETURN_CODES ret = mUsbAccessoryManager.enable(this, getIntent());
		switch (ret) {
		case ACCESSORIES_LIST_IS_EMPTY:
			// Toast.makeText(this, "ACCESSORIES_LIST_IS_EMPTY",
			// Toast.LENGTH_SHORT).show();
			break;
		case DEVICE_MANAGER_IS_NULL:
			// Toast.makeText(this, "DEVICE_MANAGER_IS_NULL",
			// Toast.LENGTH_SHORT).show();
			break;
		case FILE_DESCRIPTOR_WOULD_NOT_OPEN:
			// Toast.makeText(this, "FILE_DESCRIPTOR_WOULD_NOT_OPEN",
			// Toast.LENGTH_SHORT).show();
			break;
		case PERMISSION_PENDING:
			// Toast.makeText(this, "PERMISSION_PENDING",
			// Toast.LENGTH_SHORT).show();
			break;
		case SUCCESS:
			// Toast.makeText(this, "SUCCESS", Toast.LENGTH_SHORT).show();
			break;

		}

		drawHandler.post(drawRunnable);
	}

	/**
	 * バージョン文字列からファームプロトコルを得る
	 * 
	 * @param version
	 * @return
	 */
	private int getFirmwareProtocol(String version) {
		String major = "0";
		int positionOfDot;
		positionOfDot = version.indexOf('.');
		if (positionOfDot != -1) {
			major = version.substring(0, positionOfDot);
		}
		return new Integer(major).intValue();
	}

	/**
	 * ADKにHandler経由でメッセージを送る
	 * 
	 * @param what
	 */
	private void sendMessageFromHandler(int what) {
		if (!deviceAttached) {
			// デバイスがつながってなかったら何もしない
			return;
		}
		Message message = new Message();
		message.what = what;
		mHandler.sendMessage(message);
	}
}
