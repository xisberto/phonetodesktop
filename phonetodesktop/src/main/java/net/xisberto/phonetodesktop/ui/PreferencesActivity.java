/*******************************************************************************
 * Copyright (c) 2013 Humberto Fraga <xisberto@gmail.com>.
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
import android.preference.PreferenceActivity;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import net.xisberto.phonetodesktop.R;

public class PreferencesActivity extends PreferenceActivity {

    private Toolbar mToolBar;

    @SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setupToolbar();
		
		addPreferencesFromResource(R.xml.preferences);
	}

    private void setupToolbar() {
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        View content = root.getChildAt(0);
        LinearLayout toolbarContainer = (LinearLayout) View.inflate(this, R.layout.activity_prefs, null);
        root.removeAllViews();
        toolbarContainer.addView(content);
        root.addView(toolbarContainer);
        mToolBar = (Toolbar) toolbarContainer.findViewById(R.id.toolbar);
        mToolBar.setTitle(getTitle());
        mToolBar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(PreferencesActivity.this);
            }
        });
    }
	
}
