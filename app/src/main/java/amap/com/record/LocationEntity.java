package amap.com.record;


import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class LocationEntity {
    @Id
    public long id;
    public double latitude;
    public double longitude;
}
