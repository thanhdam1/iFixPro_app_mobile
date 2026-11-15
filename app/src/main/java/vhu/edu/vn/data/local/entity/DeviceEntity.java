package vhu.edu.vn.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "devices",
    foreignKeys = @ForeignKey(
        entity = UserEntity.class,
        parentColumns = "id",
        childColumns = "customerId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = {"customerId"}),
        @Index(value = {"imeiSerial"}, unique = true)
    }
)
public class DeviceEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int customerId;
    private String deviceType;
    private String brandModel;
    private String imeiSerial;
    private String screenPassword;
    private String initialCondition;
    private String imagePath;

    public DeviceEntity(int customerId, String deviceType, String brandModel, String imeiSerial, String screenPassword, String initialCondition, String imagePath) {
        this.customerId = customerId;
        this.deviceType = deviceType;
        this.brandModel = brandModel;
        this.imeiSerial = imeiSerial;
        this.screenPassword = screenPassword;
        this.initialCondition = initialCondition;
        this.imagePath = imagePath;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getBrandModel() { return brandModel; }
    public void setBrandModel(String brandModel) { this.brandModel = brandModel; }
    public String getImeiSerial() { return imeiSerial; }
    public void setImeiSerial(String imeiSerial) { this.imeiSerial = imeiSerial; }
    public String getScreenPassword() { return screenPassword; }
    public void setScreenPassword(String screenPassword) { this.screenPassword = screenPassword; }
    public String getInitialCondition() { return initialCondition; }
    public void setInitialCondition(String initialCondition) { this.initialCondition = initialCondition; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}
