package org.jboss.as.server.manager;

import java.io.IOException;
import java.io.Reader;

import org.jboss.as.model.HostModel;

/**
 * Handles persistence of the {@link HostModel}.
 *
 * @author Brian Stansberry
 */
public interface HostConfigurationPersister {

    /**
     * Gets a reader from which the persisted form of the domain configuration
     * can be read.
     *
     * @return the reader. Will not be <code>null</code>
     * @throws IOException if a problem accessing the persisted form occurs
     */
    Reader getConfigurationReader() throws IOException;

    /**
     * Persist the given host model.
     *
     * @param hostModel the model. Cannot be <code>null</code>
     */
    void persistConfiguration(HostModel hostModel);

}

