/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.jsp;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.concurrent.TimeUnit;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;

/**
 * Tests EL evaluation in JSPs
 *
 * @author rmartinc
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup({JspELDisableImportedClassELResolverTestCase.SystemPropertiesSetup.class})
@RunAsClient
public class JspELDisableImportedClassELResolverTestCase {

    /**
     * Setup the system property to disable the disableImportedClassELResolver
     * and act exactly as the specification says delegating in .
     */
    static class SystemPropertiesSetup extends AbstractSystemPropertiesServerSetupTask {

        @Override
        protected SystemProperty[] getSystemProperties() {
            return new SystemProperty[] {
                new DefaultSystemProperty("org.wildfly.extension.undertow.deployment.disableImportedClassELResolver", "true")
            };
        }
    }

    static final String POSSIBLE_ISSUES_LINKS = "Might be caused by: https://issues.jboss.org/browse/WFLY-6939 or" +
            " https://issues.jboss.org/browse/WFLY-11065 or https://issues.jboss.org/browse/WFLY-11086 or" +
            " https://issues.jboss.org/browse/WFLY-12650";

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(DummyConstants.class, DummyEnum.class, DummyBean.class)
                .addAsWebResource(JspELDisableImportedClassELResolverTestCase.class.getResource("jsp-with-el-static-class.jsp"), "index.jsp");
    }

    /**
     * Test that for web application using default version of servlet spec, EL expressions that use implicitly imported
     * classes from <code>java.lang</code> package are evaluated correctly, and the bean is resolved OK as per specification.
     *
     * @param url
     * @throws Exception
     */
    @Test
    public void testStaticImportSameName(@ArquillianResource URL url) throws Exception {
        commonTestPart(url, POSSIBLE_ISSUES_LINKS);
        commonTestPart(url, POSSIBLE_ISSUES_LINKS);
    }

    private void commonTestPart(final URL url, final String possibleCausingIssues) throws Exception {
        final String responseBody = HttpRequest.get(url + "index.jsp", 10, TimeUnit.SECONDS);
        assertThat(responseBody).as("Unexpected EL evaluation for ${Boolean.TRUE}; " + possibleCausingIssues).contains("Boolean.TRUE: --- " + Boolean.TRUE + " ---");
        assertThat(responseBody).as("Unexpected EL evaluation for ${Integer.MAX_VALUE}; " + possibleCausingIssues).contains("Integer.MAX_VALUE: --- " + Integer.MAX_VALUE + " ---");
        assertThat(responseBody).as("Unexpected EL evaluation for ${DummyConstants.FOO};  " + possibleCausingIssues).contains("DummyConstants.FOO: --- " + DummyConstants.FOO + " ---");
        assertThat(responseBody).as("Unexpected EL evaluation for ${DummyEnum.VALUE}; " + possibleCausingIssues).contains("DummyEnum.VALUE: --- " + DummyEnum.VALUE + " ---");
        assertThat(responseBody).as("Unexpected EL evaluation for ${DummyBean.test}; " + possibleCausingIssues).contains("DummyBean.test: --- " + DummyBean.DEFAULT_VALUE + " ---");
    }
}
