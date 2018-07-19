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

package org.jboss.as.jpa;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import org.jboss.modules.ModuleClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A class file transformer which makes deployment classes written for Hibernate 5.1 be compatible with Hibernate 5.3.
 *
 * @deprecated
 */
public class Hibernate51CompatibilityTransformer implements ClassFileTransformer {

    private static final Hibernate51CompatibilityTransformer instance = new Hibernate51CompatibilityTransformer();
    private static final boolean disableAmbiguousChanges = Boolean.parseBoolean(
            WildFlySecurityManager.getPropertyPrivileged("Hibernate51CompatibilityTransformer.disableAmbiguousChanges", "false"));

    private Hibernate51CompatibilityTransformer() {
    }

    public static Hibernate51CompatibilityTransformer getInstance() {
        return instance;
    }

    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain, final byte[] classfileBuffer) {
        ROOT_LOGGER.debugf("Hibernate51CompatibilityTransformer transforming deployment class '%s' from '%s'", className, getModuleName(loader));
        final ClassReader classReader = new ClassReader(classfileBuffer);
        final ClassWriter cv = new ClassWriter(classReader, 0);
        classReader.accept(new ClassVisitor(Opcodes.ASM6, cv) {
            boolean implementsUserType = false;
            boolean implementsCompositeUserType = false;
            boolean implementsUserCollectionType = false;

            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                if (interfaces != null) {
                    for (String interfaceName : interfaces) {
                        if ("org/hibernate/usertype/UserType".equals(interfaceName)) {
                            implementsUserType = true;
                        } else if ("org/hibernate/usertype/CompositeUserType".equals(interfaceName)) {
                            implementsCompositeUserType = true;
                        } else if ("org/hibernate/usertype/UserCollectionType".equals(interfaceName)) {
                            implementsUserCollectionType = true;
                        }


                    }
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                // TODO: cover checking for classes that implement more than one of the (checked) ORM interfaces.
                // Step 1:  check if the method name matches one of the target methods.
                // Step 2:  replace all Lorg/hibernate/engine/spi/SessionImplementor with Lorg/hibernate/engine/spi/SharedSessionContractImplementor
                // Step 3:  save the parameter number of the replaced SessionImplementor and pass to MethodAdapter
                // Step 4:  update MethodAdapter to generate a local variable that is assigned the specified parameter SharedSessionContractImplementor,
                //          casted to SessionImplementor.
                // Step 5:  replace all parameter references to the SharedSessionContractImplementor, with the new local variable.

                // change SessionImplementor parameter to SharedSessionContractImplementor
                MethodParameterCast methodParameterCast = null;
                if (implementsUserType || implementsCompositeUserType) { // nullSafeGet/nullSafeSet methods are used in a few different classes
                    if (name.equals("nullSafeGet") &&
                            "(Ljava/sql/ResultSet;[Ljava/lang/String;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
                        desc = "(Ljava/sql/ResultSet;[Ljava/lang/String;Lorg/hibernate/engine/spi/SharedSessionContractImplementor;Ljava/lang/Object;)Ljava/lang/Object;";
                        methodParameterCast = new MethodParameterCast(2, "org/hibernate/engine/spi/SessionImplementor");
                    } else if (name.equals("nullSafeSet") &&
                            "(Ljava/sql/PreparedStatement;Ljava/lang/Object;ILorg/hibernate/engine/spi/SessionImplementor;)V".equals(desc)) {
                        desc = "(Ljava/sql/PreparedStatement;Ljava/lang/Object;ILorg/hibernate/engine/spi/SharedSessionContractImplementor;)V";
                        methodParameterCast = new MethodParameterCast(2, "org/hibernate/engine/spi/SessionImplementor");
                    }
                }
                if (implementsCompositeUserType) {
                    if (name.equals("assemble") &&
                            "(Ljava/io/Serializable;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
                        desc = "(Ljava/io/Serializable;Lorg/hibernate/engine/spi/SharedSessionContractImplementor;Ljava/lang/Object;)Ljava/lang/Object;";
                        methodParameterCast = new MethodParameterCast(1, "org/hibernate/engine/spi/SessionImplementor");
                    } else if (name.equals("disassemble") &&
                            "(Ljava/lang/Object;Lorg/hibernate/engine/spi/SessionImplementor;)Ljava/io/Serializable;".equals(desc)) {
                        desc = "(Ljava/lang/Object;Lorg/hibernate/engine/spi/SharedSessionContractImplementor;)Ljava/io/Serializable;";
                        methodParameterCast = new MethodParameterCast(1, "org/hibernate/engine/spi/SessionImplementor");
                    }
                    else if (name.equals("replace") &&
                            "(Ljava/lang/Object;Ljava/lang/Object;Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Ljava/lang/Object;".equals(desc)) {
                        desc = "(Ljava/lang/Object;Ljava/lang/Object;Lorg/hibernate/engine/spi/SharedSessionContractImplementor;Ljava/lang/Object;)Ljava/lang/Object;";
                        methodParameterCast = new MethodParameterCast(2, "org/hibernate/engine/spi/SessionImplementor");
                    }
                }
                if (implementsUserCollectionType) {
                    if (name.equals("instantiate") &&
                            "(Lorg/hibernate/engine/spi/SessionImplementor;Lorg/hibernate/persister/collection/CollectionPersister;)Lorg/hibernate/collection/spi/PersistentCollection;".equals(desc)) {
                        desc = "(Lorg/hibernate/engine/spi/SharedSessionContractImplementor;Lorg/hibernate/persister/collection/CollectionPersister;)Lorg/hibernate/collection/spi/PersistentCollection;";
                        methodParameterCast = new MethodParameterCast(0, "org/hibernate/engine/spi/SessionImplementor");
                    } else if (name.equals("replaceElements") &&
                            "(Ljava/lang/Object;Ljava/lang/Object;Lorg/hibernate/persister/collection/CollectionPersister;Ljava/lang/Object;Ljava/util/Map;Lorg/hibernate/engine/spi/SessionImplementor;)Ljava/lang/Object;".equals(desc)) {
                        desc = "(Ljava/lang/Object;Ljava/lang/Object;Lorg/hibernate/persister/collection/CollectionPersister;Ljava/lang/Object;Ljava/util/Map;Lorg/hibernate/engine/spi/SharedSessionContractImplementor;)Ljava/lang/Object;";
                        methodParameterCast = new MethodParameterCast(5, "org/hibernate/engine/spi/SessionImplementor");
                    } else if (name.equals("wrap") &&
                            "(Lorg/hibernate/engine/spi/SessionImplementor;Ljava/lang/Object;)Lorg/hibernate/collection/spi/PersistentCollection;".equals(desc)) {
                        desc = "(Lorg/hibernate/engine/spi/SharedSessionContractImplementor;Ljava/lang/Object;)Lorg/hibernate/collection/spi/PersistentCollection;";
                        methodParameterCast = new MethodParameterCast(0, "org/hibernate/engine/spi/SessionImplementor");
                    }

                }

                // TODO: UserVersionType
                // TODO: org.hibernate.type.Type
                // TODO: org.hibernate.type.SingleColumnType
                // TODO: org.hibernate.type.AbstractStandardBasicType
                // TODO: org.hibernate.type.VersionType
                // TODO: org.hibernate.type.ProcedureParameterExtractionAware
                // TODO: org.hibernate.type.ProcedureParameterNamedBinder
                // TODO: org.hibernate.collection.spi.PersistentCollection
                // TODO: org.hibernate.collection.internal.AbstractPersistentCollection
                // TODO: org.hibernate.collection.internal.PersistentArrayHolder constructors
                // TODO: org.hibernate.collection.internal.PersistentBag constructors
                // TODO: org.hibernate.collection.internal.PersistentIdentifierBag
                // TODO: org.hibernate.collection.internal.PersistentList constructors
                // TODO: org.hibernate.collection.internal.PersistentMap constructors
                // TODO: org.hibernate.collection.internal.PersistentSet constructors
                // TODO: org.hibernate.collection.internal.PersistentSortedMap constructors
                // TODO: org.hibernate.collection.internal.PersistentSortedSet constructors

                return new MethodAdapter(Opcodes.ASM6, super.visitMethod(access, name, desc, signature, exceptions), loader, className, methodParameterCast);
            }
        }, 0);
        return cv.toByteArray();
    }

    private static String getModuleName(ClassLoader loader) {
        if (loader instanceof ModuleClassLoader) {
            return ((ModuleClassLoader) loader).getName();
        }
        return loader.toString();
    }

    // generates visitTypeInsn(CHECKCAST, targetClass) for specified parameterNumber and
    // also changes parameter references to use (casted) local variable.
    protected static class MethodParameterCast {
        private final int parameterNumber; // zero relative method parameter number to load that will be casted to targetClass
        private final String targetClass;  // e.g. should be in form "java/util/List"

        protected MethodParameterCast(int parameterNumber, String castParameterTo) {
            this.parameterNumber = parameterNumber;
            this.targetClass = castParameterTo;
        }
    }

    protected static class MethodAdapter extends MethodVisitor {

        private final MethodVisitor mv;
        private final ClassLoader loader;
        private final String className;

        private MethodAdapter(int api, MethodVisitor mv, final ClassLoader loader, final String className, MethodParameterCast methodParameterCast) {
            super(api, mv);
            this.mv = mv;
            this.loader = loader;
            this.className = className;
        }


        // Change call to org.hibernate.BasicQueryContract.getFlushMode() to instead call BasicQueryContract.getHibernateFlushMode().
        // Change call to org.hibernate.Session.getFlushMode, to instead call Session.getHibernateFlushMode()
        // Calls to Hibernate ORM 5.3 getFlushMode(), will not be changed as the desc will not match (desc == "()Ljavax.persistence.FlushModeType;")
        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == Opcodes.INVOKEINTERFACE &&
                    (owner.equals("org/hibernate/Session") || owner.equals("org/hibernate/BasicQueryContract"))
                    && name.equals("getFlushMode") && desc.equals("()Lorg/hibernate/FlushMode;")) {
                ROOT_LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s' is calling %s.getFlushMode, which must be changed to call getHibernateFlushMode().",
                        getModuleName(loader), className, owner);
                name = "getHibernateFlushMode";
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
            } else if (opcode == Opcodes.INVOKEINTERFACE &&
                    owner.equals("org/hibernate/Query") &&
                    name.equals("getFirstResult") &&
                    desc.equals("()Ljava/lang/Integer;")) {
                ROOT_LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s' is calling %s.%s, which must be changed to expect int result, instead of Integer.",
                        getModuleName(loader), className, name, owner);
                ROOT_LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s', is calling org.hibernate.Query.getFirstResult, which must be changed to call getHibernateFirstResult() " +
                                "so null can be returned when the value is uninitialized. Please note that if a negative value was set using " +
                                "org.hibernate.Query.setFirstResult, then getHibernateFirstResult() will return 0.",
                        getModuleName(loader), className);
                name = "getHibernateFirstResult";
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
            } else if (opcode == Opcodes.INVOKEINTERFACE &&
                    owner.equals("org/hibernate/Query") &&
                    name.equals("getMaxResults") &&
                    desc.equals("()Ljava/lang/Integer;")) {
                ROOT_LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s' is calling %s.%s, which must be changed to expect int result, instead of Integer.",
                        getModuleName(loader), className, name, owner);
                ROOT_LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s', is calling org.hibernate.Query.getMaxResults, which must be changed to call getHibernateMaxResults() " +
                                "so that null will be returned when the value is uninitialized or ORM 5.1 org.hibernate.Query#setMaxResults was " +
                                "used to set a value <= 0"
                        , getModuleName(loader), className);
                name = "getHibernateMaxResults";
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
            } else if (!disableAmbiguousChanges && opcode == Opcodes.INVOKEINTERFACE &&
                    owner.equals("org/hibernate/Query") &&
                    name.equals("setFirstResult") &&
                    desc.equals("(I)Lorg/hibernate/Query;")) {
                ROOT_LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s', is calling org.hibernate.Query.setFirstResult, which must be changed to call setHibernateFirstResult() " +
                                "so setting a value < 0 results in pagination starting with the 0th row as was done in Hibernate ORM 5.1 " +
                                "(instead of throwing IllegalArgumentException as specified by JPA)."
                        , getModuleName(loader), className);
                name = "setHibernateFirstResult";
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
            } else if (!disableAmbiguousChanges && opcode == Opcodes.INVOKEINTERFACE &&
                    owner.equals("org/hibernate/Query") &&
                    name.equals("setMaxResults") &&
                    desc.equals("(I)Lorg/hibernate/Query;")) {
                ROOT_LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s', is calling org.hibernate.Query.setMaxResults, which must be changed to call setHibernateMaxResults() " +
                                "so that values <= 0 are the same as uninitialized."
                        , getModuleName(loader), className);
                name = "setHibernateMaxResults";
                mv.visitMethodInsn(opcode, owner, name, desc, itf);
            } else

            {
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
                ROOT_LOGGER.debugf("Deprecated Hibernate51CompatibilityTransformer transformed application classes in '%s', " +
                                "class '%s' is using org.hibernate.FlushMode.NEVER, change to org.hibernate.FlushMode.MANUAL."
                        , getModuleName(loader), className);
                mv.visitFieldInsn(opcode, owner, "MANUAL", desc);
            } else {
                mv.visitFieldInsn(opcode, owner, name, desc);
            }
        }
    }
}

