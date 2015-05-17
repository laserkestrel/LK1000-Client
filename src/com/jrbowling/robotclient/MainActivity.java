package com.jrbowling.robotclient;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.util.Log; 
import android.widget.ToggleButton;
import android.widget.SeekBar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.Window;
import android.view.WindowManager;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException; 
import java.net.SocketTimeoutException;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.net.wifi.WifiInfo;  
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException; 
import java.net.SocketTimeoutException;

import android.app.AlertDialog;
import android.content.DialogInterface;

import java.net.SocketAddress;
import java.net.InetSocketAddress;

import android.os.Handler;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class MainActivity extends Activity {
	
	Thread vidThread = null;
	Thread clientThread = null;
	private Socket s;
	
	private static final String DEBUG_TAG = "LK4K Client";
	//private ToggleButton tb;
	private ImageButton forward_button;
	private ImageButton reverse_button;
	private ImageButton right_button;
	private ImageButton left_button;
	private ImageButton movecam_button;
	private SeekBar throttle;
	private SeekBar phone_pan;
	private SeekBar phone_tilt;
	
	private ImageView imageWindow;
	private ImageView connectedLED;
	private ImageView signalStrengthIndicator;
	private TextView batteryInfo;
	
	private Boolean stayConnected = false;
	String vidURL = "";
	private String robotIP = "";
	String direction = "stop";
	Integer speed = 100;
	Integer phonePan = 100;
	Integer phoneTilt = 100;
	Boolean robotEnabled = true;
	Boolean robotConnected = false;
	private Handler GUIUpdateHandler = new Handler();
	private Integer signalStrength = 0;
	private Integer batteryStrength = 0;
	private SharedPreferences pref; 
	private long timeLastPress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //set fullscreeen, black
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        setActivityBackgroundColor(0xff000000);
        
        Log.i(DEBUG_TAG, "Robot client started"); 

        initGUIComponents();
       
        //get Robot IP from user, kick off threads and GUI updater
        showIPAlert();  
        
    }
    
@Override
 protected void onDestroy() {
	
	 Log.d(DEBUG_TAG, "onDestroy() called");
	 stayConnected = false;
	 socketCleanup();
	 super.onDestroy(); 

    }

//@Override
//protected void onPause() {
	
//	 Log.d(DEBUG_TAG, "onPause() called");
//	 stayConnected = false;
//	 socketCleanup();
//	 super.onDestroy();
//   }
    
@Override
 public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

