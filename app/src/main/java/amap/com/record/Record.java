package amap.com.record;

import com.amap.api.location.AMapLocation;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Transient;
import io.objectbox.relation.ToMany;

/**
 * 用于记录一条轨迹，包括起点、终点、轨迹中间点、距离、耗时、平均速度、时间
 *
 * @author ligen
 */
@Entity
public class Record {

    @Id
    public long id;
    public float distance;
    public String duration;
    public String average;
    public String pathlineSring;
    public String stratpoint;
    public String endpoint;
    public String time;
    public ToMany<LocationEntity> locationPoints ;
}
