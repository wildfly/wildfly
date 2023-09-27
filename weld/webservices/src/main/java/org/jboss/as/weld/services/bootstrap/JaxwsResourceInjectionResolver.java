/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.services.bootstrap;

import jakarta.xml.ws.WebServiceContext;

import org.jboss.as.weld.spi.ResourceInjectionResolver;
import org.jboss.ws.common.injection.ThreadLocalAwareWebServiceContext;

/**
 *
 * @author Martin Kouba
 */
public class JaxwsResourceInjectionResolver implements ResourceInjectionResolver {

    @Override
    public Object resolve(String resourceName) {
        if (resourceName.equals(WebServiceContext.class.getName())) {
            // horrible hack
            // we don't have anywhere we can look this up
            // See also WFLY-4487
            return ThreadLocalAwareWebServiceContext.getInstance();
        }
        return null;
    }

}
