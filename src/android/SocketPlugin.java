package com.tlantic.plugins.socket;

import android.annotation.SuppressLint;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * @author viniciusl
 *
 * Plugin to handle TCP socket connections.	
 */
/**
 * @author viniciusl
 *
 */
public class SocketPlugin extends CordovaPlugin {

	private Map<String, Connection> pool = new HashMap<String,Connection>();		// pool of "active" connections

	/* (non-Javadoc)
	 * @see org.apache.cordova.CordovaPlugin#execute(java.lang.String, org.json.JSONArray, org.apache.cordova.CallbackContext)
	 */
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

		if (action.equals("connect")) {
			this.connect(args, callbackContext);
			return true;

		}else if(action.equals("send")) {
			this.send(args, callbackContext);
			return true;

		} else if (action.equals("disconnect")) {
			this.disconnect(args, callbackContext);
			return true;

		} else if (action.equals("disconnectAll")) {
			this.disconnectAll(callbackContext);
			return true;

		}  else {
			return false;
		}
	}

	/**
	 * Build a key to identify a socket connection based on host and port information.
	 * 
	 * @param host Target host
	 * @param port Target port
	 * @return connection key
	 */
	@SuppressLint("DefaultLocale")
	private String buildKey(String host, int port) {
		return (host.toLowerCase() + ":" + port);
	}

	/**
	 * Opens a socket connection.
	 * 
	 * @param args
	 * @param callbackContext
	 */
	private void connect (JSONArray args, CallbackContext callbackContext) {
		String key;
		String host;
		int port;
		Connection socket;

		// validating parameters
		if (args.length() < 2) {
			callbackContext.error("Missing arguments when calling 'connect' action.");
		} else {

			// opening connection and adding into pool
			try {

				// preparing parameters
				host = args.getString(0);
				port = args.getInt(1);
				key = this.buildKey(host, port);

				// creating connection
				if (this.pool.get(key) == null) {
					socket = new Connection(this, host, port);
					socket.start();
					this.pool.put(key, socket);
				}

				// adding to pool
				callbackContext.success(key);

			} catch (JSONException e) {
				callbackContext.error("Invalid parameters for 'connect' action: " + e.getMessage());
			}
		}
	}

	/**
	 * Send information to target host
	 * 
	 * @param args
	 * @param callbackContext
	 */
	private void send(JSONArray args, CallbackContext callbackContext) {
		Connection socket;
		
		// validating parameters
		if (args.length() < 2) {
			callbackContext.error("Missing arguments when calling 'send' action.");
		} else {
			try {
				// retrieving parameters
				String key = args.getString(0);
				String data = args.getString(1);
                                String format = args.getString(2);
				
				// getting socket
				socket = this.pool.get(key);
				
				// checking if socket was not found and his connectivity
				if (socket == null) {
					callbackContext.error("No connection found with host " + key);
				
				} else if (!socket.isConnected()) {
					callbackContext.error("Invalid connection with host " + key);
				
				} else if (data.length() == 0) {
					callbackContext.error("Cannot send empty data to " + key);
				
				} else {
				
                                    if (format.equals("base64")) {
                                        data = "! 0 200 200 800 1\r\nLABEL\r\nCONTRAST 0\r\nTONE -30\r\nSPEED 2\r\nPAGE-WIDTH 304\r\nBAR-SENSE\r\nPREFEED -25\r\n;// PAGE 0000000003000800\r\nRIGHT \r\nT90 4 1 45 72 33\r\nLEFT\r\nVB 128 1 0 60 224 756 0000000000025\r\nT90 7 0 264 408 0000000000025\r\nT90 4 0 8 780 Ervilha Torta\r\nT90 4 0 58 780 kg\r\nT90 4 0 108 780 \r\nT90 4 0 158 780 \r\nRIGHT \r\nT90 7 0 239 125 10015\r\nT90 7 0 264 150 2016/05/04\r\nLEFT\r\nT90 4 1 90 400 R$\r\nRIGHT 53 -1\r\nT90 4 4 50 560 33,\r\nLEFT \r\n\r\nFORM\r\nPRINT\r\n";
                                        /*byte[] stringBytes = data.getBytes(Charset.forName("Windows-1252"));
                                        String encodedData = Base64.encodeToString(stringBytes, Base64.DEFAULT);
                                        byte[] decodedData = Base64.decode(encodedData, Base64.DEFAULT);
                                        data = new String(decodedData);*/
                                    }
                                    socket.write(data);
					
					// ending send process
					callbackContext.success();	
				}
								
			} catch (JSONException e) {
				callbackContext.error("Unexpected error sending information: " + e.getMessage());
			}
		}
	}

	/**
	 * Closes an existing connection
	 * 
	 * @param args
	 * @param callbackContext
	 */
	private void disconnect (JSONArray args, CallbackContext callbackContext) {
		String key;
		Connection socket;

		// validating parameters
		if (args.length() < 1) {
			callbackContext.error("Missing arguments when calling 'disconnect' action.");
		} else {

			try {
				// preparing parameters
				key = args.getString(0);

				// getting connection from pool
				socket = pool.get(key);

				// closing socket
				if (socket != null) {
					
					// checking connection
					if (socket.isConnected()) {
						socket.close();
					}
					
					// removing from pool
					pool.remove(key);
				}

				// ending with success
				callbackContext.success("Disconnected from " + key);

			} catch (JSONException e) {
				callbackContext.error("Invalid parameters for 'connect' action:" + e.getMessage());
			}
		}		
	}

	/**
	 * Closes all existing connections
	 * 
	 * @param callbackContext
	 */
	private void disconnectAll (CallbackContext callbackContext) {
		// building iterator
		Iterator<Entry<String, Connection>> it = this.pool.entrySet().iterator();
		
		while( it.hasNext() ) {
			
			// retrieving object
			Map.Entry<String, Connection> pairs = (Entry<String, Connection>) it.next();
			Connection socket = pairs.getValue();
			
			// checking connection
			if (socket.isConnected()) {
				socket.close();
			}
			
			// removing from pool
			this.pool.remove(pairs.getKey());
		}
		
		callbackContext.success("All connections were closed.");
	}


	/**
	 * Callback for Connection object data receive. Relay information to javascript object method: window.tlantic.plugins.socket.receive();
	 * 
	 * @param host
	 * @param port
	 * @param chunk
	 */
	public synchronized void sendMessage(String host, int port, String chunk) {
		final String receiveHook = "window.tlantic.plugins.socket.receive('" + host + "'," + port + ",'" + this.buildKey(host, port) + "','" + chunk + "');";
		
		cordova.getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				webView.loadUrl("javascript:" + receiveHook);
			}
			
		});
	}
	
}