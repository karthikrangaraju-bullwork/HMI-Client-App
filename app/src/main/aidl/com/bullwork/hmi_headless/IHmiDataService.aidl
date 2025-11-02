package com.bullwork.hmi_headless;

import com.bullwork.hmi_headless.CanMessageAidl;
import com.bullwork.hmi_headless.IHmiDataCallback;

interface IHmiDataService {

    // Query stored data
    CanMessageAidl getLatestData();
    CanMessageAidl getLatestMessage(int canId);

    // Insert new data
    void insertData(in CanMessageAidl message);
    // Register/unregister for real-time updates
    void registerCallback(in IHmiDataCallback callback);
    void unregisterCallback(in IHmiDataCallback callback);
    // NEW: Function to reload the DBC content
    boolean reloadDbc(in String dbcContent);

}