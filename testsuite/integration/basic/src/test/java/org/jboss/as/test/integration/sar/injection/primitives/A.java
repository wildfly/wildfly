package org.jboss.as.test.integration.sar.injection.primitives;

public class A implements AMBean {

    private String text;

    @Override
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }

}
