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

package org.jboss.as.server.deployment.jbossallxml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Package private class that holds the parsing state
 *
 * @author Stuart Douglas
 */
class JBossAllXmlParseContext {

    private final DeploymentUnit deploymentUnit;

    private final Map<QName, Object> parseResults = new HashMap<QName, Object>();

    public JBossAllXmlParseContext(final DeploymentUnit deploymentUnit) {
        this.deploymentUnit = deploymentUnit;
    }

    public DeploymentUnit getDeploymentUnit() {
        return deploymentUnit;
    }

    public void addResult(final QName namespace, final Object result, final Location location) throws XMLStreamException {
        if(parseResults.containsKey(namespace)) {
            throw ServerMessages.MESSAGES.duplicateJBossXmlNamespace(namespace, location);
        }
        parseResults.put(namespace, result);
    }

    public Map<QName, Object> getParseResults() {
        return Collections.unmodifiableMap(parseResults);
    }
}
