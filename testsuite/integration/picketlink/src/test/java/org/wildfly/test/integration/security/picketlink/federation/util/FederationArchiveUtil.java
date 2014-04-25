/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.integration.security.picketlink.federation.util;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author Pedro Igor
 */
public class FederationArchiveUtil {

    public static WebArchive identityProvider(String deploymentName) {
        return identityProvider(deploymentName, null, null);
    }

    public static WebArchive identityProvider(String deploymentName, String indexContent, String hostedIndexContent) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName);

        war.addAsManifestResource(new StringAsset("Dependencies: org.picketlink meta-inf,org.jboss.dmr meta-inf,org.jboss.as.controller\n"), "MANIFEST.MF");
        war.addAsWebInfResource(FederationArchiveUtil.class.getPackage(), "web.xml", "web.xml");
        war.addAsWebResource(FederationArchiveUtil.class.getPackage(), "login.jsp", "login.jsp");
        war.addAsWebResource(FederationArchiveUtil.class.getPackage(), "login-error.jsp", "login-error.jsp");
        war.add(new StringAsset(indexContent != null ? indexContent : "Welcome to IdP"), "index.jsp");
        war.add(new StringAsset(hostedIndexContent != null ? hostedIndexContent : "Welcome to IdP hosted"), "hosted/index.jsp");

        return war;
    }

    public static WebArchive identityProviderWithKeyStore(String deploymentName) {
        WebArchive war = identityProvider(deploymentName);

        war.addAsResource(FederationArchiveUtil.class.getPackage(), "jbid_test_keystore.jks", "jbid_test_keystore.jks");

        return war;
    }

    public static WebArchive serviceProvider(String deploymentName) {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName);

        war.addAsManifestResource(new StringAsset("Dependencies: org.picketlink meta-inf,org.jboss.dmr meta-inf,org.jboss.as.controller\n"), "MANIFEST.MF");
        war.addAsWebInfResource(FederationArchiveUtil.class.getPackage(), "web.xml", "web.xml");
        war.add(new StringAsset("Welcome to " + deploymentName), "index.jsp");
        war.add(new StringAsset("Logout in progress"), "logout.jsp");

        return war;
    }

    public static WebArchive serviceProviderWithKeyStore(String deploymentName) {
        WebArchive war = serviceProvider(deploymentName);

        war.addAsResource(FederationArchiveUtil.class.getPackage(), "jbid_test_keystore.jks", "jbid_test_keystore.jks");

        return war;
    }

}
