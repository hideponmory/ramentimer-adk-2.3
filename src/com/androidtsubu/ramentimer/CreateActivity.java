package com.androidtsubu.ramentimer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import twitter4j.TwitterException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidtsubu.ramentimer.quickaction.ActionItem;
import com.androidtsubu.ramentimer.quickaction.QuickAction;

public class CreateActivity extends Activity {
	// RequestCode
	private static final int REQUEST_GALLERY = 1;
	private static final int REQUEST_CAMERA = 2;

	// 秒の増減間隔
	private static final int SEC_INTERVALS = 10;
	// 分の上限値
	private static final int MIN_UPPEL_LIMIT = 59;
	// 分の下限値
	private static final int MIN_LOWER_LIMIT = 0;

	// 商品情報(NoodleMaster)のキー
	private static final String KEY_NOODLE_MASTER = "NOODLE_MASTER";

	// JANコード
	private TextView janText = null;
	// 商品名
	private EditText nameEdit = null;
	// ゆで時間(分）
	private TextView minTextView = null;
	// ゆで時間(秒)
	private TextView secTextView = null;
	// 登録ボタン
	private Button createButton = null;
	// プログレスアイコン
	private ImageView progressIcon = null;
	// 時間設定のボタン
	private Button minUpButton = null;
	private Button minDownButton = null;
	private Button secUpButton = null;
	private Button secDownButton = null;
	private CheckBox checkBoxTwitter = null;
	// //麺の種類
	// private RadioGroup noodleTypeRadioGroup = null;
	// 商品の画像
	private ImageButton noodleImageView = null;
	private Bitmap noodleImage = null;
	// リクエストコードの値（どこから呼び出されたか）
	private int requestCode = 0;
	// カップラーメン情報
	private NoodleMaster noodleMaster = null;
	// カメラ撮影用
	private Uri mPictureUri;

	// 確認ダイアログ
	AlertDialog verificationDialog = null;

	// QuickAction のアイテム カメラ
	ActionItem itemCamera = null;
	// QuickAction のアイテム ギャラリー
	ActionItem itemGallery = null;
	// QuickAction のアイテム ホーム
	ActionItem itemHome = null;
	// QuickAction のアイテム タイマー
	ActionItem itemTimer = null;

	// WEB登録用スレッド
	EntryAsyncTask entry = null;

	private String cameraFileName = null;
	/** 標準カメラのパッケージ名 */
	private static final String CAMERA_PACKAGE = "com.android.camera";
	/** 標準カメラのActivity名 */
	private static final String CAMERA_ACTIVITY = "com.android.camera.Camera";

