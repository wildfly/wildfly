/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */


package org.jboss.as.ejb3.util;

/**
 * @author Romain Pelisse - romain@redhat.com
 */
public enum MdbValidityStatus {
    MDB_CANNOT_BE_AN_INTERFACE, MDB_CLASS_CANNOT_BE_PRIVATE_ABSTRACT_OR_FINAL, MDB_ON_MESSAGE_METHOD_CANT_BE_FINAL, MDB_ON_MESSAGE_METHOD_CANT_BE_STATIC, MDB_ON_MESSAGE_METHOD_CANT_BE_PRIVATE, MDB_SHOULD_NOT_HAVE_FINALIZE_METHOD, MDB_IS_VALID;
}
