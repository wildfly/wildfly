/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.webservices.metadata;

import org.jboss.metadata.common.jboss.WebserviceDescriptionMetaData;
import org.jboss.metadata.common.jboss.WebserviceDescriptionsMetaData;
import org.jboss.wsf.spi.metadata.j2ee.PublishLocationAdapter;

/**
 * Publish location adapter implementation.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
final class PublishLocationAdapterImpl implements PublishLocationAdapter {
    /** Webservice descriptions meta data. */
    private final WebserviceDescriptionsMetaData wsDescriptionsMD;

    /**
     * Constructor.
     *
     * @param wsDescriptionsMD webservice descriptions meta data
     */
    PublishLocationAdapterImpl(final WebserviceDescriptionsMetaData wsDescriptionsMD) {
        super();

        this.wsDescriptionsMD = wsDescriptionsMD;
    }

    /**
     * @see org.jboss.wsf.spi.metadata.j2ee.PublishLocationAdapter#getWsdlPublishLocationByName(String)
     *
     * @param endpointName endpoint name
     * @return publish location
     */
    public String getWsdlPublishLocationByName(final String endpointName) {
        if (this.wsDescriptionsMD != null) {
            final WebserviceDescriptionMetaData endpointMD = this.wsDescriptionsMD.get(endpointName);

            if (endpointMD != null) {
                return endpointMD.getWsdlPublishLocation();
            }
        }

        return null;
    }
}
