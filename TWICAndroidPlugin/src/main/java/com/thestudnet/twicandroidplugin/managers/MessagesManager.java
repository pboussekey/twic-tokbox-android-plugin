package com.thestudnet.twicandroidplugin.managers;

import android.content.ContentValues;
import android.util.Log;

import com.squareup.otto.Subscribe;
import com.thestudnet.twicandroidplugin.events.EventBus;
import com.thestudnet.twicandroidplugin.events.MessageInteraction;
import com.thestudnet.twicandroidplugin.events.TokBoxInteraction;
import com.thestudnet.twicandroidplugin.libs.JsonManager;
import com.thestudnet.twicandroidplugin.models.GenericModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * INTERACTIVE LAYER
 * Created by Baptiste PHILIBERT on 27/06/2017.
 */

public class MessagesManager extends JsonManager {

    private static final String TAG = "com.thestudnet.twicandroidplugin.managers." + MessagesManager.class.getSimpleName();

    public int getOldestId() {
        return oldestId;
    }
    private int oldestId = -1;

    private ArrayList<GenericModel> messages;

    private static MessagesManager instance;
    public MessagesManager() {
    }
    public static MessagesManager getInstance() {
        if(instance == null) {
            MessagesManager mInstance = new MessagesManager();
            instance = mInstance;
            EventBus.getInstance().register(instance);
            instance.contentValues = new ContentValues();
            instance.messages = new ArrayList<>();
            return instance;
        } else {
            return instance;
        }
    }

    public void registerMessageManager() {
    }

    public void unregisterMessageManager() {
    }

    /**
     *
     * @param json
     * @return the number of messages that were inserted
     */

    /**
     * Parse and insert a json string of messages (the first in the json is inserted last)
     * @param json
     * @param atTheEnd If true, insert each message at the end of the current list, otherwise insert each message at the beginning of the current list
     * @return
     */
    public int insertMessages(String json, boolean atTheEnd) {

            //        {
            //            "count":51,
            //            "list":[
            //                {
            //                    "created_date":"2017-06-15T08:10:49Z",
            //                        "id":166,
            //                        "library":null,
            //                        "user_id":7,
            //                        "text":"dxxx",
            //                        "message_user":{
            //                            "read_date":null
            //                        }
            //                },
            //                {
            //                    "created_date":"2017-06-15T07:46:15Z",
            //                        "id":165,
            //                        "library":null,
            //                        "user_id":7,
            //                        "text":"465465465465",
            //                        "message_user":{
            //                            "read_date":null
            //                        }
            //                },
            //                ...
            //            ]
            //        }

        // Insert in reverse order

        int newMessagesCount = 0;

        try {
            JSONArray list = new JSONArray(json);
            if(list != null) {
                newMessagesCount = list.length();
                int index = 0;
                for (int i = list.length() - 1; i >= 0; i--) {
                    JSONObject item = list.optJSONObject(i);
                    if(item != null) {
                        Iterator<String> iter = item.keys();
                        ContentValues values = new ContentValues();
                        while (iter.hasNext()) {
                            String key = iter.next();
                            String value = item.optString(key, "");
                            values.put(key, value);
                            if(key.equals("id")) {
                                try {
                                    int id = Integer.parseInt(value);
                                    if(this.oldestId != -1) {
                                        if(id < this.oldestId) {
                                            this.oldestId = id;
                                        }
                                    }
                                    else {
                                        this.oldestId = id;
                                    }
                                }
                                catch (NumberFormatException e) {
                                    Log.d(TAG, e.getLocalizedMessage());
                                }
                            }
                        }
                        if(atTheEnd) {
                            this.messages.add(new GenericModel(values));
                        }
                        else {
                            this.messages.add(index++, new GenericModel(values));
                        }
                    }
                }
            }
        } catch (JSONException error) {
            Log.e(TAG, "insertMessages : JSONException : " + error.getLocalizedMessage());
        }

        return newMessagesCount;
    }

    /**
     * Insert a message at the end of the list
     * @param text
     * @param userId leave it empty if there is no user associated to the message
     * @param notify true to throw the ON_LATEST_MESSAGES_LOADED event, false otherwise
     */
    public void insertAutomaticMessage(String text, String userId, boolean notify) {
        ContentValues values = new ContentValues(3);
        values.put("type", "local");
        values.put("id", this.getLastMessageId());
        values.put("user_id", userId);
        values.put("text", text);
        this.messages.add(new GenericModel(values));

        if(notify) {
            ArrayList<Integer> list = new ArrayList<Integer>(1);
            list.add(1);
            MessageInteraction.getInstance().FireEvent(MessageInteraction.Type.ON_LATEST_MESSAGES_LOADED, list);
        }
    }

    /*
    public void insertAutomaticMessage(String text) {
        ContentValues values = new ContentValues(3);
        values.put("id", this.getLastMessageId());
        values.put("user_id", "-1");
        values.put("text", text);
        this.messages.add(new GenericModel(values));
    }

    public void insertUserMessage(String userId, String text) {
        ContentValues values = new ContentValues(3);
        values.put("id", this.getLastMessageId());
        values.put("user_id", userId);
        values.put("text", text);
        this.messages.add(new GenericModel(values));
    }
    */

    public ArrayList<GenericModel> getMessages() {
        return this.messages;
    }

    public void invertSorting() {
        Collections.reverse(this.messages);
    }

    public String getLastMessageId() {
        if(this.messages.size() > 0) {
            return this.messages.get(this.messages.size() - 1).getContentValue("id");
        }
        else {
            return "0";
        }
    }

    @Subscribe
    public void OnTokBoxInteraction(TokBoxInteraction.OnTokBoxInteractionEvent event) {
        if (event.getType() == TokBoxInteraction.Type.ON_SESSION_CONNECTED) {
            Log.d(TAG, "ON_SESSION_CONNECTED");

            // Load current messages
            APIClient.getInstance().getMessages();
        }
        else if (event.getType() == TokBoxInteraction.Type.ON_SESSION_DISCONNECTED) {
            Log.d(TAG, "ON_SESSION_DISCONNECTED");

            // Create current messages
            this.messages.clear();

            // Clear oldest id
            this.oldestId = -1;
        }
    }

}
