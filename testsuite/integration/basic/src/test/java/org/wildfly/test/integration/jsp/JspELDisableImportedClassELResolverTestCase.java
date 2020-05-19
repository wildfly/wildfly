/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.jsp;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.hamcrest.CoreMatchers;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;

/**
 * Tests EL evaluation in JSPs
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
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
        Assert.assertThat("Unexpected EL evaluation for ${Boolean.TRUE}; " + possibleCausingIssues, responseBody,
                CoreMatchers.containsString("Boolean.TRUE: --- " + Boolean.TRUE + " ---"));
        Assert.assertThat("Unexpected EL evaluation for ${Integer.MAX_VALUE}; " + possibleCausingIssues, responseBody,
                CoreMatchers.containsString("Integer.MAX_VALUE: --- " + Integer.MAX_VALUE + " ---"));
        Assert.assertThat("Unexpected EL evaluation for ${DummyConstants.FOO};  " + possibleCausingIssues, responseBody,
                CoreMatchers.containsString("DummyConstants.FOO: --- " + DummyConstants.FOO + " ---"));
        Assert.assertThat("Unexpected EL evaluation for ${DummyEnum.VALUE}; " + possibleCausingIssues, responseBody,
                CoreMatchers.containsString("DummyEnum.VALUE: --- " + DummyEnum.VALUE + " ---"));
        Assert.assertThat("Unexpected EL evaluation for ${DummyBean.test}; " + possibleCausingIssues, responseBody,
                CoreMatchers.containsString("DummyBean.test: --- " + DummyBean.DEFAULT_VALUE + " ---"));
    }
}
