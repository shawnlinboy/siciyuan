package org.qii.weiciyuan.support.lib;

import android.os.Handler;
import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;

/**
 * support long click
 * <p/>
 * A movement method that traverses links in the text buffer and scrolls if necessary.
 * Supports clicking on links with DPad Center or Enter.
 */
public class LongClickableLinkMovementMethod extends ScrollingMovementMethod {

    private static final int CLICK = 1;
    private static final int UP = 2;
    private static final int DOWN = 3;

    private boolean mHasPerformedLongPress;
    private CheckForLongPress mPendingCheckForLongPress;

    private boolean pressed;

    private Handler handler = new Handler();

    private boolean longClickable = true;

    public void setLongClickable(boolean value) {
        this.longClickable = value;
    }

    @Override
    protected boolean handleMovementKey(TextView widget, Spannable buffer, int keyCode,
            int movementMetaState, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (KeyEvent.metaStateHasNoModifiers(movementMetaState)) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getRepeatCount() == 0 && action(CLICK, widget, buffer)) {
                        return true;
                    }
                }
                break;
        }
        return super.handleMovementKey(widget, buffer, keyCode, movementMetaState, event);
    }

    @Override
    protected boolean up(TextView widget, Spannable buffer) {
        if (action(UP, widget, buffer)) {
            return true;
        }

        return super.up(widget, buffer);
    }

    @Override
    protected boolean down(TextView widget, Spannable buffer) {
        if (action(DOWN, widget, buffer)) {
            return true;
        }

        return super.down(widget, buffer);
    }

    @Override
    protected boolean left(TextView widget, Spannable buffer) {
        if (action(UP, widget, buffer)) {
            return true;
        }

        return super.left(widget, buffer);
    }

    @Override
    protected boolean right(TextView widget, Spannable buffer) {
        if (action(DOWN, widget, buffer)) {
            return true;
        }

        return super.right(widget, buffer);
    }

    private boolean action(int what, TextView widget, Spannable buffer) {
        Layout layout = widget.getLayout();

        int padding = widget.getTotalPaddingTop() +
                widget.getTotalPaddingBottom();
        int areatop = widget.getScrollY();
        int areabot = areatop + widget.getHeight() - padding;

        int linetop = layout.getLineForVertical(areatop);
        int linebot = layout.getLineForVertical(areabot);

        int first = layout.getLineStart(linetop);
        int last = layout.getLineEnd(linebot);

        MyURLSpan[] candidates = buffer.getSpans(first, last, MyURLSpan.class);

        int a = Selection.getSelectionStart(buffer);
        int b = Selection.getSelectionEnd(buffer);

        int selStart = Math.min(a, b);
        int selEnd = Math.max(a, b);

        if (selStart < 0) {
            if (buffer.getSpanStart(FROM_BELOW) >= 0) {
                selStart = selEnd = buffer.length();
            }
        }

        if (selStart > last) {
            selStart = selEnd = Integer.MAX_VALUE;
        }
        if (selEnd < first) {
            selStart = selEnd = -1;
        }

        switch (what) {
            case CLICK:
                if (selStart == selEnd) {
                    return false;
                }

                MyURLSpan[] link = buffer.getSpans(selStart, selEnd, MyURLSpan.class);

                if (link.length != 1) {
                    return false;
                }

                link[0].onClick(widget);
                break;

            case UP:
                int beststart, bestend;

                beststart = -1;
                bestend = -1;

                for (int i = 0; i < candidates.length; i++) {
                    int end = buffer.getSpanEnd(candidates[i]);

                    if (end < selEnd || selStart == selEnd) {
                        if (end > bestend) {
                            beststart = buffer.getSpanStart(candidates[i]);
                            bestend = end;
                        }
                    }
                }

                if (beststart >= 0) {
                    Selection.setSelection(buffer, bestend, beststart);
                    return true;
                }

                break;

            case DOWN:
                beststart = Integer.MAX_VALUE;
                bestend = Integer.MAX_VALUE;

                for (int i = 0; i < candidates.length; i++) {
                    int start = buffer.getSpanStart(candidates[i]);

                    if (start > selStart || selStart == selEnd) {
                        if (start < beststart) {
                            beststart = start;
                            bestend = buffer.getSpanEnd(candidates[i]);
                        }
                    }
                }

                if (bestend < Integer.MAX_VALUE) {
                    Selection.setSelection(buffer, beststart, bestend);
                    return true;
                }

                break;
        }

        return false;
    }

    private float[] lastEvent = new float[2];

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer,
            MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_DOWN) {

            Layout layout = widget.getLayout();

            if (layout == null) {
                return super.onTouchEvent(widget, buffer, event);
            }

            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            MyURLSpan[] link = buffer.getSpans(off, off, MyURLSpan.class);

            if (link.length != 0) {
                if (action == MotionEvent.ACTION_UP) {
                    if (!mHasPerformedLongPress) {
                        link[0].onClick(widget);
                    }
                    pressed = false;
                    lastEvent = new float[2];
                } else if (action == MotionEvent.ACTION_DOWN) {
                    pressed = true;
                    lastEvent[0] = event.getX();
                    lastEvent[1] = event.getY();
                    checkForLongClick(link, widget);

                }

                return true;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            float[] position = {event.getX(), event.getY()};
//            int slop = ViewConfiguration.get(widget.getContext()).getScaledTouchSlop();
            int slop = 6;
            float xInstance = Math.abs(lastEvent[0] - position[0]);
            float yInstance = Math.abs(lastEvent[1] - position[1]);
            double instance = Math.sqrt(Math.hypot(xInstance, yInstance));
            if (instance > slop) {
                pressed = false;
            }
        } else if (action == MotionEvent.ACTION_CANCEL) {
            pressed = false;
            lastEvent = new float[2];
        } else {
            pressed = false;
            lastEvent = new float[2];
        }
        return super.onTouchEvent(widget, buffer, event);
    }

    private void checkForLongClick(MyURLSpan[] spans, View widget) {
        mHasPerformedLongPress = false;
        mPendingCheckForLongPress = new CheckForLongPress(spans, widget);
        handler.postDelayed(mPendingCheckForLongPress,
                ViewConfiguration.getLongPressTimeout());
    }

    public void removeLongClickCallback() {
        if (mPendingCheckForLongPress != null) {
            handler.removeCallbacks(mPendingCheckForLongPress);
            mPendingCheckForLongPress = null;
        }
    }

    class CheckForLongPress implements Runnable {

        MyURLSpan[] spans;

        View widget;

        public CheckForLongPress(MyURLSpan[] spans, View widget) {
            this.spans = spans;
            this.widget = widget;
        }

        public void run() {
            if (isPressed() && longClickable) {
                spans[0].onLongClick(widget);
                mHasPerformedLongPress = true;

            }
        }

    }

    private void performLongClick() {

    }

    public boolean isPressed() {
        return pressed;
    }

    @Override
    public void initialize(TextView widget, Spannable text) {
        Selection.removeSelection(text);
        text.removeSpan(FROM_BELOW);
    }

    @Override
    public void onTakeFocus(TextView view, Spannable text, int dir) {
        Selection.removeSelection(text);

        if ((dir & View.FOCUS_BACKWARD) != 0) {
            text.setSpan(FROM_BELOW, 0, 0, Spannable.SPAN_POINT_POINT);
        } else {
            text.removeSpan(FROM_BELOW);
        }
    }

    public static LongClickableLinkMovementMethod getInstance() {
        if (sInstance == null) {
            sInstance = new LongClickableLinkMovementMethod();
        }

        return sInstance;
    }

    private static LongClickableLinkMovementMethod sInstance;

    private static Object FROM_BELOW = new NoCopySpan.Concrete();
}
