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
package net.xisberto.phonetodesktop.network;

import net.xisberto.phonetodesktop.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLOptions {
    private static final Pattern TITLE_TAG = Pattern.compile(
            "\\<title>(.*?)\\</title>", Pattern.CASE_INSENSITIVE
                    | Pattern.DOTALL);

    private boolean isCancelled = false;

    /**
     * @param url the HTML page
     * @return title text (null if document isn't HTML or lacks a title tag)
     * @throws IOException
     */
    public static String getPageTitle(String url) throws IOException {
        URL u = new URL(url);
        URLConnection conn = u.openConnection();

        // ContentType is an inner class defined below
        ContentType contentType = getContentTypeHeader(conn);
        if (contentType != null && !contentType.contentType.equals("text/html"))
            return null; // don't continue if not HTML
        else {
            // determine the charset, or use the default
            Charset charset = getCharset(contentType);
            if (charset == null)
                charset = Charset.defaultCharset();
            Utils.log("charset is " + charset.displayName());

            // read the response body, using BufferedReader for performance
            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
            int n;
            int totalRead = 0;
            char[] buf = new char[1024];
            StringBuilder content = new StringBuilder();

            // read until EOF or first 8192 characters
            while (totalRead < 8192 && (n = reader.read(buf, 0, buf.length)) != -1) {
                content.append(buf, 0, n);
                totalRead += n;
            }
            reader.close();

            // extract the title
            Matcher matcher = TITLE_TAG.matcher(content);
            if (matcher.find()) {
                /* replace any occurrences of whitespace (which may
                 * include line feeds and other uglies) as well
                 * as HTML brackets with a space */
                return matcher.group(1).replaceAll("[\\s\\<>]+", " ").trim();
            } else
                return null;
        }
    }

    /**
     * Loops through response headers until Content-Type is found.
     *
     * @param conn the connection to be read.
     * @return ContentType object representing the value of
     * the Content-Type header
     */
    private static ContentType getContentTypeHeader(URLConnection conn) {
        int i = 0;
        boolean moreHeaders;
        do {
            String headerName = conn.getHeaderFieldKey(i);
            String headerValue = conn.getHeaderField(i);
            if (headerName != null && headerName.equals("Content-Type")) {
                return new ContentType(headerValue);
            }

            i++;
            moreHeaders = headerName != null || headerValue != null;
        }
        while (moreHeaders);

        return null;
    }

    private static Charset getCharset(ContentType contentType) {
        if (contentType != null && contentType.charsetName != null && Charset.isSupported(contentType.charsetName))
            return Charset.forName(contentType.charsetName);
        else
            return null;
    }

    protected void cancel() {
        isCancelled = true;
    }

    protected String[] unshorten(String... params) throws IOException {
        String[] result = params.clone();
        for (int i = 0; i < params.length; i++) {
            Utils.log("unshorten " + params[i]);

            URLConnection connection = new URL(params[i]).openConnection();
            connection.connect();
            InputStream instr = connection.getInputStream();
            instr.close();

            if (isCancelled) {
                return result;
            }

            result[i] = connection.getURL().toString();
            Utils.log("got " + result[i]);
        }
        return result;
    }

    protected String[] getTitles(String... params) throws IOException,
            NullPointerException {
        String[] result = params.clone();
        for (int i = 0; i < params.length; i++) {
            Utils.log("getTitles " + params[i]);

            String title = getPageTitle(params[i]);
            if (title != null) {
                Utils.log("Found title " + title);
                result[i] = title;
            } else {
                result[i] = params[i];
            }

            if (isCancelled) {
                return result;
            }
        }
        return result;
    }

    /**
     * Class holds the content type and charset (if present)
     */
    private static final class ContentType {
        private static final Pattern CHARSET_HEADER = Pattern.compile("charset=([-_a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        private String contentType;
        private String charsetName;

        private ContentType(String headerValue) {
            if (headerValue == null)
                throw new IllegalArgumentException("ContentType must be constructed with a not-null headerValue");
            int n = headerValue.indexOf(";");
            if (n != -1) {
                contentType = headerValue.substring(0, n);
                Matcher matcher = CHARSET_HEADER.matcher(headerValue);
                if (matcher.find())
                    charsetName = matcher.group(1);
            } else
                contentType = headerValue;
        }
    }
}
