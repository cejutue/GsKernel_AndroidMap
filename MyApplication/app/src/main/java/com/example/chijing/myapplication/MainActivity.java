package com.example.chijing.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.geostar.kernel.*;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.loadLibrary("gsjavaport");
        super.onCreate(savedInstanceState);

        setContentView(new MapView(MainActivity.this));

        //setContentView(R.layout.activity_main);

        try {
            //TestNewObj();
           // testKernel();
            //testKernel();
        }catch (Exception ex)
        {
throw ex;
        }
    }

    private void TestNewObj()
    {
        GsSpatialReference d =  new GsSpatialReference(4326);
        for(int i =0; i< 15;i++) {
            d = new GsSpatialReference(4326);
            String spStr = d.toString();
            GsCoordinateSystem f = d.CoordinateSystem();
            GsAny any = new GsAny(1);
            boolean k = GsFileSystem.Exists("//usr");
        }
    }
    private  void testKernel()
    {

        Log.v("Test GsKernel","fhf");
        GsConnectProperty conn = new GsConnectProperty();
        //  conn.setServer("/mnt/sdcard/GeoGlobe/tmp/");
        conn.setServer("/mnt/sdcard/tmp/");
        GsESRIFileGeoDatabaseFactory pFac = new GsESRIFileGeoDatabaseFactory();

        GsGeoDatabase pDB = pFac.Open(conn);
        GsStringVector v = new GsStringVector();
        pDB.DataRoomNames(GsDataRoomType.eTileClass,v);
        GsTileClass pTcs = pDB .OpenTileClass("img");

        if(pTcs == null)
        {
            return ;
        }
        GsTileColumnInfo colOnfo =pTcs.TileColumnInfo();
        int ksd = pTcs.RefCount();
        for(int i = 0; i<10500;i++) {
            GsTileCursor pCur = pTcs.Search(12,15);
            GsTile pTile = pCur.Next();

            ksd = pTile.RefCount();

            int count = 0;
            do {
                if (GISHelp.IsEmptyGsOobject(pTile))
                    break;
                count++;
                long l = pTile.Level();
                long r = pTile.Row();
                long c = pTile.Col();

                pTile.Release();
                pTile = null;
                pTile = pCur.Next();
            } while (!GISHelp.IsEmptyGsOobject(pTile));

            pTile = null;
            pCur.Release();
            pCur = null;
            Log.d("Search",""+i);
            //System.gc();
        }



//        GsSpatialReference d =new GsSpatialReference(4326);
//        String spStr =  d.toString();
//        GsCoordinateSystem f = d.CoordinateSystem();
//        GsAny any = new GsAny(1);
//        boolean k =  GsFileSystem.Exists("//usr");
    }


    private  void testKernel2()
    { //  conn.setServer("/mnt/sdcard/GeoGlobe/tmp/");

        Log.v("Test GsKernel","fhf");
        GsConnectProperty conn = new GsConnectProperty();

        conn.setServer("/mnt/sdcard/tmp/");
        GsSqliteGeoDatabaseFactory pFac = new GsSqliteGeoDatabaseFactory();

        GsGeoDatabase pDB = pFac.Open(conn);
        GsStringVector v = new GsStringVector();
        pDB.DataRoomNames(GsDataRoomType.eTileClass,v);
        GsFeatureClass pTcs = pDB .OpenFeatureClass("RES1_4M_P");

        if(pTcs == null)
        {
            return ;
        }
        GsGeometryColumnInfo colOnfo =pTcs.GeometryColumnInfo();
        int ksd = pTcs.RefCount();
        for(int i = 0; i<500;i++) {
            GsFeatureCursor pCur = pTcs.Search();
            GsFeature pTile = pCur.Next();

            ksd = pTile.RefCount();

            int count = 0;
            do {
                if (GISHelp.IsEmptyFeaturePtr(pTile))
                    break;
                count++;
                GsGeometry pGeo =  pTile.Geometry();
                double a[] = new double[2];
                pGeo.GeometryBlobPtr().Coordinate(a);
                pTile.Release();
                pTile = pCur.Next();
            } while (!GISHelp.IsEmptyFeaturePtr(pTile));

            pTile = null;
            pCur.Release();
            pCur = null;
        }



        GsSpatialReference d =new GsSpatialReference(4326);
        String spStr =  d.toString();
        GsCoordinateSystem f = d.CoordinateSystem();
        GsAny any = new GsAny(1);
        boolean k =  GsFileSystem.Exists("//usr");
    }
}
