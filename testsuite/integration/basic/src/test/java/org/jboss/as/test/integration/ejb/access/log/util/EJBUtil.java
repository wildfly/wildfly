/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.access.log.util;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;

/**
 * Utility class for looking up EJBs.
 *
 * @author Tommaso Borgato
 */
public class EJBUtil {

    /**
     * Lookup for remote EJBs.
     */
    @SuppressWarnings("unchecked")
    public static <T> T lookupEJB(Class<? extends T> beanImplClass, Class<T> remoteInterface, Properties ejbProperties, String APP_NAME, String MODULE_NAME, boolean stateful) throws Exception {
        final Properties jndiProperties = new Properties();
        jndiProperties.putAll(ejbProperties);
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context context = new InitialContext(jndiProperties);
        return (T) context.lookup(
                "ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + beanImplClass.getSimpleName() + "!" + remoteInterface.getName() + (stateful ? "?stateful" : "")
        );
    }

    /**
     * Creates {@link Properties} for the EJB client configuration.
     */
    public static Properties createEjbClientConfiguration(String hostName, String username, String password) {
        final Properties pr = new Properties();
        pr.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
        pr.put("remote.connection.default.connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");
        pr.put("remote.connections", "default");
        pr.put("remote.connection.default.host", hostName);
        pr.put("remote.connection.default.port", "8080");
        if (username != null && password != null) {
            pr.put("remote.connection.default.username", username);
            pr.put("remote.connection.default.password", password);
        }
        return pr;
    }
}
