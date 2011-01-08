/*
 * TwitterApi.java
 *
 * Copyright (C) 2005-2008 Tommi Laukkanen
 * http://www.substanceofcode.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.substanceofcode.twitter;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.Vector;

import javax.microedition.io.HttpConnection;

import org.json.me.JSONObject;

import com.substanceofcode.twitter.model.Status;
import com.substanceofcode.util.HttpUtil;
import com.substanceofcode.util.Log;
import com.substanceofcode.util.StringUtil;

import com.sugree.twitter.PrivateData;
import com.substanceofcode.tracker.controller.Controller;
import com.sugree.twitter.TwitterConsumer;
import com.sugree.twitter.JSONTwitterParser;
import com.sugree.twitter.TwitterException;
import com.sugree.utils.MultiPartFormOutputStream;

/**
 * TwitterApi
 *
 * @author Tommi Laukkanen (tlaukkanen at gmail dot com)
 */
public class TwitterApi {

	private String gateway;
	private String pictureGateway;
	private String source;
	private int count;
    private String username;
    private String password;

    public static final String DEFAULT_GATEWAY = "http://nest.onedd.net/text/";
    public static final String DEFAULT_PICTURE_GATEWAY = "http://nest.onedd.net/text/twitpic/";
    private static final String PUBLIC_TIMELINE_URL = "statuses/public_timeline.json";
    private static final String HOME_TIMELINE_URL = "statuses/home_timeline.json";
    private static final String FRIENDS_TIMELINE_URL = "statuses/friends_timeline.json";
    private static final String USER_TIMELINE_URL = "statuses/user_timeline.json";
    private static final String MENTIONS_TIMELINE_URL = "statuses/mentions.json";
    private static final String STATUS_UPDATE_URL = "statuses/update.json";
	private static final String DIRECT_MESSAGES_URL = "direct_messages.json";
	private static final String FAVORITES_URL = "favorites.json";
	private static final String FAVORITES_CREATE_URL = "favorites/create/%d.json";
	private static final String FAVORITES_DESTROY_URL = "favorites/destroy/%d.json";
	private static final String TEST_URL = "help/test.json";
	private static final String SCHEDULE_DOWNTIME_URL = "help/schedule_downtime.json";
	private static final String PICTURE_POST_URL = "";
	private static final String GLM_CELL_URL = "glm/cell";
	private static final String OAUTH_ECHO_JSON = "https://api.twitter.com/1/account/verify_credentials.json";
	private static final String OAUTH_ECHO_XML = "https://api.twitter.com/1/account/verify_credentials.xml";

