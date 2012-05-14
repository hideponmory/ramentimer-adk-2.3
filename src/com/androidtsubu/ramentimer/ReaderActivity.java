/**
 * JANコード読取クラス
 * @author Ikuo Tansho(@tan1234jp)
 * @version 1.0
 */

package com.androidtsubu.ramentimer;

import java.sql.SQLException;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/**
 * JANコード読み取り
 * 
 * @author Ikuo Tansho(@tan1234jp)
 * 
 */
public class ReaderActivity extends Activity {

	// QRコードスキャナーのパッケージ名
	private static final String QRCODE_PKG_NAME = "com.google.zxing.client.android";
	// ラーメンデータ
	private NoodleMaster _noodleMaster;
	// // ラーメンデータベース・マネージャ
	// private NoodleManager _noodleManager;
	// JANコード
	private String _janCode;
	// 呼び出し元インテント
	private int _requestCode;
	// QRコードスキャナー例外フラグ
	private boolean _qrException = false;
	/** ProgressIcon */
	private ImageView progressIcon = null;
	// エラーコード(エラー文字列リソース値)
	private int _errorResId = -1;
	/** 商品検索からやってきたフラグ */
	private boolean _fromRamenSearch = false;

	// ReaderActivityの状態
	private static final int EXECUTE_QR_CODE_SCANNER = 100; // QRコードスキャナの実行
	private static final int DOWNLOAD_QR_CODE_SCANNER = 200; // QRコードスキャナーのダウンロード
	private static final int RECEIVE_NOODLE_DATA = 300; // ラーメン情報受信
	private static final int GOTO_NEXT_INTENT = 400; // 次のインテントへ処理を移す

	// intentでやってくるJANコードのキー
	public static final String KEY_JANCODE = "jancode";
	public static final String KEY_ERROR_RESID = "KEY_ERROR_RESID";

	/**
	 * メッセージハンドラーに対する処理
	 */
	private final Handler _handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case EXECUTE_QR_CODE_SCANNER:
				// QRコードスキャナーを実行する
				executeQrCodeScanner();
				break;

			case DOWNLOAD_QR_CODE_SCANNER:
				// QRコードスキャナーをAndroid Marketからダウンロードする
				getQrCodeScanner();
				break;

			case RECEIVE_NOODLE_DATA:
				// ラーメン情報を受信する
				receiveNoodleData();
				break;

			case GOTO_NEXT_INTENT:
				// 次のアクティビティへ遷移する
				gotoNextActivity();
				break;

