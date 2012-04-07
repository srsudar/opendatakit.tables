package yoonsung.odk.spreadsheet.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yoonsung.odk.spreadsheet.data.Query.SqlData;
import yoonsung.odk.spreadsheet.sync.SyncUtil;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * A class for accessing and modifying a user table.
 * 
 * @author hkworden@gmail.com (Hilary Worden)
 */
public class DbTable {
    
    public static final String DB_ROW_ID = "id";
    public static final String DB_SRC_PHONE_NUMBER = "srcPhoneNum";
    public static final String DB_LAST_MODIFIED_TIME = "lastModTime";
    public static final String DB_SYNC_TAG = "syncTag";
    public static final String DB_SYNC_STATE = "syncState";
    public static final String DB_TRANSACTIONING = "transactioning";
    
    private final DataUtil du;
    private final DbHelper dbh;
    private final TableProperties tp;
    
    public static DbTable getDbTable(DbHelper dbh, String tableId) {
        return new DbTable(dbh, tableId);
    }
    
    private DbTable(DbHelper dbh, String tableId) {
        this.du = DataUtil.getDefaultDataUtil();
        this.dbh = dbh;
        this.tp = TableProperties.getTablePropertiesForTable(dbh, tableId);
    }
    
    static void createDbTable(SQLiteDatabase db, TableProperties tp) {
        db.execSQL("CREATE TABLE " + tp.getDbTableName() + "(" +
                       DB_ROW_ID + " TEXT UNIQUE NOT NULL" +
                ", " + DB_SRC_PHONE_NUMBER + " TEXT" +
                ", " + DB_LAST_MODIFIED_TIME + " TEXT NOT NULL" +
                ", " + DB_SYNC_TAG + " TEXT" +
                ", " + DB_SYNC_STATE + " INTEGER NOT NULL" +
                ", " + DB_TRANSACTIONING + " INTEGER NOT NULL" +
                ")");
    }
    
    /**
     * @return a raw table of all the data in the table
     */
    public Table getRaw() {
        return getRaw(null, null, null, null);
    }
    
    /**
     * Gets a table of raw data.
     * @param columns the columns to select (if null, all columns will be
     * selected)
     * @param selectionKeys the column names for the WHERE clause (can be null)
     * @param selectionArgs the selection arguments (can be null)
     * @param orderBy the column to order by (can be null)
     * @return a Table of the requested data
     */
    public Table getRaw(String[] columns, String[] selectionKeys,
            String[] selectionArgs, String orderBy) {
        if (columns == null) {
            ColumnProperties[] cps = tp.getColumns();
            columns = new String[cps.length + 5];
            columns[0] = DB_SRC_PHONE_NUMBER;
            columns[1] = DB_LAST_MODIFIED_TIME;
            columns[2] = DB_SYNC_TAG;
            columns[3] = DB_SYNC_STATE;
            columns[4] = DB_TRANSACTIONING;
            for (int i = 0; i < cps.length; i++) {
                columns[i + 5] = cps[i].getColumnDbName();
            }
        }
        String[] colArr = new String[columns.length + 1];
        colArr[0] = DB_ROW_ID;
        for (int i = 0; i < columns.length; i++) {
            colArr[i + 1] = columns[i];
        }
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.query(tp.getDbTableName(), colArr,
                buildSelectionSql(selectionKeys),
                selectionArgs, null, null, orderBy);
        Table table = buildTable(c, columns);
        c.close();
        db.close();
        return table;
    }
    
    public Table getRaw(Query query, String[] columns) {
        return dataQuery(query.toSql(columns));
    }
    
    public UserTable getUserTable(Query query) {
        Table table = dataQuery(query.toSql(tp.getColumnOrder()));
        return new UserTable(table.getRowIds(), getUserHeader(),
                table.getData(), footerQuery(query));
    }
    
    public UserTable getUserOverviewTable(Query query) {
        Table table = dataQuery(query.toOverviewSql(tp.getColumnOrder()));
        return new UserTable(table.getRowIds(), getUserHeader(),
                table.getData(), footerQuery(query));
    }
    
