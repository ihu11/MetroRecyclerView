package com.ihu11.metro.recycler;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.ihu11.metro.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hxy
 */
public class MetroRecyclerView extends RecyclerView {
    private final static String TAG = "MetroRecyclerView";
    public boolean DEBUG = false;

    /**
     * 滚动允许的误差值
     */
    private final static int DEVIATION = 1;
    /**
     * 在最后一行才往下滚
     */
    public final static int SCROLL_TYPE_ON_LAST = 0;
    /**
     * 留一行表示出未到最后一行,底下留一行就开始滚
     */
    public final static int SCROLL_TYPE_ALWAYS_LEFT_ONE = 1;
    /**
     * 把下一行放到RecyclerView的正中间,屏幕中的view必须最多三行
     */
    public final static int SCROLL_TYPE_ALWAYS_CENTER = 2;

    protected final static int VIRTUAL_KEY_CODE_NEXT_ROW = 1;
    protected final static int VIRTUAL_KEY_CODE_PRE_ROW = 2;
    protected final static int VIRTUAL_KEY_CODE_PRE_ONE = 3;
    protected final static int VIRTUAL_KEY_CODE_NEXT_ONE = 4;

    private final static int ERROR_POSITION = -1;
    private final static int NONE_POSITION = -2;

    protected float SCALE = 1.1f;
    protected PropertyValuesHolder mScaleInX;
    protected PropertyValuesHolder mScaleInY;
    protected PropertyValuesHolder mScaleOutX;
    protected PropertyValuesHolder mScaleOutY;
    protected static final int SCALE_OUT_DURATION = 200;
    protected static final int SCALE_IN_DURATION = 200;
    protected boolean isScale = true;
    private static final int MAX_MULTIPLE_SPEED = 4;// 最大加速倍数，用来限制fly的速度
    protected ObjectAnimator mOutAnimator;
    protected ObjectAnimator mInAnimator;
    protected int mLastAnimOutPosition = -1;
    protected int mLastAnimInPosition = -1;
    protected static boolean isTouchMode = false;

    private OnMoveToListener mOnMoveToListener;
    private int mOrientation = VERTICAL;
    private int mScrollDistance = -1;
    protected int mPosition = 0;
    private int mLastColumn = 0;
    private int mLastVirtualKeyCode = -1;

    protected boolean isLoaded = false;
    protected boolean isFocus = false;

    protected boolean isFlying = false;
    private boolean isScrolling = false;
    private boolean waitScrolling = false;
    private boolean isStopByHand;

    private Method mSmoothScrollMethod;
    private Object mViewFlinger;
    private static final Interpolator mInterpolator = new DecelerateInterpolator();

    private int mScrollType = SCROLL_TYPE_ALWAYS_LEFT_ONE;

    private boolean isFocusViewOnFrontEnable = false;
    private View frontView;
    private int mOffsetX;
    private int mOffsetY;

    protected int mLeftDistance = 0;

    // 上一次起飞前view的top和bottom
    private int mLastViewBottom = -1;
    private int mLastViewTop = -1;

    private int mColumnCount = 1;

    private boolean isSupportVRightKey = false;
    private boolean isSupportVLeftKey = false;

    private Rect mSpacesRect;
    protected int paddingLeft;
    protected int paddingRight;
    private int paddingTop;
    private int paddingBottom;

    private Recycler mRecycler;
    private boolean isStartBind = true;
    private boolean isDelayBindEnable = false;
    private boolean isSelectAfterKeyUp = true;
    private ArrayList<View> mAttachedViewArray;

    protected boolean isFlowSmooth = true;
    private boolean isAlwaysSelected = false;

    // 删除数据时的参数
    private boolean isDeleting = false;

    private int mFlyDirection;
    private static final int DIRECTION_NONE = -1;
    private static final int DIRECTION_DOWN = 0;
    private static final int DIRECTION_UP = 1;

    public interface OnScrollEndListener {
        public void onScrollToBottom(int keyCode);

        public void onScrollToTop(int keyCode);
    }

    private OnScrollEndListener mOnScrollEndListener;

    public void setOnScrollEndListener(OnScrollEndListener onScrollEndListener) {
        this.mOnScrollEndListener = onScrollEndListener;
    }

    private MetroItemFocusListener mOnItemFocusListener;

    public void setOnItemFocusListener(MetroItemFocusListener onItemFocusListener) {
        this.mOnItemFocusListener = onItemFocusListener;
    }

    private MetroItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(MetroItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    private MetroItemLongClickListener mOnItemLongClickListener;

    public void setOnItemLongClickListener(MetroItemLongClickListener onItemLongClickListener) {
        this.mOnItemLongClickListener = onItemLongClickListener;
    }

    public void setOnMoveToListener(OnMoveToListener onMoveToListener) {
        this.mOnMoveToListener = onMoveToListener;
    }

    // 滚动条位置监听器
    public interface OnScrollBarStatusListener {
        public void onScrollBarStatus(boolean canScrollUp, boolean canScrollDown);
    }

    // 当移动到焦点时立即响应onitemfoucs
    public void setSelectAfterKeyDown() {
        isSelectAfterKeyUp = false;
    }

    private OnScrollBarStatusListener mOnScrollBarStatusListener;

    public void setOnScrollBarStatusListener(OnScrollBarStatusListener onScrollBarStatusListener) {
        this.mOnScrollBarStatusListener = onScrollBarStatusListener;
    }

    protected final class Status {
        // 上次按的虚拟按键
        int lastVirtualKeyCode;
        // 这次按的虚拟按键
        int virtualKeyCode;

        int firstVisibleItemPosition;
        int firstCompletelyVisibleItemPosition;
        int firstVisibleItemPositionInScreen;
        int lastVisibleItemPosition;
        int lastCompletelyVisibleItemPosition;
        int lastVisibleItemPositionInScreen;

        // 是否需要向下滚动
        boolean needScrollDown = false;
        // 是否需要向上滚动
        boolean needScrollUp = false;

        // 下一个滚动到的Position
        public int nextPosition;

        // 一共有多少个item
        int itemCount;

        public Status() {
        }

        public void initVisibleItemPosition(LayoutManager layoutManager) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
            firstCompletelyVisibleItemPosition = findFirstCompletelyVisibleItemPosition(firstVisibleItemPosition);
            lastVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition();
            lastCompletelyVisibleItemPosition = findLastCompletelyVisibleItemPosition(firstVisibleItemPosition,
                    lastVisibleItemPosition);

            // 屏幕中能显示到的view(有时候有些firstVisibleItemPosition不在屏幕内)
            firstVisibleItemPositionInScreen = findFirstVisibleItemPositionInScreen(firstVisibleItemPosition,
                    firstCompletelyVisibleItemPosition);
            lastVisibleItemPositionInScreen = findLastVisibleItemPositionInScreen(firstVisibleItemPosition,
                    lastVisibleItemPosition, lastCompletelyVisibleItemPosition);

            if (DEBUG) {
                Log.i(TAG, "Status firstVisibleItemPosition:" + firstVisibleItemPosition);
                Log.i(TAG, "Status firstCompletelyVisibleItemPosition:" + firstCompletelyVisibleItemPosition);
                Log.i(TAG, "Status lastVisibleItemPosition:" + lastVisibleItemPosition);
                Log.i(TAG, "Status lastCompletelyVisibleItemPosition:" + lastCompletelyVisibleItemPosition);
            }
        }

        public int getNextViewIndex() {
            return nextPosition - firstVisibleItemPosition;
        }

        public View getNextChildView() {
            return getChildAt(nextPosition - firstVisibleItemPosition);
        }

        public View getChildView(int position) {
            return getChildAt(position - firstVisibleItemPosition);
        }

    }

    public MetroRecyclerView(Context context) {
        this(context, null);
    }

