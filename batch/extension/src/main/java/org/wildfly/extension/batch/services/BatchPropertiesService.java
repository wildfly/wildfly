package org.wildfly.extension.batch.services;

import java.util.Properties;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchPropertiesService implements Service<Properties> {
    private Properties properties = new Properties();
    @Override
    public void start(final StartContext context) throws StartException {
    }

    @Override
    public synchronized void stop(final StopContext context) {
        properties.clear();
    }

    @Override
    public synchronized Properties getValue() throws IllegalStateException, IllegalArgumentException {
        return properties;
    }

    public synchronized Object addProperty(final String key, final String value) {
        return properties.setProperty(key, value);
    }
}
