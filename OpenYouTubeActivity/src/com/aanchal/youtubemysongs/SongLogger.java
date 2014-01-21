package com.aanchal.youtubemysongs;

import java.util.ArrayList;
import java.util.List;

import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Log;
import com.aanchal.youtubemysongs.Song;
import com.aanchal.youtubemysongs.DeveloperKey;
import com.aanchal.youtubemysongs.JSONParser;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SongLogger {
	String youtubeId;
	Song userSong;
	int songDuration;
	long startTime;
	String androidId;
	static final int VERSION = 1;
	JSONParser jsonParser = new JSONParser();
	
	public SongLogger(String android_id) {
		this.startTime = -1;
		this.androidId = android_id;
	}
	
	void videoStarted(String videoId, Song song, int duration) {
		youtubeId = videoId;
		userSong = song;
		songDuration = duration;
		startTime = System.currentTimeMillis();
	}
	
	void resetAndSend() {
		if (startTime != -1) {
			int elapsedTime = (int) ((System.currentTimeMillis() - startTime)/1000);
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("name", "Rahul"));
			params.add(new BasicNameValuePair("fav_color", "yellow"));
			
			// getting JSON Object
			// Note that create product url accepts POST method
			JSONObject json = jsonParser.makeHttpRequest(DeveloperKey.DB_URL,
					"POST", params);
			Log.v("Logging",
				   userSong.title + userSong.album + userSong.artist + Integer.toString(elapsedTime) + androidId);
		}
		startTime = -1;
	}

}
