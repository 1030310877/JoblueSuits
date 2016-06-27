package joe.bluelibrary;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.UUID;

import joe.bluelibrary.activity.ResultActivity;
import joe.bluelibrary.dao.DeviceFoundListener;
import joe.bluelibrary.socket.ConnectThread;
import joe.bluelibrary.socket.ServerAcceptThread;

/**
 * Description
 * <uses-permission android:name="android.permission.BLUETOOTH" />
 * Created by chenqiao on 2016/6/24.
 */
public class BluetoothUtils {
    private static BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private static BluetoothUtils instance;

    private BluetoothUtils() {

    }

    public static BluetoothUtils getInstance() {
        if (instance == null) {
            synchronized (BluetoothUtils.class) {
                if (instance == null) {
                    instance = new BluetoothUtils();
                }
            }
        }
        return instance;
    }

    /**
     * 设备是否支持蓝牙
     */
    public static boolean isSupported() {
        return bluetoothAdapter != null;
    }

    /**
     * 蓝牙是否打开
     */
    public static boolean isEnable() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * 打开蓝牙
     */
    public static void enableBluetooth(Context context) {
        if (!isEnable()) {
            Intent tempIntent = new Intent(context, ResultActivity.class);
            tempIntent.putExtra("type", 1);
            context.startActivity(tempIntent);
        } else {
            if (Thread.currentThread() == context.getMainLooper().getThread()) {
                Toast.makeText(context, "蓝牙已经打开", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 获取已经配对过的设备
     */
    public static Set<BluetoothDevice> getBondedDevices() {
        if (bluetoothAdapter != null) {
            return bluetoothAdapter.getBondedDevices();
        } else {
            return null;
        }
    }

    private WeakReference<Context> disCoverContext;
    private DeviceFoundListener resultListener;

    /**
     * 停止扫描设备{@link #startDiscoverDevices}
     */
    public void stopDiscoverDevices() {
        if (disCoverContext.get() != null) {
            disCoverContext.get().unregisterReceiver(scanReceiver);
        }
        resultListener = null;
    }

    /**
     * 开始扫描蓝牙设备{@link #stopDiscoverDevices},扫描只会持续12秒。
     *
     * @param context  建议传ApplicationContext
     * @param listener 扫描结果回调（没当扫描到一个设备就进行一次回调）
     * @return 是否能开启扫描功能
     */
    public boolean startDiscoverDevices(Context context, DeviceFoundListener listener) {
        boolean result = false;
        if (bluetoothAdapter != null) {
            result = bluetoothAdapter.startDiscovery();
        }
        if (result) {
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            context.registerReceiver(scanReceiver, filter);
            disCoverContext = new WeakReference<>(context);
            resultListener = listener;
        }
        return result;
    }

    private BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (resultListener != null) {
                    resultListener.findADevice(device);
                }
            }
        }
    };

    /**
     * 设置对其他设备可见
     *
     * @param availableTime 可见时间（最大为3600,默认为120）
     */
    public static boolean makeDiscoverable(Context context, int availableTime) {
        boolean result;
        if (result = isEnable()) {
            Intent tempIntent = new Intent(context, ResultActivity.class);
            tempIntent.putExtra("type", 2);
            if (availableTime == 0) {
                availableTime = 120;
            }
            tempIntent.putExtra("availableTime", availableTime);
            context.startActivity(tempIntent);
        }
        return result;
    }

    private ServerAcceptThread serverThread;

    /**
     * 作为一个服务端启动（被动连接），这样可以连接多台设备,不再需要的时候调用{@link #stopAsServer()}
     * 客户端的连接状态通过{@link joe.bluelibrary.socket.ConnectedDeviceManager}设置监听器来监听
     *
     * @param uuid 可以通过{@link UUID#randomUUID()}生成唯一的UUID，保存下来，写成常量传入
     */
    public void connectAsServer(UUID uuid) {
        connectAsServer(uuid, -1);
    }

    /**
     * 作为一个服务端启动（被动连接），这样可以连接多台设备
     * 客户端的连接状态通过{@link joe.bluelibrary.socket.ConnectedDeviceManager}设置监听器来监听
     *
     * @param uuid    可以通过{@link UUID#randomUUID()}生成唯一的UUID，保存下来，写成常量传入
     * @param timeout 时长。固定时间后会停止服务端，拒绝连接{@link #stopAsServer()}
     */
    public void connectAsServer(UUID uuid, long timeout) {
        stopAsServer();
        serverThread = new ServerAcceptThread(uuid, bluetoothAdapter);
        serverThread.start(timeout);
    }

    /**
     * 停止作为服务端的功能（并不会取消已经连接的状态，只是不再接收连接请求）
     */
    public void stopAsServer() {
        if (serverThread != null && !serverThread.isInterrupted()) {
            serverThread.cancel();
            serverThread = null;
        }
    }

    /**
     * 设备配对
     */
    public void bondDevice(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            device.createBond();
        }
    }

    /**
     * 连接蓝牙设备
     */
    public ConnectThread connectAsClient(BluetoothDevice device, UUID uuid) {
        ConnectThread thread = new ConnectThread(device, uuid, bluetoothAdapter);
        thread.start();
        return thread;
    }

//    public void A2DP(Context context, BluetoothProfile.ServiceListener listener) {
//        bluetoothAdapter.getProfileProxy(context, listener, BluetoothProfile.A2DP);
//    }
}