	/**
	 * CreateActivityがインテントで呼び出されたときに呼ばれる リクエストコードとNoodleMasterがセットされていないと終了します
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create);

		// リクエストコードを取得
		Intent intent = getIntent();
		requestCode = intent.getIntExtra(RequestCode.KEY_REQUEST_CODE, -1);
		// リクエストコードがセットされてない場合は終了
		if (requestCode == -1)
			finish();

		// ボタンとかエディットボックスとかのViewをメンバー変数に格納
		initUi();

		// カップラーメン情報の取得
		noodleMaster = (NoodleMaster) intent
				.getParcelableExtra(KEY_NOODLE_MASTER);

		// NoodleMasterから情報を取り出す
		String nmJancode = noodleMaster.getJanCode();
		String nmName = noodleMaster.getName();
		String nmTimerLimitString = noodleMaster.getTimerLimitString(
				getString(R.string.min_unit), getString(R.string.sec_unit));
		int nmTimerLimitInt = noodleMaster.getTimerLimit();
		Bitmap nmImage = noodleMaster.getImage();

		// UIにNoodleMasterの情報をセット
		if (nmJancode != null)
			janText.setText(nmJancode);
		if (nmName != null)
			nameEdit.setText(nmName);
		if (nmTimerLimitString != null)
			updateTimerTextView(nmTimerLimitInt);
		if (nmImage != null)
			noodleImageView.setImageBitmap(nmImage);

		// twitterに投稿するCheckBoxを保存してある設定にする
		checkBoxTwitter.setChecked(TwitterManager.getInstance().isPostTwitter(
				this));
		// NoodleMasterが全部埋まっている場合は、既に登録されているので終了
		if (nmJancode != null && nmName != null && nmTimerLimitString != null
				&& nmImage != null) {
			Toast.makeText(this, R.string.sql_already_created,
					Toast.LENGTH_LONG).show();
			finish();
		}

		// QuickAction用のItemを初期化
		initActionItem();

		// //麺の種類をラジオボタンで作成
		// final NoodleType NoodleTypeValues[] = NoodleType.values();
		// for(int i=0;i<NoodleTypeValues.length;i++){
		// RadioButton radioButton=new RadioButton(this);
		// radioButton.setText(NoodleTypeValues[i].getName());
		// radioButton.setId(i);
		// radioButton.setTextColor(R.color.information_form_text2);
		// noodleTypeRadioGroup.addView(radioButton);
		// }
	}

	/**
	 * ボタンとかエディットボックスとかのUIを取ってくる
	 */
	private void initUi() {
		janText = (TextView) findViewById(R.id.JanEdit);
		nameEdit = (EditText) findViewById(R.id.NameEdit);
		minTextView = (TextView) findViewById(R.id.MinTextView);
		secTextView = (TextView) findViewById(R.id.SecTextView);
		createButton = (Button) findViewById(R.id.CreateButton);
		// //0分0秒だと登録ボタンを押せないようにする
		// createButton.setEnabled(false);
		// //無効の時の字の色を変える
		// createButton.setTextColor(R.color.button_disabled);

		progressIcon = (ImageView) findViewById(R.id.ProgressIcon);
		minUpButton = (Button) findViewById(R.id.MinUpButton);
		minDownButton = (Button) findViewById(R.id.MinDownButton);
		secUpButton = (Button) findViewById(R.id.SecUpButton);
		secDownButton = (Button) findViewById(R.id.SecDownButton);
		// noodleTypeRadioGroup = (RadioGroup)
		// findViewById(R.id.NoodleTypeRadioGroup);
		noodleImageView = (ImageButton) findViewById(R.id.NoodleImageButton);
		checkBoxTwitter = (CheckBox) findViewById(R.id.checkBoxTwitter);
	}

