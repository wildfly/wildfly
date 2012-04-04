package org.jboss.as.ejb3.subsystem;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Stuart Douglas
 */
public class DefaultDistinctNameService implements Service<DefaultDistinctNameService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "defaultDistinctName");

    private volatile String defaultDistinctName;

    public DefaultDistinctNameService(final String defaultDistinctName) {
        this.defaultDistinctName = defaultDistinctName;
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {

    }

    @Override
    public DefaultDistinctNameService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public String getDefaultDistinctName() {
        return defaultDistinctName;
    }

    public void setDefaultDistinctName(final String defaultDistinctName) {
        this.defaultDistinctName = defaultDistinctName;
    }
}
