/*******************************************************************************
 * Copyright (c) 2013 Humberto Fraga <xisberto@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * <p/>
 * Contributors:
 * Humberto Fraga <xisberto@gmail.com> - initial API and implementation
 ******************************************************************************/
package net.xisberto.phonetodesktop;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.TasksScopes;

import net.xisberto.phonetodesktop.ui.MainActivity;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static final String
            ACTION_AUTHENTICATE = "net.xisberto.phonetodesktop.action.AUTHENTICATE";
    public static final String ACTION_LIST_LOCAL_TASKS = "net.xisberto.phonetodesktop.action.LIST_LOCAL_TASKS";
    public static final String EXTRA_UPDATING = "net.xisberto.phonetodesktop.extra.UPDATING";
    public static final String EXTRA_TITLES = "net.xisberto.phonetodesktop.extra.TITLES";
    public static final String LIST_TITLE = "PhoneToDesktop";
    //	"\\b((?:https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])",
    public static final Pattern urlPattern = Pattern
            .compile(
                    "(https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_()|]*",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
                            | Pattern.DOTALL);
    public static Collection<String> SCOPES = Collections.singleton(TasksScopes.TASKS);

    public static void log(String message) {
        if (BuildConfig.DEBUG) {
            Log.d(LIST_TITLE, message);
        }
    }

    public static int getResId(Class<?> c, String resourceName) {
        try {
            Field idField = c.getDeclaredField(resourceName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static String appendInBrackets(String text, String[] parts) {
        int index = 0;
        Matcher matcher = Utils.urlPattern.matcher(text);
        while (matcher.find()) {
            if (!matcher.group().equals(parts[index])) {
                // don't replace when we have the URL and not a title
                text = text.replace(matcher.group(),
                        matcher.group() + " [" + parts[index] + "]");
            }
            index++;
        }
        return text;
    }

    public static String replace(String text, String[] parts) {
        int index = 0;
        Matcher matcher = Utils.urlPattern.matcher(text);
        while (matcher.find()) {
            text = text.replace(matcher.group(), parts[index]);
            index++;
        }
        return text;
    }


    /**
     * Filter the URLs in {@code text} and return them separated by spaces
     *
     * @param text the text to search the URLs in
     * @return the found URLs separated by space
     */
    public static String filterLinks(String text) {
        String result = "";
        Matcher matcher = Utils.urlPattern.matcher(text);
        while (matcher.find()) {
            result += matcher.group() + " ";
        }

        return result;
    }

    @WorkerThread
    public static Tasks getGoogleTasksClient(Context context) throws GoogleAuthException, IOException {
        Preferences preferences = Preferences.getInstance(context);

        String token;
        String scope = "oauth2:" + TasksScopes.TASKS;
        Account account = new Account(preferences.loadAccountName(), "com.google");
        token = GoogleAuthUtil.getToken(context, account, scope);
        GoogleCredential credential = new GoogleCredential();
        credential.setAccessToken(token);

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        return new Tasks.Builder(transport, jsonFactory, credential)
                .setApplicationName("PhoneToDesktop")
                .build();
    }

    public static void startAuthentication(Context context) {
        Intent intentAuth = new Intent(context, MainActivity.class)
                .setAction(Utils.ACTION_AUTHENTICATE)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intentAuth);
    }
}
