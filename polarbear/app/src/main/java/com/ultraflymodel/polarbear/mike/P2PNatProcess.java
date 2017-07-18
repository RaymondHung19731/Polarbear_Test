package com.ultraflymodel.polarbear.mike;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class P2PNatProcess 
{     	
	  private Socket m_socket;
	  private int SERVERPORT = ConfigInfo.UDPPORT;	  
	  private String SERVER_IP;
	  
	  UDPNetwork      m_UdpNetW;
	  OutputStream m_outStream;
	  InputStream m_inStream;
	  int             m_iCurrentCommand = 0;
	  public NetworkCallback    mNetworkCallBack;	  
	  public static String P2P_PORCESS_TAG = "P2P_PROCESS_TAG";
	  
//===========================	  	  		  
	  public static final short REGISTER                     = 0;
      public static final short LOGIN                        = 1;
      public static final short REQUEST_CONNECT              = 2;      
      public static final short RESPONSE                     = 3;      
      public static final short READY_TO_CONNECT             = 4;      
      public static final short CHECK_CONNECT_TYPE           = 5;   // ���� ,  or Other Type......      
      public static final short REQUEST_LOCAL_IP_PORT        = 6;   // ���� ,  or Other Type......      
      public static final short REPORT_IP_PORT_TO_OTHER      = 7;   // ���� ,  or Other Type......      
      public static final short NOTIFY_MAKE_HOLE             = 8;   //notify other device to make hole......      
      public static final short NOTIFY_MAKE_HOLE_READY       = 9;   //notify  make hole ready back to active device.      
      public static final short UDP_MAKE_HOLE                = 10;      
      public static final short READY_RECEIVE_DATA           = 11;      
      public static final short COMNAD_DATA                  = 12;      
      public static final short READY_RECEIVE_DATA_ACK       = 13;            
      public static final short READY_DIRECT_RECEIVE_DATA    = 14;
                                
      public static final short CONNECT_COMMAND              = 15;
      public static final short RECEIVE_AUDIO_DATA           = 16;      
      public static final short RECEIVE_VIDEO_DATA           = 17;      
      public static final short RING_ON                      = 18;
      public static final short HUNG_UP                      = 19;
      public static final short PICK_UP                      = 20;
      public static final short REPORT_LIVE                  = 21;

    //===========================================================//
      public static final short CHANGE_DEVICE_SSID           = 22;      
      public static final short CHANGE_DEVICE_PASSWORD       = 23;
	  public static final short MP3_FILE_INIT_SUCCESS        = 24;
	  public static final short MP3_REQUEST_SECSSION         = 25;
	  public static final short SCAN_LIST                    = 26;
	  public static final short BROADCAST                    = 27;
	  public static final short MP3_PLAY_END                 = 28;
	public static final short WIFI_SSID_OK                 = 29;
	public static final short WIFI_SSID_NG                 = 30;
	public static final short WIFI_SSID_STARTSCAN        = 31;
	public static final short TANK_HIT        = 32;
	public static final short COUNTDOWN        = 33;

	public static final short REGISTER_SUCCESS  = 30001;
      public static final short ID_NOEXIST        = 30002;
      public static final short PASSWORD_ERROR    = 30003;
      public static final short LOGIN_SUCCESS     = 30004;
      public static final short ALREADY_REGISTER  = 30005;      
      public static final short ALREADY_LOGIN     = 30007;
                                
      public static final short SAME_LOCAL        = 30100;         
      public static final short HOLE_ROUTER       = 30102;
      
      public static boolean m_bThreadStart = false;
      public static boolean m_bReportAliveThread = false;
      
      Thread m_RecieveThread;
      Thread m_StartAliveThread;
      
      
      public  void SetCallBack(NetworkCallback    networkCallBack)
      {
    	  mNetworkCallBack  = networkCallBack;
    	  m_UdpNetW.mNetworkCallBack = networkCallBack;
      }
      
	  public P2PNatProcess(NetworkCallback    networkCallBack)
	  {
		  mNetworkCallBack = networkCallBack;		  
		  m_UdpNetW.mNetworkCallBack = networkCallBack;
	  }
	  
	  
	  public void Disconnect()	  	  
	  {		  
		  try 
		  {   			  
			  if(m_outStream!=null)
			  {
			      m_outStream.close();
			      m_inStream.close();			      
			      m_outStream = null;
			      
			      m_inStream  = null;			      
			      m_socket.close();
			      
			      StopRecieveThread();
			  }
		   } catch (IOException e)
		   {
			// TODO Auto-generated catch block
			e.printStackTrace();
		   }
	  }
	  
//============Data Ready To Receive=============================//	  
	  public void StartRequestConnect(int iID, int iRemoteID)
	  {   		  
		  Log.d("P2PNATProcess", "StartRequestConnect");
		  m_UdpNetW.StartRequestConnect(iID, iRemoteID);		 
	  }
	  
	  public void StartMakeHoleConnect(String strIP, int Port)   //P2P......
	  {
		  Log.d("P2PNATProcess", "Start Make Hole Connect!");
		  m_UdpNetW.StartMakeHoleConnect(strIP, Port);// �}�l  make hole
	  }
	  
	  
	  public void ReadyToConnect(int  iID)   //P2P......
	  {
		  Log.d("P2PNATProcess", "ReadyToConnect");
		  m_UdpNetW.ReadyToConnect(iID);
	  }
	  
	  
	  public void  DataReadyToRecieve(int iID)
	  {
		  Log.d("P2PNATProcess", "DataReadyToRecieve");
		  
		  m_UdpNetW.DataReadyToRecieve();
		  
	  }
	  
	  public void  DataReadyDirectToRecieve()
	  {
		  m_UdpNetW.DataReadyDirectToRecieve();
	  }
//=======================================================================//
	  
	  public void ConnectToServer(String strServerIP  , int iServerPort)
	  {
		  SERVER_IP  = strServerIP;
		  SERVERPORT = iServerPort;
		  
		  m_iCurrentCommand = CONNECT_COMMAND;
		  
		  new Thread(new ClientThread()).start();
		  
		  m_UdpNetW.StartReceviceServer();
		  
	  }
	  
//===============================================================//	  
	  public void CheckConnectionType(int   iMobileID, 			                          
			                          int iRemoteID)
	  {     		    		   
		    m_iCurrentCommand = CHECK_CONNECT_TYPE;
		    
		    ByteBuffer b = ByteBuffer.allocate(10);
		    b.order(ByteOrder.LITTLE_ENDIAN);  //WINDOWS SERVER
		    
		    b.putShort(CHECK_CONNECT_TYPE);		    
		    b.putInt(iMobileID);		    		    
		    b.putInt(iRemoteID);		
		    
		    
		    byte[] result = b.array();		    
		    WriteBuffer(result);		    
	  }
	  
	  public void ReportLiveTOServer()
	  {
		  m_iCurrentCommand = this.REPORT_LIVE;		  
		  ByteBuffer b = ByteBuffer.allocate(2);
		  
		  b.order(ByteOrder.LITTLE_ENDIAN);  //WINDOWS SERVER
		  b.putShort(this.REPORT_LIVE);
		  
		  byte[] result = b.array();		    
		  WriteBuffer(result);
	  }
	  
	  
	  
	  public void CheckConnectionType(int   iMobileID)
	  {
		  m_iCurrentCommand = this.REPORT_LIVE;
		  ByteBuffer b = ByteBuffer.allocate(10);
		  b.order(ByteOrder.LITTLE_ENDIAN);  //WINDOWS SERVER
		  
		  b.putShort(CHECK_CONNECT_TYPE);		    
		  b.putInt(iMobileID);
		  
		  byte[] result = b.array();		    
		  WriteBuffer(result);
	  }
	  
	//===============================================================//	  
	  public void RequestRemoteLocalIP(int   iMobileID, 			                          
			                           int iRemoteID)
	  {     		    
		    m_iCurrentCommand = CHECK_CONNECT_TYPE;		    		    		    
		    ByteBuffer b = ByteBuffer.allocate(26);
		    
		    b.order(ByteOrder.LITTLE_ENDIAN);  //WINDOWS SERVER
		    b.putShort(REQUEST_LOCAL_IP_PORT);
		    
		    b.putInt(iMobileID);		    		    
		    b.putInt(iRemoteID);
		    
		    byte[] result = b.array();		    
		    WriteBuffer(result);		    
	  }
	  
	  
	  public static String getIpAddress(byte[] rawBytes, int iStartIndex)
	  {
	        int i = 4;
	        String ipAddress = "";
	        int iCount=0;
	        for (byte raw : rawBytes)
	        {	        	
	        	if(iCount < iStartIndex) 
	        	{
	        		iCount++;
	        		continue;	        		
	        	}	        	
	            ipAddress += (raw & 0xFF);
	            if (--i > 0)
	            {
	                ipAddress += ".";
	            }else break;
	        }	 
	        return ipAddress;
	  }
	  
//================================================================================//	  
	  public void Register(int   iMobileID , String strPassword, int iRemoteID)
	  {     		    		   
		    m_iCurrentCommand = REGISTER;
		    
		    ByteBuffer b = ByteBuffer.allocate(26);
		    b.order(ByteOrder.LITTLE_ENDIAN);  //WINDOWS SERVER
		    
		    b.putShort(REGISTER);
		    b.putInt(iMobileID);
		    
		    b.put(strPassword.getBytes());
		    b.putInt(iRemoteID);
		    
		    byte[] result = b.array();
		    
		    WriteBuffer(result);		    
	  }
	  
	  public static void SleepDelayTime(long lTime)
	  {
			try
			{
				Thread.sleep(lTime);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
	  }
	  
	  void StopRecieveThread()
	  {
		  m_bThreadStart = false;		  		  
		  this.m_bReportAliveThread = false;		  
		  m_UdpNetW.StopReceiveServer();
		  synchronized(lockReportLive)
		  {		  
			  lockReportLive.notify();
		  }

		  m_RecieveThread.interrupt();
		  this.m_StartAliveThread.interrupt();

	  }
	  
		public static  short bytesToShort(byte[] bytes) 
		{			
			return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
		}
		
		
		public static  short bytesToShort(byte[] bytes,int iStart,int iLen) 
		{						
			return ByteBuffer.wrap(bytes,iStart,iLen).order(ByteOrder.LITTLE_ENDIAN).getShort();
		}
		
		
		public static  int bytesToInt(byte[] bytes,int iStart,int iLen) 
		{						
			return ByteBuffer.wrap(bytes,iStart,iLen).order(ByteOrder.LITTLE_ENDIAN).getInt();
		}
		
		
		
		public static byte[] shortToBytes(short value) 
		{
		    return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
		}
		
//=====================================================//	  
	  void  StartReceiveThread()
	  {
		  m_bThreadStart = true;
		  
		  m_RecieveThread =  new Thread(new Runnable()
			{
			   public void run()
			   {
				   try 
				   {   					   
					   while(m_bThreadStart)
					   {
					       if(m_inStream==null) continue;
					       
						   byte[] byReceiveBuffer = new byte[255];						   
						   int iLen = m_inStream.read(byReceiveBuffer);						   
						   short sCommand = P2PNatProcess.bytesToShort(byReceiveBuffer,0,2);
						   
						   if(sCommand == RESPONSE)
						   {
						       mNetworkCallBack.success(m_iCurrentCommand,iLen>0?0:-1, byReceiveBuffer,iLen, String.valueOf(SERVERPORT));
						   }else
						   {   
							   //Send Command......
							   mNetworkCallBack.success(sCommand,iLen>0?0:-1, byReceiveBuffer,iLen, String.valueOf(SERVERPORT));
						   }
						   
						   SleepDelayTime(100);						
   						   //=== 2014 08 19 =====================================//						
						   Log.d("ConfigInfoTag","Len = " +  iLen);
					   }
					} catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}				   
			   }			    
		   });
		  
		  m_RecieveThread.start();
	  }
	  
	  synchronized void WriteBuffer(final byte[]  byBuffer)
	  {
		  if(m_outStream==null || m_inStream==null) return;
		  
		  new Thread(new Runnable()
			{
			   public void run()
			   {
				   try 
					{   																			   					   
					    m_outStream.write(byBuffer);					    
						m_outStream.flush();						
					} catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}				   
			   }			    
		   }).start();
		  
			
	  }
	  
	  
