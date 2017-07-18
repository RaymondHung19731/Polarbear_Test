package com.ultraflymodel.polarbear.model;

import android.database.Cursor;

import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.ultraflymodel.polarbear.common.DBA;


@Table(name = DBA.Table.MUSICLIST)
public class MusicList extends Model {

    @Column(name = "_id")
    public long id;
    @Column(name = DBA.Field.MUSICLISTKEY, unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    public String musiclistkey;
    @Column
    public int play_selected;
    @Column
    public int wakeup_selected;
    @Column
    public String path;
    @Column
    public String file_name;
    @Column
    public String artist;
    @Column
    public String album;
    @Column
    public String title;
    @Column
    public int mbsize;
    @Column
    public int send_to_play;
    @Column
    public int now_playing;
    @Column
    public int now_sending;

    public static Cursor fetchResultCursor() {
        String tableName = Cache.getTableInfo(MusicList.class).getTableName();
        // Query all items without any conditions
        String resultRecords = new Select(tableName + ".*, " + tableName + ".Id as _id").
                from(MusicList.class).toSql();
        // Execute query on the underlying ActiveAndroid SQLite database
        Cursor resultCursor = Cache.openDatabase().rawQuery(resultRecords, null);
        return resultCursor;
    }

}
