package com.androidtsubu.ramentimer;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLiteへの読み書きをするクラスです
 * 
 * @author hide
 */
public class NoodleSqlController {
	private static final int DB_VERSION = 8;
	private static final String NOODLEMASTERTABLENAME = "NoodleMaster";
	private static final String NOODLEHISTORYTABLENAME = "NoodleHistory";

	/** テーブルCreate文 */
	private static final String CREATE_NOODLEMASTER_TABLE = "CREATE TABLE "
			+ NOODLEMASTERTABLENAME + "(" + "jancode TEXT PRIMARY KEY,"
			+ "name TEXT ,boiltime INTEGER ,image TEXT )";

	private static final String CREATE_NOODLEHISTORY_TABLE = "CREATE TABLE "
			+ NOODLEHISTORYTABLENAME + "("
			+ "_id INTEGER PRIMARY KEY AUTOINCREMENT, jancode TEXT,"
			+ "name TEXT ,boiltime INTEGER ,measuretime TEXT)";

	/** DB読み書きクラス */
	private static SQLiteDatabase database = null;
	private Context context;

	/**
	 * コンストラクタ
	 * 
	 * @param context
	 * @param directory
	 */
	public NoodleSqlController(Context context) {
		this.context = context;
		if (database == null) {
			DataBaseOpenHelper helper = new DataBaseOpenHelper(context);
			database = helper.getWritableDatabase();
		}
	}

	/**
	 * DBのcursorから商品マスタを生成します
	 * 
	 * @param cursor
	 * @return
	 */
	private NoodleMaster createNoodleMaster(Cursor cursor) {
		String jancode = cursor.getString(cursor.getColumnIndex("jancode"));
		String name = cursor.getString(cursor.getColumnIndex("name"));
		int boilTime = cursor.getInt(cursor.getColumnIndex("boiltime"));
		// 画像パス名を得る
		String imagePath = cursor.getString(cursor.getColumnIndex("image"));
		return new NoodleMaster(jancode, name, imagePath, boilTime);
	}

