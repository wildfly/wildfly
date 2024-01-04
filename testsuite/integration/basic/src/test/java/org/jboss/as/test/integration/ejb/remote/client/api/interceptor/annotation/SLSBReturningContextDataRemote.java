/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api.interceptor.annotation;

import jakarta.ejb.Remote;

import org.jboss.ejb.client.annotation.ClientInterceptors;

/**
 * @author Jan Martiska
 */
@Remote
@ClientInterceptors({EJBClientInterceptorAddedByAnnotation.class})
public interface SLSBReturningContextDataRemote {

    /**
     * Returns the value from server-visible context data corresponding to the requested key.
     */
    Object getContextData(String key);

}
