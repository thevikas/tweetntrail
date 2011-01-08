package com.sugree.twitter;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.rms.RecordStoreException;

import org.json.me.JSONException;

import net.oauth.j2me.BadTokenStateException;
import net.oauth.j2me.Consumer;
import net.oauth.j2me.ConsumerConfig;
import net.oauth.j2me.OAuthMessage;
import net.oauth.j2me.OAuthBadDataException;
import net.oauth.j2me.OAuthServiceProviderException;
import net.oauth.j2me.token.AccessToken;
import net.oauth.j2me.token.RequestToken;
import net.oauth.j2me.Util;

import com.substanceofcode.twitter.Settings;
import com.substanceofcode.twitter.TwitterApi;
import com.substanceofcode.util.Log;
import com.substanceofcode.gps.GpsPosition;
import com.sugree.twitter.tasks.UpdateStatusTask;

//vikas
import com.substanceofcode.tracker.model.RecorderSettings;
import com.sugree.twitter.views.WaitScreen;
import com.substanceofcode.tracker.view.Logger;

public class TwitterConsumer extends Consumer {
	public static final String REQUEST_TOKEN_URL = "https://api.twitter.com/oauth/request_token";
	public static final String ACCESS_TOKEN_URL = "https://api.twitter.com/oauth/access_token";
	public static final String AUTHORIZE_URL = "https://api.twitter.com/oauth/authorize";

	private RequestToken requestToken;
	private AccessToken accessToken;

	public TwitterConsumer(String key, String secret) {
		super(key, secret);
		setSignatureMethod("HMAC-SHA1");
	}

        public RequestToken fetchNewRequestToken() throws OAuthServiceProviderException {
		requestToken = getRequestToken(REQUEST_TOKEN_URL, "oob");
		return requestToken;
	}

        public String doUpdate(UpdateStatusTask waitScreen, String update,GpsPosition position) throws OAuthServiceProviderException
        {
                WaitScreen ws = waitScreen.controller.getWaitScreen();
                if(ws != null)
                    ws.setProgress(5);
                System.out.println("oauth update ");
                ConsumerConfig config = getConfig();
                String endpoint = "https://api.twitter.com/1/statuses/update.xml";

		OAuthMessage updateMessage = new OAuthMessage();
		updateMessage.setRequestURL(endpoint);
		updateMessage.setCallback("oob");
		updateMessage.setConsumerKey(config.getKey());
                AccessToken acctok = getAccessToken();
                if(acctok == null)
                {
                    System.out.println("Access token not found. sorry mate!");
                    waitScreen.controller.setCurrentScreen(waitScreen.controller.getTrailCanvas());
                    return "";
                }
                updateMessage.setToken(acctok.getToken());
                updateMessage.setTokenSecret(getAccessToken().getSecret());
                updateMessage.setRequestMethod("POST");
                updateMessage.setTweet(update);

                updateMessage.setPosition(position);
                updateMessage.createSignature(getSignatureMethod(), config.getSecret());

		String oauthHeader = updateMessage.convertToOAuthHeader();
		System.out.println("oauth update "+endpoint + " - " + oauthHeader);

		String responseString = null;
		try {
                        String content = "status=" + update;

                        if(position != null)
                        {
                            content += "&lat=" + String.valueOf(position.latitude);
                            content += "&long=" +String.valueOf(position.longitude);
                            content += "&display_coordinates=true";
                        }

                        if(ws != null)
                            ws.setProgress(30);

			responseString = Util.postViaHttpsConnection(waitScreen, endpoint,oauthHeader,content);
		} catch (IOException e) {
			System.out.println(e.toString());
			return null;
		}
                if(ws != null)
                    ws.setProgress(70);
                Logger.debug(responseString);
		System.out.println("oauth update response "+responseString);

                return responseString;
	}

