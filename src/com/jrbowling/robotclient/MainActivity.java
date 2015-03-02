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
	
	private static final String TAG = "RobotClient";
	//private ToggleButton tb;
	private ImageButton forward_button;
	private ImageButton reverse_button;
	private ImageButton right_button;
	private ImageButton left_button;
	private SeekBar throttle;
	
	private ImageView imageWindow;
	private ImageView connectedLED;
	private ImageView signalStrengthIndicator;
	
	private Boolean stayConnected = false;
	String vidURL = "";
	private String robotIP = "";
	String direction = "stop";
	Integer speed = 100;
	Boolean robotEnabled = true;
	Boolean robotConnected = false;
	private Handler GUIUpdateHandler = new Handler();
	private Integer signalStrength = 0;
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
        
        Log.i(TAG, "Robot client started"); 

        initGUIComponents();
       
        //get Robot IP from user, kick off threads and GUI updater
        showIPAlert();  
        
    }
    
@Override
 protected void onDestroy() {
	
	 Log.d(TAG,"onDestroy() called");
	 stayConnected = false;
	 socketCleanup();
	 super.onDestroy(); 

    }

//@Override
//protected void onPause() {
	
//	 Log.d(TAG,"onPause() called");
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
        throttle = (SeekBar)findViewById(R.id.throttleSeekbar);
        throttle.setProgress(75); 
        forward_button.setOnTouchListener(forwardButtonListener);
        reverse_button.setOnTouchListener(reverseButtonListener);
        right_button.setOnTouchListener(rightButtonListener);
        left_button.setOnTouchListener(leftButtonListener);
    }
    
    private void showIPAlert() {
    	
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Please enter robot IP address");
        alert.setMessage("Example: 192.168.1.100");

        final EditText input = new EditText(this);
        alert.setView(input);
        input.setText(loadIP().toString());
        
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
        	robotIP = input.getText().toString();
        	Log.i(TAG, "User entered IP " + robotIP);
        	saveIP(robotIP);
        	//Handler launches GUIUpdater every 1000 ms. Launch when user clicks ok.
            updateGUI();
            
            //start network thread
            stayConnected = true;
			
			//launch network thread
			clientThread = new Thread(new ClientThread());
			clientThread.start();
			
			vidURL = "http://"+robotIP+":6001/shot.jpg";
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
	 Log.i(TAG, "Exit requested by user");
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
 result = pref.getString("robotIP", "192.168.1.4");
 
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
        	Log.d(TAG,"Connected is: " + robotConnected.toString());
        	
        	//update connection status
        	if (robotConnected)
        	{
        		connectedLED.setImageResource(R.drawable.led_green);
        		
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
    	Log.d("clientThread","Socket Closing");
    	if (s != null)
    		s.close();
    	setConnected(false);
    	} catch (IOException e) {
       	
       	Log.d(TAG, "Client comm thread got IOException in socketCleanup().");
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
            //TODO Auto-generated method stub
            try{
                URL url = new URL(params[0]);
                HttpURLConnection httpCon = 
                (HttpURLConnection)url.openConnection();
                if(httpCon.getResponseCode() != 200)
                    throw new Exception("Failed to connect");
                InputStream is = httpCon.getInputStream();
                return BitmapFactory.decodeStream(is);
            }catch(Exception e){
                Log.e("Image","Failed to load image",e);
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
		
	  	private static final int SERVERPORT = 6000;
	
	    public void run() {
	    	Log.d("clientThread","clientThread started");
	    	setConnected(false);
	    	
	    	while ((!Thread.currentThread().isInterrupted()) && stayConnected)
	    	{
	    	controlLoop();
	    	}
	    
	    	//user requested disconnect
	    	Log.d("clientThread","clientThread ending");
	    }
	    
	    void controlLoop()
	    {
	    	BufferedReader s_input = null;
	    	PrintWriter s_output = null;
	    	String inputString = null;
	    	String outputString = null;
	    	Boolean continueLoop = true;
	    	
	    	Log.d("clientThread","controlLoop starting");
	    	
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
                
                s_input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                s_output = new PrintWriter(s.getOutputStream(), true);
                
            	} catch (UnknownHostException e1) {
            		e1.printStackTrace();
            		Log.d("clientThread","Got invalid IP string in client thread");
            		continueLoop = false;
            	} catch (IOException e) {
            			Log.d("clientThread","Socket setup caught IOexception");
            			continueLoop = false;

            		}

	    	Log.d("clientThread","Socket Established");
	    	
	    	setConnected(true);
	    	
	    	try {
	    		
	    		while ((stayConnected) && (continueLoop)) {	
	    		
	    			speed = throttle.getProgress();
	    			
	    			if (stayConnected)
	    				outputString = robotEnabled.toString() + "," + direction.toString() + "," + speed.toString(); 
	    			else 
	    				outputString = "quit";
	    			
	    			s_output.println(outputString);
	    			
	    			if (!s_output.checkError())
	    				{
	    				inputString = s_input.readLine();
                         
	    				if (inputString == null)
	    					{
	    					continueLoop = false;
	    					Log.d("clientThread","Unexpected disconnection.");
	    					}
	    				else
	    				{
	    				Log.d("clientThread","Client got: " + inputString.toString());
	    				//parse returned string, which is just an integer containing the signal strength
	    				try {
	    				    signalStrength = Integer.parseInt(inputString);
	    					} catch(NumberFormatException nfe) {
	    						Log.d(TAG,"Got invalid signal strength from client");
	    					} 
	    					}
	    				}
	    			else
	    				{
	    				//printwriter.checkError returned true, something bad happened network-wise
	    				continueLoop = false;
    					Log.d("clientThread","Printwriter.checkError() returned true, likely network problem");
	    				}
	    		}
	    	
             socketCleanup();
             Log.d("clientThread","controlLoop ending");
             
	    	} catch (IOException e) {
            	//this happens if the connection times out.
            	Log.d("clientThread", "Client comm thread got IOException in control loop.");
            	socketCleanup();
	    	} 	 
             
	    }//end control loop
	    
	} //end client thread
	        
}
