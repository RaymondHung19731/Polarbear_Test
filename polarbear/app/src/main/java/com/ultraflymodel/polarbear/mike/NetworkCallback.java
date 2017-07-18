package com.ultraflymodel.polarbear.mike;

public interface NetworkCallback 
{   	
	public abstract void success(int iCommand, int iResponse, byte[] byResponse, int iLen, String LocalPort);
	
} 
