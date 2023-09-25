/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.junit.Test;

public class OidcActivationProcessorTest {

    private final class NonNullRealmNullEverythingElseWarMetaData extends WarMetaData {
        private final class JBossWebMetaDataExtension extends JBossWebMetaData {
            @Override
            public LoginConfigMetaData getLoginConfig() {
                return new LoginConfigMetaDataExtension();
            }
        }

        private final class LoginConfigMetaDataExtension extends LoginConfigMetaData {
            @Override
            public String getRealmName() {
                return "NON-NULL";
            }
        }

        @Override
        public JBossWebMetaData getMergedJBossWebMetaData() {
            return new JBossWebMetaDataExtension();
        }
    }

    /**
     * Should be able to deploy a web application that specifies a realm, but nothing else in its login-config
     */
    @Test
    public void testDeployLoginConfigWithRealmAndNullAuthMethod() throws Exception {
        DeploymentUnit unit = mock(DeploymentUnit.class);
        doReturn(true).when(unit).hasAttachment(WarMetaData.ATTACHMENT_KEY);
        doReturn(new NonNullRealmNullEverythingElseWarMetaData()).when(unit).getAttachment(WarMetaData.ATTACHMENT_KEY);

        DeploymentPhaseContext context = mock(DeploymentPhaseContext.class);
        doReturn(unit).when(context).getDeploymentUnit();

        new OidcActivationProcessor().deploy(context);

        assertTrue("Expect to succeed and reach this point", true);
    }
}
