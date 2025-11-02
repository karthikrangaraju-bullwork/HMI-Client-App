package com.bullwork.hmi_headless;

import com.bullwork.hmi_headless.CanMessageAidl;

interface IHmiDataCallback {
    void onNewData(in CanMessageAidl message);
}