	/**
	 * SQLiteからJANコードで商品マスタを得ます
	 * 
	 * @param janCode
	 * @return
	 * @throws SQLException
	 */
	public NoodleMaster getNoodleMaster(String janCode) throws SQLException {
		String[] columns = { "jancode", "name", "boiltime", "image" };
		String where = "jancode = ?";
		String[] args = { janCode };
		Cursor cursor = null;
		try {
			// 検索
			cursor = database.query(NOODLEMASTERTABLENAME, columns, where,
					args, null, null, null);
			if (cursor == null) {
				throw new SQLException("cursor is null");
			}
			while (cursor.moveToNext()) {
				// カーソルから商品マスタを生成する
				return createNoodleMaster(cursor);
			}
			return null;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

	}

	/**
	 * JANコードのLIKE検索で商品マスタを得ます
	 * 
	 * @param janCode
	 * @return
	 * @throws SQLException
	 */
	public List<NoodleMaster> getNoodleMastersLikeJanCode(String janCode)
			throws SQLException {
		List<NoodleMaster> noodleMasters = new ArrayList<NoodleMaster>();
		String[] columns = { "jancode", "name", "boiltime", "image" };
		String where = "jancode LIKE ?";
		String[] args = { "%" + janCode + "%" };
		Cursor cursor = null;
		try {
			// 検索
			cursor = database.query(NOODLEMASTERTABLENAME, columns, where,
					args, null, null, null);
			if (cursor == null) {
				throw new SQLException("cursor is null");
			}
			while (cursor.moveToNext()) {
				// カーソルから商品マスタを生成する
				noodleMasters.add(createNoodleMaster(cursor));
			}
			return noodleMasters;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * 名称のLIKE検索で商品マスタを得ます
	 * 
	 * @param janCode
	 * @return
	 * @throws SQLException
	 */
	public List<NoodleMaster> getNoodleMastersLikeName(String name)
			throws SQLException {
		List<NoodleMaster> noodleMasters = new ArrayList<NoodleMaster>();
		String[] columns = { "jancode", "name", "boiltime", "image" };
		String where = "name LIKE ?";
		String[] args = { "%" + name + "%" };
		Cursor cursor = null;
		try {
			// 検索
			cursor = database.query(NOODLEMASTERTABLENAME, columns, where,
					args, null, null, null);
			if (cursor == null) {
				throw new SQLException("cursor is null");
			}
			while (cursor.moveToNext()) {
				// カーソルから商品マスタを生成する
				noodleMasters.add(createNoodleMaster(cursor));
			}
			return noodleMasters;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * SQLiteからすべての商品マスタを得ます
	 * 
	 * @return
	 * @throws SQLException
	 */
	public List<NoodleMaster> getNoodleMasters() throws SQLException {
		List<NoodleMaster> noodleMasters = new ArrayList<NoodleMaster>();
		String[] columns = { "jancode", "name", "boiltime", "image" };
		String orderby = "jancode";
		Cursor cursor = null;
		try {
			// 検索
			cursor = database.query(NOODLEMASTERTABLENAME, columns, null, null,
					null, null, orderby);
			if (cursor == null) {
				throw new SQLException("cursor is null");
			}
			while (cursor.moveToNext()) {
				// カーソルから商品マスタを生成してlistに追加する
				noodleMasters.add(createNoodleMaster(cursor));
			}
			return noodleMasters;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * SQLiteから最新の引数件数の商品履歴を得ます
	 * 
	 * @param rows
	 * @return
	 * @throws SQLException
	 */
	public List<NoodleHistory> getNoodleHistories(int rows) throws SQLException {
		List<NoodleHistory> histories = new ArrayList<NoodleHistory>();
		String[] columns = { "jancode", "name", "boiltime", "measuretime" };
		String orderby = "measuretime desc";
		Cursor cursor = null;
		try {
			// 検索
			cursor = database.query(NOODLEHISTORYTABLENAME, columns, null,
					null, null, null, orderby, Integer.toString(rows));
			if (cursor == null) {
				throw new SQLException("cursor is null");
			}
			while (cursor.moveToNext()) {
				NoodleHistory history = createNoodleHistory(cursor);
				// 履歴がきちんと作成できたらリストに追加する
				if (history != null) {
					histories.add(history);
				}
			}
			return histories;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * SQLiteから指定日付から指定日付までの最新の商品履歴を得ます
	 * 
	 * @param start
	 *            end
	 * @return
	 * @throws SQLiteException
	 */
	public List<NoodleHistory> getNoodleHistories(Date start, Date end)
			throws SQLException {
		List<NoodleHistory> histories = new ArrayList<NoodleHistory>();
		String[] columns = { "jancode", "name", "boiltime", "measuretime" };
		String where = "measuretime >= ? and measuretime < ?";
		String[] whereArgs = {
				NoodleHistory.getSimpleDateFormat().format(start),
				NoodleHistory.getSimpleDateFormat().format(end) };
		String orderby = "measuretime desc";
		Cursor cursor = null;
		try {
			// 検索
			cursor = database.query(NOODLEHISTORYTABLENAME, columns, where,
					whereArgs, null, null, orderby);
			if (cursor == null) {
				throw new SQLException("cursor is null");
			}
			while (cursor.moveToNext()) {
				histories.add(createNoodleHistory(cursor));
			}
			return histories;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * SQLiteからすべての商品履歴を得ます
	 * 
	 * @return
	 * @throws SQLiteException
	 */
	public List<NoodleHistory> getNoodleHistories() throws SQLException {
		List<NoodleHistory> histories = new ArrayList<NoodleHistory>();
		String[] columns = { "jancode", "name", "boiltime", "measuretime" };
		String orderby = "measuretime desc";
		Cursor cursor = null;
		try {
			// 検索
			cursor = database.query(NOODLEHISTORYTABLENAME, columns, null,
					null, null, null, orderby);
			if (cursor == null) {
				throw new SQLException("cursor is null");
			}
			while (cursor.moveToNext()) {
				histories.add(createNoodleHistory(cursor));
			}
			return histories;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * SQLiteからJanCodeのLIKE検索で履歴を得ます
	 * 
	 * @param janCode
	 * @return
	 * @throws SQLException
	 */
	public List<NoodleHistory> getNoodleHistoriesLikeJanCode(String janCode)
			throws SQLException {
		List<NoodleHistory> histories = new ArrayList<NoodleHistory>();
		String[] columns = { "jancode", "name", "boiltime", "measuretime" };
		String where = "jancode LIKE ?";
		String[] whereArgs = { "%" + janCode + "%" };
		String orderby = "measuretime desc";
		Cursor cursor = null;
		try {
			// 検索
			cursor = database.query(NOODLEHISTORYTABLENAME, columns, where,
					whereArgs, null, null, orderby);
			if (cursor == null) {
				throw new SQLException("cursor is null");
			}
			while (cursor.moveToNext()) {
				histories.add(createNoodleHistory(cursor));
			}
			return histories;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * SQLiteから名称のLIKE検索で履歴を得ます
	 * 
	 * @param name
	 * @return
	 * @throws SQLException
	 */
	public List<NoodleHistory> getNoodleHistoriesLikeName(String name)
			throws SQLException {
		List<NoodleHistory> histories = new ArrayList<NoodleHistory>();
		String[] columns = { "jancode", "name", "boiltime", "measuretime" };
		String where = "name LIKE ?";
		String[] whereArgs = { "%" + name + "%" };
		String orderby = "measuretime desc";
		Cursor cursor = null;
		try {
			// 検索
			cursor = database.query(NOODLEHISTORYTABLENAME, columns, where,
					whereArgs, null, null, orderby);
			if (cursor == null) {
				throw new SQLException("cursor is null");
			}
			while (cursor.moveToNext()) {
				histories.add(createNoodleHistory(cursor));
			}
			return histories;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * カーソルから履歴を作成する
	 * 
	 * @param cursor
	 * @return
	 */
	private NoodleHistory createNoodleHistory(Cursor cursor)
			throws SQLException {
		try {
			String jancode = cursor.getString(cursor.getColumnIndex("jancode"));
			String measuretimeString = cursor.getString(cursor
					.getColumnIndex("measuretime"));
			Date measuretime = null;
			try {
				measuretime = NoodleHistory.getSimpleDateFormat().parse(
						measuretimeString);
			} catch (ParseException e) {
				// 絶対にExceptionは出ないがもしでた場合は履歴はないものとする
				e.printStackTrace();
				return null;
			}
			int boiltime = cursor.getInt(cursor.getColumnIndex("boiltime"));
			NoodleMaster noodleMaster = getNoodleMaster(jancode);
			if (noodleMaster == null) {
				// 該当する商品マスタがないので履歴に表示しない
				return null;
			}
			return new NoodleHistory(noodleMaster, boiltime, measuretime);
		} catch (SQLiteException ex) {
			throw new SQLException(ExceptionToStringConverter.convert(ex));
		}
	}

	/**
	 * SQLiteに商品マスタを追加します
	 * 
	 * @param noodleMaster
	 * @throws SQLiteException
	 */
	public void createNoodleMater(NoodleMaster noodleMaster)
			throws SQLException {
		try {
			ContentValues contentValues = new ContentValues();
			contentValues.put("jancode", noodleMaster.getJanCode());
			contentValues.put("name", noodleMaster.getName());
			contentValues.put("boiltime", noodleMaster.getTimerLimit());
			contentValues.put("image", noodleMaster.getImageFileName());
			long ret = database.insert(NOODLEMASTERTABLENAME, null,
					contentValues);
			if (ret < 0) {
				throw new SQLException("insert return value = " + ret);
			}
		} catch (SQLiteException ex) {
			throw new SQLException(ExceptionToStringConverter.convert(ex));
		}
	}

	/**
	 * SQLiteの商品マスタ情報を更新します
	 * 
	 * @param noodleMaster
	 * @throws SQLException
	 */
	public void updateNoodleMater(NoodleMaster noodleMaster)
			throws SQLException {
		try {
			String where = "jancode";
			String[] whereArgs = { noodleMaster.getJanCode() };
			ContentValues contentValues = new ContentValues();
			contentValues.put("jancode", noodleMaster.getJanCode());
			contentValues.put("name", noodleMaster.getName());
			contentValues.put("boiltime", noodleMaster.getTimerLimit());
			contentValues.put("image", noodleMaster.getImageFileName());
			long ret = database.update(NOODLEMASTERTABLENAME, contentValues,
					where, whereArgs);
			if (ret < 0) {
				throw new SQLException("insert return value = " + ret);
			}
		} catch (SQLiteException ex) {
			throw new SQLException(ExceptionToStringConverter.convert(ex));
		}
	}

	/**
	 * 引数の商品マスタと計測時間、計測日時をもとに履歴を作成します
	 * 
	 * @param noodleMaster
	 * @param boilTime
	 * @param measureTime
	 * @throws SQLException
	 */
	public void createNoodleHistory(NoodleMaster noodleMaster, int boilTime,
			Date measureTime) throws SQLException {
		try {
			ContentValues contentValues = new ContentValues();
			contentValues.put("jancode", noodleMaster.getJanCode());
			contentValues.put("name", noodleMaster.getName());
			// 実際に計測した時間を入力します
			contentValues.put("boiltime", boilTime);
			contentValues.put("measuretime", NoodleHistory
					.getSimpleDateFormat().format(measureTime));
			long ret = database.insert(NOODLEHISTORYTABLENAME, null,
					contentValues);
			if (ret < 0) {
				throw new SQLException("insert return value = " + ret);
			}
		} catch (SQLiteException ex) {
			throw new SQLException(ExceptionToStringConverter.convert(ex));
		}
	}

	/**
	 * DataBaseOpenHelper
	 * 
	 * @author hide
	 */
	private class DataBaseOpenHelper extends SQLiteOpenHelper {
		/**
		 * コンストラクタ
		 * 
		 * @param context
		 * @param factory
		 * @param version
		 */
		public DataBaseOpenHelper(Context context) {
			super(context, "RamenTimer.db", null, DB_VERSION);
		}

		/**
		 * データベースが新規に作成された
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			// NoodleMasterテーブルを作成する
			db.execSQL(CREATE_NOODLEMASTER_TABLE);
			// NoodleHistoryテーブルを作成する
			db.execSQL(CREATE_NOODLEHISTORY_TABLE);
		}

		/**
		 * 存在するデータベースと定義しているバージョンが異なる
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// テーブルを削除する
			StringBuilder builder = new StringBuilder("DROP TABLE ");
			builder.append(NOODLEMASTERTABLENAME);
			db.execSQL(builder.toString());
			builder = new StringBuilder("DROP TABLE ");
			builder.append(NOODLEHISTORYTABLENAME);
			db.execSQL(builder.toString());
			// テーブルを定義しなおす
			onCreate(db);
		}

	}
}
