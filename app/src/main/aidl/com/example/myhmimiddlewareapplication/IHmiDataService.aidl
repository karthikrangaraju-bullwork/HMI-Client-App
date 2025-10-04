package com.example.myhmimiddlewareapplication;

import com.example.myhmimiddlewareapplication.CanMessageAidl;
import com.example.myhmimiddlewareapplication.IHmiDataCallback;

interface IHmiDataService {

    // Query stored data
    CanMessageAidl getLatestData();
    CanMessageAidl getLatestMessage(int canId);

    // Insert new data
    void insertData(in CanMessageAidl message);

    // Register/unregister for real-time updates
    void registerCallback(in IHmiDataCallback callback);
    void unregisterCallback(in IHmiDataCallback callback);
}
