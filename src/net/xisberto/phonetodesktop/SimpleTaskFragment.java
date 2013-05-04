package net.xisberto.phonetodesktop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass. Activities that
 * contain this fragment must implement the
 * {@link SimpleTaskFragment.OnAdvancedTaskOptionsListener} interface to handle
 * interaction events. Use the {@link SimpleTaskFragment#newInstance} factory
 * method to create an instance of this fragment.
 * 
 */
public class SimpleTaskFragment extends SherlockFragment {

	/**
	 * Use this factory method to create a new instance of this fragment using
	 * the provided parameters.
	 * 
	 * @param param1
	 *            Parameter 1.
	 * @param param2
	 *            Parameter 2.
	 * @return A new instance of fragment SimpleTaskFragment.
	 */
	// TODO: Rename and change types and number of parameters
	public static SimpleTaskFragment newInstance() {
		SimpleTaskFragment fragment = new SimpleTaskFragment();
		return fragment;
	}

	public SimpleTaskFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater
				.inflate(R.layout.fragment_simple_task, container, false);
	}

}
