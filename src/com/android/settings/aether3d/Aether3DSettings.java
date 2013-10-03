/*
 * Copyright (C) 2011 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.aether3d;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.util.DisplayMetrics;
import android.util.Log;

import com.android.settings.R;

public class Aether3DSettings  extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
	
	private static final String TAG = "Aether3DSettings";
	
	private static final int TRANS_CODE_UPDATE_AR_SETTINGS = 2100;
	private static final int TRANS_CODE_READ_AR_SETTINGS   = 2101;
	
	private float mScreenWidthInMM = 0f;
	
	
//	typedef struct _ARCFG {
//		bool isSensorEnabled;
//		int version;
//		int shaderType;
//	    int camFOV;	
//		float zscale;
//		float camRotation;
//		float camDistance;
//		float sensorResetAcceleration;
//	} ARCFG;
	
	private static class ARConfig {
		boolean isMirrorEnabled;	
		boolean isSensorEnabled;
		int shaderType;
		int camFOV;
		float zscale;
		float camRotation;
		float camDistance;
		float sensorResetAcceleration;	
				
		public ARConfig(Parcel data) {
			isSensorEnabled = data.readInt() != 0;
			shaderType = data.readInt();
			camFOV = data.readInt();
			zscale = data.readFloat();
			camRotation = data.readFloat();
			camDistance = data.readFloat();
			sensorResetAcceleration = data.readFloat();
			isMirrorEnabled = data.readInt() != 0;
		}
		
		public void write(Parcel data) {
		    data.writeInt(isSensorEnabled ? 1 : 0);
		    data.writeInt(shaderType);
		    data.writeInt(camFOV);	
		    data.writeFloat(zscale);
		    data.writeFloat(camRotation);
		    data.writeFloat(camDistance);
		    data.writeFloat(sensorResetAcceleration);	
		    data.writeInt(isMirrorEnabled ? 1 : 0);		
		}
	}
	
	private ARConfig mConfig = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScreenWidthInMM = getMaxScreenWidthInMM(getResources());
        addPreferencesFromResource(R.xml.aether3d);
        
        SeekBarDialogPreference camPref = (SeekBarDialogPreference) findPreference("ui_3d_cam_position");
        camPref.setMax((int)mScreenWidthInMM);
        
        loadSettings();
        PreferenceScreen prefScreen = getPreferenceScreen();
        for (int i = 0; i < prefScreen.getPreferenceCount(); i++) {
        	prefScreen.getPreference(i).setOnPreferenceChangeListener(this);
        }
		setMirrorEnabled(mConfig.isMirrorEnabled);
    }        
    
	@Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();
		
		if ("ui_3d_enable_mirror".equals(key)) {
			mConfig.isMirrorEnabled = (Boolean) newValue;
			setMirrorEnabled(mConfig.isMirrorEnabled);
		} else if ("ui_3d_enable_sensor".equals(key)) {
			mConfig.isSensorEnabled = (Boolean) newValue;
		} else if ("ui_3d_cam_fov".equals(key)) {
			mConfig.camFOV = (Integer)newValue;
		} else if ("ui_3d_z_scale".equals(key)) {
			mConfig.zscale = (Integer)newValue / 100f;
		} else if ("ui_3d_cam_rotation".equals(key)) {
			mConfig.camRotation = (Integer)newValue / 10f;
		} else if ("ui_3d_cam_position".equals(key)) {
			mConfig.camDistance = (Integer)newValue / mScreenWidthInMM;
		} else if ("ui_3d_sensor_reset_acceleration".equals(key)) {
			mConfig.sensorResetAcceleration = (Integer)newValue;
		} else if ("ui_3d_shader_type".equals(key)) {
			mConfig.shaderType = Integer.parseInt(newValue.toString());
		} else {
			return false;
		}
		
		saveSettings();
		return true;
	}
	
	private void setMirrorEnabled(boolean enabled) {
        PreferenceScreen prefScreen = getPreferenceScreen();
        for (int i = 1; i < prefScreen.getPreferenceCount() - 1; i++) {
        	prefScreen.getPreference(i).setEnabled(enabled);
        }
	}

	private void loadSettings() {
        try {
        	Log.d(TAG, "loading settings from surface flinger...");
        	PreferenceScreen prefScreen = getPreferenceScreen();
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                flinger.transact(TRANS_CODE_READ_AR_SETTINGS, data, reply, 0);
                
                ARConfig config = new ARConfig(reply);
                ((TwoStatePreference)prefScreen.findPreference("ui_3d_enable_mirror")).setChecked(config.isMirrorEnabled);
                ((TwoStatePreference)prefScreen.findPreference("ui_3d_enable_sensor")).setChecked(config.isSensorEnabled);
                ((SeekBarDialogPreference)prefScreen.findPreference("ui_3d_cam_fov")).setValue(config.camFOV);
                ((SeekBarDialogPreference)prefScreen.findPreference("ui_3d_z_scale")).setValue((int)(config.zscale * 100));
                ((SeekBarDialogPreference)prefScreen.findPreference("ui_3d_cam_rotation")).setValue((int)(config.camRotation * 10));
                ((SeekBarDialogPreference)prefScreen.findPreference("ui_3d_cam_position")).setValue((int)(config.camDistance * mScreenWidthInMM));
                ((SeekBarDialogPreference)prefScreen.findPreference("ui_3d_sensor_reset_acceleration")).setValue((int)(config.sensorResetAcceleration));                
                ((ListPreference)prefScreen.findPreference("ui_3d_shader_type")).setValue(Integer.toString(config.shaderType));                
                mConfig = config;
                
                data.recycle();
                reply.recycle();
            }
        } catch (Exception e) {
        	Log.e(TAG, "failed to load AR settings.", e);
        }
    }
    
    private void saveSettings() {
    	if (mConfig == null) {
    		return;
    	}
        try {
        	Log.d(TAG, "saving settings to surface flinger...");
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {             
                ARConfig config = mConfig;                
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                config.write(data);
                flinger.transact(TRANS_CODE_UPDATE_AR_SETTINGS, data, null, 0);                
                data.recycle();
            }
        } catch (Exception e) {
        	Log.e(TAG, "failed to save AR settings.", e);
        }
    }
    

    public static float getMaxScreenWidthInMM(Resources res) {
    	DisplayMetrics metrics = res.getDisplayMetrics();
    	int w = metrics.widthPixels;
    	int h = metrics.heightPixels;
    	w = Math.max(w, h);
    	int dpi = metrics.densityDpi;
    	int inchSize = w / dpi;
    	return inchSize * 25.4f;
    }
}