			default:
				// その他・・・
				// switch()内に処理が書かれていないステートが存在するため、
				// 例外を投げておく
				throw new ReaderStateException(msg.toString());
			}
		}
	};

	/**
	 * コンストラクタ
	 */
	public ReaderActivity() {
		// 内部変数の初期化
		_noodleMaster = null;
		// _noodleManager = null;
		_janCode = null;
	}

	/**
	 * ReaderActivityがインテント呼び出しされた時に実行する
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reader);
		progressIcon = (ImageView) findViewById(R.id.ReaderProgressIcon);
		// アニメーションの開始
		progressIcon.startAnimation(AnimationUtils.loadAnimation(this,
				R.anim.progress_icon));

		// どのボタンからこのインテントが呼び出されたかを取得する
		Intent intent = getIntent();
		_requestCode = intent.getIntExtra(RequestCode.KEY_REQUEST_CODE, -1);
		if (RequestCode.values()[_requestCode]
				.equals(RequestCode.RAMENSEARCH2READER)) {
			// 手入力の商品検索から飛んできた場合はIntentで飛んできたJANコードで商品検索を行う
			_janCode = intent.getStringExtra(KEY_JANCODE);
			_fromRamenSearch = true;
			_handler.sendEmptyMessage(RECEIVE_NOODLE_DATA);
			return;
		}

		// QRコードスキャナーを実行する
		_handler.sendEmptyMessage(EXECUTE_QR_CODE_SCANNER);
	}

	/**
	 * QRコードスキャナを実行する
	 */
	private void executeQrCodeScanner() {
		// QRコードスキャナーのインテントを設定する
		final Intent intent = new Intent(QRCODE_PKG_NAME + ".SCAN");
		// JANコードを読み取る
		intent.putExtra("SCAN_MODE", "ONE_D_MODE");

		// QRコードスキャナーを呼び出す
		try {
			startActivityForResult(intent, EXECUTE_QR_CODE_SCANNER);
		} catch (ActivityNotFoundException e) {
			// アクティビティが存在しない(=インテントの開始に失敗した)場合は、
			// Android Marketからダウンロードするか問い合わせる
			_handler.sendEmptyMessage(DOWNLOAD_QR_CODE_SCANNER);
			_qrException = true;
		}
	}

	/**
	 * アクティビティの実行結果処理
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {

		// アクティビティのリクエストコードで処理を分ける
		switch (requestCode) {
		case EXECUTE_QR_CODE_SCANNER:
			// QRコードスキャナー実行後
			if (RESULT_OK == resultCode) {
				// QRコードスキャナーからJANコードをスキャンできた場合は、
				// ラーメン情報を履歴またはGAEから取得してみる
				_janCode = intent.getStringExtra("SCAN_RESULT");
				_handler.sendEmptyMessage(RECEIVE_NOODLE_DATA);
			} else {
				if (false == _qrException) {
					// 「Back」キー等でJANコードをスキャンできなかった場合は、
					// JANコード、ラーメン情報をNULLに設定して、次のインテントへ遷移する
					_janCode = null;
					_noodleMaster = null;
					_handler.sendEmptyMessage(GOTO_NEXT_INTENT);
				} else {
					_qrException = false;
				}
			}
			break;

		case DOWNLOAD_QR_CODE_SCANNER:
			// Android Market実行後
			if (RESULT_OK == resultCode) {
				// ダウンロードが完了したら、QRコードスキャナーを実行する
				_handler.sendEmptyMessage(EXECUTE_QR_CODE_SCANNER);
			} else {
				// ダウンロード失敗(or しない)場合は、Dashboardに遷移する
				finish();
			}
			break;

		default:
			// Intentをダッシュボードまで戻す。 TimerActivityやCreateActivityから戻ってくる。
			if (RESULT_OK == resultCode) {
				setResult(RESULT_OK, intent);
			}
			// 呼び出したインテントが空の場合は、処理を終了する
			finish(); // by @leibun ここで終了
			break;
		}

	}

	/**
	 * QRコードスキャナーが受信したJANコードを検索キーとして 履歴・GAEから商品情報を取得する
	 */
	private void receiveNoodleData() {
		// 問い合わせ用AsyncTaskを起動する
		ReadAsyncTask readAsyncTask = new ReadAsyncTask();
		readAsyncTask.execute(_janCode);
	}

	/**
	 * Android MarketからQRコードスキャナを取得する (直接ダウンロードするのではなく、Android
	 * Marketのダウンロードページへ飛ぶ)
	 */
	private void getQrCodeScanner() {

		// リソースを取得する
		final Resources res = getResources();

		// QRコードをAndroid Marketからダウンロードしてよいか
		// ダイアログを表示して問い合わせる
		CustomAlertDialog dialog = new CustomAlertDialog(this,
				R.style.CustomDialog);
		dialog.setIcon(android.R.drawable.ic_dialog_alert);
		dialog.setTitle(res.getString(R.string.no_qrcode_title));
		dialog.setMessage(res.getString(R.string.no_qrcode_message));
		dialog.setButton(res.getString(R.string.dialog_yes),
				new DialogInterface.OnClickListener() {
					// 「はい」押下時は、Android Marketへ飛び、QRコードスキャナーの
					// ダウンロードページを表示する
					public void onClick(DialogInterface dialog, int which) {
						final Intent intent = new Intent(Intent.ACTION_VIEW,
								Uri.parse("market://search?q=pname:"
										+ QRCODE_PKG_NAME));
						try {
							startActivityForResult(intent,
									DOWNLOAD_QR_CODE_SCANNER);
						} catch (ActivityNotFoundException ex) {
							// 通常使用ではありえない
						}
					}
				});
		dialog.setButton2(res.getString(R.string.dialog_no),
				new DialogInterface.OnClickListener() {
					// 「いいえ」押下時は、ダッシュボードに戻る
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
		dialog.show();
	}

	// 商品情報(NoodleMaster)のキー
	private static final String KEY_NOODLE_MASTER = "NOODLE_MASTER";

	/**
	 * 次の画面へ遷移する
	 */
	private void gotoNextActivity() {
		Intent intent;

		// 次の画面へ遷移する
		// (1)ダッシュボード→商品読込みの場合
		// →タイマー画面へ遷移する
		// (2)ダッシュボード→商品登録の場合
		// →登録画面へ遷移する

		// 商品情報オブジェクトが空の場合は、ダッシュボードに戻る
		// GAE問い合わせ、SQliteエラーの場合はnullが返ってきます @hideponm
		// ※通常はありえない
		if (null == _noodleMaster) {
			intent = getIntent();
			// // エラーコードをリクエストコードとして返す
			// intent.putExtra(RequestCode.KEY_REQUEST_CODE, _errorResId);
			// エラーコードをエラーコードして返す
			intent.putExtra(KEY_ERROR_RESID, _errorResId);
			// 処理失敗なのでRESULT_CANCELEDをダッシュボードに返す
			setResult(RESULT_CANCELED, intent);
			finish();
			return;
		}

		// (1)ダッシュボード→商品読込みもしくは商品検索→商品読込の場合
		RequestCode requestCode = RequestCode.values()[_requestCode];
		if (requestCode.equals(RequestCode.DASHBORAD2READER)
				|| requestCode.equals(RequestCode.RAMENSEARCH2READER)) {
			// →タイマー画面へ遷移する
			intent = new Intent(this, TimerActivity.class);
			intent.putExtra(RequestCode.KEY_REQUEST_CODE,
					RequestCode.READER2TIMER.ordinal());
			// 手動検索かどうかを渡す
			intent.putExtra(TimerActivity.KEY_FROMRAMENSEARCH, _fromRamenSearch);
			_requestCode = RequestCode.READER2TIMER.ordinal();
		}
		// (2)ダッシュボード→商品登録の場合
		else {
			// →登録画面へ遷移する
			intent = new Intent(this, CreateActivity.class);
			intent.putExtra(RequestCode.KEY_REQUEST_CODE,
					RequestCode.READER2CREATE.ordinal());
			_requestCode = RequestCode.READER2CREATE.ordinal();
		}

		// NoodleMaster情報もインテントに情報を送る
		intent.putExtra(KEY_NOODLE_MASTER, _noodleMaster);
		// インテントを発行する
		startActivityForResult(intent, _requestCode);
		// お役ごめん @hideponm
		progressIcon.clearAnimation();
		// finish(); // 削除 by @leibun ここで終わられるとアクションバーのボタンの動作がダッシュボードに伝わらない
		// onActivityForResultでfinish()
	}

	/**
	 * アクティビティ停止時
	 */
	@Override
	protected void onStop() {
		super.onStop();
		// 念のため、ハンドラーに登録されたメッセージを削除しておく
		_handler.removeMessages(EXECUTE_QR_CODE_SCANNER);
		_handler.removeMessages(RECEIVE_NOODLE_DATA);
		_handler.removeMessages(GOTO_NEXT_INTENT);
	}

	/**
	 * GAE問い合わせ用のAsyncTask
	 * 
	 * @author hide
	 * 
	 */
	private class ReadAsyncTask extends
			AsyncTask<String, NoodleMaster, NoodleMaster> {
		private NoodleManager noodleManager;
		private int errorResId;

		/**
		 * コンストラクタ
		 */
		public ReadAsyncTask() {
			noodleManager = new NoodleManager(ReaderActivity.this);
		}

		@Override
		protected NoodleMaster doInBackground(String... params) {
			NoodleMaster noodleMaster = null;
			// JANコードを検索キーにして、履歴またはGAEから
			// 商品を検索する
			try {
				noodleMaster = noodleManager.getNoodleMaster(params[0]);
				// @hideponm
				if (noodleMaster == null) {
					// 該当商品がないのでJANコードだけ入れたNoodleMasterを作ってあげる
					noodleMaster = new NoodleMaster(params[0], "", "", 0);
				}
				return noodleMaster;
			} catch (SQLException e) {
				Log.d("ramentimerbug", ExceptionToStringConverter.convert(e));
				errorResId = R.string.sql_local_error;
				// Toast.makeText(ReaderActivity.this,
				// "ローカルデータ問い合わせでエラーが発生しました",
				// Toast.LENGTH_LONG).show();
				return null;
			} catch (GaeException e) {
				Log.d("ramentimerbug", ExceptionToStringConverter.convert(e));
				errorResId = R.string.sql_gae_error;
				// Toast.makeText(ReaderActivity.this,
				// "サーバー問い合わせでエラーが発生しました",
				// Toast.LENGTH_LONG).show();
				return null;
			} catch (Exception e) {
				Log.d("ramentimerbug", ExceptionToStringConverter.convert(e));
				errorResId = R.string.sql_unknown_error;
				// Toast.makeText(ReaderActivity.this,
				// "原因不明のエラーが発生しました",
				// Toast.LENGTH_LONG).show();
				return null;
			}

		}

		protected void onPostExecute(NoodleMaster noodleMaster) {
			// 引数のnoodleMasterがnullの時はDBアクセス時のエラー
			if (null == noodleMaster) {
				// エラー文字列リソースを次のインテントへ渡す
				_errorResId = errorResId;
			} else {
				_errorResId = -1;
			}
			_noodleMaster = noodleMaster;
			// 次のインテントへ遷移する
			_handler.sendEmptyMessage(GOTO_NEXT_INTENT);
		}

	}

}
