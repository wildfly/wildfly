package org.jboss.as.test.integration.sar.injection.depends;

import java.util.List;

import javax.management.ObjectName;

public interface BMBean {
        public List<ObjectName> getObjectNames();
        public void setObjectNames(List<ObjectName> names);
}
