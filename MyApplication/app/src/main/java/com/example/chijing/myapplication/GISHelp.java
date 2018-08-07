package com.example.chijing.myapplication;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.geostar.kernel.GsFeature;
import com.geostar.kernel.GsRefObject;
import com.geostar.kernel.GsRow;
import com.geostar.kernel.GsTile;

import static android.graphics.Bitmap.Config.ARGB_8888;

class TileKey
{

    public long l = 0;
    public long r = 0;
    public  long c = 0;
    public TileKey(long ll, long rr, long cc) {
        l = ll; r = rr; c =cc;
    }
}
public class GISHelp {

    public static   boolean IsEmptyGsOobject(GsRefObject pTile)
    {
        if(pTile == null || GsRefObject.getCPtr(pTile) == 0)
            return true;
        GsRefObject pRow = pTile;
        if( GsRefObject.getCPtr(pRow) == 0)
            return true;
        else
            return false;
    }
    public static   boolean IsEmptyTilePtr(GsTile pTile)
    {
        if(pTile == null || GsTile.getCPtr(pTile) == 0)
            return true;
        GsRow pRow = pTile;
        if( GsRow.getCPtr(pRow) == 0)
            return true;
        else
            return false;
    }

    public static   boolean IsEmptyFeaturePtr(GsFeature pTile)
    {
        if(pTile == null || GsFeature.getCPtr(pTile) == 0)
            return true;
        GsRow pRow = pTile;
        if( GsRow.getCPtr(pRow) == 0)
            return true;
        else
            return false;
    }
    public static   void TileDelte(GsTile pTile)
    {
        if(pTile == null || GsTile.getCPtr(pTile) == 0)
            return ;
        GsRow pRow = pTile;
        if( GsRow.getCPtr(pRow) != 0)
            pRow.delete();
        pTile = null;
        pRow = null;
    }
    public static Bitmap Tile2Bitmap(GsTile pTile)
    {
        if(pTile ==null && GsTile.getCPtr(pTile)==0)
        {
            return Bitmap.createBitmap(256,256,ARGB_8888);
        }
        byte[] pdata = new byte[pTile.TileDataLength()];
        pTile.TileDataPtr(pdata);

        return BitmapFactory.decodeByteArray(pdata,0,pTile.TileDataLength());
    }

    public static void LogRefCount(GsRefObject pRef)
    {
        Log.d(""+pRef.toString(),""+pRef.RefCount());
    }
}
