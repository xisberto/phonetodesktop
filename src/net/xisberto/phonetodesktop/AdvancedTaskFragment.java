package net.xisberto.phonetodesktop;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass. Activities that
 * contain this fragment must implement the
 * {@link AdvancedTaskFragment.OnAdvancedTaskOptionsListener} interface to
 * handle interaction events. Use the {@link AdvancedTaskFragment#newInstance}
 * factory method to create an instance of this fragment.
 * 
 */
public class AdvancedTaskFragment extends SimpleTaskFragment implements OnCheckedChangeListener {
	
	private OnAdvancedTaskOptionsListener mListener;
	private View view;

	/**
	 * Use this factory method to create a new instance of this fragment using
	 * the provided parameters.
	 * 
	 * @param text
	 *            The text to send
	 * @return A new instance of fragment AdvancedTaskFragment.
	 */
	public static AdvancedTaskFragment newInstance(String text) {
		AdvancedTaskFragment fragment = new AdvancedTaskFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_TEXT, text);
		fragment.setArguments(args);
		return fragment;
	}

	public AdvancedTaskFragment() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		view = inflater.inflate(R.layout.fragment_advanced_task, container,
				false);
		((TextView)view.findViewById(R.id.text_extra)).setText(mText);
		
		((CheckBox)view.findViewById(R.id.cb_only_links)).setOnCheckedChangeListener(this);
		
		return view;
	}

	// TODO: Rename method, update argument and hook method into UI event
	public void onButtonPressed(Uri uri) {
		if (mListener != null) {
			mListener.onFragmentInteraction(uri);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnAdvancedTaskOptionsListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
		case R.id.cb_only_links:
			if (isChecked) {
				((TextView)view.findViewById(R.id.text_extra)).setText(filterLinks(mText));
			} else {
				((TextView)view.findViewById(R.id.text_extra)).setText(mText);
			}
			break;

		default:
			break;
		}
	}
	
	private String filterLinks(String text) {
		String[] parts = text.split("\\s");
		String result = "";
		for (int i = 0; i < parts.length; i++) {
			try {
				URL u = new URL(parts[i]);
				result += parts[i] + " ";
			} catch (MalformedURLException e) {
				// do nothing
			}
		}
		return result;
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated to
	 * the activity and potentially other fragments contained in that activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnAdvancedTaskOptionsListener {
		// TODO: Update argument type and name
		public void onFragmentInteraction(Uri uri);
	}

}