//=========================================================//
	  private Object lockReportLive = new Object();
	  void  ReportAliveThread()
	  {   
		  m_bReportAliveThread  = true;
		  m_StartAliveThread =  new Thread(new Runnable()
			{
			   public void run()
			   {				    					   
				   while(m_bReportAliveThread)
				   {					   
					   synchronized(lockReportLive) 
					   {
						   if(m_outStream==null) continue;				       
					       ReportLiveTOServer();   //Device Set......				       
					       try 
					       {
					    	   lockReportLive.wait(30000);  //30s
						   } catch (InterruptedException e)
						   {
							   // TODO Auto-generated catch block
							   e.printStackTrace();
						   }					       						   						  					       
					   }					   
				   }							  
			   }			    
		   });		  
		  m_StartAliveThread.start();
	  }
	  
	  
	  
	  
	  
	  class ClientThread implements Runnable
	  {		  
		       @Override
		       public void run() 
		       {		  		   		  
		              try 
		              {	  		            	  
		                  InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
		                  m_socket = new Socket(serverAddr, SERVERPORT);
		                  
		                  m_outStream = m_socket.getOutputStream();
		                  m_inStream =  m_socket.getInputStream();
		                  
		                  mNetworkCallBack.success(m_iCurrentCommand,m_outStream!=null? 0:-1,null,0, String.valueOf(SERVERPORT));
		                  
		                  StartReceiveThread();		                 
		                  ReportAliveThread();
		                  
		              } catch (UnknownHostException e1)
		              {		  
		                  e1.printStackTrace();		  
		              } catch (IOException e1)
		              {		  
		                  e1.printStackTrace();		  
		              }
		       }		
     }
	 
	  
	 public void SetP2PServer(String strIP, int iPort)
	 {
		 m_UdpNetW.SetP2PAddress(strIP, iPort);
		 m_UdpNetW.StartReceviceServer();
	 }
	 
	 public void SendMsg(String strMSg)
	 {
		 m_UdpNetW.SendMsg(strMSg);
	 }
	 
	 //====Send Audio Data======================//	 
	 public void SendAudioData(byte[]  byData)
	 {
		 m_UdpNetW.SendAudioData(byData);
	 }
	 
	 public  void  SendReadyReceiveDataAck(int iID)
	 {   		 
		 m_UdpNetW.SendReadyReceiveDataAck(iID);		 
	 }
	 
	 public  void SendVideoOn()
	 {	
		 m_UdpNetW.SendVideoOn();
	 }
		
	 public  void SendVideoOff()
	 {
		 m_UdpNetW.SendVideoOff();
	 }
	 
	 public  void SendAudioOn()
	 {
		 m_UdpNetW.SendAudioOn();
	 }
	 
	 public  void SendAudioOFF()
	 {
		 m_UdpNetW.SendAudioOFF();
	 }
	 
	 public  void Pickup(int iID)
	 {
		 m_UdpNetW.Pickup(iID);
	 }
	 
	  public static byte[] get(byte[] array, int offset) 
	  {
	    return get(array, offset, array.length - offset);
	  }

	
	  public static byte[] get(byte[] array, int offset, int length) 
	  {
	      byte[] result = new byte[length];
	      System.arraycopy(array, offset, result, 0, length);
	      return result;
	  }
	  
	  public void  BreakP2PConnect()
	  {
		    m_UdpNetW.BreakP2PConnect();
	  }
	  
	  public void  SendRingON()
	  {
		    m_UdpNetW.SendRingON();
	  }
	  
	  
	  public void ReportLiveTODevice()
	  {   		  
		  m_UdpNetW.ReportLiveTODevice();
	  }
	  
	  
	  public void ChangeClientSSID(String strClientID)
	  {
		  m_UdpNetW.ChangeClientSSID(strClientID);
	  }



	  public void ChangeClientPassword(String strPassowrd) {
		  m_UdpNetW.ChangeClientPassword(strPassowrd);
	  }


   	  public void ChangeHostSSID(String strClientID)
	{
		m_UdpNetW.ChangeHostSSID(strClientID);
	}

	  public void ChangeHostPassword(String strPassowrd)
	  {
		  m_UdpNetW.ChangeHostPassword(strPassowrd);
	  }

	  public  void ScanClientSSID()
	  {
		  m_UdpNetW.ScanClientSSID();
	  }

	  public  void SendPIROn()
	  {
		  m_UdpNetW.SendPIROn();
	  }

	  public  void SendPIROff()
	  {
		  m_UdpNetW.SendPIROff();
	  }


}


