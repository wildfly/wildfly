/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
