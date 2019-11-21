package com.jetec.bluetooth_bleexample;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DeviceControl extends AppCompatActivity {
    String TAG = DeviceControl.class.getSimpleName();
    TextView tvTitle,tvAddress,tvStatus,tvResult;
    Button btBack,btConnect,btSend,btClear;
    EditText edInput;
    String deviceName,deviceAddress,deviceRssi,deviceInfo;
    Boolean connectStatus = false;
    SimpleAdapter simpleAdapter;
    LinearLayout liResult;
    ArrayList<String> getResult = new ArrayList<>();
    ArrayAdapter adapter;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    ListView listView,liDataLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        baseSetting();//基礎設定
        setBLEFunction();//設置藍芽功能
        setLogView();//設置紀錄介面
        buttonsClickEvent();//設置按鈕們的觸發事件
    }//onCreate

    private void baseSetting() {//基礎設定
        Intent intent = getIntent();
        deviceName = intent.getStringExtra("DeviceName");
        deviceAddress = intent.getStringExtra("Address");
        deviceRssi = intent.getStringExtra("Rssi");
        deviceInfo = intent.getStringExtra("Info");
        tvTitle = findViewById(R.id.textView_DC_Title);
        tvAddress = findViewById(R.id.textView_DC_Address);
        tvStatus = findViewById(R.id.textView_DC_Status);
        liDataLog = findViewById(R.id.listView_DC_Log);
        btSend = findViewById(R.id.button_DC_SendCommend);
        btClear = findViewById(R.id.button_DC_Clear);
        edInput = findViewById(R.id.editText_InputCommend);
        btBack = findViewById(R.id.button_DC_turnBack);
        btConnect = findViewById(R.id.button_DC_connect);
        listView = findViewById(R.id.listview_DC_UUIDs);
        liResult = findViewById(R.id.layout_DC_layoutResult);
        tvTitle.setText(deviceName);
        tvAddress.setText("裝置位址: "+deviceAddress);

    }
    private void setBLEFunction() {//設置藍芽功能
        Intent gattServiceIntent = new Intent(DeviceControl.this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    private void setLogView() {//設置紀錄介面
        adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,getResult);
        liDataLog.setAdapter(adapter);
    }

    private void buttonsClickEvent() {//設置按鈕們的觸發事件
        btBack.setOnClickListener((v -> {
            mBluetoothLeService.disconnect();
            finish();
        }));
        btConnect.setOnClickListener(v -> {
            if (connectStatus == true){
                mBluetoothLeService.disconnect();
            }else{
                mBluetoothLeService.connect(deviceAddress);
            }
        });
        btSend.setOnClickListener(v -> {
            SendType.SendForBLEDataType = edInput.getText().toString();
            SendType.getSendBluetoothLeService.
                    setCharacteristicNotification(SendType.Mycharacteristic, true);
            getResult.add("送出:"+edInput.getText().toString());
            adapter.notifyDataSetChanged();
            edInput.setText("");
        });
        btClear.setOnClickListener(v -> {
            getResult.clear();
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");

    }

    /*@Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }
*/
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLeService.disconnect();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {//連線事件

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {


            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();//在此獲取
            Log.d(TAG, "onResume: " + mBluetoothLeService);
            if (!mBluetoothLeService.initialize()) {
                finish();
            }

            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(deviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };//serviceConnection

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {//廣播器事件
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: "+action);

            if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {//連線尚未成功，設備仍須努力
                tvStatus.setText("裝置狀態: 未連線");
                btConnect.setText("Connect");
                listView.setVisibility(View.GONE);
                connectStatus = false;
                liResult.setVisibility(View.GONE);


            } else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {//連現成功
                connectStatus = true;
                listView.setVisibility(View.VISIBLE);
                tvStatus.setText("裝置狀態: 已連線");
                btConnect.setText("Disconnect");
                liResult.setVisibility(View.VISIBLE);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {//搜尋可用的服務(UUID)
                if (SampleGattAttributes.myGatt.size()>0){
                    SampleGattAttributes.myGatt.clear();
                }
                displayGattServices(mBluetoothLeService.getSupportedGattServices());//取得UUID們
                getUUIDList();//將可用的UUID製成列表
                /**以下為設置發送程序*/
                final BluetoothGattCharacteristic characteristic =
                        mGattCharacteristics.get(2).get(0);
                mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                SendType.getSendBluetoothLeService = mBluetoothLeService;
                SendType.Mycharacteristic = characteristic;
                SendType.SendForBLEDataType = "";

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {//取得從設備傳回來的資訊
                resultFromDeviceData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }

        }
    };//onReceive

    private void resultFromDeviceData(byte[] data){//取得回傳值
        new Thread(()->{
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
            runOnUiThread(()->{
                getResult.add("回傳(String): "+new String(data) + "\n" + stringBuilder.toString());
                getResult.add("回傳(byte): "+byteArrayToHexStr(data));
                getResult.add("----------------------");
                adapter.notifyDataSetChanged();

            });

        }).start();


    }

    private void getUUIDList() {//將可用的UUID製成列表
        ArrayList<HashMap<String,String>> arrayList = new ArrayList<>();
        HashMap <String,String> hashMap = new HashMap<>();
        hashMap.put("Name","可用的UUID");
        hashMap.put("Service","服務: "+ SampleGattAttributes.myGatt.get(5));
        hashMap.put("Write","發送資訊: "+SampleGattAttributes.myGatt.get(6));
        hashMap.put("Read","接收資訊: "+SampleGattAttributes.myGatt.get(7));
        arrayList.add(hashMap);
        String [] from = {"Name","Service","Write","Read"};
        int[] value = {R.id.textView_DeviceName,R.id.textView_Address,R.id.textView_Rssi,R.id.textView_byte};
        simpleAdapter =
                new SimpleAdapter(getBaseContext(),arrayList,R.layout.activity_scanitem,from,value);
        listView.setAdapter(simpleAdapter);
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {//取得UUID們
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = "未知";
        String unknownCharaString = "未知";
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();

        //將可用的GATT Service迴圈顯示
        /**這邊顯示的是關於裝置的基本性質*/
        for (BluetoothGattService gattService : gattServices) {
//        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
//            Log.d(TAG, "displayGattServices: " + uuid);

            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            //將可用的GATT Characteristics迴圈顯示
            /**這邊顯示的是關於裝置可寫入與輸出等等*/
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);

        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() {//設置動作種類
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);//連接一個GATT服務
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);//從GATT服務中斷開連接
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);//查找GATT服務
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);//從服務中接受(收?)數據
        return intentFilter;
    }

    /**
     * Byte轉16進字串工具
     */
    public static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }

        StringBuilder hex = new StringBuilder(byteArray.length * 2);
        for (byte aData : byteArray) {
            hex.append(String.format("%02X", aData));
        }
        String gethex = hex.toString();
        return gethex;

    }
}
