package com.ultraflymodel.polarbear.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Cache;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.otto.Subscribe;
import com.ultraflymodel.polarbear.R;
import com.ultraflymodel.polarbear.adapter.MusicListViewCursorAdapter;
import com.ultraflymodel.polarbear.common.Constants;
import com.ultraflymodel.polarbear.common.DBA;
import com.ultraflymodel.polarbear.common.HILog;
import com.ultraflymodel.polarbear.eventbus.MusicItemSwiped;
import com.ultraflymodel.polarbear.eventbus.MusicListClicked;
import com.ultraflymodel.polarbear.eventbus.MusicListUpdate;
import com.ultraflymodel.polarbear.eventbus.PlayListUpdate;
import com.ultraflymodel.polarbear.eventbus.PlayMp3Event;
import com.ultraflymodel.polarbear.eventbus.StopMp3Event;
import com.ultraflymodel.polarbear.eventbus.WakeupListUpdate;
import com.ultraflymodel.polarbear.model.MusicList;
import com.ultraflymodel.polarbear.ultraflymodel.UltraflyModelApplication;
import com.ultraflymodel.polarbear.utils.CommonUtils;
import com.ultraflymodel.polarbear.utils.SimpleFileUtils;
import com.ultraflymodel.polarbear.utils.SongMetadataReader;
import com.ultraflymodel.polarbear.utils.StringUtil;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


public class SearchMusicListFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = SearchMusicListFragment.class.getSimpleName();
    private AbsListView mMusicListView;
    private Cursor mMusicListCursor=null;
    private Cursor mPlayListCursor=null;
    private Cursor mWakeupListCursor=null;
    private MusicListViewCursorAdapter musicAdapter;
    private int mNowPosition;
    private static int mSelected = Constants.NONE;
    private static Activity m_ctx;
    private boolean mIsPlaying = false;
    private int mPlayPosition = -1;

    public Handler m_OneSecondHandler = new Handler();
    private Runnable m_OneSecondTimeoutTask = new Runnable() {
        public void run()
        {

            showMusicListView();
            scroll(mNowPosition);
            if(mSelected==Constants.PLAYLIST){
                UltraflyModelApplication.getInstance().bus.post(new PlayListUpdate());
            } else {
                UltraflyModelApplication.getInstance().bus.post(new WakeupListUpdate());
            }
        }
    };

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mainview = inflater.inflate(R.layout.layout_searchmusiclist, container, false);
        HILog.d(TAG, "onCreateView:");
        m_ctx = getActivity();

        Bundle bundle = getArguments();
        mSelected = bundle.getInt(Constants.BC_BUNDLE_SELECTED);
        HILog.d(TAG, "onCreateView: mSelected: " + mSelected);

        mMusicListView = (ListView) mainview.findViewById(R.id.lv_music_inphone);
        mMusicListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mMusicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HILog.d(TAG, "mMusicListView: position = " + position);
                //                String testmp3file = "android.resource://" + getActivity().getPackageName() + "/raw/test_128kbps";
