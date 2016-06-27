package joe.jobluesuits;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.UUID;

import joe.bluelibrary.BluetoothUtils;
import joe.bluelibrary.UUIDs;
import joe.bluelibrary.dao.DeviceFoundListener;

public class MainActivity extends AppCompatActivity implements DeviceFoundListener {

    private RecyclerView recyclerView;
    private DevicesAdapter adapter;
    private ArrayList<BluetoothDevice> devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.listview);
        devices = new ArrayList<>();
        adapter = new DevicesAdapter(devices);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

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
                Log.d("bluetooth", "try to connect:" + devices.get(position).getName());
                BluetoothUtils.getInstance().connectAsClient(devices.get(position), UUID.fromString(UUIDs.SDP_AudioSourceServiceClass_UUID));
            }
        });
    }

    @Override
    public void findADevice(BluetoothDevice device) {
        devices.add(device);
        Log.d("Bluetooth", "scan:" + device.getName());
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BluetoothUtils.getInstance().stopDiscoverDevices();
    }
}
