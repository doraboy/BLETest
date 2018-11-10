package tw.dora.bletest;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.Constants;
import com.inuker.bluetooth.library.beacon.Beacon;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.model.BleGattCharacter;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.model.BleGattService;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothLog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothClient mClient;

    private ListView list;
    private SimpleAdapter adapter;
    private LinkedList<HashMap<String,String>> data;
    private String[] from = {"name", "mac"};
    private int[] to = {R.id.bleitem_name, R.id.bleitem_mac};

    private HashSet<BluetoothDevice> deviceSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,},
                    31);
        }else {
            init();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        init();
    }

    private void init(){
//        final BluetoothManager bluetoothManager =
//                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = bluetoothManager.getAdapter();
//        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, 123);
//        }

        mClient = new BluetoothClient(this);
        if (!mClient.isBluetoothOpened()){
            mClient.openBluetooth();
        }

        list = findViewById(R.id.list);
        initListView();
    }

    private void initListView(){
        data = new LinkedList<>();
        adapter = new SimpleAdapter(this,data,R.layout.bleitem,from,to);
        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String mac = data.get(position).get("mac");
                connectDevice(mac);
            }
        });

    }

    private void connectDevice(String mac){
        final String deviceMac = mac;
        mClient.connect(mac, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile profile) {
                if (code == Constants.REQUEST_SUCCESS) {
                    // Service: UUID: 0000180f-0000-1000-8000-00805f9b34fb
                    // Char: UUID = 00002a19-0000-1000-8000-00805f9b34fb

                    List<BleGattService> listService = profile.getServices();
                    for (BleGattService service : listService){
                        UUID uuidService = service.getUUID();
                        Log.v("brad", uuidService.toString());
                        List<BleGattCharacter> bleChar = service.getCharacters();
                        for (BleGattCharacter character: bleChar){
                            UUID uuidChar = character.getUuid();
                            Log.v("brad", uuidChar.toString());
                        }
                    }

                    UUID uuidService = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
                    UUID uuidChar = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

                    mClient.notify(deviceMac, uuidService, uuidChar, new BleNotifyResponse() {
                        @Override
                        public void onNotify(UUID service, UUID character, byte[] value) {
                            for (byte v : value){
                                Log.v("brad", "=> "  + v);
                            }
                        }

                        @Override
                        public void onResponse(int code) {
                            if (code == Constants.REQUEST_SUCCESS) {
                                Log.v("brad", "callback");
                            }
                        }
                    });


                }
            }
        });
    }



    @Override
    public void finish() {
        if (mClient.isBluetoothOpened()){
            mClient.closeBluetooth();
        }
        super.finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    // SCAN BLE
    public void test1(View view) {
        deviceSet = new HashSet<>();
        final SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(3000, 1)   // 先扫BLE设备3次，每次3s
                .searchBluetoothClassicDevice(5000) // 再扫经典蓝牙5s
                .searchBluetoothLeDevice(2000)      // 再扫BLE设备2s
                .build();

        mClient.search(request, new SearchResponse() {
            @Override
            public void onSearchStarted() {
                Log.v("brad", "start search...");
            }

            @Override
            public void onDeviceFounded(SearchResult result) {
                BluetoothDevice bleDevice = result.device;
                String bleName = bleDevice.getName();
                String bleMAC = bleDevice.getAddress();
                //Log.v("brad", bleName + ":" + bleMAC);

                deviceSet.add(bleDevice);

            }

            @Override
            public void onSearchStopped() {
                Log.v("brad", "stop search.");
                showDevices();
            }

            @Override
            public void onSearchCanceled() {
                Log.v("brad", "cancel search.");
                showDevices();
            }
        });
    }

    private void showDevices(){
        data.clear();
        for (BluetoothDevice bluetoothDevice : deviceSet){
            String bleName = bluetoothDevice.getName();
            String bleMAC = bluetoothDevice.getAddress();
            HashMap<String,String> device = new HashMap<>();
            device.put(from[0], bleName);
            device.put(from[1], bleMAC);
            data.add(device);
        }
        adapter.notifyDataSetChanged();
    }

}
