/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.mdr;

import java.util.Set;

import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.core.spi.mdr.MetadataRepository;

/**
 * AS7's extension of MetadataRepository
 *
 * @author Stefano Maestri (c) 2011 Red Hat Inc.
 */
public interface AS7MetadataRepository extends MetadataRepository {

    Activation getIronJacamarMetaData(String uniqueId);

    Set<String> getResourceAdaptersWithIronJacamarMetadata();

}
