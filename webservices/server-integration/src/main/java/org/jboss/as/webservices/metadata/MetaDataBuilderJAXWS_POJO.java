/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.metadata;

import static org.jboss.as.webservices.util.ASHelper.getJaxwsPojos;

import java.util.List;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.webservices.metadata.model.POJOEndpoint;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class MetaDataBuilderJAXWS_POJO extends AbstractMetaDataBuilderPOJO {

    @Override
    protected List<POJOEndpoint> getPojoEndpoints(final DeploymentUnit unit) {
        return getJaxwsPojos(unit);
    }

}
