/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

/**
 * @author Paul Ferraro
 */
public class DefaultSessionMetaDataEntryTestCase extends AbstractSessionMetaDataEntryTestCase {

    @Override
    public void accept(ContextualSessionMetaDataEntry<Object> entry) {
        this.updateState(entry);
        this.verifyUpdatedState(entry);
    }
}
