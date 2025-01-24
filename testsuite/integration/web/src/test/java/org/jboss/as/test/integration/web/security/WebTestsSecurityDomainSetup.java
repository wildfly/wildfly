/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.logging.Logger;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.PropertyFileBasedDomain;
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;

/**
 * @author Stuart Douglas
 */
public class WebTestsSecurityDomainSetup extends AbstractSecurityDomainSetup {

    private static final Logger log = Logger.getLogger(WebTestsSecurityDomainSetup.class);

    public static final String WEB_SECURITY_DOMAIN = "web-tests";

    private static final String GOOD_USER_NAME = "anil";
    private static final String GOOD_USER_PASSWORD = "anil";
    private static final String GOOD_USER_ROLE = "gooduser";

    private static final String SUPER_USER_NAME = "marcus";
    private static final String SUPER_USER_PASSWORD = "marcus";
    private static final String SUPER_USER_ROLE = "superuser";

    private static final String BAD_GUY_NAME = "peter";
    private static final String BAD_GUY_PASSWORD = "peter";
    private static final String BAD_GUY_ROLE = "badguy";

    private ModelControllerClient modelControllerClient;
    private CLIWrapper cli;

    private List<ConfigurableElement> configurableElements = null;


    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        // Retrieve application.keystore file from jar archive of wildfly-testsuite-shared module
        File destFile = new File(System.getProperty("jboss.home") + File.separator + "standalone" + File.separator +
                "configuration" + File.separatorChar + "application.keystore");
        URL resourceUrl = this.getClass().getResource("/org/jboss/as/test/shared/shared-keystores/application.keystore");
        FileUtils.copyInputStreamToFile(resourceUrl.openStream(), destFile);

        modelControllerClient = managementClient.getControllerClient();
        cli = new CLIWrapper(true);
        setElytronBased();
    }

    @Override
    protected String getSecurityDomainName() {
        return WEB_SECURITY_DOMAIN;
    }

    protected List<ConfigurableElement> getAdditionalElements() {
        return Collections.emptyList();
    }

    protected PropertyFileBasedDomain.Builder withUsers(final PropertyFileBasedDomain.Builder builder) {
        return builder
            .withUser(GOOD_USER_NAME, GOOD_USER_PASSWORD, GOOD_USER_ROLE)
            .withUser(SUPER_USER_NAME, SUPER_USER_PASSWORD, SUPER_USER_ROLE)
            .withUser(BAD_GUY_NAME, BAD_GUY_PASSWORD, BAD_GUY_ROLE);
    }

    protected ConfigurableElement getApplicationSecurityDomainMapping() {
        return UndertowDomainMapper.builder().withName(WEB_SECURITY_DOMAIN).build();
    }

    protected void setElytronBased() throws Exception {
        log.debug("start of the elytron based domain creation");
        List<ConfigurableElement> additionalElements = getAdditionalElements();
        List<ConfigurableElement> elements = new ArrayList<>(additionalElements.size() + 2);
        elements.addAll(additionalElements);

        // Prepare properties files with users, passwords and roles
        elements.add(withUsers(PropertyFileBasedDomain.builder())
                .withName(WEB_SECURITY_DOMAIN).build());
        elements.add(getApplicationSecurityDomainMapping());

        for (ConfigurableElement current : elements) {
            current.create(modelControllerClient, cli);
        }
        configurableElements = elements;
        log.debug("end of the elytron based domain creation");
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) {
        try {
            if (configurableElements != null) {
                // Remove in reverse order as we are not in a batch.
                for (int i = configurableElements.size() - 1; i >= 0; i--) {
                    configurableElements.get(i).remove(modelControllerClient, cli);
                }
            }
            configurableElements = null;
            cli.close();
            cli = null;
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        } catch (Exception e) {
            throw new RuntimeException("Cleaning up for Elytron based security domain failed.", e);
        }
    }
}
