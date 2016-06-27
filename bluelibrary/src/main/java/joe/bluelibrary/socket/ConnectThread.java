package joe.bluelibrary.socket;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

/**
 * Description
 * Created by chenqiao on 2016/6/27.
 */
public class ConnectThread extends Thread {

    private BluetoothDevice device;
    private BluetoothSocket socket;
    private BluetoothAdapter bluetoothAdapter;

    public ConnectThread(BluetoothDevice device, UUID uuid, BluetoothAdapter bluetoothAdapter) {
        this.device = device;
        this.bluetoothAdapter = bluetoothAdapter;
        try {
            socket = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        //暂停蓝牙的扫描，因为会降低连接速度
        bluetoothAdapter.cancelDiscovery();
        if (socket != null) {
            try {
                socket.connect();
                ConnectedDeviceManager.getInstance().add(socket);
            } catch (IOException e) {
                e.printStackTrace();
                cancel();
            }
        }
    }

    public void cancel() {
        try {
            socket.close();
            ConnectedDeviceManager.getInstance().remove(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
