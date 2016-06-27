package joe.jobluesuits;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Description
 * Created by chenqiao on 2016/6/27.
 */
public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.MyViewHolder> {

    private ArrayList<BluetoothDevice> devices;

    public DevicesAdapter(ArrayList<BluetoothDevice> devices) {
        this.devices = devices;
    }

    private OnItemClickListener listener;

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView textView = new TextView(parent.getContext());
        textView.setTextColor(Color.BLACK);
        textView.setPadding(20, 20, 20, 20);
        textView.setTextSize(26);
        return new MyViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        if (position >= 0 && position < devices.size()) {
            holder.setData(devices.get(position).getName());
        }
        holder.content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClicked(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView content;

        public MyViewHolder(View itemView) {
            super(itemView);
            content = (TextView) itemView;
        }

        public void setData(String deviceName) {
            content.setText(deviceName);
        }
    }

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    interface OnItemClickListener {
        void onItemClicked(int position);
    }
}
