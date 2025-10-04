package com.example.myhmimiddlewareapplication;

import com.example.myhmimiddlewareapplication.CanMessageAidl;

interface IHmiDataCallback {
    void onNewData(in CanMessageAidl message);
}
