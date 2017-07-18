/*	
 * 	Created by Fenfan on 16/7/7.
 * 	Copyright 2011 Fenfan. All rights reserved.
 */

package com.ultraflymodel.polarbear.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class InternalStore {
	private SharedPreferences mSp;
	private SharedPreferences.Editor mSpe ;

	private final String SP_PREF_DATA = "SP_PREF_DATA";

	public InternalStore(Context context) {
		initSelf(context, SP_PREF_DATA);
	}
	
	public InternalStore(Context context, String storeName) {
		initSelf(context, storeName);
	}
	
	private void initSelf(Context context, String name) {
		mSp = context.getSharedPreferences(name, 0);
		mSpe = mSp.edit();
	}
	
	public String getString(String key, String defValue) {
		return mSp.getString(key, defValue);
	}
	
	public int getInt(String key, int defValue) {
		return mSp.getInt(key, defValue);
	}
	
	public float getFloat(String key, float defValue) {
		return mSp.getFloat(key, defValue);
	}
	
	public long getLong(String key, long defValue) {
		return mSp.getLong(key, defValue);
	}
	
	public boolean getBoolean(String key, boolean defValue) {
		return mSp.getBoolean(key, defValue);
	}
	
	public void putString(String key, String value) {
		mSpe.putString(key, value);
	}
	
	public void putInt(String key, int value) {
		mSpe.putInt(key, value);
	}
	
	public void putFloat(String key, float value) {
		mSpe.putFloat(key, value);
	}
	
	public void putLong(String key, long value) {
		mSpe.putLong(key, value);
	}
	
	public void putBoolean(String key, boolean value) {
		mSpe.putBoolean(key, value);
	}

	public boolean hasKey(String key) {
		return mSp.contains(key);
	}

	public void remove(String key) {
		mSpe.remove(key);
	}
	
	public void clear() {
		mSpe.clear();
	}
	
	public void save() {
		mSpe.commit();
	}
}
