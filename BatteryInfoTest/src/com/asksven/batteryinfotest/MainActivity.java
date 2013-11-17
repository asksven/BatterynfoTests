package com.asksven.batteryinfotest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{

	final static String TAG = "BetteryInfoTest.MainActivity";

	Object m_stats = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final Button buttonTest = (Button) findViewById(R.id.button1);
		buttonTest.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				try
				{
					initService();
					//callService();
					getIsOnBattery();
					Toast.makeText(MainActivity.this, "Succeeded", Toast.LENGTH_LONG).show();
				}
				catch (Exception e)
				{
//					Log.e(TAG, e.getMessage());
					Log.e(TAG, "Exception: " + Log.getStackTraceString(e));
					Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_LONG).show();
				}
			}
		});

		final TextView result = (TextView) findViewById(R.id.textViewPerm);
		if (hasBatteryStatsPermission(this))
		{
			result.setText("Granted");
		}
		else
		{
			result.setText("Not granted");
		}

		final Button buttonRemount = (Button) findViewById(R.id.button2);
		setButtonText(buttonRemount);

		final EditText textStatus = (EditText) findViewById(R.id.editTextStatus);
		textStatus.setText("");
		
		buttonRemount.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				try
				{
					if (!SystemAppInstaller.isSystemApp())
					{
						SystemAppInstaller.mountSystemRw();
						if (SystemAppInstaller.isSystemRw())
						{
							appendStatus("Mounted system rw");
							SystemAppInstaller.installAsSystemApp();
							appendStatus("Install as system app");
							if (SystemAppInstaller.isSystemApp())
							{
								SystemAppInstaller.mountSystemRo();
								if (!SystemAppInstaller.isSystemRw())
								{
									appendStatus("Mounted system ro. Finished");
								}
								else
								{
									appendStatus("An error while remounting system to ro. Aborted");
								}
							}
							else
							{
								appendStatus("An error while installing app. Aborted");
							}
							
						}
						else
						{
							appendStatus("An error occured mounting system rw. Aborted");
						}
						
					}
					else
					{
						SystemAppInstaller.mountSystemRw();
						if (SystemAppInstaller.isSystemRw())
						{
							appendStatus("Mounted system rw");
							SystemAppInstaller.uninstallAsSystemApp();
							appendStatus("Uninstall as system app");
							if (!SystemAppInstaller.isSystemApp())
							{
								SystemAppInstaller.mountSystemRo();
								if (!SystemAppInstaller.isSystemRw())
								{
									appendStatus("Mounted system ro. Finished");
								}
								else
								{
									appendStatus("An error while remounting system to ro. Aborted");
								}
							}
							else
							{
								appendStatus("An error while uninstalling app. Aborted");
							}	
						}
						else
						{
							appendStatus("An error occured mounting system rw. Aborted");
						}
					}
						
					setButtonText(buttonRemount);
				
					Toast.makeText(MainActivity.this, "Succeeded", Toast.LENGTH_LONG).show();
				}
				catch (Exception e)
				{
					Log.e(TAG, "Exception: " + Log.getStackTraceString(e));
					Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_LONG).show();
				}
				
				// refresh status of button
				setButtonText(buttonRemount);

			}
		});

	}

	void setButtonText(Button button)
	{
		if (SystemAppInstaller.isSystemApp())
		{
			button.setText("Installed as system app");
		}
		else
		{
			button.setText("Not installed as system app");
		}
	}
	
	void appendStatus(String text)
	{
		final EditText textStatus = (EditText) findViewById(R.id.editTextStatus);

		String current = textStatus.getText().toString();
		current += "\n" + text;
		textStatus.setText(current);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void initService() throws Exception
	{
		ClassLoader cl = this.getClassLoader();
		Class m_ClassDefinition = cl.loadClass("com.android.internal.os.BatteryStatsImpl");

		// get the IBinder to the "batteryinfo" service
		@SuppressWarnings("rawtypes")
		Class serviceManagerClass = cl.loadClass("android.os.ServiceManager");

		// parameter types
		@SuppressWarnings("rawtypes")
		Class[] paramTypesGetService = new Class[1];
		paramTypesGetService[0] = String.class;

		@SuppressWarnings("unchecked")
		Method methodGetService = serviceManagerClass.getMethod("getService", paramTypesGetService);

		// parameters
		Object[] paramsGetService = new Object[1];
		
		String service = "";
		if (Build.VERSION.SDK_INT == 19)
		{
			service = "batterystats";
		}
		else
		{
			service = "batteryinfo";
		}
		paramsGetService[0] = service;

		Log.i(TAG, "invoking android.os.ServiceManager.getService(\"" + service + "\")");

		IBinder serviceBinder = (IBinder) methodGetService.invoke(serviceManagerClass, paramsGetService);

		Log.i(TAG, "android.os.ServiceManager.getService(\"batteryinfo\") returned a service binder");

		// now we have a binder. Let's us that on IBatteryStats.Stub.asInterface
		// to get an IBatteryStats
		// Note the $-syntax here as Stub is a nested class
		@SuppressWarnings("rawtypes")
		Class iBatteryStatsStub = cl.loadClass("com.android.internal.app.IBatteryStats$Stub");

		// Parameters Types
		@SuppressWarnings("rawtypes")
		Class[] paramTypesAsInterface = new Class[1];
		paramTypesAsInterface[0] = IBinder.class;

		@SuppressWarnings("unchecked")
		Method methodAsInterface = iBatteryStatsStub.getMethod("asInterface", paramTypesAsInterface);

		// Parameters
		Object[] paramsAsInterface = new Object[1];
		paramsAsInterface[0] = serviceBinder;

		Log.i(TAG, "invoking com.android.internal.app.IBatteryStats$Stub.asInterface");

		Object iBatteryStatsInstance = methodAsInterface.invoke(iBatteryStatsStub, paramsAsInterface);

		// and finally we call getStatistics from that IBatteryStats to obtain a
		// Parcel
		@SuppressWarnings("rawtypes")
		Class iBatteryStats = cl.loadClass("com.android.internal.app.IBatteryStats");

		@SuppressWarnings("unchecked")
		Method methodGetStatistics = iBatteryStats.getMethod("getStatistics");

		Log.i(TAG, "invoking getStatistics");

		byte[] data = (byte[]) methodGetStatistics.invoke(iBatteryStatsInstance);

		Log.i(TAG, "retrieving parcel");

		Parcel parcel = Parcel.obtain();
		parcel.unmarshall(data, 0, data.length);
		parcel.setDataPosition(0);

		@SuppressWarnings("rawtypes")
		Class batteryStatsImpl = cl.loadClass("com.android.internal.os.BatteryStatsImpl");

		Log.i(TAG, "reading CREATOR field");

		Field creatorField = batteryStatsImpl.getField("CREATOR");

		// From here on we don't need reflection anymore
		@SuppressWarnings("rawtypes")
		Parcelable.Creator batteryStatsImpl_CREATOR = (Parcelable.Creator) creatorField.get(batteryStatsImpl);

		m_stats = batteryStatsImpl_CREATOR.createFromParcel(parcel);

	}

	private void callService() throws Exception
	{
		if (m_stats != null)
		{
			
			ClassLoader cl = this.getClassLoader();
			Class m_ClassDefinition = cl.loadClass("com.android.internal.os.BatteryStatsImpl");

			Long ret = new Long(0);

	          //Parameters Types
	          @SuppressWarnings("rawtypes")
	          Class[] paramTypes= new Class[2];
	          paramTypes[0]= long.class;
	          paramTypes[1]= int.class;          

	          @SuppressWarnings("unchecked")
			  Method method = m_ClassDefinition.getMethod("computeBatteryRealtime", paramTypes);

	          //Parameters
	          Object[] params= new Object[2];
//	          params[0]= new Long(curTime);
//	          params[1]= new Integer(iStatsType);

				ret = (Long) method.invoke(m_stats, params);
		}

	}

	public boolean getIsOnBattery() throws Exception
	{
		boolean ret = true;

		// Parameters Types
		@SuppressWarnings("rawtypes")
		Class[] paramTypes = new Class[2];
		paramTypes[0] = long.class;
		paramTypes[1] = int.class;

		@SuppressWarnings("unchecked")
		ClassLoader cl = this.getClassLoader();

		Class m_ClassDefinition = cl.loadClass("com.android.internal.os.BatteryStatsImpl");

		Method method = m_ClassDefinition.getMethod("getIsOnBattery");

		ret = (Boolean) method.invoke(m_stats);

		return ret;

	}
	
	private boolean hasBatteryStatsPermission(Context context)
	{
		return wasPermissionGranted(context, android.Manifest.permission.BATTERY_STATS);
	}
	
	private boolean wasPermissionGranted(Context context, String permission)
	{
		PackageManager pm = context.getPackageManager();
		int hasPerm = pm.checkPermission(
		    permission, 
		    context.getPackageName());
		return (hasPerm == PackageManager.PERMISSION_GRANTED);
	}

}
