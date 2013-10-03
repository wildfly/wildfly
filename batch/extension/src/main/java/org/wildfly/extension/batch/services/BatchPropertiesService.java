package org.wildfly.extension.batch.services;

import java.util.Properties;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A service for adding default properties for the {@link javax.batch.operations.JobOperator JobOperator}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchPropertiesService implements Service<Properties> {
    private final Properties properties = new Properties();

    @Override
    public void start(final StartContext context) throws StartException {
    }

    @Override
    public void stop(final StopContext context) {
        properties.clear();
    }

    @Override
    public Properties getValue() throws IllegalStateException, IllegalArgumentException {
        return properties;
    }

    /**
     * Adds a property to the global properties for the {@link javax.batch.operations.JobOperator JobOperator}.
     *
     * @param key   the key of the property
     * @param value the value of the property
     *
     * @return the previous value of the property or {@code null} if there was not one
     */
    public Object addProperty(final String key, final String value) {
        return properties.setProperty(key, value);
    }
}
