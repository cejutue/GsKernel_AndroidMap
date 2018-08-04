package com.example.chijing.myapplication;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Matrix;
import com.geostar.kernel.*;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;
import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.graphics.Paint.Style.STROKE;
import static android.graphics.Path.Direction.CCW;

public class MapView extends AppCompatImageView {

    GsDisplayTransformation m_DisplayTrans = null;
    GsTileClass m_Tcls = null;
    GsBox m_Box = new GsBox();
    GsSpatialReference m_Spatial = new GsSpatialReference(4326);
    GsPyramid m_Pyramid = new GsPyramid();

    HashMap<TileKey, GsTile> m_TileCache = null;
    int mParentWidth = 0, mParentHeight = 0;
    Bitmap m_Superbitmap = null;
    Canvas m_Cansvas = null;
    Paint m_Panit = null;



    void Init() {
        m_Panit = new Paint();
        m_Panit.setAntiAlias(true);
        setClickable(true);// 设置为可点击控件
        if (m_Tcls != null)
            return;
        m_TileCache = new HashMap<TileKey, GsTile>();
        GsConnectProperty conn = new GsConnectProperty();
        //conn.setServer("/mnt/sdcard/GeoGlobe/tmp/");
        conn.setServer("/mnt/sdcard/tmp/");
        GsESRIFileGeoDatabaseFactory pFac = new GsESRIFileGeoDatabaseFactory();

        GsGeoDatabase pDB = pFac.Open(conn);
        GsStringVector v = new GsStringVector();
        pDB.DataRoomNames(GsDataRoomType.eTileClass, v);
        m_Tcls = pDB.OpenTileClass("img.tpk");

        m_Box = m_Tcls.TileColumnInfo().getXYDomain();
        m_Spatial = m_Tcls.SpatialReference();
        String WKT = m_Spatial.ExportToWKT();
        m_Pyramid = m_Tcls.Pyramid();
        View mView = (View) getParent();
        ViewGroup mViewGroup = (ViewGroup) getParent();
        if (null != mViewGroup) {
            mParentWidth = mViewGroup.getWidth();
            mParentHeight = mViewGroup.getHeight();
        }
        double x2 = m_Box.getXMax();
        double xq= m_Box.getXMin();
        GsRect rc = new GsRect(0, 0, mParentWidth, mParentHeight);
        m_DisplayTrans = new GsDisplayTransformation(m_Box, rc);
         x2 = m_Box.getXMax();
         xq= m_Box.getXMin();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        m_Cansvas = canvas;

        DrawTiles();
        DrawGrid();
        super.onDraw(canvas);
    }

    private void InitCache() {
        double res = m_DisplayTrans.Resolution();
        int nLevel = m_Pyramid.BestLevel(res);
        int[] range = new int[4];
        m_Pyramid.TileIndexRange(m_Box.getXMin(), m_Box.getYMin(), m_Box.getXMax(), m_Box.getYMax(), nLevel, range);
        GsTileCursor pCur = m_Tcls.Search(nLevel, range[0], range[1], range[2], range[3]);
        GsTile pTile = pCur.Next();
        int count = 0;

        do {
            if (GISHelp.IsEmptyTilePtr(pTile))
                break;
            count++;
            long l = pTile.Level();
            long r = pTile.Row();
            long c = pTile.Col();
            TileKey pkey = new TileKey(l, r, c);

            if (!m_TileCache.containsKey(pkey))
                m_TileCache.put(pkey, pTile);
            pTile = pCur.Next();

        } while (!GISHelp.IsEmptyTilePtr(pTile));

        //pCur.delete();
        //pCur = null;
        //System.gc();
        Log.i("tilescount", count + "");
    }

    public MapView(Context context) {
        super(context);
        Init();
        InitEvent();
    }


