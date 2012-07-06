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

import javax.xml.stream.XMLStreamException;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.staxmapper.XMLExtendedStreamReader;

/**
 * Parser class that should be implemented by JBoss XML element parsers to parse their deployment descriptor data.
 *
 * @author Stuart Douglas
 */
public interface JBossAllXMLParser<T> {

    T parse(final XMLExtendedStreamReader reader, final DeploymentUnit deploymentUnit) throws XMLStreamException;
}
