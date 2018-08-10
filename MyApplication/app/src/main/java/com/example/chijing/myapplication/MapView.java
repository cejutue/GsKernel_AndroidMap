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
import android.graphics.drawable.shapes.PathShape;
import android.service.quicksettings.Tile;
import android.support.annotation.NonNull;
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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static android.content.ContentValues.TAG;
import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.graphics.Paint.Style.FILL;
import static android.graphics.Paint.Style.FILL_AND_STROKE;
import static android.graphics.Paint.Style.STROKE;
import static android.graphics.Path.Direction.CCW;

public class MapView extends AppCompatImageView {

    GsDisplayTransformation m_DisplayTrans = null;
    GsTileClass m_Tcls = null;
    GsBox m_Box = new GsBox();
    GsSpatialReference m_Spatial = null;//new GsSpatialReference(4326);
    GsPyramid m_Pyramid = new GsPyramid();

    HashMap<TileKey, GsTile> m_TileCache = null;
    int mParentWidth = 0, mParentHeight = 0;
    Bitmap m_Superbitmap = null;
    Canvas m_Cansvas = null;
    Paint m_Panit = null;
    ArrayList<TileKey> m_tileKey =  new ArrayList<TileKey>();

    GsESRIFileGeoDatabaseFactory m_Fac = null;
    GsGeoDatabase m_DB = null;

    GsTile m_ptrTile = null;
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
        m_Fac= new GsESRIFileGeoDatabaseFactory();

