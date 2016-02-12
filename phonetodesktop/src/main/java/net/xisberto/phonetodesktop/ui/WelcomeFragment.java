package net.xisberto.phonetodesktop.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import net.xisberto.phonetodesktop.R;
import net.xisberto.phonetodesktop.ui.MainFragment.PhoneToDesktopAuthorization;

public class WelcomeFragment extends Fragment implements OnClickListener {

    private PhoneToDesktopAuthorization mListener;

    public WelcomeFragment() {
        // Required empty public constructor
    }

    public static WelcomeFragment newInstance() {
        return new WelcomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome, container, false);

        view.findViewById(R.id.btn_authorize).setOnClickListener(this);
        view.findViewById(R.id.btn_how_it_works).setOnClickListener(this);
        view.findViewById(R.id.btn_about).setOnClickListener(this);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (PhoneToDesktopAuthorization) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
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
