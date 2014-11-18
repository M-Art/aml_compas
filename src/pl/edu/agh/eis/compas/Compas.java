package pl.edu.agh.eis.compas;

import pl.edu.agh.eid.compas.R;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class Compas extends Activity implements SensorEventListener {
	
	public final static String TAG = "Rotation";

	// define the display assembly compass picture
	private ImageView image;

	// record the compass picture angle turned
	private float currentDegree = 0f;

	// device sensor manager
	private SensorManager mSensorManager;
	
	// accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];
    
    // real rotation matrix, dependant on screen orientation
    private float[] realRotationMatrix = new float[9];
    
    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];
    
    // magnetic field vector
    private float[] magnet = new float[3];
    
    // accelerometer vector
    private float[] accel = new float[3];
    
    //coordinate rotation
    int COORDINATES_ROTATION_X;
    int COORDINATES_ROTATION_Y; 

	TextView tvHeading;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_compas);

		// our compass image
		image = (ImageView) findViewById(R.id.imageViewCompass);

		// TextView that will tell the user what degree is he heading
		tvHeading = (TextView) findViewById(R.id.tvHeading);

		// initialize your android device sensor capabilities
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		
		Display display = getWindowManager().getDefaultDisplay(); 
        int deviceRot = display.getRotation();

        //Decide how to remap coordinate system
        switch (deviceRot)
        {
            // natural 
            case Surface.ROTATION_0: 
            	COORDINATES_ROTATION_X = SensorManager.AXIS_X;
            	COORDINATES_ROTATION_Y = SensorManager.AXIS_Y;
            	Log.d(TAG, "Natural position.");
            break;
            // rotated left )
            case Surface.ROTATION_90: 
            	COORDINATES_ROTATION_X = SensorManager.AXIS_Y;
        		COORDINATES_ROTATION_Y = SensorManager.AXIS_MINUS_X;
        		Log.d(TAG, "Rotation 90 degrees.");
            break;
            // upside down
            case Surface.ROTATION_180: 
            	COORDINATES_ROTATION_X = SensorManager.AXIS_MINUS_X;
        		COORDINATES_ROTATION_Y =SensorManager.AXIS_MINUS_Y;
        		Log.d(TAG, "Rotation 180 degrees.");
            break;
            // rotated right
            case Surface.ROTATION_270: 
            	COORDINATES_ROTATION_X = SensorManager.AXIS_MINUS_Y;
        		COORDINATES_ROTATION_Y = SensorManager.AXIS_X;
        		Log.d(TAG, "Rotation 270 degrees.");
            break;

            default:  break;
        }
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		// for the system's orientation sensor registered listeners
		mSensorManager.registerListener(this, 
				mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this,
	            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
	            SensorManager.SENSOR_DELAY_GAME);
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		// to stop the listener and save battery
		mSensorManager.unregisterListener(this);
	}

	// zmienne stanu
    private double q = 0.1;
    private double r = 1000;
    private double k = 0;
    private double p = 0;
    private double x1 = 0; // poprzedni stan
    private double x2 = 0; // stan poprzedzający stan x1

    // żeby usunąć problem z przeskakiwaniem kompasu
    private double odejmij(double x, double y)
    {
    	double ret = x - y;
    	
    	if (ret >= 180)
    	{
    		ret = ret - 360;
    	}
    	else if (ret < -180)
    	{
    		ret = ret + 360;
    	}
    	
    	return ret;
    }
    
    // w razie gdyby wartość wyszła poza zakres [0, 360)
    private double poprawZakres(double x) {
    	double ret = x;
    	
        if (x >= 360) {
        	ret = x - 360;
        }
        else if (x < 0) {
        	ret = x + 360;
        }
        
        return ret;
    }

	@Override
	public void onSensorChanged(SensorEvent event) {
		final float alpha = 0.96f;
		
		switch(event.sensor.getType()) {
		case Sensor.TYPE_MAGNETIC_FIELD:
            // copy new magnetometer data into magnet array
            System.arraycopy(event.values, 0, magnet, 0, 3);
            break;
		case Sensor.TYPE_ACCELEROMETER:
            System.arraycopy(event.values, 0, accel, 0, 3);
            break;
		}
		
		if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {	
			SensorManager.remapCoordinateSystem(rotationMatrix,
                    COORDINATES_ROTATION_X, COORDINATES_ROTATION_Y,
                    realRotationMatrix);
			
	        SensorManager.getOrientation(realRotationMatrix, accMagOrientation);
	        double degree =  Math.toDegrees(accMagOrientation[0]);
	        if (degree < 0.0f) {
	        	degree += 360.0f;
	        }
	        
	        degree = (Math.round(degree*100.0))/100.0;
	        
	        double v = odejmij(x1, x2); // prędkość
	        double x; // x1 i x2 przechowują stan, x jest tylko pomocnicze
	        x = x1 + 0.9 * v; // x to stan poprzedni + prędkość, współczynnik 0.9 sobie wymyśliłem

            x = poprawZakres(x); // x mogło wyjść poza [0, 360)
	        
	        // kalman
	        p = p + q;
	        k = p / (p + r);
	        x = x + k * (odejmij(degree, x));
            x = poprawZakres(x); // x mogło wyjść poza [0, 360)

	        p = (1 - k) * p;
	        
	        // zapisanie do zmiennych stanu
	        x2 = x1;
	        x1 = x;
	        		
	    	// do dalszych obliczeń w tej metodzie
	    	degree = x;
	    	        
	        
	        tvHeading.setText("Heading: " + Double.toString(degree) + " degrees");

			// create a rotation animation (reverse turn degree degrees)
			RotateAnimation ra = new RotateAnimation(
					currentDegree, 
					(float) -degree,
					Animation.RELATIVE_TO_SELF, 0.5f, 
					Animation.RELATIVE_TO_SELF,
					0.5f);

			// how long the animation will take place
			ra.setDuration(210);

			// set the animation after the end of the reservation status
			ra.setFillAfter(true);

			// Start the animation
			image.startAnimation(ra);
			currentDegree = (float) -degree;
	    }

		

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// not in use
	}
}
