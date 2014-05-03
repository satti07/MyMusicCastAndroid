package com.aanchal.youtubemysongs;

import com.aanchal.youtubemysongs.Song;
import com.aanchal.youtubemysongs.DeveloperKey;
import com.aanchal.youtubemysongs.SongLogger;
import com.aanchal.youtubemysongs.AppRater;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings.Secure;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import com.aanchal.youtubemysongs.R;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;

import com.google.ads.*;

public class MainActivity extends Activity {

	static final int MAX_QUERY_SONGS = 5;
	private static final int SONG_CANDIDATES = 1;
	private static final String YOUTUBE_VIDEO_INFORMATION_URL = "http://www.youtube.com/get_video_info?&video_id=";

	ListView musiclist;
    Cursor musiccursor;
    Song querySong;
    List<Song> allSongs;
    List<Song> currentSongs;
    int count;
    static int selectedIndex;
    private ConnectivityManager cm;
    Activity myself;
    EditText inputSearch;
    MusicAdapter adapter;
    private ProgressDialog progressDialog; 
    SongLogger logger;
    static String duration;
    long startTime = -1;
    static Random randomGenerator = new Random();
    static AppRater appRate = new AppRater();
	
    
    private TextWatcher searchTextWatcher = new TextWatcher() {
       // @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            //@Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

           // @Override
            public void afterTextChanged(Editable s) {
                adapter.getFilter().filter(s.toString());
            }
        };
        
        private class LoggerTask extends AsyncTask<Object, Void, Long> {
        	protected Long doInBackground(Object... param) {
	        	logger.resetAndSend(MainActivity.this);
	        	return (long) 1;
        	 }
        }
    
    class Process extends AsyncTask<Object, Void, String> {
		
		 
		 @Override
	        protected void onPreExecute()
	        {
	            super.onPreExecute();   
	            progressDialog = ProgressDialog.show(MainActivity.this, null, "Automagic search in progress...", true, false); 
	        }

	        @Override
	        protected String doInBackground(Object... param) {
	        	return getVideoIdForSong(querySong);
        	 }
	        
	        private boolean canResolveIntent(Intent intent) {
	            List<ResolveInfo> resolveInfo = getPackageManager().queryIntentActivities(intent, 0);
	            return resolveInfo != null && !resolveInfo.isEmpty();
	        }
	        
	        private int getDuration() {
	        	try {
	        		return Integer.parseInt(duration);
	        	} catch (Exception e) {
	        		return -1;
	        	}
	        }

	        @Override
	        protected void onPostExecute(String result)
	        {
	        	if (progressDialog != null)
	        		progressDialog.dismiss();
	            super.onPostExecute(result);	            
	        	if (result == null) 
		        	   Toast.makeText(myself, "Sorry! No video found :(", Toast.LENGTH_SHORT).show();
	        	else {
             		Intent intent = YouTubeStandalonePlayer.createVideoIntent(
             	         myself , DeveloperKey.DEVELOPER_KEY, result,0, true, false);
             		if (intent == null || !canResolveIntent(intent))
             			YouTubeInitializationResult.SERVICE_MISSING.getErrorDialog(myself, 2).show();
             		else {
             			startTime = System.currentTimeMillis();
             			logger.videoStarted(result, querySong, getDuration(), selectedIndex);
             			startActivity(intent);
             		}
		        }    
	        }
	}
    
    public static String sha256(String base) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch(Exception ex){
           throw new RuntimeException(ex);
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	logger =new SongLogger(sha256(Secure.getString(this.getContentResolver(),
	                Secure.ANDROID_ID)));
	    	
		
          super.onCreate(savedInstanceState);
          setContentView(R.layout.mainactivity);
          cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
          myself = this;
          inputSearch = (EditText) findViewById(R.id.inputSearch);
          inputSearch.addTextChangedListener(searchTextWatcher);
          init_phone_music_grid();    
          startTime = -1;
      }
    
    @Override
    public void onPause() {
        super.onPause();
        //Log.v("Logging", "Pause Called");
        

        if(progressDialog != null)
            progressDialog.dismiss();
        progressDialog = null;
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	//Log.v("Logging", "Start Called");
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	//Log.v("Logging", "Resume Called");
    	new LoggerTask().execute(null,null,null); 
    	if (startTime != -1) {
    		int elapsedTime = (int) ((System.currentTimeMillis() - startTime)/1000);
    		//Log.v("Logging", "Registering a click with time = " + Integer.toString(elapsedTime));
    		AppRater.songClicked(this, elapsedTime > 20);  
    	}
    	startTime = -1;
    }
    
    @Override
    public void onStop() {
    	super.onStop();
    	//Log.v("Logging", "Stop Called");
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //Log.v("Logging","Orientation changed");
        /*// Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }*/
    }
    
