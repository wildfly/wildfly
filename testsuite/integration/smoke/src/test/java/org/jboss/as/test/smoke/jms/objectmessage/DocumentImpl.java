package org.jboss.as.test.smoke.jms.objectmessage;


public class DocumentImpl implements Document {

    private static final long serialVersionUID = -5219286757437748880L;

    private final String text;

    public DocumentImpl(String text) {
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }

}
