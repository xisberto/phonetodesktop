package net.xisberto.phonetodesktop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.google.api.services.tasks.model.Task;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass. Activities that
 * contain this fragment must implement the
 * {@link SimpleTaskFragment.OnAdvancedTaskOptionsListener} interface to handle
 * interaction events. Use the {@link SimpleTaskFragment#newInstance} factory
 * method to create an instance of this fragment.
 * 
 */
public class SimpleTaskFragment extends SherlockFragment {
	protected static final String EXTRA_TEXT = "extra_text";

	protected String mText;

	private View mView;

	/**
	 * Use this factory method to create a new instance of this fragment using
	 * the provided parameters.
	 * 
	 * @param text
	 *            The text to send
	 * @return A new instance of fragment AdvancedTaskFragment.
	 */
	public static SimpleTaskFragment newInstance(String text) {
		SimpleTaskFragment fragment = new SimpleTaskFragment();
		Bundle args = new Bundle();
		args.putString(EXTRA_TEXT, text);
		fragment.setArguments(args);
		return fragment;
	}

	public SimpleTaskFragment() {
		// Required empty public constructor
	}

	/**
	 * Build the {@link Task} object to be sent
	 * @return the Task to be sent
	 */
	public Task buildTask() {
		Task task = new Task();
		task.setTitle(((TextView) mView.findViewById(R.id.text_preview))
				.getText().toString());
		return task;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mText = getArguments().getString(EXTRA_TEXT);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		mView = inflater.inflate(R.layout.fragment_simple_task, container,
				false);
		((TextView) mView.findViewById(R.id.text_preview)).setText(mText);
		return mView;
	}

}
