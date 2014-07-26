/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jmx.model;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ConfiguredDomains {
    private final String legacyDomain;
    private final String exprDomain;

    public ConfiguredDomains(String legacyDomain, String exprDomain){
        assert legacyDomain != null || exprDomain != null;
        this.legacyDomain = legacyDomain;
        this.exprDomain = exprDomain;
    }

    String[] getDomains() {
        if (legacyDomain != null && exprDomain != null) {
            return new String[] {legacyDomain, exprDomain};
        } else if (legacyDomain != null) {
            return new String[] {legacyDomain};
        } else if (exprDomain != null) {
            return new String[] {exprDomain};
        } else {
            return new String[] {};
        }

    }

    ObjectName getMirroredObjectName(ObjectName name) {
        String domain = name.getDomain();
        String mirroredDomain = null;
        if (domain.equals(legacyDomain)) {
            mirroredDomain = exprDomain;
        } else if (domain.equals(exprDomain)) {
            mirroredDomain = legacyDomain;
        }
        if (mirroredDomain == null) {
            return null;
        }
        try {
            return new ObjectName(mirroredDomain, name.getKeyPropertyList());
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    boolean isLegacyDomain(ObjectName name) {
        return name.getDomain().equals(legacyDomain);
    }

    String getLegacyDomain() {
        return legacyDomain;
    }

    String getExprDomain() {
        return exprDomain;
    }
}
