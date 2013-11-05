/*
    Android Asynchronous Http Client
    Copyright (c) 2011 James Smith <james@loopj.com>
    http://loopj.com

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.loopj.android.http;

import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HttpStatus;

public abstract class BaseJsonHttpResponseHandler<JSON_TYPE> extends TextHttpResponseHandler {
    private static final String LOG_TAG = "BaseJsonHttpResponseHandler";

    /**
     * Creates a new JsonHttpResponseHandler
     */

    public BaseJsonHttpResponseHandler() {
        this(DEFAULT_CHARSET);
    }

    public BaseJsonHttpResponseHandler(String encoding) {
        super(encoding);
    }

    public abstract void onSuccess(int statusCode, Header[] headers, String rawResponse, JSON_TYPE response);

    public abstract void onFailure(int statusCode, Header[] headers, Throwable e, String rawData, JSON_TYPE errorResponse);

    @Override
    public final void onSuccess(final int statusCode, final Header[] headers, final String responseBody) {
        if (statusCode != HttpStatus.SC_NO_CONTENT) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final JSON_TYPE jsonResponse = parseResponse(responseBody);
                        postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                onSuccess(statusCode, headers, responseBody, jsonResponse);
                            }
                        });
                    } catch (final Throwable t) {
                        Log.d(LOG_TAG, "parseResponse thrown an problem", t);
                        postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                onFailure(statusCode, headers, t, responseBody, null);
                            }
                        });
                    }
                }
            }).start();
        } else {
            onSuccess(statusCode, headers, null, null);
        }
    }

    @Override
    public final void onFailure(final int statusCode, final Header[] headers, final String responseBody, final Throwable e) {
        if (responseBody != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final JSON_TYPE jsonResponse = parseResponse(responseBody);
                        postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                onFailure(statusCode, headers, e, responseBody, jsonResponse);
                            }
                        });
                    } catch (Throwable t) {
                        Log.d(LOG_TAG, "parseResponse thrown an problem", t);
                        postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                onFailure(statusCode, headers, e, responseBody, null);
                            }
                        });
                    }
                }
            }).start();
        } else {
            onFailure(statusCode, headers, e, null, null);
        }
    }

    protected abstract JSON_TYPE parseResponse(String responseBody) throws Throwable;
}