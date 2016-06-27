package joe.bluelibrary.socket;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Description
 * Created by chenqiao on 2016/6/27.
 */
public class ConnectedDeviceManager {

    private ConnectedDeviceManager() {
        connectedSockets = new ArrayList<>();
    }

    private static ConnectedDeviceManager instance;

    private ArrayList<BluetoothSocket> connectedSockets;

    private SocketConnectListener listener;

    public static ConnectedDeviceManager getInstance() {
        if (instance == null) {
            synchronized (ConnectedDeviceManager.class) {
                if (instance == null) {
                    instance = new ConnectedDeviceManager();
                }
            }
        }
        return instance;
    }

    public int size() {
        return connectedSockets.size();
    }

    public void add(BluetoothSocket socket) {
        connectedSockets.add(socket);
        if (listener != null) {
            listener.aClientConnected(socket);
        }
    }

    public void remove(BluetoothSocket socket) {
        connectedSockets.remove(socket);
    }

    public void clearAll() {
        for (BluetoothSocket socket : connectedSockets) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        connectedSockets.clear();
    }

    public void setClientConnectListener(SocketConnectListener listener) {
        this.listener = listener;
    }

    public void removeListener() {
        listener = null;
    }

    public interface SocketConnectListener {
        void aClientConnected(BluetoothSocket socket);
    }
}
