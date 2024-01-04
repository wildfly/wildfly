/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import org.wildfly.clustering.ee.cache.function.Remappable;

/**
 * @author Paul Ferraro
 */
public interface SessionAccessMetaDataEntry extends SessionAccessMetaData, Remappable<SessionAccessMetaDataEntry, SessionAccessMetaDataEntryOffsets> {

}
