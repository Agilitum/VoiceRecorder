package com.ava.voicerecorder;

import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ava.voicerecorder.Adapters.RecordingsListViewAdapter;
import com.ava.voicerecorder.Sound.SoundRecorder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;


@RuntimePermissions
public class VoiceRecorderMainActivity extends AppCompatActivity {

	private String LOG_TAG = VoiceRecorderMainActivity.class.getName();

	private boolean clicked = false;
	private boolean clickedItem = false;

	private MediaRecorder mediaRecorder = null;
	private MediaPlayer mediaPlayer = null;

	private File audioFile = null;

	@Bind(R.id.activity_voice_recorder_main_recordButton)
	ImageButton recordButton;

	@Bind(R.id.recordings_listView)
	ListView recordingsListView;

	@Bind(R.id.listView_descriptor_textView)
	TextView listViewDescriptor;

	private ArrayList<String> recentRecordingsArray;
	private RecordingsListViewAdapter recordingsListViewAdapter;

	private boolean writeExternalStorageAllowed = false;
	private boolean recordAudioAllowed = false;

	SoundRecorder soundRecorder;


	/** Runtime Permissions **/

	// Record Audio permissions
	@NeedsPermission(Manifest.permission.RECORD_AUDIO)
	void allowRecordAudio(){
		// set trigger record is allowed
		recordAudioAllowed = true;

		// chain second required permission here
		VoiceRecorderMainActivityPermissionsDispatcher.allowWriteExternalStorageWithCheck(this);
	}

	@OnShowRationale(Manifest.permission.RECORD_AUDIO)
	void showRationaleForRecordAudio(final PermissionRequest request){
		new AlertDialog.Builder(this)
			.setMessage(R.string.permission_recordAudio_rationale)
				.setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						request.proceed();
					}
				})
			.setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					request.cancel();
				}
			})
			.show();
	}

	@OnPermissionDenied(Manifest.permission.RECORD_AUDIO)
	void showDeniedForRecordAudio(){
		Toast.makeText(this, R.string.permissions_recordaudio_denied, Toast.LENGTH_SHORT).show();
	}

	@OnNeverAskAgain(Manifest.permission.RECORD_AUDIO)
	void showNeverAskForRecordAudio(){
		Toast.makeText(this, R.string.permissions_recordaudio_neverask, Toast.LENGTH_SHORT).show();

	}


	//External Storage permissions
	@NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	void allowWriteExternalStorage(){
		// set trigger write external storage is allowed
		writeExternalStorageAllowed = true;
	}

	@OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	void showRationaleForWriteExternalStorage(final PermissionRequest request){
		new AlertDialog.Builder(this)
			.setMessage(R.string.permission_writeExternalStorage_rationale)
			.setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					request.proceed();
				}
			})
			.setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					request.cancel();
				}
			})
			.show();
	}

	@OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	void showDeniedForWriteExternalStorage(){
		Toast.makeText(this, R.string.permissions_writeExternalStorage_denied, Toast.LENGTH_SHORT)
			.show();
	}

	@OnNeverAskAgain(Manifest.permission.WRITE_EXTERNAL_STORAGE)
	void showNeverAskForWriteExternalStorage(){
		Toast.makeText(this, R.string.permissions_writeExternalStorage_neverask, Toast.LENGTH_SHORT)
			.show();

	}



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_voice_recorder_main);

		ButterKnife.bind(this);

		recentRecordingsArray = new ArrayList<>();
		recordingsListViewAdapter = new RecordingsListViewAdapter((LayoutInflater) getSystemService
			(LAYOUT_INFLATER_SERVICE), recentRecordingsArray);
		recordingsListView.setAdapter(recordingsListViewAdapter);

		VoiceRecorderMainActivityPermissionsDispatcher.allowRecordAudioWithCheck(this);

		soundRecorder = new SoundRecorder();

		discoverRecordings();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		VoiceRecorderMainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode,
			grantResults);
	}


	@OnClick(R.id.activity_voice_recorder_main_recordButton)
	protected void onClick(){
		if (!clicked) {

			if(recordAudioAllowed && writeExternalStorageAllowed) {
				buttonBackgroundAnimation(true);

				onRecord(true);
			} else {
				Toast.makeText(this, "Permissions missing", Toast.LENGTH_LONG).show();
			}

			clicked = true;
		} else {

			buttonBackgroundAnimation(false);

			onRecord(false);

			clicked = false;
		}
	}

	@OnItemClick(R.id.recordings_listView)
	public void onItemClick(AdapterView<?> parent, int position){
		if(clickedItem) {
			onPlay(false, null);
			clickedItem = false;
		} else {
			onPlay(true, recentRecordingsArray.get(position));
			clickedItem = true;
		}
	}

	private void buttonBackgroundAnimation(Boolean on){
		// init animation for button background
		final AnimationDrawable drawable = new AnimationDrawable();
		drawable.addFrame(new ColorDrawable(Color.RED), 1200);
		drawable.addFrame(new ColorDrawable(Color.TRANSPARENT), 1200);
		drawable.setOneShot(false);

		if(on){
			recordButton.setImageResource(R.drawable.microphone_on);
			recordButton.setBackgroundColor(Color.RED);

			// start pulsing when recording
			recordButton.setBackground(drawable);
			drawable.start();
		} else {
			recordButton.setImageResource(R.drawable.microphone_off);
			recordButton.setBackgroundColor(Color.TRANSPARENT);

			recordButton.clearAnimation();
		}
	}

	public void onRecord(Boolean start){
		if(start){
			startRecording();
		} else {
			stopRecording();
		}
	}

	private void startRecording(){
		soundRecorder.startRecording();
	}


	private void stopRecording(){
		soundRecorder.stopRecording();

		// discover old & new audio recordings
		discoverRecordings();
	}


	private void discoverRecordings(){
		String path = Environment.getExternalStorageDirectory().toString() + "/" + soundRecorder.AUDIO_RECORDER_FOLDER;

		File f = new File(path);
		File files[] = f.listFiles();

		ArrayList<String> recordings = new ArrayList<>();
		for (int i = 0; i < files.length; i++) {
			recordings.add(files[i].getName());
		}

		recentRecordingsArray.clear();
		recentRecordingsArray.addAll(recordings);
		recordingsListViewAdapter.notifyDataSetChanged();

		showListViewDescriptor();

	}


	private void showListViewDescriptor(){
		if(recentRecordingsArray.size() > 0){
			listViewDescriptor.setVisibility(View.VISIBLE);
		} else {
			listViewDescriptor.setVisibility(View.INVISIBLE);
		}
	}

 private void onPlay(boolean start, String recording) {
		if (start) {
			startPlaying(recording);
		} else {
			stopPlaying();
		}
	}

	@SuppressWarnings("setSubtitleAnchor")
	private void startPlaying(String recordingName) {
		mediaPlayer = new MediaPlayer();
		try {
			mediaPlayer.setDataSource("/storage/emulated/0/AudioRecorder/" + recordingName);
			mediaPlayer.prepare();
			mediaPlayer.start();
		} catch (IOException e) {
			Log.e(LOG_TAG, "prepare() failed");
		}
	}

	private void stopPlaying() {
		mediaPlayer.release();
		mediaPlayer = null;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mediaRecorder != null) {
			mediaRecorder.release();
			mediaRecorder = null;
		}

		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
	}
}
