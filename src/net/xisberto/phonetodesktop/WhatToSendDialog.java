package net.xisberto.phonetodesktop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class WhatToSendDialog extends SherlockDialogFragment {
	public static WhatToSendDialog newInstance() {
		WhatToSendDialog dialog = new WhatToSendDialog();
		return dialog;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(STYLE_NORMAL, R.style.AppTheme_Sherlock_Light_Dialog_Titlebar);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().finish();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.layout_what_to_send, container, false);
		ListView list = (ListView) v.findViewById(R.id.list_what_to_send);
		if (getActivity() instanceof OnItemClickListener) {
			list.setOnItemClickListener((OnItemClickListener) getActivity());
		}
		if (getShowsDialog()) {
			getDialog().setTitle(R.string.title_what_to_send);
		}
		
		return v;
		
	}
}