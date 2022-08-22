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

import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DelegatingSupplier;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.junit.Test;

public class OidcActivationProcessorTest {
    private final class MockDeploymentPhaseContext implements DeploymentPhaseContext {
        private final class MockDeploymentUnit implements DeploymentUnit {
            @Override
            public boolean hasAttachment(AttachmentKey<?> key) {
                return false;
            }

            @Override
            public <T> T getAttachment(AttachmentKey<T> key) {
                if (WarMetaData.ATTACHMENT_KEY.equals(key)) {
                    return (T) new NonNullRealmNullEverythingElseWarMetaData();
                }

                return null;
            }

            @Override
            public <T> List<T> getAttachmentList(AttachmentKey<? extends List<T>> key) {
                return null;
            }

            @Override
            public <T> T putAttachment(AttachmentKey<T> key, T value) {
                return null;
            }

            @Override
            public <T> T removeAttachment(AttachmentKey<T> key) {
                return null;
            }

            @Override
            public <T> void addToAttachmentList(AttachmentKey<AttachmentList<T>> key, T value) {
            }

            @Override
            public ServiceName getServiceName() {
                return null;
            }

            @Override
            public DeploymentUnit getParent() {
                return null;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public ServiceRegistry getServiceRegistry() {
                return null;
            }

            @Override
            public ModelNode getDeploymentSubsystemModel(String subsystemName) {
                return null;
            }

            @Override
            public ModelNode createDeploymentSubModel(String subsystemName, PathElement address) {
                return null;
            }

            @Override
            public ModelNode createDeploymentSubModel(String subsystemName, PathAddress address) {
                return null;
            }

            @Override
            public ModelNode createDeploymentSubModel(String subsystemName, PathAddress address,
                    Resource resource) {
                return null;
            }
        }

        @Override
        public <T> void addToAttachmentList(AttachmentKey<AttachmentList<T>> arg0, T arg1) {
        }

        @Override
        public <T> T getAttachment(AttachmentKey<T> arg0) {
            return null;
        }

        @Override
        public <T> List<T> getAttachmentList(AttachmentKey<? extends List<T>> arg0) {
            return null;
        }

        @Override
        public boolean hasAttachment(AttachmentKey<?> arg0) {
            return false;
        }

        @Override
        public <T> T putAttachment(AttachmentKey<T> arg0, T arg1) {
            return null;
        }

        @Override
        public <T> T removeAttachment(AttachmentKey<T> arg0) {
            return null;
        }

        @Override
        public <T> void addDependency(ServiceName arg0, AttachmentKey<T> arg1) {
        }

        public <T> void addDependency(ServiceName arg0, Class<T> arg1, Injector<T> arg2) {
        }

        @Override
        public <T> void addDeploymentDependency(ServiceName arg0, AttachmentKey<T> arg1) {
        }

        @Override
        public DeploymentUnit getDeploymentUnit() {
            return new MockDeploymentUnit();
        }

        @Override
        public Phase getPhase() {
            return null;
        }

        @Override
        public ServiceName getPhaseServiceName() {
            return null;
        }

        @Override
        public ServiceRegistry getServiceRegistry() {
            return null;
        }

        @Override
        public ServiceTarget getServiceTarget() {
            return null;
        }

        @Override
        public <T> void requires(ServiceName arg0, DelegatingSupplier<T> arg1) {
        }
    }

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
        new OidcActivationProcessor().deploy(new MockDeploymentPhaseContext());

        assertTrue("Expect to succeed and reach this point", true);
    }
}
