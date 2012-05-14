package com.androidtsubu.ramentimer.bugreport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;

/**
 * キャッチできない例外が発生した時に、レポートを作成・登録・送付する
 * 
 * @author TAN
 * 
 */
public class AppUncaughtExceptionHandler implements UncaughtExceptionHandler {

	// バグレポートファイル名
	private final static String BUG_REPORT_FILE_NAME = "BugReport.txt";
	// ファイル取り扱い
	private static File BUG_REPORT_FILE = null;
	// バグレポート送信URI
	private final static String BUG_REPORT_URI = "http://ramentimer-bugreport.appspot.com";

	// 外部メディア上にバグレポートを配置するため、フルパス名を生成する
	static {
		String sdcard = Environment.getExternalStorageDirectory().getPath();
		String path = sdcard + File.separator + BUG_REPORT_FILE_NAME;
		BUG_REPORT_FILE = new File(path);
	}

	private static Activity sActivity; // アクティビティ
	private static PackageInfo sPackInfo; // パッケージ
	private UncaughtExceptionHandler _defaultUEH; // デフォルトの例外ハンドラ
	private static String sDialogTitle; // ダイアログのタイトル
	private static String sDialogMessage; // ダイアログメッセージ
	private static String sDialog_Yes; // 「Yes」ボタンメッセージ
	private static String sDialog_No; // 「No」ボタンメッセージ

	private static ProgressDialog sDialog; // 進捗ダイアログ

	/**
	 * コンストラクタ
	 * 
	 * @param activity
	 * @param dialogTitle
	 * @param dialogMessage
	 * @param dialog_Yes
	 * @param dialog_No
	 */
	public AppUncaughtExceptionHandler(Activity activity, String dialogTitle,
			String dialogMessage, String dialog_Yes, String dialog_No) {
		// アクティビティをクラス変数に保存しておく
		sActivity = activity;
		// ダイアログで使用する文字列を設定する
		sDialogTitle = dialogTitle;
		sDialogMessage = dialogMessage;
		sDialog_Yes = dialog_Yes;
		sDialog_No = dialog_No;

		try {
			// パッケージ情報を取得する
			Context context = activity.getApplicationContext();
			sPackInfo = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		// デフォルトのUncaughtExceptionHandlerをメンバ変数に保存する
		_defaultUEH = Thread.getDefaultUncaughtExceptionHandler();

		// 進捗ダイアログを生成する
		sDialog = new ProgressDialog(sActivity);
		sDialog.setIndeterminate(true);
		// sDialog.setTitle("BUSY");
		// sDialog.setMessage("Now sending...");
	}

	/**
	 * 例外ハンドラを登録する
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		try {
			// エラーのスタックトレースを保存する
			saveStackTrace(ex);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		_defaultUEH.uncaughtException(thread, ex);
	}

	/**
	 * 例外エラーのスタックトレースを外部メモリに保存する
	 * 
	 * @param ex
	 * @throws FileNotFoundException
	 */
	private void saveStackTrace(Throwable ex) throws FileNotFoundException {

		// スタックトレースを取得する
		StackTraceElement[] stacks = ex.getStackTrace();
		// 保存先ファイルをオープンする
		File file = BUG_REPORT_FILE;
		PrintWriter pw = new PrintWriter(new FileOutputStream(file));
		StringBuffer sb = new StringBuffer();
		int length = stacks.length;

		pw.println(ex.getClass().getName());

		for (int i = 0; i < length; i++) {
			StackTraceElement stack = stacks[i];
			sb.setLength(0);
			sb.append("\tat ").append(stack.toString());
			pw.println(sb.toString());
		}

		Throwable cause = ex.getCause();
		if (cause != null) {
			StackTraceElement[] stackCause = cause.getStackTrace();
			length = stackCause.length;

			pw.println("caused by: " + cause.getClass().getName());
			for (int i = 0; i < length; i++) {
				StackTraceElement stack = stackCause[i];
				sb.setLength(0);
				sb.append("\tat ").append(stack.toString());
				pw.println(sb.toString());
			}
		}
		// バグレポートファイルをクローズする
		pw.close();
	}

	/**
	 * 外部メディアにレポートが存在している場合に、レポート送信ダイアログを表示する
	 */
	public static final void showBugReportDialogIfExist() {
		File file = BUG_REPORT_FILE;

		if (null != file && true == file.exists()) {

			AlertDialog.Builder builder = new AlertDialog.Builder(sActivity);
			builder.setTitle(sDialogTitle);
			builder.setMessage(sDialogMessage);
			builder.setNegativeButton(sDialog_No, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish(dialog);
				}
			});
			builder.setPositiveButton(sDialog_Yes, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					sDialog.show();
					postBugReportInBackground();
					dialog.dismiss();
				}
			});

			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	/**
	 * バグレポートをGoogle App Engineに送信する
	 */
	private static void postBugReportInBackground() {
		new Thread(new Runnable() {
			public void run() {
				// バグレポートを送信する
				postBugReport();
				// 送信済みのバグレポートを削除する
				File file = BUG_REPORT_FILE;
				if (null != file && true == file.exists()) {
					file.delete();
				}
				// 進捗ダイアログを消去する
				sDialog.dismiss();
			}
		}).start();
	}

	/**
	 * バグレポートをGoogle App Engineに送信する
	 */
	private static void postBugReport() {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		String bug = getFileBody(BUG_REPORT_FILE);
		nvps.add(new BasicNameValuePair("dev", Build.DEVICE));
		nvps.add(new BasicNameValuePair("mod", Build.MODEL));
		nvps.add(new BasicNameValuePair("sdk", Build.VERSION.SDK));
		nvps.add(new BasicNameValuePair("ver", sPackInfo.versionName));
		nvps.add(new BasicNameValuePair("bug", bug));

		try {
			HttpPost httpPost = new HttpPost(BUG_REPORT_URI);
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			DefaultHttpClient httpClient = new DefaultHttpClient();
			httpClient.execute(httpPost);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * バグレポートを取得する
	 * 
	 * @param file
	 * @return
	 */
	private static String getFileBody(File file) {
		StringBuffer sb = new StringBuffer();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while (null != (line = br.readLine())) {
				sb.append(line).append("\r\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sb.toString();
	}

	/**
	 * 本クラス終了時の処理
	 * 
	 * @param dialog
	 */
	private static void finish(DialogInterface dialog) {
		File file = BUG_REPORT_FILE;
		if (true == file.exists()) {
			file.delete();
		}

		dialog.dismiss();
	}
}
