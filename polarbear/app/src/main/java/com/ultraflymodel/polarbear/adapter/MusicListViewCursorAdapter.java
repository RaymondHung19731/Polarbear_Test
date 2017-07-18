package com.ultraflymodel.polarbear.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.ultraflymodel.polarbear.R;
import com.ultraflymodel.polarbear.common.Constants;
import com.ultraflymodel.polarbear.common.DBA;
import com.ultraflymodel.polarbear.common.HILog;
import com.ultraflymodel.polarbear.eventbus.MusicItemSwiped;
import com.ultraflymodel.polarbear.eventbus.PlayItemSwiped;
import com.ultraflymodel.polarbear.eventbus.WakeupItemSwiped;
import com.ultraflymodel.polarbear.ultraflymodel.UltraflyModelApplication;
import com.ultraflymodel.polarbear.utils.CommonUtils;
import com.ultraflymodel.polarbear.utils.StringUtil;

/**
 * Created by William on 2016/8/8.
 */
public class MusicListViewCursorAdapter extends CursorAdapter {
    final String TAG = MusicListViewCursorAdapter.class.getSimpleName();
    LayoutInflater mInflater;
    private int active_distance;
    private int active_position;
    private boolean mReveal = true;
    private int whichlist=Constants.MUSICLIST;
    private boolean onRevealTimer = true;
    private int m_Height_item=125;


    public Handler m_OneSecondHandler = new Handler();
    private Runnable m_OneSecondTimeoutTask = new Runnable() {
        public void run()
        {
            switch(whichlist){
                case Constants.MUSICLIST:
                    HILog.d(TAG, "MusicListViewCursorAdapter: addRevealListener: MUSICLIST:");
                    MusicItemSwiped musicItemSwiped = new MusicItemSwiped();
                    musicItemSwiped.position = active_position;
                    UltraflyModelApplication.getInstance().bus.post(musicItemSwiped);
                    break;
                case Constants.PLAYLIST:
                    HILog.d(TAG, "MusicListViewCursorAdapter: addRevealListener: PLAYLIST:");
                    PlayItemSwiped playItemSwiped = new PlayItemSwiped();
                    playItemSwiped.position = active_position;
                    UltraflyModelApplication.getInstance().bus.post(playItemSwiped);
                    break;
                case Constants.WAKEUPLIST:
                    HILog.d(TAG, "MusicListViewCursorAdapter: addRevealListener: WAKEUPLIST:");
                    WakeupItemSwiped wakeupItemSwiped = new WakeupItemSwiped();
                    wakeupItemSwiped.position = active_position;
                    UltraflyModelApplication.getInstance().bus.post(wakeupItemSwiped);
                    break;
            }
            onRevealTimer = true;
        }
    };

    public MusicListViewCursorAdapter(Context context, Cursor c, int whichlist) {
        super(context, c);
        HILog.d(TAG, "MusicListViewCursorAdapter:");
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.whichlist = whichlist;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        HILog.d(TAG, "MusicListViewCursorAdapter: newView: cursor position = " + cursor.getPosition() + ", whichlist = " + whichlist);
        ViewHolder mViewHolder = new ViewHolder();
        int layout = R.layout.item_browserlist_swipe;
        View convertView = mInflater.inflate(layout, parent, false);
        mViewHolder.topView = (TextView) convertView.findViewById(R.id.top_view);
        mViewHolder.bottomView = (TextView) convertView.findViewById(R.id.bottom_view);
        mViewHolder.swipecontainer = (SwipeLayout) convertView.findViewById(R.id.swipecontainer);
        mViewHolder.surface_back = (LinearLayout) convertView.findViewById(R.id.surface_back);
        mViewHolder.surface_front = (LinearLayout) convertView.findViewById(R.id.surface_front);
        mViewHolder.swipecontainer.setShowMode(SwipeLayout.ShowMode.PullOut);
        mViewHolder.swipecontainer.addDrag(SwipeLayout.DragEdge.Right, mViewHolder.surface_back);
        mViewHolder.swipecontainer.addDrag(SwipeLayout.DragEdge.Left, mViewHolder.surface_back);
        convertView.setTag(R.id.surface_front, mViewHolder);
        return convertView;
    }

