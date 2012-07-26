package org.jboss.as.test.smoke.jms.objectmessage;

import java.io.Serializable;

import org.w3c.dom.Document;

public class Payload implements Serializable {

    private static final long serialVersionUID = 8584958274201298407L;

    private final Document document;

    public Payload(Document document) {
        this.document = document;
    }

    public Document getDocument() {
        return document;
    }
    
    @Override
    public String toString() {
        return "Payload[document=" + document + "]";
    }

}
