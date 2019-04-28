package amap.com.android_path_record;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.amap.api.location.AMapLocation;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnMapLoadedListener;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.trace.LBSTraceClient;
import com.amap.api.trace.TraceListener;
import com.amap.api.trace.TraceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import amap.com.database.DbAdapter;
import amap.com.record.LocationEntity;
import amap.com.record.PathRecord;
import amap.com.record.Record;
import amap.com.record.Record_;
import amap.com.recorduitl.ObjectBox;
import amap.com.recorduitl.Util;
import amap.com.tracereplay.TraceRePlay;
import io.objectbox.Box;


/**
 * 实现轨迹回放、纠偏后轨迹回放
 */
public class RecordsShowActivity extends Activity {


    private MapView mMapView;
    private AMap mAMap;
    private Polyline mPolyline;

    private long mRecordItemId;
    private List<LatLng> mLatLngList = new ArrayList<>();
    private PolylineOptions mPolyoptions;
    private Polyline mpolyline;
    private Box<Record> recordBox;
    private Record record;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recordshow_activity);
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);// 此方法必须重写
        mRecordItemId = getIntent().getLongExtra("record_id", -1);
        recordBox = ObjectBox.get().boxFor(Record.class);
        record = recordBox.query().equal(Record_.__ID_PROPERTY, mRecordItemId).build().findUnique();
        initMap();
        initpolyline();
        redrawline();
        Toast.makeText(this,"总距离"+record.distance+"平均速度"+record.average,Toast.LENGTH_SHORT).show();
    }


    private void initMap() {
        if (mAMap == null) {
            mAMap = mMapView.getMap();
        }
    }

    private void initpolyline() {
        mPolyoptions = new PolylineOptions();
        mPolyoptions.width(10f);
        mPolyoptions.color(Color.BLUE);
        if (record.locationPoints.size() > 0) {
            for (LocationEntity entity : record.locationPoints) {
                LatLng mylocation = new LatLng(entity.latitude, entity.longitude);
                mLatLngList.add(mylocation);
                mPolyoptions.add(mylocation);
            }
        }
    }

    /**
     * 实时轨迹画线
     */
    private void redrawline() {
        if (mPolyoptions.getPoints().size() > 1) {
            mAMap.animateCamera(CameraUpdateFactory
                    .newLatLngBounds(getBounds(), 100));
            if (mpolyline != null) {
                mpolyline.setPoints(mPolyoptions.getPoints());
            } else {
                mpolyline = mAMap.addPolyline(mPolyoptions);
            }
        }
    }

    public void onBackClick(View view) {
        this.finish();
    }

    public void onDestroy() {
        super.onDestroy();

    }

    private LatLngBounds getBounds() {
        LatLngBounds.Builder b = LatLngBounds.builder();
        if (mLatLngList == null) {
            return b.build();
        }
        for (int i = 0; i < mLatLngList.size(); i++) {
            b.include(mLatLngList.get(i));
        }
        return b.build();

    }

}
