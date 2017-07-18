package com.ultraflymodel.polarbear.model;

import android.database.Cursor;

import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.ultraflymodel.polarbear.common.DBA;


@Table(name = DBA.Table.WIFILIST)
public class WifiList extends Model {

    @Column(name = "_id")
    public long id;
    @Column(name = DBA.Field.WIFINAME, unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    public String wifi_name;
    @Column
    public int wifi_channel;
    @Column
    public String wifi_dbm;
    @Column
    public String wifi_security;

    public static Cursor fetchResultCursor() {
        String tableName = Cache.getTableInfo(WifiList.class).getTableName();
        // Query all items without any conditions
        String resultRecords = new Select(tableName + ".*, " + tableName + ".Id as _id").
                from(WifiList.class).toSql();
        // Execute query on the underlying ActiveAndroid SQLite database
        Cursor resultCursor = Cache.openDatabase().rawQuery(resultRecords, null);
        return resultCursor;
    }

}
