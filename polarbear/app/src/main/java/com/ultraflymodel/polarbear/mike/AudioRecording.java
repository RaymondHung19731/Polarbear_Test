package com.ultraflymodel.polarbear.mike;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Environment;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AudioRecording 
{
	AudioRecord m_AudioRecord;
    Context m_Context;
    private Thread m_recordingThread = null;
    private boolean isRecording = false;
    int minBufferSize;


	UDPNetwork   m_P2PNatProcess;
    boolean  m_bOutputAudioLogFile = false;
    FileOutputStream m_outputFile;
    AcousticEchoCanceler m_AcousticEchoCanceler;
    NoiseSuppressor m_NoiseSuppressor;
    
    
    public void  EnableMicLogFile(boolean  bEnable)
    {
    	m_bOutputAudioLogFile = bEnable;
    }
    
    public boolean IsRecording()
    {
    	return isRecording;
    }
    
    public void StartRecording()
    {
    	 m_AudioRecord.startRecording();    	 
		 isRecording = true;
		 
	     if(m_bOutputAudioLogFile)
		 {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		    	String currentDateandTime = sdf.format(new Date()) + ".mic";
		    	
		    	String strPath = getSdCardPath() + currentDateandTime;
				try 
				{			
					 m_outputFile = new FileOutputStream(strPath);
				} catch (Exception e)
				{	
					
				}			
		 }		
	     
		 m_recordingThread = new Thread(new Runnable()
		 {
		        public void run() 
		        {
		        	  SendAudioDataToRemote();

		        }
		 }, "AudioRecorder Thread");		 
		 m_recordingThread.start();
		 
    }
    
    public void Release()
    {
    	AECrelease();
    	NoiseRelease();
    	
    	m_AudioRecord.release();
    	m_AudioRecord = null;
    }
    
	public AudioRecording(Context context, UDPNetwork   p2pNatProcess)
	{    		 
		 m_Context = context;
		 m_P2PNatProcess = p2pNatProcess;
		 
		 //AudioFormat.ENCODING_PCM_16BIT			 
		 minBufferSize = AudioTrack.getMinBufferSize(16000,
				 AudioFormat.CHANNEL_IN_STEREO,
				 AudioFormat.ENCODING_PCM_16BIT
		         );
		 
		 //AudioFormat.ENCODING_PCM_16BIT
		 m_AudioRecord = new AudioRecord(AudioSource.MIC,
				         8000,
		                 AudioFormat.CHANNEL_IN_MONO,
		                 AudioFormat.ENCODING_PCM_16BIT,
		                 minBufferSize * 2
		                 );
		 
		 if(AcousticEchoCanceler.isAvailable())
		 {
		     m_AcousticEchoCanceler =  AcousticEchoCanceler.create(m_AudioRecord.getAudioSessionId());
		     setAECEnabled(true);		     
		 }else
		 {
			 Log.d(ConfigInfo.P2P_DEBUG_TAG,"No Support AcousticEchoCanceler!" );
		 }	
		 
		 if(NoiseSuppressor.isAvailable())
	     {
	         m_NoiseSuppressor = NoiseSuppressor.create(m_AudioRecord.getAudioSessionId());
	         if(m_NoiseSuppressor!=null)
	         {
	            m_NoiseSuppressor.setEnabled(true);
	         }
	     }		  
	}
	
	public boolean setAECEnabled(boolean enable)
	{
	    if (null == m_AcousticEchoCanceler)
	    {
	        return false;
	    }
	    m_AcousticEchoCanceler.setEnabled(enable);
	    return m_AcousticEchoCanceler.getEnabled();
	}
    
	
	public boolean NoiseRelease()
	{ 
		if(m_NoiseSuppressor!=null)
       {
           m_NoiseSuppressor.setEnabled(false);
           m_NoiseSuppressor.release();
           return true;
       }		   
	   return false;
	}
	
	
	public boolean AECrelease()
	{
	    if (null == m_AcousticEchoCanceler)
	    {
	        return false;
	    }
	    m_AcousticEchoCanceler.setEnabled(false);
	    m_AcousticEchoCanceler.release();
	    return true;
	}

	
	
	 public static String getSdCardPath()
	 {
		     return Environment.getExternalStorageDirectory().getPath() + "/";
	 }
	 
	 
	//convert short to byte
	private byte[] short2byte(short[] sData) 
	{
	    int shortArrsize = sData.length;
	    byte[] bytes = new byte[shortArrsize * 2];
	    for (int i = 0; i < shortArrsize; i++) 
	    {
	        bytes[i * 2] = (byte) (sData[i] & 0x00FF);
	        bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
	        sData[i] = 0;
	    }
	    return bytes;
	}
	
	private void SendAudioDataToRemote() 
	{	    	    
	    byte byData[] = new byte[minBufferSize*2];

	    while (isRecording) 
	    {   	    	
	    	// gets the voice output from microphone to byte format
	    	//m_AudioRecord.read(sData, 0, minBufferSize);
	    	int iLen = m_AudioRecord.read(byData, 0, minBufferSize*2);
	    	
	    	//Write Data to String......
	        m_P2PNatProcess.SendAudioData(byData);
	        
	        if(m_bOutputAudioLogFile)
			{
			      try 
			      {				
			    	  m_outputFile.write(byData);			    	  
			      } catch (IOException e)
			      {
			      	// TODO Auto-generated catch block
			      	e.printStackTrace();
			      }			      
			}
	        
	        System.out.println("Audio Data Capture Size = " + iLen);
	    }	    	   
	}
	
	public void stopRecording()
	{
	    // stops the recording activity
	    if (null != m_AudioRecord && isRecording) 
	    {
	        isRecording = false;
	        m_AudioRecord.stop();	        	                
	        m_recordingThread = null;
	    }
	    
	    if(m_bOutputAudioLogFile)
		{
    		try 
    		{
				this.m_outputFile.close();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
}
