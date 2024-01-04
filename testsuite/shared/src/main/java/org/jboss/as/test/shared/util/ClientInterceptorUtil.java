/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.util;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.test.shared.integration.ejb.security.Util;

/**
 * Util class which is used for remote lookups in client-side interceptor related tests.
 *
 * @author <a href="mailto:szhantem@redhat.com">Sultan Zhantemirov</a> (c) 2019 Red Hat, inc.
 */
public class ClientInterceptorUtil {

    public ClientInterceptorUtil() {
        // empty
    }

    public static <T> T lookupStatelessRemote(String archiveName, Class<? extends T> beanType, Class<T> remoteInterface) throws NamingException {
        final Hashtable<String, String> props = new Hashtable<>();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context ctx = new InitialContext(props);

        String ejbLookup = Util.createRemoteEjbJndiContext(
                "", archiveName, "", beanType.getSimpleName(), remoteInterface.getName(), false);
        return remoteInterface.cast(ctx.lookup(ejbLookup));

    }
}
