/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.txn.subsystem;

/**
 * The {@link org.jboss.staxmapper.XMLElementReader} that handles the version 6.1 of Transaction subsystem xml.
 */
class TransactionSubsystem61Parser extends TransactionSubsystem60Parser {

    TransactionSubsystem61Parser() {
        super(Namespace.TRANSACTIONS_6_1);
    }

    TransactionSubsystem61Parser(Namespace namespace) {
        super(namespace);
    }
}