    @SuppressWarnings("deprecation")
	private void init_phone_music_grid() {
          System.gc();
          String[] proj = { MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DISPLAY_NAME,MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION };
          try {
          musiccursor = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,proj, null, null, null);
          // TODO: Check if musiccursor is null
          count = musiccursor.getCount();
          int titleCol = musiccursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
          int artistCol = musiccursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
          int albumCol = musiccursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
          int durationCol = musiccursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
          allSongs = new ArrayList<Song>();
          currentSongs = new ArrayList<Song>();
          musiccursor.moveToFirst();
          for(int i = 0; i < count; ++i, musiccursor.moveToNext()) { 
        	  int duration = musiccursor.getInt(durationCol) / 1000;
        	  allSongs.add(new Song(musiccursor.getString(titleCol), musiccursor.getString(albumCol), musiccursor.getString(artistCol), duration));
           	  currentSongs.add(new Song(musiccursor.getString(titleCol), musiccursor.getString(albumCol), musiccursor.getString(artistCol), duration));
          }
          musiclist = (ListView) findViewById(R.id.PhoneMusicList);
          adapter = new MusicAdapter(getApplicationContext());
          musiclist.setAdapter(adapter);
          musiclist.setOnItemClickListener(musicgridlistener);
          } catch (NullPointerException e) {
        	  Toast.makeText(getApplicationContext(), "No songs found on phone", Toast.LENGTH_LONG).show();
          }
    }
    
	// returns query results in JSON form
	private static JSONArray getResults(String query) {
		if (query.contains("<unknown>"))
			return null;
		try {
		// TODO: Get short and medium duration only, exclude long videos
		String url = "https://www.googleapis.com/youtube/v3/search?part=id&videoSyndicated=true&type=video&q=" + query + "&max_results=" + MAX_QUERY_SONGS + "&key=" + DeveloperKey.YOUTUBE_API_KEY;
		URL jsonURL = new URL(url);
		URLConnection jc = jsonURL.openConnection();
		InputStream is = jc.getInputStream();
		String jsonTxt = IOUtils.toString( is );
		//Log.v("temp","Query:" + query + " Returned: " + jsonTxt);
		JSONObject jj = new JSONObject(jsonTxt);
		JSONArray ret = jj.getJSONArray("items");
		if (ret.length() == 0)
			return null;
		return ret;
		} catch (Exception e) {
			//e.printStackTrace();
			//Log.v("Duration",e.toString());
			return null;
		}
	}
	
	private static boolean isNum(char ch) {
		return Character.isDigit(ch);
	}
	
	private static String getDurationinSeconds(String str) {
		int ret = 0;
		int index = str.length() - 1;
		while(index > 0 && str.charAt(index) != 'S') --index;
		if (str.charAt(index) == 'S') {
			int temp = index - 1;
			while(temp >=0 && isNum(str.charAt(temp))) --temp;
			ret += Integer.parseInt(str.substring(temp + 1, index));
		}
		while(index > 0 && str.charAt(index) != 'M') --index;
		if (str.charAt(index) == 'M') {
			int temp = index - 1;
			while(temp >=0 && isNum(str.charAt(temp))) --temp;
			ret += 60*Integer.parseInt(str.substring(temp + 1, index));
		}
		while(index > 0 && str.charAt(index) != 'H') --index;
		if (str.charAt(index) == 'H') {
			int temp = index - 1;
			while(temp >=0 && isNum(str.charAt(temp))) --temp;
			ret += 3600*Integer.parseInt(str.substring(temp + 1, index));
		}
		return Integer.toString(ret);
	}
	
	// given a song, retrieve the youtube id, returns null if no playable video is found
	private static String getVideoIdForSong(Song song) {
		JSONArray[] results = new JSONArray[3];
		results[0] = getResults(song.getAlbumQueryString());
		results[1] = getResults(song.getArtistQueryString());
		if (results[0] == null && results[1] == null) {
			results[2] = getResults(song.getTitleQueryString());
			//Log.v("Duration","q1:" + song.getAlbumQueryString() + " q2: " + song.getArtistQueryString() + " q3:" + song.getTitleQueryString());
		}
		else {
			results[2] = getResults(song.getArtistAlbumQueryString());
			//Log.v("Duration","q1:" + song.getAlbumQueryString() + " q2: " + song.getArtistQueryString() + " q3:" + song.getArtistAlbumQueryString());
		}
		int[] indexes = new int[3];
		indexes[0] = indexes[1] = indexes[2] = 0;
		String best_duration = null;
		String best_videoid = null;
		//Log.v("Duration","Res1: " + results[0].length() + " Res2: " + results[1].length() + " Res3: " + results[2].length());
		while(true) {
			Boolean exhausted = true;
			for(int i = 0; i < 3; ++i) 
				if (results[i] != null && indexes[i] < results[i].length()) {
					try {
					  
					  JSONObject item0 = results[i].getJSONObject(indexes[i]);
					  indexes[i]++;
					  exhausted = false;
					  String ret = item0.getJSONObject("id").getString("videoId");
					  String url = "https://www.googleapis.com/youtube/v3/videos?id=" + ret +
							  "&part=contentDetails" + "&key=" + DeveloperKey.YOUTUBE_API_KEY;
					  URL jsonURL = new URL(url);
					  URLConnection jc = jsonURL.openConnection();
					  InputStream is = jc.getInputStream();
					  String jsonTxt = IOUtils.toString( is );
					  JSONObject jj = new JSONObject(jsonTxt);
					  JSONArray items = jj.getJSONArray("items");
					  if (items.length() != 1) {
						  Log.e("Error","This should not happen");
					  } else {
						  String dur = items.getJSONObject(0).getJSONObject("contentDetails").getString("duration");
						  String cur_duration = getDurationinSeconds(dur);
						  String videoid = ret;
						  int video_length = Integer.parseInt(cur_duration);
						  if (Math.abs(video_length - song.duration) < 30) {
							  //Log.v("Duration", "Video length = " + cur_duration + " Song length = " + song.duration);
							  duration = cur_duration;
							  //Log.v("Duration", "id:" + videoid);
							  return videoid;
						  } else if (best_videoid == null) {
							  //Log.v("Duration", "Not matched Video length = " + cur_duration + " Song length = " + song.duration);
							  best_videoid = videoid;
							  best_duration = cur_duration;
						  }
					  } 
					  exhausted = false;
					} catch (Exception e) {
						//Log.v("Duration",e.toString());
						// do nothing, continue
					}
					  
				}
			if (exhausted)
				break;
		}
		//Log.v("Duration","Res1: " + indexes[0] + " Res2: " + indexes[1] + " Res3: " + indexes[2]);
		
		duration = best_duration;
		return best_videoid;
	}

    private OnItemClickListener musicgridlistener = new OnItemClickListener() {
          public void onItemClick(AdapterView parent, View v, int position,long id) {
        	  try {
                    //Upon clicking on a song name this will retrieve MAX_QUERY_SONGS number of songs from youtube and play the top result.
                    System.gc();
                    querySong = currentSongs.get(position);
                  	if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected())
                  		new Process().execute(null,null,null); 
                  	else 
                  		Toast.makeText(getApplicationContext(), "No internet connection", Toast.LENGTH_LONG).show();
             
                  } catch (Exception e) {e.printStackTrace();}
          }
    };

    public class MusicAdapter extends BaseAdapter implements Filterable {
      private Context mContext;

      public MusicAdapter(Context c) {
        mContext = c;
      }

      public int getCount() {
        return currentSongs.size();
      }

      public Object getItem(int position) {
        return position;
      }

      public long getItemId(int position) {
        return position;
      }

      public View getView(int position, View convertView, ViewGroup parent) {
        System.gc();
        TextView tv;
        if (convertView == null) {
          tv = new TextView(mContext.getApplicationContext());
        } else{
          tv = (TextView) convertView;
        }
        tv.setTextSize(12);
        Song song = currentSongs.get(position);
        int titleLength = song.title.length();
        int artistLength = song.artist.length();
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(song.title + "\n"+song.artist);
        stringBuilder.setSpan(new RelativeSizeSpan(1.5f), 0, titleLength,Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        stringBuilder.setSpan(new ForegroundColorSpan(Color.rgb(135, 206, 250)), titleLength + 1, titleLength + artistLength +1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        tv.setText(stringBuilder);
        return tv;
      }

	//@Override
	public Filter getFilter() {
		return new Filter() {
			  @SuppressWarnings("unchecked")
	            @Override
	            protected void publishResults(CharSequence constraint, FilterResults results) {
				  // Now we have to inform the adapter about the new list filtered
				    if (results.count == 0)
				        notifyDataSetInvalidated();
				    else {
				        currentSongs = (List<Song>) results.values;
				        notifyDataSetChanged();
				    }
				}

	            @Override
	            protected FilterResults performFiltering(CharSequence constraint) {
	            	 FilterResults results = new FilterResults();
	            	    // We implement here the filter logic
	            	    if (constraint == null || constraint.length() == 0) {
	            	        // No filter implemented we return all the list
	            	        results.values = allSongs;
	            	        results.count = allSongs.size();
	            	    }
	            	    else {
	            	        // We perform filtering operation
	            	        List<Song> filteredSongs = new ArrayList<Song>();
	            	        String searchString = constraint.toString().toLowerCase();
	            	        for (Song song : allSongs) {
	            	        	if (song.title.toLowerCase().contains(searchString) ||
	            	        		song.artist.toLowerCase().contains(searchString))
	            	                filteredSongs.add(song);
	            	        }
	            	         
	            	        results.values = filteredSongs;
	            	        results.count = filteredSongs.size();
	            	 
	            	    }
	            	    return results;
	            }
		};
	}
    }
}



