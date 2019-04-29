package amap.com.android_path_record;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.text.NoCopySpan;

public class SpashActivity extends Activity {
    @Override
    protected void onStart() {
        super.onStart();
        setContentView(R.layout.basicmap_activity);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                0);
        startActivity(new Intent(SpashActivity.this, MainActivity.class));
    }
}