        m_DB = m_Fac.Open(conn);
        //GsStringVector v = new GsStringVector();
        //m_DB.DataRoomNames(GsDataRoomType.eTileClass, v);
        m_Tcls = m_DB.OpenTileClass("wutpk3");

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
    protected void BestLevelSet()    {

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
    }
    GsTileCursor m_pCur= null;
    protected  void DrawTilesWithNoCache()    {
        BestLevelSet();

        double res = m_DisplayTrans.Resolution();
        int nLevel = m_Pyramid.BestLevel(res);
        //nLevel = 12;
        int[] range = new int[4];
        double x1  = m_Box.getXMin();
        double y1= m_Box.getYMin();
        double x2 = m_Box.getXMax();
        double y2  = m_Box.getYMax();
        m_Pyramid.TileIndexRange(m_Box.getXMin(), m_Box.getYMin(), m_Box.getXMax(), m_Box.getYMax(), nLevel, range);
        if(m_pCur != null) {
            m_pCur.delete();
            m_pCur = null;
        }

         m_pCur = m_Tcls.Search(nLevel, range[0], range[1], range[2], range[3]);
         if(m_pCur == null)
             return;
        if (m_ptrTile != null) {
            m_ptrTile.delete();
            m_ptrTile= null;
            //m_ptrTile = m_pCur.Next();
        }
        //else
        m_ptrTile =    m_pCur.Next();
        int count = 0;
        m_tileKey.clear();
        boolean g = false;
        do {
            if (m_ptrTile == null)
                break;
            count++;
            long l = m_ptrTile.Level();
            long r = m_ptrTile.Row();
            long c = m_ptrTile.Col();
            TileKey pkey = new TileKey(l, r, c);
            m_tileKey.add(pkey);
            DrawOneTile(m_ptrTile);
            g = m_pCur.Next(m_ptrTile);

        } while (g == true &&!GISHelp.IsEmptyTilePtr(m_ptrTile));


        Log.i("drawtilescount", count + "");
    }
    protected void DrawGridWithNoCache() {
        m_Panit.setColor(Color.BLUE);
        m_Panit.setStyle(STROKE);//设置为空填充,画线
        m_Panit.getStrokeWidth();

        GsRect rc = new GsRect(0, 0, mParentWidth, mParentHeight);
        double[] dblarray = new double[4];
        float[] fr = new float[4];
        RectF rf = new RectF();
        for (TileKey item : m_tileKey) {
            int l = (int)item.l;
            int r = (int)item.r;
            int c = (int)item.c;

            m_Pyramid.TileExtent(l, r, c, dblarray);

            m_DisplayTrans.FromMap(dblarray, 4, 2, fr);
            rf.left = fr[0];
            rf.top = fr[3];
            rf.right = fr[2];
            rf.bottom = fr[1];
            Path path = new Path();
            path.addRect(rf, CCW);
            m_Panit.setColor(Color.BLUE);
            m_Cansvas.drawPath(path, m_Panit);
            String str = "Level=" + l + "Row=" + r + "Col=" + c;
            m_Panit.setColor(Color.RED);
            m_Cansvas.drawText(str, rf.left, rf.bottom, m_Panit);
        }
        m_tileKey.clear();
        Log.i("drawtilesKey", 1 + "");
    }
    protected void DrawOneTile(GsTile tile)    {
        int l = tile.Level();
        int r = tile.Row();
        int c = tile.Col();
        Rect src = new Rect(0, 0, 256, 256);
        RectF dst = new RectF();
        double[] dblarray = new double[4];
        float[] fr = new float[4];
        m_Pyramid.TileExtent(l, r, c, dblarray);

        m_DisplayTrans.FromMap(dblarray, 4, 2, fr);
        dst.left = fr[0];
        dst.top = fr[3];
        dst.right = fr[2];
        dst.bottom = fr[1];
        //GISHelp.LogRefCount(tile);
        Bitmap bmp = GISHelp.Tile2Bitmap(tile);
        //GISHelp.LogRefCount(tile);
        m_Cansvas.drawBitmap(bmp, src, dst, m_Panit);
        bmp.recycle();
        bmp = null;
    }
    @Override
    protected void onDraw(Canvas canvas) {

        m_Cansvas = canvas;

        DrawTilesWithNoCache();
        DrawGridWithNoCache();
        InitFeatureClass();
        DrawFeatureClass();
//        DrawTiles();
//        DrawGrid();
//        ClearCache();
        super.onDraw(canvas);
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


    //------------------矢量绘制-----------------
    GsFeatureClass m_ptrFcs = null;
    GsSqliteGeoDatabaseFactory  m_FcsFac = null;
    GsGeoDatabase m_FcsDB = null;
    public void InitFeatureClass()
    {
        if(m_ptrFcs!= null)
            return;
        GsConnectProperty conn = new GsConnectProperty();
        conn.setServer("/mnt/sdcard/tmp/");
        m_FcsFac = new GsSqliteGeoDatabaseFactory();
        m_FcsDB = m_FcsFac.Open(conn);
        if(m_FcsDB == null)
            return ;
        //m_ptrFcs= m_FcsDB.OpenFeatureClass("RES1_4M_P");
        //m_ptrFcs= m_FcsDB.OpenFeatureClass("BOU2_4M_S");
        m_ptrFcs= m_FcsDB.OpenFeatureClass("地类图斑");
        if(m_ptrFcs == null)
            return;
        //m_Box = m_ptrFcs.GeometryColumnInfo().getXYDomain();
    }

    public void DrawFeatureClass()
    {

        if(m_ptrFcs == null)
            return;
        ViewGroup mViewGroup = (ViewGroup) getParent();
        if (null != mViewGroup) {
            mParentWidth = mViewGroup.getWidth();
            mParentHeight = mViewGroup.getHeight();
        }
        GsRect rc = new GsRect(0, 0, mParentWidth, mParentHeight);
        if (m_DisplayTrans == null)
            m_DisplayTrans = new GsDisplayTransformation(m_Box, rc);
        //GsBox tmpbox = m_DisplayTrans.MapExtent();
        //m_DisplayTrans.MapExtent(m_ptrFcs.GeometryColumnInfo().getXYDomain());
        GsEnvelope pEnv = new GsEnvelope(m_Box);
        GsFeatureCursor pFeatureCursor =  m_ptrFcs.Search(pEnv);
        GsFeature pFea = pFeatureCursor.Next();

        m_Panit.setColor(Color.RED);
        float a = m_Panit.getStrokeWidth();
        m_Panit.setStrokeWidth(5);
        m_Panit.setStyle(STROKE);
        do{
            if(pFea == null)
                break;
            DrawFeature(pFea);
        }while(pFeatureCursor.Next(pFea));
        if(pFeatureCursor!= null)
        {
            pFeatureCursor.delete();
            pFeatureCursor = null;
        }
        m_Panit.setColor(Color.BLUE);
        m_Panit.setStrokeWidth(a);
    }

    double[] g = new double[2];
    float[] gf= new float[2];
    public void DrawFeature(GsFeature pFea)
    {
        if(pFea.GeometryBlob().GeometryType() == GsGeometryType.eGeometryTypePoint )//
        {
            pFea.GeometryBlob().Coordinate(g);
            m_DisplayTrans.FromMap(g,2,2,gf);

        }else if( pFea.GeometryBlob().GeometryType() == GsGeometryType.eGeometryTypeMultiPoint )
        {
            int length =  pFea.GeometryBlob().CoordinateLength();
            double[] gpoints = new double[length];
            float[] gpointsf = new float[length];
            pFea.GeometryBlob().Coordinate(gpoints);
            m_DisplayTrans.FromMap(gpoints,2,2,gpointsf);
            m_Cansvas.drawPoints(gpointsf,0,length,m_Panit);
        }
        else if(pFea.GeometryBlob().GeometryType() == GsGeometryType.eGeometryTypePolyline)
        {
            //m_Panit.setStyle(STROKE);
        }
        else if(pFea.GeometryBlob().GeometryType() == GsGeometryType.eGeometryTypePolygon)
        {
            //m_Panit.setStyle(FILL);
        }
        else
        {

        }
        GsGeometryBlob blob = pFea.GeometryBlob();
        DrawBlob(blob);

    }

    void DrawBlob(GsGeometryBlob pGeo)
    {
        GsGraphicsGeometry GsDisPath = new GsGraphicsGeometry(pGeo,m_DisplayTrans);
        int nCount= GsDisPath.PartCount();
        Path path = new Path();
        for(int i = 0 ; i<nCount; i++ )
        {
            int l =  GsDisPath.PartLength(i);
            float[] fl = new float[l];
            GsDisPath.PartPtr(i,fl);
            path.moveTo(fl[0],fl[1]);
            for(int j = 1; j<l/2;j++)
            {
                path.lineTo(fl[2*j],fl[2*j+1]);
            }
            if(pGeo.GeometryType() == GsGeometryType.eGeometryTypePolygon)
                path.close();
        }
        m_Cansvas.drawPath(path,m_Panit);
    }
    //------------------矢量绘制-----------------


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
//            if(m_DisplayTrans == null)
//                return false;
//
//            double f = m_DisplayTrans.Resolution()/3;
//            if(f>0)
//                m_DisplayTrans.Resolution(m_DisplayTrans.Resolution()-m_DisplayTrans.Resolution()/3);
//            m_Box = m_DisplayTrans.MapExtent();
//            invalidate();
            return true;
        }
    };
}
