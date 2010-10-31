package org.jboss.as.domain.controller;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.model.DomainModel;

/**
 * Handles persistence of the {@link DomainModel}.
 *
 * @author Brian Stansberry
 */
public interface DomainConfigurationPersister {

    /**
     * Gets an input stream from which the persisted form of the domain configuration
     * can be read.
     *
     * @return the input stream. Will not be <code>null</code>
     * @throws IOException if a problem accessing the persisted form occurs
     */
    InputStream getConfigurationInputStream() throws IOException;

    /**
     * Persist the given domain model.
     *
     * @param domainModel the model. Cannot be <code>null</code>
     */
    void persistConfiguration(DomainModel domainModel);

}

