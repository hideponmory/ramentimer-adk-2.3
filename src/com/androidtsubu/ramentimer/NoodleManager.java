package com.androidtsubu.ramentimer;

import java.io.File;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;

/**
 * データの読み書きをするマネージャーです
 * 
 * @author hide
 * 
 */
public class NoodleManager {
	/** SQLite読み書きクラス */
	private NoodleSqlController noodleSqlController;
	/** GAE読み書きクラス */
	private NoodleGaeController noodleGaeController;
	private Context context;
	/** image画像保存ディレクトリ */
	public static File SAVE_IMAGE_DIRECTORY;
	/** NoodleManager */
	private static NoodleManager noodleManager = null;

	/**
	 * コンストラクタ
	 * 
	 * @param _context
	 */
	public NoodleManager(Context _context) {
		this.context = _context;
		if (!hasExternalStorage()) {
			Handler handler = new Handler();
			handler.post(new Runnable() {
				@Override
				public void run() {
					CustomAlertDialog dialog = new CustomAlertDialog(context,
							R.style.CustomDialog);
					dialog.setTitle(R.string.alert_messeage);
					dialog.setButton("OK", (OnClickListener) null);
					dialog.setMessage(context
							.getString(R.string.No_Storage_Message));
					dialog.show();
				}
			});
		}
		noodleGaeController = new NoodleGaeController(context);
		noodleSqlController = new NoodleSqlController(context);
	}

	/**
	 * JANコードを引数にSQliteやGAEから商品マスタを得ます
	 * 
	 * @param janCode
	 * @return
	 */
	public NoodleMaster getNoodleMaster(String janCode) throws SQLException,
			GaeException {
		// SQliteに商品マスタがあるか問い合わせる
		NoodleMaster master = noodleSqlController.getNoodleMaster(janCode);
		if (master != null) {
			// SQliteの商品マスタを返す
			return master;
		}
		// GAEに問い合わせる
		master = noodleGaeController.getNoodleMaster(janCode);
		if (master != null) {
			// SQliteになくてGAEにある場合はSQliteに登録してあげる
			noodleSqlController.createNoodleMater(master);
		}
		return master;
	}

	/**
	 * SQLiteとGAEに商品を登録する
	 * 
	 * @param noodleMaster
	 * @throws SQLException
	 * @throws DuplexNoodleMasterException
	 * @throws GaeException
	 */
	public void createNoodleMaster(NoodleMaster noodleMaster)
			throws SQLException, DuplexNoodleMasterException, GaeException {
		// SQliteにすでに登録されていないか調べる
		NoodleMaster master = noodleSqlController.getNoodleMaster(noodleMaster
				.getJanCode());
		if (master != null) {
			// 削除する

			// 重複エラーを返す
			throw new DuplexNoodleMasterException();
		}
		// GAEに登録
		noodleGaeController.create(noodleMaster);
		// SQliteに登録
		noodleSqlController.createNoodleMater(noodleMaster);
	}

	/**
	 * 商品履歴を登録する
	 * 
	 * @param noodleMaster
	 * @throws SQLException
	 */
	public void createNoodleHistory(NoodleMaster noodleMaster, int measureTime,
			Date measureDate) throws SQLException {
		// 商品マスタは何かしら登録されてから使われているはずなのでGAEにマスタを登録することはしない
		noodleSqlController.createNoodleHistory(noodleMaster, measureTime,
				measureDate);
	}

	/**
	 * 商品履歴を返す
	 * 
	 * @return
	 * @throws SQLException
	 */
	public List<NoodleHistory> getNoodleHistories() throws SQLException {
		// とりあえず最新30件を返すようにしておく
		return noodleSqlController.getNoodleHistories(30);
	}

	/**
	 * マスタすべてを返す
	 * 
	 * @return
	 * @throws SQLException
	 */
	public List<NoodleMaster> getNoodleMastersForSqlite() throws SQLException {
		return noodleSqlController.getNoodleMasters();
	}

	/**
	 * JANコードで検索する
	 * 
	 * @param janCode
	 * @return
	 * @throws SQLException
	 */
	public List<NoodleMaster> searchNoodleMastersLikeJanCode(String janCode)
			throws SQLException {
		return noodleSqlController.getNoodleMastersLikeJanCode(janCode);
	}

	/**
	 * 名称で検索する
	 * 
	 * @param name
	 * @return
	 * @throws SQLException
	 */
	public List<NoodleMaster> searchNoodleMastersLikeName(String name)
			throws SQLException {
		return noodleSqlController.getNoodleMastersLikeName(name);
	}

	/**
	 * 商品マスタを検索する
	 * 
	 * @param keyword
	 * @return
	 * @throws SQLException
	 */
	public List<NoodleMaster> searchNoodleMaster(String keyword)
			throws SQLException {
		List<NoodleMaster> noodleMasters;
		// JANコードで探してみる
		noodleMasters = searchNoodleMastersLikeJanCode(keyword);
		// 名称で探してみる
		List<NoodleMaster> noodleMasters2 = searchNoodleMastersLikeName(keyword);
		for (NoodleMaster n2 : noodleMasters2) {
			if (!noodleMasters.contains(n2)) {
				noodleMasters.add(n2);
			}
		}
		return noodleMasters;
	}

	/**
	 * 商品履歴を検索する
	 * 
	 * @param keyword
	 * @return
	 * @throws SQLException
	 */
	public List<NoodleHistory> searchNoodleHistories(String keyword)
			throws SQLException {
		List<NoodleHistory> noodleHistories;
		// JANコードで探してみる
		noodleHistories = noodleSqlController
				.getNoodleHistoriesLikeJanCode(keyword);
		// 名称で探してみる
		List<NoodleHistory> noodleHisotHistories2 = noodleSqlController
				.getNoodleHistoriesLikeName(keyword);
		for (NoodleHistory n2 : noodleHisotHistories2) {
			if (!noodleHistories.contains(n2)) {
				noodleHistories.add(n2);
			}
		}
		return noodleHistories;
	}

	/**
	 * 外部ストレージチェック
	 */
	public boolean hasExternalStorage() {
		if (SAVE_IMAGE_DIRECTORY != null) {
			return true;
		}

		String status = Environment.getExternalStorageState();
		if (status.equals(Environment.MEDIA_MOUNTED)) {
			SAVE_IMAGE_DIRECTORY = new File(
					Environment.getExternalStorageDirectory(),
					context.getPackageName());
			if (!SAVE_IMAGE_DIRECTORY.exists()) {
				SAVE_IMAGE_DIRECTORY.mkdirs();
			}
			return true;
		}
		return false;
	}

	/**
	 * 商品マスタの登録件数を返す
	 * 
	 * @return
	 * @throws GaeException
	 */
	public int getMasterCount() throws GaeException {
		return noodleGaeController.getMasterCount();
	}

	/**
	 * GAEの情報でSQLiteの情報を上書きする
	 * 
	 * @throws SQLException
	 * @throws GaeException
	 */
	public void synchronizeFromGaeToSqlite() throws SQLException, GaeException {
		// SQLiteにある商品マスタリストを得る
		List<NoodleMaster> masters = noodleSqlController.getNoodleMasters();
		for (NoodleMaster n : masters) {
			// SQLiteにある商品マスタリストのバーコードから商品マスタを得る
			NoodleMaster noodleMaster = noodleGaeController.getNoodleMaster(n
					.getJanCode());
			noodleSqlController.updateNoodleMater(noodleMaster);
		}

	}

}
