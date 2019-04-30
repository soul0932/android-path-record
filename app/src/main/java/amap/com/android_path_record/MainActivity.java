package amap.com.android_path_record;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
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


public class MainActivity extends Activity implements AMapLocationListener {
    private MapView mMapView;
    private AMap aMap;
    private Chronometer chronometer;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    private PolylineOptions mPolyoptions;
    private Polyline mpolyline;
    private Record record;
    private long mStartTime;
    private long mEndTime;
    private ToggleButton btn;
    private List<AMapLocation> recordList = new ArrayList<AMapLocation>();
    private AMapLocation lastLocation;
    private float mDistance = 0;
    private TextView mResultShow;
    private Box<Record> recordBox;
    private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);
    private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);
    private DecimalFormat decimalFormat = new DecimalFormat("0.00");
    private boolean firstLocate = true;
    private MyLocationStyle myLocationStyle;
    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = null;
    private int slowTimes = 0;
    private int quickTimes = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.basicmap_activity);
        PackageManager pkgManager = getPackageManager();
        // 读写 sd card 权限非常重要, android6.0默认禁止的, 建议初始化之前就弹窗让用户赋予该权限
        boolean sdCardWritePermission =
                pkgManager.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getPackageName()) == PackageManager.PERMISSION_GRANTED;

        // read phone state用于获取 imei 设备信息
        boolean phoneSatePermission =
                pkgManager.checkPermission(Manifest.permission.READ_PHONE_STATE, getPackageName()) == PackageManager.PERMISSION_GRANTED;

        boolean coarseLocationPermission =
                pkgManager.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, getPackageName()) == PackageManager.PERMISSION_GRANTED;

        boolean fineLocationPermission =
                pkgManager.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, getPackageName()) == PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= 23 && !sdCardWritePermission || !phoneSatePermission ||
                !coarseLocationPermission || !fineLocationPermission) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }

        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);// 此方法必须重写
        recordBox = ObjectBox.get().boxFor(Record.class);
        init();
        initpolyline();
        //初始化定位
        initLocation();
    }

    /**
     * 初始化AMap对象
     */
    private void init() {
        if (aMap == null) {
            aMap = mMapView.getMap();
            setUpMap();
        }
        chronometer = findViewById(R.id.chronometer);
        chronometer.setBase(SystemClock.elapsedRealtime());

        btn = (ToggleButton) findViewById(R.id.locationbtn);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btn.isChecked()) {
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.start();
                    aMap.clear(true);
                    //启动后台定位
                    locationClient.startLocation();
                    locationClient.enableBackgroundLocation(2001, buildNotification());
                    if (record != null) {
                        record = null;
                    }
                    record = new Record();
                    mStartTime = System.currentTimeMillis();
                    record.time = (getcueDate(mStartTime));
                    mResultShow.setText("总距离");
                    mDistance = 0;
                } else {
                    chronometer.stop();
                    locationClient.stopLocation();
                    locationClient.disableBackgroundLocation(true);
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

    private void initLocation() {
        //初始化client
        locationClient = new AMapLocationClient(this.getApplicationContext());
        locationOption = new AMapLocationClientOption();
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        locationOption.setInterval(3000);//可选，设置定位间隔。默认为2秒
        //设置定位参数
        locationClient.setLocationOption(locationOption);
        // 设置定位监听
        locationClient.setLocationListener(this);
    }

    protected void saveRecord() {
        if (recordList != null && recordList.size() > 0) {
            record.average = getAverage(mDistance);
            record.distance = getFloat(mDistance);
            record.duration = getChronometerSeconds(chronometer);
            record.time = (getcueDate(mStartTime));
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
        float average;
        average = (distance * 3600) / (mEndTime - mStartTime);
        return String.valueOf(getFloat(average));
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
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW);//只定位一次。
        // 将自定义的 myLocationStyle 对象添加到地图上
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.moveCamera(CameraUpdateFactory.zoomTo(18));
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
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
        locationClient.onDestroy();
        locationClient = null;
        locationOption = null;
    }

    /**
     * 实时轨迹画线
     */
    private void redrawline() {
        if (mPolyoptions.getPoints().size() > 1) {
            if (mpolyline != null) {
                mpolyline.setPoints(mPolyoptions.getPoints());
            } else {
                mpolyline = aMap.addPolyline(mPolyoptions);
            }
        }

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

    private void getDistance(AMapLocation curLocation) {
        if (lastLocation == null)
            return;
        float distance;
        distance = AMapUtils.calculateLineDistance(new LatLng(lastLocation.getLatitude(),
                lastLocation.getLongitude()), new LatLng(curLocation.getLatitude(),
                curLocation.getLongitude()));
        mDistance = mDistance + distance;
        mResultShow.setText("总距离:" + decimalFormat.format(mDistance / 1000d) + "KM");
    }

    private float getFloat(float x) {
        return (float) (Math.round(x * 100) / 100);
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {

        if (aMapLocation != null) {

            if (aMapLocation.getSpeed() < 0.6) {
                quickTimes = 0;
                slowTimes++;
            } else {
                quickTimes++;
                slowTimes = 0;
            }
            if (slowTimes > 2)
                chronometer.stop();
            if (quickTimes > 2)
                chronometer.start();
            if (aMapLocation.getSpeed() > 0.2 && aMapLocation.getAccuracy() < 60) {
                LatLng mylocation=new LatLng(aMapLocation.getLatitude(),
                        aMapLocation.getLongitude());
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mylocation, 17));
                Toast.makeText(this, "当前速度" + aMapLocation.getSpeed() + "m/s", Toast.LENGTH_SHORT).show();
                Log.e("amap", aMapLocation.getLongitude() + "");
                getDistance(aMapLocation);
                lastLocation = aMapLocation;
                LocationEntity entity = new LocationEntity();
                entity.latitude = aMapLocation.getLatitude();
                entity.longitude = aMapLocation.getLongitude();
                entity.location = aMapLocation.getLongitude() + "," + aMapLocation.getLatitude();
                entity.accuracy = aMapLocation.getAccuracy();
                entity.direction = aMapLocation.getBearing();
                record.locationPoints.add(entity);
                recordBox.put(record);
                recordList.add(aMapLocation);
                mPolyoptions.add(mylocation);
                redrawline();
            }
        } else {
            Log.e("amap", "定位失败");
        }
    }

    private static final String NOTIFICATION_CHANNEL_NAME = "BackgroundLocation";
    private NotificationManager notificationManager = null;
    boolean isCreateChannel = false;

    @SuppressLint("NewApi")
    private Notification buildNotification() {

        Notification.Builder builder = null;
        Notification notification = null;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            //Android O上对Notification进行了修改，如果设置的targetSDKVersion>=26建议使用此种方式创建通知栏
            if (null == notificationManager) {
                notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            }
            String channelId = getPackageName();
            if (!isCreateChannel) {
                NotificationChannel notificationChannel = new NotificationChannel(channelId,
                        NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
                notificationChannel.enableLights(true);//是否在桌面icon右上角展示小圆点
                notificationChannel.setLightColor(Color.BLUE); //小圆点颜色
                notificationChannel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
                notificationManager.createNotificationChannel(notificationChannel);
                isCreateChannel = true;
            }
            builder = new Notification.Builder(getApplicationContext(), channelId);
        } else {
            builder = new Notification.Builder(getApplicationContext());
        }
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("车优道")
                .setContentText("正在后台运行")
                .setWhen(System.currentTimeMillis());

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            notification = builder.build();
        } else {
            return builder.getNotification();
        }
        return notification;
    }

    /**
     * @param cmt Chronometer控件
     * @return 小时+分钟+秒数  的所有秒数
     */
    public static String getChronometerSeconds(Chronometer cmt) {
        int totalss = 0;
        String string = cmt.getText().toString();
        if (string.length() == 7) {

            String[] split = string.split(":");
            String string2 = split[0];
            int hour = Integer.parseInt(string2);
            int Hours = hour * 3600;
            String string3 = split[1];
            int min = Integer.parseInt(string3);
            int Mins = min * 60;
            int SS = Integer.parseInt(split[2]);
            totalss = Hours + Mins + SS;
            return String.valueOf(totalss);
        } else if (string.length() == 5) {

            String[] split = string.split(":");
            String string3 = split[0];
            int min = Integer.parseInt(string3);
            int Mins = min * 60;
            int SS = Integer.parseInt(split[1]);

            totalss = Mins + SS;
            return String.valueOf(totalss);
        }
        return String.valueOf(totalss);
    }

}
