package com.androidtsubu.ramentimer;

/**
 * リクエストコードの種類 呼び出し元の分岐を管理 足りないものは各自で足してくれると嬉しい
 * 
 * @author leibun
 * 
 */
public enum RequestCode {
	DASHBORAD2READER, // ダッシュボードからリーダー
	DASHBORAD2HISTORY, // ダッシュボードから履歴
	DASHBOARD2FAVORITE, // ダッシュボードからお気に入り
	DASHBORAD2TIMER, // ダッシュボードからタイマー
	READER2TIMER, // リーダーからタイマー
	READER2CREATE, // リーダーから登録
	CREATE2TIMER, // 登録からタイマー
	TIMER2CREATE, // タイマーから登録
	TIMER2SEARCH, // タイマーから検索
	HISTORY2TIMER, // 履歴からタイマー
	FAVORITE2TIMER, // お気に入りからタイマー
	DASHBOARD2CREATE, // ダッシュボードから登録
	DASHBOARD2RAMENSEARCH, // ダッシュボードから商品検索
	RAMENSEARCH2READER, // 手入力JANコードからリーダー
	TIMER2AUTHORIZATION, // タイマーからtwitter認証
	FAVORITE2SEARCH, // お気に入りから検索
	HISTORY2SEARCH, // 履歴から検索
	// ActionBar用
	ACTION_READER, // アクションバーの読込ボタンが押された場合
	ACTION_HISTORY, // アクションバーの履歴ボタンが押された場合
	ACTION_SEARCH, // アクションバーの検索ボタンが押された場合
	ACTION＿TIMER; // アクションバーのタイマーボタンが押された場合

	// Intent.putExtra()の第１引数に利用
	public static final String KEY_REQUEST_CODE = "REQUEST_CODE";

};