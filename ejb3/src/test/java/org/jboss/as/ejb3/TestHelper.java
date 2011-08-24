/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3;

import org.jboss.as.server.deployment.Attachable;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.Services;
import org.jboss.as.server.deployment.SimpleAttachable;
import org.jboss.jandex.Indexer;
import org.jboss.msc.service.ServiceName;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class TestHelper {
    public static void index(Indexer indexer, Class<?> cls) throws IOException {
        InputStream stream = cls.getClassLoader().getResourceAsStream(cls.getName().replace('.', '/') + ".class");
        try {
            indexer.index(stream);
        } finally {
            stream.close();
        }
    }

    public static DeploymentUnit mockDeploymentUnit() {
        return mockDeploymentUnit("Mock Deployment Unit");
    }

    public static DeploymentUnit mockDeploymentUnit(String duName) {
        final Attachable attachable = new SimpleAttachable();
        final DeploymentUnit deploymentUnit = mock(DeploymentUnit.class);
        when(deploymentUnit.getName()).thenReturn(duName);

        when(deploymentUnit.getAttachment((AttachmentKey<Object>) any())).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                AttachmentKey<?> key = (AttachmentKey<?>) invocation.getArguments()[0];
                return attachable.getAttachment(key);
            }
        });
        when(deploymentUnit.putAttachment((AttachmentKey<Object>) any(), any())).thenAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                AttachmentKey<Object> key = (AttachmentKey<Object>) invocation.getArguments()[0];
                Object value = invocation.getArguments()[1];
                return attachable.putAttachment(key, value);
            }
        });
        ServiceName deploymentUnitServiceName = Services.deploymentUnitName(duName);
        when(deploymentUnit.getServiceName()).thenReturn(deploymentUnitServiceName);
        return deploymentUnit;
    }
}
