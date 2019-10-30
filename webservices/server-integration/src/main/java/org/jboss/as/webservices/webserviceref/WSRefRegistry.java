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

package org.jboss.as.webservices.webserviceref;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;

/**
 * WebServiceRef registry.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSRefRegistry {

    private final Map<String, UnifiedServiceRefMetaData> references = new HashMap<String, UnifiedServiceRefMetaData>(8);

    private WSRefRegistry() {
        // forbidden inheritance
    }

    public static WSRefRegistry newInstance() {
        return new WSRefRegistry();
    }

    public void add(final String refName, final UnifiedServiceRefMetaData serviceRefUMDM) {
        if (references.containsKey(refName)) throw new UnsupportedOperationException();
        references.put(refName, serviceRefUMDM);
    }

    public UnifiedServiceRefMetaData get(final String refName) {
        return references.get(refName);
    }

    public Collection<UnifiedServiceRefMetaData> getUnifiedServiceRefMetaDatas() {
        return Collections.unmodifiableCollection(references.values());
    }


    public void clear() {
        references.clear();
    }

}

