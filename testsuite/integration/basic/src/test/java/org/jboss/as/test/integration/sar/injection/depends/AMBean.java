package org.jboss.as.test.integration.sar.injection.depends;

import javax.management.ObjectName;

public interface AMBean {
        public ObjectName getObjectName();
        public void setObjectName(ObjectName text);
}
