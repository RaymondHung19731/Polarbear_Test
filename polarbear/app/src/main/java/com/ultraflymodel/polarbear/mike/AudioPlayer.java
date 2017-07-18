package com.ultraflymodel.polarbear.mike;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;


public class AudioPlayer 
{   	
	AudioTrack m_track;
	Context m_Context;
	List<ByteArrayOutputStream> m_listBuffer = new ArrayList<ByteArrayOutputStream>();
	private Thread m_playingThread = null;
	
	
	
	boolean   m_bIsPlaying;
	public AudioPlayer(Context context)
	{    		 
		 m_Context = context;
		 
		 int minBufferSize = AudioTrack.getMinBufferSize(8000,
				 AudioFormat.CHANNEL_OUT_MONO,
		          AudioFormat.ENCODING_PCM_16BIT);

         m_track = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
		                 AudioFormat.CHANNEL_OUT_MONO,
		                 AudioFormat.ENCODING_PCM_16BIT,
		                 minBufferSize * 2,		                
		                 AudioTrack.MODE_STREAM);
         
       
	}
	
	public   void WriteAudioData(byte[]  byData, int iLen)
	{   				
		 ///m_track.write(byData,0, iLen);
		 synchronized(m_listBuffer)
		 {
             ByteArrayOutputStream byArrayOutputStream  = new ByteArrayOutputStream();
             byArrayOutputStream.write(byData,0,iLen);
             m_listBuffer.add(byArrayOutputStream);
		 }
		 
	}
	
	void ReadAudioData()
	{
		 while(m_bIsPlaying)
		 {
		     synchronized(m_listBuffer)
		      {    			 
			      if(m_listBuffer.size()>0)
   		          {
   			          ByteArrayOutputStream byArrayOutputStream = m_listBuffer.get(0);
   		              m_listBuffer.remove(0);
   		              
   		              byte[] byData = byArrayOutputStream.toByteArray();   		         
   		              m_track.write(byData,0, byData.length);   		              
   		              m_track.flush();
   		           }   		         		         
		      }
		      
		      try 
		      {
				Thread.sleep(1);
			  } catch (InterruptedException e)
			  {
				  // TODO Auto-generated catch block
				  e.printStackTrace();
			  }	     
		 }		 
	}
	
	public long getPosition() 
	{   		
		if (m_track != null)
		{
			return m_track.getPlaybackHeadPosition() * 1000L / m_track.getSampleRate();
		}
		
		return 0;
	}
	
	public void play() 
	{   		
		if (m_track != null)
		{			
			m_track.play();			
			m_bIsPlaying = true;
			
			m_playingThread = new Thread(new Runnable()
			{
			        public void run() 
			        {
			        	ReadAudioData();
			        }
			 }, "Audio Plyaing Thread");		 
			m_playingThread.start();
			
			
		}		
	}
	
	public void pause() 
	{
		if (m_track != null)
		{
			m_bIsPlaying = false;			
			m_track.pause();

			m_playingThread.interrupt();

		}
	}

	public void stop() 
	{
		if (m_track != null)
		{				
			m_bIsPlaying = false;
			
			m_track.stop();			
			m_listBuffer.clear();

			if(m_playingThread!=null) m_playingThread.interrupt();

		}
	}

	public void release() 
	{
		if (m_track != null) 
		{   				
			m_bIsPlaying = false;
			
			m_track.stop();			
			m_track.release();			
			m_track = null;			
		}
	}
	
	
}
