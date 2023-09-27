/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.iiop;

import jakarta.transaction.Transaction;
import java.security.Principal;

/**
 * Interface used by local IIOP invocations.
 *
 * @author <a href="mailto:reverbel@ime.usp.br">Francisco Reverbel</a>
 */
public interface LocalIIOPInvoker {
    Object invoke(String opName, Object[] arguments, Transaction tx, Principal identity, Object credential) throws Exception;
}
