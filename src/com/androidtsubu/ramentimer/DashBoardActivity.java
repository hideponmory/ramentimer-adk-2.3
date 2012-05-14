package com.androidtsubu.ramentimer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.androidtsubu.ramentimer.R.id;
import com.androidtsubu.ramentimer.bugreport.AppUncaughtExceptionHandler;

public class DashBoardActivity extends Activity {

	private static String TSUBU_WEB_ADDRESS;
	private static String RAMEN_LIST_WEB_ADDRESS;
	private NoodleManager manager = null;
	private TextView textCountView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dashboard);
		// ヘルプのURL取得
		TSUBU_WEB_ADDRESS = getResources().getString(R.string.help_url);
		// ラーメンタイマーWebのURL取得
		RAMEN_LIST_WEB_ADDRESS = getResources().getString(
				R.string.ramen_list_url);
		textCountView = (TextView) findViewById(id.ramen_count);
		// つ部ロゴを動かす
		Button button = (Button) findViewById(R.id.ButtonLogo);
		button.setAnimation(AnimationUtils.loadAnimation(this,
				R.anim.dashboard_tsubu_icon_action));

		// 登録件数の呼び出し
		manager = new NoodleManager(this);
		doCountTask();

		String packegeName = getPackageName();
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(
					packegeName, PackageManager.GET_META_DATA);
			TextView textView = (TextView) findViewById(id.ramen_version);
			textView.setText(packageInfo.versionName + " "
					+ getString(R.string.dashboard_versionname_hai));
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		// キャッチできない例外エラーが発生した場合に備え、例外ハンドラを設定する
		Resources res = getResources();
		Thread.setDefaultUncaughtExceptionHandler(new AppUncaughtExceptionHandler(
				this, res.getString(R.string.bugreport_title), res
						.getString(R.string.bugreport_message), res
						.getString(R.string.dialog_yes), res
						.getString(R.string.dialog_no)));
	}

	/**
	 * メニュー内容を生成
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_dashboard, menu);
		return true;
	}

	/**
	 * メニューのボタンを押した時の動作
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int id = item.getItemId();

		// XML中のメニューボタンにアクセスするにはR.id以下を利用する
		if (id == R.id.help) {
			gotoTsubuSite();
		}
		return true;
	}

	/**
	 * タイマーのボタンが押されたとき
	 * 
	 * @param view
	 */
	public void onTimerButtonClick(View view) {
		gotoTimerActivity();
	}

	/**
	 * 登録ボタンが押されたとき
	 * 
	 * @param view
	 */
	public void onCreateButtonClick(View view) {
		int requestCode = RequestCode.DASHBOARD2CREATE.ordinal();
		gotoReaderActivity(requestCode);
	}

	/**
	 * お気に入りボタンが押されたとき
	 * 
	 * @param view
	 */
	public void onFavoriteButtonClick(View view) {
		gotoFavoriteActivity();
	}

	/**
	 * 履歴ボタンが押されたとき
	 * 
	 * @param view
	 */
	public void onHistoryButtonClick(View view) {
		gotoHistoryActivity();
	}

	/**
	 * 読込ボタンが押されたとき
	 * 
	 * @param view
	 */
	public void onReaderButtonClick(View view) {

		int requestCode = RequestCode.DASHBORAD2READER.ordinal();
		gotoReaderActivity(requestCode);
	}

	/**
	 * ラーメン一覧ボタンが押されたとき
	 * 
	 * @param view
	 */
	public void onRamenListButtonClick(View view) {

		gotoRamenListWeb();
	}

	/**
	 * 検索ボタンが押されたとき
	 * 
	 * @param view
	 */
	public void onRSearchButtonClick(View view) {

		gotoRSerchActivity();
	}

	/**
	 * つ部のロゴが押されたとき
	 * 
	 * @param view
	 */
	public void onTsubuLogoClick(View view) {
		gotoTsubuSite();
	}

	/**
	 * リーダーの起動 requestCodeでその後にTimerActivityかCreateActivityを選択
	 * 
	 * @param requestCode
	 */
	private void gotoReaderActivity(int requestCode) {
		Intent intent = new Intent(this, ReaderActivity.class);
		intent.putExtra(RequestCode.KEY_REQUEST_CODE, requestCode);
		startActivityForResult(intent, requestCode);
	}

	/**
	 * 履歴の起動
	 */
	private void gotoHistoryActivity() {
		Intent intent = new Intent(this, HistoryActivity.class);
		int requestCode = RequestCode.DASHBORAD2HISTORY.ordinal();
		intent.putExtra(RequestCode.KEY_REQUEST_CODE, requestCode);
		startActivityForResult(intent, requestCode);
	}

	/**
	 * 検索の起動
	 */
	private void gotoRSerchActivity() {
		Intent intent = new Intent(this, RamenSearchActivity.class);
		int requestCode = RequestCode.DASHBOARD2RAMENSEARCH.ordinal();
		intent.putExtra(RequestCode.KEY_REQUEST_CODE, requestCode);
		startActivityForResult(intent, requestCode);
	}

	/**
	 * マイリストの起動
	 */
	private void gotoFavoriteActivity() {
		Intent intent = new Intent(this, FavoriteActivity.class);
		int requestCode = RequestCode.DASHBOARD2FAVORITE.ordinal();
		intent.putExtra(RequestCode.KEY_REQUEST_CODE, requestCode);
		startActivityForResult(intent, requestCode);
	}

	/**
	 * タイマーの起動
	 */
	private void gotoTimerActivity() {
		Intent intent = new Intent(this, TimerActivity.class);
		int requestCode = RequestCode.DASHBORAD2TIMER.ordinal();
		intent.putExtra(RequestCode.KEY_REQUEST_CODE, requestCode);
		startActivityForResult(intent, requestCode);
	}

	/**
	 * ラーメンタイマーWebサイトを開く
	 */
	private void gotoRamenListWeb() {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(RAMEN_LIST_WEB_ADDRESS));
		startActivity(intent);
	}

	/**
	 * つ部のWebサイトを開く
	 */
	private void gotoTsubuSite() {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(TSUBU_WEB_ADDRESS));
		startActivity(intent);
	}

	/**
	 * インテントがもどってきた時の動作 アクションバーが他のActivityで押さたときは
	 * Dashboardまで戻って、目的のActivityを実行する
	 * 
	 * @param requestCode
	 * @param resultCode
	 * @param intent
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (textCountView.getText().equals(
				getString(R.string.dashboard_countu_fail))) {
			// 軒数取得に失敗していたらもう一度取得にいってみる
			doCountTask();
		}

		if (resultCode == RESULT_OK) {
			if (intent == null) {
				return;
			}
			// 戻り値のREQUEST_CODEに応じてActivityを選択する
			int rtn_code = intent.getIntExtra(RequestCode.KEY_REQUEST_CODE, -1);
			// エラー処理
			if (rtn_code == -1) {
				return;
			}
			if (rtn_code >= RequestCode.values().length) {
				return;
			}
			// アクションバーが他のActivityで押さたときはDashboardまで戻って、他のActivityを実行する
			switch (RequestCode.values()[rtn_code]) {
			case ACTION_HISTORY: // アクションバーの履歴ボタンが押された場合
				gotoHistoryActivity();
				break;
			case ACTION＿TIMER: // アクションバーのタイマーボタンが押された場合
				gotoTimerActivity();
				break;
			case ACTION_READER: // アクションバーの読込ボタンが押された場合
				gotoReaderActivity(RequestCode.DASHBORAD2READER.ordinal());
				break;
			case ACTION_SEARCH:// アクションバーの検索ボタンが押された場合
				gotoRSerchActivity();
				break;
			}
		} else if (RESULT_CANCELED == resultCode) {
			// @leibun追加
			if (null == intent) {
				return;
			}
			// @tan1234jp追加
			// int rtn_code = intent.getIntExtra(RequestCode.KEY_REQUEST_CODE,
			// -1);
			// エラーコードのKEYから返り値をもらう
			int rtn_code = intent.getIntExtra(ReaderActivity.KEY_ERROR_RESID,
					-1);
			// エラー処理
			if (-1 == rtn_code) {
				return;
			}
			// エラーコード(エラー文字列リソースID)がrtn_codeに入っているので、
			// トーストで表示する
			Toast.makeText(this, getString(rtn_code), Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * アクティビティが表示される直前に呼び出される ここでは、前回バグで強制終了した場合に、レポート送信を行うかどうか 問い合わせる
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// 前回バグで強制終了した場合は、ダイアログを表示する
		AppUncaughtExceptionHandler.showBugReportDialogIfExist();
	}

	/**
	 * 軒数取得Taskを起動する
	 */
	private void doCountTask() {
		CountTask task = new CountTask();
		task.execute();
	}

	/**
	 * Logoがクリックされた
	 * 
	 * @param view
	 */
	public void onClickLogo(View view) {
		// テストとしてtwitter認証画面を表示させる
		Intent intent = new Intent();
		intent.setClass(this, AuthorizationActivity.class);
		startActivity(intent);
	}

	/**
	 * Postしてみる
	 * 
	 * @param view
	 */
	public void onClickTitle(View view) {
		// try {
		// NoodleMaster master = new NoodleMaster("9999999999", "てすてすラーメン",
		// "", 180);
		// TwitterManager.getInstance().post(this, master);
		// Toast.makeText(this, "つぶやきました！", Toast.LENGTH_LONG).show();
		// } catch (TwitterException e) {
		// Log.e(DashBoardActivity.class.getName(), e.getMessage());
		// Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		// } catch (Exception e2) {
		// Log.e(DashBoardActivity.class.getName(), e2.getMessage());
		// Toast.makeText(this, e2.getMessage(), Toast.LENGTH_LONG).show();
		// }
	}

	/**
	 * 件数取得用非同期Task
	 * 
	 * @author miguse
	 * 
	 */
	private class CountTask extends AsyncTask<String, Void, String> {
		@Override
		protected void onPreExecute() {
			textCountView.setTextColor(getResources().getColor(
					R.color.count_message_getorfail));
			textCountView.setText(R.string.dashboard_countu_get);

		}

		@Override
		protected String doInBackground(String... areg0) {
			// try {
			// Thread.sleep(5000);
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

			try {
				return getString(R.string.dashboard_toroku)
						+ Integer.toString(manager.getMasterCount())
						+ getString(R.string.dashboard_countu);
			} catch (GaeException e) {
				// TODO Auto-generated catch block
				return getString(R.string.dashboard_countu_fail);
			}

		}

		@Override
		protected void onPostExecute(String result) {

			if (result.equals(getString(R.string.dashboard_countu_fail))) {
				textCountView.setTextColor(getResources().getColor(
						R.color.count_message_getorfail));
			} else {
				textCountView.setTextColor(getResources().getColor(
						R.color.count_message));
			}
			textCountView.setText(result);
		}

	}
}
