package com.ultraflymodel.polarbear.mike;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import com.ultraflymodel.polarbear.R;
import com.ultraflymodel.polarbear.activity.PolarbearMainActivity;
import com.ultraflymodel.polarbear.common.DBA;
import com.ultraflymodel.polarbear.common.HILog;
import com.ultraflymodel.polarbear.eventbus.WifiUdpEvent;
import com.ultraflymodel.polarbear.fragment.PolarbearMainFragment;
import com.ultraflymodel.polarbear.fragment.ScanWifiListFragment;
import com.ultraflymodel.polarbear.ultraflymodel.UltraflyModelApplication;
import com.ultraflymodel.polarbear.utils.CommonUtils;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class UDPNetwork 
{
	private static final String TAG = UDPNetwork.class.getSimpleName();
	DatagramSocket m_datagramSocket;
	Thread m_RecieveThread,m_SendThread;
	private boolean  m_bThreadStart = false;
	String m_strP2PServerAddress;
	int       m_iP2PPort;
	boolean      m_bDataReceive;
	boolean      m_bDataReceiveAck;
	public NetworkCallback    mNetworkCallBack;
	static Context m_context;


	private Object m_Commandlock = new Object();


	public UDPNetwork()
	{
		HILog.d(TAG, "UDPNetwork:");
	}

	List<ByteBuffer> m_listByteBuffer = new ArrayList<ByteBuffer>();

	public UDPNetwork(Context context)
	{
		m_context = context;
	}
	public int  GetCurrentPort()
	{		
		return   ConfigInfo.UDPPORT_LOCAL;
	}
	private static String m_LocalPort;

	public static boolean mChangeToClient = false;


	public void StopReceiveServer()
	{
		HILog.d(TAG, "StopReceiveServer: m_bThreadStart = " + m_bThreadStart);
		if(m_bThreadStart)
		{
			m_RecieveThread.interrupt();
			m_SendThread.interrupt();
		    m_bThreadStart = false;

					synchronized(m_Commandlock) {
						m_Commandlock.notify();
					}

		    if(m_datagramSocket!=null)
		    {
		       m_datagramSocket.close();
		    }

		}
	}
	
	public void SetP2PAddress(String strIP, int iPort)
	{
		HILog.d(TAG, "SetP2PAddress: " + strIP + ", iPort = " + iPort);
		m_bThreadStart = false;
		m_strP2PServerAddress = strIP;
		m_iP2PPort = iPort;
	}
	
	//=============================================//
	public void  BreakP2PConnect()
	{		
		 ByteBuffer b = ByteBuffer.allocate(2);
		 
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		 
		 b.putShort(P2PNatProcess.HUNG_UP);     //2

		 SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);		 
	}
	
	///////////////////////////////////////////
	//2014  10 17 added......
	public void  DataReadyToRecieve()
	{
		 ByteBuffer b = ByteBuffer.allocate(2);
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		 b.putShort(P2PNatProcess.READY_RECEIVE_DATA);  //2
		 SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
	}

	public void  DataReadyDirectToRecieve()
	{
		 ByteBuffer b = ByteBuffer.allocate(2);
		 b.order(ByteOrder.LITTLE_ENDIAN);

		 //WINDOWS SERVER
		 b.putShort(P2PNatProcess.READY_DIRECT_RECEIVE_DATA);  //2
		 
		 SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);		 		 
	}
	
	
