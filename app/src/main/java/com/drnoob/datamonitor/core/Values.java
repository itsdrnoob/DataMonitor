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

package com.drnoob.datamonitor.core;

public class Values {
    public static final int DATA_USAGE_NOTIFICATION_ID = 0x0045;
    public static final String DATA_USAGE_NOTIFICATION_CHANNEL_ID = "DataUsage.Notification";
    public static final String DATA_USAGE_NOTIFICATION_CHANNEL_NAME = "Data Usage";
    public static final String DATA_USAGE_NOTIFICATION_NOTIFICATION_GROUP = "Data Usage";
    public static final int DATA_USAGE_WARNING_NOTIFICATION_ID = 0x00A0;
    public static final String DATA_USAGE_WARNING_CHANNEL_ID = "DataUsage.Warning";
    public static final String DATA_USAGE_WARNING_CHANNEL_NAME = "Data Usage Warning";
    public static final int APP_DATA_USAGE_WARNING_NOTIFICATION_ID = 0x00BE; // 190
    public static final String APP_DATA_USAGE_WARNING_CHANNEL_ID = "AppDataUsage.Warning";
    public static final String APP_DATA_USAGE_WARNING_CHANNEL_NAME = "App Data Usage Warning";
    public static final int NETWORK_SIGNAL_NOTIFICATION_ID = 0x010D;
    public static final String NETWORK_SIGNAL_CHANNEL_ID = "NetworkSignal.Notification";
    public static final String NETWORK_SIGNAL_CHANNEL_NAME = "Network Speed Monitor";
    public static final String NETWORK_SIGNAL_NOTIFICATION_GROUP = "Network Speed Monitor";
    public static final int OTHER_NOTIFICATION_ID = 0x012C;
    public static final String OTHER_NOTIFICATION_CHANNEL_ID = "Other.Notification";
    public static final String OTHER_NOTIFICATION_CHANNEL_NAME = "Other";
    public static final String DEFAULT_NOTIFICATION_GROUP = "Default";


    public static final int SESSION_TODAY = 0x000A;
    public static final int SESSION_YESTERDAY = 0x0014;
    public static final int SESSION_THIS_MONTH = 0x001E;
    public static final int SESSION_LAST_MONTH = 0x0028;
    public static final int SESSION_THIS_YEAR = 0x0032;
    public static final int SESSION_ALL_TIME = 0x003C;
    public static final int SESSION_MONTHLY = 0x00A9;
    public static final int SESSION_CUSTOM = 0x00AC;

    public static final int TYPE_MOBILE_DATA = 0x0046;
    public static final int TYPE_WIFI = 0x0050;

    public static final String DATA_USAGE_VALUE = "data_usage_value";
    public static final String DATA_USAGE_SESSION = "data_usage_session";
    public static final String DATA_USAGE_TYPE = "data_usage_type";
    public static final int DATA_USAGE_SYSTEM = 0x005A;
    public static final int DATA_USAGE_USER = 0x0064;
    public static final int DATA_USAGE_TODAY = 0x00D3;

    public static final String GENERAL_FRAGMENT_ID = "GENERAL_FRAGMENT_ID";

    public static final int ABOUT_FRAGMENT = 0x006E;
    public static final int LICENSE_FRAGMENT = 0x0078;
    public static final int CONTRIBUTORS_FRAGMENT = 0x0082;
    public static final int DONATE_FRAGMENT = 0x008C;
    public static final int APP_LICENSE_FRAGMENT = 0x0096;
    public static final int OSS_LICENSE_FRAGMENT = 0x00DC;
    public static final int APP_DATA_LIMIT_FRAGMENT = 0x00AA;
    public static final int NETWORK_STATS_FRAGMENT = 0x00C8;
    public static final int APP_LANGUAGE_FRAGMENT = 0x00D2;
    public static final int DISABLE_BATTERY_OPTIMISATION_FRAGMENT = 0x00E6;


    public static final int BOTTOM_NAVBAR_ITEM_HOME = 0;
    public static final int BOTTOM_NAVBAR_ITEM_SETUP = 1;
    public static final int BOTTOM_NAVBAR_ITEM_APP_DATA_USAGE = 2;
    public static final int BOTTOM_NAVBAR_ITEM_SETTINGS = 3;

