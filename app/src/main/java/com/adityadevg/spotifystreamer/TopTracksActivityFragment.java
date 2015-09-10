package com.adityadevg.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.adityadevg.spotifystreamer.toptracks.Tracks;
import com.adityadevg.spotifystreamer.toptracks.TracksArrayAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import retrofit.RetrofitError;


/**
 * A placeholder fragment containing a simple view.
 */
public class TopTracksActivityFragment extends Fragment {

    private TracksArrayAdapter tracksArrayAdapter;
    private List<Tracks> listOfTracks;
    private SpotifyApi api;
    private SpotifyService spotifyService;
    private List<Tracks> localListOfTracks = new ArrayList<Tracks>();
    String artistId, artistName;

    public TopTracksActivityFragment() {
        listOfTracks = new ArrayList<Tracks>();
        api = new SpotifyApi();
        spotifyService = api.getService();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putParcelableArrayList(getString(R.string.tracklist_parcel_key), (ArrayList<? extends Parcelable>) listOfTracks);
        super.onSaveInstanceState(bundle);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null || !savedInstanceState.containsKey(getString(R.string.track_parcel_key))) {
            // get artist name from previous intent
            Intent intent = getActivity().getIntent();
            artistId = intent.getStringExtra(Intent.EXTRA_TEXT);
            artistName = intent.getStringExtra(getString(R.string.artist_name_key));
            new FetchTracksTask().execute(artistId);
        } else {
            listOfTracks = savedInstanceState.getParcelableArrayList(getString(R.string.track_parcel_key));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_tracks);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent newIntent = new Intent(getActivity(), MediaPlayerActivity.class);
                newIntent.putParcelableArrayListExtra(getString(R.string.track_list_key), (ArrayList<? extends Parcelable>) listOfTracks);
                newIntent.putExtra(getString(R.string.track_position_key), position);
                startActivity(newIntent);
            }
        });

        tracksArrayAdapter = new TracksArrayAdapter(getActivity(), listOfTracks);
        listView.setAdapter(tracksArrayAdapter);

        Intent baseIntent = getActivity().getIntent();
        new FetchTracksTask().execute(baseIntent.getStringExtra(getString(R.string.artist_id_key)));
        return rootView;
    }

    public class FetchTracksTask extends AsyncTask<String, Void, List<Tracks>> {
        private static final int MAX_TRACKS = 10;
        private final String LOG_TAG = TopTracksActivity.class.getSimpleName();
        List<kaaes.spotify.webapi.android.models.Artist> spotifyArtistList;
        Map<String, Object> options;
        kaaes.spotify.webapi.android.models.Tracks spotifyTracks;

        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         * <p/>
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param params The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        @Override
        protected List<Tracks> doInBackground(String... params) {
            try {
                if (null != params[0]) {
                    localListOfTracks.clear();
                    if (null != params[0]) {
                        options = new HashMap<String, Object>();
                        options.put(getString(R.string.country), getString(R.string.country_name));
                        spotifyTracks = spotifyService.getArtistTopTrack(params[0], options);
                        if (null != spotifyTracks.tracks && !spotifyTracks.tracks.isEmpty()) {
                            int size = spotifyTracks.tracks.size() > MAX_TRACKS ? MAX_TRACKS : spotifyTracks.tracks.size();
                            String albumName;
                            String albumThumbnailLink;
                            String trackName;
                            String artistName;
                            String previewUrl;
                            String externalSpotifyLink;

                            kaaes.spotify.webapi.android.models.Track currentTrack;
                            for (int i = 0; i < size; i++) {
                                currentTrack = spotifyTracks.tracks.get(i);
                                if (null != currentTrack && null != currentTrack.album) {
                                    albumName = currentTrack.album.name;
                                    artistName = currentTrack.artists.get(0).name;
                                    previewUrl = currentTrack.preview_url;
                                    externalSpotifyLink = currentTrack.external_urls.get("spotify");
                                    if (null != currentTrack.album.images && !currentTrack.album.images.isEmpty()) {
                                        albumThumbnailLink = currentTrack.album.images.get(0).url;
                                        trackName = currentTrack.name;

                                        localListOfTracks.add(new Tracks(albumName, albumThumbnailLink, trackName, artistName, previewUrl, externalSpotifyLink));
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (
                    RetrofitError rfe) {
                Log.d(LOG_TAG, rfe.getMessage());
            }
            return localListOfTracks;
        }

        @Override
        protected void onPostExecute(List<Tracks> tracksResult) {
            if (null != tracksResult) {
                if (tracksResult.isEmpty())
                    Toast.makeText(getActivity(), getString(R.string.no_results), (Toast.LENGTH_LONG)).show();
                else
                    tracksArrayAdapter.addAll(tracksResult);
            }
        }
    }
}
