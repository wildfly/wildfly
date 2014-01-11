package org.jboss.as.test.integration.sar.injection.depends;

import javax.management.ObjectName;

public class A implements AMBean {

    ObjectName name;

    public ObjectName getObjectName() {
        return name;
    }

    public void setObjectName(ObjectName name) {
        this.name = name;
    }
}