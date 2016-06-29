package joe.bluelibrary;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import joe.bluelibrary.activity.ResultActivity;
import joe.bluelibrary.dao.ClientAction;
import joe.bluelibrary.dao.ConnectImpl;
import joe.bluelibrary.dao.DeviceFoundListener;
import joe.bluelibrary.socket.ConnectThread;
import joe.bluelibrary.socket.ServerAcceptThread;

/**
 * Description
 * <uses-permission android:name="android.permission.BLUETOOTH" />
 * <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
 * Created by chenqiao on 2016/6/24.
 */
public class BluetoothUtils implements ConnectImpl {
    private static BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private static BluetoothUtils instance;

    public static Context mContext;

    private BluetoothUtils() {

    }

    public static void init(Context context) {
        mContext = context;
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
    @Override
    public void connectAsServer(UUID uuid, long timeout) {
        stopAsServer();
        serverThread = new ServerAcceptThread(uuid, bluetoothAdapter);
        serverThread.start(timeout);
    }

    /**
     * 停止作为服务端的功能（并不会取消已经连接的状态，只是不再接收连接请求）
     */
    @Override
    public void stopAsServer() {
        if (serverThread != null && !serverThread.isInterrupted()) {
            serverThread.cancel();
            serverThread = null;
        }
    }

    /**
     * 设备配对
     */
    @Override
    public boolean bondDevice(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return device.createBond();
        } else {
            Method m;
            try {
                m = device.getClass().getDeclaredMethod("createBond");
                m.setAccessible(true);
                Boolean result = (Boolean) m.invoke(device);
                return result;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return false;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return false;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * 连接蓝牙设备
     */
    @Override
    public ClientAction connectAsClient(BluetoothDevice device, UUID uuid) {
        ClientAction action = null;
        if (uuid.compareTo(UUID.fromString(UUIDs.PBAP_UUID_STR)) == 0) {
            action = new ConnectDelegate().connect(ConnectDelegate.TYPE_PBAP, device);
        } else {
            ConnectThread thread = new ConnectThread(device, uuid, bluetoothAdapter);
            thread.start();
        }
        return action;
    }

    private BluetoothA2dp a2dp;

    /**
     * 连接A2dp设备
     */
    @Override
    public void connectAsA2dp(Context context, final BluetoothDevice device) {
        bluetoothAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.A2DP) {
                    a2dp = (BluetoothA2dp) proxy;
                    try {
                        a2dp.getClass().getMethod("connect", BluetoothDevice.class).invoke(a2dp, device);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.A2DP) {
                    a2dp = null;
                }
            }
        }, BluetoothProfile.A2DP);
    }

    private BluetoothHeadset headset;

    /**
     * 连接Headset设备
     */
    @Override
    public void connectAsHeadset(Context context, final BluetoothDevice device) {
        bluetoothAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HEADSET) {
                    headset = (BluetoothHeadset) proxy;
                    try {
                        headset.getClass().getMethod("connect", BluetoothDevice.class).invoke(headset, device);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.HEADSET) {
                    headset = null;
                }
            }
        }, BluetoothProfile.HEADSET);
    }

    @Override
    public void connectAsPan(Context context, final BluetoothDevice device) {
        bluetoothAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == 5) {
                    try {
                        Class clzz = Class.forName("android.bluetooth.BluetoothPan");
                        Method method = clzz.getDeclaredMethod("isValidDevice", BluetoothDevice.class);
                        method.setAccessible(true);
                        Boolean result = (Boolean) method.invoke(proxy, device);
                        if (result) {
                            clzz.getMethod("connect", BluetoothDevice.class).invoke(proxy, device);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == 5) {
                }
            }
        }, 5);
    }
}