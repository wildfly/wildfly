package org.jboss.as.test.patching;

/**
 * @author Martin Simka
 */
public class ResourceItem {
    private String itemName;
    private byte[] content;

    public ResourceItem(String itemName, byte[] content) {
        if(itemName == null) {
            throw new NullPointerException("itemName");
        }
        if(content == null) {
            throw new NullPointerException("content");
        }
        this.itemName = itemName;
        this.content = content;
    }

    public String getItemName() {
        return itemName;
    }

    public byte[] getContent() {
        return content;
    }
}