//===========================================================//	
	public void StartRequestConnect(int iID, int iRemoteID)   //P2P......
	{  		
		 //StructureInfo.REQUEST_CONNECT		
		 ByteBuffer b = ByteBuffer.allocate(14);
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		 
		 b.putShort(P2PNatProcess.REQUEST_CONNECT);  //2		 
		 b.putInt(iID);	                             //4		    		 		   
		 b.putInt(iRemoteID);  //4		 
		 b.putInt(1);   //Sequency Number......4
		 
		 SendCommand(b.array(),ConfigInfo.STRSERVERIP,ConfigInfo.UDPPORT);		 
	}


	//////////////////////////////////////////////
	//Need to Create Thread.
	public  void QueryHost(String strIPAddress)
	{
		HILog.d(TAG, "QueryHost: strIPAddress: " + strIPAddress);
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		b.put((byte) 0xAF);
		b.put((byte) 0x00);
		b.put((byte) 0x00);
		b.put((byte) 0x00);
		//SendCommand(b.array(), strIPAddress, this.m_iP2PPort);
		WriteData(b.array(), strIPAddress, this.m_iP2PPort);
	}


	public void sendBroadcast()
	{
		HILog.d(TAG, "sendBroadcast:");
		// Hack Prevent crash (sending should be done using an async task)
		StrictMode.ThreadPolicy policy = new   StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		try {
			//Open a random port to send the package
			DatagramSocket socket = new DatagramSocket();
			socket.setBroadcast(true);

			byte[] sendData = new byte[]{(byte)0xAF,(byte)0x00,(byte)0x00,(byte)0x00};

			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
					            getBroadcastAddress(),m_iP2PPort);

			socket.send(sendPacket);


		} catch (IOException e) {

		}
	}

	InetAddress getBroadcastAddress() throws IOException
	{
		WifiManager wifi = (WifiManager) m_context.getSystemService(Context.WIFI_SERVICE);

		DhcpInfo dhcp = wifi.getDhcpInfo();

		// handle null somehow

		int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
			quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);

		return InetAddress.getByAddress(quads);
	}


	
	public void ReadyToConnect(int  iID)   //P2P......
	{
		 ByteBuffer b = ByteBuffer.allocate(6);
		 
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		 b.putShort(P2PNatProcess.READY_TO_CONNECT); //2		 
		 b.putInt(iID);   //IID;;;;;;
		 
		 SendCommand(b.array(),ConfigInfo.STRSERVERIP,ConfigInfo.UDPPORT);		 		 
	}

	public void SendChangeCarName(String strMSg)
	{
		HILog.d(TAG, "SendChangeCarName: carname = " + strMSg);
		int length = strMSg.getBytes().length;
		ByteBuffer b = ByteBuffer.allocate(length+4);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER

		b.put((byte) 0xb4);
		b.put((byte) 0x00);
		b.put((byte) (length & 0xff));
		b.put((byte) 0x00);
		b.put(strMSg.getBytes());

		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);

	}
	
	public void SendMsg(String strMSg)
	{   		
		ByteBuffer b = ByteBuffer.allocate(2+strMSg.getBytes().length);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		
		b.putShort(P2PNatProcess.COMNAD_DATA);  //2
		b.put(strMSg.getBytes());
		
		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
		
	}
	
	
	public void StartMakeHoleConnect(String strIP, int Port)   //P2P......
	{   		
		 //StructureInfo.REQUEST_CONNECT		 		   
		 ByteBuffer b = ByteBuffer.allocate(2);
		 m_strP2PServerAddress = strIP; 
		 m_iP2PPort            = Port;		 
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		 b.putShort(P2PNatProcess.UDP_MAKE_HOLE);  //2
		 m_bDataReceive = false;
		 
		 for(int i=0;i<10 && (!m_bDataReceive);i++)
		 {
		     SendCommand(b.array(),strIP,Port);
		     try 
		     {
				Thread.sleep(200);
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		 }		 
	}

	
	void  SendCommand(final byte[] sendData,
					  final String strIP, final int Port)
	{
//		HILog.d(TAG, "SendCommand: sendData.length = " + sendData.length + ", strIP = " + strIP + ", Port = " + String.valueOf(Port));
		new Thread(new Runnable()
        {
          public void run() 
          {

           	   WriteData(sendData,strIP,Port);

          }      
        }).start();

	}

	//Send Command........
	synchronized  void WriteData(byte[] sendData,
								 String strIP,
								 int Port)
	{
//		HILog.d(TAG, "WriteData: sendData.length = " + sendData.length + ", strIP = " + strIP + ", Port = " + String.valueOf(Port));
		 try 
		   {    		    	    			
			    InetAddress serverAddr = InetAddress.getByName(strIP);
			    DatagramPacket dp = new DatagramPacket(sendData, sendData.length, serverAddr, Port);
			    try 
			    {       			    	
			    	if(m_datagramSocket!=null)
			    	{
						if(!m_datagramSocket.isClosed()) {
							m_datagramSocket.send(dp);
						}

			    	}
			    } catch (IOException e)
			    {
				     // TODO Auto-generated catch block
				    e.printStackTrace();
			    }
		   } catch (UnknownHostException e)
		   {
			   // TODO Auto-generated catch block
			   e.printStackTrace();
		   }


	}


	public    void StartSendingThread()
	{
		m_SendThread =new Thread(new Runnable()
		{
			public void run()
			{
				while(m_bThreadStart)
				{
					synchronized(m_Commandlock) {

						try {
							m_Commandlock.wait();

							if(m_listByteBuffer.size()>0) {
								ByteBuffer byBuffer = m_listByteBuffer.get(0);
								WriteData(byBuffer.array(), m_strP2PServerAddress, m_iP2PPort);
								m_listByteBuffer.remove(0);
							}

						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});


		m_SendThread.start();
	}

	
	public void StartReceviceServer()
	{
		  if(m_bThreadStart)return;
		  m_bThreadStart = true;

		  StartSendingThread();
		  m_RecieveThread =  new Thread(new Runnable()
			{
			   public void run()
			   {					   				    
				    try 
				    {
					        //DatagramPacket dpReceive = new DatagramPacket(recevieData, recevieData.length);
				    	    //m_datagramSocket = new DatagramSocket(ConfigInfo.UDPPORT_LOCAL);
						    byte[] recevieData = new byte[1048];
							m_datagramSocket = new DatagramSocket();
						    m_LocalPort = String.valueOf(m_datagramSocket.getLocalPort());
							HILog.d(TAG, "StartReceviceServer : getLocalPort: " + m_LocalPort);
						    m_datagramSocket.setSoTimeout(1000);


						    ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.fromDatagramSocket(m_datagramSocket);

						    FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
						    FileInputStream fin = new FileInputStream(fileDescriptor);

				    	    while(m_bThreadStart)
						    {					    													    				    	    	
				    	    	       try 
							           {
										   String sCommand="Not Any";
										   Arrays.fill(recevieData, (byte) 0);
										   if(fileDescriptor.valid()){
											   int readByte = fin.read(recevieData);
											   if(readByte!=-1 && readByte>0) {
												   ProcessCommand(recevieData, readByte, m_LocalPort);
											   }
										   }

							           	//Do Process......
							           } catch (IOException e)
								       {
								           //TODO Auto-generated catch block
								           e.printStackTrace();
								       }
				    	    }											    	
					} catch (SocketException e1)
					{
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				 						  
			   }			    
		   });



		   m_RecieveThread.start();		  		  	    
	}
	
//====================================//	
	void StartSendData()
	{
		new Thread(new Runnable()
        {
          public void run() 
          {   
        	  for(int i=0;i<1000;i++)
        	  {           		  
        		  byte[]  byData = new byte[]{0x0C,0x00,0x11,0x12,0x13,0x14,0x15};
        		  
        	      WriteData(byData,m_strP2PServerAddress,m_iP2PPort);
        	      
        	      try 
        	      {
					Thread.sleep(500);
				  } catch (InterruptedException e)
				  {
					  // TODO Auto-generated catch block
					  e.printStackTrace();					  
				  }        	      
        	  }        	  
          }      
        }).start();
	}
	
	
	public  void SendReadyReceiveDataAck(int iID)
	{		  
         ByteBuffer b = ByteBuffer.allocate(6);
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		 
		 b.putShort(P2PNatProcess.READY_RECEIVE_DATA_ACK);  //2		 
		 b.putInt(iID);  //4 byte
		 
		 SendCommand(b.array(), this.m_strP2PServerAddress, this.m_iP2PPort);
	}


	public  void SendQVGAVideoOn()
	{
//		SendVideoOff();
		int m=0;
		int n;
		HILog.d(TAG, "SendQVGAVideoOn:");
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		b.put((byte) 0xA0);
		b.put((byte) 0x01);
		b.put((byte) 0x00);
		b.put((byte) 0x00);
		for (m=0; m<5; m++) {
			SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
			for (n=0; n<100;n++) {;}
		}
	}
	
	public  void SendVideoOn()   // change to send QVGA command
	{
//		SendVideoOff();
		int m=0;
		int n;
		HILog.d(TAG, "SendVideoOn:");
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		b.put((byte) 0xA0);
//		b.put((byte) 0x00);   // this is  VGA
		b.put((byte) 0x01);  // this is QVGA
		b.put((byte) 0x00);
		b.put((byte) 0x00);
		for (m=0; m<5; m++) {
			SendCommand(b.array(), this.m_strP2PServerAddress, this.m_iP2PPort);
			for (n=0; n<100;n++) {;}
		}
	}

	public  void SendVideoOff()
	{
		int m=0;
		int n;
		HILog.d(TAG, "SendVideoOff:");
         ByteBuffer b = ByteBuffer.allocate(4);
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		 b.put((byte) 0xA1);
		 b.put((byte) 0x00);
		 b.put((byte) 0x00);
		 b.put((byte) 0x00);
		 for (m=0; m<5; m++) {
		 	SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
		 	for (n=0; n<100;n++) {;}
		 }
	}



	public  void SendPIROn()
	{
		HILog.d(TAG, "SendPIROn:");
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		b.put((byte) 0xA7);
		b.put((byte) 0x01);
		b.put((byte) 0x00);
		b.put((byte) 0x00);

		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
	}

	public  void SendPIROff()
	{
		HILog.d(TAG, "SendPIROff:");
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		b.put((byte) 0xA7);
		b.put((byte) 0x00);
		b.put((byte) 0x00);
		b.put((byte) 0x00);

		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
	}



	public  void ScanClientSSID()
	{
		HILog.d(TAG, "ScanClientSSID:");
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER

		b.put((byte) 0xB8);
		b.put((byte) 0x00);
		b.put((byte) 0x00);
		b.put((byte) 0x00);

		SendCommand(b.array(), this.m_strP2PServerAddress, this.m_iP2PPort);

	}

	public void Change2ClientMode()
	{
		HILog.d(TAG, "Change2ClientMode:");
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);
		b.put((byte) 0xB7);
		b.put((byte) 0x00);
		b.put((byte) 0x00);
		b.put((byte) 0x00);

		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
	}

	public void ChangeClientSSID(String strClientID)
	{   		 			
		 ByteBuffer b = ByteBuffer.allocate(4+strClientID.length());
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER

		 b.put((byte) 0xB2);
		 b.put((byte) 0x00);

		 b.putShort((short)strClientID.length());

		byte[] valuesAscii;
		try 
		{
			valuesAscii = strClientID.getBytes("US-ASCII");
			b.put(valuesAscii);
			SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void ChangeClientPassword(String strPassowrd)
	{
		 ByteBuffer b = ByteBuffer.allocate(4+strPassowrd.length());
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		 b.put((byte) 0xB3);
		 b.put((byte) 0x00);
		 b.putShort((short)strPassowrd.length());

		 byte[] valuesAscii;
		 try 
		 {
				valuesAscii = strPassowrd.getBytes("US-ASCII");

				b.put(valuesAscii);
				SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
		 } catch (UnsupportedEncodingException e)
		 {
				// TODO Auto-generated catch block
				e.printStackTrace();
		 }	 		 		 		
	}




	public void ChangeHostSSID(String strClientID)
	{
		ByteBuffer b = ByteBuffer.allocate(6+strClientID.length());
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		b.put((byte) 0xBF);
		b.put((byte) 0xFF);
		b.put((byte) 0xFF);
		b.put((byte) 0xFF);
		b.put((byte) 0x03);
		b.put((byte) 0x0B);



		byte[] valuesAscii;
		try
		{
			valuesAscii = strClientID.getBytes("US-ASCII");
			b.put(valuesAscii);
			SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void ChangeHostPassword(String strPassowrd)
	{
		ByteBuffer b = ByteBuffer.allocate(6+strPassowrd.length());
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		b.put((byte) 0xBF);
		b.put((byte) 0xFF);
		b.put((byte) 0xFF);
		b.put((byte) 0xFF);
		b.put((byte) 0x04);
		b.put((byte) 0x08);

		byte[] valuesAscii;
		try
		{
			valuesAscii = strPassowrd.getBytes("US-ASCII");
			b.put(valuesAscii);
			SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
		} catch (UnsupportedEncodingException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	
	public void SendAudioData(byte[]  byData)
	{    			
		    int iLen = byData.length / 1024;		 
		    for(int i=0;i<iLen;i++)
		    {
		        ByteBuffer b = ByteBuffer.allocate(1028);
		        b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		        
		        b.put((byte) 0x61);	 
		        b.put((byte) 0x00);

		        short Value = 1024;    	
		        b.putShort(Value);

		        b.put(byData, i*1024, 1024);   //PUT DATA......
		        SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);

		        try 
		        {
					Thread.sleep(50);
				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}		        
		     }		 
	}
	
	public  void Pickup(int iID)
	{		  
         ByteBuffer b = ByteBuffer.allocate(8);
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		 
		 b.put((byte) 0xAB);		 
		 b.put((byte) 0x00);
		 b.put((byte) 0x00);		 
		 b.put((byte) 0x04);
		 
		 b.putInt(iID);
		 
		 SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);		 
	}

	public  void ResetDevice()
	{

		//0xBE 01 02 00 05 00
		int m,n;
		for (m=0; m<2; m++)
		{
			SendVideoOff();
			for (n=0; n<100; n++) {;}
		}
		HILog.d(TAG, "ResetDevice:");
		ByteBuffer b = ByteBuffer.allocate(6);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER

		b.put((byte) 0xBE);
		b.put((byte) 0x01);
		b.put((byte) 0x02);
		b.put((byte) 0x00);
		b.put((byte) 0x05);
		b.put((byte) 0x00);

		//m_listByteBuffer.add(b);
		for (m=0; m<5; m++)
		{
			SendCommand(b.array(), this.m_strP2PServerAddress, this.m_iP2PPort);
			for (n=0; n<100; n++) {;}
		}
	}

	public static byte[] intToByteArray4(int a)
	{
		byte[] ret = new byte[4];
		ret[0] = (byte) (a & 0xFF);
		ret[1] = (byte) ((a >> 8) & 0xFF);
		ret[2] = (byte) ((a >> 16) & 0xFF);
		ret[3] = (byte) ((a >> 24) & 0xFF);
		return ret;
	}

	public  void SendCountDown2(int seconds)
	{
		HILog.d(TAG, "SendCountDown: seconds = " + seconds);
		ByteBuffer b = ByteBuffer.allocate(9);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER

		b.put((byte) 0xBE);
		b.put((byte) 0x01);
		b.put((byte) 0x05);
		b.put((byte) 0x00);
		b.put((byte) 0x10);
		b.put((byte) 0x00);
		b.put((byte) 0x01);
		b.put((byte) (seconds& 0xFF));
		b.put((byte) ((seconds >> 8) & 0xFF));

		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);

	}

	private int test_count = 0;
	public void SendVideoAck(byte packet01, byte packet02, byte frame01)
	{
		HILog.d(TAG, "SendVideoAck = " +  packet01 +  packet02 + frame01);
		ByteBuffer b = ByteBuffer.allocate(8);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER

		b.put((byte) 0xBF);
		b.put((byte) 0xFF);
		b.put((byte) 0xFF);
		b.put((byte) 0xFF);
		b.put((byte) 0xF3);
		b.put((byte) packet01);
		b.put((byte) packet02);
//		b.put((byte) frame01);
		b.put((byte) test_count);
		test_count++;



		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);

	}

	public void SendVideCapture(long miliseconds)
	{
		HILog.d(TAG, "SendVideCaptureInterval = " + miliseconds);
		ByteBuffer b = ByteBuffer.allocate(11);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER

		long tmp01 = (long) (miliseconds % 10);
		long tmp02 = (long) (((miliseconds - (tmp01)) % 100)/10) ;
		long tmp03 = (long) (((miliseconds - (tmp01) - (tmp02 * 10)) % 1000) /100);
		long tmp04 = (long) ((miliseconds / 1000));
		b.put((byte) 0xBE);
		b.put((byte) 0x01);
		b.put((byte) 0x07);
		b.put((byte) 0x00);
		b.put((byte) 0x10);
		b.put((byte) 0x00);
		b.put((byte) 0xFF);
		b.put((byte) (tmp04 + '0'));
		b.put((byte) (tmp03 + '0'));
		b.put((byte) (tmp02 + '0'));
		b.put((byte) (tmp01 + '0'));

		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);

	}

	public  void SendCountDown(int seconds)
	{
		HILog.d(TAG, "SendCountDown: seconds = " + seconds);
		byte[]   byData = new byte[9];
		byData[0] = (byte)0xBE;
		byData[1] = (byte)0x01;
		byData[2] = (byte)0x00;
		byData[3] = (byte)0x05;
		byData[4] = (byte)0x10;
		byData[5] = (byte)0x00;
		byData[6] = (byte)0x01;
		byData[7] = (byte)(seconds& 0xFF);
		byData[8] = (byte)((seconds >> 8) & 0xFF);
		for(int i=7; i<byData.length; i++){
			HILog.d(false, TAG, "SendCountDown:  i = : " + i + ": " + Integer.toHexString(CommonUtils.unsignedToBytes(byData[i])).toUpperCase());
		}
//		HILog.d(true, TAG, "SendCountDown:  this.m_strP2PServerAddress = " + this.m_strP2PServerAddress + ", this.m_iP2PPort = " + this.m_iP2PPort);
		SendCommand(byData, this.m_strP2PServerAddress, this.m_iP2PPort);
	}


	public  void StartMP3()
	{
		HILog.d(TAG, "StartMP3:");
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER

		b.put((byte) 0xAC);
		b.put((byte) 0x00);
		b.put((byte) 0xFF);
		b.put((byte) 0xFF);

		//m_listByteBuffer.add(b);
		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
	}

	public void SendAlive()
	{
		HILog.d(TAG, "SendAlive:");
		ByteBuffer b = ByteBuffer.allocate(1);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER

		b.put((byte) 0xAF);

		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
	}

	public void FireOneShot()
	{
		HILog.d(TAG, "FireOneShot:");
		ByteBuffer b = ByteBuffer.allocate(1);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER

		b.put((byte) 0xBB);

		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
	}

	public  void StopMP3()
	{
		HILog.d(TAG, "StopMP3:");
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER

		b.put((byte) 0xAC);
		b.put((byte) 0x04);
		b.put((byte) 0xFF);
		b.put((byte) 0xFF);

		//m_listByteBuffer.add(b);
		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
	}

	public  void PauseMP3()
	{
		HILog.d(TAG, "PauseMP3:");
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER

		b.put((byte) 0xAC);
		b.put((byte) 0x03);
		b.put((byte) 0xFF);
		b.put((byte) 0xFF);

		//m_listByteBuffer.add(b);
		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
	}


	public  void StartMP3FileSection(short sLenSection)
	{
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		b.put((byte) 0xAC);
		b.put((byte) 0x01);
		b.putShort(sLenSection);
		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
	}

	public void SendMP3FileBuffer(byte[] byBuffer,short sLen,short sSectionNum)
	{
		ByteBuffer b = ByteBuffer.allocate(6+byBuffer.length);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER

		b.put((byte) 0x63);
		b.put((byte) 0x01);
		b.putShort(sLen);
		b.putShort(sSectionNum);
		b.put(byBuffer);

		synchronized(m_Commandlock) {
			m_listByteBuffer.add(b);
			m_Commandlock.notify();
		}
		//SendCommand(b.array(), this.m_strP2PServerAddress, this.m_iP2PPort);
	}

	public  void  SendControlMotorCommand(int iDataMotor1,int iDataMotor2,
										  int iDataMotor3,int iDataMotor4,int iDataMotor5,int iDataMotor6)
	{
//		HILog.d(TAG, "SendControlMotorCommand:");
		SendCommand(ControlMotor(iDataMotor1, iDataMotor2, iDataMotor3, iDataMotor4, iDataMotor5, iDataMotor6)
				, this.m_strP2PServerAddress, this.m_iP2PPort);
	}

	byte[]   ControlMotor(int iDataMotor1,int iDataMotor2,
						  int iDataMotor3,int iDataMotor4,
						  int iDataMotor5,int iDataMotor6)
	{
//		HILog.d(TAG, "ControlMotor:");
		byte[]   byData = new byte[13];

		byData[0] = (byte)0xC2;
		byData[1] = (byte)0xFF;
		byData[2] = (byte)9;
		byData[3] = (byte)0;

		int iData1 = iDataMotor2 | (iDataMotor1<<12);
		int iData2 = iDataMotor4 | (iDataMotor3<<12);
		int iData3 = iDataMotor6 | (iDataMotor5<<12);
//		int iData4 = 0;

		System.arraycopy(intToByteArray(iData1), 0, byData, 4, 3);
		System.arraycopy(intToByteArray(iData2), 0, byData, 7, 3);
		System.arraycopy(intToByteArray(iData3), 0, byData, 10, 3);
//		System.arraycopy(intToByteArray(iData4), 0, byData, 13, 3);

		return byData;
	}

	public static final byte[] intToByteArray(int value)
	{
		return new byte[]
				{
						(byte)(value >>> 16),
						(byte)(value >>> 8),
						(byte)value};
	}

	public  void SendAudioOn()
	{
		HILog.d(TAG, "SendAudioOn:");
         ByteBuffer b = ByteBuffer.allocate(4);
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		 b.put((byte) 0xA2);		 
		 b.put((byte) 0x00);
		 b.put((byte) 0x00);
		 b.put((byte) 0x00);
		 SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);		 
	}
	
	public  void SendAudioOFF()
	{
		HILog.d(TAG, "SendAudioOFF:");
         ByteBuffer b = ByteBuffer.allocate(4);
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		 b.put((byte) 0xA3);		 
		 b.put((byte) 0x00);
		 b.put((byte) 0x00);
		 b.put((byte) 0x00);
		 SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);		 
	}
	
	//
	public void  SendRingON()
	{
		HILog.d(TAG, "SendRingON:");
		ByteBuffer b = ByteBuffer.allocate(4);
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		 b.put((byte) 0xA6);    //==-90	 
		 b.put((byte) 0x00);
		 b.put((byte) 0x00);  //
		 b.put((byte) 0x00);  //LENS
		 SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);		 
	}
	
	
	 public void ReportLiveTODevice()
	 {   		  
		 ByteBuffer b = ByteBuffer.allocate(4);
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		 
		 b.put((byte) 0xAF);    //==-90	 
		 b.put((byte) 0x00);
		 b.put((byte) 0x00);  //
		 b.put((byte) 0x00);  //LENS
		 
		 if(m_iP2PPort==0) return;
		 
		 SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);		
	 }

	 public static void PopUnderVoltage()
	 {
		 new Thread() {
			 public void run() {
				 Looper.prepare();
				 Toast.makeText(m_context, R.string.under_voltage, Toast.LENGTH_SHORT).show();
				 Looper.loop();
			 };
		 }.start();
	 }

	public int now_frame = 0;
	public int last_frame = 0;
	public short now_pack = 0;
	public short last_pack = 0;

	public long correct_pack = 0;
	public long wrong_pack = 0;

	public int correct_fps = 0;
	public int wrong_fps = 0;
    public int check_fps_flag = 0;

	void ProcessCommand(byte[]  byData, int iLen , String LocalPort)
	 {
		 int iCommand = CommonUtils.unsignedToBytes(byData[0]);
//		 HILog.d(true, TAG, "ProcessCommand: iCommand = 0x" + Integer.toHexString(iCommand).toUpperCase() + ", iLen = " + iLen);
		 if(mNetworkCallBack==null) {
			 HILog.d(false, TAG, "ProcessCommand: mNetworkCallBack is null!!");
			 return;
		 }
//		 HILog.d(TAG, "ProcessCommand:");

		/* mNetworkCallBack.success(P2PNatProcess.RECEIVE_VIDEO_DATA, 0,
				 byData,iLen);
				 */
//		 short sCommand  = P2PNatProcess.bytesToShort(byData,0,2);
		 //strClientID.getBytes("US-ASCII");
		 // Log.d(P2PNatProcess.P2P_PORCESS_TAG, "Ready Receive Data IP=" + strIP +
		//		           "   Port=" + iPort +" DateLen =" + iLen);
		// mNetworkCallBack.success(P2PNatProcess.RECEIVE_VIDEO_DATA, 0,
          //       byData,iLen);
			// Log.d(ConfigInfo.P2P_DEBUG_TAG, "Data First Byte=" + byData[0]);			 
			//A0  == Video ON
			//A1 == Video Off
			//A2 == Audio ON...
			//A3  == Audio  OFF...
		 if(iCommand==0x60) //Video
		 {
			 //Log.d(ConfigInfo.P2P_DEBUG_TAG, "Video Data!");
			 mNetworkCallBack.success(P2PNatProcess.RECEIVE_VIDEO_DATA, 0,
					 byData, iLen, LocalPort);
             PolarbearMainFragment.UserMilliTimeInterval();
//			 SendVideoAck(byData[1028], byData[1029], byData[1030]);
             now_frame = (short) (byData[1030] & 0xFF);
			 now_pack = (short) (byData[1028] & 0xFF);
			 now_pack += (short) ((byData[1029] & 0xFF) * 256);


             if (now_frame != last_frame )
             {
				 PolarbearMainFragment.UserMilliTimeFrameInterval();
				 if (check_fps_flag != 0)
				 {
				 	wrong_fps++;
				 }
				 else
				 {
				 	correct_fps++;
				 }


				 check_fps_flag = 0;
			 }

			 if (((now_pack - last_pack) == 1)  ||((now_pack == last_pack)))
			 {
				 correct_pack++;
			 }
			 else
			 {
				 wrong_pack++;
				 check_fps_flag++;
			 }

			 last_frame = now_frame;
			 last_pack = now_pack;
			 if ( PolarbearMainFragment.GetVideo_flag == 0) {
				 PolarbearMainFragment.Video_ON_02 = PolarbearMainFragment.UserGetTime();
				 PolarbearMainFragment.Video_ON_interval = PolarbearMainFragment.Video_ON_02 - PolarbearMainFragment.Video_ON_01;
				 PolarbearMainFragment.GetVideo_flag = 1;
//				 SendVideCapture(PolarbearMainFragment.Video_ON_interval);
			 }

		 }
		 else if(byData[0]==(byte)0xBC) //0xBC: be hitted
		 {
			 mNetworkCallBack.success(P2PNatProcess.TANK_HIT, 0, byData, iLen, LocalPort);
		 }
		 else if(byData[0]==(byte)0xA6) //test...
		 {
				//Play RingON
				//SendAudioOn();				
				mNetworkCallBack.success(P2PNatProcess.RING_ON, 0, null,0, LocalPort);
		 }
		 else if(byData[0]==98) //H.264
		 {
				//Log.d(ConfigInfo.P2P_DEBUG_TAG, "Video Data!");
				mNetworkCallBack.success(P2PNatProcess.RECEIVE_VIDEO_DATA, 0,
						                 byData,iLen, LocalPort);
		 }
		 else if(byData[0]==97) //Audio
		 {
				Log.d(ConfigInfo.P2P_DEBUG_TAG, "Audio Data RECEIVE Data!");
				
				mNetworkCallBack.success(P2PNatProcess.RECEIVE_AUDIO_DATA, 0, 
						                 byData,iLen, LocalPort);
				
				
		 }
		 else if(byData[0]==((byte)0xAB)) //Audio
		 {
				//Log.d(ConfigInfo.P2P_DEBUG_TAG, "Audio Data RECEIVE Data!");				
				mNetworkCallBack.success(P2PNatProcess.PICK_UP, 0,byData,iLen, LocalPort);
//			}else if(byData[0] == (byte)0xAF)  //-81
//			{
//				Nofify Device Alive.
		 }
		 else if(byData[0]==(byte)0xAC) //test...
	     {
				if(byData[1]==(byte)0x00 && byData[2]==(byte)0xFF && byData[3]==(byte)0xFF)
				{
					Log.d(ConfigInfo.P2P_DEBUG_TAG,"MP3 Send Already!");
					mNetworkCallBack.success(P2PNatProcess.MP3_FILE_INIT_SUCCESS, 0, null, 0, LocalPort);
				}else if(byData[1]==0x01)
				{
					ByteBuffer bb = ByteBuffer.allocate(2);
					bb.order(ByteOrder.LITTLE_ENDIAN);
					bb.put(byData[2]);
					bb.put(byData[3]);
					int  iSection  = bb.getShort(0);

					mNetworkCallBack.success(P2PNatProcess.MP3_REQUEST_SECSSION, iSection, null, 0, LocalPort);
				}else if(byData[1]==0x02)   //END SONG
				{
					mNetworkCallBack.success(P2PNatProcess.MP3_PLAY_END, 0, null, 0, LocalPort);
				}
		  }
		  else if(byData[0]==((byte)0xB8)) //SCAN CLIENT CALLBACK......
	      {

	      }
	      else if(byData[0]==((byte)0xB9)) //SCAN CLIENT CALLBACK......
	      {
				mNetworkCallBack.success(P2PNatProcess.SCAN_LIST, 0,byData,iLen, LocalPort);
	      }
	      else if(byData[0]==((byte)0xAF)) //BROADCAST
	      {
				HILog.d(false, TAG, "UDPNetwork: BROADCAST received.");
				mNetworkCallBack.success(P2PNatProcess.BROADCAST, 0,byData,iLen, LocalPort);
		  }
		  else if(byData[0]==((byte)0xBF)) //WIFI_SSID
	 	  {
				if(byData[1]==(byte)0xFF && byData[2]==(byte)0x20 && byData[3]==(byte)0x00){
					mNetworkCallBack.success(P2PNatProcess.WIFI_SSID_OK, 0,byData,iLen, LocalPort);
					HILog.d(false, TAG, "UDPNetwork: WIFI_SSID_OK:");
//					WifiUdpEvent wifiUdpEvent = new WifiUdpEvent();
//					wifiUdpEvent.mDialog = false;
//					wifiUdpEvent.message = m_context.getString(R.string.ssid_ok);
//					UltraflyModelApplication.getInstance().bus.post(wifiUdpEvent);

					new Thread() {
						public void run() {
							Looper.prepare();
							Toast.makeText(m_context, R.string.ssid_ok, Toast.LENGTH_SHORT).show();
							Looper.loop();
						};
					}.start();
					int count = 0;

//					ScanWifiListFragment.user_change_to_home_router();
//					ScanWifiListFragment.user_change_to_home_router();
//					PolarbearMainActivity.wm.reconnect();
					while((UDPNetwork.mChangeToClient == false) && (count <= 100))
					{
						PolarbearMainActivity.wm.reconnect();

						count++;
						Change2ClientMode();
						for (int j = 0; j<1000; j++)
						{;}
					}
//					UDPNetwork.mChangeToClient = true;
					String wifissid = PolarbearMainActivity.getSettingStringValue(DBA.Field.WIFINAME);
					String wifipass = PolarbearMainActivity.getSettingStringValue(DBA.Field.WIFIPASSWORD);
					ScanWifiListFragment.setSsidAndPassword(ScanWifiListFragment.mActivity, wifissid, wifipass);

				} else if(byData[1]==(byte)0xFF && byData[2]==(byte)0x1B && byData[3]==(byte)0x00) {
					mNetworkCallBack.success(P2PNatProcess.WIFI_SSID_NG, 0,byData,iLen, LocalPort);
					HILog.d(false, TAG, "UDPNetwork: WIFI_SSID_NG:");
					new Thread() {
						public void run() {
							Looper.prepare();
							Toast.makeText(m_context, "WIFI_SSID_NG", Toast.LENGTH_SHORT).show();
							Looper.loop();
						};
					}.start();
				} else if(byData[1]==(byte)0xFF && byData[2]==(byte)0x12 && byData[3]==(byte)0x00) {
					mNetworkCallBack.success(P2PNatProcess.WIFI_SSID_STARTSCAN, 0,byData,iLen, LocalPort);
					HILog.d(false, TAG, "UDPNetwork: WIFI_SSID_STARTSCAN:");
					UDPNetwork.mChangeToClient = false;
					WifiUdpEvent wifiUdpEvent = new WifiUdpEvent();
					wifiUdpEvent.mDialog = false;
					wifiUdpEvent.message = m_context.getString(R.string.ssid_startscan);
					UltraflyModelApplication.getInstance().bus.post(wifiUdpEvent);
					new Thread() {
						public void run() {
							Looper.prepare();
							Toast.makeText(m_context, R.string.ssid_startscan, Toast.LENGTH_SHORT).show();
							Looper.loop();
						};
					}.start();

					PolarbearMainActivity.wm.reconnect();
				} else {
					Log.d(ConfigInfo.P2P_DEBUG_TAG, "0xBF: UnKnow UDP RECEIVE Data!");
				}

			}
		 else if(byData[0]==((byte)0xB2)) // Get Client ssid name
		 {
//			 ScanWifiListFragment.mSsidName = true;
		 }
		 else if(byData[0]==((byte)0xB3)) // Get Client ssid password
		 {
//			 ScanWifiListFragment.mSsidPass = true;
		 }
		 else if(byData[0]==((byte)0xB7)) // Get Change to Client
		 {
			 UDPNetwork.mChangeToClient = true;
		 }

			else
			{
				HILog.d(false, TAG, "ProcessCommand: iCommand = 0x" + Integer.toHexString(iCommand).toUpperCase() + ", iLen = " + iLen);

				HILog.d(false, TAG, "UnKnow UDP RECEIVE Data!");
			}

	}
	
}
