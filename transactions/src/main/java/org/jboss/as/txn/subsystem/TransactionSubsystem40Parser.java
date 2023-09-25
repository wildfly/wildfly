/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.txn.subsystem;

/**
 * The {@link org.jboss.staxmapper.XMLElementReader} that handles the version 4.0 of Transaction subsystem xml.
 */
class TransactionSubsystem40Parser extends TransactionSubsystem30Parser {

    TransactionSubsystem40Parser() {
        super(Namespace.TRANSACTIONS_4_0);
        this.relativeToHasDefaultValue = false;
    }

    TransactionSubsystem40Parser(Namespace namespace) {
        super(namespace);
        this.relativeToHasDefaultValue = false;
    }
}
