/*
 * Copyright (C) 2021 Dr.NooB
 *
 * This file is a part of Data Monitor <https://github.com/itsdrnoob/DataMonitor>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.drnoob.datamonitor.core.task;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.drnoob.datamonitor.adapters.data.AppDataUsageModel;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final String TAG = DatabaseHandler.class.getSimpleName();
    private static final String DATABASE_NAME = "appDataUsage";
    private static final int DATABASE_VERSION = 12;

    public DatabaseHandler(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.e(TAG, "onCreate: ");
        String createTable = "CREATE TABLE app_data_usage(uid INTEGER PRIMARY KEY, app_name TEXT," +
                "package_name TEXT, system_app BOOLEAN)";
//        String createTable2 = "CREATE TABLE app_data_monitor_list(uid INTEGER PRIMARY KEY, app_name TEXT," +
//                "package_name TEXT, system_app BOOLEAN)";
        db.execSQL(createTable);
//        db.execSQL(createTable2);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS app_data_usage");
        onCreate(db);
    }

    public void addData(AppDataUsageModel model) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("uid", model.getUid());
        values.put("app_name", model.getAppName());
        values.put("package_name", model.getPackageName());
        values.put("system_app", model.isSystemApp());

        try {
            database.insert("app_data_usage", null, values);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            database.close();
        }

    }

    public List<AppDataUsageModel> getUsageList() {
        List<AppDataUsageModel> mList = new ArrayList<>();
        String selectQuery = "SELECT * FROM app_data_usage";

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                AppDataUsageModel model = new AppDataUsageModel();
                model.setUid(cursor.getInt(cursor.getColumnIndex("uid")));
                model.setAppName(cursor.getString(cursor.getColumnIndex("app_name")));
                model.setPackageName(cursor.getString(cursor.getColumnIndex("package_name")));
                model.setIsSystemApp(cursor.getInt(cursor.getColumnIndex("system_app")) != 0);

                mList.add(model);
            }
            while (cursor.moveToNext());
            cursor.close();
            database.close();

        }

        return mList;
    }


    public int updateData(AppDataUsageModel model) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        return database.update("app_data_usage", values, "uid =?",
                new String[]{String.valueOf(model.getUid())});
    }

    public void createAppDataMonitorList(AppDataUsageModel model) {
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("uid", model.getUid());
        values.put("app_name", model.getAppName());
        values.put("package_name", model.getPackageName());
        values.put("system_app", model.isSystemApp());

        try {
            database.insert("app_data_monitor_list", null, values);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            database.close();
        }
    }

    public List<AppDataUsageModel> getAppMonitorList() {
        List<AppDataUsageModel> mList = new ArrayList<>();
        String selectQuery = "SELECT * FROM app_data_monitor_list";

        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                AppDataUsageModel model = new AppDataUsageModel();
                model.setUid(cursor.getInt(cursor.getColumnIndex("uid")));
                model.setAppName(cursor.getString(cursor.getColumnIndex("app_name")));
                model.setPackageName(cursor.getString(cursor.getColumnIndex("package_name")));
                model.setIsSystemApp(cursor.getInt(cursor.getColumnIndex("system_app")) != 0);

                mList.add(model);
            }
            while (cursor.moveToNext());
            cursor.close();
            database.close();

        }

        return mList;
    }
}
