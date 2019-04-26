package amap.com.recorduitl;

import android.content.Context;
import android.util.Log;

import amap.com.App;
import amap.com.record.MyObjectBox;
import io.objectbox.BoxStore;
import io.objectbox.android.AndroidObjectBrowser;
import io.objectbox.android.BuildConfig;

public class ObjectBox {

    private static BoxStore boxStore;

    public static void init(Context context) {
        boxStore = MyObjectBox.builder()
                .androidContext(context.getApplicationContext())
                .build();

        if (BuildConfig.DEBUG) {
            new AndroidObjectBrowser(boxStore).start(context.getApplicationContext());
            Log.d(App.TAG, String.format("Using ObjectBox %s (%s)",
                    BoxStore.getVersion(), BoxStore.getVersionNative()));
        }
    }

    public static BoxStore get() {
        return boxStore;
    }
}
