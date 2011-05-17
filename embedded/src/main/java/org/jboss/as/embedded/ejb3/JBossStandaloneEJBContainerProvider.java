/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.embedded.ejb3;

import org.jboss.as.embedded.EmbeddedServerFactory;
import org.jboss.as.embedded.StandaloneServer;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

import javax.ejb.EJBException;
import javax.ejb.embeddable.EJBContainer;
import javax.ejb.spi.EJBContainerProvider;
import java.io.File;
import java.security.PrivilegedAction;
import java.util.Map;

import static java.security.AccessController.doPrivileged;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class JBossStandaloneEJBContainerProvider implements EJBContainerProvider {
    @Override
    public EJBContainer createEJBContainer(Map<?, ?> properties) throws EJBException {
        //setSystemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        String jbossHomeKey = "jboss.home";
        String jbossHomeProp = System.getProperty(jbossHomeKey);
        if (jbossHomeProp == null)
            throw new EJBException("Cannot find system property: " + jbossHomeKey);

        File jbossHomeDir = new File(jbossHomeProp);
        if (jbossHomeDir.isDirectory() == false)
            throw new EJBException("Invalid jboss home directory: " + jbossHomeDir);

        final ModuleLoader moduleLoader = Module.getContextModuleLoader();
        final StandaloneServer server;
        if (moduleLoader == null)
            server = EmbeddedServerFactory.create(jbossHomeDir, System.getProperties(), System.getenv(), "org.jboss.logmanager");
        else
            server = EmbeddedServerFactory.create(moduleLoader, jbossHomeDir, System.getProperties(), System.getenv());
        try {
            server.start();
            final JBossStandaloneEJBContainer container = new JBossStandaloneEJBContainer(server);
            boolean okay = false;
            try {
                container.init();
                okay = true;
                return container;
            }
            finally {
                if (!okay)
                    container.close();
            }
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception e) {
            throw new EJBException(e);
        }
    }

    private static String setSystemProperty(final String key, final String value) {
        if (System.getSecurityManager() == null)
            return System.setProperty(key, value);
        return doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.setProperty(key, value);
            }
        });
    }
}
