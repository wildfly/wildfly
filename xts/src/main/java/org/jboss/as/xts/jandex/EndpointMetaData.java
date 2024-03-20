/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts.jandex;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.xts.XTSException;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 * @author <a href="mailto:paul.robinson@redhat.com">Paul Robinson</a>
 */
public class EndpointMetaData {

    private final TransactionalAnnotation transactionalAnnotation;
    private final StatelessAnnotation statelessAnnotation;
    private final WebServiceAnnotation webServiceAnnotation;

    private EndpointMetaData(final StatelessAnnotation statelessAnnotation,
            final TransactionalAnnotation transactionalAnnotation,
            final WebServiceAnnotation webServiceAnnotation) {

        this.statelessAnnotation = statelessAnnotation;
        this.transactionalAnnotation = transactionalAnnotation;
        this.webServiceAnnotation = webServiceAnnotation;
    }

    public static EndpointMetaData build(final DeploymentUnit unit, final String endpoint) throws XTSException {
        final TransactionalAnnotation transactionalAnnotation = TransactionalAnnotation.build(unit, endpoint);
        final StatelessAnnotation statelessAnnotation = StatelessAnnotation.build(unit, endpoint);
        final WebServiceAnnotation webServiceAnnotation = WebServiceAnnotation.build(unit, endpoint);

        return new EndpointMetaData(statelessAnnotation, transactionalAnnotation, webServiceAnnotation);
    }

    public WebServiceAnnotation getWebServiceAnnotation() {
        return webServiceAnnotation;
    }

    public boolean isWebservice() {
        return webServiceAnnotation != null;
    }

    public boolean isEJB() {
        return statelessAnnotation != null;
    }

    public boolean isBridgeEnabled() {
        return transactionalAnnotation != null;
    }

    public boolean isXTSEnabled() {
        return transactionalAnnotation != null;
    }
}
