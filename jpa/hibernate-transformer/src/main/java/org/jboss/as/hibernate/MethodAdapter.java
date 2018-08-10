/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.hibernate;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * MethodAdapter
 *
 * @deprecated
 */
class MethodAdapter extends MethodVisitor {

    private static final boolean disableAmbiguousChanges = Boolean.parseBoolean(
            WildFlySecurityManager.getPropertyPrivileged("Hibernate51CompatibilityTransformer.disableAmbiguousChanges", "false"));
    public static final BasicLogger logger = Logger.getLogger("org.jboss.as.hibernate.transformer");
    private final boolean rewriteSessionImplementor;
    private final TransformedState transformedState;
    private final MethodVisitor mv;
    private final String moduleName;
    private final String className;

    MethodAdapter(boolean rewriteSessionImplementor, int api, MethodVisitor mv,
                  final String moduleName, final String className, TransformedState transformedState) {
        super(api, mv);
        this.rewriteSessionImplementor = rewriteSessionImplementor;
        this.mv = mv;
        this.moduleName = moduleName;
        this.className = className;
        this.transformedState = transformedState;
    }


    // Change call to org.hibernate.BasicQueryContract.getFlushMode() to instead call BasicQueryContract.getHibernateFlushMode().
    // Change call to org.hibernate.Session.getFlushMode, to instead call Session.getHibernateFlushMode()
    // Calls to Hibernate ORM 5.3 getFlushMode(), will not be changed as the desc will not match (desc == "()Ljavax.persistence.FlushModeType;")
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (rewriteSessionImplementor &&
                (opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKEVIRTUAL) &&
                owner.startsWith("org/hibernate/")) {
            // if we have a user type calling a method from org.hibernate, we rewrite it to use SharedSessionContractImplementor
            logger.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                            "class '%s' is calling method %s.%s, which must be changed to use SharedSessionContractImplementor as parameter.",
                    moduleName, className, owner, name);
            mv.visitMethodInsn(opcode, owner, name, replaceSessionImplementor(desc), itf);
            transformedState.setClassTransformed(true);
        } else if (opcode == Opcodes.INVOKEINTERFACE &&
                (owner.equals("org/hibernate/Session") || owner.equals("org/hibernate/BasicQueryContract"))
                && name.equals("getFlushMode") && desc.equals("()Lorg/hibernate/FlushMode;")) {
            logger.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                            "class '%s' is calling %s.getFlushMode, which must be changed to call getHibernateFlushMode().",
                    moduleName, className, owner);
            name = "getHibernateFlushMode";
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
            transformedState.setClassTransformed(true);
        } else if (opcode == Opcodes.INVOKEINTERFACE &&
                owner.equals("org/hibernate/Query") &&
                name.equals("getFirstResult") &&
                desc.equals("()Ljava/lang/Integer;")) {
            logger.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                            "class '%s', is calling org.hibernate.Query.getFirstResult, which must be changed to call getHibernateFirstResult() " +
                            "so null can be returned when the value is uninitialized. Please note that if a negative value was set using " +
                            "org.hibernate.Query.setFirstResult, then getHibernateFirstResult() will return 0.",
                    moduleName, className);
            name = "getHibernateFirstResult";
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
            transformedState.setClassTransformed(true);
        } else if (opcode == Opcodes.INVOKEINTERFACE &&
                owner.equals("org/hibernate/Query") &&
                name.equals("getMaxResults") &&
                desc.equals("()Ljava/lang/Integer;")) {
            logger.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                            "class '%s', is calling org.hibernate.Query.getMaxResults, which must be changed to call getHibernateMaxResults() " +
                            "so that null will be returned when the value is uninitialized or ORM 5.1 org.hibernate.Query#setMaxResults was " +
                            "used to set a value <= 0"
                    , moduleName, className);
            name = "getHibernateMaxResults";
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
            transformedState.setClassTransformed(true);
        } else if (!disableAmbiguousChanges && opcode == Opcodes.INVOKEINTERFACE &&
                owner.equals("org/hibernate/Query") &&
                name.equals("setFirstResult") &&
                desc.equals("(I)Lorg/hibernate/Query;")) {
            logger.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                            "class '%s', is calling org.hibernate.Query.setFirstResult, which must be changed to call setHibernateFirstResult() " +
                            "so setting a value < 0 results in pagination starting with the 0th row as was done in Hibernate ORM 5.1 " +
                            "(instead of throwing IllegalArgumentException as specified by JPA)."
                    , moduleName, className);
            name = "setHibernateFirstResult";
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
            transformedState.setClassTransformed(true);
        } else if (!disableAmbiguousChanges && opcode == Opcodes.INVOKEINTERFACE &&
                owner.equals("org/hibernate/Query") &&
                name.equals("setMaxResults") &&
                desc.equals("(I)Lorg/hibernate/Query;")) {
            logger.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                            "class '%s', is calling org.hibernate.Query.setMaxResults, which must be changed to call setHibernateMaxResults() " +
                            "so that values <= 0 are treated the same as uninitialized.  Review Hibernate ORM migration doc "
                    , moduleName, className);
            name = "setHibernateMaxResults";
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
            transformedState.setClassTransformed(true);
        } else {
            mv.visitMethodInsn(opcode, owner, name, desc, itf);
        }

    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        // App References to Enum org.hibernate.FlushMode.NEVER (0) should be transformed to reference FlushMode.MANUAL (0) instead.
        if (opcode == Opcodes.GETSTATIC &&
                owner.equals("org/hibernate/FlushMode") &&
                name.equals("NEVER") &&
                desc.equals("Lorg/hibernate/FlushMode;")) {
            logger.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                            "class '%s' is using org.hibernate.FlushMode.NEVER, change to org.hibernate.FlushMode.MANUAL."
                    , moduleName, className);
            mv.visitFieldInsn(opcode, owner, "MANUAL", desc);
            transformedState.setClassTransformed(true);
        } else {
            mv.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    private static String replaceSessionImplementor(String desc) {
        return desc.replace("Lorg/hibernate/engine/spi/SessionImplementor;",
                "Lorg/hibernate/engine/spi/SharedSessionContractImplementor;");
    }

}
