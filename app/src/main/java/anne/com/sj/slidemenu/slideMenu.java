package anne.com.sj.slidemenu;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.AbsSavedState;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.support.v4.widget.ViewDragHelper;

public class slideMenu extends FrameLayout {
    private static final String TAG = "slideMenu";
    private static final int MOVE_TO_LEFT = 0;
    private static final int MOVE_TO_RIGHT = 1;
    private static final float  TOUCH_SLOP_SENSITIVITY = 1.0f;
    private static final int SPRING_BACK_DISTANCE = 80;
    private static final int MENU_MARGIN_RIGHT = 120;
    private static final int MENU_OFFSET = 128;
    private static final int MENU_OPEN = 1;
    private static final int MENU_CLOSE = 0;



    private View mMainView;
    private View mMenuView;
    private ViewDragHelper mViewDragHelper;
    private int mBackDistence;
    private int mOrientation;
    private int mMenuMarginRight;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mMenuWidth;
    private int mMenuOffset;
    private int mMenuState;
    private String mShadowOpacity = "00";


    public slideMenu(Context context) {
        this(context,null);
    }

    public slideMenu(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public slideMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final float density = getResources().getDisplayMetrics().density;
        mBackDistence = (int)(density * SPRING_BACK_DISTANCE + 0.5f);
        mMenuMarginRight = (int)(MENU_MARGIN_RIGHT * density + 0.5f);
        mScreenWidth = (int) (getResources().getDisplayMetrics().widthPixels + 0.5f);
        mScreenHeight = (int) (getResources().getDisplayMetrics().heightPixels + 0.5f);
        mMenuWidth = (int) (mScreenWidth - MENU_MARGIN_RIGHT * density + 0.5f);
        mMenuOffset = (int) (MENU_OFFSET * density + 0.5f);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mMainView = getChildAt(1);
        mMenuView = getChildAt(0);
        mViewDragHelper = ViewDragHelper.create(this,TOUCH_SLOP_SENSITIVITY,new ViwMenuCallback());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG , "onTouchEvent");
        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d(TAG , "onInterceptTouchEvent");
        return mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    private class ViwMenuCallback extends ViewDragHelper.Callback{

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mMainView;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {

            if(left < 0)
                left = 0;
            else if(left > mMenuView.getWidth())
                left = mMenuView.getWidth();
            return left;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if (dx < 0)
                mOrientation = MOVE_TO_LEFT;
            else if (dx > 0)
                mOrientation = MOVE_TO_RIGHT;

            float shadowScale = (float)(mScreenWidth - left) / (float)mScreenWidth;
            int shadow = 255 - Math.round(shadowScale * 255);
            if (shadow < 16)
                mShadowOpacity = "0" + Integer.toHexString(shadow);
            else
                mShadowOpacity = "" + Integer.toHexString(shadow);
            float scale = ((float) (mMenuWidth - mMenuOffset)) / (float) mMenuWidth;
            int menuLeft = (int) (scale * mMainView.getLeft() - mMenuWidth);
            mMenuView.layout(menuLeft + mMenuOffset, mMenuView.getTop(), menuLeft + mMenuWidth + mMenuOffset, mMenuView.getBottom());
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int mainLeft = mMainView.getLeft();
            if(mainLeft > mMenuWidth/2){
                openMenu();
            }else{
                closeMenu();
            }
        }
    }
    private void closeMenu(){
        mViewDragHelper.smoothSlideViewTo(mMainView,getPaddingLeft(),0);
        ViewCompat.postInvalidateOnAnimation(slideMenu.this);
        mMenuState = MENU_CLOSE;
    }

    private void openMenu(){
        mViewDragHelper.smoothSlideViewTo(mMainView,mMenuView.getWidth(),0);
        ViewCompat.postInvalidateOnAnimation(slideMenu.this);
        mMenuState = MENU_OPEN;
    }

    @Override
    public void computeScroll() {
        if(mViewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(slideMenu.this);
        }
        super.computeScroll();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final int restoreCount = canvas.save();//保存画布当前的剪裁信息

        final int height = getHeight();
        final int clipLeft = 0;
        int clipRight = mMainView.getLeft();
        //优化绘制View
        if (child == mMenuView) {
            canvas.clipRect(clipLeft, 0, clipRight, height);//剪裁显示的区域
        }
        boolean result = super.drawChild(canvas, child, drawingTime);//绘制当前view
        //恢复画布之前保存的剪裁信息
        //以正常绘制之后的view
        canvas.restoreToCount(restoreCount);
        int left = mMainView.getLeft();
        Paint shadowPaint = new Paint();
        shadowPaint.setColor(Color.parseColor("#" + mShadowOpacity + "777777"));
        shadowPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(left , 0 , mMainView.getWidth() , mScreenHeight , shadowPaint);
        return result;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        ViewGroup.LayoutParams menuParams = mMenuView.getLayoutParams();
        menuParams.width = mMenuWidth;
        mMenuView.setLayoutParams(menuParams);
        if(mMenuState == MENU_OPEN){
            mMainView.layout(mMenuWidth , 0 , mScreenWidth-mMenuWidth , bottom);
            mMenuView.layout(left , 0 , mMenuWidth , bottom);
            return;
        }
        mMenuView.layout(-mMenuOffset,0,mMenuWidth-mMenuOffset,bottom);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable savedState =  super.onSaveInstanceState();
        slideMenu.SavedState ss = new slideMenu.SavedState(savedState);
        ss.menuState = mMenuState;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof slideMenu.SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        final slideMenu.SavedState ss = (slideMenu.SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (ss.menuState == MENU_OPEN) {
            openMenu();
        }

    }

    protected static class SavedState extends AbsSavedState {
        int menuState;

        SavedState(Parcel in, ClassLoader loader) {
            super(in);
            menuState = in.readInt();
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(menuState);
        }

        public static final Creator<slideMenu.SavedState> CREATOR = ParcelableCompat.newCreator(
                new ParcelableCompatCreatorCallbacks<SavedState>() {
                    @Override
                    public slideMenu.SavedState createFromParcel(Parcel in, ClassLoader loader) {
                        return new slideMenu.SavedState(in, loader);
                    }

                    @Override
                    public slideMenu.SavedState[] newArray(int size) {
                        return new slideMenu.SavedState[size];
                    }
                });
    }
}
