/**
 * Wire
 * Copyright (C) 2018 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.controllers.globallayout;

import android.app.Activity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import com.waz.zclient.controllers.navigation.Page;
import com.waz.zclient.utils.ViewUtils;
import com.waz.zclient.utils.keyboard.KeyboardVisibilityListener;

import java.util.HashSet;
import java.util.Set;

public class GlobalLayoutController implements IGlobalLayoutController {
    public static final String TAG = GlobalLayoutController.class.getName();

    protected Set<KeyboardVisibilityObserver> keyboardVisibilityObservers = new HashSet<>();
    protected Set<KeyboardHeightObserver> keyboardHeightObservers = new HashSet<>();

    private View globalLayout;
    private Activity activity;

    private final ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            keyboardListener.onLayoutChange();
        }
    };

    private KeyboardVisibilityListener keyboardListener;

    private KeyboardVisibilityListener.Callback keyboardCallback = new KeyboardVisibilityListener.Callback() {
        @Override
        public void onKeyboardChanged(boolean keyboardIsVisible, int keyboardHeight) {
            notifyKeyboardVisibilityHasChanged(keyboardIsVisible, keyboardHeight);
        }

        @Override
        public void onKeyboardHeightChanged(int keyboardHeight) {
            notifyKeyboardHeightHasChanged(keyboardHeight);
        }
    };

    public GlobalLayoutController() {
    }

    @Override
    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void setGlobalLayout(View view) {
        if (globalLayout != null) {
            globalLayout.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
            keyboardListener.setCallback(null);
            keyboardListener = null;
        }
        globalLayout = view;
        globalLayout.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);

        // Listen to layout changes to determine when keyboard becomes visible / hidden
        keyboardListener = new KeyboardVisibilityListener(view);
        keyboardListener.setCallback(keyboardCallback);
    }

    @Override
    public void keepScreenAwake() {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void resetScreenAwakeState() {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void tearDown() {
        if (globalLayout != null) {
            this.globalLayout.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
            globalLayout = null;
        }
        activity = null;
        keyboardListener = null;
    }

    @Override
    public void addKeyboardVisibilityObserver(KeyboardVisibilityObserver keyboardVisibilityObserver) {
        keyboardVisibilityObservers.add(keyboardVisibilityObserver);
    }

    @Override
    public void removeKeyboardVisibilityObserver(KeyboardVisibilityObserver keyboardVisibilityObserver) {
        keyboardVisibilityObservers.remove(keyboardVisibilityObserver);
    }

    protected void notifyKeyboardVisibilityHasChanged(boolean keyboardIsVisible, int keyboardHeight) {
        for (KeyboardVisibilityObserver keyboardVisibilityObserver : keyboardVisibilityObservers) {
            keyboardVisibilityObserver.onKeyboardVisibilityChanged(keyboardIsVisible, keyboardHeight, activity.getCurrentFocus());
        }
    }

    @Override
    public void addKeyboardHeightObserver(KeyboardHeightObserver keyboardHeightObserver) {
        keyboardHeightObservers.add(keyboardHeightObserver);
    }

    @Override
    public void removeKeyboardHeightObserver(KeyboardHeightObserver keyboardHeightObserver) {
        keyboardHeightObservers.remove(keyboardHeightObserver);
    }

    @Override
    public void setSoftInputModeForPage(Page page) {
        if (activity == null ||
            activity.getWindow() == null) {
            return;
        }

        ViewUtils.setSoftInputMode(activity.getWindow(),
                                   getSoftInputModeForPage(page),
                                   TAG);
    }

    @Override
    public int getSoftInputModeForPage(Page page) {
        int softInputMode;
        switch (page) {
            case PHONE_REGISTRATION_ADD_NAME:
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
                break;
            case PICK_USER:
            case COLLECTION:
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
                break;
            default:
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

        }
        return softInputMode;
    }

    @Override
    public boolean isKeyboardVisible() {
        if (keyboardListener == null) {
            return false;
        }
        return keyboardListener.getKeyboardHeight() > 0;
    }

    protected void notifyKeyboardHeightHasChanged(int keyboardHeight) {
        for (KeyboardHeightObserver keyboardHeightObserver : keyboardHeightObservers) {
            keyboardHeightObserver.onKeyboardHeightChanged(keyboardHeight);
        }
    }
}
