package net.xisberto.phonetodesktop;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class TutorialActivity extends SherlockFragmentActivity implements OnClickListener, OnPageChangeListener {
	private final int
		total_slides = 6;
	private TutorialPageAdapter page_adapter;
	private ViewPager view_pager;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.layout_presentation);
		findViewById(R.id.button_back).setOnClickListener(this);
		findViewById(R.id.button_next).setOnClickListener(this);
		
		page_adapter = new TutorialPageAdapter(getSupportFragmentManager());
		view_pager = (ViewPager) findViewById(R.id.view_pager);
		view_pager.setAdapter(page_adapter);
		view_pager.setOnPageChangeListener(this);
		onPageSelected(view_pager.getCurrentItem());
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_back:
			view_pager.setCurrentItem(view_pager.getCurrentItem()-1);
			break;
		case R.id.button_next:
			view_pager.setCurrentItem(view_pager.getCurrentItem()+1);
			break;
		}
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {}

	@Override
	public void onPageSelected(int position) {
		Button button_next = (Button) findViewById(R.id.button_next);
		switch (position) {
		case 0:
			findViewById(R.id.button_back).setVisibility(View.INVISIBLE);
			break;
		case total_slides-1:
			//At the last slide, the "next" button will make the activity finish
			button_next.setText(android.R.string.ok);
			button_next.setCompoundDrawables(null, null, null, null);
			button_next.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					TutorialActivity.this.finish();
				}
			});
			break;
		default:
			findViewById(R.id.button_back).setVisibility(View.VISIBLE);
			button_next.setText(R.string.btn_next);
			Drawable next_icon = getResources().getDrawable(R.drawable.next_icon);
			button_next.setCompoundDrawablesWithIntrinsicBounds(null, null, next_icon, null);
			button_next.setOnClickListener(this);
			break;
		}
	}
	
	public class TutorialPageAdapter extends FragmentPagerAdapter {

		public TutorialPageAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return TutorialFragment.newInstace(position);
		}

		@Override
		public int getCount() {
			return total_slides;
		}
		
	}
	
	public static class TutorialFragment extends SherlockFragment {
		int page_number;
		
		public static TutorialFragment newInstace(int page_number) {
			TutorialFragment fragment = new TutorialFragment();
			Bundle args = new Bundle();
			args.putInt("page_number", page_number);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			try {
				page_number = getArguments().getInt("page_number");
			} catch (NullPointerException e) {
				page_number = 0;
			} 
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View v;
			v = inflater.inflate(R.layout.layout_slide_1, container, false);
			TextView text = (TextView) v.findViewById(R.id.slide_text);
			switch (page_number) {
			case 0:
				text.setText(R.string.txt_tutorial1);
				text.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.tutorial, 0, 0);
				break;
			case 1:
				text.setText(R.string.txt_tutorial2);
				text.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.browser_share, 0, 0);
				break;
			case 2:
				text.setText(R.string.txt_tutorial3);
				text.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.share_list, 0, 0);
				break;
			case 3:
				text.setText(R.string.txt_tutorial4);
				text.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.notification, 0, 0);
				break;
			case 4:
				text.setText(R.string.txt_tutorial5);
				text.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.gmail_select_tasks, 0, 0);
				break;
			case 5:
				text.setText(R.string.txt_tutorial6);
				text.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.gmail_select_list, 0, 0);
				break;
			default:
				text.setText(R.string.txt_tutorial1);
				text.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.tutorial, 0, 0);
				break;
			};
			return v;
		}
	}
	

}
