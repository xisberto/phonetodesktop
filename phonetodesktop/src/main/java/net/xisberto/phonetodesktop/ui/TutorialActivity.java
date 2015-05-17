/*******************************************************************************
 * Copyright (c) 2012 Humberto Fraga <xisberto@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Humberto Fraga <xisberto@gmail.com> - initial API and implementation
 ******************************************************************************/
package net.xisberto.phonetodesktop.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.xisberto.phonetodesktop.BuildConfig;
import net.xisberto.phonetodesktop.R;
import net.xisberto.phonetodesktop.Utils;

public class TutorialActivity extends AppCompatActivity implements OnClickListener, OnPageChangeListener {
	private final int
		total_slides = 7;
	private TutorialPageAdapter page_adapter;
	private ViewPager view_pager;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
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
	
	public static class TutorialFragment extends Fragment {
		int page_number;
		private View v;
		
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
			v = inflater.inflate(R.layout.layout_slide, container, false);
			int res_string = Utils.getResId(R.string.class, "txt_tutorial_" + page_number);
			int res_image = Utils.getResId(R.drawable.class, "tutorial_"+page_number);
			((TextView) v.findViewById(R.id.slide_text)).setText(res_string);
			((ImageView) v.findViewById(R.id.slide_image)).setImageResource(res_image);
			return v;
		}
	}
	

}
