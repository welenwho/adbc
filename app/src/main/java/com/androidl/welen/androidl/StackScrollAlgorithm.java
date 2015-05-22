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

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * The Algorithm of the {@link com.android.systemui.statusbar.stack
 * .NotificationStackScrollLayout} which can be queried for {@link com.android.systemui.statusbar
 * .stack.StackScrollState}
 */
public class StackScrollAlgorithm {

    private static final String LOG_TAG = "StackScrollAlgorithm";

    private int mPaddingBetweenElements;
    private int mRoundedRectCornerRadius;

    private int mLayoutHeight;

    private boolean mRevertLayout;
    private int mTopPadding;
    private StackScrollAlgorithmState mTempAlgorithmState = new StackScrollAlgorithmState();

    public StackScrollAlgorithm(Context context) {
        initConstants(context);
    }


    private void initConstants(Context context) {
        mPaddingBetweenElements = context.getResources()
                .getDimensionPixelSize(R.dimen.notification_padding);
        mRoundedRectCornerRadius = context.getResources().getDimensionPixelSize(
                R.dimen.notification_material_rounded_rect_radius);
        mRevertLayout = true;
    }

    public void getStackScrollState(AmbientState ambientState, StackScrollState resultState) {
        // The state of the local variables are saved in an algorithmState to easily subdivide it
        // into multiple phases.
        StackScrollAlgorithmState algorithmState = mTempAlgorithmState;

        // First we reset the view states to their default values.
        resultState.resetViewStates();

        float bottomOverScroll = ambientState.getOverScrollAmount(false /* onTop */);
        float topOverScroll = ambientState.getOverScrollAmount(true);

        int scrollY = ambientState.getScrollY();

        // Due to the overScroller, the stackscroller can have negative scroll state. This is
        // already accounted for by the top padding and doesn't need an additional adaption
        scrollY = Math.max(0, scrollY);
        algorithmState.scrollY = (int) (scrollY + bottomOverScroll-topOverScroll);

        updateVisibleChildren(resultState, algorithmState);

        updatePositionsForState(resultState, algorithmState);

        handleDraggedViews(ambientState, resultState, algorithmState);
        updateClipping(resultState, algorithmState);
    }


