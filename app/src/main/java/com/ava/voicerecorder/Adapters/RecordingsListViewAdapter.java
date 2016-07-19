package com.ava.voicerecorder.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ava.voicerecorder.R;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by ludwig on 17/07/16.
 */
public class RecordingsListViewAdapter extends BaseAdapter{

	private LayoutInflater layoutInflater;
	private ArrayList<String> recordingsArray;

	public RecordingsListViewAdapter(LayoutInflater inflater, ArrayList<String> inputArray){
		this.layoutInflater = inflater;
		this.recordingsArray = inputArray;
	}
	@Override
	public int getCount() {
		return recordingsArray.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		View view = layoutInflater.inflate(R.layout.list_view_item, parent, false);
		holder = new ViewHolder(view);

		holder.text.setText(recordingsArray.get(position));

		return view;
	}

	static class ViewHolder{

		@Bind(R.id.textview_item)
		TextView text;

		public ViewHolder(View view) {
			ButterKnife.bind(this, view);
		}
	}
}
