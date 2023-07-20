/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
