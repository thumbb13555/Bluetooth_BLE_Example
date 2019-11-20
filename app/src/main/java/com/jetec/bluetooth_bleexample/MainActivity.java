package com.jetec.bluetooth_bleexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    String TAG = MainActivity.class.getSimpleName() + "My";
    private static final int REQUEST_FINE_LOCATION_PERMISSION = 102;
    private static final int REQUEST_ENABLE_BT = 2;
    boolean mScanning;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private Button btScan;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private Handler mHandler;
    private ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
    private RecyclerView mRecyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CheckPermission();//確認開啟藍芽與取得位置權限
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mHandler = new Handler();
        mBluetoothAdapter = bluetoothManager.getAdapter();
        btScan = findViewById(R.id.button_Rescan);
        mRecyclerView = findViewById(R.id.recyclerview_ScanDevice);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        btScan.setOnClickListener((v -> {
            if (btScan.getText().toString().matches("ReScan")) {
                scanLeDevice(true);
                btScan.setText("Stop");
                mLeDeviceListAdapter.clear();
            } else {
                scanLeDevice(false);
                btScan.setText("ReScan");
            }
        }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mRecyclerView.setAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);

    }

    private void scanLeDevice(final boolean enable) {//開啟或關閉掃描
        if (enable) {
            mHandler.postDelayed(() -> {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                btScan.setText("ReScan");
            }, 5000);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            btScan.setText("Stop");
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private void CheckPermission() {//確認開啟藍芽與取得位置權限
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int hasGone = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (hasGone != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_FINE_LOCATION_PERMISSION);
            }
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, "裝置無支援藍牙", Toast.LENGTH_SHORT).show();
                finish();
            }
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }


    private class LeDeviceListAdapter extends RecyclerView.Adapter<LeDeviceListAdapter.ViewHolder> {
        private ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
        private ArrayList<BluetoothDevice> Devices = new ArrayList<>();

        public void clear() {
            arrayList.clear();
            Devices.clear();

        }

        public void addDevice(BluetoothDevice device, String rssi,String getByteArray) {

            if (!Devices.contains(device)){
                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("DeviceName", device.getName());
                hashMap.put("Address", device.getAddress());
                hashMap.put("Rssi", rssi);
                hashMap.put("Byte",getByteArray);
                arrayList.add(hashMap);
                Devices.add(device);
            }
        }


        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName,tvAddress,tvRssi,tvByte;
            View mView;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.textView_DeviceName);
                tvAddress = itemView.findViewById(R.id.textView_Address);
                tvRssi = itemView.findViewById(R.id.textView_Rssi);
                tvByte = itemView.findViewById(R.id.textView_byte);
                mView = itemView;
            }
            public void setOnItemClick(View.OnClickListener l) {
                this.mView.setOnClickListener(l);
            }
        }


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_scanitem, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String deviceName = arrayList.get(position).get("DeviceName");
            String address    = arrayList.get(position).get("Address");
            String rssi       = arrayList.get(position).get("Rssi");
            String deviceInfo = arrayList.get(position).get("Byte");

            holder.tvName.setText(deviceName);
            holder.tvAddress.setText("裝置位址: "+address);
            holder.tvRssi.setText("訊號強度: "+rssi);
            holder.tvByte.setText("裝置資訊: "+deviceInfo);
            holder.setOnItemClick((v -> {
                Intent intent = new Intent(MainActivity.this,DeviceControl.class);
                intent.putExtra("DeviceName",deviceName);
                intent.putExtra("Address",address);
                intent.putExtra("Rssi",rssi);
                intent.putExtra("Info",deviceInfo);
                startActivity(intent);
            }));

        }

        @Override
        public int getItemCount() {
            return arrayList.size();
        }


    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback
            = new BluetoothAdapter.LeScanCallback() {//掃描並製成列表
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            runOnUiThread(() -> {
                if (device.getName() != null) {
                    mLeDeviceListAdapter.addDevice(device, String.valueOf(rssi),byteArrayToHexStr(scanRecord));
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });

        }
    };

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
