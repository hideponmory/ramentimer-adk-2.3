package com.androidtsubu.ramentimer;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * twitter認証画面
 * 
 * @author hide
 */
public class AuthorizationActivity extends Activity {
	private static final String CALLBACK_URL = "myapp://myApp";
	private static final String PREFERENCE_NAME = "ramentimer";
	private Twitter twitter;
	private RequestToken requestToken;
	private String oauthToken;
	private String oauthVerifier;
	private Handler handler = new Handler();
	private Exception e;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_authorization);
		// Twitterに接続する
		try {
			// twitterに接続する
			connectTwitter();
			WebView webView = (WebView) findViewById(R.id.webViewAuthorization);
			webView.setWebViewClient(new WebViewClient() {
				/**
				 * ページが終了したよ
				 */
				@Override
				public void onPageFinished(WebView view, String url) {
					if (url != null && url.startsWith(CALLBACK_URL)) {
						String[] urlParameters = url.split("\\?")[1].split("&");
						if (urlParameters.length <= 1) {
							// キャンセルしてアプリに戻るをクリックされた
							setResult(RESULT_CANCELED);
							finish();
							return;
						}

						// OathTokenを得る
						if (urlParameters[0].startsWith("oauth_token")) {
							oauthToken = urlParameters[0].split("=")[1];
						} else if (urlParameters[1].startsWith("oauth_token")) {
							oauthToken = urlParameters[1].split("=")[1];
						}

						// OauthVeriferを得る
						if (urlParameters[0].startsWith("oauth_verifier")) {
							oauthVerifier = urlParameters[0].split("=")[1];
						} else if (urlParameters[1]
								.startsWith("oauth_verifier")) {
							oauthVerifier = urlParameters[1].split("=")[1];
						}
						saveTwitterConnectSetting();
						return;

					}
					super.onPageFinished(view, url);
				}
			});
			// 認証ページを開く
			webView.loadUrl(requestToken.getAuthenticationURL());
		} catch (TwitterException _e) {
			this.e = _e;
			Log.e(AuthorizationActivity.class.getName(), e.getMessage());
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(AuthorizationActivity.this, e.getMessage(),
							Toast.LENGTH_LONG).show();
					finish();
				}
			}, 100);
		}

	}

	// Twitter連携
	private void connectTwitter() throws TwitterException {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.setOAuthConsumerKey(TwitterManager.CONSUMER_KEY);
		builder.setOAuthConsumerSecret(TwitterManager.CONSUMER_SECRET);
		// 認証インスタンスをもらう
		twitter = new TwitterFactory().getInstance(new OAuthAuthorization(
				builder.build()));
		// RequestTokenをもらう
		requestToken = twitter.getOAuthRequestToken(CALLBACK_URL);
	}

	private void saveTwitterConnectSetting() {

		AccessToken accessToken = null;

		try {
			// RequestTokenとOAuthVerifierからaccessTokenを得る
			accessToken = twitter.getOAuthAccessToken(requestToken,
					oauthVerifier);
			// 連携状態とトークンの書き込み
			TwitterManager.getInstance().saveAuthorization(this,
					accessToken.getToken(), accessToken.getTokenSecret());
			setResult(Activity.RESULT_OK);
			// 設定おしまい。
			finish();
		} catch (TwitterException _e) {
			this.e = _e;
			Log.e(AuthorizationActivity.class.getName(), e.getMessage());
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(AuthorizationActivity.this, e.getMessage(),
							Toast.LENGTH_LONG).show();
					finish();
				}
			}, 100);
		}

	}

}
