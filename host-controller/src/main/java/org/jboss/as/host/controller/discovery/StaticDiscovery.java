/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.host.controller.discovery;

import static java.lang.String.valueOf;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MASTER;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jboss.as.host.controller.discovery.S3Util.AWSAuthConnection;
import org.jboss.as.host.controller.discovery.S3Util.Bucket;
import org.jboss.as.host.controller.discovery.S3Util.GetResponse;
import org.jboss.as.host.controller.discovery.S3Util.ListAllMyBucketsResponse;
import org.jboss.as.host.controller.discovery.S3Util.ListBucketResponse;
import org.jboss.as.host.controller.discovery.S3Util.ListEntry;
import org.jboss.as.host.controller.discovery.S3Util.PreSignedUrlParser;
import org.jboss.as.host.controller.discovery.S3Util.S3Object;

/**
 * Handle domain controller discovery via static (i.e., hard-wired) configuration.
 *
 * @author Farah Juma
 */
public class StaticDiscovery implements DiscoveryOption {

    private String remoteDcHost;
    private int remoteDcPort;

    /**
     * Create the StaticDiscovery option.
     *
     * @param remoteDcHost the host name of the domain controller
     * @param remoteDcPort the port number of the domain controller
     */
    public StaticDiscovery(String remoteDcHost, int remoteDcPort) {
        this.remoteDcHost = remoteDcHost;
        this.remoteDcPort = remoteDcPort;
    }

    @Override
    public void allowDiscovery(String host, int port) {
        // no-op
    }

    @Override
    public void discover() {
        // no-op
    }

    @Override
    public void cleanUp() {
        // no-op
    }

    @Override
    public String getRemoteDomainControllerHost() {
        return remoteDcHost;
    }

    @Override
    public int getRemoteDomainControllerPort() {
        return remoteDcPort;
    }
}
