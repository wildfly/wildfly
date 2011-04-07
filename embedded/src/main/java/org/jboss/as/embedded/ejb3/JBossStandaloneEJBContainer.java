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

import org.jboss.as.embedded.StandaloneServer;
import org.jboss.logging.Logger;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.Context;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class JBossStandaloneEJBContainer extends EJBContainer {
    private static final Logger log = Logger.getLogger(JBossStandaloneEJBContainer.class);

    private final StandaloneServer server;
    private final List<File> deployments = new LinkedList<File>();

    JBossStandaloneEJBContainer(final StandaloneServer server) {
        this.server = server;
    }

    @Override
    public void close() {
        for (File deployment : deployments) {
            try {
                server.undeploy(deployment);
            }
            catch (Exception e) {
                log.warnf(e, "Failed to undeploy %s", deployment);
            }
        }
        server.stop();
    }

    void deploy(File deployment) throws IOException, ExecutionException, InterruptedException {
        server.deploy(deployment);
        deployments.add(deployment);
    }

    /**
     * Search the JVM classpath to find EJB modules and deploy them.
     */
    void init() throws IOException, ExecutionException, InterruptedException {
        // TODO: the ClassPathEjbJarScanner is not the optimal way to find EJBs, see TODOs in there.
        // ClassPathEjbJarScanner uses TCCL
        final String[] candidates = ClassPathEjbJarScanner.getEjbJars();
        // TODO: use a DeploymentPlan
        for (int i = 0; i < candidates.length; i++) {
            deploy(new File(candidates[i]));
        }
    }

    @Override
    public Context getContext() {
        return server.getContext();
    }
}
