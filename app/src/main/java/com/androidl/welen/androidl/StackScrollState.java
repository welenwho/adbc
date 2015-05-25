/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.androidl.welen.androidl;

import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * 这个类保存列表item的状态
 */
public class StackScrollState {

    private static final String CHILD_NOT_FOUND_TAG = "StackScrollStateNoSuchChild";

    private final ViewGroup mHostView;
    private Map<NotificationView, ViewState> mStateMap;
    private final Rect mClipRect = new Rect();

    public StackScrollState(ViewGroup hostView) {
        mHostView = hostView;
        mStateMap = new HashMap<NotificationView, ViewState>();
    }

    public ViewGroup getHostView() {
        return mHostView;
    }

    public void resetViewStates() {
        int numChildren = mHostView.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            NotificationView child = (NotificationView) mHostView.getChildAt(i);
            ViewState viewState = mStateMap.get(child);
            if (viewState == null) {
                viewState = new ViewState();
                mStateMap.put(child, viewState);
            }
            // initialize with the default values of the view
            viewState.height = child.getHeight();
            viewState.gone = child.getVisibility() == View.GONE;
            viewState.alpha = 1;
            viewState.scale = 1f;
            viewState.notGoneIndex = -1;
        }
    }

    public ViewState getViewStateForView(View requestedView) {
        return mStateMap.get(requestedView);
    }

    public void removeViewStateForView(View child) {
        mStateMap.remove(child);
    }

    /**
     * Apply the properties saved in {@link #mStateMap} to the children of the {@link #mHostView}.
     * The properties are only applied if they effectively changed.
     */
    public void apply() {
        int numChildren = mHostView.getChildCount();
        for (int i = 0; i < numChildren; i++) {
            NotificationView child = (NotificationView) mHostView.getChildAt(i);
            ViewState state = mStateMap.get(child);
            if (state == null) {
                Log.wtf(CHILD_NOT_FOUND_TAG, "No child state was found when applying this state " +
                        "to the hostView");
                continue;
            }
            if (!state.gone) {
                float alpha = child.getAlpha();
                float yTranslation = child.getTranslationY();
                float xTranslation = child.getTranslationX();
//                float zTranslation = child.getTranslationZ();
                float scale = child.getScaleX();
                int height = child.getHeight();
                float newAlpha = state.alpha;
                float newYTranslation = state.yTranslation;
                float newScale = state.scale;
                int newHeight = state.height;
                boolean becomesInvisible = newAlpha == 0.0f;
                if (alpha != newAlpha && xTranslation == 0) {
                    // apply layer type
                    boolean becomesFullyVisible = newAlpha == 1.0f;
                    boolean newLayerTypeIsHardware = !becomesInvisible && !becomesFullyVisible;//newAlpha not 0 and 1,set hardware
                    int layerType = child.getLayerType();
                    int newLayerType = newLayerTypeIsHardware
                            ? View.LAYER_TYPE_HARDWARE
                            : View.LAYER_TYPE_NONE;
                    if (layerType != newLayerType) {
                        child.setLayerType(newLayerType, null);
                    }

                    // apply alpha
                    if (!becomesInvisible) {//if newalpha is 0,setvisibility invisibility
                        child.setAlpha(newAlpha);
                    }
                }

                // apply visibility
                int oldVisibility = child.getVisibility();
                int newVisibility = becomesInvisible ? View.INVISIBLE : View.VISIBLE;
                if (newVisibility != oldVisibility) {
                    child.setVisibility(newVisibility);
                }

                // apply yTranslation
                if (yTranslation != newYTranslation) {
                    child.setTranslationY(newYTranslation);
                }

                // apply scale
                if (scale != newScale) {
                    child.setScaleX(newScale);
                    child.setScaleY(newScale);
                }


            }
        }
    }


    public static class ViewState {

        float alpha;
        float yTranslation;
        int height;
        boolean gone;
        float scale;
        int notGoneIndex;
    }
}
