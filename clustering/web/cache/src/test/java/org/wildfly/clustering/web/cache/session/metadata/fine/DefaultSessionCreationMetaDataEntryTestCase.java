/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

/**
 * Unit test for {@link DefaultSessionCreationMetaDataEntry}.
 * @author Paul Ferraro
 */
public class DefaultSessionCreationMetaDataEntryTestCase extends AbstractSessionCreationMetaDataEntryTestCase {

    @Override
    public void accept(SessionCreationMetaDataEntry<Object> entry) {
        this.updateState(entry);
        this.verifyUpdatedState(entry);
    }
}
