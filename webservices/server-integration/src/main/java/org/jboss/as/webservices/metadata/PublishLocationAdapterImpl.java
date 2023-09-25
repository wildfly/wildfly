/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.metadata;

import org.jboss.wsf.spi.metadata.j2ee.PublishLocationAdapter;
import org.jboss.wsf.spi.metadata.webservices.JBossWebserviceDescriptionMetaData;

/**
 * Publish location adapter implementation.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
final class PublishLocationAdapterImpl implements PublishLocationAdapter {

    private final JBossWebserviceDescriptionMetaData[] wsDescriptionsMD;

    PublishLocationAdapterImpl(final JBossWebserviceDescriptionMetaData[] wsDescriptionsMD) {
        this.wsDescriptionsMD = wsDescriptionsMD;
    }

    public String getWsdlPublishLocationByName(final String endpointName) {
        if (wsDescriptionsMD != null) {
            for (final JBossWebserviceDescriptionMetaData wsDescriptionMD : wsDescriptionsMD) {
                if (endpointName.equals(wsDescriptionMD.getWebserviceDescriptionName()))
                    return wsDescriptionMD.getWsdlPublishLocation();
            }
        }

        return null;
    }

}
