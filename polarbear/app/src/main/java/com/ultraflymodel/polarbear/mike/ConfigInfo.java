package com.ultraflymodel.polarbear.mike;

import com.ultraflymodel.polarbear.common.Constants;

public class ConfigInfo
{   	
	public   enum  PLAYMODE
	{
		NO_SET,
		ONE_V_ONE,
		MORE_V_MORE,
		INVITEE		
	}

	public   PLAYMODE    m_iPlayMode = PLAYMODE.NO_SET;

	static public int   COMMAND_PORT = 852;
	//static public int   COMMAND_PORT =  5152;
	//static public int   VIDEO_PORT   = 5153;
	static public int   REMOTE_PORT   = 852;

    static public int   AUDIO_PORT   = 5154;

	static public int   NULL_PORT   = 8000;


	static public String SERVER_ADDRESS = Constants.Polarbear_IP;
    static public String DEBUG_TAG = "WIFICAMPLAYER";
	public static String STRSERVERIP = "220.135.154.101";
	public static String DEVICEID = "9876";
	public static String REMOTEID = "1234";
	public static String PASSWORD = "2222222222222222";
	public static   int      TCPPORT  = 4000;
	public static   int      UDPPORT  = 4001;
	public static   int      UDPPORT_LOCAL  = 11112;
	public static  String   P2P_DEBUG_TAG = "P2P_DEBUG_TAG";
	public static  String    SHAREDPREFERENCES = "P2P_NAT_PROCESS";
}
