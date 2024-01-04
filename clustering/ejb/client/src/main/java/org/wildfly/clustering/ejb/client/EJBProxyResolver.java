/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.client;

import org.jboss.ejb.client.EJBClient;
import org.jboss.marshalling.ObjectResolver;

/**
 * Resolver for EJB proxies.
 * @author Paul Ferraro
 */
public class EJBProxyResolver implements ObjectResolver {

    @Override
    public Object readResolve(Object replacement) {
        return replacement;
    }

    @Override
    public Object writeReplace(Object object) {
        return EJBClient.isEJBProxy(object) ? new SerializableEJBProxy(object) : object;
    }
}