	/**
	 * QuickActionのためのItemを作成 QuickAction自体は onLoadImageClick()で作成
	 */
	private void initActionItem() {
		Resources resources = getResources();

		itemCamera = new ActionItem();
		itemCamera.setTitle(resources
				.getString(R.string.create_quick_action_camera));
		itemCamera.setIcon(getResources().getDrawable(
				R.drawable.ic_popup_camera));
		itemCamera.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				callCamera(); // カメラを起動
			}
		});

		itemGallery = new ActionItem();
		itemGallery.setTitle(resources
				.getString(R.string.create_quick_action_gallery));
		itemGallery.setIcon(getResources().getDrawable(
				R.drawable.ic_popup_photos));
		itemGallery.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				callGallery(); // ギャラリーを起動
			}
		});
	}

	/**
	 * 画面が回転時に呼び出される
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	/**
	 * インテントがもどってきた時の動作
	 * 
	 * @param requestCode
	 * @param resultCode
	 * @param intent
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// dimens.xmlから値を取得 リサイズのパラメータ
		int resizeLength = (int) getResources().getDimension(
				R.dimen.image_longer_length);
		if (requestCode == REQUEST_CAMERA || requestCode == REQUEST_GALLERY) {
			if (resultCode == RESULT_OK) {
				Uri uri = null;
				if (requestCode == REQUEST_GALLERY) {// ギャラリー
					uri = intent.getData();
				} else {
					/*
					 * カメラの動作 GalaxyS対策：uriをPath(String)から生成　
					 * callCamera側でセットしたUriはnullになってしまうらしい。
					 * Xperia対策：セットしたファイル名の通りに画像が作られないので、getData()からUriを取得
					 */
					uri = mPictureUri;
					// Xperia 2.1対策
					if (intent != null) {
						Uri _uri = intent.getData();
						if (_uri != null)
							uri = intent.getData();
					}
				}
				try {
					// 画像の取得
					// URI -> image size -> small bitmap
					noodleImage = getImageFromUriUsingBitmapFactoryOptions(uri,
							resizeLength);

					// // URI -> bitmap -> small bitmap
					// noodleImage =
					// getImageFromUriUsingResizeImage(uri,resizeLength);

					// ビューに画像をセット
					noodleImageView.setImageBitmap(noodleImage);

				} catch (IOException e) {
					Log.e(CreateActivity.class.getName(), e.getMessage());
					Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
							.show();
				} catch (NullPointerException e) {
					Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
							.show();
				} catch (OutOfMemoryError e) {
					Toast.makeText(this, e.toString(), Toast.LENGTH_LONG)
							.show();
				}
			}
		}
		// アクションバーのボタン動作をダッシュボードまで伝達させる
		else if (requestCode == RequestCode.CREATE2TIMER.ordinal()) {
			setResult(resultCode, intent);
			finish();
		}
	}

	/**
	 * UriからBitmapを取得する メモリを節約のために 大まかに1/（2^n）にサイズを縮小してBitmapを読み込んで
	 * さらに、Bitmap.createで微調整
	 * 
	 * @param uri
	 * @param resizeLength
	 *            　リサイズパラメータ
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Bitmap getImageFromUriUsingBitmapFactoryOptions(Uri uri,
			int resizeLength) throws FileNotFoundException, IOException {
		try {
			if (uri == null)
				// throw new NullPointerException();
				return null;
			// UriからBitmapクラスを取得
			InputStream is = getContentResolver().openInputStream(uri);
			// オプション
			BitmapFactory.Options opts = new BitmapFactory.Options();
			// 画像サイズだけを取得するように設定　デコードはされない
			opts.inJustDecodeBounds = true;
			Bitmap image = BitmapFactory.decodeStream(is, null, opts);
			is.close();
			// デコードするように設定
			opts.inJustDecodeBounds = false;
			// 縦横比を固定したままリサイズ
			resizeOptions(opts, resizeLength);
			is = getContentResolver().openInputStream(uri);
			image = BitmapFactory.decodeStream(is, null, opts);
			is.close();
			Bitmap rImage = resizeImage(image, resizeLength);
			return rImage;
		} catch (NullPointerException ex) {
			return null;
		}
	}

	/**
	 * UriからBitmapを取得する
	 * 
	 * @param uri
	 * @param resizeLength
	 *            　リサイズパラメータ
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Bitmap getImageFromUriUsingResizeImage(Uri uri, int resizeLength)
			throws FileNotFoundException, IOException {
		// UriからBitmapクラスを取得
		InputStream is = getContentResolver().openInputStream(uri);
		// メモリを大量に使うのでガベコレ これしておかないと、何回か呼び出されるとエラーで止まる
		System.gc();
		Bitmap tmp = BitmapFactory.decodeStream(is);
		is.close();
		// 縦横比を固定したままリサイズ
		Bitmap image = resizeImage(tmp, resizeLength);
		return image;
	}

	/**
	 * 縦横比を維持したまま画像をリサイズするメソッド 長い方の辺が引数のlengthの長さなる
	 * 
	 * @param img
	 * @param length
	 * @return
	 */
	public Bitmap resizeImage(Bitmap img, int length) {
		int height = img.getHeight();
		int width = img.getWidth();
		// 縦、横の長い方
		float longer = height < width ? (float) width : (float) height;
		// 伸縮するスケール
		float scale = length / longer;
		Matrix matrix = new Matrix();
		matrix.postScale(scale, scale);
		// リサイズした画像を作成
		Bitmap dst = Bitmap
				.createBitmap(img, 0, 0, width, height, matrix, true);
		return dst;
	}

	/**
	 * 縦横比を維持したまま画像をリサイズするメソッド 長い方の辺が引数のlengthの長さなる
	 * 
	 * @param opts
	 * @param length
	 */
	public void resizeOptions(BitmapFactory.Options opts, int length) {
		int height = opts.outWidth;
		int width = opts.outHeight;
		// 縦、横の長い方
		float longer = height < width ? (float) width : (float) height;
		// 伸縮するスケール
		float scale = longer / length;
		// inSampleSizeが2の倍数しか受付ないので、端数を切り捨て
		// Log2(scale)を計算
		int log2 = (int) (Math.log10(scale) / Math.log10(2));
		int scale_int = 1;
		for (int i = 0; i < log2; i++)
			scale_int *= 2;

		// 置き換え
		opts.outHeight = Math.round(height / scale_int);
		opts.outWidth = Math.round(width / scale_int);
		opts.inSampleSize = scale_int;
	}

	/**
	 * アクションバーの履歴ボタンが押されたとき インテントに（RequestCode）をセットしてfinish()
	 * 
	 * @param v
	 */
	public void onHistoryButtonClick(View v) {
		Intent intent = new Intent();
		intent.putExtra(RequestCode.KEY_REQUEST_CODE,
				RequestCode.ACTION_HISTORY.ordinal());
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * アクションバーのタイマーボタンが押されたとき インテントに（）をセットしてFinish()
	 * 
	 * @param v
	 */
	public void onTimerButtonClick(View v) {
		Intent intent = new Intent();
		intent.putExtra(RequestCode.KEY_REQUEST_CODE,
				RequestCode.ACTION＿TIMER.ordinal());
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * 画像読み込みボタンが押された時の動作 インテントでギャラリーかカメラを呼び出す
	 * 
	 * @param v
	 */
	public void onLoadImageClick(View v) {
		// ストレージが使えるか確認
		NoodleManager noodleManager = new NoodleManager(this);
		if (noodleManager.hasExternalStorage()) {
			// QuickAction
			QuickAction qa = new QuickAction(v);

			qa.addActionItem(itemCamera);
			qa.addActionItem(itemGallery);
			qa.show();
		}
	}

	/**
	 * 分の＋ボタンが押されたとき
	 * 
	 * @param v
	 */
	public void onMinUpClick(View v) {
		addTimerCount(60);
	}

	/**
	 * 分のーボタンが押されたとき
	 * 
	 * @param v
	 */
	public void onMinDownClick(View v) {
		addTimerCount(-60);
	}

	/**
	 * 秒の＋ボタンが押されたとき ※分も変わる場合がある
	 * 
	 * @param v
	 */
	public void onSecUpClick(View v) {
		addTimerCount(SEC_INTERVALS);
	}

	/**
	 * 秒のーボタンが押されたとき ※分も変わる場合がある
	 * 
	 * @param v
	 */
	public void onSecDownClick(View v) {
		addTimerCount(-SEC_INTERVALS);
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
		minTextView.setText(String.valueOf(sec / 60));
		secTextView.setText(getSecText(sec % 60));
		// //0秒ならボタンを無効にする
		// createButton.setEnabled(sec != 0);
		// //ボタンが有効の時と無効の時の字の色を変える
		// createButton.setTextColor(createButton.isEnabled() ?
		// R.color.button_enabled : R.color.button_disabled);
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
	 * ギャラリーをインテントで起動
	 */
	private void callGallery() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(intent, REQUEST_GALLERY);
	}

	/**
	 * カメラをインテントで起動
	 */
	private void callCamera() {
		cameraFileName = "RamenTimer_" + System.currentTimeMillis() + ".jpg";

		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, cameraFileName);
		values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
		mPictureUri = getContentResolver().insert(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		// mPictureUri = Uri.fromFile(new
		// File(Environment.getExternalStorageDirectory(),cameraFileName));

		Intent intent = new Intent();
		intent.setAction("android.media.action.IMAGE_CAPTURE");
		intent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri);
		// List<ResolveInfo> apps = getPackageManager().queryIntentActivities(
		// intent, 0);
		// for (ResolveInfo app : apps) {
		// Log.d(CreateActivity.class.getName(), app.activityInfo.name);
		// }
		// 標準カメラ狙い撃ちとする
		try {
			intent.setClassName(CAMERA_PACKAGE, CAMERA_ACTIVITY);
			startActivityForResult(intent, REQUEST_CAMERA);
		} catch (ActivityNotFoundException ex) {
			// Toast.makeText(CreateActivity.this, ex.getMessage(),
			// Toast.LENGTH_LONG).show();
			// 標準カメラ狙い撃ちでエラーが出た場合は暗黙Intentを投げる
			intent = new Intent();
			intent.setAction("android.media.action.IMAGE_CAPTURE");
			intent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri);
			startActivityForResult(intent, REQUEST_CAMERA);
		}
	}

	/**
	 * タイマーをインテントで呼び出す
	 */
	private void callTimerActivity() {
		Intent intent = new Intent(this, TimerActivity.class);
		intent.putExtra(RequestCode.KEY_REQUEST_CODE,
				RequestCode.CREATE2TIMER.ordinal());
		intent.putExtra(KEY_NOODLE_MASTER, noodleMaster);
		startActivityForResult(intent, RequestCode.CREATE2TIMER.ordinal());
	}

	/**
	 * 登録ボタンが押された時の動作
	 * 
	 * @param v
	 */
	public void onCreateClick(View v) {
		// twitterに投稿するかどうかを保存しておく
		TwitterManager.getInstance().setPostTwitter(this,
				checkBoxTwitter.isChecked());
		// ソフトウェアキーボードを非表示にする
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);

		// 登録ボタンの二重押し防止
		createButton.setEnabled(false);

		try {
			noodleMaster = getNoodleMaster();
		} catch (CreateNoImageException e) {
			Toast.makeText(this, R.string.create_set_image_message,
					Toast.LENGTH_LONG).show();
			createButton.setEnabled(true);
			return;
		} catch (CreateZeroException e) {
			Toast.makeText(this, R.string.create_set_time_message,
					Toast.LENGTH_LONG).show();
			createButton.setEnabled(true);
			return;
		} catch (Exception e) {
			Toast.makeText(this, R.string.create_fill_form_message,
					Toast.LENGTH_LONG).show();
			createButton.setEnabled(true);
			return;
		}
		entry = new EntryAsyncTask(this);

		LayoutInflater inflater = (LayoutInflater) this
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_create_verification,
				(ViewGroup) findViewById(R.id.layout_verification_root));
		TextView jan_text = (TextView) layout.findViewById(R.id.JanText);
		jan_text.setText(noodleMaster.getJanCode());
		TextView name_text = (TextView) layout.findViewById(R.id.RamenName);
		name_text.setText(noodleMaster.getName());
		TextView time_text = (TextView) layout.findViewById(R.id.BoilingTime);
		time_text.setText(noodleMaster.getTimerLimitString(
				getString(R.string.min_unit), getString(R.string.sec_unit)));
		ImageView image = (ImageView) layout.findViewById(R.id.NoodleImage);
		image.setImageBitmap(noodleMaster.getImage());
		Button okButton = (Button) layout
				.findViewById(R.id.CreateDialogOkButton);
		okButton.setOnClickListener(dialogOkClick);
		Button cancelButton = (Button) layout
				.findViewById(R.id.CreateDialogCancelButton);
		cancelButton.setOnClickListener(dialogCancelClick);

		// 確認ダイアログの作成
		verificationDialog = new CustomAlertDialog(this, R.style.CustomDialog);
		verificationDialog.setTitle(R.string.dialog_create_verification_title);
		verificationDialog.setView(layout);
		verificationDialog.show();
	}

	/**
	 * ダイアログでOKがクリックされたとき
	 */
	OnClickListener dialogOkClick = new OnClickListener() {
		public void onClick(View v) {
			if (entry != null && verificationDialog != null) {
				entry.execute(noodleMaster);
				progressMode();
				verificationDialog.dismiss();
			}
		}
	};

	/**
	 * ダイアログでキャンセルがクリックされたとき
	 */
	OnClickListener dialogCancelClick = new OnClickListener() {
		public void onClick(View v) {
			if (verificationDialog != null)
				verificationDialog.cancel();
			createButton.setEnabled(true);
		}
	};

	/**
	 * 登録ボタンとかタイマーボタンを消してプログレスアイコンを表示する
	 */
	private void progressMode() {
		// 登録ボタンを消す (GONEなので空間ごと消える)
		createButton.setVisibility(View.GONE);
		// ImageButtonを無効化する
		noodleImageView.setClickable(false);
		// EditTextを無効化する
		nameEdit.setEnabled(false);
		// pickerButtonを隠す
		hidePickerButton();
		// プログレスアイコンの表示とアニメーションのセット
		progressIcon.setVisibility(ImageView.VISIBLE);
		progressIcon.startAnimation(AnimationUtils.loadAnimation(this,
				R.anim.progress_icon));
	}

	/**
	 * 登録ボタンとかタイマーボタンを表示してプログレスアイコンを非表示にする
	 */
	private void inputMode() {
		// 登録ボタンを消す (GONEなので空間ごと消える)
		createButton.setVisibility(View.VISIBLE);
		createButton.setEnabled(true);
		// ImageButtonを押せるようにする
		noodleImageView.setClickable(true);
		// EditTextを入力可能にする
		nameEdit.setEnabled(true);
		// pickerButtonを表示する
		showPickerButton();
		// プログレスアイコンの表示とアニメーションのセット
		progressIcon.setVisibility(ImageView.GONE);
		progressIcon.clearAnimation();

	}

	/**
	 * 時間調整ボタンを非表示にする
	 */
	private void hidePickerButton() {
		minUpButton.setVisibility(View.INVISIBLE);
		minDownButton.setVisibility(View.INVISIBLE);
		secUpButton.setVisibility(View.INVISIBLE);
		secDownButton.setVisibility(View.INVISIBLE);
	}

	/**
	 * 時間調整ボタンを表示する
	 */
	private void showPickerButton() {
		minUpButton.setVisibility(View.VISIBLE);
		minDownButton.setVisibility(View.VISIBLE);
		secUpButton.setVisibility(View.VISIBLE);
		secDownButton.setVisibility(View.VISIBLE);
	}

	/**
	 * logoボタンが押された時の動作
	 * 
	 * @param v
	 */
	public void onLogoClick(View v) {
		finish();
	}

	/**
	 * UIから登録情報を集めて返す
	 * 
	 * @return
	 * @throws CreateNoImageException
	 */
	NoodleMaster getNoodleMaster() throws CreateNoImageException,
			CreateZeroException {
		// EditTextやRadioGroupから状態を取得
		String jancode = janText.getText().toString();
		String name = nameEdit.getText().toString();
		// 分と秒を取得
		int min = Integer.parseInt(minTextView.getText().toString());
		int sec = Integer.parseInt(secTextView.getText().toString());
		// 秒に変換
		int boilTime = min * 60 + sec;
		if (boilTime == 0) {
			throw new CreateZeroException();
		}
		// 画像の取得
		Bitmap image;
		if (noodleImage == null) // セットされていない場合なダミー画像を入れる
			throw new CreateNoImageException();
		else
			image = noodleImage;
		// NoodleType noodleType =
		// NoodleType.values()[noodleTypeRadioGroup.getCheckedRadioButtonId()];
		NoodleMaster noodle = new NoodleMaster(jancode, name, image, boilTime);
		return noodle;
	}

	/**
	 * ラーメンの情報を登録する
	 * 
	 * @author leibun
	 * 
	 */
	private class EntryAsyncTask extends
			AsyncTask<NoodleMaster, Integer, Integer> {
		// 表示用にコンテキストを保持
		private Activity activity = null;
		private NoodleManager nm;
		private AlertDialog dialog = null;

		private static final int RESULT_CREATE_OK = 0; // 登録成功
		private static final int RESULT_ERROR_SQLITE = 1;// SQLITEでエラー
		private static final int RESULT_ERROR_GAE = 2; // Webへの登録でエラー
		private static final int RESULT_DUPLEX = 3; // 重複登録

		private GaeException exception = null;

		/**
		 * コンストラクタ
		 * 
		 * @param context
		 */
		public EntryAsyncTask(Activity activity) {
			this.activity = activity;
			this.nm = new NoodleManager(activity);
		}

		/**
		 * Web登録があるので別スレッドで実行
		 * 
		 * @params params
		 */
		@Override
		protected Integer doInBackground(NoodleMaster... params) {
			try {
				// カップラーメンの情報をWebとローカルに登録
				nm.createNoodleMaster(params[0]);
			} catch (DuplexNoodleMasterException e) {
				Log.i("ramentimer.CreateActivity",
						ExceptionToStringConverter.convert(e));
				return RESULT_DUPLEX;
			} catch (GaeException e) {
				Log.e("ramentimer.CreateActivity",
						ExceptionToStringConverter.convert(e));
				this.exception = e;
				return RESULT_ERROR_GAE;
			} catch (java.sql.SQLException e) {
				Log.e("ramentimer.CreateActivity",
						ExceptionToStringConverter.convert(e));
				return RESULT_ERROR_SQLITE;
			}
			return RESULT_CREATE_OK;
		}

		/**
		 * 
		 * doInBackgroundが呼ばれた後に呼び出される
		 */
		@Override
		protected void onPostExecute(Integer result) {
			switch (result) {
			case RESULT_CREATE_OK:
				Toast.makeText(activity, R.string.sql_complete,
						Toast.LENGTH_LONG).show();
				// アニメーションの停止
				progressIcon.clearAnimation();
				Handler handler = new Handler();
				handler.post(new Runnable() {
					@Override
					public void run() {
						// //twitterに投稿するかどうか聞く
						if (checkBoxTwitter.isChecked()) {
							// twitterに投稿する
							progressMode();
							PostTwitterTask task = new PostTwitterTask();
							task.execute(noodleMaster);
						} else {
							selectNextMode();
						}
					}
				});
				return;
				// break;
			case RESULT_DUPLEX:
				Toast.makeText(activity, R.string.sql_already_created,
						Toast.LENGTH_LONG).show();
				// アニメーションの停止
				progressIcon.clearAnimation();
				break;
			case RESULT_ERROR_GAE:
				// エラー内容にGAEからの戻り値を表示する
				Toast.makeText(
						activity,
						getString(R.string.sql_gae_entry_error) + "\n"
								+ exception.getMessage(), Toast.LENGTH_LONG)
						.show();
				// UIを入力可能モードにする
				inputMode();
				return;
			case RESULT_ERROR_SQLITE:
				Toast.makeText(activity, R.string.sql_local_entry_error,
						Toast.LENGTH_LONG).show();
				// UIを入力可能モードにする
				inputMode();
				return;
			}
			// リーダーから呼び出された場合
			if (requestCode == RequestCode.READER2CREATE.ordinal()) {
				// ダイアログで選択させる
				dialog = getGotoDialog();
				dialog.show();
				// タイマーから呼び出された場合
			} else if (requestCode == RequestCode.TIMER2CREATE.ordinal()) {
				activity.finish();
			}
		}

	}

	/**
	 * TwitterPost用AsynkTask
	 * 
	 * @author hide
	 * 
	 */
	private class PostTwitterTask extends
			AsyncTask<NoodleMaster, Integer, Boolean> {
		private Exception ex = null;

		@Override
		protected Boolean doInBackground(NoodleMaster... params) {
			try {
				TwitterManager.getInstance().post(CreateActivity.this,
						params[0]);
			} catch (IllegalStateException e) {
				this.ex = e;
				Log.e(CreateActivity.class.getName(), e.getMessage(), e);
				return false;
			} catch (TwitterException e) {
				this.ex = e;
				Log.e(CreateActivity.class.getName(), e.getMessage(), e);
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if (result) {
				Toast.makeText(CreateActivity.this,
						getString(R.string.post_twitter_success),
						Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(
						CreateActivity.this,
						getString(R.string.post_twitter_failure) + "\n"
								+ ex.getMessage(), Toast.LENGTH_LONG).show();
			}
			selectNextMode();
		}

	}

	/**
	 * 次の行動を決定する
	 */
	private void selectNextMode() {
		// リーダーから呼び出された場合
		if (requestCode == RequestCode.READER2CREATE.ordinal()) {
			// ダイアログで選択させる
			Dialog dialog = getGotoDialog();
			dialog.show();
			// タイマーから呼び出された場合
		} else if (requestCode == RequestCode.TIMER2CREATE.ordinal()) {
			CreateActivity.this.finish();
		}
	}

	/**
	 * ダイアログを返す。レイアウトはdialog_create_goto.xmlを参照
	 * 
	 * @return
	 */
	private AlertDialog getGotoDialog() {
		Resources resources = getResources();
		final String DIALOG_TIMERSTART_TITLE = resources
				.getString(R.string.dialog_create_goto_title);
		final String DIALOG_TIMERSTART_TIMER = resources
				.getString(R.string.dialog_create_goto_timer);
		final String DIALOG_TIMERSTART_BACK = resources
				.getString(R.string.dialog_create_goto_back);

		CustomAlertDialog dialog = new CustomAlertDialog(this,
				R.style.CustomDialog);
		dialog.setTitle(DIALOG_TIMERSTART_TITLE);
		dialog.setButton(DIALOG_TIMERSTART_TIMER, onTimerClick);
		dialog.setButton2(DIALOG_TIMERSTART_BACK, onHomeClick);
		return dialog;
	}

	/**
	 * タイマーを起動のボタンが押されたとき
	 */
	private Dialog.OnClickListener onTimerClick = new Dialog.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int i) {
			dialog.dismiss();
			try {
				callTimerActivity();
			} catch (Exception e) {
				Toast.makeText(CreateActivity.this, e.getMessage(),
						Toast.LENGTH_LONG).show();
				finish();
			}
		}
	};
	/**
	 * ホームに戻るのボタンが押されたとき
	 */
	private Dialog.OnClickListener onHomeClick = new Dialog.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int i) {
			dialog.dismiss();
			finish();
		}
	};

	/**
	 * 0分0秒Exception
	 * 
	 * @author hide
	 */
	private class CreateZeroException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * コンストラクタ
		 */
		public CreateZeroException() {

		}

		/**
		 * コンストラクタ
		 * 
		 * @param throwable
		 */
		public CreateZeroException(Throwable throwable) {
			super(throwable);
		}

	}

}
