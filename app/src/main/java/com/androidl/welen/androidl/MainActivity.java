package com.androidl.welen.androidl;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;


public class MainActivity extends Activity {

    NotificationScrollLayout mStackLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStackLayout = (NotificationScrollLayout) findViewById(R.id.stack_scroll_layout);
        mStackLayout.setScrollingEnabled(true);
        mStackLayout.setMainActivity(this);
        mStackLayout.setAnimationsEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            NotificationRow row = (NotificationRow) LayoutInflater.from(this).inflate(R.layout.status_bar_notification_row, mStackLayout, false);

//            viewById.setExpandedChild(getTextView("welenwho aaa"));
//            row.setUserLocked(true);
            mNotificationView.add(0,row);
            updateNotificationShade();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public ArrayList<NotificationRow> mNotificationView = new ArrayList<NotificationRow>();
    public void updateNotificationShade() {
        if (mStackLayout == null) return;

        ArrayList<NotificationRow> toShow = new ArrayList<>(mNotificationView.size());
        final int N = mNotificationView.size();
        for (int i=0; i<N; i++) {
            toShow.add(mNotificationView.get(i));
        }

        ArrayList<View> toRemove = new ArrayList<View>();
        for (int i=0; i< mStackLayout.getChildCount(); i++) {
            View child = mStackLayout.getChildAt(i);
            if (!toShow.contains(child) && child instanceof NotificationRow) {
                toRemove.add(child);
            }
        }

        for (View remove : toRemove) {
            mStackLayout.removeView(remove);
        }
        for (int i=0; i<toShow.size(); i++) {
            View v = toShow.get(i);
            if (v.getParent() == null) {
                mStackLayout.addView(v);
            }
        }

        // So after all this work notifications still aren't sorted correctly.
        // Let's do that now by advancing through toShow and mStackScroller in
        // lock-step, making sure mStackScroller matches what we see in toShow.
        int j = 0;
        for (int i = 0; i < mStackLayout.getChildCount(); i++) {
            View child = mStackLayout.getChildAt(i);
            if (!(child instanceof NotificationRow)) {
                // We don't care about non-notification views.
                continue;
            }

            if (child == toShow.get(j)) {
                // Everything is well, advance both lists.
                j++;
                continue;
            }

            // Oops, wrong notification at this position. Put the right one
            // here and advance both lists.
            mStackLayout.changeViewPosition(toShow.get(j), i);
            j++;
        }
    }

    private TextView getTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setLayoutParams(
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        return textView;
    }
}