    private void updateClipping(StackScrollState resultState,
            StackScrollAlgorithmState algorithmState) {
        float previousNotificationEnd = 0;
        float previousNotificationStart = 0;
        boolean previousNotificationIsSwiped = false;
        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            NotificationView child = algorithmState.visibleChildren.get(i);
            StackScrollState.ViewState state = resultState.getViewStateForView(child);
            float newYTranslation = state.yTranslation + state.height * (1f - state.scale) / 2f;
            float newHeight = state.height * state.scale;
            // apply clipping and shadow
            float newNotificationEnd = newYTranslation + newHeight;

            // In the unlocked shade we have to clip a little bit higher because of the rounded
            // corners of the notifications.
            float clippingCorrection = mRoundedRectCornerRadius * state.scale;

            // When the previous notification is swiped, we don't clip the content to the
            // bottom of it.
            float clipHeight = previousNotificationIsSwiped
                    ? newHeight
                    : newNotificationEnd - (previousNotificationEnd - clippingCorrection);

            updateChildClippingAndBackground(state, newHeight, clipHeight,
                    newHeight - (previousNotificationStart - newYTranslation));

            if (!child.isTransparent()) {
                // Only update the previous values if we are not transparent,
                // otherwise we would clip to a transparent view.
                previousNotificationStart = newYTranslation + state.clipTopAmount * state.scale;
                previousNotificationEnd = newNotificationEnd;
                previousNotificationIsSwiped = child.getTranslationX() != 0;
            }
        }
    }

    /**
     * Updates the shadow outline and the clipping for a view.
     *
     * @param state the viewState to update
     * @param realHeight the currently applied height of the view
     * @param clipHeight the desired clip height, the rest of the view will be clipped from the top
     * @param backgroundHeight the desired background height. The shadows of the view will be
     *                         based on this height and the content will be clipped from the top
     */
    private void updateChildClippingAndBackground(StackScrollState.ViewState state,
            float realHeight, float clipHeight, float backgroundHeight) {
        if (realHeight > clipHeight) {
            // Rather overlap than create a hole.
            state.topOverLap = (int) Math.floor((realHeight - clipHeight) / state.scale);
        } else {
            state.topOverLap = 0;
        }
        if (realHeight > backgroundHeight) {
            // Rather overlap than create a hole.
            state.clipTopAmount = (int) Math.floor((realHeight - backgroundHeight) / state.scale);
        } else {
            state.clipTopAmount = 0;
        }
    }

    /**
     * Handle the special state when views are being dragged
     */
    private void handleDraggedViews(AmbientState ambientState, StackScrollState resultState,
            StackScrollAlgorithmState algorithmState) {
        ArrayList<View> draggedViews = ambientState.getDraggedViews();
        if(draggedViews.isEmpty())return;
        int size = algorithmState.visibleChildren.size();
        //TODO 当前不可见的应该跳过设置,否则view太多,效率就低了
        for(View view : algorithmState.visibleChildren){
            StackScrollState.ViewState viewState = resultState.getViewStateForView(
                    view);
            if(!draggedViews.contains(view)){
                // The child below the dragged one must be fully visible
                viewState.alpha = 0.6f;
            }else {
                viewState.alpha = view.getAlpha();
            }
        }

       /* for (View draggedView : draggedViews) {
            int childIndex = algorithmState.visibleChildren.indexOf(draggedView);
            if (childIndex >= 0 && childIndex < algorithmState.visibleChildren.size() - 1) {
                View nextChild = algorithmState.visibleChildren.get(childIndex + 1);
                if (!draggedViews.contains(nextChild)) {
                    // only if the view is not dragged itself we modify its state to be fully
                    // visible
                    StackScrollState.ViewState viewState = resultState.getViewStateForView(
                            nextChild);
                    // The child below the dragged one must be fully visible
                    viewState.alpha = 0.6f;
                }

                // Lets set the alpha to the one it currently has, as its currently being dragged
                StackScrollState.ViewState viewState = resultState.getViewStateForView(draggedView);
                // The dragged child should keep the set alpha
                viewState.alpha = draggedView.getAlpha();
            }
        }*/
    }

    /**
     * Update the visible children on the state.
     */
    private void updateVisibleChildren(StackScrollState resultState,
            StackScrollAlgorithmState state) {
        ViewGroup hostView = resultState.getHostView();
        int childCount = hostView.getChildCount();
        state.visibleChildren.clear();
        state.visibleChildren.ensureCapacity(childCount);
        for (int i = 0; i < childCount; i++) {
            NotificationView v = (NotificationView) hostView.getChildAt(i);
            if (v.getVisibility() != View.GONE) {
                StackScrollState.ViewState viewState = resultState.getViewStateForView(v);
                viewState.notGoneIndex = state.visibleChildren.size();
                state.visibleChildren.add(v);
            }
        }
    }

    /**
     * Determine the positions for the views. This is the main part of the algorithm.
     *
     * @param resultState The result state to update if a change to the properties of a child occurs
     * @param algorithmState The state in which the current pass of the algorithm is currently in
     */
    private void updatePositionsForState(StackScrollState resultState,
            StackScrollAlgorithmState algorithmState) {
        // The y coordinate of the current child.
        float currentYPosition = 0.0f;

        // How far in is the element currently transitioning into the bottom stack.
        float yPositionInScrollView = 0.0f;

        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            NotificationView child = algorithmState.visibleChildren.get(i);
            StackScrollState.ViewState childViewState = resultState.getViewStateForView(child);
            int childHeight = getChildHeight(child);
            //当前这个element的下面element的位置
            float yPositionInScrollViewAfterElement = yPositionInScrollView
                    + childHeight
                    + mPaddingBetweenElements;

            childViewState.yTranslation = currentYPosition;

            if (i == 0) {//之所以第一个特殊处理,是因为所有的item都是以第一个为参考,第一个的translation处理了滑动,后面的item都会跟着他
                childViewState.yTranslation = - algorithmState.scrollY;
                Log.i("welen","y="+childViewState.yTranslation +" alpha="+childViewState.alpha);
            }

            currentYPosition = childViewState.yTranslation + childHeight + mPaddingBetweenElements;
            yPositionInScrollView = yPositionInScrollViewAfterElement;

            childViewState.yTranslation += mTopPadding;
        }
    }

    private int getChildHeight(NotificationView child) {
        return child.getActualHeight();
    }

    public void setLayoutHeight(int layoutHeight) {
        this.mLayoutHeight = layoutHeight;
    }

    public void setTopPadding(int topPadding) {
        mTopPadding = topPadding;
    }


    public void onReset(NotificationView view) {

    }

    class StackScrollAlgorithmState {

        /**
         * The scroll position of the algorithm
         */
        public int scrollY;

        /**
         * The children from the host view which are not gone.
         */
        public final ArrayList<NotificationView> visibleChildren = new ArrayList<NotificationView>();
    }

}
