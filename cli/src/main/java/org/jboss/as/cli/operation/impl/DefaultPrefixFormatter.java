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
package org.jboss.as.cli.operation.impl;

import java.util.Iterator;

import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.NodePathFormatter;
import org.jboss.as.cli.operation.OperationRequestAddress.Node;

/**
 *
 * @author Alexey Loubyansky
 */
public class DefaultPrefixFormatter implements NodePathFormatter {

    /* (non-Javadoc)
     * @see org.jboss.as.cli.PrefixFormatter#format(org.jboss.as.cli.Prefix)
     */
    @Override
    public String format(OperationRequestAddress prefix) {

        Iterator<Node> iterator = prefix.iterator();
        if(!iterator.hasNext()) {
            return "/";
        }

        StringBuilder builder = new StringBuilder();
        builder.append('/');
        Node next = iterator.next();
        builder.append(next.getType());
        if(next.getName() != null) {
            builder.append('=').append(next.getName());
        }
        while(iterator.hasNext()) {
            builder.append('/');
            next = iterator.next();
            builder.append(next.getType());
            if(next.getName() != null) {
                builder.append('=').append(next.getName());
            }
        }
        return builder.toString();
    }

}
