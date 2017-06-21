# 自定义view--手势密码



###### 项目地址：https://github.com/pcatzj/GestureLock



### 效果和预览

之前项目里新需求需要增加手势密码锁功能，正好当时不是很忙，就自己写了一个。大致也测试了一段时间，现在记录一下。

首先是效果图

![](https://ooo.0o0.ooo/2017/06/16/594335739b5e9.gif)





### 自定义属性

思路就是监听onTouch事件，然后手动刷新视图，在onDraw回调里绘制画面。

先看一下支持哪些属性定制：

```java
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
```

这些属性都是项目里需求的或者我在写的时候想到的可能比较有可能有用的。



在`styles.xml` 文件里添加自定义属性

```xml
<declare-styleable name="GestureLockView">
        <attr name="countSide" format="integer"/>
        <attr name="minEffectiveLockCount" format="integer"/>
        <attr name="drawingColor" format="color"/>
        <attr name="effectiveColor" format="color"/>
        <attr name="noneffectiveColor" format="color"/>
        <attr name="onlyCheckUnderTouch" format="boolean"/>
        <attr name="showLine" format="boolean"/>
        <attr name="lineWidth" format="dimension"/>
        <attr name="lock" format="reference"/>
        <attr name="lockChecked" format="reference"/>
        <attr name="lockWidth" format="dimension"/>
        <attr name="lockHeight" format="dimension"/>
        <attr name="drawAnchorPoint" format="boolean"/>
        <attr name="drawAnchorShadow" format="boolean"/>
        <attr name="anchorShadowRadius" format="dimension"/>
        <attr name="checkedCircleRadius" format="dimension"/>
        <attr name="durationPatternDisappear" format="integer"/>
        <attr name="durationErrorPatternDisappear" format="integer"/>
    </declare-styleable>
```

然后在自定义view的构造函数里获取这些属性的值

```java
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
```



### view的属性配置和绘制

下面就是根据定制属性来绘制view了

首先是先在`onMesure()` 方法里计算各中尺寸，比如做view正方形处理，重新截取`mesuredWidth` 和`mesuredHeight`， 还有每个check point 的边长以及它们之间的间距，以及边距等的处理。

下面就是动态绘制的过程了，监听view的`onTouch()` 事件，在“touch down” 的时候，做一些初始化的操作，譬如 paint color 和一些状态值的初始化。同时检测 touch point 是否正好落在锁上，如果落在锁上，将选中的点的数据存储起来。

```java
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
            ……

        return super.onTouchEvent(event);
    }
```

当“touch move” 的时候，需要做的就是和“touch down” 的时候一样，检测触控点是否落在小锁圈上，如果重合了，则记录下该小锁环的坐标，另外，需要记录的应该还有touch point的坐标，在`onDraw()` 里需要用到这个坐标来画轨迹线。

```java
@Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mTouchable) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            ……
            case MotionEvent.ACTION_MOVE:
                mTouchPointX = event.getX();
                mTouchPointY = event.getY();
                Point point = checkBox(mTouchPointX, mTouchPointY);
                dataStorage(point);
                invalidate();
                return true;
            ……
        }

        return super.onTouchEvent(event);
    }
```

在`MotionEvent.ACTION_DOWN` 和`MotionEvent.ACTION_MOVE` 的回调里，都`return true` ，因为需要拦截事件，否则后续的`MotionEvent.ACTION_MOVE` 或者`MotionEvent.ACTION_UP` 事件可能会被其子view 或者下层view 拦截，而无法传递到当前view，参见Android 事件分发。

最后的“touch up” 回调锁需要做的事，就是一些状态判定的事了。比如，绘制的点数是否符合配置的最少点数的要求，或者在手势密码验证的状态下，绘制的手势密码是否和创建时匹配，即密码是否正确等。然后根据这些状态，将画笔颜色设置成错误色等，以及传递自定义的连接点数不足或者密码不匹配的回调等。

```java
@Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mTouchable) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            ……
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
                    // 验证手势密码是否正确
                    authority();
                }
                break;
        }

        return super.onTouchEvent(event);
    }
```

所需要的状态我们都已经记录好了，接着要进行的就是在`onDraw()` 函数中根据这些状态绘制不同的图形了。

这些都比较简单，所以偷个懒，贴一下代码

```java
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
```



### 回调函数

下面是回调函数的定义和调用

定义的回调接口

```java
public interface GestureEvent {
        int AUTHORITY_NOT_EXACTLY = 0x001;
        int AUTHORITY_EXACTLY = 0x002;

        int CREATE_NOT_SAME_AS_FIRST_TIMES = 0x011;
        int CREATE_CHECK_POINT_NOT_ENOUGH = 0x012;

        void onGestureAuthority(int authority);

        void onGestureCreate(int create);

        void onGestureCreateSuccessful(String password);

        void onGestureCreateEffective(int leftSteps, String password);

        boolean verifyPassword(String password);

    }
```

接口定义了5个方法，但是，其实是有8个回调。

下面依次介绍一下

- `void onGestureAuthority(int authority)` 手势密码验证阶段的回调，其中参数有两个选项——`AUTHORITY_NOT_EXACTLY`和 `AUTHORITY_EXACTLY`，分别代表“密码不正确”和“密码正确”。
- `void onGestureCreate(int create)` 手势密码创建阶段的回调，其中参数有两个选项——`CREATE_NOT_SAME_AS_FIRST_TIMES`和 `CREATE_CHECK_POINT_NOT_ENOUGH`，分别代表“手势密码创建成功”、“密码和第一次输入不同”以及“连接点数少于设定的最少连接点数”。
- `void onGestureCreateSuccessful(String password)` 手势密码创建成功的回调，参数表示已成功创建的手势点阵密码按照坐标拼接的一串数字密码。
- `void onGestureCreateEffective(int leftSteps, String password)` 手势密码创建阶段分部回调，即创建密码时每一次生效（连接点数不少于设定的最少连接点数）都会进行此回调，参数`leftSteps` 表示剩余的所需创建步数，`password` 为第一次输入的点阵密码按照坐标拼接的一串数字密码。在这里可以加入存储密码的逻辑。
- `boolean verifyPassword(String password)` 验证密码是否正确的回调，这是一个有返回值的回调方法。返回值为用户输入的验证密码是否和设定的密码匹配，如果匹配，则返回`true`， 否则返回`false`。参数表示用户输入的验证图形密码点阵按照坐标拼接的一串数字密码。



### 使用

目录结构

![](https://ooo.0o0.ooo/2017/06/19/594782f9e81d4.png)

其中用红色框标注的是必须的文件，`styles.xml` 文件里有自定义的`declare-styleable`，下划线标注的两个类是工具类，可以移植到相关的类中进行调用。`shape_circle.xml` 文件是自定义view的每个点的资源drawable，可以自定义。



### 小结

写博客的时候又回顾了一遍代码，整体看上去还是有点乱。但是边写的过程中也边顺带着优化了一些不足的地方。因为其实很多属性在自己的项目中没有用到，所以测试方面可能有所不足，有机会我会把所有的定制属性都使用一遍，找到不足的地方改正它。这篇博客在档案留存的同时，也是告诫自己养成多记录的好习惯（还是太懒，这一篇都差点没有顺产）。代码和逻辑肯定还有很多不足的地方，也希望大家多提意见，感谢！



### TODO 

- [ ] 在`onMesured()` 方法中将view 的width 和height 设置成相等——两者中小的那个，以期将其设置成正方形，调用`setMesuredDimension()` 方法未生效
  ![ji](https://ooo.0o0.ooo/2017/06/16/594335ab2856f.gif)
- [ ] getter and setter
- [ ] `LockHeight` 和`LockWidth` 以及`checkedCircleRadius` 属性的默认值计算


