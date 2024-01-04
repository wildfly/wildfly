/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

