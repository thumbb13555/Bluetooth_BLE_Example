package com.jetec.bluetooth_bleexample;

import android.bluetooth.BluetoothGattCharacteristic;

public class SendType {

    public static String SendForBLEDataType;//要送出的指令
    public static byte[] SendForBLEbyteType;//要送出的指令(byte)
    public static BluetoothGattCharacteristic Mycharacteristic;//每次傳輸資料用的
    public static BluetoothLeService getSendBluetoothLeService;
}
