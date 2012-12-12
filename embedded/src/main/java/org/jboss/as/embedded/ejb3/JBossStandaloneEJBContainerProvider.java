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

import static java.security.AccessController.doPrivileged;
import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.client.helpers.ClientConstants.EXTENSION;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.embedded.EmbeddedMessages.MESSAGES;

import java.io.File;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.ejb.EJBException;
import javax.ejb.embeddable.EJBContainer;
import javax.ejb.spi.EJBContainerProvider;

import org.jboss.as.embedded.EmbeddedServerFactory;
import org.jboss.as.embedded.StandaloneServer;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class JBossStandaloneEJBContainerProvider implements EJBContainerProvider {

    private void addEmbeddedExtensionTo(final StandaloneServer server) throws IOException {
        // FIXME: doesn't work, because org.jboss.as.embedded lives on the wrong side of the CL
        final ModelNode address = new ModelNode().setEmptyList();
        final ModelNode add = new ModelNode();
        add.get(OP_ADDR).set(address).add(EXTENSION, "org.jboss.as.embedded");
        add.get(OP).set(ADD);
        final ModelNode result = server.getModelControllerClient().execute(add);
        if (!result.get(OUTCOME).equals(SUCCESS)) {
            throw new EJBException(result.asString());
        }
    }

    @Override
    public EJBContainer createEJBContainer(Map<?, ?> properties) throws EJBException {
        //setSystemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        // see EjbDependencyDeploymentUnitProcessor
        setSystemProperty("org.jboss.as.ejb3.EMBEDDED", "true");

        String jbossHomeKey = "jboss.home";
        String jbossHomeProp = System.getProperty(jbossHomeKey);
        if (jbossHomeProp == null)
            throw MESSAGES.systemPropertyNotFound(jbossHomeKey);

        File jbossHomeDir = new File(jbossHomeProp);
        if (jbossHomeDir.isDirectory() == false)
            throw MESSAGES.invalidJBossHome(jbossHomeProp);

        // Per default we assume that we're running in a modular environment.
        // To allow setting up the modular environment ourselves, set org.jboss.as.embedded.ejb3.BARREN to true.
        // We can't use Module itself, because that partially initializes the environment.
        final boolean barren = Boolean.getBoolean("org.jboss.as.embedded.ejb3.BARREN");
        final StandaloneServer server;
        if (barren)
            server = EmbeddedServerFactory.create(jbossHomeProp, null, null, "org.jboss.logmanager");
        else
            server = EmbeddedServerFactory.create(Module.getContextModuleLoader(), jbossHomeDir);
        try {
            server.start();
//            addEmbeddedExtensionTo(server);
            final JBossStandaloneEJBContainer container = new JBossStandaloneEJBContainer(server);
            boolean okay = false;
            try {
                container.init(properties);
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
