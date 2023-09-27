/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.callerprincipal;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import java.security.Principal;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Tests that when the default security domain is disabled at EJB3 subsystem level then the security API
 * invocations on the EJB work correctly
 *
 * @author Jaikiran Pai
 * @see https://issues.jboss.org/browse/AS7-5581
 */
@RunWith(Arquillian.class)
@ServerSetup({GetCallerPrincipalWithNoDefaultSecurityDomainTestCase.DisableDefaultSecurityDomainSetupTask.class})
@Category(CommonCriteria.class)
public class GetCallerPrincipalWithNoDefaultSecurityDomainTestCase {
    private static final Logger LOGGER = Logger.getLogger(GetCallerPrincipalWithNoDefaultSecurityDomainTestCase.class);

    private static final String ANONYMOUS = "anonymous"; //TODO: is this constant configured somewhere?

    private static final String MODULE_NAME = "callerprincipal-without-default-security-domain";

    /**
     * Server setup task responsible for disabling (and then re-enabling) the default security domain
     * that's configured at EJB3 subsystem level
     */
    static class DisableDefaultSecurityDomainSetupTask extends AbstractMgmtServerSetupTask {

        private static final String DEFAULT_SECURITY_DOMAIN = "default-security-domain";
        private static final String SUBSYSTEM_NAME = "ejb3";

        private String defaultSecurityDomain;

        @Override
        protected void doSetup(final ManagementClient managementClient) throws Exception {
            // first read the current value of the default-security-domain
            final PathAddress ejb3SubsystemPathAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME));

            final ModelNode defaultSecurityDomainAttr = new ModelNode();
            defaultSecurityDomainAttr.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
            defaultSecurityDomainAttr.get(NAME).set(DEFAULT_SECURITY_DOMAIN);
            defaultSecurityDomainAttr.get(OP_ADDR).set(ejb3SubsystemPathAddress.toModelNode());

            final ModelNode readResult = executeOperation(defaultSecurityDomainAttr);
            this.defaultSecurityDomain = readResult.asString();
            // remove the default security domain from EJB3 subsystem
            final ModelNode disableDefaultSecurityDomain = new ModelNode();
            disableDefaultSecurityDomain.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION);
            disableDefaultSecurityDomain.get(NAME).set(DEFAULT_SECURITY_DOMAIN);
            disableDefaultSecurityDomain.get(OP_ADDR).set(ejb3SubsystemPathAddress.toModelNode());

            executeOperation(disableDefaultSecurityDomain);


        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final PathAddress ejb3SubsystemPathAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME));
            final ModelNode defaultSecurityDomainAttr = new ModelNode();
            defaultSecurityDomainAttr.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            defaultSecurityDomainAttr.get(NAME).set(DEFAULT_SECURITY_DOMAIN);
            defaultSecurityDomainAttr.get(VALUE).set(this.defaultSecurityDomain);
            defaultSecurityDomainAttr.get(OP_ADDR).set(ejb3SubsystemPathAddress.toModelNode());

            executeOperation(defaultSecurityDomainAttr);
        }

    }

    @ArquillianResource
    private InitialContext initialContext;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar")
                .addClass(GetCallerPrincipalWithNoDefaultSecurityDomainTestCase.class)
                .addClass(SLSBWithoutSecurityDomain.class)
                .addClass(ISLSBWithoutSecurityDomain.class)
                .addClasses(DisableDefaultSecurityDomainSetupTask.class, AbstractMgmtTestBase.class)
                .addPackage(AbstractMgmtTestBase.class.getPackage()).addClasses(MgmtOperationException.class, XMLElementReader.class, XMLElementWriter.class)
                .addAsManifestResource(GetCallerPrincipalWithNoDefaultSecurityDomainTestCase.class.getPackage(), "MANIFEST.MF-no-default-security-domain", "MANIFEST.MF");
        jar.addPackage(CommonCriteria.class.getPackage());
        return jar;
    }

    /**
     * Tests that the {@link jakarta.ejb.EJBContext#getCallerPrincipal()} works as expected in the absence of
     * any default security domain at EJB3 susbsystem level and any explicit security domain on the bean
     */
    @Test
    public void testUnauthenticatedNoSecurityDomain() throws Exception {
        try {
            ISLSBWithoutSecurityDomain bean = (ISLSBWithoutSecurityDomain) initialContext.lookup("ejb:/" + MODULE_NAME + "//" + SLSBWithoutSecurityDomain.class.getSimpleName() + "!" + ISLSBWithoutSecurityDomain.class.getName());
            final Principal principal = bean.getCallerPrincipal();
            assertNotNull("EJB 3.1 FR 17.6.5 The container must never return a null from the getCallerPrincipal method.",
                    principal);
            assertEquals(ANONYMOUS, principal.getName());
        } catch (RuntimeException e) {
            LOGGER.error("EJB 3.1 FR 17.6.5", e);
            fail("EJB 3.1 FR 17.6.5 The EJB container must provide the caller’s security context information during the execution of a business method ("
                    + e.getMessage() + ")");
        }
    }

}