    private Table dataQuery(SqlData sd) {
        SQLiteDatabase db = dbh.getReadableDatabase();
        Log.d("DBTSQ", sd.getSql());
        Cursor c = db.rawQuery(sd.getSql(), sd.getArgs());
        Table table = buildTable(c, tp.getColumnOrder());
        c.close();
        db.close();
        return table;
    }
    
    /**
     * Builds a Table with the data from the given cursor.
     * The cursor, but not the columns array, must include the row ID column.
     */
    private Table buildTable(Cursor c, String[] columns) {
        int[] colIndices = new int[columns.length];
        int rowCount = c.getCount();
        String[] rowIds = new String[rowCount];
        String[][] data = new String[rowCount][columns.length];
        int rowIdIndex = c.getColumnIndexOrThrow(DB_ROW_ID);
        for (int i = 0; i < columns.length; i++) {
            colIndices[i] = c.getColumnIndexOrThrow(columns[i]);
        }
        c.moveToFirst();
        for (int i = 0; i < rowCount; i++) {
            rowIds[i] = c.getString(rowIdIndex);
            for (int j = 0; j < columns.length; j++) {
                data[i][j] = c.getString(colIndices[j]);
            }
            c.moveToNext();
        }
        return new Table(rowIds, columns, data);
    }
    
    private String[] getUserHeader() {
        ColumnProperties[] cps = tp.getColumns();
        String[] header = new String[cps.length];
        for (int i = 0; i < header.length; i++) {
            header[i] = cps[i].getDisplayName();
        }
        return header;
    }
    
    private String[] footerQuery(Query query) {
        ColumnProperties[] cps = tp.getColumns();
        List<String> sc = new ArrayList<String>();
        for (ColumnProperties cp : cps) {
            String colDbName = cp.getColumnDbName();
            int mode = cp.getFooterMode();
            switch (mode) {
            case ColumnProperties.FooterMode.COUNT:
                sc.add("COUNT(" + colDbName + ") AS " + colDbName);
                break;
            case ColumnProperties.FooterMode.MAXIMUM:
                sc.add(", MAX(" + colDbName + ") AS " + colDbName);
                break;
            case ColumnProperties.FooterMode.MEAN:
                sc.add(", COUNT(" + colDbName + ") AS count" + colDbName);
                sc.add(", SUM(" + colDbName + ") AS sum" + colDbName);
                break;
            case ColumnProperties.FooterMode.MINIMUM:
                sc.add(", MIN(" + colDbName + ") AS " + colDbName);
                break;
            case ColumnProperties.FooterMode.SUM:
                sc.add(", SUM(" + colDbName + ") AS " + colDbName);
                break;
            }
        }
        String[] footer = new String[cps.length];
        SqlData sd = query.toSql(sc);
        SQLiteDatabase db = dbh.getReadableDatabase();
        Cursor c = db.rawQuery(sd.getSql(), sd.getArgs());
        c.moveToFirst();
        for (int i = 0; i < cps.length; i++) {
            if (cps[i].getFooterMode() == ColumnProperties.FooterMode.MEAN) {
                int sIndex = c.getColumnIndexOrThrow("sum" +
                        cps[i].getColumnDbName());
                int cIndex = c.getColumnIndexOrThrow("count" +
                        cps[i].getColumnDbName());
                double sum = c.getInt(sIndex);
                int count = c.getInt(cIndex);
                footer[i] = String.valueOf(sum / count);
            } else if (cps[i].getFooterMode() !=
                    ColumnProperties.FooterMode.NONE) {
                int index = c.getColumnIndexOrThrow(cps[i].getColumnDbName());
                footer[i] = c.getString(index);
            }
        }
        c.close();
        db.close();
        return footer;
    }
    
