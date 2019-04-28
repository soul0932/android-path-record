package amap.com.android_path_record;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import amap.com.record.LocationEntity;
import amap.com.record.Record;
import amap.com.recorduitl.ObjectBox;
import io.objectbox.Box;


public class MainActivity extends Activity implements AMap.OnMyLocationChangeListener {
    private MapView mMapView;
    private AMap aMap;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    private PolylineOptions mPolyoptions;
    private Polyline mpolyline;
    private Record record;
    private long mStartTime;
    private long mEndTime;
    private ToggleButton btn;
    private List<Location> recordList = new ArrayList<Location>();
    private Location lastLocation;
    private float mDistance = 0;
    private TextView mResultShow;
    private Box<Record> recordBox;
    private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);
    private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);
    private DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private boolean firstLocate = true;
    MyLocationStyle myLocationStyle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.basicmap_activity);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                0);
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);// 此方法必须重写
        recordBox = ObjectBox.get().boxFor(Record.class);
        init();
        initpolyline();
    }

    /**
     * 初始化AMap对象
     */
    private void init() {
        if (aMap == null) {
            aMap = mMapView.getMap();
            aMap.moveCamera(CameraUpdateFactory.zoomTo(18));
            setUpMap();
        }
        btn = (ToggleButton) findViewById(R.id.locationbtn);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btn.isChecked()) {
                    aMap.clear(true);
                    if (record != null) {
                        record = null;
                    }
                    record = new Record();
                    mStartTime = System.currentTimeMillis();
                    mResultShow.setText("总距离");
                } else {
                    mEndTime = System.currentTimeMillis();
                    mResultShow.setText(
                            decimalFormat.format(mDistance / 1000d) + "KM");
                    //LBSTraceClient mTraceClient = new LBSTraceClient(getApplicationContext());
                    // mTraceClient.queryProcessedTrace(2, Util.parseTraceLocationList(recordList), LBSTraceClient.TYPE_AMAP, MainActivity.this);
                    saveRecord();
                }
            }
        });
        mResultShow = (TextView) findViewById(R.id.show_all_dis);
    }

    protected void saveRecord() {
        if (recordList != null && recordList.size() > 0) {
          /*  DbHepler = new DbAdapter(this);
            DbHepler.open();
            String duration = getDuration();
            float distance = getDistance(list);
            String average = getAverage(distance);
            String pathlineSring = getPathLineString(list);
            AMapLocation firstLocaiton = list.get(0);
            AMapLocation lastLocaiton = list.get(list.size() - 1);
            String stratpoint = amapLocationToString(firstLocaiton);
            String endpoint = amapLocationToString(lastLocaiton);
            DbHepler.createrecord(String.valueOf(distance), duration, average,
                    pathlineSring, stratpoint, endpoint, time);
            DbHepler.close();*/
            String duration = getDuration();
            String average = getAverage(mDistance);
            Record record = new Record();
            record.average = average;
            record.distance = mDistance;
            record.duration = duration;
            record.average = average;
            record.time = (getcueDate(mStartTime));
            for (Location item : recordList) {
                LocationEntity entity = new LocationEntity();
                entity.latitude = item.getLatitude();
                entity.longitude = item.getLongitude();
                entity.location = item.getLongitude() + "," + item.getLatitude();
                entity.accuracy = item.getAccuracy();
                entity.direction = item.getBearing();
                record.locationPoints.add(entity);
            }
            recordBox.put(record);
        } else {
            Toast.makeText(MainActivity.this, "没有记录到路径", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private String getDuration() {
        return String.valueOf((mEndTime - mStartTime) / 1000f);
    }

    private String getAverage(float distance) {
        return String.valueOf(distance / (float) (mEndTime - mStartTime));
    }
/*
    private float getDistance(List<Location> list) {
        float distance = 0;
        if (list == null || list.size() == 0) {
            return distance;
        }
        for (int i = 0; i < list.size() - 1; i++) {
            Location firstpoint = list.get(i);
            Location secondpoint = list.get(i + 1);
            LatLng firstLatLng = new LatLng(firstpoint.getLatitude(),
                    firstpoint.getLongitude());
            LatLng secondLatLng = new LatLng(secondpoint.getLatitude(),
                    secondpoint.getLongitude());
            double betweenDis = AMapUtils.calculateLineDistance(firstLatLng,
                    secondLatLng);
            distance = (float) (distance + betweenDis);
        }
        return distance;
    }*/

    private String getPathLineString(List<AMapLocation> list) {
        if (list == null || list.size() == 0) {
            return "";
        }
        StringBuffer pathline = new StringBuffer();
        for (int i = 0; i < list.size(); i++) {
            AMapLocation location = list.get(i);
            String locString = amapLocationToString(location);
            pathline.append(locString).append(";");
        }
        String pathLineString = pathline.toString();
        pathLineString = pathLineString.substring(0,
                pathLineString.length() - 1);
        return pathLineString;
    }

    private String amapLocationToString(AMapLocation location) {
        StringBuffer locString = new StringBuffer();
        locString.append(location.getLatitude()).append(",");
        locString.append(location.getLongitude()).append(",");
        locString.append(location.getProvider()).append(",");
        locString.append(location.getTime()).append(",");
        locString.append(location.getSpeed()).append(",");
        locString.append(location.getBearing());
        return locString.toString();
    }

    private void initpolyline() {
        mPolyoptions = new PolylineOptions();
        mPolyoptions.width(10f);
        mPolyoptions.color(Color.BLUE);
    }

    /**
     * 设置一些amap的属性
     */
    private void setUpMap() {
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        // 自定义系统定位蓝点
        myLocationStyle = new MyLocationStyle();
        // 自定义定位蓝点图标
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.
                fromResource(R.drawable.gps_point));
        // 自定义精度范围的圆形边框颜色
        myLocationStyle.strokeColor(STROKE_COLOR);
        //自定义精度范围的圆形边框宽度
        myLocationStyle.strokeWidth(3);
        // 设置圆形的填充颜色
        myLocationStyle.radiusFillColor(FILL_COLOR);
        // 将自定义的 myLocationStyle 对象添加到地图上
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setOnMyLocationChangeListener(this);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    /**
     * 实时轨迹画线
     */
    private void redrawline(Location curLocation) {
        if (curLocation.getLatitude() != 0.0 && curLocation.getLongitude() != 0.0
                && lastLocation.getLongitude() != 0.0 && lastLocation.getLatitude() != 0.0) {
            PolylineOptions options = new PolylineOptions();
            //上一个点的经纬度
            options.add(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
            //当前的经纬度
            options.add(new LatLng(curLocation.getLatitude(), curLocation.getLongitude()));
            options.width(10).geodesic(true).color(Color.GREEN);
            aMap.addPolyline(options);
        }
      /*  if (mPolyoptions.getPoints().size() > 1) {
            if (mpolyline != null) {
                mpolyline.setPoints(mPolyoptions.getPoints());
            } else {
                mpolyline = aMap.addPolyline(mPolyoptions);
            }
        }*/
//		if (mpolyline != null) {
//			mpolyline.remove();
//		}
//		mPolyoptions.visible(true);
//		mpolyline = mAMap.addPolyline(mPolyoptions);
//			PolylineOptions newpoly = new PolylineOptions();
//			mpolyline = mAMap.addPolyline(newpoly.addAll(mPolyoptions.getPoints()));
//		}
    }

    @SuppressLint("SimpleDateFormat")
    private String getcueDate(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "yyyy-MM-dd  HH:mm:ss ");
        Date curDate = new Date(time);
        String date = formatter.format(curDate);
        return date;
    }

    public void record(View view) {
        Intent intent = new Intent(MainActivity.this, RecordActivity.class);
        startActivity(intent);
    }

    private void getDistance(Location curLocation) {
        if (lastLocation == null)
            return;
        float distance;
        distance = AMapUtils.calculateLineDistance(new LatLng(lastLocation.getLatitude(),
                lastLocation.getLongitude()), new LatLng(curLocation.getLatitude(),
                curLocation.getLongitude()));
        mDistance = mDistance + distance;
        mResultShow.setText("总距离:" + decimalFormat.format(mDistance / 1000d) + "KM");
    }

    @Override
    public void onMyLocationChange(Location location) {
        if (firstLocate) {
            myLocationStyle.interval(5000);
            // 将自定义的 myLocationStyle 对象添加到地图上
            aMap.setMyLocationStyle(myLocationStyle);
            firstLocate = false;
        }
        if (location != null) {
            getDistance(location);
            lastLocation = location;
            LatLng mylocation = new LatLng(location.getLatitude(),
                    location.getLongitude());
            recordList.add(location);
            mPolyoptions.add(mylocation);
            redrawline(location);
        } else {
            Log.e("amap", "定位失败");
        }
    }
}