    /** Creates a new instance of TwitterApi */
    public TwitterApi(String source) {
		this.source = source;
		this.gateway = "http://twitter.com/";
		this.pictureGateway = "http://twitpic.com/";
		this.count = 0;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public void setPictureGateway(String pictureGateway) {
        this.pictureGateway = pictureGateway;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setCount(int count) {
		this.count = count;
    }

    public void setOAuth(TwitterConsumer oauth) {
		HttpUtil.setOAuth(oauth);
    }

    public void setAlternateAuthentication(boolean flag) {
		HttpUtil.setAlternateAuthentication(flag);
    }

    public void setOptimizeBandwidth(boolean flag) {
		HttpUtil.setOptimizeBandwidth(flag);
    }

    public void setForceNoHost(boolean flag) {
		HttpUtil.setForceNoHost(flag);
    }

    public void setGzip(boolean flag) {
		HttpUtil.setGzip(flag);
    }

    /**
     * Request public timeline from Twitter API.
     * @return Vector containing StatusEntry items.
     */
    public Vector requestPublicTimeline(String sinceId) throws TwitterException {
		if (sinceId != null && sinceId.length() > 0) {
			sinceId = "since_id="+StringUtil.urlEncode(sinceId);
		}
        HttpUtil.setBasicAuthentication("", "");
        return requestTimeline(gateway+PUBLIC_TIMELINE_URL, "");
    }    
    
    /**
     * Request home timeline from Twitter API.
     * @return Vector containing StatusEntry items.
     */
    public Vector requestHomeTimeline(String sinceId) throws TwitterException {
		if (sinceId != null && sinceId.length() > 0) {
			sinceId = "since_id="+StringUtil.urlEncode(sinceId);
		}
	    HttpUtil.setBasicAuthentication(username, password);
        return requestTimeline(gateway+HOME_TIMELINE_URL, prepareParamCount(sinceId));
    }    

    /**
     * Request public timeline from Twitter API.
     * @return Vector containing StatusEntry items.
     */
    public Vector requestFriendsTimeline(String sinceId) throws TwitterException {
		if (sinceId != null && sinceId.length() > 0) {
			sinceId = "since_id="+StringUtil.urlEncode(sinceId);
		}
	    HttpUtil.setBasicAuthentication(username, password);
        return requestTimeline(gateway+FRIENDS_TIMELINE_URL, prepareParamCount(sinceId));
    }    

    /**
     * Request public timeline from Twitter API.
     * @return Vector containing StatusEntry items.
     */
    public Vector requestUserTimeline(String sinceId) throws TwitterException {
		if (sinceId != null && sinceId.length() > 0) {
			sinceId = "since_id="+StringUtil.urlEncode(sinceId);
		}
	    HttpUtil.setBasicAuthentication(username, password);
        return requestTimeline(gateway+USER_TIMELINE_URL, prepareParamCount(sinceId));
    }    

    /**
     * Request responses timeline from Twitter API.{
     * @return Vector containing StatusEntry items.
     */
    public Vector requestMentionsTimeline(String sinceId) throws TwitterException {
		if (sinceId != null && sinceId.length() > 0) {
			sinceId = "since_id="+StringUtil.urlEncode(sinceId);
		}
	    HttpUtil.setBasicAuthentication(username, password);
        return requestTimeline(gateway+MENTIONS_TIMELINE_URL, prepareParamCount(sinceId));
    }  
    
    /**
     * Request direct messages timeline from Twitter API.{
     * @return Vector containing StatusEntry items.
     */
    public Vector requestDirectMessagesTimeline(String sinceId) throws TwitterException {
		if (sinceId != null && sinceId.length() > 0) {
			sinceId = "since_id="+StringUtil.urlEncode(sinceId);
		}
        HttpUtil.setBasicAuthentication(username, password);
        Vector entries = new Vector();
        try {
            String response = HttpUtil.doGet(gateway+DIRECT_MESSAGES_URL, sinceId);
			if (response.length() > 0) {
	            entries = JSONTwitterParser.parseDirectMessages(response);
			}
        } catch (IOException ex) {
            ex.printStackTrace();
			throw new TwitterException("request "+ex);
        } catch (Exception ex) {
            ex.printStackTrace();
			throw new TwitterException("request "+ex);
        }
        return entries;        
    }  
    
    /**
     * Request favorites timeline from Twitter API.{
     * @return Vector containing StatusEntry items.
     */
    public Vector requestFavoritesTimeline() throws TwitterException {
	    HttpUtil.setBasicAuthentication(username, password);
        return requestTimeline(gateway+FAVORITES_URL, prepareParamCount(""));
    }  

	public Status createFavorite(String id) throws TwitterException {
	    HttpUtil.setBasicAuthentication(username, password);
        return requestObject(gateway+FAVORITES_CREATE_URL, id);
	}
    
	public Status destroyFavorite(String id) throws TwitterException {
	    HttpUtil.setBasicAuthentication(username, password);
        return requestObject(gateway+FAVORITES_DESTROY_URL, id);
	}
    
    public Status updateStatus(String status) throws TwitterException {
		return updateStatus(status, 0);
	}

    public Status updateStatus(String status, long replyTo) throws TwitterException {
		String response = "";
        try {
            String query = "status="+StringUtil.urlEncode(status);
			if (replyTo != 0) {
				query += "&in_reply_to_status_id="+replyTo;
			}
            HttpUtil.setBasicAuthentication(username, password);
            response = HttpUtil.doPost(gateway+STATUS_UPDATE_URL, prepareParam(query));
        } catch(Exception ex) {
			ex.printStackTrace();
            Log.error("Error while updating status: " + ex.getMessage());
			throw new TwitterException("update "+ex.toString());
        }
		return null;
        //return JSONTwitterParser.parseStatus(response);
    }

	/*public void postPicture(String status, byte[] picture, String mimeType) throws TwitterException {
		String pictureGateway = this.pictureGateway;
		String fileName = "jibjib.jpg";
		if (mimeType.indexOf("jpeg") >= 0 || mimeType.indexOf("jpeg") >= 0) {
			fileName = "jibjib.jpg";
		} else if (mimeType.indexOf("png") >= 0) {
			fileName = "jibjib.png";
		} else if (mimeType.indexOf("gif") >= 0) {
			fileName = "jibjib.gif";
		}

		byte[] bs = null;
		try {
			bs = status.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
		}

		try {
			String url = "";
			url = postTwitPic(mimeType, fileName, picture, bs);
        	HttpUtil.setContentType(null);
			HttpUtil.setOAuthEcho(null, null);
			updateStatus(status+" "+url, 0);
		} catch (Exception ex) {
			HttpUtil.setOAuthEcho(null, null);
            HttpUtil.setContentType(null);
			ex.printStackTrace();
            Log.error("Error while posting picture: " + ex.getMessage());
			throw new TwitterException("post "+ex.toString());
		}
		HttpUtil.setOAuthEcho(null, null);
        HttpUtil.setContentType(null);
	}*/

	/*protected String postTwitPic(String mimeType, String fileName, byte[] picture, byte[] message) throws Exception {
		String boundary = MultiPartFormOutputStream.createBoundary();
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		MultiPartFormOutputStream out = new MultiPartFormOutputStream(data, boundary);
		out.writeFile("media", mimeType, fileName, picture);
		if (pictureGateway.indexOf("twitpic") > 0) {
			out.writeField("key", PrivateData.TWITPIC_API_KEY);
		} else if (pictureGateway.indexOf("yfrog") > 0) {
			out.writeField("key", PrivateData.IMAGESHACK_API_KEY);
		}
		out.writeField("message", message);

		HttpUtil.setBasicAuthentication("", "");
		HttpUtil.setContentType(MultiPartFormOutputStream.getContentType(boundary));
		HttpUtil.setOAuthEcho(OAUTH_ECHO_JSON, HttpConnection.GET);
		String response = HttpUtil.doPost(pictureGateway+PICTURE_POST_URL, data.toByteArray());
		String url = "";
		try {
			JSONObject jo = new JSONObject(response);
			url = jo.getString("url");
		} catch (Exception e) {
			url = parseXML(response, "<mediaurl>", "</mediaurl>");
			if ("".equals(url)) {
				url = parseXML(response, "<url>", "</url>");
			}
		}
		return url;
	}*/

	public String requestTest() throws TwitterException {
		String result = "";
        HttpUtil.setBasicAuthentication("", "");
        try {
            String response = HttpUtil.doGet(gateway+TEST_URL, "");
			if (response.length() > 0) {
	            result = JSONTwitterParser.parseTest(response);
			}
        } catch (IOException ex) {
            ex.printStackTrace();
			throw new TwitterException("request "+ex);
        } catch (Exception ex) {
            ex.printStackTrace();
			throw new TwitterException("request "+ex);
        }
        return result;
	}

	public String requestScheduleDowntime() throws TwitterException {
		String result = "";
        HttpUtil.setBasicAuthentication("", "");
        try {
            String response = HttpUtil.doGet(gateway+SCHEDULE_DOWNTIME_URL, "");
			if (response.length() > 0) {
	            result = JSONTwitterParser.parseScheduleDowntime(response);
			}
        } catch (IOException ex) {
            ex.printStackTrace();
			throw new TwitterException("request "+ex);
        } catch (Exception ex) {
            ex.printStackTrace();
			throw new TwitterException("request "+ex);
        }
        return result;
	}

	public String requestLocationByCellID(int cid, int lac) throws IOException, Exception {
		HttpUtil.setBasicAuthentication("", "");
		String response = HttpUtil.doGet(gateway+GLM_CELL_URL+"/"+cid+"/"+lac, "");
		return response;
	}

	private Status requestObject(String url, String id) throws TwitterException {
		String response = "";
		Status status = null;
		try {
			url = StringUtil.replace(url, "%d", id);
	        HttpUtil.setBasicAuthentication(username, password);
		    response = HttpUtil.doPost(url, "");
	        status = JSONTwitterParser.parseStatus(response);
		} catch(Exception ex) {
            ex.printStackTrace();
			throw new TwitterException("request "+ex);
		}
		return status;
	}
    
    private Vector requestTimeline(String timelineUrl, String param) throws TwitterException {
        Vector entries = new Vector();
        try {
            String response = HttpUtil.doGet(timelineUrl, param);
			if (response.length() > 0) {
	            entries = JSONTwitterParser.parseStatuses(response);
			}
        } catch (IOException ex) {
            ex.printStackTrace();
			throw new TwitterException("request "+ex);
        } catch (Exception ex) {
            ex.printStackTrace();
			throw new TwitterException("request "+ex);
        }
        return entries;        
    }
    
    private String prepareParam(String param) {
		String newParam = "";
		if (param.length() > 0) {
			newParam = param+"&";
		}
		newParam += "source="+source;
		return newParam;
	}
    
    private String prepareParamCount(String param) {
		String newParam = prepareParam(param);
		if (count > 0) {
		    if (newParam.length() > 0) {
				newParam += "&";
			}
			newParam += "count="+count;
		}
		return newParam;
	}
    
	private String parseXML(String text, String prefix, String suffix) {
		String body = "";
		int i = text.indexOf(prefix);
		int j = text.indexOf(suffix);
		if (i >= 0 && j >= 0) {
			body = text.substring(i+prefix.length(), j);
		}
		return body;
	}
}
