package vhu.edu.vn.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import vhu.edu.vn.utils.Constants;

@Entity(
    tableName = "parts_inventory",
    indices = {@Index(value = {"partCode"}, unique = true)}
)
public class InventoryPartEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String partCode;
    private String partName;
    private String category;
    private int quantity;
    private double importPrice;
    private double sellingPrice;
    private int minThreshold;

    public InventoryPartEntity(String partCode, String partName, String category, int quantity, double importPrice, double sellingPrice, int minThreshold) {
        this.partCode = partCode;
        this.partName = partName;
        this.category = category;
        this.quantity = quantity;
        this.importPrice = importPrice;
        this.sellingPrice = sellingPrice;
        this.minThreshold = minThreshold <= 0 ? Constants.DEFAULT_MIN_STOCK_THRESHOLD : minThreshold;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getPartCode() { return partCode; }
    public void setPartCode(String partCode) { this.partCode = partCode; }
    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getImportPrice() { return importPrice; }
    public void setImportPrice(double importPrice) { this.importPrice = importPrice; }
    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }
    public int getMinThreshold() { return minThreshold; }
    public void setMinThreshold(int minThreshold) { this.minThreshold = minThreshold; }
}