    /**
     * Adds a row to the table with the given values, no source phone number,
     * the current time as the last modification time, and an inserting
     * synchronization state.
     */
    public void addRow(Map<String, String> values) {
        addRow(values, null, null);
    }
    
    /**
     * Adds a row to the table with an inserting synchronization state and the
     * transactioning status set to false.
     */
    public void addRow(Map<String, String> values, String lastModTime,
            String srcPhone) {
        if (lastModTime == null) {
            lastModTime = du.formatNowForDb();
        }
        ContentValues cv = new ContentValues();
        for (String column : values.keySet()) {
            cv.put(column, values.get(column));
        }
        cv.put(DB_LAST_MODIFIED_TIME, lastModTime);
        cv.put(DB_SRC_PHONE_NUMBER, srcPhone);
        cv.put(DB_SYNC_STATE, SyncUtil.State.INSERTING);
        cv.put(DB_TRANSACTIONING, SyncUtil.Transactioning.FALSE);
        actualAddRow(cv);
    }
    
    /**
     * Actually adds a row.
     * @param values the values to put in the row
     */
    public void actualAddRow(ContentValues values) {
        String id = UUID.randomUUID().toString();
        values.put(DB_ROW_ID, id);
        SQLiteDatabase db = dbh.getWritableDatabase();
        long result = db.insert(tp.getDbTableName(), null, values);
        db.close();
        Log.d("DBT", "insert, id=" + result);
    }
    
    /**
     * Updates a row in the table with the given values, no source phone
     * number, and the current time as the last modification time.
     */
    public void updateRow(String rowId, Map<String, String> values) {
        updateRow(rowId, values, null, du.formatNowForDb());
    }
    
    /**
     * Updates a row in the table and marks its synchronization state as
     * updating.
     * @param rowId the ID of the row to update
     * @param values the values to update the row with
     * @param srcPhone the source phone number to put in the row
     * @param lastModTime the last modification time to put in the row
     */
    public void updateRow(String rowId, Map<String, String> values,
            String srcPhone, String lastModTime) {
        ContentValues cv = new ContentValues();
        cv.put(DB_SYNC_STATE, SyncUtil.State.UPDATING);
        for (String column : values.keySet()) {
            cv.put(column, values.get(column));
        }
        actualUpdateRowByRowId(rowId, cv);
    }
    
    /**
     * Actually updates a row.
     * @param rowId the ID of the row to update
     * @param values the values to update the row with
     */
    public void actualUpdateRowByRowId(String rowId, ContentValues values) {
        String[] whereArgs = { rowId };
        actualUpdateRow(values, DB_ROW_ID + " = ?", whereArgs);
    }
    
    private void actualUpdateRow(ContentValues values, String where,
            String[] whereArgs) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.update(tp.getDbTableName(), values, where, whereArgs);
        db.close();
    }
    
    /**
     * Marks the given row as deleted.
     */
    public void markDeleted(String rowId) {
        String[] whereArgs = { rowId };
        ContentValues values = new ContentValues();
        values.put(DB_SYNC_STATE, SyncUtil.State.DELETING);
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.update(tp.getDbTableName(), values, DB_ROW_ID + " = ?", whereArgs);
        db.close();
    }
    
    /**
     * Actually deletes a row from the table.
     * @param rowId the ID of the row to delete
     */
    public void deleteRowActual(String rowId) {
        String[] whereArgs = { rowId };
        SQLiteDatabase db = dbh.getWritableDatabase();
        db.delete(tp.getDbTableName(), DB_ROW_ID + " + ?", whereArgs);
        db.close();
    }
    
    /**
     * Builds a string of SQL for selection with the given column names.
     */
    private String buildSelectionSql(String[] selectionKeys) {
        if ((selectionKeys == null) || (selectionKeys.length == 0)) {
            return null;
        }
        StringBuilder selBuilder = new StringBuilder();
        for (String key : selectionKeys) {
            selBuilder.append(" AND " + key + " = ?");
        }
        selBuilder.delete(0, 5);
        return selBuilder.toString();
    }
}
