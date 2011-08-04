/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ee.deployment.spi.factories;

import java.net.URI;
import java.net.URISyntaxException;

import javax.enterprise.deploy.shared.factories.DeploymentFactoryManager;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;

import org.jboss.as.ee.deployment.spi.DeploymentManagerImpl;
import org.jboss.logging.Logger;

/**
 * The DeploymentFactory interface is a deployment driver for a J2EE plaform product.
 *
 * It returns a DeploymentManager object which represents a connection to a specific J2EE platform product. Each application
 * server vendor must provide an implementation of this class in order for the J2EE Deployment API to work with their product.
 *
 * The class implementing this interface should have a public no-argument constructor, and it should be stateless (two instances
 * of the class should always behave the same). It is suggested but not required that the class have a static initializer that
 * registers an instance of the class with the DeploymentFactoryManager class.
 *
 * A connected or disconnected DeploymentManager can be requested. A DeploymentManager that runs connected to the platform can
 * provide access to J2EE resources. A DeploymentManager that runs disconnected only provides module deployment configuration
 * support.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Scott.Stark@jboss.com
 *
 */
public class DeploymentFactoryImpl implements DeploymentFactory {
    // deployment logging
    private static final Logger log = Logger.getLogger(DeploymentFactoryImpl.class);

    // The name of the JBoss DeploymentFactory
    private static String DISPLAY_NAME;
    // The product version
    private static String PRODUCT_VERSION;

    /*
     * Obtain the display name and version from the Package object for org.jboss.deploy.spi.factories
     */
    static {
        register();
    }

    /**
     * Register a DeploymentFactoryImpl instance with the DeploymentFactoryManager. This obtains the display name and version
     * from the Package object for org.jboss.deploy.spi.factories
     *
     */
    public static synchronized void register() {
        // Register this deployment factory with the manager
        if (DISPLAY_NAME == null) {
            DeploymentFactoryManager manager = DeploymentFactoryManager.getInstance();
            manager.registerDeploymentFactory(new DeploymentFactoryImpl());
            Package pkg = DeploymentFactoryImpl.class.getPackage();
            if (pkg != null) {
                DISPLAY_NAME = pkg.getImplementationVendor();
                PRODUCT_VERSION = pkg.getImplementationVersion();
            }
            if (DISPLAY_NAME == null || PRODUCT_VERSION == null) {
                DISPLAY_NAME = "DeploymentFactoryImpl";
                PRODUCT_VERSION = "1.1-DEV";
            }
        }
    }

    /**
     * Look for jboss-deployer:.... URIs. Returns true if uri is has a scheme of jboss-deployer, false otherwise.
     *
     * @param uri the uri
     * @return true for jboss-deployer schemes, false otherwise.
     */
    public boolean handlesURI(String uri) {
        boolean handlesURI = DeploymentManagerImpl.DEPLOYER_URI.equals(uri);
        if (handlesURI == false) {
            try {
                URI deployURI = parseURI(uri);
                handlesURI = "jnp".equals(deployURI.getScheme());
            } catch (URISyntaxException e) {
                log.warn("Failed to parse uri: " + uri, e);
            }
        }

        log.debug("handlesURI [" + uri + "]: " + handlesURI);
        return handlesURI;
    }

    /**
     * Get a connected deployment manager
     *
     * @param uri the uri of the deployment manager
     * @param userName the user name
     * @param password the password
     * @return the deployment manager
     * @throws javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException
     *
     */
    public DeploymentManager getDeploymentManager(String uri, String userName, String password) throws DeploymentManagerCreationException {
        log.debug("getDeploymentManager (uri=" + uri + ")");
        DeploymentManager mgr = null;

        try {
            URI deployURI = parseURI(uri);
            mgr = new DeploymentManagerImpl(deployURI, true, userName, password);
        } catch (URISyntaxException e) {
            DeploymentManagerCreationException ex = new DeploymentManagerCreationException("Failed to create DeploymentManagerImpl");
            ex.initCause(e);
            throw ex;
        }
        return mgr;
    }

    /**
     * Get a disconnected version of the deployment manager
     *
     * @param uri the uri to connect to
     * @return the disconnected deployment manager
     * @throws javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException
     *
     */
    public DeploymentManager getDisconnectedDeploymentManager(String uri) throws DeploymentManagerCreationException {
        log.debug("getDisconnectedDeploymentManager (uri=" + uri + ")");
        DeploymentManager mgr = null;

        try {
            URI deployURI = parseURI(uri);
            mgr = new DeploymentManagerImpl(deployURI, false);
        } catch (URISyntaxException e) {
            DeploymentManagerCreationException ex = new DeploymentManagerCreationException("Failed to create DeploymentManagerImpl");
            ex.initCause(e);
            throw ex;
        }
        return mgr;
    }

    /**
     * The name of the JBoss DeploymentFactory.
     *
     * @return the vendor name
     */
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    /**
     * The version of the deployment manager
     *
     * @return the version
     */
    public String getProductVersion() {
        return PRODUCT_VERSION;
    }

    private URI parseURI(String uri) throws URISyntaxException {
        URI deployURI = new URI(uri);
        return deployURI;
    }
}
