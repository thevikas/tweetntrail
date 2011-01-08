/*
 * HttpUtil.java
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

package com.substanceofcode.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import com.java4ever.apime.io.GZIP;

import com.substanceofcode.tracker.TweetNTrailMidlet;
import com.substanceofcode.tracker.controller.Controller;
import com.sugree.twitter.TwitterConsumer;
import com.sugree.twitter.JSONTwitterParser;
import com.sugree.infrastructure.Device;

/**
 *
 * @author Tommi Laukkanen
 */
public class HttpUtil extends HttpAbstractUtil {

    /** Total bytes transfered */
    private static long totalBytes = 0;
	private static String userAgent = "curl/7.18.0 (i486-pc-linux-gnu) libcurl/7.18.0 OpenSSL/0.9.8g zlib/1.2.3.3 libidn/1.1 gzip";
	private static boolean alternateAuthen = false;
	private static boolean optimizeBandwidth = true;
	private static boolean forceNoHost = false;
	private static boolean gzip = true;
	private static String contentType = "application/x-www-form-urlencoded";
	private static Controller controller = null;
	private static TwitterConsumer oauth = null;
	private static String oauthEchoUrl = null;
	private static String oauthEchoMethod = null;
    
    /** Creates a new instance of HttpUtil */
    public HttpUtil() {
    }

	public static void setTwitterController(Controller controller) {
		HttpUtil.controller = controller;
	}

	public static void setOAuth(TwitterConsumer oauth) {
		HttpUtil.oauth = oauth;
	}

	public static void setOAuthEcho(String url, String method) {
		HttpUtil.oauthEchoUrl = url;
		HttpUtil.oauthEchoMethod = method;
	}

	public static void setUserAgent(String userAgent) {
		HttpUtil.userAgent = userAgent+" gzip";
	}

	public static void setAlternateAuthentication(boolean flag) {
		HttpUtil.alternateAuthen = flag;
	}

	public static void setOptimizeBandwidth(boolean flag) {
		HttpUtil.optimizeBandwidth = flag;
	}

	public static void setForceNoHost(boolean flag) {
		HttpUtil.forceNoHost = flag;
	}

	public static void setGzip(boolean flag) {
		HttpUtil.gzip = flag;
	}

	public static void setContentType(String contentType) {
		if (contentType == null) {
			HttpUtil.contentType = "application/x-www-form-urlencoded";
		} else {
			HttpUtil.contentType = contentType;
		}
	}

    public static String getLocation(String url, String query) throws IOException, Exception {
		if (query.length() > 0) {
			url += "?"+query;
		}

        int status = -1;
        String message = null;
        HttpConnection con = null;
		final String platform = Device.getPlatform();
		try {
			//Log.setState("connecting");
			con = (HttpConnection)Connector.open(url);
			//Log.setState("connected");
			Log.verbose("opened connection to "+url);
			con.setRequestMethod(HttpConnection.GET);
			//con.setRequestProperty("User-Agent", HttpUtil.userAgent);
			if (!platform.equals(Device.PLATFORM_NOKIA)) {
				con.setRequestProperty("Host", con.getHost()+":"+con.getPort());
			}

			//Log.setState("sending request");
			status = con.getResponseCode();
			message = con.getResponseMessage();
			//Log.setState("received response");
			//Log.debug("user-agent "+con.getRequestProperty("User-Agent"));
			Log.verbose("response code "+status+" "+message);
			String newUrl = con.getHeaderField("Location");
			if (newUrl != null) {
				url = newUrl;
			}
			Log.info(url);
		} catch (IOException ioe) {
            throw ioe;
		}
		return url;
    }

    public static String doPost(String url, String query) throws IOException, Exception {
        return doRequest(url, prepareQuery(query), HttpConnection.POST);
    }

    public static String doPost(String url, byte[] query) throws IOException, Exception {
        return doRequest(url, query, HttpConnection.POST);
    }

    public static String doGet(String url, String query) throws IOException, Exception {
		String fullUrl = url;
		query = prepareQuery(query);
		if (query.length() > 0) {
			fullUrl += "?"+query;
		}
        return doRequest(fullUrl, "", HttpConnection.GET);
    }

    public static String doRequest(String url, String query, String requestMethod) throws IOException, Exception {
		return doRequest(url, query.getBytes(), requestMethod);
	}

