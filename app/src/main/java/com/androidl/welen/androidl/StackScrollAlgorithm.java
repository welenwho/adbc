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

    private int mLayoutHeight;

    private boolean mRevertLayout = true;
    private StackScrollAlgorithmState mTempAlgorithmState = new StackScrollAlgorithmState();

    public StackScrollAlgorithm(Context context) {
        initConstants(context);
    }

    public boolean isRevertLayout(){
        return mRevertLayout;
    }

    private void initConstants(Context context) {
        mPaddingBetweenElements = context.getResources()
                .getDimensionPixelSize(R.dimen.notification_padding);
    }

    public void getStackScrollState(AmbientState ambientState, StackScrollState resultState, int range) {
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
        if(mRevertLayout){
            algorithmState.scrollY = (int) (scrollY - bottomOverScroll+topOverScroll);
        }else{
            algorithmState.scrollY = (int) (scrollY + bottomOverScroll-topOverScroll);
        }

        updateVisibleChildren(resultState, algorithmState);

        if(mRevertLayout){
            updatePositionsForStateRevert(resultState, algorithmState);
        }else{
            updatePositionsForState(resultState, algorithmState);
        }

        handleDraggedViews(ambientState, resultState, algorithmState);
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
            }

            currentYPosition = childViewState.yTranslation + childHeight + mPaddingBetweenElements;
            yPositionInScrollView = yPositionInScrollViewAfterElement;

        }
    }

    private void updatePositionsForStateRevert(StackScrollState resultState,
                                         StackScrollAlgorithmState algorithmState) {
        // The y coordinate of the current child.
        float currentYPosition = 0.0f;

        int childCount = algorithmState.visibleChildren.size();
        for (int i = 0; i < childCount; i++) {
            NotificationView child = algorithmState.visibleChildren.get(i);
            StackScrollState.ViewState childViewState = resultState.getViewStateForView(child);
            int childHeight = getChildHeight(child);
            //当前这个element的下面element的位置
            childViewState.yTranslation = currentYPosition;

            if (i == 0) {//之所以第一个特殊处理,是因为所有的item都是以第一个为参考,第一个的translation处理了滑动,后面的item都会跟着他
                childViewState.yTranslation = algorithmState.scrollY+childHeight;
            }

            childViewState.yTranslation -= childHeight;
            currentYPosition = childViewState.yTranslation - mPaddingBetweenElements;

        }
    }

    private int getChildHeight(NotificationView child) {
        return child.getHeight();
    }

    public void setLayoutHeight(int layoutHeight) {
        this.mLayoutHeight = layoutHeight;
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