    @Override
    public void bindView(View view, Context context, final Cursor cursor) {
        AbsListView.LayoutParams params = (AbsListView.LayoutParams) view.getLayoutParams();
        params.height = m_Height_item;
        ViewHolder mViewHolder = null;
        String Artist=null, Album=null, Title=null;
        final int position = cursor.getPosition();
        HILog.d(TAG, "MusicListViewCursorAdapter: bindView: whichlist = " + whichlist);
        mViewHolder = (ViewHolder) view.getTag(R.id.surface_front);
//        mViewHolder.swipecontainer.addRevealListener(R.id.tv_swipetoselect, null);
        mViewHolder.swipecontainer.addRevealListener(R.id.tv_swipetoselect, new SwipeLayout.OnRevealListener() {
            @Override
            public void onReveal(View child, SwipeLayout.DragEdge edge, float fraction, int distance) {
                if(onRevealTimer) {
                    onRevealTimer = false;
                    m_OneSecondHandler.postDelayed(m_OneSecondTimeoutTask, Constants.SWIPEWAIT);
                }
                active_distance = distance;
                active_position = position;
            }
        });

        if(whichlist==Constants.WIFILIST){
            Title = cursor.getString(cursor.getColumnIndex(DBA.Field.WIFINAME));
            Album = cursor.getString(cursor.getColumnIndex(DBA.Field.WIFIDBM));
            Artist = cursor.getString(cursor.getColumnIndex(DBA.Field.WIFISECURITY));
        } else {
            Title = cursor.getString(cursor.getColumnIndex(DBA.Field.TITLE));
            Album = cursor.getString(cursor.getColumnIndex(DBA.Field.ALBUM));
            Artist = cursor.getString(cursor.getColumnIndex(DBA.Field.ARTIST));
        }


        HILog.d(TAG, "MusicListViewCursorAdapter: bindView: Title = " + Title);
        HILog.d(TAG, "MusicListViewCursorAdapter: bindView: Album = " + Album);
        if(!StringUtil.isStrNullOrEmpty(Title)) mViewHolder.topView.setText(Title);
        else if(!StringUtil.isStrNullOrEmpty(Album)) mViewHolder.topView.setText(Album);
        mViewHolder.topView.setTypeface(CommonUtils.getGothicFont(mContext), Typeface.NORMAL);


        if(!StringUtil.isStrNullOrEmpty(Artist)) mViewHolder.bottomView.setText(Artist);
        else mViewHolder.bottomView.setText(String.valueOf(cursor.getInt(cursor.getColumnIndex(DBA.Field.MBSIZE))) + " MB");
        mViewHolder.bottomView.setTypeface(CommonUtils.getGothicFont(mContext), Typeface.NORMAL);

        if(whichlist==Constants.PLAYLIST){
            int now_sending = cursor.getInt(cursor.getColumnIndex(DBA.Field.NOWSENDING));
            if(now_sending==Constants.ON) {
                mViewHolder.topView.setTextColor(context.getResources().getColor(R.color.text_style7));
                mViewHolder.bottomView.setTextColor(context.getResources().getColor(R.color.text_style7));
            } else {
                mViewHolder.topView.setTextColor(context.getResources().getColor(R.color.text_style2));
                mViewHolder.bottomView.setTextColor(context.getResources().getColor(R.color.text_style2));
            }
        }

        if(whichlist==Constants.MUSICLIST){

            int now_playing = cursor.getInt(cursor.getColumnIndex(DBA.Field.NOWPLAYING));
            if(now_playing==Constants.ON) {
                HILog.d(TAG, "MusicListViewCursorAdapter: bindView: ON Position = " + cursor.getPosition());
                mViewHolder.topView.setTextColor(context.getResources().getColor(R.color.text_style7));
                mViewHolder.bottomView.setTextColor(context.getResources().getColor(R.color.text_style7));
            } else {
                mViewHolder.topView.setTextColor(context.getResources().getColor(R.color.text_style2));
                mViewHolder.bottomView.setTextColor(context.getResources().getColor(R.color.text_style2));
            }
        }

        view.setLayoutParams(params);
        return;
    }


    private static class ViewHolder {
        TextView topView;
        TextView bottomView;
        SwipeLayout swipecontainer;
        LinearLayout surface_front;
        LinearLayout surface_back;
    }
}
