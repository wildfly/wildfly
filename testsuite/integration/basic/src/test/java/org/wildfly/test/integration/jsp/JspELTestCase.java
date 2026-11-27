/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.jsp;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Tests EL evaluation in JSPs
 *
 * @author Jaikiran Pai
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class JspELTestCase {

    private static final String Servlet_No_Spec_War = "jsp-el-test-no-web-xml";
    private static final String Servlet_Spec_4_0_War = "jsp-el-test-4_0_servlet_spec";
    private static final String Servlet_Spec_3_1_War = "jsp-el-test-3_1_servlet_spec";
    private static final String Servlet_Spec_3_0_War = "jsp-el-test-3_0_servlet_spec";

    @Deployment(name = Servlet_No_Spec_War)
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(DummyConstants.class, DummyEnum.class)
                .addAsWebResource(JspELTestCase.class.getResource("jsp-with-el.jsp"), "index.jsp");
    }

    @Deployment(name = Servlet_Spec_4_0_War)
    public static WebArchive deploy40War() {
        return deploy().addAsWebInfResource(JspELTestCase.class.getResource("web-app_4_0.xml"), "web.xml");
    }

    @Deployment(name = Servlet_Spec_3_1_War)
    public static WebArchive deploy31War() {
        return deploy().addAsWebInfResource(JspELTestCase.class.getResource("web-app_3_1.xml"), "web.xml");
    }

    @Deployment(name = Servlet_Spec_3_0_War)
    public static WebArchive deploy30War() {
        return deploy().addAsWebInfResource(JspELTestCase.class.getResource("web-app_3_0.xml"), "web.xml");
    }

    final String POSSIBLE_ISSUES_LINKS = "Might be caused by: https://issues.jboss.org/browse/WFLY-6939 or" +
            " https://issues.jboss.org/browse/WFLY-11065 or https://issues.jboss.org/browse/WFLY-11086";

    /**
     * Test that for web application using default version of servlet spec, EL expressions that use implicitly imported
     * classes from <code>java.lang</code> package are evaluated correctly
     *
     * @param url
     * @throws Exception
     */
    @Test
    public void testJavaLangImplicitClassELEvaluation(@ArquillianResource @OperateOnDeployment(Servlet_No_Spec_War) URL url) throws Exception {
        commonTestPart(url, POSSIBLE_ISSUES_LINKS);
        commonTestPart(url, POSSIBLE_ISSUES_LINKS);
    }

    /**
     * Test that for web application using 4.0 version of servlet spec, EL expressions that use implicitly imported
     * classes from <code>java.lang</code> package are evaluated correctly
     *
     * @param url
     * @throws Exception
     */
    @Test
    public void testJavaLangImplicitClassELEvaluation40(@ArquillianResource @OperateOnDeployment(Servlet_Spec_4_0_War) URL url) throws Exception {
        commonTestPart(url, POSSIBLE_ISSUES_LINKS);
        commonTestPart(url, POSSIBLE_ISSUES_LINKS);
    }

    /**
     * Test that for web application using 3.1 version of servlet spec, EL expressions that use implicitly imported
     * classes from <code>java.lang</code> package are evaluated correctly
     *
     * @param url
     * @throws Exception
     */
    @Test
    public void testJavaLangImplicitClassELEvaluation31(@ArquillianResource @OperateOnDeployment(Servlet_Spec_3_1_War) URL url) throws Exception {
        commonTestPart(url, POSSIBLE_ISSUES_LINKS);
        commonTestPart(url, POSSIBLE_ISSUES_LINKS);
    }

    private void commonTestPart(final URL url, final String possibleCausingIssues) throws Exception {
        final String responseBody = HttpRequest.get(url + "index.jsp", 10, TimeUnit.SECONDS);
        Assertions.assertTrue(responseBody.contains("Boolean.TRUE: --- " + Boolean.TRUE + " ---"),
                "Unexpected EL evaluation for ${Boolean.TRUE}; " + possibleCausingIssues);
        Assertions.assertTrue(responseBody.contains("Integer.MAX_VALUE: --- " + Integer.MAX_VALUE + " ---"),
                "Unexpected EL evaluation for ${Integer.MAX_VALUE}; " + possibleCausingIssues);
        Assertions.assertTrue(responseBody.contains("DummyConstants.FOO: --- " + DummyConstants.FOO + " ---"),
                "Unexpected EL evaluation for ${DummyConstants.FOO}; " + possibleCausingIssues);
        Assertions.assertTrue(responseBody.contains("DummyEnum.VALUE: --- " + DummyEnum.VALUE + " ---"),
                "Unexpected EL evaluation for ${DummyEnum.VALUE}; " + possibleCausingIssues);
    }

    /**
     * Test that for web application using servlet spec version lesser than 3.1, EL expressions can't consider classes
     * belonging to <code>java.lang</code> package as implicitly imported and usable in the EL expressions
     *
     * @param url
     * @throws Exception
     */
    @Test
    public void testJavaLangImplicitClassELEvaluationForLesserSpecVersion(@ArquillianResource @OperateOnDeployment(Servlet_Spec_3_0_War) URL url) throws Exception {
        // with the Jakarta upgrade for jsp spec (WFLY-12439), we can now evaluate Boolean.TRUE and Integer.MAX_VALUE
        // even when using the previous spec version
        commonTestPart(url, POSSIBLE_ISSUES_LINKS);
        commonTestPart(url, POSSIBLE_ISSUES_LINKS);
    }
}