@Override
 public void onBackPressed() {
    	//on back button, prompt user to press it again within 2 seconds to exit
        Toast onBackPressedToast = Toast.makeText(this, "Press again within 2 seconds to confirm exit", Toast.LENGTH_SHORT);
        long currentTime = System.currentTimeMillis();
        if (currentTime - timeLastPress > 2000) {
            onBackPressedToast.show();
            timeLastPress = currentTime;
        } else {
            onBackPressedToast.cancel();  
            super.onBackPressed();           
        }
    }
    
 private void initGUIComponents()
    {
        imageWindow = (ImageView) findViewById(R.id.imageView1);
        connectedLED = (ImageView) findViewById(R.id.imageViewConnectStatus);
        signalStrengthIndicator  = (ImageView) findViewById(R.id.ImageViewSignalStrength);
        forward_button = (ImageButton) findViewById(R.id.forwardButton);
        reverse_button = (ImageButton) findViewById(R.id.reverseButton);
        right_button = (ImageButton) findViewById(R.id.rightButton);
        left_button = (ImageButton) findViewById(R.id.leftButton);
        movecam_button = (ImageButton) findViewById(R.id.moveCamButton);// THIS IS A REFERENCE TO THE GADGET ON LAYOUT
        batteryInfo=(TextView) findViewById(R.id.textViewBatteryInfo);
        throttle = (SeekBar)findViewById(R.id.throttleSeekbar);
        throttle.setProgress(75); 
        phone_pan = (SeekBar)findViewById(R.id.phonePanSeekBar);
        phone_pan.setProgress(75); 
        phone_tilt = (SeekBar)findViewById(R.id.phoneTiltSeekBar);
        phone_tilt.setProgress(75); 
        forward_button.setOnTouchListener(forwardButtonListener);
        reverse_button.setOnTouchListener(reverseButtonListener);
        right_button.setOnTouchListener(rightButtonListener);
        left_button.setOnTouchListener(leftButtonListener);
        movecam_button.setOnTouchListener(moveCamButtonListener);
        
    }
    
    private void showIPAlert() {
    	
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Please enter robot IP address");
        alert.setMessage("Example: 10.20.30.43");

        final EditText input = new EditText(this);
        alert.setView(input);
        input.setText(loadIP().toString());
        
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
        	robotIP = input.getText().toString();
        	Log.i(DEBUG_TAG, "User entered IP " + robotIP);
        	saveIP(robotIP);
        	//Handler launches GUIUpdater every 1000 ms. Launch when user clicks ok.
            updateGUI();
            
            //start network thread
            stayConnected = true;
			
			//launch network thread
			clientThread = new Thread(new ClientThread());
			clientThread.start();
			// AJR: This line is using standalone app IP webcam pro to serve up an image from the Android server" 
			vidURL = "http://"+robotIP+":8080/shot.jpg";
			Log.i(DEBUG_TAG, "http://"+robotIP+":8080/shot.jpg AJR video stream available here");
			vidLoop();
          }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
        	  appExit();
          }
          
        });

        alert.show();	
    }
    
 private void appExit() 
 {
	 Log.i(DEBUG_TAG, "Exit requested by user");
	 this.finish();	 
 }
    
 private OnTouchListener forwardButtonListener = new OnTouchListener(){
        public boolean onTouch(View v, MotionEvent event) {
           switch ( event.getAction() ) {
            case MotionEvent.ACTION_DOWN: 
            	//setActivityBackgroundColor(0xffff0000);
            	direction = "forward";
            	 break;
            case MotionEvent.ACTION_UP: 
            	//setActivityBackgroundColor(0xff000000);
            	direction = "stop";
            	 break;
            }
           return false;
        }  
   };
   
 private OnTouchListener reverseButtonListener = new OnTouchListener(){
       public boolean onTouch(View v, MotionEvent event) {
          switch ( event.getAction() ) {
           case MotionEvent.ACTION_DOWN: 
           	//setActivityBackgroundColor(0xffff0000);
           	direction = "reverse";
           	 break;
           case MotionEvent.ACTION_UP: 
           	//setActivityBackgroundColor(0xff000000);
           	direction = "stop";
           	 break;
           }
          return false;
       }  
  };
  
 private OnTouchListener rightButtonListener = new OnTouchListener(){
      public boolean onTouch(View v, MotionEvent event) {
         switch ( event.getAction() ) {
          case MotionEvent.ACTION_DOWN: 
          	//setActivityBackgroundColor(0xffff0000);
          	direction = "rotateRight";
          	 break;
          case MotionEvent.ACTION_UP: 
          	//setActivityBackgroundColor(0xff000000);
          	direction = "stop";
          	 break;
          }
         return false;
      }  
 };
 
 private OnTouchListener leftButtonListener = new OnTouchListener(){
     public boolean onTouch(View v, MotionEvent event) {
        switch ( event.getAction() ) {
         case MotionEvent.ACTION_DOWN: 
         	//setActivityBackgroundColor(0xffff0000);
         	direction = "rotateLeft";
         	 break;
         case MotionEvent.ACTION_UP: 
         	//setActivityBackgroundColor(0xff000000);
         	direction = "stop";
         	 break;
         }
        return false;
     }  
};

private OnTouchListener moveCamButtonListener = new OnTouchListener(){
    public boolean onTouch(View v, MotionEvent event) {
       switch ( event.getAction() ) {
        case MotionEvent.ACTION_DOWN: 
        	//setActivityBackgroundColor(0xffff0000);
        	direction = "moveCamera";
        	 break;
        case MotionEvent.ACTION_UP: 
        	//setActivityBackgroundColor(0xff000000);
        	direction = "stop";
        	 break;
        }
       return false;
    }  
};


