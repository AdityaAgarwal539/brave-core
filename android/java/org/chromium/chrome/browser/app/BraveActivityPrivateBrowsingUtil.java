/* Copyright (c) 2024 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.app;

import android.content.Intent;

import org.chromium.base.BravePreferenceKeys;
import org.chromium.base.Log;
import org.chromium.chrome.browser.preferences.ChromeSharedPreferences;
import org.chromium.chrome.browser.tabmodel.TabCreator;
import org.chromium.chrome.browser.tabmodel.TabModel;
import org.chromium.components.embedder_support.util.UrlConstants;

/**
 * Utility class to handle enforcement of Private Browsing Only mode when app starts.
 * This ensures that when the user has enabled "Private Browsing Only" mode in settings,
 * the app always starts in private browsing mode.
 */
public class BraveActivityPrivateBrowsingUtil {
    private static final String TAG = "PrivateBrowsingUtil";

    /**
     * Checks if Private Browsing Only mode is enabled.
     * @return true if the user has enabled the Private Browsing Only setting
     */
    public static boolean isPrivateBrowsingOnlyEnabled() {
        return ChromeSharedPreferences.getInstance()
                .readBoolean(BravePreferenceKeys.BRAVE_PRIVATE_BROWSING_ONLY, false);
    }

    /**
     * Enforces private browsing mode by clearing regular tabs and creating a private tab.
     * This should be called during app startup when Private Browsing Only mode is enabled.
     * 
     * @param tabModel the regular (non-private) TabModel
     * @param tabCreator the TabCreator used to create new tabs
     * @param privateModeTabModel the private TabModel
     */
    public static void enforcePrivateBrowsingMode(
            TabModel tabModel,
            TabCreator tabCreator,
            TabModel privateModeTabModel) {
        
        if (!isPrivateBrowsingOnlyEnabled()) {
            return;
        }

        try {
            // Close all regular (non-private) tabs
            int tabCount = tabModel.getCount();
            for (int i = tabCount - 1; i >= 0; i--) {
                tabModel.closeTabAt(i, false);
            }

            // If there are no private tabs, create one with the home/NTP URL
            if (privateModeTabModel.getCount() == 0) {
                tabCreator.launchUrl(
                        UrlConstants.NTP_URL,
                        org.chromium.chrome.browser.tab.TabLaunchType.FROM_CHROME_UI);
            }

            Log.i(TAG, "Private Browsing Only mode enforced: Regular tabs cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error enforcing Private Browsing Only mode: " + e.getMessage());
        }
    }

    /**
     * Handles intent when Private Browsing Only mode is enabled.
     * Routes intents that would normally open in regular mode to private mode instead.
     * 
     * @param intent the intent to handle
     * @param tabCreator the TabCreator for private mode
     * @return true if the intent was handled in private mode, false otherwise
     */
    public static boolean maybeHandleIntentInPrivateBrowsingMode(
            Intent intent,
            TabCreator tabCreator) {
        
        if (!isPrivateBrowsingOnlyEnabled() || intent == null) {
            return false;
        }

        try {
            String action = intent.getAction();
            String url = null;

            // Extract URL from various intent types
            if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
                url = intent.getData().toString();
            } else if (Intent.ACTION_WEB_SEARCH.equals(action)) {
                // Handle web search intents
                return false; // Let the normal flow handle this
            }

            // If we have a URL, open it in private mode
            if (url != null && !url.isEmpty()) {
                tabCreator.launchUrl(
                        url,
                        org.chromium.chrome.browser.tab.TabLaunchType.FROM_EXTERNAL_APP);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling intent in Private Browsing Only mode: " + e.getMessage());
        }

        return false;
    }
}
