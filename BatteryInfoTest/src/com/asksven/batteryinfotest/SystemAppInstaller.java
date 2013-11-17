/**
 * 
 */
package com.asksven.batteryinfotest;

import java.util.List;

import com.asksven.android.common.RootShell;

/**
 * @author sven
 *
 */
public class SystemAppInstaller
{

	static final String APK_NAME 			= "com.asksven.batteryinfotest";
	static final String SYSTEM_DIR 			= "/system/priv-app";
	
	static final String REMOUNT_SYSTEM_RW 	= "mount -o rw,remount /system";
	static final String REMOUNT_SYSTEM_RO 	= "mount -o ro,remount /system";
	// returns ro or rw
	static final String CHECK_MOUNT_STATE 	= "mount | grep /system | awk '{print $4}' | awk -F\",\" '{print $1}'";
	static final String CHECK_SYSTEM_APP_EXISTS 	= "ls " + SYSTEM_DIR + "/" + APK_NAME + "*";
	static final String INSTALL_AS_SYSTEM_APP 		= "cp /data/app/" + APK_NAME + "* " + SYSTEM_DIR + " && chmod 644 /system/priv-app/" + APK_NAME + "*";
	static final String UNINSTALL_AS_SYSTEM_APP 	= "rm " + SYSTEM_DIR + "/" + APK_NAME + "*";
	
	public static boolean mountSystemRw()
	{
		if (isSystemRw()) return true;
		
		RootShell.getInstance().run(REMOUNT_SYSTEM_RW);
		
		return isSystemRw();
		
	}
	
	public static boolean mountSystemRo()
	{
		if (!isSystemRw()) return true;
		
		List<String> res = RootShell.getInstance().run(REMOUNT_SYSTEM_RO);
		
		return !isSystemRw();
		
	}

	public static boolean isSystemRw()
	{
		boolean ret = false;
		List<String> res = RootShell.getInstance().run(CHECK_MOUNT_STATE);
		if (res.size() > 0)
		{
			ret = res.get(0).equals("rw");
		}
		
		return ret;
	}
	
	public static boolean isSystemApp()
	{
		boolean ret = false;
		List<String> res = RootShell.getInstance().run(CHECK_SYSTEM_APP_EXISTS);
		if (res.size() > 0)
		{
			ret = !res.get(0).contains("No such file or directory");
		}
		
		return ret;
	}
	
	public static boolean installAsSystemApp()
	{
		List<String> res = RootShell.getInstance().run(INSTALL_AS_SYSTEM_APP);
		
		return isSystemApp();
	}
	
	public static boolean uninstallAsSystemApp()
	{
		List<String> res = RootShell.getInstance().run(UNINSTALL_AS_SYSTEM_APP);
		
		return !isSystemApp();
	}
}