    public static final String SETUP_VALUE = "SETUP_VALUE";
    public static final int USAGE_ACCESS_DISABLED = 0x00B4;
    public static final int READ_PHONE_STATE_DISABLED = 0x00B5;
    public static final int REQUEST_READ_PHONE_STATE = 2011;

    public static final String SETUP_COMPLETED = "is_setup_complete";
    public static final String DATA_LIMIT = "data_limit";
    public static final String DATA_TYPE = "data_type";
    public static final String LIMIT = "limit";
    public static final String DATA_RESET = "data_reset";
    public static final String DATA_RESET_DAILY = "daily";
    public static final String DATA_RESET_MONTHLY = "monthly";
    public static final String DATA_RESET_CUSTOM = "custom";
    public static final String DATA_RESET_HOUR = "reset_hour";
    public static final String DATA_RESET_MIN = "reset_min";
    public static final String DATA_RESET_DATE = "reset_date";
    public static final String DATA_RESET_CUSTOM_DATE_START = "custom_reset_date_start";
    public static final String DATA_RESET_CUSTOM_DATE_END = "custom_reset_date_end";
    public static final String DATA_RESET_CUSTOM_DATE_RESTART = "custom_reset_date_restart";
    public static final String DATA_WARNING_TRIGGER_LEVEL = "data_warning_trigger_level";
    public static final String DATA_USAGE_WARNING_SHOWN = "data_usage_warning_shown";
    public static final String DATA_USAGE_ALERT = "data_usage_alert";
    public static final String WIDGET_REFRESH_INTERVAL_SUMMARY = "widget_refresh_interval_summary";
    public static final String WIDGET_REFRESH_INTERVAL = "widget_refresh_interval";
    public static final String NOTIFICATION_REFRESH_INTERVAL_SUMMARY = "notification_refresh_interval_summary";
    public static final String NOTIFICATION_REFRESH_INTERVAL = "notification_refresh_interval";
    public static final String NOTIFICATION_MOBILE_DATA = "notification_mobile_data";
    public static final String NOTIFICATION_WIFI = "notification_wifi";
    public static final String APP_LANGUAGE = "app_language";
    public static final String APP_LANGUAGE_CODE = "app_language_code";
    public static final String APP_COUNTRY_CODE = "app_country_code";
    public static final String DAILY_DATA_HOME_ACTION = "daily_data_home_action";
    public static final String APP_THEME = "app_theme";
    public static final String APP_THEME_SUMMARY = "app_theme_summary";

    public static final String DARK_MODE_TOGGLE = "dark_mode_toggle";

    public static final String USAGE_SESSION_TODAY = "Today";
    public static final String USAGE_SESSION_YESTERDAY = "Yesterday";
    public static final String USAGE_SESSION_THIS_MONTH = "This Month";
    public static final String USAGE_SESSION_LAST_MONTH = "Last Month";
    public static final String USAGE_SESSION_THIS_YEAR = "This Year";
    public static final String USAGE_SESSION_ALL_TIME = "All Time";

    public static final String USAGE_TYPE_MOBILE_DATA = "Mobile Data";
    public static final String USAGE_TYPE_WIFI = "Wifi";

    public static final String MAX_DOWNLOAD_SPEED = "max_download_speed";
    public static final String AVG_DOWNLOAD_SPEED = "avg_download_speed";
    public static final String MAX_UPLOAD_SPEED = "max_upload_speed";
    public static final String AVG_UPLOAD_SPEED = "avg_upload_speed";
    public static final String MIN_LATENCY = "min_latency";
    public static final String AVG_LATENCY = "avg_latency";
    public static final String NETWORK_IP = "network_ip";
    public static final String ISP = "isp";
    public static final String SERVER = "server";
    public static final String REGION = "region";

    public static final String UPDATE_VERSION = "update_version";
    public static final String MD5_GITHUB = "39aa537128b70c2886cb771c33944a7d";

    public static final String CRASH_REPORT_KEY = "datamonitor.crashReport";

    public static final String INTENT_ACTION = "datamonitor.intent.action";
    public static final String ACTION_SHOW_DATA_PLAN_NOTIFICATION = "datamonitor.intent.action.dataPlanNotification";

}
