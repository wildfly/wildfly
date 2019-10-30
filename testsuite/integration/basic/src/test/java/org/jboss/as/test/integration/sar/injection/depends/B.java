package org.jboss.as.test.integration.sar.injection.depends;

import java.util.List;

import javax.management.ObjectName;

public class B implements BMBean {

    private List<ObjectName> names;

    @Override
    public List<ObjectName> getObjectNames() {
        return names;
    }

    @Override
    public void setObjectNames(List<ObjectName> names) {
        this.names = names;
    }
}