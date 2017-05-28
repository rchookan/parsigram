package org.telegram.SQLite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.telegram.messenger.FileLog;
import org.telegram.ui.Components.Favourite;

public class DatabaseHandler extends SQLiteOpenHelper
{
    private static final String DATABASE_NAME = "favourites";
    private static final int DATABASE_VERSION = 1;
    private static final String KEY_CHAT_ID = "chat_id";
    private static final String KEY_ID = "id";
    private static final String TABLE_FAVS = "tbl_favs";

    public DatabaseHandler(Context paramContext)
    {
        super(paramContext, "favourites", null, 1);
    }

    public void addFavourite(Favourite paramFavourite)
    {
        SQLiteDatabase localSQLiteDatabase = getWritableDatabase();
        ContentValues localContentValues = new ContentValues();
        localContentValues.put("chat_id", Long.valueOf(paramFavourite.getChatID()));
        localSQLiteDatabase.insert("tbl_favs", null, localContentValues);
        localSQLiteDatabase.close();
    }

    public void deleteFavourite(Long paramLong)
    {
        SQLiteDatabase localSQLiteDatabase = getWritableDatabase();
        String[] arrayOfString = new String[1];
        arrayOfString[0] = String.valueOf(paramLong);
        localSQLiteDatabase.delete("tbl_favs", "chat_id = ?", arrayOfString);
        localSQLiteDatabase.close();
    }

    public Favourite getFavouriteByChatId(long paramLong)
    {
        SQLiteDatabase localSQLiteDatabase = getReadableDatabase();
        Cursor localCursor = null;
        try
        {
            String[] arrayOfString1 = { "id", "chat_id" };
            String[] arrayOfString2 = new String[1];
            arrayOfString2[0] = String.valueOf(paramLong);
            localCursor = localSQLiteDatabase.query("tbl_favs", arrayOfString1, "chat_id=?", arrayOfString2, null, null, null);
            if ((localCursor != null) && (localCursor.moveToFirst()))
            {
                Favourite localFavourite = new Favourite(localCursor.getLong(1));
                return localFavourite;
            }
            return null;
        }
        catch (Exception localException)
        {
            if (localCursor != null)
                localCursor.close();
            FileLog.e("tmessages", localException);
            return null;
        }
        finally
        {
            if (localCursor != null)
                localCursor.close();
        }
    }

    public void onCreate(SQLiteDatabase paramSQLiteDatabase)
    {
        paramSQLiteDatabase.execSQL("CREATE TABLE tbl_favs(id INTEGER PRIMARY KEY AUTOINCREMENT,chat_id INTEGER)");
    }

    public void onUpgrade(SQLiteDatabase paramSQLiteDatabase, int paramInt1, int paramInt2)
    {
        paramSQLiteDatabase.execSQL("DROP TABLE IF EXISTS tbl_favs");
        onCreate(paramSQLiteDatabase);
    }
}