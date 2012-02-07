/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.scanner;

/**
 * @author Emanuel Muckenhuber
 */
interface CommonAttributes {

    String AUTO_DEPLOY_ZIPPED = "auto-deploy-zipped";
    String AUTO_DEPLOY_EXPLODED = "auto-deploy-exploded";
    String AUTO_DEPLOY_XML = "auto-deploy-xml";
    String DEPLOYMENT_SCANNER = "deployment-scanner";
    String DEPLOYMENT_TIMEOUT = "deployment-timeout";
    String NAME = "name";
    String PATH = "path";
    String RELATIVE_TO = "relative-to";
    String SCANNER = "scanner";
    String SCAN_ENABLED = "scan-enabled";
    String SCAN_INTERVAL = "scan-interval";

}
