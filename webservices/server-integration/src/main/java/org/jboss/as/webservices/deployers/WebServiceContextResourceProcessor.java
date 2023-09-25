/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.deployers;

import jakarta.xml.ws.WebServiceContext;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessor;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.webservices.injection.WebServiceContextInjectionSource;

/**
 * Processes {@link jakarta.annotation.Resource @Resource} and {@link jakarta.annotation.Resources @Resources} annotations
 * for a {@link WebServiceContext} type resource
 * <p/>
 *
 * @author Jaikiran Pai
 */
public final class WebServiceContextResourceProcessor implements EEResourceReferenceProcessor {

    @Override
    public String getResourceReferenceType() {
        return WebServiceContext.class.getName();
    }

    @Override
    public InjectionSource getResourceReferenceBindingSource() throws DeploymentUnitProcessingException {
        return new WebServiceContextInjectionSource();
    }

}
