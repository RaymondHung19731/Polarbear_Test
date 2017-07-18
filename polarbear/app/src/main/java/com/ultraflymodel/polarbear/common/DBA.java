package com.ultraflymodel.polarbear.common;

public class DBA {

    public static final String DATABASE_NAME = "polarbear.db";

    public static final class Table {
        public static final String MUSICLIST = "MusicList";
        public static final String WIFILIST = "WifiList";
        public static final String SETTINGS = "Settings";
        public static final String LOGS = "Logs";
    }

    public static final class Field {
        public static final String ID = "Id";
        public static final String MUSICLISTKEY = "musiclistkey";
        public static final String PATH = "path";
        public static final String FILE_NAME = "file_name";
        public static final String PLAYSELECTED = "play_selected";
        public static final String WAKEUPSELECTED = "wakeup_selected";
        public static final String ARTIST = "artist";
        public static final String ALBUM = "album";
        public static final String TITLE = "title";
        public static final String MBSIZE = "mbsize";
        public static final String MYCOPPERIP = "my_copper_ip";
        public static final String MYCOPPERNAME = "my_copper_name";
        public static final String AUDIOON = "audio_on";
        public static final String BROADCASTSET = "broadcast_set";
        public static final String VIDEOON = "video_on";
        public static final String PIRON = "pir_on";
        public static final String MICON = "mic_on";
        public static final String MUSICON = "music_on";
        public static final String WIFINAME = "wifi_name";
        public static final String WIFICH = "wifi_channel";
        public static final String WIFIDBM = "wifi_dbm";
        public static final String WIFISECURITY = "wifi_security";
        public static final String WIFIPASSWORD = "wifi_password";
        public static final String NOWPLAYING = "now_playing";
        public static final String NOWSENDING = "now_sending";
    }
}