//save and retrieve IP address using the shared preferences framework
 private void saveIP(String IP)
 {
	//set up shared preferences editor
     pref = getApplicationContext().getSharedPreferences("RobotClient", 0); 
     Editor editor = pref.edit();
	 editor.putString("robotIP", IP); 
	 editor.commit();
 }
 
 private String loadIP()
 {
 String result;
 
 pref = getApplicationContext().getSharedPreferences("RobotClient", 0); 
 Editor editor = pref.edit();
 result = pref.getString("robotIP", "10.20.30.43");
 
 return result;
 }
   
 private void setActivityBackgroundColor(int color) {
    	//0xff00ff00  first two are transparency, then rgb
    	
        View view = this.getWindow().getDecorView();
        view.setBackgroundColor(color);
    }
 
 //periodically run updater to set connection status and wifi signal strength from robot
 private void updateGUI()
    {
    	GUIUpdateHandler.postDelayed(GUIUpdater, 1000);
    }
    
 private Runnable GUIUpdater = new Runnable(){

        public void run() {
        	//Periodically update GUI elements from sensor and other data
        	//Log.d(DEBUG_TAG, "Connected is: " + robotConnected.toString());
        	
        	//update connection status
        	if (robotConnected)
        	{
        		connectedLED.setImageResource(R.drawable.led_green);
        		batteryInfo.setText("Batt:"+batteryStrength+"%\n");
        		
        		//update the wifi signal strength indicator
            	if ((signalStrength == 5) || (signalStrength==4))
            		signalStrengthIndicator.setImageResource(R.drawable.wifi4);
            	
            	if (signalStrength == 3) 
            			signalStrengthIndicator.setImageResource(R.drawable.wifi3);

            	if (signalStrength == 2) 
        			signalStrengthIndicator.setImageResource(R.drawable.wifi2);
            	
            	if (signalStrength == 1) 
        			signalStrengthIndicator.setImageResource(R.drawable.wifi1);
            	
            	if (signalStrength == 0) 
        			signalStrengthIndicator.setImageResource(R.drawable.wifi0);
        	}
        	
        	else
        		{
        		connectedLED.setImageResource(R.drawable.led_red);
        		signalStrengthIndicator.setImageResource(R.drawable.wifi0);
        		}
        	
        	if (stayConnected)       	
        		updateGUI();
        }

    };
    
 public void setConnected(boolean connected) {
    		
    robotConnected = connected;
    }
    
 public void socketCleanup()
    {
    	try {
    	Log.d(DEBUG_TAG, "Socket Closing");
    	if (s != null)
    		s.close();
    	setConnected(false);
    	} catch (IOException e) {
       	
       	Log.d(DEBUG_TAG, "Client comm thread got IOException in socketCleanup().");
       	} 	 	
    }
    
 private void vidLoop() //started from GUI alert, then kept going with call me from asynctask
    {
    	if (stayConnected)
    		{
    		ImageDownloader id = new ImageDownloader();
    		id.execute(vidURL); 
    		}
    }
     
    //this very useful chunk of code is from http://www.peachpit.com/articles/article.aspx?p=1823692&seqNum=3
 private class ImageDownloader extends AsyncTask<String, Integer, Bitmap>{
    protected void onPreExecute(){
            
        }
        @Override
        protected Bitmap doInBackground(String... params) {
            try{
                URL url = new URL(params[0]);
                HttpURLConnection httpCon = 
                (HttpURLConnection)url.openConnection();
                if(httpCon.getResponseCode() != 200)
                    throw new Exception("Failed to connect");
                InputStream is = httpCon.getInputStream();
                return BitmapFactory.decodeStream(is);
            }catch(Exception e){
                Log.e(DEBUG_TAG, "Failed to load image",e);
            }
            return null;
        } 
        
        protected void onProgressUpdate(Integer... params){
            //Update a progress bar here, or ignore it, it's up to you
        }
        protected void onPostExecute(Bitmap img){
            ImageView iv = (ImageView)findViewById(R.id.imageView1);
            if(iv!=null && img !=null){
                iv.setImageBitmap(img);
                //start next image grab
                vidLoop();
            }
        }
            protected void onCancelled(){
            }
        }
   
class ClientThread implements Runnable {
		
	  	private static final int SERVERPORT = 8082;
	
