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

package org.wildfly.extension.mod_cluster;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;

/**
 * @author Jean-Frederic Clere
 */
public class ProxyListValidator implements ParameterValidator {

    @Override
    public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
        if (value.isDefined()) {
            String str = value.asString();
            String[] results = str.split(",");
            for (String result : results) {
                int i = result.lastIndexOf(":");
                int port = 0;
                String host = null;
                if (i > 0) {
                    host = result.substring(0, i);
                    port = Integer.valueOf(result.substring(i + 1));
                }
                try {
                    InetAddress.getByName(host);
                } catch (UnknownHostException e) {
                    host = null;
                }
                if (host == null || port == 0) {
                    throw new OperationFailedException(ModClusterLogger.ROOT_LOGGER.needHostAndPort());
                }
            }
        }

    }

    @Override
    public void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException {
        validateParameter(parameterName, value.resolve());
    }

}
