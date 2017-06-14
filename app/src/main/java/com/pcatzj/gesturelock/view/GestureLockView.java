package com.pcatzj.gesturelock.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Vibrator;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.pcatzj.gesturelock.R;
import com.pcatzj.gesturelock.util.DisplayUtils;
import com.pcatzj.gesturelock.util.ResourceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 类  名： GestureLockView<br/>
 * 包  名： com.htsc.android.mcrm.view.custom<br/>
 * 描  述： 图案锁<br/>
 * 日  期： 2017-04-25 14:45<br/>
 *
 * @author pcatzj <br/>
 */
public class GestureLockView extends View {

    // item图标
    private Bitmap mLockBitmap;

    // 控件高度
    private int mHeight;
    // 控件宽度
    private int mWidth;
    // 左边距
    private int mPaddingLeft;
    // 上边距
    private int mPaddingTop;
    // 右边距
    private int mPaddingRight;
    // 下边距
    private int mPaddingBottom;
    // 每个item的水平间隔
    private int mSpaceHorizontal;
    // 每个item的垂直间隔
    private int mSpaceVertical;
    // 触控点坐标记录变量
    private float mTouchPointX = -1, mTouchPointY = -1;
    // 点与点的最小间隔
    private int mMinSpace = DisplayUtils.dip2px(getContext(), 4);

    /**
     * 属性变量
     */
    // 控件边长（item数量）
    private int mCountSide = 3;
    // 最少的点生效数
    private int mMinEffectiveLockCount = 4;
    // 绘制图形时的线条颜色
    @ColorInt
    private int mDrawingColor = 0xffffff00;
    // 绘制完成后的线条颜色
    @ColorInt
    private int mEffectiveColor = 0xff00ff00;
    // 图形不符合最低点数或者错误时的错误色
    @ColorInt
    private int mNoneffectiveColor = 0xffff0000;
    // 图形锁图案自动消失时间.0为立马消失，小于0为永不自动消失
    private long mDurationPatternDisappear = 1_000;
    // 图形锁错误时图案的自动消失时间.0为立马消失，小于0为永不自动消失
    private long mDurationErrorPatternDisappear = 1_000;
    // 是否只有触控点接触到每个可checked的Lock时才会checked
    private boolean mOnlyCheckedUnderTouch = true;
    // 是否绘制点与点之间的线条
    private boolean mShowLine = true;
    // 线的宽度（单位：dp）
    private int mLineWidthDp = DisplayUtils.dip2px(getContext(), 8);
    // Lock图案
    private Drawable mLockDrawable;
    private StateListDrawable mLockStateListDrawable;
    // 图标宽度
    private int mLockWidth;
    // 图标高度
    private int mLockHeight;
    // Lock的长
    // 是否在每个checked的点位置画一个圆
    private boolean mDrawAnchorPoint = false;
    // 是否绘制锚点阴影
    private boolean mDrawAnchorShadow = false;
    // 锚点阴影的半径
    private int mAnchorShadowRadius = DisplayUtils.dip2px(getContext(), 32);
    // 每个点位置圆的半径
    private float mCheckedCircleRadius = DisplayUtils.dip2px(getContext(), 16);

    // 是否可进行手势操作（针对PreviewMode）
    private boolean mTouchable = true;

    private int mGestureMode = GestureMode.MODE_TRAVERSER;
    private int mVerifyTimes = 2;

    private Paint mPaint;

    // 记录每个点选中状态的数组
    private boolean[][] mPointCheckedStateArray;
    // 记录点的check次序
    private ArrayList<Point> mCheckedOrder;
    // 临时存储密码的变量
    private String mTempPassword;

    private int[] mStateChecked = new int[]{android.R.attr.state_checked};
    private boolean mChecked = false;

    // 震动传感器
    private Vibrator mVibrator;

    // 手势锁自定义事件
    private GestureEvent mGestureEvent;

    private Context mContext = this.getContext();

    public GestureLockView(Context context) {
        super(context);
        init();
    }