    public MapView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        Init();
        InitEvent();
    }

    public MapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Init();
        InitEvent();
    }

    protected void DrawTiles() {
        Log.d("DrawTile",""+1);
        ViewGroup mViewGroup = (ViewGroup) getParent();
        if (null != mViewGroup) {
            mParentWidth = mViewGroup.getWidth();
            mParentHeight = mViewGroup.getHeight();
        }
        GsRect rc = new GsRect(0, 0, mParentWidth, mParentHeight);
        if (m_DisplayTrans == null)
            m_DisplayTrans = new GsDisplayTransformation(m_Box, rc);
        double x2 = m_Box.getXMax();
        double xq= m_Box.getXMin();
        m_DisplayTrans.DeviceExtent(rc);
        m_DisplayTrans.MapExtent(m_Box);

        m_TileCache.clear();
        InitCache();


        Rect src = new Rect(0, 0, 256, 256);
        RectF dst = new RectF();
        double[] dblarray = new double[4];
        float[] fr = new float[4];

        for (Map.Entry<TileKey, GsTile> item : m_TileCache.entrySet()) {
            int l = item.getValue().Level();
            int r = item.getValue().Row();
            int c = item.getValue().Col();

            m_Pyramid.TileExtent(l, r, c, dblarray);

            m_DisplayTrans.FromMap(dblarray, 4, 2, fr);
            dst.left = fr[0];
            dst.top = fr[3];
            dst.right = fr[2];
            dst.bottom = fr[1];

            Bitmap bmp = GISHelp.Tile2Bitmap(item.getValue());
            m_Cansvas.drawBitmap(bmp, src, dst, m_Panit);

        }
        Log.d("DrawTile",""+2);
    }

    protected void DrawGrid() {
        Log.d("DraeGrid",""+2);
        m_Panit.setColor(Color.BLUE);
        m_Panit.setStyle(STROKE);//设置为空填充,画线

        GsRect rc = new GsRect(0, 0, mParentWidth, mParentHeight);
        double[] dblarray = new double[4];
        float[] fr = new float[4];
        RectF rf = new RectF();
        for (Map.Entry<TileKey, GsTile> item : m_TileCache.entrySet()) {
            int l = item.getValue().Level();
            int r = item.getValue().Row();
            int c = item.getValue().Col();

            m_Pyramid.TileExtent(l, r, c, dblarray);

            m_DisplayTrans.FromMap(dblarray, 4, 2, fr);
            rf.left = fr[0];
            rf.top = fr[3];
            rf.right = fr[2];
            rf.bottom = fr[1];
            Path path = new Path();
            path.addRect(rf, CCW);
            // float[] a= {rf.left,rf.bottom,rf.right,rf.bottom,rf.right,rf.top,rf.left,rf.top,rf.left,rf.bottom};
            m_Panit.setColor(Color.BLUE);
            m_Cansvas.drawPath(path, m_Panit);
            String str = "Level=" + l + "Row=" + r + "Col=" + c;
            m_Panit.setColor(Color.RED);
            m_Cansvas.drawText(str, rf.left, rf.bottom, m_Panit);
            item.getValue().delete();

        }

        Log.d("DraeGrid",""+3);
    }


    private GestureDetectorCompat m_GestureDetectorCompat = null;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return m_GestureDetectorCompat.onTouchEvent(event);
    }

    void InitEvent()
    {

        m_GestureDetectorCompat = new  GestureDetectorCompat( this.getContext(), new GestureListener());

        //setOnTouchListener(m_GestureDetectorCompat.);
        setFocusable(true);
        setClickable(true);
        setLongClickable(true);
    }

    private class GestureListener   implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener{
        public boolean onDown(MotionEvent e) {
            //showlog("onDown");
           // Toast.makeText(MainActivity.this, "onDown", Toast.LENGTH_SHORT).show();
return true;
        }

        public void onShowPress(MotionEvent e) {
            //showlog("onShowPress");
           // Toast.makeText(MainActivity.this, "onShowPress", Toast.LENGTH_SHORT).show();
        }

        public boolean onSingleTapUp(MotionEvent e) {
            if(m_DisplayTrans == null)
                return false;

            double f = m_DisplayTrans.Resolution()/3;
            if(f>0)
                m_DisplayTrans.Resolution(m_DisplayTrans.Resolution()+m_DisplayTrans.Resolution()/3);
            m_Box = m_DisplayTrans.MapExtent();
            invalidate();
            return true;
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                float distanceX, float distanceY) {
            //showlog("onScroll:"+(e2.getX()-e1.getX()) +"   "+distanceX);
           // Toast.makeText(MainActivity.this, "onScroll", Toast.LENGTH_LONG).show();
            return true;
        }

        public void onLongPress(MotionEvent e) {
            //showlog("onLongPress");
            //Toast.makeText(MainActivity.this, "onLongPress", Toast.LENGTH_LONG).show();
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            //showlog("onFling");
            //Toast.makeText(MainActivity.this, "onFling", Toast.LENGTH_LONG).show();
            return true;
        }

        public boolean onSingleTapConfirmed(MotionEvent e) {
            //showlog("onSingleTapConfirmed");
            //Toast.makeText(MainActivity.this, "onSingleTapConfirmed",Toast.LENGTH_LONG).show();
            return true;
        }

        public boolean onDoubleTap(MotionEvent e) {
            //showlog("onDoubleTap");
            //Toast.makeText(MainActivity.this, "onDoubleTap", Toast.LENGTH_LONG).show();
            return true;
        }

        public boolean onDoubleTapEvent(MotionEvent e) {
            if(m_DisplayTrans == null)
                return false;

            double f = m_DisplayTrans.Resolution()/3;
            if(f>0)
                m_DisplayTrans.Resolution(m_DisplayTrans.Resolution()-m_DisplayTrans.Resolution()/3);
            m_Box = m_DisplayTrans.MapExtent();
            invalidate();
            return true;
        }
    };
}
