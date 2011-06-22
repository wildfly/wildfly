/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.protocol.mgmt;

/**
 * Contains information relevant to a request being handled on the receiving end
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ManagementRequestContext {

    final ManagementChannel channel;

    final ManagementRequestHeader header;

    ManagementRequestContext(final ManagementChannel channel, final ManagementRequestHeader header) {
        this.channel = channel;
        this.header = header;
    }

    /**
     * Get the channel
     * @return the channel
     */
    public ManagementChannel getChannel() {
        return channel;
    }

    /**
     * Get the header
     * @return the header
     */
    public ManagementRequestHeader getHeader() {
        return header;
    }
}