	    public void run() {
	    	Log.d(DEBUG_TAG, "clientThread started");
	    	setConnected(false);
	    	while ((!Thread.currentThread().isInterrupted()) && stayConnected)
	    	{
	    	controlLoop();
	    	//stayConnected = false; //AJR: Had this line uncommented..makes connection massively unreliable.. 
	    	}
	    
	    	//user requested disconnect
	    	Log.d(DEBUG_TAG, "clientThread ending");
	    }
	    
	    void controlLoop()
	    {
	    	BufferedReader s_input = null;
	    	PrintWriter s_output = null;
	    	String inputString = "0,0";
	    	String outputString = null;
	    	Boolean continueLoop = true;
	    	//String st = null;
	    	//CLIENT CRASHES WITH NULL POINTER EXCEPTION...I think its around this shit.
	    	Log.d(DEBUG_TAG, "controlLoop starting");
	    	
	    	 //protocol:
            //Java boolean: enabled or disabled 
            //Directions: stop, rotateRight, rotateLeft, forward, reverse
            //Client sends: robotEnabled,direction,servoPanValue
            //Server replies: sensor1,sensor2...
	    	
	    	continueLoop = true;
	    	
	    	try {
	    		
                InetAddress serverAddr = InetAddress.getByName(robotIP);
                Socket s = new Socket();
                int timeout = 2000;   // milliseconds
                SocketAddress sockaddr = new InetSocketAddress(serverAddr, SERVERPORT);
                s.connect(sockaddr, timeout);
                                
                // Setup Input and Output stream on the connection
                s_input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                s_output = new PrintWriter(s.getOutputStream(), true);
                
                
            	} catch (UnknownHostException e1) {
            		e1.printStackTrace();
            		Log.d(DEBUG_TAG, "Got invalid IP string in client thread");
            		continueLoop = false;
            	} catch (IOException e) {
            			Log.d(DEBUG_TAG, "Socket setup caught IOexception");
            			continueLoop = false;

            		}
	    	setConnected(true);
	    	
	    	try {
	    		
	    		while ((stayConnected) && (continueLoop)) {	
	    		
	    			speed = throttle.getProgress();
	    			phonePan = phone_pan.getProgress();
	    			phoneTilt = phone_tilt.getProgress();
	    			
	    			if (stayConnected)
	    				{
	    				outputString = robotEnabled.toString() + "," + direction.toString() + "," + speed.toString() + "," + phonePan.toString() + "," + phoneTilt.toString();
	    				//Log.d(DEBUG_TAG, "Client sends: " + outputString.toString());
	    				}
	    			else 
	    				outputString = "quit";
	    			
	    			s_output.println(outputString);
	    			
	    			if (!s_output.checkError())
	    				{
	    				inputString = s_input.readLine();
                         
	    				if (inputString == null)
	    					{
	    					continueLoop = false;
	    					Log.d(DEBUG_TAG, "Unexpected disconnection - sensor values from server were empty");
	    					}
	    				else
	    				{
	    				Log.d(DEBUG_TAG, "Client got: " + inputString.toString());

						// parse input line from LK server
						String[] separated = inputString.split(",");
	    				
	    				if (separated.length == 2)
						{
							signalStrength = Integer.valueOf(separated[0]);
							batteryStrength = Integer.valueOf(separated[1]);
							Log.d(DEBUG_TAG, "Client picked up sensor values ." + signalStrength + "and" +batteryStrength);
 
							// example st input is "4,45"
						} 
	    				else 
						{
							// we got a bad command string. All stop.
							//direction = "stop";
							//robotEnabled = false;
							Log.d(DEBUG_TAG, "WARNING invalid sensor data from server.");
						}	
	    				}}
	    				
	    			else
	    				{
	    				//printwriter.checkError returned true, something bad happened network-wise
	    				//continueLoop = false;
    					Log.d(DEBUG_TAG, "Printwriter.checkError() returned true, likely network problem");
	    				}
	    		}
	    	
             socketCleanup();
             Log.d(DEBUG_TAG, "controlLoop ending");
             
	    	} catch (IOException e) {
            	//this happens if the connection times out.
            	Log.d(DEBUG_TAG, "Client comm thread got IOException in control loop.");
            	socketCleanup();
	    	} 	 

	    }//end control loop
	    
	} //end client thread
	        
}
