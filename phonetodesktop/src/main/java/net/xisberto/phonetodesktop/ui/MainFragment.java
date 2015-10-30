package net.xisberto.phonetodesktop.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.xisberto.phonetodesktop.Preferences;
import net.xisberto.phonetodesktop.R;
import net.xisberto.phonetodesktop.Utils;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass. Activities that
 * contain this fragment must implement the
 * {@link MainFragment.PhoneToDesktopAuthorization} interface to handle
 * interaction events. Use the {@link MainFragment#newInstance} factory method
 * to create an instance of this fragment.
 */
public class MainFragment extends Fragment implements OnClickListener {

    private View mView;

    private PhoneToDesktopAuthorization mListener;

    private boolean mUpdating;

    public static MainFragment newInstance() {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        fragment.setUpdating(false);
        return fragment;
    }

    public MainFragment() {
        // Required empty public constructor
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_main, container, false);

        mView.findViewById(R.id.btn_authorize).setOnClickListener(this);
        mView.findViewById(R.id.btn_link_list).setOnClickListener(this);
        mView.findViewById(R.id.btn_wait_list).setOnClickListener(this);
        mView.findViewById(R.id.btn_how_it_works).setOnClickListener(this);
        mView.findViewById(R.id.btn_preferences).setOnClickListener(this);
        mView.findViewById(R.id.btn_about).setOnClickListener(this);

        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateMainLayout();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_how_it_works:
                startActivity(new Intent(getActivity(), TutorialActivity.class));
                break;
            case R.id.btn_about:
                startActivity(new Intent(getActivity(), AboutActivity.class));
                break;
            case R.id.btn_link_list:
                startActivity(new Intent(getActivity(), LinkListActivity.class));
                break;
            case R.id.btn_wait_list:
                startActivity(new Intent(getActivity(), WaitListActivity.class));
                break;
            case R.id.btn_preferences:
                startActivity(new Intent(getActivity(), PreferencesActivity.class));
                break;
            case R.id.btn_authorize:
                setUpdating(true);
                updateMainLayout();
                mListener.startAuthorization();
        }
    }

    public void setUpdating(boolean updating) {
        Utils.log("changing updating to " + updating);
        mUpdating = updating;
    }

    public void updateMainLayout() {
        Utils.log("fragment updating " + mUpdating);
        Button btn_authorize = (Button) mView.findViewById(R.id.btn_authorize);
        TextView txt_authorize = (TextView) mView.findViewById(R.id.txt_authorize);

        getActivity().setProgressBarIndeterminateVisibility(mUpdating);
        btn_authorize.setEnabled(!mUpdating);

        if (mUpdating) {
            txt_authorize.setText(R.string.txt_waiting_authorization);
        } else {
            Preferences prefs = Preferences.getInstance(getActivity());
            String account_name = prefs.loadAccountName();
            if (account_name != null) {
                txt_authorize.setText(getString(R.string.txt_authorized_to, account_name));
                btn_authorize.setText(R.string.btn_authorize_other);
//				if (BuildConfig.DEBUG) {
//                    getActivity().getActionBar().setSubtitle(
//							"List: " + prefs.loadListId());
//				}
            } else {
                txt_authorize.setText(R.string.txt_authorize);
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated to
     * the activity and potentially other fragments contained in that activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface PhoneToDesktopAuthorization {
        void startAuthorization();
    }

}
