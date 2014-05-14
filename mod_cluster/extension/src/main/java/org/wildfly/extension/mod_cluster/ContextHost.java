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

package org.wildfly.extension.mod_cluster;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.util.Iterator;
import java.util.List;

public class ContextHost {
    String webcontext = null;
    String webhost = null;
    int waittime = 10;

    public ContextHost(ModelNode operation) throws OperationFailedException {
        List<Property> list = operation.asPropertyList();
        Iterator<Property> it = list.iterator();
        webcontext = null;
        webhost = null;
        while (it.hasNext()) {
            Property prop = it.next();
            if (prop.getName().equals("context")) {
                webcontext = prop.getValue().toString();
            }
            if (prop.getName().equals("virtualhost")) {
                webhost = prop.getValue().toString();
            }
            if (prop.getName().equals("waittime")) {
                waittime = Integer.parseInt(RemoveQuotes(prop.getValue().toString()));
            }
        }
        if (webcontext == null || webhost == null) { throw new OperationFailedException(new ModelNode().set(ModClusterLogger.ROOT_LOGGER.needContextAndHost())); }

        webcontext = RemoveQuotes(webcontext);
        webhost = RemoveQuotes(webhost);
        if (webcontext.equals("/")) { webcontext = ""; }
    }

    public static String RemoveQuotes(String string) {
        if (string.endsWith("\"") && string.startsWith("\"")) { return string.substring(1, string.length() - 1); }
        return string;
    }
}