    public static String doRequest(String url, byte[] query, String requestMethod) throws IOException, Exception {
		String response = "";
        int status = -1;
        String message = null;
		int depth = 0;
		boolean redirected = false;
        String auth = null;
        InputStream is = null;
        OutputStream os = null;
        HttpConnection con = null;
		final String platform = Device.getPlatform();
		long timeOffset = new Date().getTime();

        while (con == null) {
			Log.setState("connecting");
            con = (HttpConnection)Connector.open(url);
			Log.setState("connected");
            Log.verbose("opened connection to "+url);
            con.setRequestMethod(requestMethod);

			String body = "";
			if ("application/x-www-form-urlencoded".equals(contentType)) {
				body = new String(query, "ISO-8859-1");
			}

			if (oauth == null || !oauth.sign(con, oauthEchoUrl, oauthEchoMethod, body)) {
				Log.verbose("authorization: basic");
				if (!alternateAuthen && username != null && password != null && username.length() > 0) {
					String userPass;
					Base64 b64 = new Base64();
					userPass = username + ":" + password;
					userPass = b64.encode(userPass.getBytes());
					con.setRequestProperty("Authorization", "Basic " + userPass);
				}
			}
            con.setRequestProperty("User-Agent", HttpUtil.userAgent);
			if (!platform.equals(Device.PLATFORM_NOKIA) &&
				!platform.equals(Device.PLATFORM_WELLCOM) &&
				!forceNoHost) {
				String host = con.getHost();
				if (con.getPort() != 80) {
					host += ":"+con.getPort();
				}
	            con.setRequestProperty("Host", host);
			}
			if (gzip) {
				con.setRequestProperty("Accept-Encoding", "gzip");
			}

            if(query.length > 0) {
            	con.setRequestProperty("Content-Type", contentType);
	            con.setRequestProperty("Content-Length", "" + query.length);
            	os = con.openOutputStream();
				Log.verbose("opened output stream");
                //os.write(query);
				int n = query.length;
				Log.info("Size: "+n);
                for(int i = 0; i < n; i++) {
					os.write(query[i]);
					if (i%500 == 0 || i == n-1) {
						Log.setProgress(i*100/n/2);
					}
                }
				//Log.verbose("sent query "+query);
	            os.close();
	            os = null;
				Log.verbose("closed output stream");
            }
			Log.setProgress(50);

			Log.setState("sending request");
            status = con.getResponseCode();
            message = con.getResponseMessage();
			timeOffset = con.getDate()-timeOffset+new Date().getTime();
			Log.setState("received response");
			Log.info(status+" "+message);
            Log.debug("host "+con.getRequestProperty("Host"));
            Log.debug("user-agent "+con.getRequestProperty("User-Agent"));
            Log.verbose("response code "+status+" "+message);
            switch (status) {
				case HttpConnection.HTTP_OK:
				case HttpConnection.HTTP_NOT_MODIFIED:
				case HttpConnection.HTTP_BAD_REQUEST:
					break;
				case HttpConnection.HTTP_MOVED_TEMP:
				case HttpConnection.HTTP_TEMP_REDIRECT:
				case HttpConnection.HTTP_MOVED_PERM:
					if (depth > 2) {
						throw new IOException("Too many redirect");
					}
					redirected = true;
					url = con.getHeaderField("location");
        			Log.verbose("redirected to "+url);
					con.close();
					con = null;
					Log.verbose("closed connection");
					depth++;
					break;
				case 100:
					throw new IOException("unexpected 100 Continue");
				default:
					con.close();
					con = null;
					Log.verbose("closed connection");
					throw new IOException("Response status not OK:"+status+" "+message);
            }
        }

        is = con.openInputStream();
		Log.setState("receiving data");
        Log.verbose("opened input stream");
		if (!redirected) {
			response = getUpdates(con, is, os);
		} else {
            try {
                if (con != null) {
                    con.close();
					Log.verbose("closed connection");
                }
                if (os != null) {
                    os.close();
					Log.verbose("closed output stream");
                }
                if (is != null) {
                    is.close();
					Log.verbose("closed input stream");
                }
            } catch (IOException ioe) {
                throw ioe;
            }
		}
		if (status == HttpConnection.HTTP_BAD_REQUEST) {
			System.out.println(response);
			throw new IOException("Response status not OK:"+status+" "+message+" "+JSONTwitterParser.parse400(response));
		}

		//wcontroller.setServerTimeOffset(new Date().getTime()-timeOffset);
		return response;
	}

    private static String getUpdates(HttpConnection con, InputStream is, OutputStream os)  throws IOException {
		Log.setProgress(0);
        StringBuffer stb = new StringBuffer();
        int ch = 0;
        try {
            int n = (int)con.getLength();
			Log.info("Size: "+n);
        	Log.verbose("reading response");
            if(n != -1) {
                for(int i = 0; i < n; i++) {
                    if((ch = is.read()) != -1) {
                        stb.append((char) ch);
                    }
					if (i%100 == 0 || i == n-1) {
						Log.setProgress(50+i*100/n/2);
					}
                }
            } else {
                while((ch = is.read()) != -1) {
                    n = is.available();
                    stb.append((char) ch);
                }
            }
			if ("gzip".equals(con.getEncoding())) {
				Log.verbose("encoding: gzip");
				byte[] decompressed = GZIP.inflate(stb.toString().getBytes("ISO-8859-1"));
				stb = new StringBuffer(new String(decompressed, "ISO-8859-1"));
				Log.verbose("size: "+decompressed.length+" "+stb.length());
			}
			Log.setProgress(100);
        } catch (UnsupportedEncodingException e) {
            Log.verbose("read response: unknown encoding");
        } catch (IOException ioe) {
            Log.verbose("read response: "+ioe.getMessage());
            throw ioe;
        } finally {
            try {
                if (os != null) {
                    os.close();
					Log.verbose("closed output stream");
                }
                if (is != null) {
                    is.close();
					Log.verbose("closed input stream");
                }
                if (con != null) {
                    con.close();
					Log.verbose("closed connection");
                }
            } catch (IOException ioe) {
                throw ioe;
            }
        }
        return stb.toString();
    }

	private static String prepareQuery(String query) {
		if (alternateAuthen && username != null && password != null && username.length() > 0) {
			String userPass;
			Base64 b64 = new Base64();
			userPass = username + ":" + password;
			userPass = b64.encode(userPass.getBytes());
			if (query.length() > 0) {
				query += "&";
			}
			query += "__token__="+StringUtil.urlEncode(userPass);
		}
		return query;
	}
}
