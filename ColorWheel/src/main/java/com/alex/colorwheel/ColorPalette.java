package com.alex.colorwheel;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import androidx.core.content.res.ResourcesCompat;

public class ColorPalette extends View implements OnTouchListener{
    private int mColor;
    private Drawable Image;
    protected static final int		NOTHING_SET	    = 0;
    protected static final int		SET_H	        = 1;
    protected static final int		SET_V	        = 2;
    protected static final int		SET_S	        = 3;
    protected static final int		SET_A	        = 4;
    protected static final int		SET_FINAL_COLOR	= 5;
    private int	mMode;
    int	size=0;
    float imagesize=0;
    float cx=0;
    float cy=0;
    float r_H;
    float r_SV;
    float r_A;
    float r_centr;

    float r_H_border; //
    float r_SV_border; //
    float r_A_border; // внешние границы полей выбора
    // всякие краски
    Paint p_H_palette = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint p_SV_palette = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint p_A_palette = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint p_cursors = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint p_final_color = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint p_red = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint p_green = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint p_blue = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float deg_H;
    private float deg_V;
    private float deg_S;
    private float deg_A;

    private float lm; // отступы и выступы линий
    private float lw; //
    int[] r_H_palette_colors = new int[] {Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED};
    //private int[] argb = new int[]{255, 0, 0, 0};
    private float[] hsv = new float[]{0, 1f, 1f};
    private OnColorChangeListener listener;

    public interface OnColorChangeListener{
        void onDismiss(int color);
        void onColorChanged(int color);
        void onFocusSetOnPalette();
    }

    public void setOnColorChangeListener(OnColorChangeListener l){this.listener=l;}

    public ColorPalette(Context context){this(context, null);}

    public ColorPalette(Context context, AttributeSet attrs){this(context, attrs, 0);}

    public ColorPalette(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(); init(attrs);
    }

    private void init(AttributeSet attrs){
        int ref=0;
        for(int i=0;i<attrs.getAttributeCount();i++){
            if(attrs.getAttributeName(i).equals("Image")){
                ref=attrs.getAttributeResourceValue(i,0);
            }
        }
        if(ref!=0){
            Image=ResourcesCompat.getDrawable(getResources(),ref,null);
        }
    }

    private void init(){
        setFocusable(true);
        p_H_palette.setStyle(Paint.Style.STROKE);
        p_SV_palette.setStyle(Paint.Style.STROKE);
        p_A_palette.setStyle(Paint.Style.STROKE);
        p_final_color.setStyle(Paint.Style.FILL_AND_STROKE);
        p_cursors.setStrokeWidth(5);
        p_cursors.setColor(Color.WHITE);
        p_cursors.setStrokeCap(Paint.Cap.ROUND);
        p_red.setColor(Color.RED);
        p_red.setStyle(Paint.Style.STROKE);
        p_green.setColor(Color.GREEN);
        p_green.setStyle(Paint.Style.STROKE);
        p_blue.setColor(Color.BLUE);
        p_blue.setStyle(Paint.Style.STROKE);
        setOnTouchListener(this);
    }

    //public int[] intColorToARGB(int color){return new int[]{(color>>24)&0xFF,(color>>16)&0xFF,(color>>8)&0xFF,color&0xFF};}

    public void setColor(int color) {
        mColor=color;
        Color.colorToHSV(mColor,hsv);
        deg_H=hsv[0];
        deg_S=hsv[1]*180;
        deg_V=hsv[2]*180+180;
        deg_A=(float)(-((mColor>>24)&0xFF)*360)/255;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){

        int mWidth=measure(widthMeasureSpec);
        int mHeight=measure(heightMeasureSpec);
        size=Math.min(mWidth,mHeight);
        setMeasuredDimension(size,size);

        // Вычислили размер доступной области, определили что меньше
        // и установили размер нашей View в виде квадрата со стороной в
        // высоту или ширину экрана в зависимости от ориентации.
        // Вместо Math.min как вариант можно использовать getConfiguration,
        // величину size можно умножать на какие-нибудь коэффициенты,
        // задавая размер View относительно размера экрана. Например так:

		/*int orient = getResources().getConfiguration().orientation;

		switch (orient) {
		case Configuration.ORIENTATION_PORTRAIT:
			size = (int) (measureHeight * port);

			break;
		case Configuration.ORIENTATION_LANDSCAPE:
			size = (int) (measureHeight * land);
			break;
		}*/
        calculateSizes();
    }

    private int measure(int measureSpec){return ((MeasureSpec.getMode(measureSpec)==MeasureSpec.UNSPECIFIED) ? 200 : MeasureSpec.getSize(measureSpec));}

    private void calculateSizes() {
        cx=size*0.5f;
        cy=cx;
        float lc=size*0.06f;
        r_H=size*0.46f;
        lm=lc/2+0.01f;
        lw=lc/2;
        r_H_border=size*0.5f;
        r_SV=size*0.38f;
        r_SV_border=size*0.4f;
        r_A=size*0.3f;
        r_A_border=size*0.3f;
        r_centr=size*0.2f;

        imagesize=size/11f;

        p_H_palette.setStrokeWidth(lc);
        p_SV_palette.setStrokeWidth(lc);
        p_A_palette.setStrokeWidth(lc);

        if(Image!=null){
            Image.setBounds((int)(cx-imagesize),(int)(cy-imagesize),(int)(cx+imagesize),(int)(cy+imagesize));
            Image.setAlpha(255);
        }

    }



