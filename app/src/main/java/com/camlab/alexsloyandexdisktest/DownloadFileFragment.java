package com.camlab.alexsloyandexdisktest;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Handler;
import android.util.Log;

import com.yandex.disk.client.Credentials;
import com.yandex.disk.client.ListItem;
import com.yandex.disk.client.ProgressListener;
import com.yandex.disk.client.TransportClient;
import com.yandex.disk.client.exceptions.WebdavException;

import java.io.File;
import java.io.IOException;

public class DownloadFileFragment extends Fragment {

    private static final String TAG = "LoadFileFragment";

    private static final String WORK_FRAGMENT_TAG = "LoadFileFragment.Background";

    private static final String FILE_ITEM = "photoframe.file.item";

    protected static final String CREDENTIALS = "photoframe.credentials";

    protected static final String SHOW_ID = "show.id";

    private Credentials credentials;
    private ListItem item;
    private int showId;

    private static ProgressDialog mProgressDialog;
    private static boolean firstDialog = true;


    private DownloadFileRetainedFragment workFragment;

    public static DownloadFileFragment newInstance(Credentials credentials, ListItem item, int showId) {
        DownloadFileFragment fragment = new DownloadFileFragment();

        Bundle args = new Bundle();
        args.putInt(SHOW_ID, showId);
        args.putParcelable(CREDENTIALS, credentials);
        args.putParcelable(FILE_ITEM, item);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        credentials = getArguments().getParcelable(CREDENTIALS);
        item = getArguments().getParcelable(FILE_ITEM);
        showId = getArguments().getInt(SHOW_ID);

        if (firstDialog) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.show();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fragmentManager = getActivity().getFragmentManager();
        workFragment = (DownloadFileRetainedFragment) fragmentManager.findFragmentByTag(WORK_FRAGMENT_TAG + item.getEtag());
        workFragment = new DownloadFileRetainedFragment();
        fragmentManager.beginTransaction().add(workFragment, WORK_FRAGMENT_TAG + item.getEtag()).commit();
        workFragment.setTargetFragment(getActivity().getFragmentManager()
                .findFragmentByTag(SlideshowFragment.TAG_PREFIX + showId), 0);
        workFragment.loadFile(getActivity(), credentials, item);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (workFragment != null) {
            workFragment.setTargetFragment(null, 0);
        }
    }

    public static class DownloadFileRetainedFragment extends Fragment implements ProgressListener {

        private boolean cancelled;
        private File result;
        protected Handler handler;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setRetainInstance(true);
            handler = new Handler();
        }


        @Override
        public void onSaveInstanceState(final Bundle outState) {
            setTargetFragment(null, -1);
        }

        public void loadFile(final Context context, final Credentials credentials, final ListItem item) {
            result = new File(context.getFilesDir(), new File(item.getFullPath()).getName());

            new Thread(new Runnable() {
                @Override
                public void run () {
                    TransportClient client = null;
                    try {
                        client = TransportClient.getInstance(context, credentials);
                        client.downloadFile(item.getFullPath(), result, DownloadFileRetainedFragment.this);
                        downloadComplete();
                    } catch (IOException ex) {
                        Log.d(TAG, "loadFile", ex);
                        ex.printStackTrace();
                    } catch (WebdavException ex) {
                        Log.d(TAG, "loadFile", ex);
                        ex.printStackTrace();
                    } finally {
                        if (client != null) {
                            client.shutdown();
                        }
                    }
                }
            }).start();

        }

        @Override
        public void updateProgress (final long loaded, final long total) {

        }

        @Override
        public boolean hasCancelled () {
            return cancelled;
        }

        public void downloadComplete() {
            handler.post(new Runnable() {
                @Override
                public void run () {
                    SlideshowFragment targetFragment = (SlideshowFragment) getTargetFragment();
                    if (targetFragment != null) {
                        targetFragment.addPhoto(result);
                    }
                }
            });
            if (firstDialog) {
                mProgressDialog.dismiss();
                firstDialog = false;
            }
        }

        public void cancelDownload() {
            cancelled = true;
        }
    }
}

