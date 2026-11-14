package com.rtvvoice.app.ui.article

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * SwipeRefreshLayout tuned for a WebView child.
 *
 * The stock layout volunteers for every vertical nested scroll and, because its
 * refresh target is the wrapping FrameLayout (which never scrolls itself), its
 * default "can the child scroll up" probe always answers no. The pull-to-refresh
 * then hijacks the WebView's drags and the article only scrolls one direction.
 *
 * This subclass hands every gesture to the WebView — interception is limited to
 * a genuine pull-down while the WebView sits at the very top of its content —
 * and it never joins the nested scroll chain, whose activation alone was enough
 * to stop the WebView from scrolling.
 */
class ArticleSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SwipeRefreshLayout(context, attrs) {

    /** True when the WebView is scrolled away from the top of its content. */
    var isScrolledAwayFromTop: () -> Boolean = { false }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_MOVE && isScrolledAwayFromTop()) {
            // The WebView owns this gesture — never take it away mid-scroll.
            return false
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        // Stay out of the nested scroll chain entirely; the pull-to-refresh is
        // driven purely by touch interception above.
        return false
    }
}
