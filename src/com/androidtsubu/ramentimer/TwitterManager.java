package com.androidtsubu.ramentimer;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * twitter関連の色々
 * 
 * @author hide
 * 
 */
public class TwitterManager {
	private static TwitterManager manager = null;

	/** Twitter認証用のKEY */
	public static final String CONSUMER_KEY = "6OM64XL6PgVkRs4JB7Og";
	/** Twitter認証用のSECRET */
	public static final String CONSUMER_SECRET = "HlqfFX863uU9myNpQNzlEyZjPYVbkKy2XVNW0XUpHok";
	private static final String OAUTH_TOKEN = "oauth_token";
	private static final String OAUTH_TOKEN_SECRET = "oauth_token_secret";
	private static final String TWITTERID = "twitterId";
	private static final String POSTTWITTER = "postTwitter";
	private static final long NOAUTHORIZATION_ID = -99999999;

	/**
	 * 認証が完了しているかどうかを返す
	 * 
	 * @return
	 */
	public boolean isAuthorization(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String token = pref.getString(OAUTH_TOKEN, "");
		String secret = pref.getString(OAUTH_TOKEN_SECRET, "");
		long twitterId = pref.getLong(TWITTERID, NOAUTHORIZATION_ID);
		// TOKENもTOKEN_SECRETも文字列が入っていれば認証しているとみなす
		return !token.equals("") && !secret.equals("")
				&& twitterId != NOAUTHORIZATION_ID;
	}

	/**
	 * 認証情報をSaveする
	 * 
	 * @param context
	 * @param token
	 * @param tokenSecret
	 * @throws TwitterException
	 * @throws IllegalStateException
	 */
	public void saveAuthorization(Context context, String token,
			String tokenSecret) throws IllegalStateException, TwitterException {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = pref.edit();
		editor.putString(OAUTH_TOKEN, token);
		editor.putString(OAUTH_TOKEN_SECRET, tokenSecret);
		// ここで一度commitしておかないとgetTwitterIdFromTwitterが失敗する
		editor.commit();
		// Twitterから自分のTwitterIdをもらってから保存する
		long twitterId = getTwitterIdFromTwitter(context);
		editor.putLong(TWITTERID, twitterId);
		editor.commit();
	}

	/**
	 * twitterにpostする
	 * 
	 * @param context
	 * @param master
	 * @throws IllegalStateException
	 * @throws TwitterException
	 */
	public void post(Context context, NoodleMaster master)
			throws IllegalStateException, TwitterException {
		Twitter twitter = getTwitter(context);
		StringBuilder buf = new StringBuilder(".@");
		buf.append(twitter.getScreenName());
		buf.append(" ");
		buf.append(context.getString(R.string.twitter_post_message1));
		buf.append(" ");
		buf.append(master.getName());
		buf.append(" ");
		buf.append(context.getString(R.string.twitter_post_message2));
		twitter.updateStatus(buf.toString());
	}

	private Twitter getTwitter(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String oAuthToken = pref.getString(OAUTH_TOKEN, "");
		String oAuthSecret = pref.getString(OAUTH_TOKEN_SECRET, "");
		AccessToken accessToken = new AccessToken(oAuthToken, oAuthSecret);
		TwitterFactory factory = new TwitterFactory();
		Twitter twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
		twitter.setOAuthAccessToken(accessToken);
		return twitter;
	}

	/**
	 * Instanceを返す
	 * 
	 * @return
	 */
	public static TwitterManager getInstance() {
		if (manager == null) {
			manager = new TwitterManager();
		}
		return manager;
	}

	/**
	 * TwitterからTwitterIDを返してもらう
	 * 
	 * @param context
	 * @return
	 * @throws IllegalStateException
	 * @throws TwitterException
	 */
	private long getTwitterIdFromTwitter(Context context)
			throws IllegalStateException, TwitterException {
		Twitter twitter = getTwitter(context);
		return twitter.getId();
	}

	/**
	 * twitterIDを返す
	 * 
	 * @param context
	 * @return
	 */
	public long getTwitterId(Context context) throws TwitterException {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		long twitterId = pref.getLong(TWITTERID, NOAUTHORIZATION_ID);
		if (twitterId == NOAUTHORIZATION_ID) {
			throw new TwitterException("No Authorization TwitterID");
		}
		return twitterId;
	}

	/**
	 * Twitterに投稿するかどうかを返す
	 * 
	 * @param context
	 * @return
	 */
	public boolean isPostTwitter(Context context) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		int post = pref.getInt(POSTTWITTER, 0);
		return post != 0;
	}

	/**
	 * Twitterに投稿するかどうかを設定する
	 * 
	 * @param context
	 * @param post
	 */
	public void setPostTwitter(Context context, boolean post) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = pref.edit();
		editor.putInt(POSTTWITTER, post ? 1 : 0);
		editor.commit();
	}
}
