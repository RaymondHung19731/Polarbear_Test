package com.ultraflymodel.polarbear.model;

import android.database.Cursor;

import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;


@Table(name = "Settings", id = "_id")
public class Settings extends Model {

    @Column(name = "id", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    public long id;
    @Column
    public String my_copper_ip;
    @Column
    public String my_copper_name;
    @Column
    public int audio_on;
    @Column
    public int broadcast_set;
    @Column
    public int video_on;
    @Column
    public int pir_on;
    @Column
    public int mic_on;
    @Column
    public int music_on;
    @Column
    public String wifi_name;
    @Column
    public String wifi_password;

    public static Cursor fetchResultCursor() {
        String tableName = Cache.getTableInfo(Settings.class).getTableName();
        // Query all items without any conditions
        String resultRecords = new Select(tableName + ".*, " + tableName + ".Id as _id").
                from(Settings.class).toSql();
        // Execute query on the underlying ActiveAndroid SQLite database
        Cursor resultCursor = Cache.openDatabase().rawQuery(resultRecords, null);
        return resultCursor;
    }

}
