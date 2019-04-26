package amap.com;

import android.app.Application;

import amap.com.recorduitl.ObjectBox;

public class App extends Application {

    public static final String TAG = "ObjectBoxExample";

    @Override
    public void onCreate() {
        super.onCreate();
        ObjectBox.init(this);
    }

}