//                String testmp3file = "android.resource://" + getActivity().getPackageName() + "/"+ R.raw.test_128kbps;
//                Uri mp3uri = Uri.parse(testmp3file);
//                HILog.d(TAG, "mMusicListView: mp3uri: " + mp3uri);
//                HILog.d(TAG, "GetFileSection : mp3uri.getPath = " + mp3uri.getPath());
                mMusicListCursor.moveToPosition(position);
                String mp3filepath = mMusicListCursor.getString(mMusicListCursor.getColumnIndex(DBA.Field.PATH));
                ClearMusicListField(DBA.Field.NOWPLAYING);
                if(!mIsPlaying){
                    PlayMp3Event playMp3Event = new PlayMp3Event();
                    playMp3Event.mp3path = mp3filepath;
                    playMp3Event.remote = false;
                    UltraflyModelApplication.getInstance().bus.post(playMp3Event);
                    mIsPlaying = true;
                    mPlayPosition = position;
                    SetMusicListField(DBA.Field.NOWPLAYING, position, Constants.ON);
                } else {
                    SetMusicListField(DBA.Field.NOWPLAYING, mPlayPosition, Constants.OFF);
                    StopMp3Event stopMp3Event = new StopMp3Event();
                    stopMp3Event.mp3path = null;
                    stopMp3Event.remote = false;
                    UltraflyModelApplication.getInstance().bus.post(stopMp3Event);

                    if(mPlayPosition==position){
                        mIsPlaying = false;
                    } else {
                        mPlayPosition = position;
                        SetMusicListField(DBA.Field.NOWPLAYING, mPlayPosition, Constants.ON);
                        HILog.d(TAG, "mMusicListView: mp3filepath = " + mp3filepath);
                        PlayMp3Event playMp3Event = new PlayMp3Event();
                        playMp3Event.mp3path = mp3filepath;
                        playMp3Event.remote = false;
                        UltraflyModelApplication.getInstance().bus.post(playMp3Event);
                        mIsPlaying = true;
                    }
                }
                showMusicListView();
                scroll(mPlayPosition);
            }
        });
        showMusicListView();
        UltraflyModelApplication.getInstance().bus.register(this);
        return mainview;
	}

    @SuppressLint("NewApi")
    private void showMusicListView() {
        HILog.d(TAG, "showMusicListView");
        int size;
        int lastViewedPosition = mMusicListView.getFirstVisiblePosition();
        View v = mMusicListView.getChildAt(0);
        int topOffset = (v == null) ? 0 : v.getTop();

        mMusicListCursor = getMusicListCursor();
        if(mMusicListCursor==null)return;
        size = mMusicListCursor.getCount();
        HILog.d(TAG, "onCreateView : mMusicListCursor.size = " + size);
        if(size==0) return;
        musicAdapter = new MusicListViewCursorAdapter(getActivity(), mMusicListCursor, Constants.MUSICLIST);
        mMusicListView.setAdapter(musicAdapter);
//        mMusicListView.setSelectionFromTop(lastViewedPosition, topOffset);
    }

    private void scroll(int position) {
        if((position-2)>=0) position -= 2;
        mMusicListView.setSelection(position);
    }

    @Override
    public void onStart() {
        HILog.d(TAG, "onStart : ");
        super.onStart();
        if(!CommonUtils.checkDataBase(getActivity())) SearchMp3AndShowList();
        else showMusicListView();

    }

    private Cursor getMusicListCursor(){
        String whereclause = null;
        Cursor resultCursor=null;
        HILog.d(TAG, "getMusicListCursor:");
        From from = new Select().from(MusicList.class);
        boolean exists = from.exists();
        HILog.d(TAG, "getMusicListCursor: exists  = " + exists);
        if(!exists) return resultCursor;

        if(mSelected==Constants.PLAYLIST){
            whereclause = DBA.Field.PLAYSELECTED ;
        } else {
            whereclause = DBA.Field.WAKEUPSELECTED ;
        }
        whereclause += " = " + StringUtil.addquote(String.valueOf(Constants.CLEARED));
        HILog.d(TAG, "getMusicListCursor: whereclause = " + whereclause);
        String sqlcommand = from.where(whereclause)
                .toSql();
        HILog.d(TAG, "getMusicListCursor: sqlcommand = " + sqlcommand);
        resultCursor = Cache.openDatabase().rawQuery(sqlcommand, null);
        HILog.d(TAG, "getMusicListCursor: count = " + resultCursor.getCount());
        return resultCursor;
    }

    public void SetMusicListField(String fieldname, int position, int value){
        HILog.d(TAG, "SetMusicListField: fieldname = " + fieldname + ", position: " + position + ", value = " + value);
        String whereclause = null;
        String title = null;
        HILog.d(TAG, "SetMusicListField:");
        From from = new Select().from(MusicList.class);
        boolean exists = from.exists();
        HILog.d(TAG, "SetMusicListField: exists  = " + exists);
        if(!exists) return;

        if(mSelected==Constants.PLAYLIST){
            whereclause = DBA.Field.PLAYSELECTED ;
        } else {
            whereclause = DBA.Field.WAKEUPSELECTED ;
        }
        whereclause += " = " + StringUtil.addquote(String.valueOf(Constants.CLEARED));
        HILog.d(TAG, "SetMusicListField: whereclause = " + whereclause);

        List<MusicList> musicLists = from.where(whereclause).execute();
        int size = musicLists.size();
        HILog.d(TAG, "SetMusicListField: total size  = " + size);
        if(size>0&&position>=0){
            ActiveAndroid.beginTransaction();
            switch(fieldname){
                case DBA.Field.NOWPLAYING:
                    title = musicLists.get(position).title;
                    HILog.d(TAG, "SetMusicListField: title = " + title + ", NOWPLAYING: set value to "  + value);
                    musicLists.get(position).now_playing = value;
                    break;
                case DBA.Field.PLAYSELECTED:
                    title = musicLists.get(position).title;
                    HILog.d(TAG, "SetMusicListField: title = " + title + ", PLAYSELECTED: set value to "  + value);
                    musicLists.get(position).play_selected = value;
                    break;
                case DBA.Field.WAKEUPSELECTED:
                    title = musicLists.get(position).title;
                    HILog.d(TAG, "SetMusicListField: title = " + title + ", WAKEUPSELECTED: set value to "  + value);
                    musicLists.get(position).wakeup_selected = value;
                    break;
                default:
                    HILog.d(TAG, "SetMusicListField: not supported fieldname =: " + fieldname );
                    break;
            }
            musicLists.get(position).save();
            ActiveAndroid.setTransactionSuccessful();
            ActiveAndroid.endTransaction();
            CommonUtils.CopyDBFromData2SD(m_ctx);
        } else {
            HILog.d(TAG, "SetMusicListField: null or empty." );
        }
    }

    public static void ClearMusicListField(String fieldname){
        HILog.d(TAG, "ClearMusicListField: fieldname = " + fieldname);
        Select select = new Select();
        String whereclause = fieldname + " = ?";
        List<MusicList> off_selected = select.from(MusicList.class)
                .where(whereclause, Constants.ON)
                .execute();
        int size = off_selected.size();
        HILog.d(TAG, "ClearMusicListField: size = " + size);
        if(size>0){
            ActiveAndroid.beginTransaction();
            for(int i=0; i<size; i++){
                switch(fieldname){
                    case DBA.Field.NOWPLAYING:
                        off_selected.get(i).now_playing = Constants.OFF;
                        break;
                    case DBA.Field.NOWSENDING:
                        off_selected.get(i).now_sending = Constants.OFF;
                        break;
                    case DBA.Field.PLAYSELECTED:
                        off_selected.get(i).play_selected = Constants.OFF;
                        break;
                    case DBA.Field.WAKEUPSELECTED:
                        off_selected.get(i).wakeup_selected = Constants.OFF;
                        break;
                    default:
                        HILog.d(TAG, "ClearMusicListField: not supported fieldname = " + fieldname);
                        break;
                }
                off_selected.get(i).save();
            }
            ActiveAndroid.setTransactionSuccessful();
            ActiveAndroid.endTransaction();
        }
    }

    public void setPlaySelectedColumn(int position, int value){
        String whereclause = null;
        HILog.d(TAG, "setPlaySelectedColumn: position: " + position + ", value = " + value);
        Select select = new Select();

        if(mSelected==Constants.PLAYLIST){
            HILog.d(TAG, "setPlaySelectedColumn: PLAYSELECTED!");
            whereclause = DBA.Field.PLAYSELECTED ;
        } else {
            HILog.d(TAG, "setPlaySelectedColumn: WAKEUPSELECTED!");
            ClearMusicListField(DBA.Field.WAKEUPSELECTED);
            whereclause = DBA.Field.WAKEUPSELECTED ;
        }
        whereclause += " = ?";
        HILog.d(TAG, "setPlaySelectedColumn: whereclause: " + whereclause);

        List<MusicList> musicLists = select.from(MusicList.class)
                .where(whereclause, Constants.CLEARED)
                .execute();
        ActiveAndroid.beginTransaction();
        HILog.d(TAG, "setPlaySelectedColumn: swipped title = " + musicLists.get(position).title);
        if(mSelected==Constants.PLAYLIST){
            musicLists.get(position).play_selected = value;
        } else {
            musicLists.get(position).wakeup_selected = value;
        }
        musicLists.get(position).save();
        ActiveAndroid.setTransactionSuccessful();
        ActiveAndroid.endTransaction();
        CommonUtils.CopyDBFromData2SD(getActivity());
    }

    private Handler MusicItemSwipedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            HILog.d(TAG, "MusicItemSwipedHandler:");
            int pos = (int) msg.obj;
            mNowPosition = pos;
            setPlaySelectedColumn(pos, Constants.SWIPPED);
            m_OneSecondHandler.postDelayed(m_OneSecondTimeoutTask, Constants.SWIPEWAIT);
        }
    };

    private void SearchMp3AndShowList(){
//        new Delete().from(MusicList.class).execute();
        String folder = Environment.getExternalStorageDirectory().getPath();
        dialog_folder(folder);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void dialog_folder(final String folder){
        HILog.d(TAG, "dialog_folder: folder = " + folder);

        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .titleColorRes(R.color.bc_text_fg)
                .positiveColorRes(R.color.bc_text_fg)
                .widgetColorRes(R.color.md_divider_white)
                .title(getString(R.string.search_sdcard))
                .positiveText(R.string.ok)
                .inputType(InputType.TYPE_CLASS_TEXT )
                .input(folder, folder, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        SearchTask mTask=null;
                        String location = String.valueOf(input);
                        HILog.d(TAG, "dialog_folder: onClick: location = " + location);
                        if(StringUtil.isStrNullOrEmpty(location)) mTask = new SearchTask(m_ctx, folder);
                        mTask = new SearchTask(m_ctx, location);
                        mTask.execute("mp3");
                    }
                }).show();
    }

    private Handler MusicListClickHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            HILog.d(TAG, "MusicListClickHandler:");
            SearchMp3AndShowList();
        }
    };

    @Subscribe
    public void getMusicItemSwiped(MusicItemSwiped musicItemSwiped) {
        HILog.d(TAG, "Subscribe : getMusicItemSwiped: ");
        Message myMessage=new Message();
        myMessage.obj = musicItemSwiped.position;
        MusicItemSwipedHandler.sendMessage(myMessage);
    }

    @Subscribe
    public void getMusicListClicked(MusicListClicked musicListClicked) {
        HILog.d(TAG, "Subscribe : getMusicListClicked: ");
        Message myMessage=new Message();
        myMessage.obj = musicListClicked;
        MusicListClickHandler.sendMessage(myMessage);
    }

    @Subscribe
    public void getMusicListUpdate(MusicListUpdate musicListUpdate) {
        HILog.d(TAG, "Subscribe : getMusicListUpdate: ");
        showMusicListView();
    }

    @Override
    public void onDestroyView() {
        HILog.d(TAG, "onDestroyView:");
        super.onDestroyView();
        ClearMusicListField(DBA.Field.NOWPLAYING);
        ClearMusicListField(DBA.Field.NOWSENDING);
        UltraflyModelApplication.getInstance().bus.unregister(this);
        System.gc();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        }
    }

    @Override
    public boolean onLongClick(View v) {
        HILog.d(TAG, "mMusicListView: onLongClick:");
        return false;
    }

    private class SearchTask extends AsyncTask<String, Void, ArrayList<String>> {
        private final Context context;
        String location=null;
        private MaterialDialog md_diaog;

        private SearchTask(Context c) {
            context = c;
        }

        private SearchTask(Context c, String location) {
            context = c;
            this.location = location;
            md_diaog = new MaterialDialog.Builder(context)
                    .title(R.string.app_name)
                    .content(getString(R.string.search) + location)
                    .progress(true, 0)
                    .progressIndeterminateStyle(false)
                    .show();
        }

        @Override
        protected void onPreExecute() {
            HILog.d(TAG, "onPreExecute:");
            if(StringUtil.isStrNullOrEmpty(location)) location = Environment.getExternalStorageDirectory().getPath();

        }

        @Override
        protected ArrayList<String> doInBackground(String... params) {
            HILog.d(TAG, "doInBackground:");
            ArrayList<String> files=null;
            HILog.d(TAG, "SearchTask: doInBackground: location = " + location + ", search for = " + params[0]);

//            files = SimpleFileUtils.searchInDirectory(location, params[0]);
            HILog.d(TAG, "SearchTask: doInBackground: files.size = " + files.size());
            return files;
        }

        @Override
        protected void onPostExecute(final ArrayList<String> files) {
            int len = files != null ? files.size() : 0;
            HILog.d(TAG, "SearchTask: onPostExecute: len = " + len );

            md_diaog.dismiss();

            if (len == 0) {
                new Thread() {
                    public void run() {
                        Looper.prepare();
                        Toast.makeText(context, R.string.itcouldntbefound, Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    };
                }.start();
            } else {
                WriteMP3FilesIntoMusicList(files);
                showMusicListView();
                UltraflyModelApplication.getInstance().bus.post(new PlayListUpdate());
            }
        }
    }

    private void WriteMP3FilesIntoMusicList(ArrayList<String> files){
        String filepath, utf8Filepath, utf8Filename=null, musiclistkey, Artist=null, Album=null, Title=null;
        BigInteger filesize = BigInteger.ZERO;
        int len = files != null ? files.size() : 0;
        HILog.d(TAG, "WriteMP3FilesIntoMusicList: onPostExecute: len = " + len );
        ActiveAndroid.beginTransaction();
        for(int i=0; i<len; i++){
            File file = new File(files.get(i));
            if (file.isFile()) {
                MusicList musicList = new MusicList();
                filepath = file.getPath();
                HILog.d(TAG, "WriteMP3FilesIntoMusicList: filepath = " + filepath);
                utf8Filepath = null;
                try {
                    utf8Filepath = new String(filepath.getBytes(), "UTF-8");
                    utf8Filename = new String(file.getName().getBytes(), "UTF-8");
                    HILog.d(TAG, "WriteMP3FilesIntoMusicList: utf8Filepath = " + utf8Filepath);
                    HILog.d(TAG, "WriteMP3FilesIntoMusicList: utf8Filename = " + utf8Filename);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                HILog.d(TAG, "WriteMP3FilesIntoMusicList: utf8Filepath = " + utf8Filepath);

                SongMetadataReader metadataReader = new SongMetadataReader((Activity) getActivity(), utf8Filepath);
                Artist = metadataReader.mArtist;
                Album = metadataReader.mAlbum;
                Title = metadataReader.mTitle;

                musiclistkey = Artist + "_" + Album + "_" + Title;
                filesize = SimpleFileUtils.CalculatedMBSize(file.length());
                HILog.d(TAG, "WriteMP3FilesIntoMusicList: musiclistkey = " + musiclistkey);
                HILog.d(TAG, "WriteMP3FilesIntoMusicList: artist = " + Artist);
                HILog.d(TAG, "WriteMP3FilesIntoMusicList: album = " + Album);
                HILog.d(TAG, "WriteMP3FilesIntoMusicList: title = " + Title);
                HILog.d(TAG, "WriteMP3FilesIntoMusicList: filesize = " + filesize.toString());
                musicList.musiclistkey = musiclistkey;
                musicList.play_selected = 0;
                musicList.wakeup_selected = 0;
                musicList.path = utf8Filepath;
                musicList.file_name = utf8Filename;
                musicList.artist = Artist;
                musicList.album = Album;
                musicList.title = Title;
                musicList.mbsize = filesize.intValue();
                musicList.save();
            }
        }
        ActiveAndroid.setTransactionSuccessful();
        ActiveAndroid.endTransaction();
        CommonUtils.CopyDBFromData2SD(getActivity());
    }
}