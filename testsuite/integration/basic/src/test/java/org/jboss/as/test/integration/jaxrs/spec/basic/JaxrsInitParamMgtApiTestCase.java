/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.spec.basic;

import java.io.IOException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.jaxrs.spec.basic.resource.JaxrsAppResource;
import org.jboss.as.test.integration.jaxrs.spec.basic.resource.JaxrsAppTwo;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Assert;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JaxrsInitParamMgtApiTestCase  extends ContainerResourceMgmtTestBase {

    @Deployment
    public static Archive<?> deploySimpleResource() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, JaxrsInitParamMgtApiTestCase.class.getSimpleName() + ".war");
        war.addAsWebInfResource(JaxrsInitParamMgtApiTestCase.class.getPackage(), "JaxrsInitParamWeb.xml", "web.xml");
        war.addClasses(JaxrsAppResource.class, JaxrsAppTwo.class);
        return war;
    }


    /**
     * When web.xml is present in the archive and the Application subclass is
     * declared only in the init-param element.  The subclass must provide a
     * list of classes to use in the getClasses or getSingletons method.
     * confirm resource class is registered in the (CLI) management model
     * Corresponding CLI cmd:
     * ./jboss-cli.sh -c --command="/deployment=JaxrsInitParamMgtApiTestCase.war/subsystem=jaxrs:read-resource(include-runtime=true,recursive=true)"
     *
     */
    @Test
    public void testInitParam() throws IOException, MgmtOperationException {
        ModelNode op =  Util.createOperation(READ_RESOURCE_OPERATION,
            PathAddress.pathAddress(DEPLOYMENT, JaxrsInitParamMgtApiTestCase.class.getSimpleName() + ".war")
                .append(SUBSYSTEM, "jaxrs")
                .append("rest-resource", JaxrsAppResource.class.getCanonicalName()));
        op.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);

        ModelNode result = executeOperation(op);
        Assert.assertFalse("Subsystem is empty.", result.keys().size() == 0);
        ModelNode resClass = result.get("resource-class");
        Assert.assertNotNull("No resource-class present.", resClass);
        Assert.assertTrue("Expected resource-class not found.",
            resClass.toString().contains(JaxrsAppResource.class.getSimpleName()));
    }
}