    public GestureLockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.GestureLockView);

        mCountSide = ta.getInt(R.styleable.GestureLockView_countSide, 3);
        mMinEffectiveLockCount = ta.getInt(R.styleable.GestureLockView_minEffectiveLockCount, 4);
        mDrawingColor = ta.getColor(R.styleable.GestureLockView_drawingColor, 0xffffff00);
        mEffectiveColor = ta.getColor(R.styleable.GestureLockView_effectiveColor, 0xff00ff00);
        mDurationPatternDisappear =
                        ta.getInt(R.styleable.GestureLockView_durationPatternDisappear, 1_000);
        mDurationErrorPatternDisappear =
                        ta.getInt(R.styleable.GestureLockView_durationErrorPatternDisappear, 1_000);
        mNoneffectiveColor = ta.getColor(R.styleable.GestureLockView_noneffectiveColor, 0xffff0000);
        mOnlyCheckedUnderTouch =
                        ta.getBoolean(R.styleable.GestureLockView_onlyCheckUnderTouch, true);
        mShowLine = ta.getBoolean(R.styleable.GestureLockView_showLine, true);
        mLineWidthDp = ta.getDimensionPixelSize(R.styleable.GestureLockView_lineWidth,
                        DisplayUtils.dip2px(mContext, 8));
        mLockDrawable = ta.getDrawable(R.styleable.GestureLockView_lock);
        mLockWidth = ta.getDimensionPixelSize(R.styleable.GestureLockView_lockWidth,
                        DisplayUtils.dip2px(mContext, 64));
        mLockHeight = ta.getDimensionPixelSize(R.styleable.GestureLockView_lockHeight,
                        DisplayUtils.dip2px(mContext, 64));
        mDrawAnchorPoint = ta.getBoolean(R.styleable.GestureLockView_drawAnchorPoint, false);
        mDrawAnchorShadow = ta.getBoolean(R.styleable.GestureLockView_drawAnchorShadow, false);
        mAnchorShadowRadius = ta.getDimensionPixelOffset(
                        R.styleable.GestureLockView_anchorShadowRadius, DisplayUtils.dip2px(mContext, 32));
        mCheckedCircleRadius = ta.getDimensionPixelSize(
                        R.styleable.GestureLockView_checkedCircleRadius, DisplayUtils.dip2px(mContext, 16));
        ta.recycle();

        init();
    }

    private void init() {
        // 初始化震动传感器
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        // 设置Lock的item图片
        if (mLockDrawable instanceof StateListDrawable) {
            mLockStateListDrawable = (StateListDrawable) mLockDrawable;
        }
        Bitmap bitmap = null;
        if (mLockStateListDrawable != null) {
            refreshDrawableState();
        } else if (mLockDrawable != null) {
            bitmap = ((BitmapDrawable) mLockDrawable).getBitmap();
        } else {
            // 默认checkPoint 的背景
            bitmap = ResourceUtils.getBitmap(mContext, R.drawable.shape_circle);
        }
        if (bitmap != null) {
            /*
              {@link Bitmap#createScaledBitmap(Bitmap, int, int, boolean)} 方法在指定的缩放宽高和原图
              相同时，会直接返回原图，所以调用{@link Bitmap#recycle()} 方法时可能会导致{@link NullPointerException}
             */
            if (bitmap.getWidth() == mLockWidth && bitmap.getHeight() == mLockHeight) {
                mLockBitmap = bitmap;
            } else {
                mLockBitmap = Bitmap.createScaledBitmap(bitmap, mLockWidth, mLockHeight, true);
                bitmap.recycle();
            }
        }

        // 初始化单位
//        mLockWidth = mLockBitmap.getWidth();
//        mLockHeight = mLockBitmap.getHeight();

        // 初始化画笔
        mPaint = new Paint();
        mPaint.setStrokeWidth(mLineWidthDp);
        mPaint.setAntiAlias(true);
        mPaint.setColor(mDrawingColor);

        // 初始化状态存储变量
        mPointCheckedStateArray = new boolean[mCountSide][mCountSide];
        mCheckedOrder = new ArrayList<>();

        initPointCheckedStateArray();
    }

    /**
     * 初始化存储各点check状态的数组
     */
    private void initPointCheckedStateArray() {
        for (int i = 0; i < mCountSide; i++) {
            for (int j = 0; j < mCountSide; j++) {
                mPointCheckedStateArray[i][j] = false;
            }
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (mChecked) {
            mergeDrawableStates(drawableState, mStateChecked);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (mLockStateListDrawable != null) {

            int[] myDrawableState = getDrawableState();

            mLockStateListDrawable.setState(myDrawableState);

            BitmapDrawable drawable = (BitmapDrawable) mLockStateListDrawable.getCurrent();
            Bitmap bitmap = drawable.getBitmap();
            if (bitmap.getWidth() == mLockWidth && bitmap.getHeight() == mLockHeight) {
                mLockBitmap = bitmap;
            } else {
                mLockBitmap = Bitmap.createScaledBitmap(bitmap, mLockWidth, mLockHeight, true);
                bitmap.recycle();
            }
        }
    }

    // 选中状态
    private void setChecked(boolean checked) {
        if(mChecked != checked) {
            mChecked = checked;
            refreshDrawableState();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        int widthMod = MeasureSpec.getMode(widthMeasureSpec);
//        int heightMod = MeasureSpec.getMode(heightMeasureSpec);
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);

        setPaddingValue();

        int modeWidth  = MeasureSpec.getMode(widthMeasureSpec);
        if (modeWidth == MeasureSpec.EXACTLY) {
            mWidth = Math.max(mWidth, getSuggestedMinimumWidth());
        } else if (modeWidth == MeasureSpec.AT_MOST) {
            int width = mPaddingLeft + mPaddingRight + mLockWidth * mCountSide
                            + mMinSpace * (mCountSide - 1);
            mWidth = Math.min(width, mWidth);
        }

        int modeHeight  = MeasureSpec.getMode(heightMeasureSpec);
        if (modeHeight == MeasureSpec.EXACTLY) {
            mHeight = Math.max(mHeight, getSuggestedMinimumHeight());
        } else if (modeHeight == MeasureSpec.AT_MOST) {
            int height = mPaddingTop + mPaddingBottom + mLockHeight * mCountSide
                    + mMinSpace * (mCountSide - 1);
            mHeight = Math.min(height, mHeight);
        }
        setMeasuredDimension(mWidth, mHeight);

        calculateValues();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 先绘制线再绘制图案，目的是为了将线盖再图案下面

        // 绘制路径线
        if (mShowLine && !mCheckedOrder.isEmpty()) {
            for (int i = 0; i < mCheckedOrder.size(); i++) {
                if (i >= mCheckedOrder.size() - 1) {
                    // 跟随手指的活动路径
                    if (mTouchPointX >= 0 && mTouchPointY >= 0) {
                        Point startPoint = calculateItemCenterCoordinate(mCheckedOrder.get(i));
                        canvas.drawLine(startPoint.x, startPoint.y, mTouchPointX, mTouchPointY, mPaint);
                    }
                } else {
                    // 固定的点点之间路径
                    Point startPoint = calculateItemCenterCoordinate(mCheckedOrder.get(i));
                    Point endPoint = calculateItemCenterCoordinate(mCheckedOrder.get(i + 1));
                    canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, mPaint);
                }
            }
        }

        // 绘制各个点
        for (int i = 0; i < mCountSide; i++) {
            for (int j = 0; j < mCountSide; j++) {
                int x = mPaddingLeft + mSpaceHorizontal * i + mLockWidth * i;
                int y = mPaddingTop + mSpaceVertical * j + mLockHeight * j;

                if (mPointCheckedStateArray[i][j]) {
                    if (mLockDrawable != null) {
                        mLockDrawable.setState(mStateChecked);
                    }
                    // 绘制选中状态
                    setChecked(true);
                    canvas.drawBitmap(mLockBitmap, x, y, mPaint);
                    // 绘制选中状态的点
                    if (mDrawAnchorPoint) {
                        canvas.drawCircle(x + mLockWidth / 2,
                                y + mLockHeight / 2,
                                mCheckedCircleRadius, mPaint);
                    }

                    // 绘制阴影
                    if (mDrawAnchorShadow) {
                        mPaint.setAlpha(100);
                        canvas.drawCircle(x + mLockWidth / 2,
                                y + mLockHeight / 2,
                                mAnchorShadowRadius, mPaint);
                        mPaint.setAlpha(255);
                    }
                } else {
                    // 绘制未选中状态
                    setChecked(false);
                    canvas.drawBitmap(mLockBitmap, x, y, mPaint);
                }

            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mTouchable) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                resetState();

                mTouchPointX = event.getX();
                mTouchPointY = event.getY();
                Point startedPoint = checkBox(mTouchPointX, mTouchPointY);
                dataStorage(startedPoint);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                mTouchPointX = event.getX();
                mTouchPointY = event.getY();
                Point point = checkBox(mTouchPointX, mTouchPointY);
                dataStorage(point);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                mTouchPointX = -1;
                mTouchPointY = -1;

                mPaint.setColor(mEffectiveColor);

                // 如果选中的Lock数量少于设置的最小值，则清除所有的选中状态
                if (mCheckedOrder.size() < mMinEffectiveLockCount && mCheckedOrder.size() > 0) {
                    // 传递手势密码不可用事件
                    if (mGestureEvent != null && mGestureMode == GestureMode.MODE_CREATOR) {
                            mGestureEvent.onGestureCreate(GestureEvent.CREATE_CHECK_POINT_NOT_ENOUGH);
                    }
                    // 设置无效的显示结果
                    setGestureResult(mNoneffectiveColor);
                } else if (mCheckedOrder.size() >= mMinEffectiveLockCount) {
                    authority();
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    /**
     * 设置有效手势（点的数量不小于设定的最小数量）绘制完成后的显示效果
     *
     * @param color 线条和点等的显示颜色
     */
    private void setGestureResult(@ColorInt int color) {
        mPaint.setColor(color);

        // 设置自动清除
        if (mDurationErrorPatternDisappear == 0) {
            resetState();
        } else if (mDurationErrorPatternDisappear > 0) {
            resetStateDelay(mDurationErrorPatternDisappear);
        }

        invalidate();
    }

    /**
     * 设置边距值
     */
    private void setPaddingValue() {
        int minPadding = DisplayUtils.dip2px(mContext, 16);
        mPaddingLeft = Math.max(getPaddingStart() > 0 ? getPaddingStart() : getPaddingLeft(),
                minPadding);
        mPaddingRight = Math.max(getPaddingEnd() > 0 ? getPaddingEnd() : getPaddingRight(),
                minPadding);
        mPaddingTop = Math.max(getPaddingTop(), minPadding);
        mPaddingBottom = Math.max(getPaddingBottom(), minPadding);
    }

    /**
     * 计算各个点的坐标
     *
     * @param itemLocation 点的次序坐标
     * @return 点的位置坐标
     */
    private Point calculateItemCenterCoordinate(Point itemLocation) {
        int x = itemLocation.x;
        int y = itemLocation.y;
        int minX = mPaddingLeft + mSpaceHorizontal * x + mLockWidth * x;
        int maxX = mPaddingLeft + mSpaceHorizontal * x + mLockWidth * x + mLockWidth;
        int minY = mPaddingTop + mSpaceVertical * y + mLockHeight * y;
        int maxY = mPaddingTop + mSpaceVertical * y + mLockHeight * y + mLockHeight;
        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;
        return new Point(centerX, centerY);
    }

    /**
     * 计算各种参数数值
     */
    private void calculateValues() {
        mWidth = mHeight = Math.min(mWidth, mHeight);
        mLockWidth = mLockHeight = Math.max(mLockHeight, mLockWidth);
        mSpaceHorizontal = (mWidth - mLockWidth * mCountSide - mPaddingLeft - mPaddingRight)
                        / (mCountSide - 1);
        mSpaceVertical = (mHeight - mLockHeight * mCountSide - mPaddingTop - mPaddingBottom)
                        / (mCountSide - 1);
    }

    /**
     * 通过点位置检查是否在某个Lock的区域内
     * @param x x坐标
     * @param y y坐标
     * @return Lock的次序坐标
     */
    private Point checkBox(float x, float y) {
        for (int i = 0; i < mCountSide; i++) {
            for (int j = 0; j < mCountSide; j++) {
                int minX = mPaddingLeft + mSpaceHorizontal * i + mLockWidth * i;
                int maxX = mPaddingLeft + mSpaceHorizontal * i + mLockWidth * i + mLockWidth;
                int minY = mPaddingTop + mSpaceVertical * j + mLockHeight * j;
                int maxY = mPaddingTop + mSpaceVertical * j + mLockHeight * j + mLockHeight;

                RectF rectF = new RectF(minX, minY, maxX, maxY);
                if (rectF.contains(x, y)) {
                    return new Point(i, j);
                }

            }
        }

        return new Point(-1, -1);
    }

    /**
     * 将选中的点存储起来
     * @param point 选中的点次序坐标
     */
    private void dataStorage(Point point) {
        if (point.x >= 0 && point.y >= 0 &&
                !mCheckedOrder.contains(new Point(point.x, point.y))) {
            mPointCheckedStateArray[point.x][point.y] = true;

            if (!mCheckedOrder.isEmpty() && !mOnlyCheckedUnderTouch) {
                checkEscapedFish(mCheckedOrder.get(mCheckedOrder.size() - 1), point);
            }

            if (!mCheckedOrder.contains(point)) {
                // 将checked点加入list
                mCheckedOrder.add(point);
                // 调用震动传感器，启动check的震动效果
                try {
                    mVibrator.vibrate(200);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 检测两个checked的点之间的点是否应该checked
     * @param p1 上一个checked的点的次序坐标
     * @param p2 当前checked的点的次序坐标
     */
    private void checkEscapedFish(Point p1, Point p2) {
        int x1 = Math.min(p1.x, p2.x);
        int y1 = Math.min(p1.y, p2.y);
        int x2 = Math.max(p1.x, p2.x);
        int y2 = Math.max(p1.y, p2.y);

        if (x1 == x2) {
            // 判断竖直方向线的中间点
            for (int i = y1 + 1; i < y2; i++) {
                Point point = new Point(x1, i);
                if (!mCheckedOrder.contains(point)) {
                    mPointCheckedStateArray[x1][i] = true;
                    mCheckedOrder.add(point);
                }
            }
        } else if (y1 == y2) {
            // 判断水平方向线的中间点
            for (int i = x1 + 1; i < x2; i++) {
                Point point = new Point(i, y1);
                if (!mCheckedOrder.contains(point)) {
                    mPointCheckedStateArray[i][y1] = true;
                    mCheckedOrder.add(point);
                }
            }
        } else {
            // 判断倾斜线的中间点
            for (int i = x1 + 1; i < x2; i++) {
                for (int j = y1 + 1; j < y2; j++) {
                    if ((float)j == ((float) y2 - y1) / (x2 - x1) * i) {
                        Point point = new Point(i, j);
                        if (!mCheckedOrder.contains(point)) {
                            mPointCheckedStateArray[i][j] = true;
                            mCheckedOrder.add(point);
                        }
                    }
                }
            }
        }
    }

    private void authority() {
        switch (mGestureMode) {
            case GestureMode.MODE_CREATOR:
                creator();
                break;
            case GestureMode.MODE_TRAVERSER:
                traverser();
                break;
        }

        invalidate();
        // 设置自动清楚
        if (mDurationPatternDisappear == 0) {
            resetState();
        } else if (mDurationPatternDisappear > 0) {
            resetStateDelay(mDurationPatternDisappear);
        }
    }

    /**
     * 创建手势密码
     */
    private void creator() {
        String password = getPasswordString(mCheckedOrder);
        if (TextUtils.isEmpty(mTempPassword)) {
            mTempPassword = password;
        }
        if (mTempPassword.equals(password)) {
            setGestureResult(mEffectiveColor);
            mVerifyTimes--;
            if (mGestureEvent != null) {
                // 传递剩余验证次数的回调
                mGestureEvent.onGestureCreateEffective(mVerifyTimes, mTempPassword);
            }
            if (mVerifyTimes <= 0) {
                if (mGestureEvent != null) {
                    // 存储密码
                    mGestureEvent.storePassword(password);
                    // 传递手势密码创建成功的回调
                    mGestureEvent.onGestureCreate(GestureEvent.CREATE_SUCCESSFUL);
                }
            }
        } else {
            setGestureResult(mNoneffectiveColor);
            if (mGestureEvent != null) {
                mGestureEvent.onGestureCreate(GestureEvent.CREATE_NOT_SAME_AS_FIRST_TIMES);
            }
        }
    }

    /**
     * 验证手势密码
     */
    private void traverser() {
        if (mGestureEvent != null) {
            boolean exactly = mGestureEvent.verifyPassword(convertPoint2Password(mCheckedOrder));
            if (exactly) {
                // 传递密码正确事件
                mGestureEvent.onGestureAuthority(GestureEvent.AUTHORITY_EXACTLY);
            } else {
                mPaint.setColor(mNoneffectiveColor);
                // 传递密码不正确事件
                mGestureEvent.onGestureAuthority(GestureEvent.AUTHORITY_NOT_EXACTLY);
            }
        }
    }

    /**
     * 延时重置密码锁状态
     * @param ms 延迟时间（单位：毫秒）
     */
    private void resetStateDelay(long ms) {
        getHandler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        resetState();
                        invalidate();
                    }
                },
                ms
        );
    }

    /**
     * 重置状态值
     */
    private void resetState() {
        // 将画笔颜色重置为初始色
        mPaint.setColor(mDrawingColor);
        initPointCheckedStateArray();
        mCheckedOrder.clear();
    }

    /**
     * 根据提供的密码绘制图案
     *
     * @param password 密码（各点坐标的横纵坐标值按顺序拼接）
     *                 当调用此方法时，手势图案即变为不自动消失模式
     */
    public void setPreviewMode(String password) {
        mTouchable = false;
        resetState();
        for (int i = 0; i < password.length(); i += 2) {
            int x = Integer.parseInt(password.substring(i, i + 1));
            int y = Integer.parseInt(password.substring(i + 1, i + 2));
            if (x < mCountSide && y < mCountSide) {
                mCheckedOrder.add(new Point(x, y));
                mPointCheckedStateArray[x][y] = true;
            }
        }
        mDurationPatternDisappear = -1;
        mDurationErrorPatternDisappear = -1;

        // 默认使用绘制成功的颜色
        mPaint.setColor(mEffectiveColor);
        invalidate();
    }

    /**
     * 设置为创建者模式（用作创建手势密码）
     * @param verifyTimes 创建成功需求验证次数
     */
    public void setModeCreator(int verifyTimes) {
        mGestureMode = GestureMode.MODE_CREATOR;
        mVerifyTimes = verifyTimes;
    }

    /**
     * 设置为探索者模式（验证密码模式）
     */
    public void setModeTraverser() {
        mGestureMode = GestureMode.MODE_TRAVERSER;
    }

    /**
     * 设置自定义事件
     * @param event 自定义事件
     */
    public void setGestureEvent (GestureEvent event) {
        mGestureEvent = event;
    }

    public interface GestureEvent {
        int AUTHORITY_NOT_EXACTLY = 0x001;
        int AUTHORITY_EXACTLY = 0x002;

        int CREATE_SUCCESSFUL = 0x011;
        int CREATE_NOT_SAME_AS_FIRST_TIMES = 0x012;
        int CREATE_CHECK_POINT_NOT_ENOUGH = 0x013;

        void onGestureAuthority(int authority);

        void onGestureCreate(int create);

        void onGestureCreateEffective(int leftSteps, String password);

        void storePassword(String password);

        boolean verifyPassword(String password);

    }

    private interface GestureMode {
        int MODE_CREATOR = 0x001;
        int MODE_TRAVERSER = 0x002;
    }

    /**
     *将选中点的list转化为string密码
     *
     * @param pList 存储选中点的List
     * @return 生成的一串数字密码
     */
    private String getPasswordString(List<Point> pList) {
        if (pList != null && !pList.isEmpty()) {
            String password = "";
            for (Point point : pList) {
                password += point.x + "" + point.y;
            }
            return password;
        }
        return null;
    }

    /**
     * 将手势点阵转换为字符串数字密码
     *
     * @param pList 手势点阵列表
     * @return 数字密码
     */
    private String convertPoint2Password(List<Point> pList) {
        if (pList == null || pList.isEmpty()) {
            return "";
        }
        String password = "";
        for (Point point : pList) {
            password += point.x + "" + point.y;
        }
        return password;
    }
}