package org.zstack.header.image;

import org.zstack.header.message.APIEvent;

/**
 * Created by xing5 on 2016/5/6.
 */
public class APISyncImageActualSizeEvent extends APIEvent {
    private ImageInventory inventory;

    public APISyncImageActualSizeEvent() {
    }

    public APISyncImageActualSizeEvent(String apiId) {
        super(apiId);
    }

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
