/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.webservices.deployers.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.as.webservices.parser.WSDeploymentAspectParser;
import org.jboss.ws.common.sort.DeploymentAspectSorter;
import org.jboss.wsf.spi.classloading.ClassLoaderProvider;
import org.jboss.wsf.spi.deployment.DeploymentAspect;

/**
 * Provides the configured WS deployment aspects
 *
 * @author alessio.soldano@jboss.com
 */
public class DeploymentAspectsProvider {

    private static List<DeploymentAspect> aspects = null;

    public static synchronized List<DeploymentAspect> getSortedDeploymentAspects() {
        if (aspects == null) {
            final List<DeploymentAspect> deploymentAspects = new LinkedList<DeploymentAspect>();
            final ClassLoaderProvider provider = ClassLoaderProvider.getDefaultProvider();
            final ClassLoader cl = provider.getServerIntegrationClassLoader();
            deploymentAspects.addAll(getDeploymentAspects(cl, "/META-INF/stack-agnostic-deployment-aspects.xml"));
            deploymentAspects.addAll(getDeploymentAspects(cl, "/META-INF/stack-specific-deployment-aspects.xml"));
            aspects = DeploymentAspectSorter.getInstance().sort(deploymentAspects);
        }
        return aspects;
    }

    private static List<DeploymentAspect> getDeploymentAspects(final ClassLoader cl, final String resourcePath) {
        try {
            Enumeration<URL> urls = DeploymentAspectsProvider.class.getClassLoader().getResources(resourcePath);
            if (urls != null && urls.hasMoreElements()) {
                URL url = urls.nextElement();
                InputStream is = null;
                try {
                    is = url.openStream();
                    return WSDeploymentAspectParser.parse(is, cl);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
            else {
                WSLogger.ROOT_LOGGER.cannotLoadDeploymentAspectsDefinitionFile(resourcePath);
                return Collections.emptyList();
            }
        } catch (IOException e) {
            throw WSLogger.ROOT_LOGGER.cannotLoadDeploymentAspectsDefinitionFile(e, resourcePath);
        }
    }
}