    private float getAngle360(float x, float y){return (float)Math.toDegrees(Math.atan((x!=0) ? (y/x) : 0))+((x<0) ? 180 : ((y<0) ? 360 : 0));}

    private void set_H(float f){
        deg_H=f; hsv[0]=f;
        mColor=Color.HSVToColor((mColor>>24)&0xFF, hsv);
        p_final_color.setColor(mColor);
    }

    private void set_S(float f){
        deg_S=(f>270) ? 0 : ((f>180) ? 180 : f);
        hsv[1]=deg_S/180;
        mColor=Color.HSVToColor((mColor>>24)&0xFF, hsv);
        p_final_color.setColor(mColor);
    }

    private void set_V(float f){
        deg_V=(f<90) ? 360 : ((f<180) ? 180 : f);
        hsv[2]=(deg_V-180)/180;
        mColor=Color.HSVToColor((mColor>>24)&0xFF, hsv);
        p_final_color.setColor(mColor);
    }

    private void set_A(float f) {
        deg_A=f;
        mColor=Color.HSVToColor(Math.round(((360-f)*255)/360), hsv);
        p_final_color.setColor(mColor);
    }

    private void draw_H_palette(Canvas canvas){
        Shader s=new SweepGradient(cx, cy, r_H_palette_colors, null);
        p_H_palette.setShader(s);
        canvas.drawCircle(cx, cy, r_H, p_H_palette);
    }

    private void draw_SV_palette(Canvas canvas){
        int[] colorint=new int[]{Color.HSVToColor(new float[]{hsv[0], 0, 1}), Color.HSVToColor(new float[]{hsv[0], 1, 1}), Color.HSVToColor(new float[]{hsv[0], hsv[1], 0}), Color.HSVToColor(new float[]{hsv[0], hsv[1], 1})};
        SweepGradient sg=new SweepGradient(cx,cy,colorint,new float[]{0,0.5f,0.5f,1});
        p_SV_palette.setShader(sg);
        canvas.drawCircle(cx, cy, r_SV, p_SV_palette);
    }

    private void draw_A_palette(Canvas canvas) {
        canvas.drawCircle(cx, cy, r_A - lw, p_red);
        canvas.drawCircle(cx, cy, r_A, p_green);
        canvas.drawCircle(cx, cy, r_A + lw, p_blue);
        Shader sw = new SweepGradient(cx, cy, new int[]{mColor | 0xFF000000,(mColor & 0x00FFFFFF)}, null);
        p_A_palette.setShader(sw);
        canvas.drawCircle(cx, cy, r_A, p_A_palette);
    }

    private void drawCursor(Canvas canvas,float xc,float yc){
        canvas.drawLine(xc+lm, yc, xc-lm, yc, p_cursors);
    }

    private void drawCursors(Canvas canvas) {
        float d=deg_H;
        canvas.rotate(d, cx, cy);
        drawCursor(canvas,cx+ r_H,cy);
        //canvas.rotate(-d, cx, cy);
        d=deg_S-deg_H;
        canvas.rotate(d, cx, cy);
        drawCursor(canvas,cx+ r_SV,cy);
        //canvas.rotate(-d, cx, cy);
        d=deg_V-deg_S;
        canvas.rotate(d, cx, cy);
        drawCursor(canvas,cx+ r_SV,cy);
        //canvas.rotate(-d, cx, cy);
        d=deg_A-deg_V;
        canvas.rotate(d, cx, cy);
        drawCursor(canvas,cx+ r_A,cy);
        canvas.rotate(-deg_A, cx, cy);
    }

    private void drawFinalColor(Canvas canvas){
        p_final_color.setColor(mColor);
        canvas.drawCircle(cx, cy, r_centr, p_final_color);
    }

    private void drawVectorImage(Canvas canvas){
        if(Image!=null){Image.draw(canvas);}
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        draw_H_palette(canvas);
        draw_SV_palette(canvas);
        draw_A_palette(canvas);
        drawCursors(canvas);
        drawFinalColor(canvas);
        drawVectorImage(canvas);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x=event.getX()-cx;
        float y=event.getY()-cy;
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                listener.onFocusSetOnPalette();
                float r=(float)Math.sqrt(Math.abs(x*x)+Math.abs(y*y));
                mMode=NOTHING_SET;
                if(r<r_H_border){mMode=SET_H;
                if(r<r_SV_border){mMode=(y<0) ? SET_V : SET_S;
                if(r<r_A_border){mMode=SET_A;
                if(r<r_centr){mMode=SET_FINAL_COLOR; listener.onDismiss(mColor);}}}}
            break;
            case MotionEvent.ACTION_MOVE:
                switch (mMode){
                    case SET_H:
                        set_H(getAngle360(x, y));
                        invalidate();
                        listener.onColorChanged(mColor);
                        break;
                    case SET_S:
                        set_S(getAngle360(x, y));
                        invalidate();
                        listener.onColorChanged(mColor);
                        break;
                    case SET_V:
                        set_V(getAngle360(x, y));
                        invalidate();
                        listener.onColorChanged(mColor);
                        break;
                    case SET_A:
                        set_A(getAngle360(x, y));
                        invalidate();
                        listener.onColorChanged(mColor);
                        break;
                    case NOTHING_SET:
                    case SET_FINAL_COLOR:
                    default: break;
                }
            break;
        }
        return true;
    }
}
