package amap.com.record;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

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
}
