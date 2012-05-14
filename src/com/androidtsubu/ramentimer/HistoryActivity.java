package com.androidtsubu.ramentimer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class HistoryActivity extends ListActivity {

	// 検索用EditText
	private EditText searchEdit = null;
	// タイトルテキストビュー
	private TextView titleText = null;
	private NoodleManager manager = null;
	// データがない場合
	private TextView emptyHistoryText = null;
	private SearchKind kind = SearchKind.HISTORY;

	// 履歴情報のリスト
	private List<NoodleHistory> list = new ArrayList<NoodleHistory>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.activity_history);
		searchEdit = (EditText) findViewById(R.id.SearchBarcodeEdit);
		emptyHistoryText = (TextView) findViewById(R.id.TextViewEmptyHistory);
		// Viewの取得
		// searchEdit = (EditText) findViewById(R.id.title_edit);
		titleText = (TextView) findViewById(R.id.title_text);
		// 履歴の呼び出し
		manager = new NoodleManager(this);
		search("");
	}

	/**
	 * 検索する
	 * 
	 * @param searchString
	 */
	private void search(String searchString) {
		// 検索Activityを呼び出す
		Intent intent = new Intent();
		intent.putExtra(SearchActivity.KEY_SEARCH_KIND, kind.ordinal());
		intent.putExtra(SearchActivity.KEY_SEARCH_STRING, searchString);
		intent.setClass(this, SearchActivity.class);
		startActivityForResult(intent, RequestCode.HISTORY2SEARCH.ordinal());
	}

	/**
	 * リストがクリックされた時の動作
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);

		// 押されたListItemに対応するNoodleMasterを取得
		NoodleHistory nh = list.get(position);
		NoodleMaster nm = nh.getNoodleMaster();
		// タイマーを起動
		Intent intent = new Intent(HistoryActivity.this, TimerActivity.class);
		intent.putExtra(RequestCode.KEY_REQUEST_CODE,
				RequestCode.HISTORY2TIMER.ordinal());
		intent.putExtra(TimerActivity.KEY_NOODLE_MASTER, nm);
		// 履歴も渡す @hideponm
		intent.putExtra(TimerActivity.KEY_NOODLE_HISTORY, nh);
		startActivityForResult(intent, RequestCode.HISTORY2TIMER.ordinal());
	}

	/**
	 * アクティビティの実行結果処理
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {

		// アクティビティのリクエストコードで処理を分ける
		if (requestCode == RequestCode.HISTORY2TIMER.ordinal()) {
			if (RESULT_OK == resultCode) {
				setResult(RESULT_OK, intent);
				// Intentをダッシュボードまで戻す。
				// 呼び出したインテントが空の場合は、処理を終了する
				finish();
			}
		}
		if (requestCode == RequestCode.HISTORY2SEARCH.ordinal()) {
			if (RESULT_OK == resultCode) {
				// 検索結果を取り出す
				list = intent.getParcelableArrayListExtra(kind.getKey());
				draw();
			}
		}
	}

	/**
	 * リストアイテムを扱うためのアダプタークラス
	 * 
	 * @author leibun
	 * 
	 */
	public class RamenListItemAdapter extends ArrayAdapter<NoodleHistory> {
		private LayoutInflater mInflater;
		private View mViews[];
		private Bitmap noImage = null;

		/**
		 * コンストラクタ
		 * 
		 * @param context
		 * @param rid
		 * @param list
		 */
		public RamenListItemAdapter(Context context, int rid,
				List<NoodleHistory> list) {
			super(context, rid, list);
			mInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			// Viewの入れものを作っておく
			mViews = new View[list.size()];
			for (int i = 0; i < list.size(); i++)
				mViews[i] = null;
			// 空のときの画像をロード
			noImage = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.img_ramen_noimage);

		}

		/**
		 * リストのアイテムを表示する部分
		 * 
		 * @param position
		 * @param convertView
		 * @param parent
		 */
		public View getView(int position, View convertView, ViewGroup parent) {
			if (null == mViews[position]) {
				NoodleHistory item = (NoodleHistory) getItem(position);
				mViews[position] = getView(item);
			}
			return mViews[position];
		}

		/**
		 * NoodleHistoryからViewを作る関数
		 * 
		 * @param item
		 * @return
		 */
		private View getView(NoodleHistory item) {
			// レイアウトファイルからViewを生成
			View view = mInflater.inflate(R.layout.list_item_ramen, null);
			// 画像をセット
			ImageView image;
			image = (ImageView) view.findViewById(R.id.NoodleImage);
			if (item.getImage() != null)
				image.setImageBitmap(item.getImage());
			else
				// 空のとき
				image.setImageBitmap(noImage);
			// カップラーメンの名前をセット
			TextView name;
			name = (TextView) view.findViewById(R.id.RamenName);
			name.setText(item.getName());
			// Janコードをセット
			TextView jancode;
			jancode = (TextView) view.findViewById(R.id.JanText);
			jancode.setText("" + item.getJanCode());
			// 時間をセット
			TextView boilTime = (TextView) view.findViewById(R.id.BoilingTime);
			boilTime.setText(item.getBoilTimeString(
					getString(R.string.min_unit), getString(R.string.sec_unit)));
			// 日付をセット
			TextView date;
			date = (TextView) view.findViewById(R.id.date);
			SimpleDateFormat format = new SimpleDateFormat(
					getString(R.string.decimalformat));
			date.setText(format.format(item.getMeasureTime()));
			// 初期値は不可視（GONE）なので見えるように変更
			date.setVisibility(TextView.VISIBLE);
			return view;
		}

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
	 * アクションバーの読込みボタンが押されたとき
	 */
	public void onReaderButtonClick(View v) {
		Intent intent = new Intent();
		intent.putExtra(RequestCode.KEY_REQUEST_CODE,
				RequestCode.ACTION_READER.ordinal());
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * アクションバーの検索ボタンが押されたとき これはleibunが作った方のクリックイベント。有効にする時までおいておく
	 */
	public void onSearchButtonClick(View v) {
		if (searchEdit.getVisibility() == View.GONE) {
			searchEdit.setVisibility(View.VISIBLE);
			titleText.setVisibility(View.GONE);
		} else {
			searchEdit.setVisibility(View.GONE);
			titleText.setVisibility(View.VISIBLE);
			// ソフトウェアキーボードを非表示にする
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		}

	}

	/**
	 * listを描画する
	 */
	private void draw() {
		if (list != null && list.size() > 0) {
			getListView().setVisibility(View.VISIBLE);
			emptyHistoryText.setVisibility(View.GONE);
			// RamenListItemAdapterを生成
			RamenListItemAdapter adapter;
			adapter = new RamenListItemAdapter(this, 0, list);
			setListAdapter(adapter);
			return;
		}

		getListView().setVisibility(View.GONE);
		emptyHistoryText.setVisibility(View.VISIBLE);
	}

	/**
	 * 検索ボタンが押された
	 * 
	 * @param v
	 */
	public void onSearchClick(View v) {
		// ソフトウェアキーボードを非表示にする
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		// 検索文字列で検索する
		String key = searchEdit.getText().toString();
		search(key);
	}

}
