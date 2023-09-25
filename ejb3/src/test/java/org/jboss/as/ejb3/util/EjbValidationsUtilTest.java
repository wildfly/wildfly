/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.util;

import static org.jboss.as.ejb3.util.EjbValidationsUtil.assertEjbClassValidity;
import static org.jboss.as.ejb3.util.EjbValidationsUtil.verifyEjbPublicMethodAreNotFinalNorStatic;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.Test;

/**
 * Unit test for Mdb validation logic - see Jakarta Enterprise Beans 3.2 specification section 5.6.2 and 5.6.4 for more information
 *
 * @author <a href="mailto:romain@redhat.com">Romain Pelisse</a>
 */
public class EjbValidationsUtilTest {

    @Test
    public void mdbWithFinalClass() {
        assertTrue(assertEjbClassValidity(buildClassInfoForClass(InvalidMdbFinalClass.class.getName())).contains(
                MdbValidityStatus.MDB_CLASS_CANNOT_BE_PRIVATE_ABSTRACT_OR_FINAL));
    }

    @Test
    public void mdbWithInterface() {
        assertTrue(assertEjbClassValidity(buildClassInfoForClass(InvalidMdbInterface.class.getName())).contains(
                MdbValidityStatus.MDB_CANNOT_BE_AN_INTERFACE));
    }

    @Test
    public void mdbWithFinalOnMessageMethod() {
        assertTrue(assertEjbClassValidity(buildClassInfoForClass(InvalidMdbOnMessageCantBeFinal.class.getName())).contains(
                MdbValidityStatus.MDB_ON_MESSAGE_METHOD_CANT_BE_FINAL));
    }

    @Test
    public void mdbWithStaticOnMessageMethod() {
        assertTrue(assertEjbClassValidity(buildClassInfoForClass(InvalidMdbOnMessageCantBeStatic.class.getName())).contains(
                MdbValidityStatus.MDB_ON_MESSAGE_METHOD_CANT_BE_STATIC));
    }

    @Test
    public void mdbWithPrivateOnMessageMethod() {
        assertTrue(assertEjbClassValidity(buildClassInfoForClass(InvalidMdbOnMessageCantBePrivate.class.getName())).contains(
                MdbValidityStatus.MDB_ON_MESSAGE_METHOD_CANT_BE_PRIVATE));
    }

    @Test
    public void mdbWithFinalizeMethod() {
        assertTrue(assertEjbClassValidity(buildClassInfoForClass(InvalidMdbWithFinalizeMethod.class.getName())).contains(
                MdbValidityStatus.MDB_SHOULD_NOT_HAVE_FINALIZE_METHOD));
    }

    @Test
    public void ejbWithFinalOrStaticMethods() {
        assertTrue(assertEjbClassValidity(buildClassInfoForClass(EjbWithPrivateFinalMethod.class.getName())).isEmpty());
        assertTrue(verifyEjbPublicMethodAreNotFinalNorStatic(EjbWithPrivateFinalMethod.class.getMethods(), EjbWithPrivateFinalMethod.class.getName()));

        // EjbWithStaticMethod and EjbWithFinalMethod should produce false validation result
        assertFalse(verifyEjbPublicMethodAreNotFinalNorStatic(EjbWithStaticMethod.class.getMethods(), EjbWithStaticMethod.class.getName()));
        assertFalse(verifyEjbPublicMethodAreNotFinalNorStatic(EjbWithFinalMethod.class.getMethods(), EjbWithFinalMethod.class.getName()));

        // CleanBean contains some non-public final or static methods along with regular business methods,
        // and should not cause validation warning
        assertTrue(verifyEjbPublicMethodAreNotFinalNorStatic(CleanBean.class.getMethods(), CleanBean.class.getName()));

        // TaintedBean inherits public static and final methods from its super class, so produce false validation result
        assertFalse(verifyEjbPublicMethodAreNotFinalNorStatic(TaintedBean.class.getMethods(), TaintedBean.class.getName()));
    }

    private ClassInfo buildClassInfoForClass(String mdbClassName) {
        String mdbClassNameAsResource = mdbClassName.replaceAll("\\.", "/").concat(".class");
        Index index = indexStream(getClass().getClassLoader().getResourceAsStream(mdbClassNameAsResource)).complete();
        return index.getClassByName(DotName.createSimple(mdbClassName));
    }


    private Indexer indexStream(InputStream stream) {
        try {
            Indexer indexer = new Indexer();
            indexer.index(stream);
            stream.close();
            return indexer;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
