/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A ResourceAdaptersService.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 */
final class XaDataSourcePropertiesService implements Service<String> {


    private final String value;
    private final String name;


    /**
     * create an instance *
     */
    public XaDataSourcePropertiesService(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getValue() throws IllegalStateException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {

    }

    public String getName() {
        return name;
    }

}
