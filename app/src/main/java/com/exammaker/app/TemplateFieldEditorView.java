package com.exammaker.app;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/** Visual editor: drag boxes; drag lower-left handle to resize. */
public class TemplateFieldEditorView extends View {
    public static final class FieldBox {
        public String id, title; public RectF rect;
        public FieldBox(String id,String title,RectF rect){this.id=id;this.title=title;this.rect=rect;}
    }
    private Bitmap page; private final List<FieldBox> boxes=new ArrayList<>();
    private final Paint imagePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint boxPaint=new Paint(Paint.ANTI_ALIAS_FLAG), textPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private Matrix imageMatrix=new Matrix(), inverse=new Matrix();
    private FieldBox active; private float lastX,lastY; private boolean resizing;

    public TemplateFieldEditorView(Context c){super(c);init();} public TemplateFieldEditorView(Context c, AttributeSet a){super(c,a);init();}
    private void init(){boxPaint.setStyle(Paint.Style.STROKE);boxPaint.setStrokeWidth(4);boxPaint.setColor(Color.rgb(37,99,235));textPaint.setColor(Color.WHITE);textPaint.setTextSize(24);setBackgroundColor(Color.DKGRAY);}
    public void setPage(Bitmap bitmap){page=bitmap;requestLayout();invalidate();}
    public void setDetection(AdvancedTemplateScanner.Result r){boxes.clear();boxes.add(new FieldBox("questionArea","ناحیه سؤالات",new RectF(r.questionArea)));boxes.add(new FieldBox("scoreColumn","ستون بارم",new RectF(r.scoreColumn)));invalidate();}
    public List<FieldBox> getBoxes(){return boxes;}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(page==null)return;float scale=Math.min(getWidth()/(float)page.getWidth(),getHeight()/(float)page.getHeight());float dx=(getWidth()-page.getWidth()*scale)/2f,dy=(getHeight()-page.getHeight()*scale)/2f;imageMatrix.reset();imageMatrix.postScale(scale,scale);imageMatrix.postTranslate(dx,dy);imageMatrix.invert(inverse);c.drawBitmap(page,imageMatrix,imagePaint);c.save();c.concat(imageMatrix);for(FieldBox b:boxes){boxPaint.setColor(b==active?Color.RED:Color.rgb(37,99,235));c.drawRect(b.rect,boxPaint);textPaint.setTextSize(22/scale);Paint bg=new Paint();bg.setColor(0xCC2563EB);float tw=textPaint.measureText(b.title);c.drawRect(b.rect.right-tw-18/scale,b.rect.top,b.rect.right,b.rect.top+30/scale,bg);c.drawText(b.title,b.rect.right-tw-9/scale,b.rect.top+23/scale,textPaint);c.drawCircle(b.rect.left,b.rect.bottom,12/scale,boxPaint);}c.restore();}
    @Override public boolean onTouchEvent(MotionEvent e){if(page==null)return false;float[] pt={e.getX(),e.getY()};inverse.mapPoints(pt);float x=pt[0],y=pt[1];if(e.getAction()==MotionEvent.ACTION_DOWN){active=find(x,y);if(active==null)return false;resizing=Math.hypot(x-active.rect.left,y-active.rect.bottom)<35;lastX=x;lastY=y;invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_MOVE&&active!=null){float dx=x-lastX,dy=y-lastY;if(resizing){active.rect.left=Math.min(active.rect.right-40,active.rect.left+dx);active.rect.bottom=Math.max(active.rect.top+40,active.rect.bottom+dy);}else active.rect.offset(dx,dy);clamp(active.rect);lastX=x;lastY=y;invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP){active=null;invalidate();return true;}return true;}
    private FieldBox find(float x,float y){for(int i=boxes.size()-1;i>=0;i--){FieldBox b=boxes.get(i);RectF hit=new RectF(b.rect);hit.inset(-25,-25);if(hit.contains(x,y))return b;}return null;}
    private void clamp(RectF r){if(page==null)return;if(r.left<0)r.offset(-r.left,0);if(r.top<0)r.offset(0,-r.top);if(r.right>page.getWidth())r.offset(page.getWidth()-r.right,0);if(r.bottom>page.getHeight())r.offset(0,page.getHeight()-r.bottom);}
}
