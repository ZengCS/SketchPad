package com.zcs.android.lib.sketch.pen.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.zcs.android.lib.sketch.pen.BasePenExtend;
import com.zcs.android.lib.sketch.pen.CustomPen;
import com.zcs.android.lib.sketch.pen.IPenConfig;
import com.zcs.android.lib.sketch.pen.IPenType;
import com.zcs.android.lib.sketch.pen.SteelPen;

/**
 * @author ZengCS
 * @version v1.0 create at 2017/8/24
 * @des DrawPenView实现手写关键类，目前只提供了，手绘的功能和清除画布，后期根据业务逻辑可以动态的设置方法
 */
public class NewDrawPenView extends View {
    private static final String TAG = "DrawPenView";
    private Paint mNewPenPaint;// 画笔
    private Canvas mCanvas;// 画布
    private Bitmap mBitmap;
    private Context mContext;
    public static int mCanvasCode = IPenConfig.STROKE_TYPE_PEN;
    private BasePenExtend mStokeBrushPen;
    private boolean mIsCanvasDraw;

    public NewDrawPenView(Context context) {
        super(context);
        initParameter(context);
    }

    public NewDrawPenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initParameter(context);
    }

    public NewDrawPenView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initParameter(context);
    }

    private void initParameter(Context context) {
        mContext = context;
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(dm);
        mBitmap = Bitmap.createBitmap(dm.widthPixels, dm.heightPixels, Bitmap.Config.ARGB_8888);
        mStokeBrushPen = new SteelPen(context);
        initPaint();
        initCanvas();
    }

    private void initPaint() {
        mNewPenPaint = new Paint();
        mNewPenPaint.setColor(IPenConfig.PEN_CORLOUR);
        mNewPenPaint.setStrokeWidth(IPenConfig.PEN_WIDTH);
        mNewPenPaint.setStyle(Paint.Style.STROKE);
        mNewPenPaint.setStrokeCap(Paint.Cap.ROUND);//结束的笔画为圆心
        mNewPenPaint.setStrokeJoin(Paint.Join.ROUND);//连接处元
        mNewPenPaint.setAlpha(0xFF);
        mNewPenPaint.setAntiAlias(true);
        mNewPenPaint.setStrokeMiter(1.0f);
        mStokeBrushPen.setPaint(mNewPenPaint);
    }

    private void initCanvas() {
        mCanvas = new Canvas(mBitmap);
        // 设置画布的颜色的问题
        mCanvas.drawColor(Color.TRANSPARENT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, mNewPenPaint);
        switch (mCanvasCode) {
            case IPenConfig.STROKE_TYPE_PEN:
            case IPenConfig.STROKE_TYPE_BRUSH:
            case IPenConfig.STROKE_TYPE_CHALK:
            case IPenConfig.STROKE_TYPE_PENCIL:
                mStokeBrushPen.draw(canvas);
                break;
            case IPenConfig.STROKE_TYPE_ERASER:
                reset();
                break;
            default:
                Log.e(TAG, "onDraw" + Integer.toString(mCanvasCode));
                break;
        }
        super.onDraw(canvas);
    }

    public void redo(){
        mStokeBrushPen.redo();
        invalidate();
    }

    public void setCanvasCode(int canvasCode) {
        mCanvasCode = canvasCode;
        switch (mCanvasCode) {
            case IPenConfig.STROKE_TYPE_PEN:// 钢笔
                mStokeBrushPen = new SteelPen(mContext);
                break;
            case IPenConfig.STROKE_TYPE_BRUSH:// 毛笔
                mStokeBrushPen = new CustomPen(mContext, IPenType.BRUSH);
                break;
            case IPenConfig.STROKE_TYPE_CHALK:// 粉笔
                mStokeBrushPen = new CustomPen(mContext, IPenType.CHALK);
                break;
            case IPenConfig.STROKE_TYPE_PENCIL:// 铅笔
                mStokeBrushPen = new CustomPen(mContext, IPenType.PENCIL);
                break;
        }
        //设置
        if (mStokeBrushPen.isNull()) {
            mStokeBrushPen.setPaint(mNewPenPaint);
        }
        invalidate();
    }

    /**
     * event.getAction() //获取触控动作比如ACTION_DOWN
     * event.getPointerCount(); //获取触控点的数量，比如2则可能是两个手指同时按压屏幕
     * event.getPointerId(nID); //对于每个触控的点的细节，我们可以通过一个循环执行getPointerId方法获取索引
     * event.getX(nID); //获取第nID个触控点的x位置,记录的第一个点为getX，getY
     * event.getY(nID); //获取第nID个点触控的y位置
     * event.getPressure(nID); //LCD可以感应出用户的手指压力，当然具体的级别由驱动和物理硬件决定的
     * event.getDownTime() //按下开始时间
     * event.getEventTime() // 事件结束时间
     * event.getEventTime()-event.getDownTime()); //总共按下时花费时间
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mIsCanvasDraw = true;
        MotionEvent event2 = MotionEvent.obtain(event);
        mStokeBrushPen.onTouchEvent(event2, mCanvas);
        invalidate();
        return true;
    }

    /**
     * @return 判断是否有绘制内容在画布上
     */
    public boolean getHasDraw() {
        return mIsCanvasDraw;
    }

    /**
     * 清除画布，记得清除点的集合
     */
    public void reset() {
        mNewPenPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mCanvas.drawPaint(mNewPenPaint);
        mNewPenPaint.setXfermode(null);
        mIsCanvasDraw = false;
        mStokeBrushPen.clear();

        // 清空后恢复至钢笔
        setCanvasCode(IPenConfig.STROKE_TYPE_PEN);
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }
}
