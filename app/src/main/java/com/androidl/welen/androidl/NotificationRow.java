/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License.
 */

package com.androidl.welen.androidl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

public class NotificationRow extends NotificationView {

    private String mLeftText;
    private String mRightText;
    private int mMaxExpandHeight;
    private int mAnimationTime;
    private boolean mWasReset;
    public NotificationRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        mAnimationTime = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
        updateText();
    }



    private void updateText(){
        mLeftText = "进入";//getResources().getString(R.string.notification_entry);
        mRightText = "删除";//getResources().getString(R.string.notification_delete);
        mPaint.setColor(Color.WHITE);
        mPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics()));
        mRightTextLength = mPaint.measureText(mRightText);
        mTopMargin = DimenUtils.dp2px(getContext(), 5);
        mLeftMargin = mRightMargin = DimenUtils.dp2px(getContext(), 15);
    }

    Paint mPaint = new Paint();
    private float mRightTextLength;
    private float mTopMargin, mLeftMargin,mRightMargin;

    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setAlpha((int) (mAlpha * 255));
        float offsetY = Math.max(0,-getTop())+mTopMargin+ mPaint.getTextSize();
        String text;
        float offsetX;
        if(mState == State.LEFT){
            text = mLeftText;
            offsetX = mTranslationX + mLeftMargin;
        }else if(mState == State.RIGHT){
            text = mRightText;
            offsetX = getWidth()+mTranslationX-mRightTextLength - mRightMargin;
        }else{
            text = "";
            offsetX = 0;
        }
        canvas.drawText(text, offsetX, offsetY, mPaint);
        super.onDraw(canvas);
    }

    private View mChild;

    private float mOffset;

    public void setSwipeOffset(float offset){
        mOffset = offset;
    }

   /* public void resetTranslationX(AnimatorListenerAdapter l){
        toTranslationX(0,l);
    }*/
/*
    public void toTranslationX(final float target,final AnimatorListenerAdapter l){

        AnimatorListenerAdapter animatorListener = new AnimatorListenerAdapter(){
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                ViewHelper.setAlpha(mChild, 1f);
                calculate(0);
                l.onAnimationEnd(animation);
            }
        };

        if(target == 0){
            ViewPropertyAnimator.animate(mChild).setDuration(mAnimationTime)
                    .translationX(target).setListener(animatorListener);
        }else{
            ObjectAnimator animator = ObjectAnimator.ofFloat(mChild, "translationX", target);
            animator.setDuration((long) (mAnimationTime * 1.5f));
            animator.addListener(animatorListener);
            animator.setInterpolator(new DecelerateInterpolator(1));
            final float reverseX = mTranslationX;
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float value = (Float) valueAnimator.getAnimatedValue();
                    float percent = (1f - value / target);
                    float offset = reverseX * percent;
                    calculateReverse(offset);
                    ViewHelper.setAlpha(mChild, mAlpha);
                }
            });
            animator.start();
        }

    }*/
    enum State{
        NONE,LEFT,RIGHT
    }

    private float mAlpha = 0;
    private float mTranslationX = 0;
    private State mState = State.NONE;

    private void calculateReverse(float x){
        if(x < 0){
            mState = State.RIGHT;
        }else if(x > 0){
            mState = State.LEFT;
        }else{
            mState = State.NONE;
        }

        if(mState == State.RIGHT){
            mTranslationX = x;
            mAlpha = Math.abs(x)/mOffset;
        }else if(mState == State.LEFT){
            mTranslationX = x;
            mAlpha = Math.abs(x)/mOffset;
        }else{
            mAlpha = 0f;
            mTranslationX = 0;
        }
        invalidate();
    }
    private void calculate(float x){
        if(x < 0){
            mState = State.RIGHT;
        }else if(x > 0){
            mState = State.LEFT;
        }else{
            mState = State.NONE;
        }
        if(mState == State.NONE){
            mAlpha = 0f;
            mTranslationX = 0;
        }else{
            if(Math.abs(x) > mOffset){
                mAlpha = 1f;
                if(mState == State.RIGHT){
                    mTranslationX = x + mOffset;
                }else{
                    mTranslationX = x - mOffset;
                }

            }else{
                mAlpha = Math.abs(x)/mOffset;
                mTranslationX = 0;
            }
        }
        invalidate();
    }
    public void setDragX(boolean left,float swipeProcess){
        float x = (left? -1:1) *swipeProcess * getMeasuredWidth();
        /*if( x > 0){
            if(Math.abs(x) > mOffset){
                x = mOffset+(x-mOffset) / 3;
            }
            mChild.setTranslationX(x);

        }else if(x < 0){
            if(Math.abs(x) > mOffset){
                x = -mOffset+(x+mOffset) / 3;
            }
            mChild.setTranslationX(x);

        }*/
        calculate(x);
    }

    private boolean isTransparent(float color){
        return color == 0;
    }

    public void reset() {
        resetHeight();
    }

    public void resetHeight() {
        mMaxExpandHeight = 0;
        mWasReset = true;
        onHeightReset();
        requestLayout();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mChild = findViewById(R.id.child);
    }

    /**
     * Apply an expansion state to the layout.
     */
    public void applyExpansionToLayout() {
        setActualHeight(getMeasuredHeight());
    }

    @Override
    public int getIntrinsicHeight() {
        return getActualHeight();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int width = right - left;
        if(changed && width> 2){
            setSwipeOffset(width / 6);
        }
        boolean updateExpandHeight = mMaxExpandHeight == 0 && !mWasReset;
        updateMaxExpandHeight();
        if (updateExpandHeight) {
            applyExpansionToLayout();
        }
        mWasReset = false;
    }

    private void updateMaxExpandHeight() {
        int intrinsicBefore = getIntrinsicHeight();
//        mMaxExpandHeight = mPrivateLayout.getMaxHeight();
        if (intrinsicBefore != getIntrinsicHeight()) {
            notifyHeightChanged();
        }
    }

    @Override
    public void performRemoveAnimation(long duration, float translationDirection, Runnable onFinishedRunnable) {


    }

    @Override
    public void performAddAnimation(long delay, long duration) {
//        AnimationSet set = new AnimationSet(false);
        ScaleAnimation sa = new ScaleAnimation(0f,1f,0f,1f,0.5f,0.5f);
        sa.setDuration(duration);
        startAnimation(sa);
    }
}
