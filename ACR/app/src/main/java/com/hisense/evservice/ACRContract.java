package com.hisense.evservice;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

public class ACRContract {

    protected static final String CONTENT_AUTHORITY = "com.hisense.evservice";
    protected static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    protected static final String PATH_ACR = "acr";

    public static final class TestEntry implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_ACR).build();
        protected static Uri buildUri(Uri uri, long id) {
            return ContentUris.withAppendedId(uri, id);
        }

        protected static final String MISC_TABLE_NAME = "misc";
        public static final String[] MISC_COLUMN_NAME = {"FTE", "kill_switch"};
        public static final Uri MISC_URI = CONTENT_URI.buildUpon().appendPath(MISC_TABLE_NAME).build();

        protected static final String TOKEN_TABLE_NAME = "token";
        public static final String[] TOKEN_COLUMN_NAME = {"token", "create_time", "expire_time"};
        public static final Uri TOKEN_URI = CONTENT_URI.buildUpon().appendPath(TOKEN_TABLE_NAME).build();

        protected static final String ACR_SETTINGS_TABLE_NAME = "settings";
        public static final String[] ACR_SETTINGS_COLUMN_NAME = {"acr_switch", "picture_mode_auto_adaptation", "sound_mode_auto_adaptation", "content_recognition", "pop_up", "change_date"};
        public static final Uri ACR_SETTINGS_URI = CONTENT_URI.buildUpon().appendPath(ACR_SETTINGS_TABLE_NAME).build();

        protected static final String ACR_PAQ_TABLE_NAME = "PAQ";
        public static final String[] ACR_PAQ_COLUMN_NAME = {"type", "genre", "PQ", "AQ"};
        public static final Uri ACR_PAQ_URI = CONTENT_URI.buildUpon().appendPath(ACR_PAQ_TABLE_NAME).build();

//        protected static final String ACR_DEEPLINK_TABLE_NAME = "deeplink";
//        public static final String[] ACR_DEEPLINK_COLUMN_NAME = {"title", "description", "thumbnail_url", "link_url"};
//        public static final Uri ACR_DEEPLINK_URI = CONTENT_URI.buildUpon().appendPath(ACR_DEEPLINK_TABLE_NAME).build();

        protected static final String ACR_WHITELIST_TABLE_NAME = "whitelist";
        public static final String[] ACR_WHITELIST_COLUMN_NAME = {"package", "acr", "popularity", "recommendation", "trending"};
        public static final Uri ACR_WHITELIST_URI = CONTENT_URI.buildUpon().appendPath(ACR_WHITELIST_TABLE_NAME).build();

    }
}
