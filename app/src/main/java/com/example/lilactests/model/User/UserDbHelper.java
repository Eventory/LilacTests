package com.example.lilactests.model.User;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.lilactests.model.Note.NoteContract;

/**
 * Created by Eventory on 2017/5/19.
 *
 */
public class UserDbHelper extends SQLiteOpenHelper {
    private static final String NAME = "Users.db";
    public static final int DATABASE_VERSION = 1;
    private static final String TEXT_TYPE = " TEXT";
    private static final String BOOL_TYPE = " BOOLEAN";
    private static final String DATE_TYPE = " DATETIME";
    private static final String CHAR_TYPE = " varchar(40)";
    private static final String NOT_NULL = " not null";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES = "create table " +
            NoteContract.FeedReaderContract.NoteEntry.TABLE_NAME + "(" +
            NoteContract.FeedReaderContract.NoteEntry._ID + " INTEGER PRIMARY KEY," +
            NoteContract.FeedReaderContract.NoteEntry.COLUMN_NAME_TITLE + CHAR_TYPE + NOT_NULL + COMMA_SEP +
            NoteContract.FeedReaderContract.NoteEntry.COLUMN_NAME_CONTENT + TEXT_TYPE + NOT_NULL + COMMA_SEP +
            NoteContract.FeedReaderContract.NoteEntry.COLUMN_NAME_ALARM + BOOL_TYPE + " Default false" + COMMA_SEP +
            NoteContract.FeedReaderContract.NoteEntry.COLUMN_NAME_ALARM_TIME + DATE_TYPE + COMMA_SEP +
            NoteContract.FeedReaderContract.NoteEntry.COLUMN_NAME_CREATE_TIME + DATE_TYPE + NOT_NULL + ");";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + NoteContract.FeedReaderContract.NoteEntry.TABLE_NAME;

    public UserDbHelper(Context context) {
        super(context, NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}
