package org.zstack.sdk;

public class QueryResourceResult {
    public java.util.List<ResourceInventory> inventories;
    public void setInventories(java.util.List<ResourceInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<ResourceInventory> getInventories() {
        return this.inventories;
    }

    public java.lang.Long total;
    public void setTotal(java.lang.Long total) {
        this.total = total;
    }
    public java.lang.Long getTotal() {
        return this.total;
    }

}
