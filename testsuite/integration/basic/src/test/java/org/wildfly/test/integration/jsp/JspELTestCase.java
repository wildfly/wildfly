/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.jsp;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
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

/**
 * Tests EL evaluation in JSPs
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JspELTestCase {

    private static final String Servlet_Spec_3_1_War = "jsp-el-test-3_1_servlet_spec";
    private static final String Servlet_Spec_3_0_War = "jsp-el-test-3_0_servlet_spec";

    @Deployment(name = Servlet_Spec_3_1_War)
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(DummyConstants.class, DummyEnum.class)
                .addAsWebInfResource(JspELTestCase.class.getResource("web-app_3_1.xml"), "web.xml")
                .addAsWebResource(JspELTestCase.class.getResource("jsp-with-el.jsp"), "index.jsp");
    }

    @Deployment(name = Servlet_Spec_3_0_War)
    public static WebArchive deploy30War() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(DummyConstants.class, DummyEnum.class)
                .addAsWebInfResource(JspELTestCase.class.getResource("web-app_3_0.xml"), "web.xml")
                .addAsWebResource(JspELTestCase.class.getResource("jsp-with-el.jsp"), "index.jsp");
    }


    /**
     * Test that for web application using 3.1 version of servlet spec, EL expressions that use implicitly imported classes from <code>java.lang</code>
     * package are evaluated correctly
     *
     * @param url
     * @throws Exception
     */
    @OperateOnDeployment(Servlet_Spec_3_1_War)
    @Test
    public void testJavaLangImplicitClassELEvaluation(@ArquillianResource URL url) throws Exception {
        final String responseBody = HttpRequest.get(url + "index.jsp", 10, TimeUnit.SECONDS);
        Assert.assertTrue("Unexpected EL evaluation for ${Boolean.TRUE}", responseBody.contains("Boolean.TRUE: --- " + Boolean.TRUE + " ---"));
        Assert.assertTrue("Unexpected EL evaluation for ${Integer.MAX_VALUE}", responseBody.contains("Integer.MAX_VALUE: --- " + Integer.MAX_VALUE + " ---"));
        Assert.assertTrue("Unexpected EL evaluation for ${DummyConstants.FOO}", responseBody.contains("DummyConstants.FOO: --- " + DummyConstants.FOO + " ---"));
        Assert.assertTrue("Unexpected EL evaluation for ${DummyEnum.VALUE}", responseBody.contains("DummyEnum.VALUE: --- " + DummyEnum.VALUE + " ---"));
    }

    /**
     * Test that for web application using servlet spec version lesser than 3.1, EL expressions can't consider classes belonging to <code>java.lang</code>
     * package as implicitly imported and usable in the EL expressions
     *
     * @param url
     * @throws Exception
     */
    @OperateOnDeployment(Servlet_Spec_3_0_War)
    @Test
    public void testJavaLangImplicitClassELEvaluationForLesserSpecVersion(@ArquillianResource URL url) throws Exception {
        final String responseBody = HttpRequest.get(url + "index.jsp", 10, TimeUnit.SECONDS);
        // since this is a servlet spec version lesser than 3.1, we don't expect the Integer.MAX_VALUE and Boolean.TRUE to be correctly evaluated.
        // instead they will have empty values wherever those EL expressions occur
        Assert.assertTrue("Unexpected EL evaluation for ${Boolean.TRUE}", responseBody.contains("Boolean.TRUE: --- " + " ---"));
        Assert.assertTrue("Unexpected EL evaluation for ${Integer.MAX_VALUE}", responseBody.contains("Integer.MAX_VALUE: --- " + " ---"));
        Assert.assertTrue("Unexpected EL evaluation for ${DummyConstants.FOO}", responseBody.contains("DummyConstants.FOO: --- " + " ---"));
        Assert.assertTrue("Unexpected EL evaluation for ${DummyEnum.VALUE}", responseBody.contains("DummyEnum.VALUE: --- " + " ---"));
    }
}
