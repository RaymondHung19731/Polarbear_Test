package com.ultraflymodel.polarbear.utils;


import com.ultraflymodel.polarbear.common.Constants;

import java.io.IOException;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.Random;

/**
 * Created by rudy on 15/12/28.
 */
public class StringUtil {
    private static final String TAG = StringUtil.class.getSimpleName();

    private static final String JRSYS_ACCOUNT_DEFAULT_VALUE = "尚未設定";
    private static final String ALLOWED_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnm";

    private StringUtil() {
    }

    public static String getRandomString(int sizeOfRandomString){
        Random random = new Random();
        StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for(int i=0;i<sizeOfRandomString;++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }

    /**
     * Check if it is JRSYS default string.
     * JRSYS default string "尚未設定" for 會員資料
     *
     * @param text
     * @return
     */
    public static final boolean isJrsysStrValid(String text) {
        if (!isStrNullOrEmpty(text) && !StringUtil.JRSYS_ACCOUNT_DEFAULT_VALUE.equals(text)) {
            return true;
        }
        return false;
    }


    public static String addquote(String name){
        return "'"+ name + "'";
    }

    /**
     * Check String is null or empty string
     *
     * @param str
     * @return
     */
    public static final boolean isStrNullOrEmpty(String str) {
        if (str == null || Constants.EMPTY_STRING.equals(str)) {
            return true;
        }
        return false;
    }

    /**
     * Check String is null or empty string
     *
     * @param str
     * @return
     */
    public static final boolean isStrNullOrEmpty(String... str) {

        if (str != null) {
            for (int index = 0; index < str.length; index++) {
                if (str[index] == null || Constants.EMPTY_STRING.equals(str[index])) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }


    public static String priceFormat(Double price) {
        DecimalFormat formatter = new DecimalFormat("###,###,###");
        return formatter.format(price);
    }

    public static String priceFormat(Float price) {
        DecimalFormat formatter = new DecimalFormat("###,###,###");
        return formatter.format(price);
    }

    public static String priceWithDecimal(Double price) {
        DecimalFormat formatter = new DecimalFormat("###,###,###.-");
        return "$" + formatter.format(price);
    }

    public static String priceWithDecimalNoSymbol(Double price) {
        DecimalFormat formatter = new DecimalFormat("###,###,###.-");
        return formatter.format(price);
    }


    public static String big52unicode(String strBIG5) {
        String strReturn = "";
        try {
            strReturn = new String(strBIG5.getBytes("big5"), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return strReturn;
    }

    public static String unicode2big5(String strUTF8) {
        String strReturn = "";
        try {
            strReturn = new String(strUTF8.getBytes("UTF-8"), "big5");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return strReturn;
    }

    public static String UnicodeToStr(String unicodeStr) throws Exception {
        StringBuffer outStrBuf = new StringBuffer();
        String uCodes[] = unicodeStr.trim().split("\\\\u");
        for (String uc : uCodes) {
            if (uc.trim().isEmpty()) continue;
            byte bs[] = HexByteKit.Hex2Byte(uc.trim());
            if (bs != null) {
                String str = new String(bs, "Unicode");
                outStrBuf.append(str);
            } else System.err.printf("Illegal uc=%s\n", uc);
        }

        return outStrBuf.toString();
    }

    public static String UnicodeToChinese(String unicodeStr)  {
        StringReader sr = new StringReader(unicodeStr);
        UnicodeUnescapeReader uur = new UnicodeUnescapeReader(sr);

        StringBuffer buf = new StringBuffer();
        try {
            for(int c = uur.read(); c != -1; c = uur.read())
            {
                buf.append((char)c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buf.toString();
    }

    public static String getBCAddress(String myipaddress) {
        String strReturn = "";
        StringBuilder strBuilder = new StringBuilder();
        String[] ip = myipaddress.split("\\.");
        if(ip.length==4){
            ip[3] = "255";
            for(int i=0; i<4; i++){
                strBuilder.append(ip[i]);
                if(i!=3) strBuilder.append(".");
            }
            strReturn = strBuilder.toString();
        }
        return strReturn;
    }
}
