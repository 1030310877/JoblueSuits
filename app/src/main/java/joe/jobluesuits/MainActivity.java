package joe.jobluesuits;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;

import com.android.vcard.VCardEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import joe.bluelibrary.BluetoothUtils;
import joe.bluelibrary.UUIDs;
import joe.bluelibrary.dao.ClientAction;
import joe.bluelibrary.dao.DeviceFoundListener;
import joe.bluelibrary.socket.ConnectedDeviceManager;

public class MainActivity extends AppCompatActivity implements DeviceFoundListener {

    private RecyclerView recyclerView;
    private DevicesAdapter adapter;
    private ArrayList<BluetoothDevice> devices;
    private Set<BluetoothDevice> tempDevices;
    private int type = 1;
    private ClientAction client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.listview);
        devices = new ArrayList<>();
        tempDevices = new HashSet<>();

        adapter = new DevicesAdapter(devices);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

        devices.addAll(BluetoothUtils.getBondedDevices());

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.rg_main);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rd_1:
                        type = 1;
                        break;
                    case R.id.rd_2:
                        type = 2;
                        break;
                    case R.id.rd_3:
                        type = 3;
                        break;
                    case R.id.rd_4:
                        type = 4;
                        break;
                    case R.id.rd_5:
                        type = 5;
                        break;
                }
            }
        });
        findViewById(R.id.open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothUtils.enableBluetooth(MainActivity.this);
            }
        });
        findViewById(R.id.enable_available).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothUtils.makeDiscoverable(MainActivity.this, 120);
            }
        });
        findViewById(R.id.btn_start_discover).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                devices.clear();
                tempDevices.clear();
                adapter.notifyDataSetChanged();
                BluetoothUtils.getInstance().startDiscoverDevices(MainActivity.this, MainActivity.this);
            }
        });
        findViewById(R.id.btn_stop_discover).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothUtils.getInstance().stopDiscoverDevices();
            }
        });

        adapter.setListener(new DevicesAdapter.OnItemClickListener() {
            @Override
            public void onItemClicked(int position) {
                switch (type) {
                    case 1:
                        BluetoothUtils.getInstance().bondDevice(devices.get(position));
                        devices.get(position).fetchUuidsWithSdp();
                        break;
                    case 2:
                        BluetoothDevice device = devices.get(position);
                        client = BluetoothUtils.getInstance().connectAsClient(device, UUID.fromString(UUIDs.PBAP_UUID_STR));
                        client.setResultListener(new ClientAction.ResultListener() {
                            @Override
                            public void onResultReceived(Object result) {
                                ArrayList<VCardEntry> entries = (ArrayList<VCardEntry>) result;
                                Log.d("chenqiao", entries.toString());
                            }
                        });
//                        client.doAction(BluetoothPbapClient.PB_PATH, null, null);
                        break;
                    case 3:
                        BluetoothUtils.getInstance().connectAsA2dp(MainActivity.this, devices.get(position));
                        break;
                    case 4:
                        BluetoothUtils.getInstance().connectAsHeadset(MainActivity.this, devices.get(position));
                        break;
                    case 5:
                        BluetoothUtils.getInstance().connectAsPan(MainActivity.this, devices.get(position));
                        break;
                }
            }
        });

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_UUID);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED");
        registerReceiver(mReceiver, intentFilter);

        ConnectedDeviceManager.getInstance().setClientConnectListener(new ConnectedDeviceManager.SocketConnectListener() {
            @Override
            public void aClientConnected(BluetoothSocket socket) {
            }
        });
    }

    @Override
    public void findADevice(BluetoothDevice device) {
        tempDevices.add(device);
        devices.clear();
        devices.addAll(tempDevices);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BluetoothUtils.getInstance().stopDiscoverDevices();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals("android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED")) {
                int role = intent.getIntExtra("android.bluetooth.pan.extra.LOCAL_ROLE", -1);
                Log.d("chenqiao", "role:" + role);
            }
            //配对成功
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice btd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                btd.fetchUuidsWithSdp();
            }
            //fetchuuid返回
            if (action.equals(BluetoothDevice.ACTION_UUID)) {
                BluetoothDevice btd = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i("chenqiao", "Received uuids for " + btd.getName());
                Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                StringBuilder sb = new StringBuilder();
                if (uuidExtra == null) {
                    return;
                }
                List<String> uuids = new ArrayList<>(uuidExtra.length);
                for (int i = 0; i < uuidExtra.length; i++) {
                    sb.append(uuidExtra[i].toString()).append(',');
                    uuids.add(uuidExtra[i].toString());
                }
                Log.i("chenqiao", "ACTION_UUID received for " + btd.getName() + " uuids: " + sb.toString());
            }
        }
    };
}