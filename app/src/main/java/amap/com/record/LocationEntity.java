package amap.com.record;


import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class LocationEntity {
    @Id
    public long id;
    public double latitude;
    public double longitude;
    public long locatetime;
    public String location;
    public long speed;
    public float direction;
    public float accuracy;
}
