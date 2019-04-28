package amap.com.android_path_record;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import amap.com.database.DbAdapter;
import amap.com.record.PathRecord;
import amap.com.record.Record;
import amap.com.record.Record_;
import amap.com.recorduitl.ObjectBox;
import io.objectbox.Box;

/**
 * 所有轨迹list展示activity
 *
 */
public class RecordActivity extends Activity implements OnItemClickListener {

	private RecordAdapter mAdapter;
	private ListView mAllRecordListView;
	private DbAdapter mDataBaseHelper;
	private List<PathRecord> mAllRecord = new ArrayList<PathRecord>();
	private Box<Record> recordBox;
	private List<Record> records;
	public static final String RECORD_ID = "record_id";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recordlist);
		mAllRecordListView = (ListView) findViewById(R.id.recordlist);
		recordBox = ObjectBox.get().boxFor(Record.class);
		records = recordBox.query().order(Record_.time).build().find();
		mAdapter = new RecordAdapter(this, records);
		mAllRecordListView.setAdapter(mAdapter);
		mAllRecordListView.setOnItemClickListener(this);
	}

	public void onBackClick(View view) {
		this.finish();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Record recorditem = (Record) parent.getAdapter().getItem(
				position);
		Intent intent = new Intent(RecordActivity.this,
				RecordShowActivity.class);
		intent.putExtra(RECORD_ID, recorditem.id);
		startActivity(intent);
	}
}
