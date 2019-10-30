/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.integration.elytron.realmmappers;

import java.net.MalformedURLException;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.wildfly.test.integration.elytron.realmmappers.RealmMapperServerSetupTask.SECURITY_DOMAIN_REFERENCE;
import org.wildfly.test.security.servlets.SecuredPrincipalPrintingServlet;

/**
 *
 * @author olukas
 */
public abstract class AbstractRealmMapperTest {

    protected static final String DEPLOYMENT = "dep";

    @Deployment(name = DEPLOYMENT)
    public static WebArchive createWar() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war").addClasses(SecuredPrincipalPrintingServlet.class)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(SECURITY_DOMAIN_REFERENCE), "jboss-web.xml")
                .addAsWebInfResource(new StringAsset(SecurityTestConstants.WEB_XML_BASIC_AUTHN), "web.xml");
    }

    protected void setupRealmMapper(String realmMapperName) throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:write-attribute(name=realm-mapper,value=%s)",
                    RealmMapperServerSetupTask.SECURITY_DOMAIN_NAME, realmMapperName));
            cli.sendLine(String.format("reload"));
        }
    }

    protected void undefineRealmMapper() throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:undefine-attribute(name=realm-mapper)",
                    RealmMapperServerSetupTask.SECURITY_DOMAIN_NAME));
            cli.sendLine(String.format("reload"));
        }
    }

    protected URL principalServlet(URL url) throws MalformedURLException {
        return new URL(url.toExternalForm() + SecuredPrincipalPrintingServlet.SERVLET_PATH.substring(1));
    }
}
