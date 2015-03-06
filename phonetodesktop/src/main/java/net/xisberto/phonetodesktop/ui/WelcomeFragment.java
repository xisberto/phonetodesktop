package net.xisberto.phonetodesktop.ui;

import net.xisberto.phonetodesktop.ui.MainFragment.PhoneToDesktopAuthorization;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass. Activities that
 * contain this fragment must implement the
 * {@link WelcomeFragment.OnFragmentInteractionListener} interface to handle
 * interaction events. Use the {@link WelcomeFragment#newInstance} factory
 * method to create an instance of this fragment.
 * 
 */
public class WelcomeFragment extends SherlockFragment implements OnClickListener {

	private PhoneToDesktopAuthorization mListener;
	private View mView;

	public static WelcomeFragment newInstance() {
		WelcomeFragment fragment = new WelcomeFragment();
		return fragment;
	}

	public WelcomeFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.fragment_welcome, container, false);
		
		mView.findViewById(R.id.btn_authorize).setOnClickListener(this);
		mView.findViewById(R.id.btn_how_it_works).setOnClickListener(this);
		mView.findViewById(R.id.btn_about).setOnClickListener(this);
		return mView;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (PhoneToDesktopAuthorization) activity;
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
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_authorize:
			mListener.startAuthorization();
			break;
		case R.id.btn_how_it_works:
			startActivity(new Intent(getActivity(), TutorialActivity.class));
			break;
		case R.id.btn_about:
			startActivity(new Intent(getActivity(), AboutActivity.class));
			break;
		default:
			break;
		}
	}

}
