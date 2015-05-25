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


    public NotificationRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }



    public void reset() {
        resetHeight();
    }

    public void resetHeight() {

        requestLayout();
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
