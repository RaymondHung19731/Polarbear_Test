package com.ultraflymodel.polarbear.mike;

/**
 * Created by lghost2018 on 2018/4/11.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import android.util.Log;

import com.ultraflymodel.polarbear.fragment.PolarbearMainFragment;
import com.ultraflymodel.polarbear.mike.UDPNetwork;

public class FileOperations {
    public FileOperations() {

    }

    public Boolean write(String fname, String fcontent){
        try {

            Calendar rightNow = Calendar.getInstance();
            long mili_time = rightNow.getTimeInMillis();
            long year = rightNow.get(Calendar.YEAR);
            long month = rightNow.get(Calendar.MONTH);
            long day = rightNow.get(Calendar.DAY_OF_MONTH);
            long hour = rightNow.get(Calendar.HOUR_OF_DAY);
            long minute = rightNow.get(Calendar.MINUTE);
            long second = rightNow.get(Calendar.SECOND);
            long millisecond = rightNow.get(Calendar.MILLISECOND);
            String fpath = "/sdcard/data_"+ year + "_" + month + "_" + day + "_" + hour + "_" + minute + "_" + second+".txt";
            String save_time = "Save time: " + Long.toString(mili_time)  + "\r\n";
            String save_data;

            File file = new File(fpath);

            // If file does not exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(save_time);
            UDPNetwork.Buffer_save_flag = 0;

            save_data = UDPNetwork.Buffer_mTime.toString() + "\r\n";
            bw.write(save_data);
            save_data = UDPNetwork.Buffer_fps.toString() + "\r\n";
            bw.write(save_data);
            save_data = UDPNetwork.Buffer_pps.toString() + "\r\n";
            bw.write(save_data);

            int i;
 //           save_data = "";
//            int length = UDPNetwork.Buffer_mTime.
            for (i=0; i<2000; i++)
            {
              save_data = i + "," + String.valueOf(UDPNetwork.Buffer_mTime.get()) +"," + String.valueOf(UDPNetwork.Buffer_fps.get()) +"," + String.valueOf(UDPNetwork.Buffer_pps.get()) + "\r\n";
 //               save_data = save_data + "\r\n" +  i + "," + String.valueOf(UDPNetwork.Buffer_mTime.get()) +"," + String.valueOf(UDPNetwork.Buffer_fps.get()) +"," + String.valueOf(UDPNetwork.Buffer_pps.get()) + "\r\n";
                bw.write(save_data);

            }


            UDPNetwork.Buffer_save_flag = 1;
//            UDPNetwork.Buffer_mTime.get();
//            UDPNetwork.Buffer_fps.get();
//            UDPNetwork.Buffer_pps.get();

//            bw.write(fcontent);
//            bw.write(fcontent);
//            bw.write(fcontent);
            bw.close();

            Log.d("Suceess","Sucess");
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }


    }

    public String read(String fname){

        BufferedReader br = null;
        String response = null;

        try {

            StringBuffer output = new StringBuffer();
            String fpath = "/sdcard/"+fname+".txt";

            br = new BufferedReader(new FileReader(fpath));
            String line = "";
            while ((line = br.readLine()) != null) {
                output.append(line +"n");
            }
            response = output.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null;

        }
        return response;

    }
}