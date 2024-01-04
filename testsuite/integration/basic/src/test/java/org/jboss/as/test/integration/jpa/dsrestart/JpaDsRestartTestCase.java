/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.dsrestart;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.security.Permission;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransactionControl;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.security.ControllerPermission;
import org.jboss.as.server.Services;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
public class JpaDsRestartTestCase {
    @ArquillianResource
    private InitialContext iniCtx;

    @ArquillianResource
    ServiceContainer serviceContainer;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "dsrestartjpa.war");
        war.addPackage(JpaInjectedSfsb.class.getPackage());
        // WEB-INF/classes is implied
        war.addAsResource(JpaDsRestartTestCase.class.getPackage(), "persistence.xml", "META-INF/persistence.xml");
        war.addAsManifestResource(JpaDsRestartTestCase.class.getPackage(), "MANIFEST.MF", "MANIFEST.MF");
        final Permission[] permissions = new Permission[] {
                ControllerPermission.CAN_ACCESS_MODEL_CONTROLLER, ControllerPermission.CAN_ACCESS_IMMUTABLE_MANAGEMENT_RESOURCE_REGISTRATION
                };
        war.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(permissions), "permissions.xml");
        return war;
    }


    @Test
    public void testRestartDataSource() throws Exception {
        Assert.assertNotNull(serviceContainer);

        NonJpaSfsb nonJpaSfsb = (NonJpaSfsb) iniCtx.lookup("java:module/" + NonJpaSfsb.class.getSimpleName());
        Assert.assertEquals("test", nonJpaSfsb.echo("test"));
        JpaInjectedSfsb jpaInjected = (JpaInjectedSfsb) iniCtx.lookup("java:module/" + JpaInjectedSfsb.class.getSimpleName());
        Assert.assertNotNull(jpaInjected.getEntityManager());
        EjbInjectedSfsb ejbInjected = (EjbInjectedSfsb) iniCtx.lookup("java:module/" + EjbInjectedSfsb.class.getSimpleName());
        Assert.assertNotNull(ejbInjected.getJpaEjb());
        Assert.assertNotNull(ejbInjected.getJpaEjb().getEntityManager());

        //Should fail since app has dependencies on it
        toggleDataSource(false, true);

        //The persistence unit service should be restarted
        Assert.assertEquals("test", nonJpaSfsb.echo("test"));
        Assert.assertNotNull(jpaInjected.getEntityManager());
        Assert.assertNotNull(ejbInjected.getJpaEjb());
        Assert.assertNotNull(ejbInjected.getJpaEjb().getEntityManager());
    }

    private void toggleDataSource(boolean enable, boolean expectFailure) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(enable ? "enable" : "disable");
        op.get(OP_ADDR).add("subsystem", "datasources").add("data-source", "H2DS");
        if (!enable) {
            op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        }
        ModelController controller = (ModelController) serviceContainer.getRequiredService(Services.JBOSS_SERVER_CONTROLLER).getValue();

        ModelNode result = controller.execute(op, OperationMessageHandler.logging, OperationTransactionControl.COMMIT, null);
        if (expectFailure) {
            //System.out.println("Expected failure " + result.get(FAILURE_DESCRIPTION).asString());
            Assert.assertEquals(FAILED, result.get(OUTCOME).asString());
        } else {
            Assert.assertEquals(result.get(FAILURE_DESCRIPTION).asString(), SUCCESS, result.get(OUTCOME).asString());
        }
    }
}
