/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

/**
 * @author Paul Ferraro
 */
public class DefaultSessionAccessMetaDataEntryTestCase extends AbstractSessionAccessMetaDataEntryTestCase {

    @Override
    public void accept(SessionAccessMetaDataEntry entry) {
        this.updateState(entry);
        this.verifyUpdatedState(entry);
    }
}