    public MetroRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MetroRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MetroRecyclerView);
            int left = a.getDimensionPixelSize(R.styleable.MetroRecyclerView_itemSpaceLeft, 0);
            int top = a.getDimensionPixelSize(R.styleable.MetroRecyclerView_itemSpaceTop, 0);
            int right = a.getDimensionPixelSize(R.styleable.MetroRecyclerView_itemSpaceRight, 0);
            int bottom = a.getDimensionPixelSize(R.styleable.MetroRecyclerView_itemSpaceBottom, 0);
            // padding值只能通过recyclerPadding设置

            paddingLeft = a.getDimensionPixelSize(R.styleable.MetroRecyclerView_recyclerPaddingLeft, 0);
            paddingRight = a.getDimensionPixelSize(R.styleable.MetroRecyclerView_recyclerPaddingRight, 0);
            paddingTop = a.getDimensionPixelSize(R.styleable.MetroRecyclerView_recyclerPaddingTop, 0);
            paddingBottom = a.getDimensionPixelSize(R.styleable.MetroRecyclerView_recyclerPaddingBottom, 0);

            int padding = a.getDimensionPixelSize(R.styleable.MetroRecyclerView_recyclerPadding, 0);
            if (padding != 0) {
                paddingLeft = padding;
                paddingRight = padding;
                paddingTop = padding;
                paddingBottom = padding;
            }

            boolean isSupportVLeftKey = a.getBoolean(R.styleable.MetroRecyclerView_supportVLeftKey, false);
            boolean isSupportVRightKey = a.getBoolean(R.styleable.MetroRecyclerView_supportVRightKey, false);
            boolean isFocusViewOnFrontEnable = a.getBoolean(R.styleable.MetroRecyclerView_focusViewOnFrontEnable,
                    false);
            boolean isDelayBindEnable = a.getBoolean(R.styleable.MetroRecyclerView_delayBindEnable, false);
            boolean isScaleEnable = a.getBoolean(R.styleable.MetroRecyclerView_scaleEnable, true);

            SCALE = a.getFloat(R.styleable.MetroRecyclerView_scale, SCALE);
            mScaleInX = PropertyValuesHolder.ofFloat("scaleX", SCALE, 1.0f);
            mScaleInY = PropertyValuesHolder.ofFloat("scaleY", SCALE, 1.0f);
            mScaleOutX = PropertyValuesHolder.ofFloat("scaleX", 1.0f, SCALE);
            mScaleOutY = PropertyValuesHolder.ofFloat("scaleY", 1.0f, SCALE);

            a.recycle();

            if (left != 0 || top != 0 || right != 0 || bottom != 0) {
                setItemSpaces(left, top, right, bottom);
            }
            setSupporExtraKey(isSupportVLeftKey, isSupportVRightKey);
            setFocusViewOnFrontEnable(isFocusViewOnFrontEnable);
            setDelayBindEnable(isDelayBindEnable);
            setScaleEnable(isScaleEnable);
        }
        init();
    }

    public void setPaddingParam(int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) {
        this.paddingLeft = paddingLeft;
        this.paddingTop = paddingTop;
        this.paddingRight = paddingRight;
        this.paddingBottom = paddingBottom;
    }

    private void init() {
        setHasFixedSize(true);
        initSmoothScrollMethod();
        initFirstShowView();
        initFocusViewOnFront();
        initScrollEvent();
        initDeletingItemAnimator();
        if (isTouchMode) {
            setScaleEnable(false);
        }
    }

    public static void setTouchMode(boolean b) {
        isTouchMode = b;
    }

    private void initRecycler() {
        try {
            final Field f = RecyclerView.class.getDeclaredField("mRecycler");
            f.setAccessible(true);
            mRecycler = (Recycler) f.get(MetroRecyclerView.this);
            mAttachedViewArray = new ArrayList<View>();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initSmoothScrollMethod() {
        try {
            final Field f = RecyclerView.class.getDeclaredField("mViewFlinger");
            f.setAccessible(true);
            mViewFlinger = f.get(MetroRecyclerView.this);
            mSmoothScrollMethod = mViewFlinger.getClass().getDeclaredMethod("smoothScrollBy", int.class, int.class,
                    int.class, Interpolator.class);
            mSmoothScrollMethod.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initFirstShowView() {
        addOnChildAttachStateChangeListener(new OnChildAttachStateChangeListener() {

            @Override
            public void onChildViewDetachedFromWindow(View view) {
                // Log.i("Catch", "onChildViewDetachedFromWindow"
                // + getChildAdapterPosition(view));
                if (isDelayBindEnable && mAttachedViewArray != null && mAttachedViewArray.contains(view)) {
                    mAttachedViewArray.remove(view);
                }
            }

            @Override
            public void onChildViewAttachedToWindow(final View view) {
                // Log.i("Catch", "onChildViewAttachedToWindow"
                // + getChildAdapterPosition(view));
                if (!isNeedBind() && mAttachedViewArray != null) {
                    mAttachedViewArray.add(view);
                }

                if (!isLoaded) {
                    final int childPosition = getChildAdapterPosition(view);
                    if (childPosition == mPosition) {
                        view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

                            @SuppressWarnings("deprecation")
                            @Override
                            public void onGlobalLayout() {
                                view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                                if (!isLoaded) {
                                    saveViewLocationBeforeFly(view);
                                    if (isFocus) {
                                        if (DEBUG) {
                                            Log.i(TAG, "first focus" + getChildAdapterPosition(view));
                                        }
                                        flowMoveTo(view, 0);
                                        scaleOut(view, childPosition);
                                    } else if (isAlwaysSelected) {
                                        selectView(view);
                                    }
                                    if (mOnScrollBarStatusListener != null) {
                                        mOnScrollBarStatusListener.onScrollBarStatus(canScrollUp(), canScrollDown());
                                    }
                                    isLoaded = true;
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    private void initFocusViewOnFront() {
        setChildDrawingOrderCallback(new RecyclerView.ChildDrawingOrderCallback() {

            @Override
            public int onGetChildDrawingOrder(int childCount, int iteration) {
                if (isFocusViewOnFrontEnable) {
                    if (frontView == null) {
                        return iteration;
                    }
                    int indexOfFrontChild = indexOfChild(frontView);
                    if (iteration == childCount - 1) {// 这是最后一个需要刷新的item
                        if (getChildAt(indexOfFrontChild) != null) {
                            return indexOfFrontChild;
                        }
                    }
                    if (iteration == indexOfFrontChild) {//
                        // 这是原本要在最后一个刷新的item
                        return childCount - 1;
                    }
                }
                return iteration;// 正常次序的item
            }
        });
    }

    private void initScrollEvent() {
        addOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (DEBUG) {
                    Log.i(TAG, "onScrollStateChanged:" + newState);
                }
                switch (newState) {
                    case SCROLL_STATE_DRAGGING:
                    case SCROLL_STATE_SETTLING:
                        isScrolling = true;
                        break;
                    case SCROLL_STATE_IDLE:
                        isScrolling = false;
                        waitScrolling = false;
                        if (isStopByHand) {
                            isStopByHand = false;
                        } else {
                            if (mLeftDistance != 0) {
                                Log.w(TAG, "setStatusByNearestView mLeftDistance:" + mLeftDistance);
                                mLeftDistance = 0;
                                setStatusByNearestView();
                            }
                            if (!canScrollDown() && mOnScrollEndListener != null) {
                                mOnScrollEndListener.onScrollToBottom(KeyEvent.KEYCODE_UNKNOWN);
                            }
                        }
                        if (mOnScrollBarStatusListener != null) {
                            mOnScrollBarStatusListener.onScrollBarStatus(canScrollUp(), canScrollDown());
                        }
                        break;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                //Log.i(TAG, "onScrolled:" + dx);
                if (mLeftDistance != 0) {
                    if (mOrientation == VERTICAL) {
                        mLeftDistance -= dy;
                    } else {
                        mLeftDistance -= dx;
                    }
                }

                if (isFlying) {
                    if (mFlyDirection == DIRECTION_UP && !canScrollUp()) {
                        if (DEBUG) {
                            Log.i(TAG, "scroll end on top!");
                        }
                        // 顶部
                        int topIndex = mLastColumn;
                        setFinalStatus(topIndex);
                    } else if (mFlyDirection == DIRECTION_DOWN && !canScrollDown()) {
                        // 底部
                        if (DEBUG) {
                            Log.i(TAG, "scroll end on bottom!");
                        }
                        int bottomIndex = (getChildCount() - 1) / mColumnCount * mColumnCount + mLastColumn;
                        while (bottomIndex > getChildCount() - 1) {
                            bottomIndex = getChildCount() - 1;
                        }

                        setFinalStatus(bottomIndex);
                    } else if (mLeftDistance == 0) {
                        if (DEBUG) {
                            Log.i(TAG, "scroll end!(mLeftDistance == 0)");
                        }
                        scrollExtraForEnd();
                    }
                } else {
                    if (mLeftDistance == 0) {
                        scaleOut(findViewByPosition(mPosition), mPosition);
                    }
                }
            }

        });
    }

    @Override
    public void stopScroll() {
        isStopByHand = true;
        super.stopScroll();
    }

    /**
     * 默认的Animator,如果要使用deleteItem和changeItem,则必须使用该动画
     */
    private void initDeletingItemAnimator() {
        MetroItemAnimator deletingItemAnimator = new MetroItemAnimator();
        deletingItemAnimator.setChangeDuration(0);
        setItemAnimator(deletingItemAnimator);
    }

    private void bindView() {
        if (!isDelayBindEnable || mRecycler == null || mAttachedViewArray == null || getAdapter() == null) {
            return;
        }
        isStartBind = true;

        int itemCount = getAdapter().getItemCount();
        for (View view : mAttachedViewArray) {
            int position = getChildAdapterPosition(view);
            // mRecycler.convertPreLayoutPositionToPostLayout(position);
            if (position < 0 || position > itemCount - 1) {
                continue;
            }
            mRecycler.bindViewToPosition(view, position);
            if (DEBUG) {
                Log.i(TAG, "bindViewToPosition:" + position);
            }
        }
        mAttachedViewArray.clear();
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (DEBUG) {
            Log.i(TAG, "onFocusChanged:" + gainFocus);
        }
        if (!isFocus && gainFocus) {
            isFocus = gainFocus;
            if (isLoaded) {
                View view = findViewByPosition(mPosition);
                onFocusMoveTo(view);
                scaleOut(view, mPosition);
            }
        } else if (!gainFocus) {
            isFocus = gainFocus;
            lostFocus();
        }
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        return super.requestFocus(direction, previouslyFocusedRect);
    }

    protected void lostFocus() {
        View view = findViewByPosition(mPosition);
        if (view != null) {
            boolean isClearSeleted = !isAlwaysSelected;
            scaleIn(view, mPosition, isClearSeleted);
        }
        mLastAnimOutPosition = -1;
        mLastAnimInPosition = -1;
    }

    protected void onFocusMoveTo(View view) {
        flowMoveTo(view, 0);
    }

    /**
     * 滚动是否还在继续
     *
     * @return
     */
    public boolean isScrolling() {
        return isScrolling || isFlying || waitScrolling;
    }

    /**
     * 是否支持左右按键换行(在垂直模式(VERTICAL)是左右按键，水平模式(HORIZONTAL)下是上下按键)
     *
     * @param isSupportVLeftKey
     * @param isSupportVRightKey
     */
    public void setSupporExtraKey(boolean isSupportVLeftKey, boolean isSupportVRightKey) {
        this.isSupportVLeftKey = isSupportVLeftKey;
        this.isSupportVRightKey = isSupportVRightKey;
    }

    /**
     * 设置滚动类型
     *
     * @param type SCROLL_TYPE_ON_LAST 在最后一行才往下滚
     *             SCROLL_TYPE_ALWAYS_LEFT_ONE 留一行表示出未到最后一行,底下留一行就开始滚
     *             SCROLL_TYPE_ALWAYS_CENTER
     *             把下一行放到RecyclerView的正中间,屏幕中的view必须最多三行
     */
    public void setScrollType(int type) {
        mScrollType = type;
    }

    public void setFlowOffset(int x, int y) {
        mOffsetX = x;
        mOffsetY = y;
    }

    /**
     * 设置focusView是否需要悬浮在最外层(如果item周围有足够空间，则可不用悬浮，这样可以提高效率)
     *
     * @param enable
     */
    public void setFocusViewOnFrontEnable(boolean enable) {
        isFocusViewOnFrontEnable = enable;
    }

    /**
     * 设置显示的item，默认为0
     *
     * @param position
     */
    public void setSelectedItem(int position) {
        View itemView = findViewByPosition(position);
        if (itemView == null) {
            isLoaded = false;
            View lastView = findViewByPosition(mPosition);
            if (lastView != null) {
                leaveView(lastView);
            }
        } else if (!hasFocus()) {
            View lastView = findViewByPosition(mPosition);
            if (lastView != null) {
                leaveView(lastView);
            }

            View v = findViewByPosition(position);
            if (v != null) {
                selectView(v);
            }
        } else {
            int selectedPosition = getSelectedPosition();
            if (selectedPosition == position) {
                return;
            }
            View v = findViewByPosition(selectedPosition);
            if (v != null) {
                scaleIn(v, selectedPosition);
            }
            View view = findViewByPosition(position);
            if (view != null) {
                onFocusMoveTo(view);
                scaleOut(view, position);
            }
        }
        setSelectedItemWithOutScroll(position);
        scrollToPosition(position);
    }

    /**
     * 设置显示的item不自带滚动功能 ，默认为0
     *
     * @param position
     */
    public void setSelectedItemWithOutScroll(int position) {
        mPosition = position;
        mLastColumn = position % mColumnCount;
    }

    /**
     * 是否需要开始绑定延迟绑定的数据
     */
    private boolean isNeedBind() {
        return isDelayBindEnable && (!isFlying || isStartBind);
    }

    private boolean isDelayBindEnable() {
        return isDelayBindEnable;
    }

    /**
     * 是否延迟绑定数据
     */
    public void setDelayBindEnable(boolean b) {
        if (b && getAdapter() != null && !(getAdapter() instanceof MetroAdapter)) {
            throw new IllegalArgumentException("Adapter is not instanceof MetroAdapter, it's not support DelayBind!");
        }

        isDelayBindEnable = b;
        if (b && mRecycler == null) {
            initRecycler();
        }
    }

    public void setScaleEnable(boolean b) {
        isScale = b;
    }

    public int getSelectedPosition() {
        if (!isFlying) {
            return mPosition;
        } else {
            // 在fly的时候mPosition的是无意义的
            return -1;
        }
    }

    /**
     * 找到当前选择的View
     *
     * @return
     */
    public View findSelectedView() {
        int position = getSelectedPosition();
        if (position != -1) {
            return findViewByPosition(position);
        } else {
            return null;
        }
    }

    /**
     * 根据position找到view(只要当前显示的才有效)
     *
     * @param position
     * @return
     */
    public View findViewByPosition(int position) {
        if (getLayoutManager() == null) {
            return null;
        }
        return getLayoutManager().findViewByPosition(position);
    }

    /**
     * 下次的飞框移动是直接闪过去的
     */
    public void setFlowWithOutSmoothOnce() {
        this.isFlowSmooth = false;
    }

    /**
     * 选中状态不会因为失去焦点而丢失
     */
    public void setAlwaysSelected() {
        this.isAlwaysSelected = true;
    }

    /**
     * 重置所有数据
     */
    public void notifyResetData() {
        mPosition = 0;
        mLastColumn = 0;
        mLastViewBottom = 0;
        mLastViewTop = 0;
        mLastAnimOutPosition = -1;
        mLastAnimInPosition = -1;
        scrollToPosition(0);
        isLoaded = false;
        if (mAttachedViewArray != null) {
            mAttachedViewArray.clear();
        }
        removeAllViews();
        getAdapter().notifyDataSetChanged();
    }

    /**
     * 删除item的view 包含了data.remove
     *
     * @param position
     */
    public boolean deleteItem(int position, List<?> data) {
        // 暂时忽略了两行不相等的情况,目前只支持的Grid
        if (isFlying || isDeleting || waitScrolling || getChildCount() == 0) {
            return false;
        }

        isDeleting = true;
        data.remove(position);
        deleteView(position);
        getAdapter().notifyItemRemoved(position);
        return true;
    }

    private void deleteView(int deletePosition) {
        View deleteView = findViewByPosition(deletePosition);
        View selectView = null;
        View selectPositionView = null;
        View leaveView = null;
        int leavePosition = 0;

        if (mPosition == deletePosition) {
            if (mPosition == getAdapter().getItemCount()) {
                // 删除的是最后一个焦点
                mPosition -= 1;
                selectView = findViewByPosition(mPosition);
                selectPositionView = selectView;
                leaveView = deleteView;
                leavePosition = mPosition;
            } else {
                // 删除的是不是最后一个焦点
                selectView = findViewByPosition(mPosition + 1);
                selectPositionView = deleteView;
                leaveView = deleteView;
                leavePosition = mPosition;
            }
        } else if (mPosition < deletePosition) {
            // 删除的是焦点后面的项
            selectPositionView = findViewByPosition(mPosition);
        } else {
            // 删除的是焦点前面的项
            int first = findFirstCompletelyVisibleItemPosition();
            if (deletePosition >= first) {
                mPosition -= 1;
                selectPositionView = findViewByPosition(mPosition);
            } else {
                // 如果当前位置的前一个位置在first之后，删除后当前的View才还会在屏幕中，不然会自动收到上一页去
                if (mPosition - 1 >= first) {
                    mPosition -= 1;
                    selectPositionView = findViewByPosition(mPosition);
                } else {
                    selectView = findViewByPosition(mPosition + 1);
                    selectPositionView = findViewByPosition(mPosition);
                    leaveView = selectPositionView;
                    leavePosition = mPosition;
                }
            }
        }

        if (selectPositionView != null) {
            int offset = 0;
            if (getRowsCount(getChildCount()) != getRowsCount(getChildCount() - 1)) {
                offset = getViewHeight(selectPositionView) + getItemSpaceTop() + getItemSpaceBottom();
                int scrollOffset = computeScrollOffset();
                offset = scrollOffset > offset ? -offset : -scrollOffset;
            }

            flowMoveTo(selectPositionView, offset);
            mLastViewTop = getViewTopLocation(selectPositionView) - mLeftDistance + offset;
            mLastViewBottom = getViewHeight(selectPositionView) + mLastViewTop + offset;
        }

        if (leaveView != null) {
            if (isScale) {
                leaveView.setScaleX(1.0f);
                leaveView.setScaleY(1.0f);
            }
            leaveView(leaveView);
            mLastAnimInPosition = leavePosition;
        }

        if (selectView != null) {
            if (isScale) {
                mOutAnimator = ObjectAnimator.ofPropertyValuesHolder(selectView, mScaleOutX, mScaleOutY);
                mOutAnimator.setDuration(SCALE_OUT_DURATION);
                mOutAnimator.start();
            }
            selectView(selectView);
        }

        if (deleteView != null) {
            removeViewInLayout(deleteView);
            deleteView = null;
        }

        mLastAnimOutPosition = mPosition;
        mLastColumn = mPosition % mColumnCount;

        postDelayed(new Runnable() {

            @Override
            public void run() {
                // 限制删除速度，容易界面出错
                isDeleting = false;
            }
        }, 500);
    }

    @Override
    public void setAdapter(@SuppressWarnings("rawtypes") Adapter adapter) {
        if (!(adapter instanceof MetroAdapter) && isDelayBindEnable) {
            throw new IllegalArgumentException(
                    "The isDelayBindEnable is true,but Adapter is not instanceof MetroAdapter, it's not support DelayBind!");
        }
        mPosition = 0;
        mLastColumn = 0;
        mLastViewBottom = 0;
        mLastViewTop = 0;
        mLastAnimOutPosition = -1;
        mLastAnimInPosition = -1;
        isLoaded = false;
        mLeftDistance = 0;
        scrollToPosition(0);
        if (mAttachedViewArray != null) {
            mAttachedViewArray.clear();
        }
        removeAllViews();
        super.setAdapter(adapter);
    }

    private int getScrollDistance() {
        if (mScrollDistance == -1) {
            View child = getChildAt(0);
            mScrollDistance = getViewHeight(child) + getItemSpaceTop() + getItemSpaceBottom();
            if (DEBUG) {
                Log.i(TAG, "default mScrollDistance:" + mScrollDistance);
            }
        }
        return mScrollDistance;
    }

    private int getViewHeight(View view) {
        if (mOrientation == VERTICAL) {
            return view.getHeight();
        } else {
            return view.getWidth();
        }
    }

    protected int getViewBottomLocation(View view) {
        int bottom = getViewTopLocation(view) + getViewHeight(view);
        return bottom;
    }

    protected int getViewTopLocation(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        int top;
        if (mOrientation == VERTICAL) {
            top = (int) (location[1] + view.getHeight() * (view.getScaleY() - 1) / 2);
        } else {
            top = (int) (location[0] + view.getWidth() * (view.getScaleX() - 1) / 2);
        }
        return top;
    }

    //剩余可滚动距离
    private int getLeftScroll(Status status, View child) {
        int left = getTotalRows(status) * (getViewHeight(child) + getItemSpaceTop() + getItemSpaceBottom())
                - getViewHeight(this) - computeScrollOffset();
        if (mOrientation == VERTICAL) {
            left += paddingLeft + paddingRight;
        } else {
            left += paddingTop + paddingBottom;
        }
        return left;
    }

    private int getItemSpaceTop() {
        if (mSpacesRect != null) {
            if (mOrientation == VERTICAL) {
                return mSpacesRect.top;
            } else {
                return mSpacesRect.left;
            }
        } else {
            return 0;
        }
    }

    private int getItemSpaceBottom() {
        if (mSpacesRect != null) {
            if (mOrientation == VERTICAL) {
                return mSpacesRect.bottom;
            } else {
                return mSpacesRect.right;
            }
        } else {
            return 0;
        }
    }

    protected void flowMoveTo(View view, int offset) {
        if (!isFocus || view == null) {
            return;
        }

        if (mOnMoveToListener != null) {
            if (mOrientation == VERTICAL) {
                mOnMoveToListener.onMoveTo(view, isScale ? SCALE : 1.0f, mOffsetX, offset, isFlowSmooth);
                isFlowSmooth = true;
                mOffsetX = 0;
            } else {
                mOnMoveToListener.onMoveTo(view, isScale ? SCALE : 1.0f, offset, mOffsetY, isFlowSmooth);
                isFlowSmooth = true;
                mOffsetY = 0;
            }
        }
    }

    public boolean canScrollDown() {
        if (mOrientation == VERTICAL) {
            return canScrollVertically(VERTICAL);
        } else {
            return canScrollHorizontally(HORIZONTAL);
        }
    }

    public boolean canScrollUp() {
        if (mOrientation == VERTICAL) {
            return computeVerticalScrollOffset() > 0;
        } else {
            return computeHorizontalScrollOffset() > 0;
        }
    }

    private int computeScrollOffset() {
        if (mOrientation == VERTICAL) {
            return computeVerticalScrollOffset();
        } else {
            return computeHorizontalScrollOffset();
        }
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        super.setLayoutManager(layout);
        if (layout instanceof GridLayoutManager) {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) layout;
            mOrientation = gridLayoutManager.getOrientation();
            mColumnCount = gridLayoutManager.getSpanCount();
            if (mColumnCount == 1) {
                // 一列的默认不支持左右按键
                isSupportVRightKey = false;
                isSupportVLeftKey = false;
            }
            // 目前不支持反转
            gridLayoutManager.setReverseLayout(false);
        } else if (layout instanceof LinearLayoutManager) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layout;
            mOrientation = linearLayoutManager.getOrientation();
            // listview类型默认不支持左右按键
            mColumnCount = 1;
            isSupportVRightKey = false;
            isSupportVLeftKey = false;
            // 目前不支持反转
            linearLayoutManager.setReverseLayout(false);
        } else {
            // not support StaggeredGridLayoutManager
            throw new IllegalArgumentException("not support StaggeredGridLayoutManager");
        }

        // 竖向布局的时候，paddingLeft属性有错误，必须替换成View padding属性,且不能设置top和bottom
        if (mOrientation == VERTICAL) {
            setPadding(paddingLeft, 0, paddingRight, 0);
            setClipToPadding(false);
            paddingLeft = 0;
            paddingRight = 0;
        }
    }

    // 进行按键转换，用于变化方向的时候直接映射到虚拟按键
    private int convertVirtualKeyCode(int keyCode) {
        if (mOrientation == VERTICAL) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return VIRTUAL_KEY_CODE_NEXT_ROW;
                case KeyEvent.KEYCODE_DPAD_UP:
                    return VIRTUAL_KEY_CODE_PRE_ROW;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    return VIRTUAL_KEY_CODE_PRE_ONE;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return VIRTUAL_KEY_CODE_NEXT_ONE;
            }
        } else {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return VIRTUAL_KEY_CODE_NEXT_ONE;
                case KeyEvent.KEYCODE_DPAD_UP:
                    return VIRTUAL_KEY_CODE_PRE_ONE;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    return VIRTUAL_KEY_CODE_PRE_ROW;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return VIRTUAL_KEY_CODE_NEXT_ROW;
            }
        }
        return -1;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (getAdapter() == null || getAdapter().getItemCount() < 1 || getLayoutManager() == null) {
            return false;
        }

        int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (isDeleting) {
                        return true;
                    }
                    int virtualKeyCode = convertVirtualKeyCode(keyCode);
                    return move(virtualKeyCode, keyCode);
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (!isFlying && !isDeleting && mPosition < getAdapter().getItemCount()
                            && mOnItemClickListener != null && mPosition >= 0) {
                        mOnItemClickListener.onItemClick(this, getLayoutManager().findViewByPosition(mPosition), mPosition);
                    }
                    break;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (!isFlying)
                        scaleOut(findViewByPosition(mPosition), mPosition);
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /**
     * 根据虚拟按键的事件处理移动
     *
     * @param virtualKeyCode
     * @return
     */
    protected boolean move(int virtualKeyCode, int keyCode) {
        if (DEBUG) {
            Log.i(TAG, "move mLeftDistance:" + mLeftDistance);
            Log.i(TAG, "isFlying:" + isFlying);
            Log.i(TAG, "mPosition:" + mPosition);
        }

        if (isFlying && mLeftDistance == 0) {
            // 容错处理
            Log.w(TAG, "isFlying && mLeftDistance == 0 !!!");
            isFlying = false;
        }

        Status status = new Status();
        status.virtualKeyCode = virtualKeyCode;
        status.lastVirtualKeyCode = mLastVirtualKeyCode;
        mLastVirtualKeyCode = status.virtualKeyCode;

        status.initVisibleItemPosition(getLayoutManager());

        // 按着上下不动则启动fly
        if (status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ROW && status.lastVirtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ROW
                && isFlying && mLeftDistance >= 0 && mFlyDirection == DIRECTION_DOWN) {
            View lastView = status.getChildView(mPosition);
            if (lastView != null) {
                scaleIn(lastView, mPosition);
            }
            return flyDown();
        } else if (status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ROW
                && status.lastVirtualKeyCode == VIRTUAL_KEY_CODE_PRE_ROW && isFlying && mLeftDistance <= 0
                && mFlyDirection == DIRECTION_UP) {
            View lastView = status.getChildView(mPosition);
            if (lastView != null) {
                scaleIn(lastView, mPosition);
            }
            return flyUp();
        } else {
            status.itemCount = getAdapter().getItemCount();

            status.nextPosition = computeNextPosition(status);
            if (status.nextPosition == ERROR_POSITION) {
                // 顶部或底部
                if (DEBUG) {
                    Log.w(TAG, "ERROR_POSITION");
                }
                callScrollEndLintener(status, keyCode);
                return false;
            } else if (status.nextPosition == NONE_POSITION) {
                if (DEBUG) {
                    Log.w(TAG, "NONE_POSITION");
                }
                // 忽略这次按键相应
                return true;
            }

            status.needScrollDown = isNeedScrollDown(status);
            status.needScrollUp = isNeedScrollUp(status);

            if (DEBUG) {
                Log.i(TAG, "xxx nextPosition:" + status.nextPosition);
                Log.i(TAG, "xxx needScrollDown:" + status.needScrollDown);
                Log.i(TAG, "xxx needScrollUp:" + status.needScrollUp);
            }

            if (isScrolling) {
                if (processSpecialKeyEvent(status)) {
                    return true;
                }
                stopScroll();
            } else if (scrollToNotDisplayRow(status)) {
                Log.i(TAG, "scrollToNotDisplayRow");
                return true;
            }

            View lastView = status.getChildView(mPosition);
            if (lastView != null) {
                scaleIn(lastView, mPosition);
            }

            if (status.getNextChildView() == null) {
                Log.e(TAG, "next position view is null!" + status.nextPosition);
                return false;
            }

            scrollOneByOne(status);

            mPosition = status.nextPosition;
            mLastColumn = status.nextPosition % mColumnCount;
        }
        return true;
    }

    // 在滚动过程中处理一些特殊的按键事件(如长按，左右上下刹车等等)
    private boolean processSpecialKeyEvent(Status status) {
        if (waitScrolling
                /* 左右一直按的时候，突然直接按上下 */
                || ((status.lastVirtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ONE
                || status.lastVirtualKeyCode == VIRTUAL_KEY_CODE_PRE_ONE)
                && (status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ROW
                || status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ROW))
                /* 左右刹车 */
                || (status.lastVirtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ONE
                && status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ONE)
                || (status.lastVirtualKeyCode == VIRTUAL_KEY_CODE_PRE_ONE
                && status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ONE)) {
            if (DEBUG) {
                Log.i(TAG, "wait for scroll!");
            }
            waitScrolling = true;
            return true;
        } else if (status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ROW
                && status.lastVirtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ROW && status.needScrollDown
                && !isInBottomRow(status.nextPosition)) {
            // 只有上下滚动可以fly
            mFlyDirection = DIRECTION_DOWN;
            isFlying = true;
            isStartBind = false;

            if (status.getNextChildView() == null) {
                View lastView = status.getChildView(mPosition);
                if (lastView != null) {
                    scaleIn(lastView, mPosition);
                }
                return flyDown();
            }

            if (DEBUG) {
                Log.i(TAG, "press down to start fly down");
            }
            return false;
        } else if (status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ROW
                && status.lastVirtualKeyCode == VIRTUAL_KEY_CODE_PRE_ROW && status.needScrollUp
                && !isInTopRow(status.nextPosition)) {
            mFlyDirection = DIRECTION_UP;
            isFlying = true;
            isStartBind = false;

            if (status.getNextChildView() == null) {
                View lastView = status.getChildView(mPosition);
                if (lastView != null) {
                    scaleIn(lastView, mPosition);
                }
                return flyUp();
            }

            if (DEBUG) {
                Log.i(TAG, "press up to start fly up");
            }
            return false;
        } else if (!isFlying && (status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ONE
                || status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ONE) && status.getNextChildView() == null) {
            // 按着左右不动时可能nextPosition还未加载
            if (DEBUG) {
                Log.i(TAG, "right or left view not init");
            }
            return true;
        } else if (isFlying && (status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ONE
                || status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ONE)) {
            // 飞行过程中按左右
            // 这里设定成只能发生在gridlayout中，认为同一列item的高度是一样的
            mLastColumn = status.nextPosition % mColumnCount;
            View child = status.getNextChildView();
            if (child != null) {
                int offset = 0;
                if (mFlyDirection == DIRECTION_UP) {
                    offset = getViewTopLocation(child) - mLastViewTop;
                } else if (mFlyDirection == DIRECTION_DOWN) {
                    offset = getViewBottomLocation(child) - mLastViewBottom;
                }
                flowMoveTo(child, offset);
                if (DEBUG) {
                    Log.i(TAG, "flying press right or left");
                }
            }
            return true;
        } else if (isFlying && (mFlyDirection == DIRECTION_DOWN && status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ROW)
                || (mFlyDirection == DIRECTION_UP && status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ROW)) {
            // fly的时候按上下刹车
            stopScroll();
            mLeftDistance = 0;
            scrollExtraForEnd();
            if (DEBUG) {
                Log.i(TAG, "flying press up or down");
            }
            return true;
        } else if (isScrollDownToUp(status)) {
            // 上下键来回按
            if (DEBUG) {
                Log.i(TAG, "Scroll Down To Up");
            }
            waitScrolling = true;
            return true;
        } else if (isScrollUpToDown(status)) {
            // 上下键来回按
            if (DEBUG) {
                Log.i(TAG, "Scroll Up To Down");
            }
            waitScrolling = true;
            return true;
        } else if (status.getNextChildView() == null) {
            // 其他情况
            if (DEBUG) {
                Log.i(TAG, "other");
            }
            return true;
        } else {
            if (DEBUG) {
                Log.i(TAG, "no process");
            }
            return false;
        }
    }

    // 是否有新添加的item并且已经滚动到了新添加进来的item上
    protected boolean scrollToNotDisplayRow(Status status) {
        if (isFlying) {
            return false;
        }
        if (status.needScrollDown && (status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ROW
                || status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ONE)) {
            // 是否在显示的最后一行
            if (getRowNumber(mPosition) == getRowNumber(status.lastVisibleItemPositionInScreen)) {
                scrollToNotDisplayRow(status.firstVisibleItemPosition, status.nextPosition);
                return true;
            }
        } else if (status.needScrollUp && (status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ROW
                || status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ONE)) {
            if (getRowNumber(mPosition) == getRowNumber(status.firstVisibleItemPositionInScreen)) {
                View currView = getChildAt(mPosition - status.firstVisibleItemPosition);
                int viewHeight = getViewHeight(currView);
                int lastPosition = mPosition;
                // int center = getViewTopLocation(this) + getViewHeight(this) /
                // 2;
                // int offset = getViewTopLocation(currView) + viewHeight / 2
                // - center;
                // mLeftDistance += offset;
                int offset = 0;

                // 这里 认为都是等高的view
                mLeftDistance -= viewHeight + getItemSpaceTop() + getItemSpaceBottom();

                mLastViewTop = getViewTopLocation(currView) - offset;
                mLastViewBottom = getViewHeight(currView) + mLastViewTop;

                isFlying = true;
                mFlyDirection = DIRECTION_UP;
                isStartBind = false;

                mPosition = status.nextPosition;
                smoothScrollBy(mLeftDistance, "flyUp2");
                flowMoveTo(currView, offset);
                scaleIn(currView, lastPosition);
                return true;
            }
        }
        return false;
    }

    private void scrollToNotDisplayInScreenCenter(int firstVisibleItemPosition, int nextPosition) {
        int lastPosition = mPosition;
        View currView = getChildAt(lastPosition - firstVisibleItemPosition);
        int center = getViewTopLocation(this) + getViewHeight(this) / 2;
        int viewHeight = getViewHeight(currView);
        int offset = getViewTopLocation(currView) + viewHeight / 2 - center;
        mLeftDistance = offset;// 直接滚到中间，忽略mLeftDistance以前的值
        // 这里 认为都是等高的view
        mLeftDistance += viewHeight + getItemSpaceTop() + getItemSpaceBottom();

        mLastViewTop = center - viewHeight / 2;
        mLastViewBottom = center + viewHeight / 2;

        mFlyDirection = DIRECTION_DOWN;
        isFlying = true;
        isStartBind = false;

        mLastColumn = nextPosition % mColumnCount;
        int focusItemIndex = getRowNumber(lastPosition - firstVisibleItemPosition) * mColumnCount + mLastColumn;
        mPosition = nextPosition;
        smoothScrollBy(mLeftDistance, "flyToNotDisplayInScreenCenter");
        flowMoveTo(getChildAt(focusItemIndex), offset);
        scaleIn(currView, lastPosition);
    }

    private void scrollToNotDisplayRow(int firstVisibleItemPosition, int nextPosition) {
        int lastPosition = mPosition;
        View currView = getChildAt(lastPosition - firstVisibleItemPosition);

        // int offset = getViewHeight(currView) / 2 + getItemSpaceTop();
        // mLeftDistance += offset;
        int offset = 0;

        mLeftDistance += getViewHeight(currView) + getItemSpaceTop() + getItemSpaceBottom();

        mLastViewTop = getViewTopLocation(currView) - offset;
        mLastViewBottom = getViewHeight(currView) + mLastViewTop;

        isFlying = true;
        mFlyDirection = DIRECTION_DOWN;
        isStartBind = false;

        mLastColumn = nextPosition % mColumnCount;
        int focusItemIndex = getRowNumber(lastPosition - firstVisibleItemPosition) * mColumnCount + mLastColumn;
        mPosition = nextPosition;
        smoothScrollBy(mLeftDistance, "flyToNotDisplayRow");
        flowMoveTo(getChildAt(focusItemIndex), offset);
        scaleIn(currView, lastPosition);
    }

    private void callScrollEndLintener(Status status, int keyCode) {
        if (mOnScrollEndListener == null) {
            return;
        }
        if (status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ROW
                || (status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ONE && mPosition == status.itemCount - 1)) {
            mOnScrollEndListener.onScrollToBottom(keyCode);
        } else if (status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ROW
                || (status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ONE && mPosition == 0)) {
            mOnScrollEndListener.onScrollToTop(keyCode);
        }
    }

    /**
     * 计算nextPosition
     */
    private int computeNextPosition(Status status) {
        int nextPosition = 0;
        switch (status.virtualKeyCode) {
            case VIRTUAL_KEY_CODE_NEXT_ROW:
                if (isFlying && mFlyDirection == DIRECTION_UP) {
                    // 第二行
                    int index = mColumnCount + mLastColumn;
                    nextPosition = index + status.firstVisibleItemPosition;
                } else if (isFlying && mFlyDirection == DIRECTION_DOWN) {
                    int index = getRowsCount(getChildCount()) * mColumnCount - (mColumnCount - mLastColumn);
                    nextPosition = index + status.firstVisibleItemPosition;
                    if (nextPosition > status.itemCount - 1) {
                        nextPosition = status.itemCount - 1;
                    }
                } else {
                    if (mPosition > status.itemCount - 1 || isInBottomRow(mPosition)) {
                        return ERROR_POSITION;
                    }
                    nextPosition = mPosition + mColumnCount;
                    if (nextPosition > status.itemCount - 1) {
                        nextPosition = status.itemCount - 1;
                    }
                }
                break;
            case VIRTUAL_KEY_CODE_PRE_ROW:
                if (isFlying && mFlyDirection == DIRECTION_DOWN) {
                    // 按着下不动，突然按了上
                    // 倒数第二行
                    int index = (getRowsCount(getChildCount()) - 1) * mColumnCount - (mColumnCount - mLastColumn);
                    nextPosition = index + status.firstVisibleItemPosition;
                } else if (isFlying && mFlyDirection == DIRECTION_UP) {
                    // 第一行
                    int index = mLastColumn;
                    nextPosition = index + status.firstVisibleItemPosition;
                } else {
                    if (mPosition == 0 || isInTopRow(mPosition)) {
                        return ERROR_POSITION;
                    }
                    nextPosition = mPosition - mColumnCount;
                }
                break;
            case VIRTUAL_KEY_CODE_PRE_ONE:
                if (!isSupportVLeftKey && mLastColumn == 0) {
                    if (isFlying) {
                        return NONE_POSITION;
                    } else {
                        return ERROR_POSITION;
                    }
                }
                if (isFlying) {
                    int col = (mLastColumn + mColumnCount - 1) % mColumnCount;
                    if (mFlyDirection == DIRECTION_DOWN) {
                        // 倒数第二行
                        int index = (getRowsCount(getChildCount()) - 1) * mColumnCount - (mColumnCount - col);
                        nextPosition = index + status.firstVisibleItemPosition;
                    } else if (mFlyDirection == DIRECTION_UP) {
                        int index = mColumnCount + col;
                        nextPosition = index + status.firstVisibleItemPosition;
                    }
                } else if (mPosition == 0) {
                    return ERROR_POSITION;
                } else {
                    nextPosition = mPosition - 1;
                }
                break;
            case VIRTUAL_KEY_CODE_NEXT_ONE:
                if (!isSupportVRightKey && mLastColumn == mColumnCount - 1) {
                    if (isFlying) {
                        return NONE_POSITION;
                    } else {
                        return ERROR_POSITION;
                    }
                }
                if (isFlying) {
                    int col = (mLastColumn + mColumnCount + 1) % mColumnCount;
                    if (mFlyDirection == DIRECTION_DOWN) {
                        // 倒数第二行
                        int index = (getRowsCount(getChildCount()) - 1) * mColumnCount - (mColumnCount - col);
                        nextPosition = index + status.firstVisibleItemPosition;
                    } else if (mFlyDirection == DIRECTION_UP) {
                        int index = mColumnCount + col;
                        nextPosition = index + status.firstVisibleItemPosition;
                    }
                } else if (mPosition == status.itemCount - 1) {
                    return ERROR_POSITION;
                } else {
                    nextPosition = mPosition + 1;
                }
                break;
        }
        return nextPosition;
    }

    private boolean flyUp() {
        if (-mLeftDistance > getScrollDistance() * MAX_MULTIPLE_SPEED) {
            return true;
        }

        if (isScrolling) {
            stopScroll();
        }
        if (canScrollUp()) {
            mLeftDistance -= getScrollDistance();
            if (DEBUG) {
                Log.i(TAG, "up flying" + mLeftDistance);
            }
            smoothScrollBy(mLeftDistance, "flyUp");
        } else {
            // 已经到顶部
            if (DEBUG) {
                Log.i(TAG, "fly on top!");
            }
        }
        return true;
    }

    private boolean flyDown() {
        if (mLeftDistance > getScrollDistance() * MAX_MULTIPLE_SPEED) {
            return true;
        }

        if (isScrolling) {
            stopScroll();
        }
        if (canScrollDown()) {
            mLeftDistance += getScrollDistance();
            if (DEBUG) {
                Log.i(TAG, "down flying" + mLeftDistance);
            }
            smoothScrollBy(mLeftDistance, "flyDown");
        } else {
            // 已经到底部
            if (DEBUG) {
                Log.i(TAG, "fly on bottom!");
            }
        }
        return true;
    }

    protected void scrollOneByOne(Status status) {
        computeAndScroll(status);
        View view = status.getNextChildView();
        saveViewLocationBeforeFly(status.getNextViewIndex());
        // 左右滚动翻页处理 这里没有考虑gridview的大小不一样的问题,直接认为左右滚动的时候都是相同大小的
        if (DEBUG) {
            Log.i(TAG, "scroll one by one mLeftDistance:" + mLeftDistance);
        }
        flowMoveTo(view, mLeftDistance);
        if (isSelectAfterKeyUp || isKeyScrollToNextOne(status) || isKeyScrollToPreOne(status)) {
            if (getRowNumber(status.nextPosition) == getRowNumber(status.itemCount - 1)
                    && status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ROW) {
                scaleOut(status.getNextChildView(), status.nextPosition);
            } else if (getRowNumber(status.nextPosition) == 0 && status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ROW) {
                scaleOut(status.getNextChildView(), status.nextPosition);
            } else if (status.nextPosition == status.itemCount - 1 || status.nextPosition == 0) {
                scaleOut(status.getNextChildView(), status.nextPosition);
            }
        } else {
            scaleOut(status.getNextChildView(), status.nextPosition);
        }
    }

    private boolean isKeyScrollToNextOne(Status status) {
        return status.needScrollDown && (status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ROW
                || status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ONE);
    }

    private boolean isKeyScrollToPreOne(Status status) {
        return status.needScrollUp && (status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ROW
                || status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ONE);
    }

    // LinearLayourManager的findFirstCompletelyVisibleItemPosition不准确,故重写
    private int findFirstCompletelyVisibleItemPosition(int firstVisibleItemPosition) {
        int first = firstVisibleItemPosition;
        int itemCount = getAdapter().getItemCount();
        View child = null;
        while ((child = getChildAt(first - firstVisibleItemPosition)) == null
                || getViewTopLocation(child) < getViewTopLocation(this)) {
            first += mColumnCount;
            if (first > itemCount - 1) {
                return firstVisibleItemPosition;
            }
        }
        int firstCompletelyVisibleItemPosition = getRowNumber(first) * mColumnCount;
        return firstCompletelyVisibleItemPosition;
    }

    // LinearLayourManager的findLastCompletelyVisibleItemPosition不准确,故重写
    private int findLastCompletelyVisibleItemPosition(int firstVisibleItemPosition, int lastVisibleItemPosition) {
        int last = lastVisibleItemPosition;
        View child = null;
        while ((child = getChildAt(last - firstVisibleItemPosition)) == null
                || getViewBottomLocation(child) > getViewBottomLocation(this)) {
            last -= mColumnCount;
            if (last < 0) {
                return lastVisibleItemPosition;
            }
        }
        int lastCompletelyVisibleItemPosition = getRowNumber(last) * mColumnCount + mColumnCount - 1;
        return lastCompletelyVisibleItemPosition;
    }

    /**
     * @return 找到第一个显示的View位置，这里不使用LayoutManager中提供的类似方法，存在bug
     */
    public int findFirstVisibleItemPositionInScreen() {
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) getLayoutManager();
        int firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
        int firstCompletelyVisibleItemPosition = findFirstCompletelyVisibleItemPosition(firstVisibleItemPosition);
        if (firstVisibleItemPosition == NO_POSITION) {
            return NO_POSITION;
        }
        return findFirstVisibleItemPositionInScreen(firstVisibleItemPosition, firstCompletelyVisibleItemPosition);
    }

    /**
     * @return 找到最后一个显示的View位置，这里不使用LayoutManager中提供的类似方法，存在bug
     */
    public int findLastVisibleItemPositionInScreen() {
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) getLayoutManager();
        int firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
        if (firstVisibleItemPosition == NO_POSITION) {
            return NO_POSITION;
        }
        int lastVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition();
        int lastCompletelyVisibleItemPosition = findLastCompletelyVisibleItemPosition(firstVisibleItemPosition,
                lastVisibleItemPosition);
        return findLastVisibleItemPositionInScreen(firstVisibleItemPosition, lastVisibleItemPosition,
                lastCompletelyVisibleItemPosition);
    }

    /**
     * @return 找到第一个完全显示的View位置，这里不使用LayoutManager中提供的类似方法，存在bug
     */
    public int findFirstCompletelyVisibleItemPosition() {
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) getLayoutManager();
        int firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
        int firstCompletelyVisibleItemPosition = findFirstCompletelyVisibleItemPosition(firstVisibleItemPosition);
        if (firstVisibleItemPosition == NO_POSITION) {
            return NO_POSITION;
        }
        return firstCompletelyVisibleItemPosition;
    }

    /**
     * @return 找到最后一个完全显示的View位置，这里不使用LayoutManager中提供的类似方法，存在bug
     */
    public int findLastCompletelyVisibleItemPosition() {
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager) getLayoutManager();
        int firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
        if (firstVisibleItemPosition == NO_POSITION) {
            return NO_POSITION;
        }
        int lastVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition();
        int lastCompletelyVisibleItemPosition = findLastCompletelyVisibleItemPosition(firstVisibleItemPosition,
                lastVisibleItemPosition);
        return lastCompletelyVisibleItemPosition;
    }

    private int findFirstVisibleItemPositionInScreen(int firstVisibleItemPosition,
                                                     int firstCompletelyVisibleItemPosition) {
        int firstVisibleItemPositionInScreen = firstVisibleItemPosition;
        while (getChildAt(firstVisibleItemPositionInScreen - firstVisibleItemPosition) != null && getViewBottomLocation(
                getChildAt(firstVisibleItemPositionInScreen - firstVisibleItemPosition)) <= getViewTopLocation(this)
                && firstVisibleItemPositionInScreen < firstCompletelyVisibleItemPosition) {
            if (DEBUG) {
                Log.i(TAG, "findFirstVisibleItemPositionInScreen, view is not in screen" + firstVisibleItemPosition);
            }
            firstVisibleItemPositionInScreen += mColumnCount;
        }
        return firstVisibleItemPositionInScreen;
    }

    private int findLastVisibleItemPositionInScreen(int firstVisibleItemPosition, int lastVisibleItemPosition,
                                                    int lastCompletelyVisibleItemPosition) {
        int lastVisibleItemPositionInScreen = lastVisibleItemPosition;
        while (getChildAt(lastVisibleItemPositionInScreen - firstVisibleItemPosition) != null
                && getViewTopLocation(getChildAt(
                lastVisibleItemPositionInScreen - firstVisibleItemPosition)) >= getViewBottomLocation(this)
                && lastVisibleItemPositionInScreen > lastCompletelyVisibleItemPosition) {
            if (DEBUG) {
                Log.i(TAG, "findLastVisibleItemPositionInScreen, view is not in screen" + lastVisibleItemPosition);
            }
            lastVisibleItemPositionInScreen -= mColumnCount;
        }
        return lastVisibleItemPositionInScreen;
    }

    private boolean isInBottomRow(int position) {
        int bottomRow = (getAdapter().getItemCount() - 1) / mColumnCount;
        int pRow = position / mColumnCount;
        return pRow == bottomRow;
    }

    private boolean isInTopRow(int position) {
        return position < mColumnCount;
    }

    private int getTotalRows(Status status) {
        return (status.itemCount - 1) / mColumnCount + 1;
    }

    private int getRowNumber(int position) {
        return position / mColumnCount;
    }

    private int getRowsCount(int totalCount) {
        return (totalCount - 1) / mColumnCount + 1;
    }

    private boolean computeAndScroll(Status status) {
        boolean isNeedScroll = false;
        switch (status.virtualKeyCode) {
            case VIRTUAL_KEY_CODE_NEXT_ROW:
                if (status.needScrollDown) {
                    isNeedScroll = scrollDown(status);
                }
                break;
            case VIRTUAL_KEY_CODE_PRE_ROW:
                if (status.needScrollUp) {
                    isNeedScroll = scrollUp(status);
                }
                break;
            case VIRTUAL_KEY_CODE_PRE_ONE:
                if (status.nextPosition % mColumnCount == mColumnCount - 1) {
                    if (status.needScrollUp) {
                        isNeedScroll = scrollUp(status);
                    }
                } else {
                    smoothScrollBy(mLeftDistance, "move left");
                }
                break;
            case VIRTUAL_KEY_CODE_NEXT_ONE:
                if (status.nextPosition % mColumnCount == 0) {
                    if (status.needScrollDown) {
                        isNeedScroll = scrollDown(status);
                    }
                } else {
                    smoothScrollBy(mLeftDistance, "move right");
                }
                break;
        }
        return isNeedScroll;
    }

    private boolean scrollUp(Status status) {
        // Log.i(TAG, "scrollUp:" + nextPosition + " mLeftDistance : "
        // + mLeftDistance);
        View child = status.getNextChildView();
        if (child != null) {
            if (isInTopRow(status.nextPosition)) {
                if (canScrollUp()) {
                    int top = getItemSpaceTop() + getRecyclerPaddingTop();
                    mLeftDistance = getViewTopLocation(child) - getViewTopLocation(this) - top;
                    smoothScrollBy(mLeftDistance, "top");
                }
                scaleOut(child, status.nextPosition);
            } else {
                if (isNeedScrollToCenter()) {
                    // 后面是按实时位置算的，所以清零
                    mLeftDistance = 0;
                    mLeftDistance -= (getViewTopLocation(this) + getViewHeight(this) / 2)
                            - (getViewTopLocation(child) + getViewHeight(child) / 2);
                    if (Math.abs(mLeftDistance) > computeScrollOffset()) {
                        mLeftDistance = -computeScrollOffset();
                    }
                } else {
                    mLeftDistance -= getViewHeight(child) + getItemSpaceTop() + getItemSpaceBottom();
                }
                smoothScrollBy(mLeftDistance, "scrollUp");
            }
            return true;
        } else {
            Log.e(TAG, "scrollUp child is null!" + status.nextPosition);
            return false;
        }
    }

    private boolean scrollDown(Status status) {
        // Log.i(TAG, "scrollDown:" + nextPosition + " mLeftDistance : "
        // + mLeftDistance);
        View child = status.getNextChildView();
        if (child != null) {
            if (isInBottomRow(status.nextPosition)) {
                if (canScrollDown()) {
                    int bottom = getItemSpaceBottom() + getRecyclerPaddingBottom();
                    mLeftDistance = getViewBottomLocation(child) - getViewBottomLocation(this) + bottom;
                    smoothScrollBy(mLeftDistance, "bottom");
                }
                scaleOut(child, status.nextPosition);
            } else {
                if (isNeedScrollToCenter()) {
                    // 后面是按实时位置算的，所以清零
                    mLeftDistance = 0;
                    mLeftDistance += getViewTopLocation(child) + getViewHeight(child) / 2
                            - (getViewTopLocation(this) + getViewHeight(this) / 2);
                    // 这里默认为高度相等的itemView
                    int left = getLeftScroll(status, child);
                    if (left > 0 && Math.abs(mLeftDistance) > left) {
                        mLeftDistance = left;
                    }
                } else {
                    mLeftDistance += getViewHeight(child) + getItemSpaceTop() + getItemSpaceBottom();
                }
                smoothScrollBy(mLeftDistance, "scrollDown");
            }
            return true;
        } else {
            Log.e(TAG, "scrollDown child is null!" + status.nextPosition);
            return false;
        }
    }

    private int getRecyclerPaddingTop() {
        if (mOrientation == VERTICAL) {
            return paddingTop;
        } else {
            return paddingLeft;
        }
    }

    private int getRecyclerPaddingBottom() {
        if (mOrientation == VERTICAL) {
            return paddingBottom;
        } else {
            return paddingRight;
        }
    }

    private boolean isNeedScrollToCenter() {
        return mScrollType == SCROLL_TYPE_ALWAYS_CENTER;
    }

    private boolean isScrollOnLastEnable() {
        return mScrollType == SCROLL_TYPE_ON_LAST;
    }

    /**
     * 是否需要向下滚
     */
    private boolean isNeedScrollDown(Status status) {
        if (status.nextPosition > status.lastCompletelyVisibleItemPosition
                || (!isScrollOnLastEnable() && status.nextPosition <= status.lastCompletelyVisibleItemPosition
                && status.lastCompletelyVisibleItemPosition == status.lastVisibleItemPositionInScreen
                && /* 当前显示的倒数第二行 */(getRowNumber(status.nextPosition) == getRowNumber(
                status.lastVisibleItemPositionInScreen)))
                || isNeedScrollDownToCenter(status)) {
            if (getRowNumber(mPosition) != getRowNumber(status.nextPosition) && canScrollDown()) {
                // 和以前一个不在同一行
                return true;
            }
        }
        return false;
    }

    private boolean isNeedScrollDownToCenter(Status status) {
        View child = status.getNextChildView();
        if (child == null) {
            return false;
        }
        int center = getViewTopLocation(this) + getViewHeight(this) / 2;
        int next = getViewTopLocation(child) + getViewHeight(child) / 2;
        return isNeedScrollToCenter() && next > center;
    }

    /**
     * 是否需要向上滚
     */
    private boolean isNeedScrollUp(Status status) {
        if (status.nextPosition < status.firstCompletelyVisibleItemPosition
                || (!isScrollOnLastEnable() && status.nextPosition >= status.firstCompletelyVisibleItemPosition
                && status.firstCompletelyVisibleItemPosition == status.firstVisibleItemPositionInScreen
                && /* 当前显示的第二行 */(getRowNumber(status.nextPosition) == getRowNumber(
                status.firstVisibleItemPositionInScreen)))
                || isNeedScrollUpToCenter(status)) {
            if (getRowNumber(mPosition) != getRowNumber(status.nextPosition) && canScrollUp()) {
                return true;
            }
        }
        return false;
    }

    private boolean isNeedScrollUpToCenter(Status status) {
        View child = status.getNextChildView();
        if (child == null) {
            return false;
        }
        int center = getViewTopLocation(this) + getViewHeight(this) / 2;
        int next = getViewTopLocation(child) + getViewHeight(child) / 2;
        return isNeedScrollToCenter() && next < center;
    }

    /**
     * 下滚的时候，按上键
     */
    private boolean isScrollDownToUp(Status status) {
        boolean b = !isFlying && (status.virtualKeyCode == VIRTUAL_KEY_CODE_PRE_ROW
                && status.lastVirtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ROW);
        return b;
    }

    /**
     * 上滚的时候，按下键
     */
    private boolean isScrollUpToDown(Status status) {
        boolean b = !isFlying && (status.virtualKeyCode == VIRTUAL_KEY_CODE_NEXT_ROW
                && status.lastVirtualKeyCode == VIRTUAL_KEY_CODE_PRE_ROW);
        return b;
    }

    private void saveViewLocationBeforeFly(View view) {
        // 算上滚动的偏移
        mLastViewTop = getViewTopLocation(view) - mLeftDistance;
        mLastViewBottom = getViewHeight(view) + mLastViewTop;
        if (DEBUG) {
            Log.i(TAG, "saveViewLocationBeforeFly mLastViewTop:" + mLastViewTop);
            Log.i(TAG, "saveViewLocationBeforeFly mLastViewBottom:" + mLastViewBottom);
        }
    }

    private void saveViewLocationBeforeFly(int index) {
        // Log.i(TAG, "saveViewLocationBeforeFly index:" + index);
        View view = getChildAt(index);
        if (view != null) {
            saveViewLocationBeforeFly(view);
        } else {
            Log.e(TAG, "saveViewLocationBeforeFly view is null!");
        }
    }

    // 把最终状态给最近的view
    private void setStatusByNearestView() {
        if (mLastViewBottom != -1 && mFlyDirection == DIRECTION_DOWN) {
            int childCount = getChildCount();
            int nearestBottom = Integer.MAX_VALUE;
            int index = -1;
            for (int i = 0; i < childCount; i++) {
                if (i % mColumnCount == 0) {
                    View child = getChildAt(i);
                    int bottom = getViewBottomLocation(child);
                    if (Math.abs(bottom - mLastViewBottom) < nearestBottom) {
                        nearestBottom = Math.abs(bottom - mLastViewBottom);
                        index = i;
                    }
                }
            }
            if (index >= 0) {
                setFinalStatus(index);
            }

        } else if (mLastViewTop != -1 && mFlyDirection == DIRECTION_UP) {
            int nearestTop = Integer.MAX_VALUE;
            int index = -1;
            int childCount = getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                if (i % mColumnCount == mLastColumn) {
                    View child = getChildAt(i);
                    int viewTopLocation = getViewTopLocation(child);
                    if (Math.abs(viewTopLocation - mLastViewTop) < nearestTop) {
                        nearestTop = Math.abs(viewTopLocation - mLastViewTop);
                        index = i;
                    }
                }
            }
            if (index >= 0) {
                setFinalStatus(index);
            }
        }
    }

    // 找到fly时保存的离mLastViewBottom 或 mLastViewTop 最近的view
    private void scrollExtraForEnd() {
        if (mLastViewBottom != -1 && mFlyDirection == DIRECTION_DOWN) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (i % mColumnCount == 0) {
                    View child = getChildAt(i);
                    int bottom = getViewBottomLocation(child);
                    // 考虑到误差
                    if (DEBUG) {
                        Log.i(TAG, "bottom:" + bottom);
                        Log.i(TAG, "mLastViewBottom:" + mLastViewBottom);
                    }
                    if (isEquals(bottom, mLastViewBottom)) {
                        // 解决一直横向滚动找不到最后一个的情况
                        int index = i + mLastColumn;
                        if (index > childCount - 1) {
                            index = childCount - 1;
                        }
                        setFinalStatus(index);
                        break;
                    } else if (bottom > mLastViewBottom + DEVIATION) {
                        if (DEBUG) {
                            Log.i(TAG, "scrollExtraForEnd bottom:" + bottom);
                            Log.i(TAG, "scrollExtraForEnd mLastViewBottom:" + mLastViewBottom);
                        }
                        mLeftDistance = bottom - mLastViewBottom;
                        smoothScrollBy(mLeftDistance, "scrollExtraForEnd down");
                        break;
                    }
                }
            }
        } else if (mLastViewTop != -1 && mFlyDirection == DIRECTION_UP) {
            int childCount = getChildCount();
            for (int i = childCount - 1; i >= 0; i--) {
                if (i % mColumnCount == mLastColumn) {
                    View child = getChildAt(i);
                    int viewTopLocation = getViewTopLocation(child);
                    // 考虑到误差
                    if (DEBUG) {
                        Log.i(TAG, "viewTopLocation:" + viewTopLocation);
                        Log.i(TAG, "mLastViewTop:" + mLastViewTop);
                    }
                    if (viewTopLocation + DEVIATION < mLastViewTop) {
                        if (DEBUG) {
                            Log.i(TAG, "scrollExtraForEnd top:" + viewTopLocation);
                            Log.i(TAG, "scrollExtraForEnd mLastViewTop:" + mLastViewTop);
                        }
                        mLeftDistance = viewTopLocation - mLastViewTop;
                        smoothScrollBy(mLeftDistance, "scrollExtraForEnd up");
                        break;
                    } else if (isEquals(viewTopLocation, mLastViewTop)) {
                        setFinalStatus(i);
                        break;
                    }
                }
            }
        }
    }

    // 考虑到计算的误差
    private boolean isEquals(int a, int b) {
        return Math.abs(a - b) <= DEVIATION;
    }

    private void setFinalStatus(int index) {
        mLeftDistance = 0;
        View view = getChildAt(index);
        if (view != null) {
            mFlyDirection = DIRECTION_NONE;
            isFlying = false;
            int position = getChildAdapterPosition(view);
            flowMoveTo(view, 0);
            scaleOut(view, position);
            mPosition = position;
            mLastColumn = position % mColumnCount;
            saveViewLocationBeforeFly(index);
            bindView();

            if (DEBUG) {
                Log.i(TAG, "setFinalStatus mPosition:" + position);
            }
        } else {
            Log.e(TAG, "setFinalStatus view is null!");
        }
    }

    protected void scaleOut(View view, int position) {
        if (view == null || !isFocus) {
            return;
        }

        // Log.i(TAG, "scaleOut:" + position);

        if (isFocusViewOnFrontEnable && frontView != view) {
            frontView = view;
            invalidate();
        }

        if (mInAnimator != null && mLastAnimInPosition == position) {
            mInAnimator.cancel();
        }

        if (mLastAnimOutPosition == position && mLastAnimInPosition != position) {
            return;
        } else {
            if (isScale) {
                if (view.getScaleX() != SCALE || view.getScaleY() != SCALE) {
                    mOutAnimator = ObjectAnimator.ofPropertyValuesHolder(view, mScaleOutX, mScaleOutY);
                    mOutAnimator.setDuration(SCALE_OUT_DURATION);
                    mOutAnimator.start();
                }
            }

            mLastAnimOutPosition = position;
            if (DEBUG) {
                Log.i(TAG, "scaleOut:" + position);
            }
            selectView(view);
            if (mOnItemFocusListener != null) {
                mOnItemFocusListener.onItemFocus(this, view, position, getAdapter().getItemCount());
            }
        }
    }

    protected void selectView(View view) {
        if (!isAlwaysSelected && isTouchMode) {
            return;
        }
        if (view instanceof ISelectedStatus) {
            ((ISelectedStatus) view).setSelected(true, isFocus);
        }
        view.setSelected(true);
    }

    protected void scaleIn(View view, int position, boolean isClearSelected) {
        if (view == null) {
            return;
        }

        if (position != mLastAnimOutPosition) {
            return;
        }

        if (mOutAnimator != null && mLastAnimOutPosition == position) {
            mOutAnimator.cancel();
        }

        if (mLastAnimInPosition == position && mLastAnimOutPosition != position) {
            return;
        } else {
            if (isScale) {
                if (view.getScaleX() != 1.0f || view.getScaleY() != 1.0f) {
                    mInAnimator = ObjectAnimator.ofPropertyValuesHolder(view, mScaleInX, mScaleInY);
                    mInAnimator.setDuration(SCALE_IN_DURATION);
                    mInAnimator.start();
                }
            }
            mLastAnimInPosition = position;
            if (DEBUG) {
                Log.i(TAG, "scaleIn:" + position);
            }

            if (isClearSelected) {
                leaveView(view);
            } else {
                selectView(view);
            }
        }
    }

    protected void scaleIn(View view, int position) {
        scaleIn(view, position, true);
    }

    protected void leaveView(View view) {
        if (!isAlwaysSelected && isTouchMode) {
            return;
        }
        if (view instanceof ISelectedStatus) {
            ((ISelectedStatus) view).setSelected(false, isFocus);
        }
        view.setSelected(false);
    }

    @Override
    public void smoothScrollBy(int dx, int dy) {
        super.smoothScrollBy(dx, dy);
    }

    public void scrollBy(int distance) {
        smoothScrollBy(distance, 0, "scrollBy");
    }

    private void smoothScrollBy2(int distance) {
        if (DEBUG) {
            Log.i(TAG, "smoothScrollBy2:" + distance);
        }
        if (mOrientation == VERTICAL) {
            smoothScrollBy(0, distance);
        } else {
            smoothScrollBy(distance, 0);
        }
    }

    private void smoothScrollBy(int distance, int duration, String tag) {
        if (DEBUG) {
            Log.i(TAG, "smoothScrollBy<<<" + tag + ">>>" + distance);
        }
        if (distance == 0) {
            return;
        }

        if (mSmoothScrollMethod == null || mViewFlinger == null) {
            smoothScrollBy2(distance);
            return;
        }

        try {
            if (mOrientation == VERTICAL) {
                mSmoothScrollMethod.invoke(mViewFlinger, 0, distance, duration, mInterpolator);
            } else {
                mSmoothScrollMethod.invoke(mViewFlinger, distance, 0, duration, mInterpolator);
            }

        } catch (Exception e) {
            e.printStackTrace();
            smoothScrollBy2(distance);
        }
    }

    private void smoothScrollBy(int distance, String tag) {
        if (mOrientation == VERTICAL) {
            smoothScrollBy(distance, computeScrollDuration(0, distance, 0, 0), tag);
        } else {
            smoothScrollBy(distance, computeScrollDuration(distance, 0, 0, 0), tag);
        }
    }

    protected int computeScrollDuration(int dx, int dy, int vx, int vy) {
        final int absDx = Math.abs(dx);
        final int absDy = Math.abs(dy);
        final boolean horizontal = absDx > absDy;
        final int velocity = (int) Math.sqrt(vx * vx + vy * vy);
        final int delta = (int) Math.sqrt(dx * dx + dy * dy);
        final int containerSize = horizontal ? getWidth() : getHeight();
        final int halfContainerSize = containerSize / 2;
        final float distanceRatio = Math.min(1.f, 1.f * delta / containerSize);
        final float distance = halfContainerSize + halfContainerSize * distanceInfluenceForSnapDuration(distanceRatio);

        int duration;
        if (velocity > 0) {
            duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
        } else {
            float absDelta = (float) (horizontal ? absDx : absDy);
            duration = (int) (((absDelta / containerSize) + 1) * 300);
        }

        duration = Math.min(duration, 2000);
        if (DEBUG) {
            Log.i(TAG, "duration:" + duration);
        }
        return duration;
    }

    private float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

    private final class MetroItemAnimator extends DefaultItemAnimator {

        public MetroItemAnimator() {
            super();
        }

        @Override
        public boolean animateChange(ViewHolder oldHolder, ViewHolder newHolder, int fromX, int fromY, int toX,
                                     int toY) {
            // change的时候保持原来放大的动画
            float scaleX = oldHolder.itemView.getScaleX();
            float scaleY = oldHolder.itemView.getScaleY();
            newHolder.itemView.setScaleX(scaleX);
            newHolder.itemView.setScaleY(scaleY);
            return super.animateChange(oldHolder, newHolder, fromX, fromY, toX, toY);
        }
    }

    /**
     * 设置每个item上下左右的间距
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    public void setItemSpaces(int left, int top, int right, int bottom) {
        if (mSpacesRect == null) {
            mSpacesRect = new Rect(left, top, right, bottom);
            super.addItemDecoration(new SpacesItemDecoration(mSpacesRect));
        }
    }

    private class SpacesItemDecoration extends ItemDecoration {
        private Rect rect;

        public SpacesItemDecoration(Rect rect) {
            this.rect = rect;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.left = rect.left;
            outRect.top = rect.top;
            outRect.right = rect.right;
            outRect.bottom = rect.bottom;

            if (paddingLeft == 0 && paddingRight == 0 && paddingTop == 0 && paddingBottom == 0) {
                return;
            }

            int position = parent.getChildAdapterPosition(view);
            int count = getAdapter().getItemCount();
            int clunmCount = Math.min(mColumnCount, count);
            if (mOrientation == VERTICAL) {
                if (position < clunmCount) {
                    outRect.top += paddingTop;
                }

                if (position % clunmCount == 0) {
                    outRect.left += paddingLeft;
                }

                if (position % clunmCount == clunmCount - 1) {
                    outRect.right += paddingRight;
                }

                if (position >= getRowsCount(count, clunmCount) * clunmCount - clunmCount) {
                    outRect.bottom += paddingBottom;
                }

            } else {
                if (position < clunmCount) {
                    outRect.left += paddingLeft;
                }

                if (position % clunmCount == 0) {
                    outRect.top += paddingTop;
                }

                if (position % clunmCount == clunmCount - 1) {
                    outRect.bottom += paddingBottom;
                }

                if (position >= getRowsCount(count, clunmCount) * clunmCount - clunmCount) {
                    outRect.right += paddingRight;
                }
            }
        }

        private int getRowsCount(int totalCount, int clunmCount) {
            return (totalCount - 1) / clunmCount + 1;
        }
    }

    public static class MetroGridLayoutManager extends GridLayoutManager {
        private int measuredWidth = 0;
        private int measuredHeight = 0;
        private boolean isWholeMeasuer = false;

        public MetroGridLayoutManager(Context context, int spanCount, int orientation) {
            super(context, spanCount, orientation, false);
        }

        /*
         * 不支持翻转
         */
        @Override
        @Deprecated
        public void setReverseLayout(boolean reverseLayout) {
            super.setReverseLayout(false);
        }

        /**
         * @param b 是否打开全局测量
         *          (用与自动测量recyclerview的高度或宽度) 全局测量效率较低
         */
        public void setWhileMeasuer(boolean b) {
            this.isWholeMeasuer = b;
        }

        @Override
        public void onMeasure(Recycler recycler, State state, int widthSpec, int heightSpec) {
            if (state.getItemCount() == 0) {
                super.onMeasure(recycler, state, widthSpec, heightSpec);
            } else {
                if (getOrientation() == RecyclerView.VERTICAL) {
                    measuredHeight = MeasureSpec.getSize(heightSpec);
                    if (measuredWidth == 0) {
                        View view = recycler.getViewForPosition(0);
                        if (view != null) {
                            measureChild(view, widthSpec, heightSpec);
                            measuredWidth = (view.getMeasuredWidth() + getLeftDecorationWidth(view)
                                    + getRightDecorationWidth(view)) * getSpanCount();
                        }
                        measuredWidth += getPaddingLeft() + getPaddingRight();
                    }
                } else {
                    measuredWidth = MeasureSpec.getSize(widthSpec);
                    if (measuredHeight == 0) {
                        if (isWholeMeasuer) {
                            int count = Math.min(getItemCount(), getSpanCount());
                            for (int i = 0; i < count; i++) {
                                View view = recycler.getViewForPosition(i);
                                measureChild(view, widthSpec, heightSpec);
                                measuredHeight += (view.getMeasuredHeight() + getTopDecorationHeight(view)
                                        + getBottomDecorationHeight(view));
                            }
                        } else {
                            View view = recycler.getViewForPosition(0);
                            if (view != null) {
                                measureChild(view, widthSpec, heightSpec);
                                measuredHeight = (view.getMeasuredHeight() + getTopDecorationHeight(view)
                                        + getBottomDecorationHeight(view)) * getSpanCount();
                            }
                        }
                    }
                }
                setMeasuredDimension(measuredWidth, measuredHeight);
            }
        }
    }

    public static abstract class MetroAdapter<VH extends MetroViewHolder> extends Adapter<VH> {
        private MetroRecyclerView mRecyclerView;

        /**
         * 绑定预先加载的数据到view上，非延迟加载的
         *
         * @param holder
         * @param position
         */
        public abstract void onPrepareBindViewHolder(VH holder, int position);

        /**
         * 绑定延迟加载的数据到view上，如图片
         *
         * @param holder
         * @param position
         */
        public abstract void onDelayBindViewHolder(VH holder, int position);

        /**
         * 解除绑定延迟加载的数据到释放内存，如图片
         *
         * @param holder
         */
        public abstract void onUnBindDelayViewHolder(VH holder);

        /**
         * 把onBindViewHolder拆解成了onPrepareBindViewHolder和onDelayBindViewHolder两步执行
         * 子类不需要重载这个方法，只需重载onPrepareBindViewHolder和onDelayBindViewHolder
         */
        @Override
        public final void onBindViewHolder(VH holder, int position) {
            //android8.0以上的系统，焦点会默认导itemview上导致错误，这里强制让itemview不能获取焦点
            holder.itemView.setFocusable(false);
            holder.setOnItemClickListner(mRecyclerView.mOnItemClickListener, mRecyclerView);
            holder.setOnItemLongClickListner(mRecyclerView.mOnItemLongClickListener, mRecyclerView);

            if (!holder.isLoadData) {
                holder.isLoadData = true;
                onPrepareBindViewHolder(holder, position);
            }

            if (mRecyclerView.isNeedBind()) {
                holder.isReLoadExtra = false;
                onDelayBindViewHolder(holder, position);
                holder.isLoadDefaultDelayData = false;
            } else if (mRecyclerView.isDelayBindEnable() && !holder.isLoadDefaultDelayData) {
                onUnBindDelayViewHolder(holder);
                holder.isLoadDefaultDelayData = true;
                holder.isReLoadExtra = true;
            }
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            this.mRecyclerView = (MetroRecyclerView) recyclerView;
        }

        @Override
        public void onViewRecycled(VH holder) {
            super.onViewRecycled(holder);
            holder.isLoadData = false;
            holder.isReLoadExtra = true;
            if (mRecyclerView.isDelayBindEnable() && !holder.isLoadDefaultDelayData) {
                onUnBindDelayViewHolder(holder);
                holder.isLoadDefaultDelayData = true;
            }
        }

        @Override
        public void onViewAttachedToWindow(VH holder) {
            super.onViewAttachedToWindow(holder);
            if (holder.isReLoadExtra && mRecyclerView.isNeedBind()) {
                int position = holder.getLayoutPosition();
                holder.isReLoadExtra = false;
                onDelayBindViewHolder(holder, position);
                holder.isLoadDefaultDelayData = false;
            }
        }
    }

    public static abstract class MetroViewHolder extends ViewHolder implements OnClickListener, OnLongClickListener {
        boolean isLoadData = false;
        boolean isReLoadExtra = false;
        boolean isLoadDefaultDelayData = false;
        private MetroItemClickListener itemClickListener;
        private MetroItemLongClickListener itemLongClickListener;
        private View parentView;

        public MetroViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        public void setOnItemClickListner(MetroItemClickListener listener, View parentView) {
            if (this.itemClickListener == null) {
                this.itemClickListener = listener;
                this.parentView = parentView;
            }
        }

        public void setOnItemLongClickListner(MetroItemLongClickListener listener, View parentView) {
            if (this.itemLongClickListener == null) {
                this.itemLongClickListener = listener;
                this.parentView = parentView;
            }
        }

        @Override
        public void onClick(View v) {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(parentView, v, getLayoutPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (itemLongClickListener != null) {
                return itemLongClickListener.onItemLongClick(parentView, v, getLayoutPosition());
            } else {
                return false;
            }
        }
    }

    public static interface ISelectedStatus {
        public void setSelected(boolean selected, boolean hasFocus);
    }

}