        public RequestToken getRequestToken(String endpoint, String callback) throws OAuthServiceProviderException {
		RequestToken token = null;
		ConsumerConfig config = getConfig();

		OAuthMessage requestMessage = new OAuthMessage();
		requestMessage.setRequestURL(endpoint);
		requestMessage.setConsumerKey(config.getKey());
		requestMessage.setCallback(callback);
		requestMessage.createSignature(getSignatureMethod(), config.getSecret());

		String url = endpoint+"?"+requestMessage.convertToUrlParameters();
		System.out.println("oauth request "+url);

		String responseString = null;
		try {
			responseString = Util.getViaHttpsConnection(url);
		} catch (IOException e) {
			System.out.println(e.toString());
			return null;
		}
		System.out.println("oauth response "+responseString);

		OAuthMessage responseMessage = new OAuthMessage();
		try {
			responseMessage.parseResponseStringForToken(responseString);
		} catch (OAuthBadDataException e) {
			System.out.println(e.toString());
			return null;
		}
		token = new RequestToken(responseMessage.getToken(), responseMessage.getTokenSecret());
		return token;
	}

	
	public AccessToken fetchNewAccessToken(String pin) throws OAuthServiceProviderException, BadTokenStateException {
		if (requestToken == null) {
			throw new BadTokenStateException("No request token set");
		}
		System.out.println("oauth pin "+pin);
		accessToken = getAccessToken(ACCESS_TOKEN_URL, pin, requestToken);
		requestToken = null;
                System.out.println("returning access token");
		return accessToken;
	}

	public boolean sign(HttpConnection con, String body) throws IOException {
		if (accessToken == null) {
			return false;
		}
		Log.verbose("authorization: oauth");
		sign(con, body, accessToken);
		return true;
	}

	public boolean sign(HttpConnection con, String url, String method, String body) throws IOException {
		if (accessToken == null) {
			return false;
		}
		if (url == null) {
			Log.verbose("authorization: oauth");
			sign(con, body, accessToken);
		} else {
			Log.verbose("authorization: oauth echo");
			String cred = signEcho(url, method, body);
			con.setRequestProperty("X-Auth-Service-Provider", url);
			con.setRequestProperty("X-Verify-Credentials-Authorization", cred);
		}
		return true;
	}

	public String signEcho(String url, String method, String body) throws IOException {
		HttpConnection con = (HttpConnection)Connector.open(url);
		con.setRequestMethod(method);
		sign(con, body);
		return con.getRequestProperty("Authorization");
	}

	public RequestToken getRequestToken() {
		return requestToken;
	}

	public void loadRequestToken(RecorderSettings settings) {
                //vikas
		/*requestToken = new RequestToken(
				settings.getStringProperty(Settings.OAUTH_REQUEST_TOKEN, ""),
				settings.getStringProperty(Settings.OAUTH_REQUEST_SECRET, ""));*/
                requestToken = new RequestToken(
				settings.getStringProperty(Settings.OAUTH_REQUEST_TOKEN, ""),
				settings.getStringProperty(Settings.OAUTH_REQUEST_SECRET, ""));
	}

	public void saveRequestToken(RecorderSettings settings) throws IOException, RecordStoreException, JSONException {
		if (requestToken != null) {
			settings.settings.setStringProperty(Settings.OAUTH_REQUEST_TOKEN, requestToken.getToken());
			settings.settings.setStringProperty(Settings.OAUTH_REQUEST_SECRET, requestToken.getSecret());
		} else {
			settings.settings.setStringProperty(Settings.OAUTH_REQUEST_TOKEN, "");
			settings.settings.setStringProperty(Settings.OAUTH_REQUEST_SECRET, "");
		}
		settings.settings.save(false);
	}

	public AccessToken getAccessToken() {
		return accessToken;
	}

	public void loadAccessToken(RecorderSettings settings) {
                System.out.println("loading access token");
		accessToken = new AccessToken(
				settings.getStringProperty(Settings.OAUTH_ACCESS_TOKEN, ""),
				settings.getStringProperty(Settings.OAUTH_ACCESS_SECRET, ""));
		if ("".equals(accessToken.getToken()) || "".equals(accessToken.getSecret())) {
			accessToken = null;
		}
	}

	public void saveAccessToken(RecorderSettings settings) throws IOException, RecordStoreException, JSONException {
		if (accessToken != null) {
			settings.settings.setStringProperty(Settings.OAUTH_ACCESS_TOKEN, accessToken.getToken());
			settings.settings.setStringProperty(Settings.OAUTH_ACCESS_SECRET, accessToken.getSecret());
			settings.settings.setBooleanProperty(Settings.OAUTH_AUTHORIZED, true);
			settings.settings.setStringProperty(Settings.GATEWAY, "http://api.twitter.com/");
		} else {
			settings.settings.setStringProperty(Settings.OAUTH_ACCESS_TOKEN, "");
			settings.settings.setStringProperty(Settings.OAUTH_ACCESS_SECRET, "");
			settings.settings.setBooleanProperty(Settings.OAUTH_AUTHORIZED, false);
		}
		settings.settings.save(false);
	}

        public boolean isAuthorized() {
		return accessToken != null;
	}

	public String getAuthorizeUrl() {
		return AUTHORIZE_URL+"?oauth_token="+requestToken.getToken();
	}
}
