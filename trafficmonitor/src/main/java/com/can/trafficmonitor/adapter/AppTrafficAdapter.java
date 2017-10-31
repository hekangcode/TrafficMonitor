package com.can.trafficmonitor.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.can.trafficmonitor.R;
import com.can.trafficmonitor.bean.AppInfo;
import com.can.trafficmonitor.utils.TextFormat;

import java.util.List;


/**
 * Created by HEKANG on 2016/11/14.
 */

public class AppTrafficAdapter extends RecyclerView.Adapter<AppTrafficAdapter.MyViewHolder> {

    private Context context;
    private List<AppInfo> mList;
    private LayoutInflater mLayoutInflater;

    private View.OnFocusChangeListener mFocusListener;
    private OnItemFocusChangeListener mOnItemFocusChangeListener;
    private OnItemStopButClickListener mOnItemStopButClickListener;

    public interface OnItemStopButClickListener {
        void onStopBtnClick(int position);
    }

    public void setOnItemStopButClickListener(OnItemStopButClickListener onStopBtnClickListener) {
        this.mOnItemStopButClickListener = onStopBtnClickListener;
    }

    public void setFocusListener(View.OnFocusChangeListener focusListener) {
        this.mFocusListener = focusListener;
    }

    public interface OnItemFocusChangeListener {
        void onItemFocusChange(View view, Button stopBtn, TextView title, int position);

        void onStopBtnFocusChange(View view, Button stopBtn, TextView title, int position);
    }

    public void setOnItemFocusChangeListener(OnItemFocusChangeListener itemFocusChangeListener) {
        this.mOnItemFocusChangeListener = itemFocusChangeListener;
    }

    public AppTrafficAdapter(List<AppInfo> list, Context context) {
        this.context = context;
        this.mList = list;
    }

    public void setListData(List<AppInfo> list) {
        mList = list;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public AppTrafficAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(parent.getContext());
        }
        View view = mLayoutInflater.inflate(R.layout.com_can_trafficmonitor_item_app_traffic, parent, false);
        view.setOnFocusChangeListener(mFocusListener);
        return new AppTrafficAdapter.MyViewHolder(view, mOnItemFocusChangeListener, mOnItemStopButClickListener);
    }

    @Override
    public void onBindViewHolder(final AppTrafficAdapter.MyViewHolder holder, final int position) {
        AppInfo app = mList.get(position);
        if (mList.size() == 1 && position == 0) {
            holder.rlItemApp.setBackground(context.getResources().getDrawable(R.drawable
                    .com_can_trafficmonitor_rect_gray));
            holder.line.setVisibility(View.INVISIBLE);
        } else if (position == 0) {
            holder.rlItemApp.setBackground(context.getResources().getDrawable(R.drawable
                    .com_can_trafficmonitor_rect_gray_top));
        } else if (position == (mList.size() - 1)) {
            holder.rlItemApp.setBackground(context.getResources().getDrawable(R.drawable
                    .com_can_trafficmonitor_rect_gray_bottom));
            holder.line.setVisibility(View.INVISIBLE);
        } else {
            holder.rlItemApp.setBackgroundColor(context.getResources().getColor(R.color.color_1AFFFFFF));
        }
        if (app.getDrawable() != null) {
            holder.ivIcon.setImageDrawable(app.getDrawable());
        } else {
            holder.ivIcon.setImageResource(R.mipmap.ic_launcher);
        }
        holder.tvName.setText(TextUtils.isEmpty(app.getName()) ? app.getPackageName() : app.getName());
        holder.tvReceived.setText(context.getResources().getString(R.string.received) + TextFormat.formatByte(app
                .getReceived(), context));
        holder.tvTransmitted.setText(context.getResources().getString(R.string.transmitted) + TextFormat.formatByte
                (app.getTransmitted(), context));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnFocusChangeListener {

        private final OnItemFocusChangeListener mItemFocusChangeListener;
        private final OnItemStopButClickListener mOnItemStopButClickListener;
        private ImageView ivIcon;
        private TextView tvName, tvReceived, tvTransmitted;
        private RelativeLayout rlItemApp;
        private View line;
        private Button btnStopApp;

        private MyViewHolder(View view, OnItemFocusChangeListener mOnItemFocusChangeListener,
                             OnItemStopButClickListener mOnItemStopButClickListener) {
            super(view);
            this.mItemFocusChangeListener = mOnItemFocusChangeListener;
            this.mOnItemStopButClickListener = mOnItemStopButClickListener;
            ivIcon = (ImageView) view.findViewById(R.id.iv_app_icon);
            tvName = (TextView) view.findViewById(R.id.tv_app_name);
            tvReceived = (TextView) view.findViewById(R.id.tv_app_received);
            tvTransmitted = (TextView) view.findViewById(R.id.tv_app_transmitted);
            line = view.findViewById(R.id.line);
            btnStopApp = (Button) view.findViewById(R.id.btn_stop);
            btnStopApp.setOnClickListener(this);
            btnStopApp.setOnFocusChangeListener(this);
            rlItemApp = (RelativeLayout) view.findViewById(R.id.rl_item_app);
            rlItemApp.setOnFocusChangeListener(this);
        }

        @Override
        public void onClick(View v) {
            int idView = v.getId();
            if (idView == R.id.btn_stop) {
                if (mOnItemStopButClickListener != null) {
                    mOnItemStopButClickListener.onStopBtnClick(getLayoutPosition());
                }
            }
        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (mFocusListener != null) {
                mFocusListener.onFocusChange(v, hasFocus);
            }
            int idView = v.getId();
            if (idView == R.id.rl_item_app) {
                if (mItemFocusChangeListener != null) {
                    mItemFocusChangeListener.onItemFocusChange(v, btnStopApp, tvName, getLayoutPosition());
                }
            } else if (idView == R.id.btn_stop) {
                if (mItemFocusChangeListener != null) {
                    mItemFocusChangeListener.onStopBtnFocusChange(v, btnStopApp, tvName, getLayoutPosition());
                }
            }
        }
    }
}
