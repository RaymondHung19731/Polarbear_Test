package com.ultraflymodel.polarbear.mike;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;


//https://android.googlesource.com/platform/cts/+/kitkat-release/tests/tests/media/src/android/media/cts

public class VideoDecoder {

    private static final String TAG = "EncodeDecodeTest";
    private static final boolean VERBOSE = false;
	public final  int  INBUF_SIZE =  4096;
	public final  int  FF_INPUT_BUFFER_PADDING_SIZE = 16;
	
	private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
	// size of a frame, in pixels
    private int mWidth = 1280;
    private int mHeight = 720;
    MediaCodec decoder = null;

    public Surface m_surface = null;
    private static final int FRAME_RATE = 30;               // 15fps
    ByteBuffer[] decoderInputBuffers = null;
    ByteBuffer[] decoderOutputBuffers = null;
    
    
    int       m_iIndex = 0;
    boolean   decoderConfigured = false;
	public VideoDecoder()
	{


		 decoderConfigured = false;
        try {

            decoder = MediaCodec.createDecoderByType(MIME_TYPE);

        } catch (IOException e) {
            e.printStackTrace();
        }
        //outputSurface = new OutputSurface(mWidth, mHeight);
		 m_iIndex = 1;
	}
	
	
	/**
     * Does the actual work for encoding frames from buffers of byte[].
     */
     public  void doDecodeVideoFromBuffer(byte[]  encodedData,int iLen)            
    {        
    	final int TIMEOUT_USEC = 10000;                                  
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        
        MediaFormat decoderOutputFormat = null;
        int checkIndex = 0;                                                             
        // Loop until the output side is done.       
        boolean encoderDone = false;
        boolean outputDone = false;        
        
        if(!decoderConfigured)
        {
            try {
                MediaFormat format =
                        MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

                ByteBuffer byBuffer = ByteBuffer.wrap(encodedData, 0, iLen);
                format.setByteBuffer("csd-0", byBuffer);


                decoder.configure(format, m_surface,
                        null, 0);
                decoder.start();

                decoderInputBuffers = decoder.getInputBuffers();
                decoderOutputBuffers = decoder.getOutputBuffers();
                decoderConfigured = true;
            }catch(Exception ex)
            {

            }
        }

        info.size  = iLen;


            long ptsUsec = computePresentationTime(m_iIndex);
            info.presentationTimeUs = ptsUsec;
        
                 int inputBufIndex = decoder.dequeueInputBuffer(-1);  //mike mark

                 if(inputBufIndex>=0) {

                     ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                     inputBuf.clear();
                     inputBuf.put(encodedData, 0, iLen);
                     decoder.queueInputBuffer(inputBufIndex, 0, info.size,
                             info.presentationTimeUs, info.flags);
                 }

                int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // The storage associated with the direct ByteBuffer may already be unmapped,
                    // so attempting to access data through the old output buffer array could
                    // lead to a native crash.
                    if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
                    decoderOutputBuffers = decoder.getOutputBuffers();
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // this happens before the first frame is returned
                    decoderOutputFormat = decoder.getOutputFormat();
                    if (VERBOSE) Log.d(TAG, "decoder output format changed: " +
                            decoderOutputFormat);
                } else if (decoderStatus < 0) 
                {
                	Log.d(TAG,"unexpected result from deocder.dequeueOutputBuffer: " + decoderStatus);
                } else 
                {  // decoderStatus >= 0
                    
                        if (VERBOSE) Log.d(TAG, "surface decoder given buffer " + decoderStatus +
                                " (size=" + info.size + ")");
                        
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            if (VERBOSE) Log.d(TAG, "output EOS");
                            outputDone = true;
                        }
                        boolean doRender = (info.size != 0);

                        // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                        // to SurfaceTexture to convert to a texture.  The API doesn't guarantee
                        // that the texture will be available before the call returns, so we
                        // need to wait for the onFrameAvailable callback to fire.
                        decoder.releaseOutputBuffer(decoderStatus, doRender);
                        if (doRender)
                        {
                            if (VERBOSE) Log.d(TAG, "awaiting frame " + inputBufIndex);
                            m_iIndex++;
                        }
                }
           

                      
    }


	/**
     * Generates the presentation time for frame N, in microseconds.
     */
    private static long computePresentationTime(int frameIndex) {
        return 132 + frameIndex * 1000000 / FRAME_RATE;
    }
    
	
	public void CloseCodec()
	{		


	}
	
}
