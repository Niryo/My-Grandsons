package net.sumppen.whatsapi4j;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


import javax.ws.rs.core.MediaType;

import net.sumppen.whatsapi4j.tools.BinHex;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Java adaptation of PHP WhatsAPI by venomous0x
 * {@link https://github.com/venomous0x/WhatsAPI}
 * 
 * @author Kim Lindberg (kim@sumppen.net)
 *
 */
public class WhatsApi {

    private static final String RELEASE_TOKEN_CONST = "PdA2DJyKoUrwLw1Bg6EIhzh502dF9noR9uFCllGk";
    private static final String RELEASE_TIME = "1430860548912";
    private final int PORT = 443;                                      // The port of the WhatsApp server.
    private final int TIMEOUT_SEC = 2;                                  // The timeout for the connection with the WhatsApp servers.
    private final String WHATSAPP_CHECK_HOST = "v.whatsapp.net/v2/exist";  // The check credentials host.
    public static final String WHATSAPP_GROUP_SERVER = "g.us";                   // The Group server hostname
    private final String WHATSAPP_HOST = "c.whatsapp.net";                 // The hostname of the WhatsApp server.
    private final String WHATSAPP_REGISTER_HOST = "v.whatsapp.net/v2/register"; // The register code host.
    private final String WHATSAPP_REQUEST_HOST = "v.whatsapp.net/v2/code";      // The request code host.
    public static final String WHATSAPP_SERVER = "s.whatsapp.net";               // The hostname used to login/send messages.
    private final String WHATSAPP_DEVICE = "S40";                      // The device name.
    private final String WHATSAPP_VER = "2.12.81";                // The WhatsApp version.
    private final String WHATSAPP_USER_AGENT = "WhatsApp/2.12.81 S40Version/14.26 Device/Nokia302";// User agent used in request/registration code.
    private final String WHATSAPP_VER_CHECKER = "https://coderus.openrepos.net/whitesoft/whatsapp_version"; // Check WhatsApp version


//    private final Cache cache = new DiskBasedCache(new File("/cache"), 1024 * 1024); // 1MB cap
//    private final Network network = new BasicNetwork(new HurlStack());
//    private final RequestQueue requestQueue= new RequestQueue(cache, network);
    RequestQueue requestQueue;

	private final Logger log = LoggerFactory.getLogger(WhatsApi.class);
	private String identity;
	private final String name;
	private final String phoneNumber;
	private LoginStatus loginStatus;
	private Socket socket;
	private String password;

	private BinTreeNodeWriter writer;
	private byte[] challengeData;
	private BinTreeNodeReader reader;
	private KeyStream inputKey;
	private KeyStream outputKey;
	private List<String> serverReceivedId = new LinkedList<String>();
	private List<ProtocolNode> messageQueue = new LinkedList<ProtocolNode>();
	private String lastId;
	private List<ProtocolNode> outQueue = new LinkedList<ProtocolNode>();
	private EventManager eventManager = new LoggingEventManager();
	private int messageCounter = 0;
	private final List<Country> countries;
	private Map<String,Map<String,Object>> mediaQueue = new HashMap<String, Map<String,Object>>();
	private MediaInfo mediaFile;
	private JSONObject mediaInfo;
	private MessageProcessor processor = null;
//	private MessagePoller poller;

    public interface Callback{
        public void doJob(JSONObject response);
    }

	public WhatsApi(String username, String identity, String nickname, Context appContext) throws NoSuchAlgorithmException, WhatsAppException {
        this.requestQueue= Volley.newRequestQueue(appContext);
        this.requestQueue.start();
		writer = new BinTreeNodeWriter();
		reader = new BinTreeNodeReader();
		this.name = nickname;
		this.phoneNumber = username;
		try {
			if(!checkIdentity(identity)) {
				this.identity = buildIdentity(identity);
			} else {
				this.identity = identity;
			}
		} catch (UnsupportedEncodingException e) {
			throw new WhatsAppException(e);
		}
		this.loginStatus = LoginStatus.DISCONNECTED_STATUS;
		countries = readCountries();
	}

	/**
	 * Add message to the outgoing queue.
	 * 
	 * @param ProtocolNode
	 */
	public void addMsgOutQueue(ProtocolNode node) {
		outQueue.add(node);
	}

	/**
	 * Register account on WhatsApp using the provided code.
	 *
	 * @param String code
	 *   Numeric pin-code value provided on requestCode().
	 *
	 * @return object
	 *   An object with server response.
	 *   - status: Account status.
	 *   - login: Phone number with country code.
	 *   - pw: Account password.
	 *   - type: Type of account.
	 *   - expiration: Expiration date in UNIX TimeStamp.
	 *   - kind: Kind of account.
	 *   - price: Formatted price of account.
	 *   - cost: Decimal amount of account.
	 *   - currency: Currency price of account.
	 *   - price_expiration: Price expiration in UNIX TimeStamp.
	 * @throws WhatsAppException 
	 * @throws JSONException 
	 *
	 * @throws Exception
	 */
	public void codeRegister(String code, final Callback callback) throws WhatsAppException, JSONException {
		Map<String, String> phone;
		if ((phone = dissectPhone()) == null) {
			throw new WhatsAppException("The prived phone number is not valid.");
		}
		String countryCode = null;
		String langCode = null;
		if(countryCode == null) {
			if(phone.get("ISO3166") != null) {
				countryCode = phone.get("ISO3166");
			} else {
				countryCode = "US";
			}
		}
		if(langCode == null) {
			if(phone.get("ISO639") != null) {
				langCode = phone.get("ISO639");
			} else {
				langCode = "en";
			}
		}

		// Build the url.
		String host = "https://"+WHATSAPP_REGISTER_HOST;
		Map<String,String> query = new LinkedHashMap<String, String>();
		query.put("cc",phone.get("cc")); 
		query.put("in",phone.get("phone")); 
		query.put("lg",langCode); 
		query.put("lc", countryCode);
		query.put("id",(identity==null?"":identity));
		query.put("code", code);
		//		query.put("c", "cookie");

        getResponse(host, query, new Callback() {
            @Override
            public void doJob(JSONObject response) {

                try {
                    if (!response.getString("status").equals("ok")) {
                        eventManager().fireCodeRegisterFailed(phoneNumber, response.getString("status"), response.getString("reason"), "");//response.getString("retry_after"));
                        throw new WhatsAppException("An error occurred registering the registration code from WhatsApp.");
                    } else {
                        eventManager().fireCodeRegister(phoneNumber, response.getString("login"), response.getString("pw"), response.getString("type"), response.getString("expiration"),
                                response.getString("kind"), response.getString("price"), response.getString("cost"), response.getString("currency"), response.getString("price_expiration"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (WhatsAppException e) {
                    e.printStackTrace();
                }

                callback.doJob(response);
            }
        });


	}

	/**
	 * Request a registration code from WhatsApp.
	 *
	 * @param String method
	 *   Accepts only 'sms' or 'voice' as a value.
	 * @param String countryCode
	 *   ISO Country Code, 2 Digit.
	 * @param String langCode
	 *   ISO 639-1 Language Code: two-letter codes.
	 *
	 * @return {@link JSONObject}
	 *   An object with server response.
	 *   - status: Status of the request (sent/fail).
	 *   - length: Registration code lenght.
	 *   - method: Used method.
	 *   - reason: Reason of the status (e.g. too_recent/missing_param/bad_param).
	 *   - param: The missing_param/bad_param.
	 *   - retry_after: Waiting time before requesting a new code.
	 * @throws JSONException 
	 * @throws WhatsAppException 
	 * @throws UnsupportedEncodingException 
	 *
	 * @throws Exception
	 */
	public void codeRequest(final String method, String countryCode, String langCode, final Callback callback) throws WhatsAppException, JSONException, UnsupportedEncodingException {

		Map<String, String> phone;
		if ((phone = dissectPhone()) == null) {
			throw new WhatsAppException("The provide phone number is not valid.");
		}

		if(countryCode == null) {
			if(phone.get("ISO3166") != null) {
				countryCode = phone.get("ISO3166");
			} else {
				countryCode = "US";
			}
		}
		if(langCode == null) {
			if(phone.get("ISO639") != null) {
				langCode = phone.get("ISO639");
			} else {
				langCode = "en";
			}
		}

		String token;
		try {
			token = generateRequestToken(phone.get("country"), phone.get("phone"));
		} catch (NoSuchAlgorithmException e) {
			throw new WhatsAppException(e);
		} catch (IOException e) {
			throw new WhatsAppException(e);
		}
		// Build the url.
		String host = "https://"+WHATSAPP_REQUEST_HOST;
		Map<String,String> query = new LinkedHashMap<String, String>();
		query.put("cc",phone.get("cc")); 
		query.put("in",phone.get("phone")); 
		//		query.put("to",phoneNumber); 
		query.put("lg",langCode); 
		query.put("lc", countryCode);
		query.put("method", method);
		//		query.put("mcc",phone.get("mcc"));
		//		query.put("mnc","001");
		query.put("sim_mcc",phone.get("mcc"));
		query.put("sim_mnc","000");
		query.put("token", URLEncoder.encode(token,"iso-8859-1"));
		query.put("id",(identity==null?"":identity));


        getResponse(host, query, new Callback() {
            @Override
            public void doJob(JSONObject response) {

                try {
                    if (!response.getString("status").equals("ok")) {
                        if(response.getString("status").equals("sent")) {
                            eventManager().fireCodeRequest(phoneNumber, method, response.getString("length"));
                        } else {
                            if(!response.isNull("reason") && response.getString("reason").equals("too_recent")) {
                                String retry_after = (response.has("retry_after")?response.getString("retry_after"):null);
                                eventManager().fireCodeRequestFailedTooRecent(phoneNumber, method, response.getString("reason"), retry_after);
                                throw new WhatsAppException("Code already sent. Retry after "+retry_after+" seconds");
                            } else {
                                eventManager().fireCodeRequestFailed(phoneNumber, method, response.getString("reason"), (response.has("param")?response.getString("param"):null));
                                throw new WhatsAppException("There was a problem trying to request the code. Status="+response.getString("status"));
                            }
                        }
                    } else {
                        eventManager().fireCodeRegister(phoneNumber, response.getString("login"), response.getString("pw"), response.getString("type"), response.getString("expiration"),
                                response.getString("kind"), response.getString("price"), response.getString("cost"), response.getString("currency"), response.getString("price_expiration"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (WhatsAppException e) {
                    e.printStackTrace();
                }

                callback.doJob(response);
            }
        });





	}

    protected String generateRequestToken(String country, String phone) throws IOException, NoSuchAlgorithmException {
        return WhatsMediaUploader.md5(RELEASE_TOKEN_CONST+RELEASE_TIME+phone);
    }

	private byte[] hash(String algo, byte[] dataBytes) throws NoSuchAlgorithmException {
		MessageDigest md;

		md = MessageDigest.getInstance(algo);

		md.update(dataBytes, 0, dataBytes.length);
		byte[] mdbytes = md.digest();        
		return mdbytes;	
	}

	/**
	 * Connect (create a socket) to the WhatsApp network.
	 */
	public boolean connect() throws UnknownHostException, IOException {
		socket = new Socket(WHATSAPP_HOST, PORT);
		if(socket.isConnected()) {
			socket.setSoTimeout(TIMEOUT_SEC*1000);
			return true;
		} else {
			log.warn("Failed to connect to WhatsApp server");
			return false;
		}	
	}

	/**
	 * Disconnect from the WhatsApp network.
	 */
	public void disconnect() {
//		if(poller != null) {
//			poller.setRunning(false);
//		}
		if (socket != null && socket.isConnected()) {
			try {
				socket.close();
			} catch (IOException e) {
				log.error("Exception while disconnecting",e);
			}
		}	
		eventManager().fireDisconnect(
				phoneNumber,
				socket
				);
	}

	/**
	 * Drain the message queue for application processing.
	 *
	 * @return List<ProtocolNode>
	 *   Return the message queue list.
	 */
	public List<ProtocolNode> getMessages() {
		List<ProtocolNode> ret = messageQueue;
		messageQueue = new LinkedList<ProtocolNode>();

		return ret;
	}

	/**
	 * Log into the Whatsapp server.
	 *
	 * ###Warning### using this method will generate a new password
	 * from the WhatsApp servers each time.
	 *
	 * If you know your password and wish to use it without generating
	 * a new password - use the loginWithPassword() method instead.
	 *
	 * @param  boolean profileSubscribe
	 *
	 * Set this to true if you would like Whatsapp to send a
	 * notification to your phone when one of your contacts
	 * changes/update their picture.
	 * @throws WhatsAppException 
	 */
	public void login(boolean profileSubscribe) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");

	}

	/**
	 * Login to the Whatsapp server with your password
	 *
	 * If you already know your password you can log into the Whatsapp server
	 * using this method.
	 *
	 * @param  String  password         Your whatsapp password. You must already know this!
	 * @param  bool $profileSubscribe Add a feature
	 */
	public void loginWithPassword(String password) throws WhatsAppException {
		this.password = password;
		try {
			doLogin();
			if(loginStatus != LoginStatus.CONNECTED_STATUS) {
				throw new WhatsAppException("Failed to log in");
			}
		} catch (Exception e) {
			throw new WhatsAppException(e);
		}
	}

	/**
	 * Send the active status. User will show up as "Online" (as long as socket is connected).
	 * @throws WhatsAppException 
	 */
	public void sendActiveStatus() throws WhatsAppException {
		HashMap<String, String> map = new HashMap<String,String>();
		map.put("type", "active");
		ProtocolNode messageNode = new ProtocolNode("presence", map, null, null);
		sendNode(messageNode);
	}

	public void sendBroadcastAudio(List<String> targets, String path) throws WhatsAppException {
		sendBroadcastAudio(targets,path, false);
	}

	/**
	 * Send a Broadcast Message with audio.
	 *
	 * The recipient MUST have your number (synced) and in their contact list
	 * otherwise the message will not deliver to that person.
	 *
	 * Approx 20 (unverified) is the maximum number of targets
	 *
	 * @param  List<String>  targets       An list of numbers to send to.
	 * @param  String  path          URL or local path to the audio file to send
	 * @param  boolean storeURLmedia Keep a copy of the audio file on your server
	 * @throws WhatsAppException 
	 */
	public void sendBroadcastAudio(List<String> targets, String path, boolean storeURLmedia) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	public void sendBroadcastImage(List<String> targets, String path) throws WhatsAppException {
		sendBroadcastImage(targets,path,false);
	}

	/**
	 * Send a Broadcast Message with an image.
	 *
	 * The recipient MUST have your number (synced) and in their contact list
	 * otherwise the message will not deliver to that person.
	 *
	 * Approx 20 (unverified) is the maximum number of targets
	 *
	 * @param  List<String>  targets       An list of numbers to send to.
	 * @param  String  path          URL or local path to the audio file to send
	 * @param  boolean storeURLmedia Keep a copy of the audio file on your server
	 * @throws WhatsAppException 
	 */
	public void sendBroadcastImage(List<String> targets, String path, boolean storeURLmedia) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Send a Broadcast Message with location data.
	 *
	 * The recipient MUST have your number (synced) and in their contact list
	 * otherwise the message will not deliver to that person.
	 *
	 * If no name is supplied , receiver will see large sized google map
	 * thumbnail of entered Lat/Long but NO name/url for location.
	 *
	 * With name supplied, a combined map thumbnail/name box is displayed

	 * Approx 20 (unverified) is the maximum number of targets
	 *
	 * @param  List<String>  targets       An list of numbers to send to.
	 * @param  float lng    The longitude of the location eg 54.31652
	 * @param  float lat     The latitude if the location eg -6.833496
	 * @param  String name    (Optional) A name to describe the location
	 * @param  String url     (Optional) A URL to link location to web resource
	 * @throws WhatsAppException 
	 */

	public void sendBroadcastLocation(List<String> targets, float lng, float lat, String name, String url) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Send a Broadcast Message
	 *
	 * The recipient MUST have your number (synced) and in their contact list
	 * otherwise the message will not deliver to that person.
	 *
	 * Approx 20 (unverified) is the maximum number of targets
	 *
	 * @param  List<String>  targets       An list of numbers to send to.
	 * @param  String message Your message
	 * @throws WhatsAppException 
	 */
	public void sendBroadcastMessage(List<String> targets, String message) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	public void sendBroadcastVideo(List<String> targets, String path) throws WhatsAppException {
		sendBroadcastVideo(targets,path,false);
	}

	/**
	 * Send a Broadcast Message with a video.
	 *
	 * The recipient MUST have your number (synced) and in their contact list
	 * otherwise the message will not deliver to that person.
	 *
	 * Approx 20 (unverified) is the maximum number of targets
	 *
	 * @param  List<String>  targets       An list of numbers to send to.
	 * @param  String  path          URL or local path to the video file to send
	 * @param  boolean storeURLmedia Keep a copy of the audio file on your server
	 * @throws WhatsAppException 
	 */
	public void sendBroadcastVideo(List<String> targets, String path, boolean storeURLmedia) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	public void sendClientConfig() throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}
	public void sendGetClientConfig() throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Send a request to return a list of groups user is currently participating
	 * in.
	 *
	 * To capture this list you will need to bind the "onGetGroups" event.
	 * @throws WhatsAppException 
	 */
	public void sendGetGroups() throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Send a request to get information about a specific group
	 *
	 * @param  String gjid The specific group id
	 * @throws WhatsAppException 
	 */
	public void sendGetGroupsInfo(String gjid) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Send a request to return a list of groups user has started
	 * in.
	 *
	 * To capture this list you will need to bind the "onGetGroups" event.
	 * @throws WhatsAppException 
	 */
	public void sendGetGroupsOwning() throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Send a request to return a list of people participating in a specific
	 * group.
	 *
	 * @param  String gjid The specific group id
	 * @throws WhatsAppException 
	 */
	public void sendGetGroupsParticipants(String gjid) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Send a request to get a list of people you have currently blocked
	 * @throws WhatsAppException 
	 */
	public void sendGetPrivacyBlockedList() throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	public void sendGetProfilePicture(String number) throws WhatsAppException {
		sendGetProfilePicture(number,false);
	}

	/**
	 * Get profile picture of specified user
	 *
	 * @param String number
	 *  Number or JID of user
	 *
	 * @param boolean large
	 *  Request large picture
	 * @throws WhatsAppException 
	 */
	public void sendGetProfilePicture(String number, boolean large) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Request to retrieve the last online time of specific user.
	 *
	 * @param String to
	 *  Number or JID of user
	 */
	public void sendGetRequestLastSeen(String to) {
		//TODO implement this
	}

	/**
	 * Send a request to get the current server properties
	 * @throws WhatsAppException 
	 */
	public void sendGetServerProperties() throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Create a group chat.
	 *
	 * @param String subject
	 *   The group Subject
	 * @param List<String> participants
	 *   An array with the participants numbers.
	 *
	 * @return String
	 *   The group ID.
	 * @throws WhatsAppException 
	 */
	public String sendGroupsChatCreate(String subject, List<String> participants) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * End or delete a group chat
	 *
	 * @param  String gjid The group ID
	 * @throws WhatsAppException 
	 */
	public void sendGroupsChatEnd(String gjid) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Leave a group chat
	 *
	 * @param  List<String> gjids A list of group IDs
	 * @throws WhatsAppException 
	 */
	public void sendGroupsLeave(List<String> gjids) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Add participant(s) to a group.
	 *
	 * @param String groupId
	 *   The group ID.
	 * @param List<String> participants
	 *   An array with the participants numbers to add
	 * @throws WhatsAppException 
	 */
	public void sendGroupsParticipantsAdd(String groupId, List<String> participants) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Remove participant(s) from a group.
	 *
	 * @param String groupId
	 *   The group ID.
	 * @param List<String> participants
	 *   An array with the participants numbers to remove
	 * @throws WhatsAppException 
	 */
	public void sendGroupsParticipantsRemove(String groupId, List<String> participants) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Send audio to the user/group.     *
	 *
	 * @param String to
	 *   The recipient.
	 * @param File file
	 *   The audio file.
	 * @return JSONObject json object with media information, or null if sending failed
	 * @throws WhatsAppException 
	 */
	public JSONObject sendMessageAudio(String to, File filepath) throws WhatsAppException {
		return sendMessageAudio(to,filepath,false);
	}

	/**
	 * Send audio to the user/group.     *
	 *
	 * @param String to
	 *   The recipient.
	 * @param File file
	 *   The audio file.
	 * @param  boolean storeURLmedia Keep copy of file
	 * @return JSONObject json object with media information, or null if sending failed
	 * @throws WhatsAppException 
	 */
	public JSONObject sendMessageAudio(String to, File file, boolean storeURLmedia) throws WhatsAppException {
		String[] allowedExtensions = { "3gp", "caf", "wav", "mp3", "mp4", "wma", "ogg", "aif", "aac", "m4a" };
		int size = 10 * 1024 * 1024; // Easy way to set maximum file size for this media type.
		try {
			// This list should be done better or at least cached!
			List<String> list = new ArrayList<String>();
			for(String ext : allowedExtensions) {
				list.add(ext);
			}
			MediaInfo info = new MediaInfo();
			info.setMediaFile(file);
			return sendCheckAndSendMedia(info, size, to, "audio", list, null);
		} catch (Exception e) {
			log.warn("Exception sending audio",e);
			throw new WhatsAppException(e);
		}
	}

	/**
	 * Checks that the media file to send is of allowable filetype and within size limits.
	 *
	 * @param File file The media file
	 * @param int maxSize Maximim filesize allowed for media type
	 * @param String to Recipient ID/number
	 * @param String type media filetype. 'audio', 'video', 'image'
	 * @param String[] allowedExtensions An array of allowable file types for the media file
	 * @param boolean storeURLmedia Keep a copy of the media file
	 * @return JSONObject json with media details, or null if failed
	 * @throws IOException 
	 * @throws InvalidTokenException 
	 * @throws InvalidMessageException 
	 * @throws IncompleteMessageException 
	 * @throws WhatsAppException 
	 * @throws JSONException 
	 * @throws NoSuchAlgorithmException 
	 * @throws DecodeException 
	 * @throws InvalidKeyException 
	 */
	private JSONObject sendCheckAndSendMedia(MediaInfo info, int maxSize, String to,
			String type, List<String> allowedExtensions, String caption) throws WhatsAppException, IncompleteMessageException, InvalidMessageException, InvalidTokenException, IOException, JSONException, NoSuchAlgorithmException, InvalidKeyException, DecodeException {
		File file = info.getMediaFile();
		if(file.length() <= maxSize && file.isFile() && file.length() > 0) {
			String fileName = file.getName();
			int lastIndexOf = fileName.lastIndexOf('.')+1;
			String extension = fileName.substring(lastIndexOf);
			if (allowedExtensions.contains(extension)) {
				mediaInfo = null;
				String b64hash = base64_encode(hash_file("sha256", file, true));
				//request upload
				sendRequestFileUpload(b64hash, type, info, to, caption);
				return mediaInfo;
			} else {
				//Not allowed file type.
				return new JSONObject("{\"error\":\"Invalid media type "+extension+"\"}");
			}
		} else {
			//Didn't get media file details.
			return null;
		}
	}

	private void sendRequestFileUpload(String b64hash, String type, MediaInfo file,
			String to, String caption) throws WhatsAppException, IncompleteMessageException, InvalidMessageException, InvalidTokenException, IOException, JSONException, NoSuchAlgorithmException, InvalidKeyException, DecodeException {
		mediaFile = file;
		Map<String,String> hash = new HashMap<String, String>();
		hash.put("hash", b64hash);
		hash.put("type", type);
		hash.put("size", Long.toString(file.getMediaFile().length()));
		ProtocolNode mediaNode = new ProtocolNode("media", hash, null, null);
		hash = new HashMap<String, String>();
		String id = createMsgId("upload");
		hash.put("id", id);
		hash.put("to", WHATSAPP_SERVER);
		hash.put("type", "set");
		hash.put("xmlns", "w:m");
		ArrayList<ProtocolNode> list = new ArrayList<ProtocolNode>();
		list.add(mediaNode);
		ProtocolNode node = new ProtocolNode("iq", hash, list, null);

		/*
		 * TODO support for multiple recipients
		 *  if (!is_array($to)) {
		 *    $to = $this->getJID($to);
		 *	}
		 *
		 */
		String messageId = createMsgId("message");
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("messageNode", node);
		map.put("file", file);
		map.put("to", to);
		map.put("message_id", messageId);
		map.put("caption", caption);
		mediaQueue.put(id,map);
		sendNode(node);
		waitForServer(id);
	}

	private String base64_encode(byte[] data) {
		byte[] enc = Base64.encodeBase64(data);
		return new String(enc);
	}

	private byte[] hash_file(String string, File file, boolean b) throws NoSuchAlgorithmException, IOException {
		MessageDigest md;

		md = MessageDigest.getInstance("SHA-256");
		FileInputStream fis = new FileInputStream(file);

		try {
			byte[] dataBytes = new byte[1024];

			int nread = 0; 
			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			};
			byte[] mdbytes = md.digest();        
			return mdbytes;
		} finally {
			fis.close();
		}
	}

	/**
	 * Send the composing message status. When typing a message.
	 *
	 * @param String to
	 *   The recipient to send status to.
	 * @throws WhatsAppException 
	 */
	public void sendMessageComposing(String to) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}


	/**
	 * Send an image file to group/user
	 *
	 * @param  String to
	 *  Recipient number
	 * @param  String filepath
	 *   The url/uri to the image file.
	 * @return JSONObject 
	 * @throws WhatsAppException 
	 */
	public JSONObject sendMessageImage(String to, File image, File preview) throws WhatsAppException {
		return sendMessageImage(to,image, preview,"");
	}

	/**
	 * Send an image file to group/user
	 *
	 * @param  String to
	 *  Recipient number
	 * @param  String filepath
	 *   The url/uri to the image file.
	 * @param  boolean storeURLmedia Keep copy of file
	 * @return JSONObject 
	 * @throws WhatsAppException 
	 */
	public JSONObject sendMessageImage(String to, File image, File preview, String caption) throws WhatsAppException {

		String[] allowedExtensions = { "jpg","jpeg","gif","png" };
		int size = 5 * 1024 * 1024; // Easy way to set maximum file size for this media type.
		try {
			// This list should be done better or at least cached!
			List<String> list = new ArrayList<String>();
			for(String ext : allowedExtensions) {
				list.add(ext);
			}
			MediaInfo info = new MediaInfo();
			info.setMediaFile(image);
			info.setPreviewFile(preview);
			info.setCaption(caption);
			return sendCheckAndSendMedia(info, size, to, "image", list, caption);
		} catch (Exception e) {
			log.warn("Exception sending audio",e);
			throw new WhatsAppException(e);
		}
	}

	/**
	 * Send a location to the user/group.
	 *
	 * If no name is supplied , receiver will see large sized google map
	 * thumbnail of entered Lat/Long but NO name/url for location.
	 *
	 * With name supplied, a combined map thumbnail/name box is displayed
	 *
	 * @param List<String> to The recipient(s) to send to.
	 * @param  float lng    The longitude of the location eg 54.31652
	 * @param  float lat     The latitude if the location eg -6.833496
	 * @param String name (Optional)  The custom name you would like to give this location.
	 * @param String url (Optional) A URL to attach to the location.
	 * @throws WhatsAppException 
	 */
	public void sendMessageLocation(List<String> to, float lng, float lat, String name, String url) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Send the 'paused composing message' status.
	 *
	 * @param String to
	 *   The recipient number or ID.
	 * @throws WhatsAppException 
	 */
	public void sendMessagePaused(String to) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Send a video to the user/group.
	 *
	 * @param  String to
	 *   The recipient to send.
	 * @param String filepath
	 *   The url/uri to the MP4/MOV video.
	 * @param  boolean $storeURLmedia Keep a copy of media file.
	 * @return boolean
	 * @throws WhatsAppException 
	 */
	public JSONObject sendMessageVideo(String to, File media, File preview, String caption) throws WhatsAppException {
		String[] allowedExtensions = { "3gp", "mp4", "mov", "avi" };
		int size = 20 * 1024 * 1024; // Easy way to set maximum file size for this media type.
		try {
			// This list should be done better or at least cached!
			List<String> list = new ArrayList<String>();
			for(String ext : allowedExtensions) {
				list.add(ext);
			}
			MediaInfo info = new MediaInfo();
			info.setMediaFile(media);
			info.setPreviewFile(preview);
			info.setCaption(caption);
			return sendCheckAndSendMedia(info, size, to, "video", list, caption);
		} catch (Exception e) {
			log.warn("Exception sending video",e);
			throw new WhatsAppException(e);
		}
	}

	/**
	 * Send the offline status. User will show up as "Offline".
	 * @throws WhatsAppException 
	 */
	public void sendOfflineStatus() throws WhatsAppException {
		HashMap<String, String> map = new HashMap<String,String>();
		map.put("type", "unavailable");
		ProtocolNode messageNode = new ProtocolNode("presence", map, null, null);
		sendNode(messageNode);
	}

	/**
	 * Send available presence status.
	 */
	public void sendPresence() throws IOException, WhatsAppException {
		sendPresence("available");
	}

	/**
	 * Send presence subscription, automatically receive presence updates as long as the socket is open.
	 *
	 * @param String to
	 *   Phone number.
	 * @throws WhatsAppException 
	 */
	public void sendPresenceSubscription(String to) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Set the picture for the group
	 *
	 * @param  String gjid The groupID
	 * @param  String path The URL/URI of the image to use
	 * @throws WhatsAppException 
	 */
	public void sendSetGroupPicture(String gjid, String path) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Set the list of numbers you wish to block receiving from.
	 *
	 * @param List<String> blockedJids Array of numbers to block messages from.
	 * @throws WhatsAppException 
	 */
	public void sendSetPrivacyBlockedList(List<String> blockedJids) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Set your profile picture. Thumbnail should be 96px size version of image
	 *
	 * @param  Image file
	 * @param  Thumbnail file
	 * @throws WhatsAppException 
	 */
	public void sendSetProfilePicture(File image, File thumbnail) throws WhatsAppException {
		sendSetPicture(phoneNumber,image, thumbnail);
	}

	/**
	 * Set your profile picture
	 *
	 * @param String jid
	 * @param File image
	 * @param File thumbnail
	 *  URL or localpath to image file
	 * @throws WhatsAppException 
	 */
	private void sendSetPicture(String jid, File image, File thumbnail) throws WhatsAppException {
		preprocessProfilePicture(image);

		if (image.exists() && image.canRead() && image.isFile()) {
			byte[] data;
			try {
				data = readFile(image);
			} catch (IOException e) {
				throw new WhatsAppException("Failed to read image file", e);
			}
			if (data != null && data.length > 0) {
				//this is where the fun starts
				ProtocolNode picture = new ProtocolNode("picture", null, null, data);

				byte[] icon;
				if(thumbnail != null && thumbnail.isFile() && thumbnail.canRead()) {
					try {
						icon = readFile(thumbnail);
					} catch (IOException e1) {
						throw new WhatsAppException("Failed to read thumbnail image",e1);
					}
				} else {
					icon = createIconGD(image, 96, true);
				}
				HashMap<String,String> typeMap = new HashMap<String, String>();
				typeMap.put("type", "preview");
				ProtocolNode thumb = new ProtocolNode("picture", typeMap, null, icon);

				HashMap<String, String> hash = new HashMap<String, String>();
				String nodeID = createMsgId("setphoto");
				hash.put("id",nodeID);
				hash.put("to",getJID(jid));
				hash.put("type","set");
				hash.put("xmlns","w:profile:picture");
				List<ProtocolNode> arr = new LinkedList<ProtocolNode>();
				arr.add(picture);
				arr.add(thumb);
				ProtocolNode node = new ProtocolNode("iq", hash, arr, null);

				sendNode(node);
				try {
					waitForServer(nodeID);
				} catch (Exception e) {
					throw new WhatsAppException("Waiting for reply failed", e);
				}
			}
		}	}

	private byte[] createIconGD(File filepath, int i, boolean b) throws WhatsAppException {
		throw new WhatsAppException("Automatic creation of thumbnail not yet implemented");
		//		list($width, $height) = getimagesize($file);
		//		if ($width > $height) {
		//			//landscape
		//			$nheight = ($height / $width) * $size;
		//			$nwidth = $size;
		//		} else {
		//			$nwidth = ($width / $height) * $size;
		//			$nheight = $size;
		//		}
		//		$image_p = imagecreatetruecolor($nwidth, $nheight);
		//		$image = imagecreatefromjpeg($file);
		//		imagecopyresampled($image_p, $image, 0, 0, 0, 0, $nwidth, $nheight, $width, $height);
		//		ob_start();
		//		imagejpeg($image_p);
		//		$i = ob_get_contents();
		//		ob_end_clean();
		//		if ($raw) {
		//			return $i;
		//		} else {
		//			return base64_encode($i);
		//		}
	}

	private byte[] readFile(File filepath) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		//write file data
		FileInputStream fileInputStream = new FileInputStream(filepath);

		// Copy the contents of the file to the output stream
		byte[] buffer = new byte[1024];
		int count = 0;

		while ((count = fileInputStream.read(buffer)) >= 0) {
			out.write(buffer, 0, count);
		}                
		fileInputStream.close();
		return out.toByteArray();
	}

	private void preprocessProfilePicture(File filepath) {
		// TODO Auto-generated method stub

	}

	/**
	 * Set the recovery token for your account to allow you to
	 * retrieve your password at a later stage.
	 * @param  String token A user generated token.
	 * @throws WhatsAppException 
	 */
	public void sendSetRecoveryToken(String token) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Update the user status.
	 *
	 * @param String txt
	 *   The text of the message status to send.
	 * @throws WhatsAppException 
	 */
	public void sendStatusUpdate(String txt) throws WhatsAppException {
		//TODO implement this

	}

	/**
	 * Send a vCard to the user/group.
	 *
	 * @param String to
	 *   The recipient to send.
	 * @param String name
	 *   The contact name.
	 * @param VCard vCard
	 *   The contact vCard to send.
	 * @throws WhatsAppException 
	 */
	public void sendVcard(String to, String name, Object vCard) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Sets the bind of the new message.
	 * @throws WhatsAppException 
	 */
	public void setNewMessageBind(MessageProcessor processor) throws WhatsAppException {
		this.processor  = processor;
	}

	/**
	 * Upload file to WhatsApp servers.
	 *
	 * @param String file
	 *   The uri of the file.
	 *
	 * @return String
	 *   Return the remote url or null on failure.
	 * @throws WhatsAppException 
	 */
	public String uploadFile(String file) throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Wait for message delivery notification.
	 * @throws WhatsAppException 
	 */
	public void waitForMessageReceipt() throws WhatsAppException {
		//TODO implement this
		throw new WhatsAppException("Not yet implemented");
	}

	/**
	 * Check if account credentials are valid.
	 *
	 * WARNING: WhatsApp now changes your password everytime you use this.
	 * Make sure you update your config file if the output informs about
	 * a password change.
	 *
	 * @return object
	 *   An object with server response.
	 *   - status: Account status.
	 *   - login: Phone number with country code.
	 *   - pw: Account password.
	 *   - type: Type of account.
	 *   - expiration: Expiration date in UNIX TimeStamp.
	 *   - kind: Kind of account.
	 *   - price: Formatted price of account.
	 *   - cost: Decimal amount of account.
	 *   - currency: Currency price of account.
	 *   - price_expiration: Price expiration in UNIX TimeStamp.
	 * @throws JSONException 
	 * @throws WhatsAppException 
	 *
	 * @throws Exception
	 */
	public void  checkCredentials(String number, final Callback callback) throws JSONException, WhatsAppException {
		Map<String, String> phone;
		if ((phone = dissectPhone()) == null) {
			throw new WhatsAppException("The prived phone number is not valid.");
		}

		// Build the url.
		String host = "https://"+WHATSAPP_CHECK_HOST;
		Map<String,String> query = new LinkedHashMap<String, String>();
		query.put("cc",phone.get("cc")); 
		query.put("in",phone.get("phone")); 
		query.put("id",identity);
		query.put("c","cookie");

		getResponse(host, query, new Callback() {
            @Override
            public void doJob(JSONObject response) {
                try {
                    if (!response.getString("status").equals("ok")) {
                        throw new WhatsAppException("There was a problem trying to request the code. Status="+response.getString("status"));
                    } else {
                        log.debug("Setting password: "+response.getString("pw"));
                        password = response.getString("pw");
                        callback.doJob(new JSONObject("TRUE"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (WhatsAppException e) {
                    e.printStackTrace();
                }
            }


        });


	}

	public String sendMessage(String to, String message) throws WhatsAppException {
		return sendMessage(to, message, null);
	}

	/**
	 * Send a text message to the user/group.
	 *
	 * @param String to
	 *   The recipient.
	 * @param String message
	 *   The text message.
	 * @param String id
	 *
	 * @return String
	 */
	public String sendMessage(String to, String message, String id) throws WhatsAppException {
		message = parseMessageForEmojis(message);
		ProtocolNode bodyNode = new ProtocolNode("body", null, null, message.getBytes());
		try {
			return sendMessageNode(to, bodyNode, id);
		} catch (Exception e) {
			throw new WhatsAppException("Failed to send message",e);
		}
	}

	private List<Country> readCountries() throws WhatsAppException {
		List<Country> result = new LinkedList<Country>();
		InputStream is = this.getClass().getResourceAsStream("/countries.csv");
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		if(is == null) {
			throw new WhatsAppException("Failed to locate countries.csv");
		}

		try {

			br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			while ((line = br.readLine()) != null) {

				// use comma as separator
				String[] entry = line.split(cvsSplitBy);
				Country country = new Country(entry);
				result.add(country);
			}

		} catch (FileNotFoundException e) {

		} catch (IOException e) {

		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {

				}
			}
			if(is != null) {
				try {
					is.close();
				} catch (IOException e) {

				}
			}
		}		
		return result;
	}

	protected List<Country> getCountries() {
		return countries;
	}

	protected String buildIdentity(String id) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		byte[] hash = hash("SHA-1", id.getBytes());
		String hashString = new String(hash, "iso-8859-1");
		String newId = URLEncoder.encode(hashString, "iso-8859-1").toLowerCase();
		if(log.isDebugEnabled()) {
			log.debug("ID: "+newId);
		}
		return newId;
	}

	protected boolean checkIdentity(String id) throws UnsupportedEncodingException {
		if(id != null)
			return (URLDecoder.decode(id, "iso-8859-1").length() == 20);
		return false;
	}

	private void doLogin() throws InvalidKeyException, NoSuchAlgorithmException, IOException, InvalidKeySpecException, WhatsAppException, IncompleteMessageException, InvalidMessageException, InvalidTokenException, JSONException, EncodeException, DecodeException {
		writer.resetKey();
		reader.resetKey();
		String resource = WHATSAPP_DEVICE + "-" + WHATSAPP_VER + "-" + PORT;
		byte[] data = writer.startStream(WHATSAPP_SERVER, resource);
		ProtocolNode feat = createFeaturesNode(false);
		ProtocolNode auth = createAuthNode();
		sendData(data);
		sendNode(feat);
		sendNode(auth);

		pollMessages();
		pollMessages();
		pollMessages();

		if(challengeData != null) {
			ProtocolNode dataNode = createAuthResponseNode();
			sendNode(dataNode);
			reader.setKey(inputKey);
			writer.setKey(outputKey);
			pollMessages();
		}
		if(loginStatus == LoginStatus.DISCONNECTED_STATUS) {
			throw new WhatsAppException("Login failure");
		}
		int cnt = 0;

//		poller.start();
//		do {
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				throw new WhatsAppException(e);
//			}
//		} while ((cnt++ < 100) && (loginStatus == LoginStatus.DISCONNECTED_STATUS));
		sendPresence("available");
	}

	private void sendPresence(String type) throws IOException, WhatsAppException {
		Map<String, String> presence = new LinkedHashMap<String, String>();
		//		presence.put("type",type);
		presence.put("name",name);
		ProtocolNode node = new ProtocolNode("presence", presence, null, null);
		sendNode(node);
		eventManager().fireSendPresence(
				phoneNumber, 
				type,
				presence.get("name")
				);
	}

	/**
	 * Add the auth response to protocoltreenode.
	 *
	 * @return ProtocolNode
	 *   Return itself.
	 * @throws EncodeException 
	 * @throws IOException 
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeyException 
	 */
	private ProtocolNode createAuthResponseNode() throws EncodeException, IOException {
		byte[] resp = authenticate();
		Map<String,String> attributes = new LinkedHashMap<String, String>();
		//		attributes.put("xmlns","urn:ietf:params:xml:ns:xmpp-sasl");
		ProtocolNode node = new ProtocolNode("response", attributes, null, resp);

		return node;
	}

	/**
	 * Authenticate with the Whatsapp Server.
	 *
	 * @return byte[]
	 *   Returns binary string
	 * @throws EncodeException 
	 * @throws IOException 
	 */
	byte[] authenticate() throws EncodeException, IOException {
		List<byte[]> keys = generateKeys();
		inputKey = new KeyStream(keys.get(2), keys.get(3));
		outputKey = new KeyStream(keys.get(0), keys.get(1));

		ByteArrayOutputStream array = new ByteArrayOutputStream();
		array.write(phoneNumber.getBytes()); 
		array.write(challengeData);
		//		array.write(Long.toString((new Date()).getTime()/1000).getBytes());
		byte[] response = outputKey.encode(array.toByteArray(), 0, 0,array.size());
		return response;
	}

	List<byte[]> generateKeys() throws EncodeException {
		try {
			List<byte[]> keys = new LinkedList<byte[]>();
			for(int i = 0; i < 4; ++i) {
				ByteArrayOutputStream nonce = getChallengeData();
				nonce.write(i+1);
				byte[] key = pbkdf2("SHA-1", base64_decode(password), nonce.toByteArray(), 2, 20,true);
				keys.add(key);
			}
			return keys;
		} catch (Exception e) {
			throw new EncodeException(e);
		}
	}

	private ByteArrayOutputStream getChallengeData() throws NoSuchAlgorithmException, IOException {
		if(challengeData == null) {
			log.info("Challenge data is missing!");
			SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
			challengeData = new byte[20];
			sr.nextBytes(challengeData);		 
		}
		ByteArrayOutputStream os = new ByteArrayOutputStream(challengeData.length);
		os.write(challengeData);
		return os;
	}

	protected byte[] pbkdf2(String algo, byte[] password,
			byte[] salt, int iterations, int length, boolean raw) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, InvalidKeyException {
		if (iterations <= 0 || length <= 0) {
			throw new InvalidKeySpecException("PBKDF2 ERROR: Invalid parameters.");
		}

		int hash_length = 20; //hash(algo, "", true).length();
		double block_count = Math.ceil(length / hash_length);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		for (int i = 1; i <= block_count; i++) {
			ByteArrayOutputStream last = new ByteArrayOutputStream();
			last.write(salt);
			ByteBuffer buffer = ByteBuffer.allocate(4);
			buffer.putInt(i);
			last.write(buffer.array());
			byte[] lastBuf = last.toByteArray();
			byte[] xorsum = KeyStream.hash_hmac(lastBuf, password);
			byte[] xorsum2 = xorsum;
			for (int j = 1; j < iterations; j++) {
				xorsum2 = KeyStream.hash_hmac(xorsum2, password);
				last.reset();
				int k=0;
				for(byte b : xorsum) {
					last.write(b ^ xorsum2[k++]);
				}
				xorsum = last.toByteArray();
			}
			output.write(xorsum);
		}
		if(raw) {
			return output.toByteArray();
		}
		return toHex(output.toByteArray()).getBytes();
	}

	public static String toHex(byte[] array) throws NoSuchAlgorithmException
	{
		BigInteger bi = new BigInteger(1, array);
		String hex = bi.toString(16);
		int paddingLength = (array.length * 2) - hex.length();
		if(paddingLength > 0)
		{
			return String.format("%0"  +paddingLength + "d", 0) + hex;
		}else{
			return hex;
		}
	}

	byte[] base64_decode(String pwd) {
		return org.apache.commons.codec.binary.Base64.decodeBase64(pwd.getBytes());

	}

	private void processInboundData(byte[] readData) throws IncompleteMessageException, InvalidMessageException, InvalidTokenException, IOException, WhatsAppException, JSONException, NoSuchAlgorithmException, InvalidKeyException, DecodeException {
		if(readData == null || readData.length == 0) {
			return;
		}
		ProtocolNode node = reader.nextTree(readData);
		if(node != null) {
			processInboundDataNode(node);
		}
	}

	/**
	 * Will process the data from the server after it's been decrypted and parsed.
	 * 
	 * This also provides a convenient method to use to unit test the event framework.
	 * @throws IOException 
	 * @throws InvalidTokenException 
	 * @throws InvalidMessageException 
	 * @throws IncompleteMessageException 
	 * @throws WhatsAppException 
	 * @throws JSONException 
	 * @throws NoSuchAlgorithmException 
	 * @throws DecodeException 
	 * @throws InvalidKeyException 
	 * 
	 */
	private void processInboundDataNode(ProtocolNode node) throws IncompleteMessageException, InvalidMessageException, InvalidTokenException, IOException, WhatsAppException, JSONException, NoSuchAlgorithmException, InvalidKeyException, DecodeException {
		while (node != null) {
			ProtocolTag tag;
			try {
				tag = ProtocolTag.fromString(node.getTag().replace(':', '_').toUpperCase());
				if(tag == null) {
					tag = ProtocolTag.UNKNOWN;
					log.info("Unknown/Unused tag "+node.getTag());
				}
			} catch (IllegalArgumentException e) {
				tag = ProtocolTag.UNKNOWN;
				log.info("Unknown/Unused tag "+node.getTag());
			}
			log.debug("rx  "+node);
			switch(tag) {
			case CHALLENGE:
				processChallenge(node);
				break;
			case SUCCESS:
				loginStatus = LoginStatus.CONNECTED_STATUS;
				challengeData = node.getData();
				file_put_contents("nextChallenge.dat", challengeData);
				writer.setKey(outputKey);
				break;
			case FAILURE:
				log.error("Failure");
				break;
			case MESSAGE:
				processMessage(node);
				break;
			case ACK:
				processAck(node);
				break;
			case RECEIPT:
				processReceipt(node);
				break;
			case PRESENCE:
				processPresence(node);
				break;
			case IQ:
				processIq(node);
				break;
			case IB:
				processIb(node);
				break;
			case NOTIFICATION:
				processNotification(node);
				break;
			case CHATSTATE:
				processChatState(node);
				break;
			case STREAM_ERROR:
				throw new WhatsAppException("stream:error received: ");
			case PING:
				break;
			case QUERY:
				break;
			case START:
				break;
			case UNKNOWN:
				break;
			default:
				break;
			}
			node = reader.nextTree(null);
		}
	}

	private void processChatState(ProtocolNode node) throws WhatsAppException {
		log.debug("Processing CHATSTATE");
		if (node.hasChild("composing")) {
			eventManager().fireMessageComposing(
					phoneNumber,
					node.getAttribute("from"),
					node.getAttribute("id"),
					node.getAttribute("type"),
					node.getAttribute("t")
					);
		}
		if (node.hasChild("paused")) {
			eventManager().fireMessagePaused(
					phoneNumber,
					node.getAttribute("from"),
					node.getAttribute("type"),
					node.getAttribute("id"),
					node.getAttribute("t")
					);
		}
	}

	private void processNotification(ProtocolNode node) throws WhatsAppException {
		String name = node.getAttribute("notify");
		String type = node.getAttribute("type");
		log.debug("Processing "+type+" NOTIFICATION: "+name);
		if(type.equals("status")) {

		}
		if(type.equals("picture")) {

		}
		if(type.equals("contacts")) {

		}
		if(type.equals("encrypt")) {

		}
		if(type.equals("w:gp2")) {
			if(node.hasChild("create")) {

			}
			if(node.hasChild("add")) {

			}
			if(node.hasChild("remove")) {

			}
			if(node.hasChild("participant")) {

			}
			if(node.hasChild("subject")) {

			}

		}
		if(type.equals("account")) {

		}
		if(type.equals("features")) {

		}
		sendNotificationAck(node);
	}

	private void sendNotificationAck(ProtocolNode node) throws WhatsAppException {
		String from = node.getAttribute("from");
		String to = node.getAttribute("to");
		String participant = node.getAttribute("participant");
		String id = node.getAttribute("id");
		String type = node.getAttribute("type");

		Map<String,String> attributes = new HashMap<String, String>();
		if (to != null && !to.isEmpty())
			attributes.put("from",to);
		if (participant != null && !participant.isEmpty())
			attributes.put("participant",participant);
		attributes.put("to", from);
		attributes.put("class", "notification");
		attributes.put("id", id);
		attributes.put("type", type);

		ProtocolNode ack = new ProtocolNode("ack", attributes, null, null);

		sendNode(ack);	
	}

	private void processReceipt(ProtocolNode node) throws WhatsAppException {
		log.debug("Processing RECEIPT");
		serverReceivedId.add(node.getAttribute("id"));
		eventManager().fireMessageReceivedClient(
				phoneNumber,
				node.getAttribute("from"),
				node.getAttribute("id"),
				(node.getAttribute("type")==null?"":node.getAttribute("type")),
				node.getAttribute("t")
				);

		sendAck(node);
	}

	private void sendAck(ProtocolNode node) throws WhatsAppException {
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("id", node.getAttribute("id"));
		attributes.put("to", node.getAttribute("from"));
		attributes.put("t", node.getAttribute("t"));
		ProtocolNode ack = new ProtocolNode("ack", attributes, null, null);
		sendNode(ack);
	}

	private void processAck(ProtocolNode node) {
		log.debug("Processing ACK");
		serverReceivedId.add(node.getAttribute("id"));
	}

	private void processIb(ProtocolNode node) throws IOException, WhatsAppException, IncompleteMessageException, InvalidMessageException, InvalidTokenException, JSONException, NoSuchAlgorithmException {
		String type = node.getAttribute("type");
		log.info("Processing IB "+(type==null?"":type));
		for(ProtocolNode n : node.getChildren()) {
			ProtocolTag tag = ProtocolTag.fromString(n.getTag());
			switch(tag) {
			case DIRTY:
				List<String> categories = new LinkedList<String>();
				categories.add(n.getAttribute("type"));
				sendClearDirty(categories);
				break;
			case OFFLINE:
				log.info("Offline count"+ n.getAttribute("count"));
				break;
			default:
			}
		}
	}
	private void processIq(ProtocolNode node) throws IOException, WhatsAppException, IncompleteMessageException, InvalidMessageException, InvalidTokenException, JSONException, NoSuchAlgorithmException, InvalidKeyException, DecodeException {
		log.info("Processing IQ "+node.getAttribute("type"));
		if (node.getAttribute("type").equals("get")
				&& node.getAttribute("xmlns").equals("urn:xmpp:ping")) {
			eventManager().firePing(
					phoneNumber,
					node.getAttribute("id")
					);
			sendPong(node.getAttribute("id"));
		}
		if (node.getAttribute("type").equals("result")) {
			if(log.isDebugEnabled()) {
				log.debug("processIq: setting received id to "+node.getAttribute("id"));
			}
			serverReceivedId.add(node.getAttribute("id"));
			if (node.getChild(0) != null &&
					node.getChild(0).getTag().equals(ProtocolTag.QUERY)) {
				if (node.getChild(0).getAttribute("xmlns").equals("jabber:iq:privacy")) {
					// ToDo: We need to get explicitly list out the children as arguments
					//       here.
					eventManager().fireGetPrivacyBlockedList(
							phoneNumber,
							node.getChild(0).getChild(0).getChildren()
							);
				}
				if (node.getChild(0).getAttribute("xmlns").equals("jabber:iq:last")) {
					eventManager().fireGetRequestLastSeen(
							phoneNumber,
							node.getAttribute("from"),
							node.getAttribute("id"),
							node.getChild(0).getAttribute("seconds")
							);				}
			}
			messageQueue.add(node);
		}
		if (node.getChild(0) != null && node.getChild(0).getTag().equals("props")) {
			//server properties
			Map<String,String> props = new LinkedHashMap<String,String>();
			for(ProtocolNode child : node.getChild(0).getChildren()) {
				props.put(child.getAttribute("name"),child.getAttribute("value"));
			}
			eventManager().fireGetServerProperties(
					phoneNumber,
					node.getChild(0).getAttribute("version"),
					props
					);
		}
		if (node.getChild(0) != null && node.getChild(0).getTag().equals("picture")) {
			eventManager().fireGetProfilePicture(
					phoneNumber,
					node.getAttribute("from"),
					node.getChild("picture").getAttribute("type"),
					node.getChild("picture").getData()
					);
		}
		if (node.getChild(0) != null && node.getChild(0).getTag().equals("media")) {
			processUploadResponse(node);
		}
		if (node.getChild(0) != null && node.getChild(0).getTag().equals("duplicate")) {
			processUploadResponse(node);
		}
		if (node.nodeIdContains("group")) {
			//There are multiple types of Group reponses. Also a valid group response can have NO children.
			//Events fired depend on text in the ID field.
			Map<String,String> groupList = new LinkedHashMap<String,String>();
			String groupId = null;
			if (node.getChild(0) != null) {
				for(ProtocolNode child : node.getChildren()) {
					groupList.putAll(child.getAttributes());
				}
			}
			if(node.nodeIdContains("creategroup")){
				groupId = node.getChild(0).getAttribute("id");
				eventManager().fireGroupsChatCreate(
						phoneNumber,
						groupId
						);
			}
			if(node.nodeIdContains("endgroup")){
				groupId = node.getChild(0).getChild(0).getAttribute("id");
				eventManager().fireGroupsChatEnd(
						phoneNumber,
						groupId
						);
			}
			if(node.nodeIdContains("getgroups")){
				eventManager().fireGetGroups(
						phoneNumber,
						groupList
						);
			}
			if(node.nodeIdContains("getgroupinfo")){
				eventManager().fireGetGroupsInfo(
						phoneNumber,
						groupList
						);
			}
			if(node.nodeIdContains("getgroupparticipants")){
				groupId = parseJID(node.getAttribute("from"));
				eventManager().fireGetGroupParticipants(
						phoneNumber,
						groupId,
						groupList
						);
			}

		}
		if (node.getTag().equals("iq") && node.getAttribute("type").equals("error")) {
			serverReceivedId.add(node.getAttribute("id"));
		}

	}

	/**
	 * Process media upload response
	 *
	 * @param ProtocolNode $node
	 *  Message node
	 * @return bool
	 * @throws WhatsAppException 
	 * @throws InvalidTokenException 
	 * @throws InvalidMessageException 
	 * @throws IncompleteMessageException 
	 * @throws IOException 
	 * @throws JSONException 
	 * @throws NoSuchAlgorithmException 
	 * @throws DecodeException 
	 * @throws InvalidKeyException 
	 */
	private boolean processUploadResponse(ProtocolNode node) throws IOException, IncompleteMessageException, InvalidMessageException, InvalidTokenException, WhatsAppException, JSONException, NoSuchAlgorithmException, InvalidKeyException, DecodeException {
		String url = null;
		String filesize = null;
		String filetype = null;
		String filename = null;
		String to = null;
		String id = node.getAttribute("id");
		Map<String, Object> messageNode = mediaQueue.get(id);
		if (messageNode == null) {
			//message not found, can't send!
			eventManager().fireMediaUploadFailed(
					phoneNumber,
					id,
					node,
					messageNode,
					"Message node not found in queue"
					);
			return false;
		}

		ProtocolNode duplicate = node.getChild("duplicate");
		if (duplicate != null) {
			//file already on whatsapp servers
			url = duplicate.getAttribute("url");
			filesize = duplicate.getAttribute("size");
			filetype = duplicate.getAttribute("type");
			String[] exploded = url.split("/");  
			filename = exploded[exploded.length-1];
			mediaInfo = createMediaInfo(duplicate);
		} else {
			//upload new file
			JSONObject json = WhatsMediaUploader.pushFile(node, messageNode, mediaFile.getMediaFile(), phoneNumber);

			if (json == null) {
				//failed upload
				eventManager().fireMediaUploadFailed(
						phoneNumber,
						id,
						node,
						messageNode,
						"Failed to push file to server"
						);
				return false;
			}

			if(log.isDebugEnabled()) {
				log.debug("Setting mediaInfo to: "+json.toString());
			}
			mediaInfo = json;
			url = json.getString("url");
			filesize = json.getString("size");
			filetype = json.getString("type");
			filename = json.getString("name");
		}

		Map<String,String> mediaAttribs = new HashMap<String, String>();
		mediaAttribs.put("xmlns","urn:xmpp:whatsapp:mms");
		mediaAttribs.put("type",filetype);
		mediaAttribs.put("url",url);
		mediaAttribs.put("encoding","raw");
		mediaAttribs.put("file",filename);
		mediaAttribs.put("size",filesize);
		if(messageNode.containsKey("caption") && !((String)messageNode.get("caption")).isEmpty()) {
			mediaAttribs.put("caption", ((String)messageNode.get("caption")));
		}

		to = (String) messageNode.get("to");

		byte[] icon = null;
		if(filetype.equals("image")) {
			icon = readFile(mediaFile.getPreviewFile());
		}
		if(filetype.equals("video")) {
			icon = readFile(mediaFile.getPreviewFile());
		}

		ProtocolNode mediaNode = new ProtocolNode("media", mediaAttribs, null, icon);
		/* 
		 * TODO support multiple recipients
		 */
		//        if (is_array($to)) {
		//            $this->sendBroadcast($to, $mediaNode);
		//        } else {
		//            $this->sendMessageNode($to, $mediaNode);
		//        }
		sendMessageNode(to, mediaNode,null);
		eventManager().fireMediaMessageSent(
				phoneNumber,
				to,
				id,
				filetype,
				url,
				filename,
				filesize,
				icon
				);
		return true;
	}

	private JSONObject createMediaInfo(ProtocolNode duplicate) {
		JSONObject info = new JSONObject();
		Map<String, String> attributes = duplicate.getAttributes();
		for(String key : attributes.keySet()) {
			try {
				info.put(key, attributes.get(key));
			} catch (JSONException e) {
				log.warn("Failed to add "+key+" to media info: "+e.getMessage());
			}
		}
		if(log.isDebugEnabled()) {
			log.debug("Created media info (for duplicate): "+info.toString());
		}
		return info;
	}

	private void sendPong(String msgid) throws IOException, WhatsAppException {
		Map<String,String> messageHash = new LinkedHashMap<String,String>();
		messageHash.put("to",WHATSAPP_SERVER);
		messageHash.put("id",msgid);
		messageHash.put("type","result");

		ProtocolNode messageNode = new ProtocolNode("iq", messageHash, null, null);
		sendNode(messageNode);
		eventManager().fireSendPong(
				phoneNumber, 
				msgid
				);
	}

	private EventManager eventManager() {
		return eventManager ;
	}

	private void file_put_contents(String string, Object challengeData2) {
		// TODO Auto-generated method stub

	}

	private void processChallenge(ProtocolNode node) {
		log.debug("processChallenge: "+node.getData().length);
		challengeData = node.getData();
	}

	private void processPresence(ProtocolNode node) throws WhatsAppException {
		if (node.getAttribute("status") != null && node.getAttribute("status").equals("dirty")) {
			//clear dirty
			List<String> categories = new LinkedList<String>();
			if (node.getChildren() != null && node.getChildren().size() > 0) {
				for(ProtocolNode child : node.getChildren()) {
					if (child.getTag().equals("category")) {
						categories.add(child.getAttribute("name"));
					}
				}
			}
			sendClearDirty(categories);
		}
		String from = node.getAttribute("from");
		String type = node.getAttribute("type");
		if(from != null && type != null) {
			if (from.startsWith(phoneNumber) 
					&& !from.contains("-")) {
				eventManager().firePresence(
						phoneNumber,
						from,
						type
						);
			}
			if(!from.startsWith(phoneNumber)
					&& from.contains("-")) {
				String groupId = parseJID(from);
				if (node.getAttribute("add") != null) {
					eventManager().fireGroupsParticipantsAdd(
							phoneNumber,
							groupId,
							parseJID(node.getAttribute("add"))
							);
				} else {
					if (node.getAttribute("remove") != null) {
						eventManager().fireGroupsParticipantsRemove(
								phoneNumber,
								groupId,
								parseJID(node.getAttribute("remove")),
								parseJID(node.getAttribute("author"))
								);
					}
				}
			}
		}		
	}

	private String parseJID(String attribute) {
		// TODO Auto-generated method stub
		return null;
	}

	private void sendClearDirty(List<String> categories) throws WhatsAppException {
		String msgId = createMsgId("cleardirty");

		List<ProtocolNode> catnodes = new LinkedList<ProtocolNode>();
		for (String category : categories) {
			Map<String,String> catmap = new HashMap<String, String>();
			catmap.put("type", category);
			ProtocolNode catnode = new ProtocolNode("clean", catmap, null, null);
			catnodes.add(catnode);
		}
		Map<String,String> nodemap = new HashMap<String, String>();
		nodemap.put("id", msgId);
		nodemap.put("type", "set");
		nodemap.put("to", WHATSAPP_SERVER);
		nodemap.put("xmlns", "urn:xmpp:whatsapp:dirty");
		ProtocolNode node = new ProtocolNode("iq", nodemap, catnodes, null);
		sendNode(node);
	}

	private void processMessage(ProtocolNode node) throws IOException, WhatsAppException {
		log.debug("processMessage:");
		messageQueue.add(node);

		//do not send received confirmation if sender is yourself
		if(node.getAttribute("type").equals("text")) {
			sendMessageReceived(node,"read");
		}
		if(node.getAttribute("type").equals("media")) {
			processMediaMessage(node);
			sendMessageReceived(node, "read");
		}
		// check if it is a response to a status request
		String[] foo = node.getAttribute("from").split("@");
		if (foo.length > 1 && foo[1].equals("s.us") && node.getChild("body") != null) {
			eventManager().fireGetStatus(
					phoneNumber,
					node.getAttribute("from"),
					node.getAttribute("type"),
					node.getAttribute("id"),
					node.getAttribute("t"),
					node.getChild("body").getData()
					);
		}
		if (node.hasChild("x") && lastId.equals(node.getAttribute("id"))) {
			sendNextMessage();
		}

		if (processor != null && (node.hasChild("body") || node.hasChild("media"))) {
			processor.processMessage(node);
		}

		if (node.hasChild("notify") && node.getChild(0).getAttribute("name") != null &&
				node.getChild(0).getAttribute("name").length() < 1 && node.getChild("body") != null) {
			String author = node.getAttribute("author");
			if(author == null || author.length() < 1)
			{
				//private chat message
				eventManager().fireGetMessage(
						phoneNumber,
						node.getAttribute("from"),
						node.getAttribute("id"),
						node.getAttribute("type"),
						node.getAttribute("t"),
						node.getChild("notify").getAttribute("name"),
						node.getChild("body").getData()
						);
			}
			else
			{
				//group chat message
				eventManager().fireGetGroupMessage(
						phoneNumber,
						node.getAttribute("from"),
						author,
						node.getAttribute("id"),
						node.getAttribute("type"),
						node.getAttribute("t"),
						node.getChild("notify").getAttribute("name"),
						node.getChild("body").getData()
						);
			}
		}
		if (node.hasChild("notification") && node.getChild("notification").getAttribute("type").equals("picture")) {
			if (node.getChild("notification").hasChild("set")) {
				eventManager().fireProfilePictureChanged(
						phoneNumber,
						node.getAttribute("from"),
						node.getAttribute("id"),
						node.getAttribute("t")
						);
			} else if (node.getChild("notification").hasChild("delete")) {
				eventManager().fireProfilePictureDeleted(
						phoneNumber,
						node.getAttribute("from"),
						node.getAttribute("id"),
						node.getAttribute("t")
						);
			}
		}
		if (node.getChild("notify") != null && node.getChild(0).getAttribute("name") != null && node.getChild("media") != null) {
			if (node.getChild(2).getAttribute("type") == "image") {
				eventManager().fireGetImage(
						phoneNumber,
						node.getAttribute("from"),
						node.getAttribute("id"),
						node.getAttribute("type"),
						node.getAttribute("t"),
						node.getChild(0).getAttribute("name"),
						node.getChild(2).getAttribute("size"),
						node.getChild(2).getAttribute("url"),
						node.getChild(2).getAttribute("file"),
						node.getChild(2).getAttribute("mimetype"),
						node.getChild(2).getAttribute("filehash"),
						node.getChild(2).getAttribute("width"),
						node.getChild(2).getAttribute("height"),
						node.getChild(2).getData()
						);
			} 
			if (node.getChild(2).getAttribute("type") == "video") {
				eventManager().fireGetVideo(
						phoneNumber,
						node.getAttribute("from"),
						node.getAttribute("id"),
						node.getAttribute("type"),
						node.getAttribute("t"),
						node.getChild(0).getAttribute("name"),
						node.getChild(2).getAttribute("url"),
						node.getChild(2).getAttribute("file"),
						node.getChild(2).getAttribute("size"),
						node.getChild(2).getAttribute("mimetype"),
						node.getChild(2).getAttribute("filehash"),
						node.getChild(2).getAttribute("duration"),
						node.getChild(2).getAttribute("vcodec"),
						node.getChild(2).getAttribute("acodec"),
						node.getChild(2).getData()
						);
			} else
				if (node.getChild(2).getAttribute("type") == "audio") {
					eventManager().fireGetAudio(
							phoneNumber,
							node.getAttribute("from"),
							node.getAttribute("id"),
							node.getAttribute("type"),
							node.getAttribute("t"),
							node.getChild(0).getAttribute("name"),
							node.getChild(2).getAttribute("size"),
							node.getChild(2).getAttribute("url"),
							node.getChild(2).getAttribute("file"),
							node.getChild(2).getAttribute("mimetype"),
							node.getChild(2).getAttribute("filehash"),
							node.getChild(2).getAttribute("duration"),
							node.getChild(2).getAttribute("acodec")
							);
				} 
			if (node.getChild(2).getAttribute("type") == "vcard") {
				eventManager().fireGetvCard(
						phoneNumber,
						node.getAttribute("from"),
						node.getAttribute("id"),
						node.getAttribute("type"),
						node.getAttribute("t"),
						node.getChild(0).getAttribute("name"),
						node.getChild(2).getChild(0).getAttribute("name"),
						node.getChild(2).getChild(0).getData()
						);
			} 
			if (node.getChild(2).getAttribute("type") == "location") {
				String url = node.getChild(2).getAttribute("url");
				String name = node.getChild(2).getAttribute("name");
				eventManager().fireGetLocation(
						phoneNumber,
						node.getAttribute("from"),
						node.getAttribute("id"),
						node.getAttribute("type"),
						node.getAttribute("t"),
						node.getChild(0).getAttribute("name"),
						name,
						node.getChild(2).getAttribute("longitude"),
						node.getChild(2).getAttribute("latitude"),
						url,
						node.getChild(2).getData()
						);
			}
		}
		if (node.getChild("x") != null) {
			if(log.isDebugEnabled()) {
				log.debug("processMessage: setting received id to "+node.getAttribute("id"));
			}
			serverReceivedId.add(node.getAttribute("id"));
			eventManager().fireMessageReceivedServer(
					phoneNumber,
					node.getAttribute("from"),
					node.getAttribute("id"),
					node.getAttribute("type"),
					node.getAttribute("t")
					);
		}
		if (node.getChild("received") != null) {
			eventManager().fireMessageReceivedClient(
					phoneNumber,
					node.getAttribute("from"),
					node.getAttribute("id"),
					node.getAttribute("type"),
					node.getAttribute("t")
					);
		}
		if (node.getAttribute("type").equals("subject")) {

			String[] reset_from = node.getAttribute("from").split("@");
			String[] reset_author = node.getAttribute("author").split("@");
			eventManager().fireGetGroupsSubject(
					phoneNumber,
					reset_from,
					node.getAttribute("t"),
					reset_author,
					reset_author,
					node.getChild(0).getAttribute("name"),
					node.getChild(2).getData()
					);
		}
	}

	private void processMediaMessage(ProtocolNode node) throws WhatsAppException {
		// TODO Auto-generated method stub
		if(node.getChild(0).getAttribute("type").equals("image") ) {
			String msgId = createMsgId("ack-media");

			Map<String,String> attributes = new HashMap<String,String>();
			attributes.put("url", node.getChild(0).getAttribute("url"));
			ProtocolNode ackNode = new ProtocolNode("ack",attributes, null, null);

			Map<String,String> iqAttributes = new HashMap<String,String>();
			iqAttributes.put("id",msgId);
			iqAttributes.put("xmlns", "w:m");
			iqAttributes.put("type", "set");
			iqAttributes.put("to", WHATSAPP_SERVER);
			List<ProtocolNode> nodeList = new LinkedList<ProtocolNode>();
			nodeList.add(ackNode);
			ProtocolNode iqNode = new ProtocolNode("iq",iqAttributes, nodeList, null);

			sendNode(iqNode);
		}

	}

	private void sendNextMessage() throws IOException, WhatsAppException {
		if (outQueue.size() > 0) {
			ProtocolNode msgnode = outQueue.remove(0);
			msgnode.refreshTimes();
			lastId = msgnode.getAttribute("id");
			sendNode(msgnode);
		} else {
			lastId = null;
		}
	}

	private void sendMessageReceived(ProtocolNode msg, String type) throws IOException, WhatsAppException {
		Map<String,String> messageHash = new LinkedHashMap<String, String>();
		messageHash.put("to",msg.getAttribute("from"));
		if(type != null && type.equals("read"))
			messageHash.put("type","type");

		messageHash.put("id",msg.getAttribute("id"));
		messageHash.put("t",Long.toString(new Date().getTime()));
		ProtocolNode messageNode = new ProtocolNode("receipt", messageHash, null, null);
		sendNode(messageNode);
		eventManager().fireSendMessageReceived(
				phoneNumber, 
				msg.getAttribute("from"),
				messageHash.get("t") 
				);
	}

	private byte[] readData() throws IOException {
		byte[] buf = null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if(socket != null && socket.isConnected()) {
			InputStream stream = socket.getInputStream();
			buf = new byte[3];
			try {
				//Read header first
				int ret = stream.read(buf);
				if(ret == 3) {
					//					log.debug("Read header: "+ProtocolNode.bin2hex(Arrays.copyOf(buf, ret)));
					int treeLength = ((buf[0] & 0x0f) & 0xFF) << 16;
					treeLength += (buf[1] & 0xFF) << 8;
					treeLength += (buf[2] & 0xFF) << 0;
					//					log.debug("Tree length = "+treeLength);
					out.write(buf);
					buf = new byte[treeLength];
					int read = 0;
					while(read < treeLength) {
						ret = stream.read(buf);
						if(ret > 0) {
							//							log.debug("Read content: "+ProtocolNode.bin2hex(Arrays.copyOf(buf, ret)));
							out.write(Arrays.copyOf(buf, ret));
							read += ret;
						} else {
							if(ret == 0) {
								break;
							} else {
								log.error("socket EOF, closing socket...");
								socket.close();
								socket = null;
								disconnect();
							}
						}
					}
				} else {
					if(ret > 0) {
						log.warn("Failed to read stanza header");
					}
					if(ret == -1) {
						log.error("socket EOF, closing socket...");
						socket.close();
						socket = null;
						disconnect();
					}
				}
			} catch (SocketTimeoutException e) {

			}
		}
		byte[] outBytes = out.toByteArray();
		return outBytes;
	}

	private void sendNode(ProtocolNode node) throws WhatsAppException {
		try {
			byte[] data = writer.write(node, true);
			log.debug("tx: "+node.toString());
			sendData(data);
		} catch (Exception e) {
			throw new WhatsAppException("Failed to send node", e);
		}
	}

	private void sendData(byte[] data) throws IOException {
		if(socket != null && socket.isConnected()) {
			socket.getOutputStream().write(data);
		}
	}

	/**
	 * Add the authentication nodes.
	 *
	 * @return ProtocolNode
	 *   Return itself.
	 * @throws EncodeException 
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	private ProtocolNode createAuthNode() throws NoSuchAlgorithmException, EncodeException, IOException
	{
		Map<String, String> attributes = new LinkedHashMap<String, String>();
		//		attributes.put("xmlns", "urn:ietf:params:xml:ns:xmpp-sasl");
		attributes.put("mechanism", "WAUTH-2");
		attributes.put("user",phoneNumber);
		byte[] data;
		data = createAuthBlob();
		ProtocolNode node = new ProtocolNode("auth", attributes, null, data);

		return node;
	}


	private byte[] createAuthBlob() throws EncodeException, IOException, NoSuchAlgorithmException {
		if(challengeData != null) {
			// TODO
			//			byte[] key = pbkdf2("PBKDF2WithHmacSHA1", base64_decode(password), challengeData, 16, 20, true);
			List<byte[]> keys = generateKeys();
			inputKey = new KeyStream(keys.get(2), keys.get(3));
			outputKey = new KeyStream(keys.get(0), keys.get(1));
			reader.setKey(inputKey);
			//			writer.setKey(outputKey);
			Map<String, String> phone = dissectPhone();
			ByteArrayOutputStream array = new ByteArrayOutputStream();
			array.write(phoneNumber.getBytes());
			array.write(challengeData);
			array.write(time().getBytes());
			array.write(WHATSAPP_USER_AGENT.getBytes());
			array.write(" MccMnc/".getBytes());
			array.write(phone.get("mcc").getBytes());
			array.write("001".getBytes());
			log.debug("createAuthBlog: challengeData="+toHex(challengeData));
			log.debug("createAuthBlog: array="+toHex(array.toByteArray()));
			challengeData = null;
			return outputKey.encode(array.toByteArray(), 0, 4, array.size()-4);
		}
		return null;	
	}
	/**
	 * Dissect country code from phone number.
	 *
	 * @return map
	 *   An associative map with country code and phone number.
	 *   - country: The detected country name.
	 *   - cc: The detected country code (phone prefix).
	 *   - phone: The phone number.
	 *   - ISO3166: 2-Letter country code
	 *   - ISO639: 2-Letter language code
	 *   Return null if country code is not found.
	 */
	private Map<String,String> dissectPhone() {
		Map<String,String> ret = new LinkedHashMap<String, String>();
		for(Country country : countries) {
			if(phoneNumber.startsWith(country.getCountryCode())) {
				ret.put("country", country.getName());
				ret.put("cc", country.getCountryCode());
				ret.put("phone", phoneNumber.substring(country.getCountryCode().length()));
				ret.put("mcc", country.getMcc());
				ret.put("ISO3166", country.getIso3166());
				ret.put("ISO639", country.getIso639());
				return ret;
			}
		}
		return null;
	}

	private String time() {
		Date now = new Date();

		return Long.toString(now.getTime()/1000);
	}

	/**
	 * Add stream features.
	 * @param bool $profileSubscribe
	 *
	 * @return ProtocolNode
	 *   Return itself.
	 */
	private ProtocolNode createFeaturesNode(boolean profileSubscribe) {
		LinkedList<ProtocolNode> nodes = new LinkedList<ProtocolNode>();
		ProtocolNode node = new ProtocolNode("readreceipts", null, null, null);
		nodes.add(node);
		if (profileSubscribe) {
			Map<String, String> attributes = new LinkedHashMap<String, String>();
			attributes.put("type", "all");
			ProtocolNode profile = new ProtocolNode("w:profile:picture", attributes, null, null);
			nodes.add(profile);
		}
		node = new ProtocolNode("privacy", null, null, null);
		nodes.add(node);
		node = new ProtocolNode("presence", null, null, null);
		nodes.add(node);
		node = new ProtocolNode("groups_v2", null, null, null);
		nodes.add(node);
		ProtocolNode parent = new ProtocolNode("stream:features", null, nodes, null);

		return parent;
	}


	private void getResponse(String host, Map<String,String> query, final Callback callback) throws JSONException {

        String testUrl= "http://httpbin.org/headers";


		StringBuilder url = new StringBuilder();
		url.append(host);
		String delimiter = "?";
		for(String key : query.keySet()) {
			url.append(delimiter);
			url.append(key);
			url.append("=");
			url.append(query.get(key));
			delimiter = "&";
		}
		if(log.isDebugEnabled()) {
			log.debug("Request: "+url.toString());
		}


// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url.toString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.

                        try {
                            callback.doJob(new JSONObject(response));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                try {
                    callback.doJob(new JSONObject("{err: Error, can't get response}"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        })  {
            @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("User-Agent", WHATSAPP_USER_AGENT);
                params.put("Content-Type", "application/json");
                return params;

            };

        };
// Add the request to the RequestQueue.
        requestQueue.add(stringRequest);

//        JsonObjectRequest request = new JsonObjectRequest(url.toString(), null, future, future){
//        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, testUrl, null, future, future){
//
//                @Override
//                public Map<String, String> getHeaders() throws AuthFailureError {
//                Map<String, String>  params = new HashMap<String, String>();
//                params.put("User-Agent", WHATSAPP_USER_AGENT);
//                params.put("Content-Type", "application/json");
//                return params;
//
//            };
//        };
//        requestQueue.add(request);
//        JSONObject response=null;
//        try {
//             response = future.get(30, TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        } catch (TimeoutException e) {
//            e.printStackTrace();
//        }

//        String resp =target.request(MediaType.APPLICATION_JSON).header("User-Agent", WHATSAPP_USER_AGENT).get(String.class);


	}


	/**
	 * Send node to the servers.
	 *
	 * @param to
	 *   The recipient to send.
	 * @param node
	 *   The node that contains the message.
	 *   @return message id
	 * @throws IOException 
	 * @throws InvalidTokenException 
	 * @throws InvalidMessageException 
	 * @throws IncompleteMessageException 
	 * @throws WhatsAppException 
	 * @throws JSONException 
	 * @throws NoSuchAlgorithmException 
	 * @throws DecodeException 
	 * @throws InvalidKeyException 
	 */
	private String sendMessageNode(String to, ProtocolNode node, String id) throws IOException, IncompleteMessageException, InvalidMessageException, InvalidTokenException, WhatsAppException, JSONException, NoSuchAlgorithmException, InvalidKeyException, DecodeException {
		Map<String,String> messageHash = new LinkedHashMap<String, String>();
		messageHash.put("to",getJID(to));
		if(node.getTag().equals("body")) {
			messageHash.put("type","text");
		} else {
			messageHash.put("type","media");
		}
		messageHash.put("id",(id == null?createMsgId("message"):id));
		messageHash.put("t",time());

		List<ProtocolNode> list = new LinkedList<ProtocolNode>();
		list.add(node);
		ProtocolNode messageNode = new ProtocolNode("message", messageHash, list, null);
		sendNode(messageNode);
		eventManager().fireSendMessage(
				phoneNumber,
				getJID(to),
				messageHash.get("id"),
				node
				);
		return messageHash.get("id");
	}

	private void waitForServer(String id) throws IncompleteMessageException, InvalidMessageException, InvalidTokenException, IOException, WhatsAppException, JSONException, NoSuchAlgorithmException, InvalidKeyException, DecodeException {
//		Date start = new Date();
//		Date now = start;
//		while (!checkReceivedId(id) && (now.getTime() - start.getTime()) < 5000) {
//			if(poller.isAlive()) {
//				try {
//					Thread.sleep(100);
//				} catch (InterruptedException e) {
//				}
//			} else {
//
//				pollMessages();
//			}
//			now = new Date();
//		}
//		if(log.isDebugEnabled()) {
//			log.debug("waitForServer done waiting for "+id);
//		}
	}

	private boolean checkReceivedId(String id) {
		if(log.isDebugEnabled()) {
			log.debug("Checking received id ("+serverReceivedId+" against "+id);
		}
		if(serverReceivedId != null && serverReceivedId.contains(id)) {
			if(log.isDebugEnabled()) {
				log.debug("received id matched");
			}
			serverReceivedId.remove(id);
			return true;
		}
		if(log.isDebugEnabled()) {
			log.debug("received id did NOT match");
		}
		return false;
	}

	public synchronized void pollMessages() throws InvalidKeyException, NoSuchAlgorithmException, IncompleteMessageException, InvalidMessageException, InvalidTokenException, IOException, WhatsAppException, JSONException, DecodeException {

        processInboundData(readData());
	}

	private String createMsgId(String prefix) {
		String msgid = prefix + "-" + time() + "-" + ++messageCounter;

		return msgid;
	}

	private String getJID(String number) {
		if (!number.contains("@")) {
			//check if group message
			if (number.contains("-")) {
				//to group
				number = number + "@" + WHATSAPP_GROUP_SERVER;
			} else {
				//to normal user
				number = number + "@" + WHATSAPP_SERVER;
			}
		}

		return number;	
	}

	/**
	 * Parse the message text for emojis
	 *
	 * This will look for special strings in the message text
	 * that need to be replaced with a unicode character to show
	 * the corresponding emoji.
	 *
	 * Emojis should be entered in the message text either as the
	 * correct unicode character directly, or if this isn't possible,
	 * by putting a placeholder of ##unicodeNumber## in the message text.
	 * Include the surrounding ##
	 * eg:
	 * ##1f604## this will show the smiling face
	 * ##1f1ec_1f1e7## this will show the UK flag.
	 *
	 * Notice that if 2 unicode characters are required they should be joined
	 * with an underscore.
	 *
	 *
	 * @param string txt
	 * The message to be parsed for emoji code.
	 *
	 * @return string
	 */
	private String parseMessageForEmojis(String txt) {
		// TODO Auto-generated method stub
		return txt;
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}

	public String getName() {
		return name;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public EventManager getEventManager() {
		return eventManager;
	}

	public void setEventManager(EventManager eventManager) {
		this.eventManager = eventManager;
	}

	public void setChallengeData(String challenge) {
		challengeData = BinHex.hex2bin(challenge);
	}

	public void setPassword(String pw) {
		this.password = pw;
	}

	public KeyStream getInputKey() {
		return inputKey;
	}

	public void setInputKey(KeyStream inputKey) {
		this.inputKey = inputKey;
	}

	public KeyStream getOutputKey() {
		return outputKey;
	}

}
