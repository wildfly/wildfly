/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;

import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ManagementDescription;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.dmr.ModelNode;

/**
 * A handler that activates the HTTP management API.
 *
 * @author Jason T. Greene
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class HttpManagementAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    private final LocalHostControllerInfoImpl hostControllerInfo;

    private HttpManagementAddHandler(final LocalHostControllerInfoImpl hostControllerInfo) {
        this.hostControllerInfo = hostControllerInfo;
    }

    public static HttpManagementAddHandler getInstance(final LocalHostControllerInfoImpl hostControllerInfo) {
        return new HttpManagementAddHandler(hostControllerInfo);
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        final String interfaceName = operation.require(ModelDescriptionConstants.INTERFACE).asString();
        final int port = getIntValue(operation, ModelDescriptionConstants.PORT);
        final int securePort = getIntValue(operation, ModelDescriptionConstants.SECURE_PORT);
        final String securityRealm = operation.hasDefined(SECURITY_REALM) ? operation.get(SECURITY_REALM).asString() : null;

        model.get(ModelDescriptionConstants.INTERFACE).set(interfaceName);
        if (port > -1) {
            model.get(ModelDescriptionConstants.PORT).set(port);
        }
        if (securePort > -1) {
            model.get(ModelDescriptionConstants.SECURE_PORT).set(securePort);
        }
        if (securityRealm != null) {
            model.get(ModelDescriptionConstants.SECURITY_REALM).set(securityRealm);
        }

        hostControllerInfo.setHttpManagementInterface(interfaceName);
        hostControllerInfo.setHttpManagementPort(port);
        hostControllerInfo.setHttpManagementSecurePort(securePort);
        hostControllerInfo.setHttpManagementSecurityRealm(securityRealm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ManagementDescription.getAddHttpManagementDescription(locale);
    }

    private int getIntValue(ModelNode source, String name) {
        if (source.has(name)) {
            return source.require(name).asInt();
        }
        return -1;
    }
}
