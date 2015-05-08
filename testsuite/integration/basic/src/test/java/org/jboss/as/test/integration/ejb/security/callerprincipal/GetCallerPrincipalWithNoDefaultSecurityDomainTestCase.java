/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security.callerprincipal;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.subsystem.EJB3Extension;
import org.jboss.as.ejb3.subsystem.EJB3SubsystemModel;
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

    private static final Logger log = Logger.getLogger(GetCallerPrincipalWithNoDefaultSecurityDomainTestCase.class);

    private static final String ANONYMOUS = "anonymous"; //TODO: is this constant configured somewhere?

    private static final String MODULE_NAME = "callerprincipal-without-default-security-domain";

    /**
     * Server setup task responsible for disabling (and then re-enabling) the default security domain
     * that's configured at EJB3 subsystem level
     */
    static class DisableDefaultSecurityDomainSetupTask extends AbstractMgmtServerSetupTask {

        private String defaultSecurityDomain;

        @Override
        protected void doSetup(final ManagementClient managementClient) throws Exception {
            // first read the current value of the default-security-domain
            final PathAddress ejb3SubsystemPathAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME));

            final ModelNode defaultSecurityDomainAttr = new ModelNode();
            defaultSecurityDomainAttr.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
            defaultSecurityDomainAttr.get(NAME).set(EJB3SubsystemModel.DEFAULT_SECURITY_DOMAIN);
            defaultSecurityDomainAttr.get(OP_ADDR).set(ejb3SubsystemPathAddress.toModelNode());

            final ModelNode readResult = executeOperation(defaultSecurityDomainAttr);
            this.defaultSecurityDomain = readResult.asString();
            // remove the default security domain from EJB3 subsystem
            final ModelNode disableDefaultSecurityDomain = new ModelNode();
            disableDefaultSecurityDomain.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION);
            disableDefaultSecurityDomain.get(NAME).set(EJB3SubsystemModel.DEFAULT_SECURITY_DOMAIN);
            disableDefaultSecurityDomain.get(OP_ADDR).set(ejb3SubsystemPathAddress.toModelNode());

            executeOperation(disableDefaultSecurityDomain);


        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final PathAddress ejb3SubsystemPathAddress = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME));
            final ModelNode defaultSecurityDomainAttr = new ModelNode();
            defaultSecurityDomainAttr.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            defaultSecurityDomainAttr.get(NAME).set(EJB3SubsystemModel.DEFAULT_SECURITY_DOMAIN);
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
        log.info(jar.toString(true));
        return jar;
    }

    /**
     * Tests that the {@link javax.ejb.EJBContext#getCallerPrincipal()} works as expected in the absence of
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
            e.printStackTrace();
            log.error(e.getStackTrace());
            fail("EJB 3.1 FR 17.6.5 The EJB container must provide the caller’s security context information during the execution of a business method ("
                    + e.getMessage() + ")");
        }
    }

}
