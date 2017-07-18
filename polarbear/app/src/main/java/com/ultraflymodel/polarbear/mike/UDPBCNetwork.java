package com.ultraflymodel.polarbear.mike;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.util.Log;

import com.ultraflymodel.polarbear.common.Constants;
import com.ultraflymodel.polarbear.common.HILog;
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


public class UDPBCNetwork
{
	private static final String TAG = UDPBCNetwork.class.getSimpleName();
	DatagramSocket m_datagramSocket;
	Thread m_RecieveThread,m_SendThread;
	boolean  m_bThreadStart = false;
	String m_strP2PServerAddress;
	int       m_iP2PPort;
	boolean      m_bDataReceive;
	boolean      m_bDataReceiveAck;
	public NetworkCallback    mNetworkCallBack;
	Context m_context;


	private Object m_Commandlock = new Object();


	public UDPBCNetwork()
	{
		HILog.d(TAG, "UDPNetwork:");
	}

	List<ByteBuffer> m_listByteBuffer = new ArrayList<ByteBuffer>();

	public UDPBCNetwork(Context context)
	{
		m_context = context;
	}
	public int  GetCurrentPort()
	{		
		return   ConfigInfo.UDPPORT_LOCAL;
	}
	private static String m_LocalPort;




	public void StopReceiveServer()
	{
		HILog.d(TAG, "StopReceiveServer: m_bThreadStart = " + m_bThreadStart);
		if(m_bThreadStart)
		{
			m_RecieveThread.interrupt();
		    m_bThreadStart = false;

					synchronized(m_Commandlock) {
						m_Commandlock.notify();
					}

		    if(m_datagramSocket!=null)
		    {
		       m_datagramSocket.close();
		    }

//			m_SendThread.interrupt();
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
		 try 
		   {    		    	    			
			    InetAddress serverAddr = InetAddress.getByName(strIP);
			    DatagramPacket dp = new DatagramPacket(sendData, sendData.length, serverAddr, Port);
			    try 
			    {       			    	
			    	if(m_datagramSocket!=null)
			    	{
			    	   m_datagramSocket.send(dp);
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

//		  StartSendingThread();
		  m_RecieveThread =  new Thread(new Runnable()
			{
			   public void run()
			   {					   				    
				    try 
				    {
					        //DatagramPacket dpReceive = new DatagramPacket(recevieData, recevieData.length);
				    	    //m_datagramSocket = new DatagramSocket(ConfigInfo.UDPPORT_LOCAL);
						    byte[] recevieData = new byte[1028];
							m_datagramSocket = new DatagramSocket(Constants.IBCPORT);
						    m_datagramSocket.setBroadcast(true);
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
										   int readByte = fin.read(recevieData);
										   if(readByte!=-1 && readByte>0) {
											   HILog.d(false, TAG, "StartReceviceServer: m_LocalPort = " + m_LocalPort + ":  readByte = " + readByte);
											   ProcessCommand(recevieData, readByte, m_LocalPort);
										   }
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
	
	
	public  void SendVideoOn()
	{
		HILog.d(TAG, "SendVideoOn:");
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		b.put((byte) 0xA0);
		b.put((byte) 0x00);
		b.put((byte) 0x00);
		b.put((byte) 0x00);

		SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);
	}

	public  void SendVideoOff()
	{
		HILog.d(TAG, "SendVideoOff:");
         ByteBuffer b = ByteBuffer.allocate(4);
		 b.order(ByteOrder.LITTLE_ENDIAN);           //WINDOWS SERVER
		 b.put((byte) 0xA1);
		 b.put((byte) 0x00);
		 b.put((byte) 0x00);
		 b.put((byte) 0x00);

		 SendCommand(b.array(),this.m_strP2PServerAddress,this.m_iP2PPort);		 		 
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
	 
     void ProcessCommand(byte[]  byData, int iLen , String LocalPort)
	 {
		 HILog.d(false, TAG, "ProcessCommand: iCommand = 0x" + Integer.toHexString(CommonUtils.unsignedToBytes(byData[0])).toUpperCase() + ", iLen = " + iLen);
		 if(mNetworkCallBack==null) {
			 HILog.d(false, TAG, "ProcessCommand: mNetworkCallBack is null!!");
			 return;
		 }
//		 HILog.d(TAG, "ProcessCommand:");

		/* mNetworkCallBack.success(P2PNatProcess.RECEIVE_VIDEO_DATA, 0,
				 byData,iLen);
				 */
		 short sCommand  = P2PNatProcess.bytesToShort(byData,0,2);
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
		 if(byData[0]==(byte)0xBE){
//			 if(byData[4]==(byte)0x10&&byData[6]==(byte)0x01) {
/*
11-03 21:36:06.047 32267-32306/com.ultraflymodel.polarbear D/UDPBCNetwork: HILog:StartReceviceServer: m_LocalPort = 8520, i = : 0: BE
11-03 21:36:06.047 32267-32306/com.ultraflymodel.polarbear D/UDPBCNetwork: HILog:StartReceviceServer: m_LocalPort = 8520, i = : 1: 1
11-03 21:36:06.047 32267-32306/com.ultraflymodel.polarbear D/UDPBCNetwork: HILog:StartReceviceServer: m_LocalPort = 8520, i = : 2: 0
11-03 21:36:06.047 32267-32306/com.ultraflymodel.polarbear D/UDPBCNetwork: HILog:StartReceviceServer: m_LocalPort = 8520, i = : 3: 5
11-03 21:36:06.047 32267-32306/com.ultraflymodel.polarbear D/UDPBCNetwork: HILog:StartReceviceServer: m_LocalPort = 8520, i = : 4: 10
11-03 21:36:06.047 32267-32306/com.ultraflymodel.polarbear D/UDPBCNetwork: HILog:StartReceviceServer: m_LocalPort = 8520, i = : 5: 0
11-03 21:36:06.047 32267-32306/com.ultraflymodel.polarbear D/UDPBCNetwork: HILog:StartReceviceServer: m_LocalPort = 8520, i = : 6: 1
11-03 21:36:06.047 32267-32306/com.ultraflymodel.polarbear D/UDPBCNetwork: HILog:StartReceviceServer: m_LocalPort = 8520, i = : 7: B4
11-03 21:36:06.047 32267-32306/com.ultraflymodel.polarbear D/UDPBCNetwork: HILog:StartReceviceServer: m_LocalPort = 8520, i = : 8: 0
*/
				 mNetworkCallBack.success(P2PNatProcess.COUNTDOWN, 0, byData, iLen, LocalPort);
//			 }
		 } else
			if(byData[0]==(byte)0xBC) //0xBC: be hitted
			{
			 mNetworkCallBack.success(P2PNatProcess.TANK_HIT, 0, byData, iLen, LocalPort);
			} else
			if(byData[0]==(byte)0xA6) //test...  
			{
				//Play RingON
				//SendAudioOn();				
				mNetworkCallBack.success(P2PNatProcess.RING_ON, 0, null,0, LocalPort);
			}else if(byData[0]==96) //Video
			{
				//Log.d(ConfigInfo.P2P_DEBUG_TAG, "Video Data!");
				mNetworkCallBack.success(P2PNatProcess.RECEIVE_VIDEO_DATA, 0,
						                 byData,iLen, LocalPort);
			}else if(byData[0]==98) //H.264
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
				
				
			}else if(byData[0]==((byte)0xAB)) //Audio
			{   				
				//Log.d(ConfigInfo.P2P_DEBUG_TAG, "Audio Data RECEIVE Data!");				
				mNetworkCallBack.success(P2PNatProcess.PICK_UP, 0,byData,iLen, LocalPort);
//			}else if(byData[0] == (byte)0xAF)  //-81
//			{
//				Nofify Device Alive.
			}if(byData[0]==(byte)0xAC) //test...
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
		    }else if(byData[0]==((byte)0xB8)) //SCAN CLIENT CALLBACK......
	        {

	        }
	        else if(byData[0]==((byte)0xB9)) //SCAN CLIENT CALLBACK......
	        {
				mNetworkCallBack.success(P2PNatProcess.SCAN_LIST, 0,byData,iLen, LocalPort);
	        }else if(byData[0]==((byte)0xAF)) //BROADCAST
	        {
				HILog.d(false, TAG, "UDPNetwork: BROADCAST received.");
				mNetworkCallBack.success(P2PNatProcess.BROADCAST, 0,byData,iLen, LocalPort);
			}else if(byData[0]==((byte)0xBF)) //WIFI_SSID
	 		{
				if(byData[1]==(byte)0xFF && byData[2]==(byte)0x20 && byData[3]==(byte)0x00){
					mNetworkCallBack.success(P2PNatProcess.WIFI_SSID_OK, 0,byData,iLen, LocalPort);
				} else if(byData[1]==(byte)0xFF && byData[2]==(byte)0x1B && byData[3]==(byte)0x00) {
					mNetworkCallBack.success(P2PNatProcess.WIFI_SSID_NG, 0,byData,iLen, LocalPort);
				} else if(byData[1]==(byte)0xFF && byData[2]==(byte)0x12 && byData[3]==(byte)0x00) {
					mNetworkCallBack.success(P2PNatProcess.WIFI_SSID_STARTSCAN, 0,byData,iLen, LocalPort);
				} else {
					Log.d(ConfigInfo.P2P_DEBUG_TAG, "UnKnow UDP RECEIVE Data!");
				}
			}
	        else
			{
		   	   Log.d(ConfigInfo.P2P_DEBUG_TAG, "UnKnow UDP RECEIVE Data!");
			}

	}
	
}
