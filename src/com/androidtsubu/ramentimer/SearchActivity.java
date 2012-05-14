package com.androidtsubu.ramentimer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.androidtsubu.ramentimer.R;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * 検索Activity
 * ここで検索して結果を返す
 * @author hide
 *
 */
public class SearchActivity extends Activity {
	//検索種別
	private SearchKind kind;
	//検索文字列
	private String searchString;
	//検索種類のキー文字列
	public static String KEY_SEARCH_KIND = "SearchKind";
	//検索文字列のキー文字列
	public static String KEY_SEARCH_STRING = "SearchString";
	private NoodleManager manager;
	private ImageView progressIcon;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		//検索種別を得る
		kind = SearchKind.values()[getIntent().getIntExtra(KEY_SEARCH_KIND, 0)];
		//検索文字列を得る
		searchString = getIntent().getStringExtra(KEY_SEARCH_STRING);
		//NoodleManagerを生成する
		manager = new NoodleManager(this);

		progressIcon = (ImageView) findViewById(R.id.ReaderProgressIcon);
		// アニメーションの開始
		progressIcon.startAnimation(AnimationUtils.loadAnimation(this,
				R.anim.progress_icon));
		
		if(kind == SearchKind.FAVORITE){
			//お気に入り検索Taskを起動する
			FavoriteSearchTask task = new FavoriteSearchTask();
			task.execute(searchString);
			return;
		}
		//履歴検索Taskを起動する
		HistorySearchTask task = new HistorySearchTask();
		task.execute(searchString);
	}
	
	
	
	/**
	 * 履歴検索用非同期Task
	 * @author morikawa
	 *
	 */
	private class HistorySearchTask extends AsyncTask<String, Void, List<NoodleHistory>>{

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(List<NoodleHistory> result) {
			// TODO Auto-generated method stub
			if(result == null){
				//検索に失敗したら空っぽのlistを渡す
				result = new ArrayList<NoodleHistory>();
			}
			//検索に成功した
			Intent intent = new Intent();
			intent.putParcelableArrayListExtra(kind.getKey(), convertArrayList(result));
			setResult(RESULT_OK,intent);
			//くるくる止める
			progressIcon.clearAnimation();
			SearchActivity.this.finish();
		}

		@Override
		protected List<NoodleHistory> doInBackground(String... arg0) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				if(arg0[0] == null || arg0[0].equals("")){
					return manager.getNoodleHistories();
				}
				return manager.searchNoodleHistories(arg0[0]);
			} catch (SQLException e) {
				Toast.makeText(SearchActivity.this, R.string.search_alert, Toast.LENGTH_LONG).show();
				return null;
			}
		}
		
		/**
		 * ListをArrayListにする
		 * @param list
		 * @return
		 */
		private ArrayList<NoodleHistory> convertArrayList(List<NoodleHistory> list){
			if(list instanceof ArrayList){
				//ListがArrayListだったらキャストして返してあげる
				return (ArrayList<NoodleHistory>)list;
			}
			//ListがArrayListじゃなかったらArrayListに詰め直す
			ArrayList<NoodleHistory> arrayList = new ArrayList<NoodleHistory>();
			arrayList.addAll(list);
			return arrayList;
		}
	}
	
	
	
	/**
	 * お気に入り検索用非同期Task
	 * @author morikawa
	 *
	 */
	private class FavoriteSearchTask extends AsyncTask<String, Void, List<NoodleMaster>>{

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(List<NoodleMaster> result) {
			// TODO Auto-generated method stub
			if(result == null){
				//検索に失敗したら空っぽのlistを渡す
				result = new ArrayList<NoodleMaster>();
			}
			//検索に成功した
			Intent intent = new Intent();
			intent.putParcelableArrayListExtra(kind.getKey(), convertArrayList(result));
			setResult(RESULT_OK,intent);
			//くるくる止める
			progressIcon.clearAnimation();
			SearchActivity.this.finish();

		}

		@Override
		protected List<NoodleMaster> doInBackground(String... arg0) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				if(arg0[0] == null || arg0[0].equals("")){
					//べたでひっぱってくる
					return manager.getNoodleMastersForSqlite();
				}
				//キーで検索
				return manager.searchNoodleMaster(arg0[0]);
			} catch (SQLException e) {
				Toast.makeText(SearchActivity.this, R.string.search_alert, Toast.LENGTH_LONG).show();
				return null;
			}
		}

		/**
		 * listをArrayListに変換する
		 * @param list
		 * @return
		 */
		private ArrayList<NoodleMaster> convertArrayList(List<NoodleMaster> list){
			if(list instanceof ArrayList){
				//ListがArrayListだったらキャストして返してあげる
				return (ArrayList<NoodleMaster>)list;
			}
			//ListがArrayListじゃなかったらArrayListに詰め直す
			ArrayList<NoodleMaster> arrayList = new ArrayList<NoodleMaster>();
			arrayList.addAll(list);
			return arrayList;
		}
	}
	
	
	
}
