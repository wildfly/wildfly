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

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A class file transformer which makes deployment classes written for Hibernate 5.1 be compatible with Hibernate 5.3.
 *
 * @deprecated
 */
public class Hibernate51CompatibilityTransformer implements ClassFileTransformer {

    private static final Hibernate51CompatibilityTransformer instance = new Hibernate51CompatibilityTransformer();
    private static final File showTransformedClassFolder;
    public static final BasicLogger logger = Logger.getLogger("org.jboss.as.hibernate.transformer");

    static {
        String folderName = WildFlySecurityManager.getPropertyPrivileged("Hibernate51CompatibilityTransformer.showTransformedClassFolder", null);
        if (folderName != null) {
            showTransformedClassFolder = new File(folderName);
        } else {
            showTransformedClassFolder = null;
        }
    }

    private static final boolean useASM7 = getMajorJavaVersion() >= 11;

    private static final String markerAlreadyTransformed = "$_org_jboss_as_hibernate_Hibernate51CompatibilityTransformer_transformed_$";

    public static Hibernate51CompatibilityTransformer getInstance() {
        return instance;
    }

    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain, final byte[] classfileBuffer) {

        // ignore these non-application classes
        if(className.startsWith("org/jboss/arquillian") ||
           className.startsWith("org/jboss/as/arquillian") ||
           className.startsWith("org/junit") ||
           className.startsWith("org/jboss/shrinkwrap")) {
            logger.debugf("Hibernate51CompatibilityTransformer ignoring class '%s' from '%s'", className, getModuleName(loader));
            return null;
        }
        logger.debugf("Hibernate51CompatibilityTransformer transforming deployment class '%s' from '%s'", className, getModuleName(loader));

        final Set<String> parentClassesAndInterfaces = new HashSet<>();

        collectClassesAndInterfaces(parentClassesAndInterfaces, loader, className);
        logger.tracef("Class %s extends or implements %s", className, parentClassesAndInterfaces);

        final boolean rewriteSessionImplementor = parentExpectsSharedSessionContractImplementor(parentClassesAndInterfaces);

        final TransformedState transformedState = new TransformedState();
        final ClassReader classReader = new ClassReader(classfileBuffer);

        final ClassWriter classWriter = new ClassWriter(classReader, 0) {

            // Pass the classloader in so org.objectweb.asm.ClassWriter.getCommonSuperClass() uses the app classloader
            // instead of ASM classloader, for loading super classes.
            @Override
            protected ClassLoader getClassLoader() {
                return loader;
            }

        };

        PrintWriter traceAfterPrintWriter = null;

        try {
            classReader.accept(new ClassVisitor(useASM7 ? Opcodes.ASM7 : Opcodes.ASM6, classWriter) {

                // clear transformed state at start of each class visit
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    // clear per class state
                    transformedState.clear();
                    super.visit(version, access, name, signature, superName, interfaces);
                }

                // check if class has already been transformed
                @Override
                public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                    // check if class has already been modified
                    if (markerAlreadyTransformed.equals(name) &&
                            desc.equals("Z")) {
                        transformedState.setAlreadyTransformed(true);
                    }
                    return super.visitField(access, name, desc, signature, value);
                }

                // mark class as transformed (only if class transformations were made)
                @Override
                public void visitEnd() {
                    if (transformedState.transformationsMade()) {
                        cv.visitField(ACC_PUBLIC + ACC_STATIC, markerAlreadyTransformed, "Z", null, null).visitEnd();
                    }
                    super.visitEnd();
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

                    // Handle changing SessionImplementor parameter to SharedSessionContractImplementor
                    final String descOrig = desc;

                    logger.tracef("method %s, description %s, signature %s", name, desc, signature);

                    // If application class has certain checked Hibernate ORM classes as super class,
                    // replace method parameters of type 'SessionImplementor', to instead use type 'SharedSessionContractImplementor'.
                    if (rewriteSessionImplementor && desc.contains("org/hibernate/engine/spi/SessionImplementor")) {
                        desc = replaceSessionImplementor(desc);
                    }
                    if (descOrig != desc) {  // if we are changing from type SessionImplementor to SharedSessionContractImplementor
                        // mark the class as transformed
                        transformedState.setClassTransformed(true);
                    }
                    return new MethodAdapter(rewriteSessionImplementor, Opcodes.ASM6, super.visitMethod(access, name, desc,
                            signature, exceptions), getModuleName(loader), className, transformedState);
                }
            }, 0);
            if (!transformedState.transformationsMade()) {
                // no change was made, indicate so by returning null
                return null;
            }

            byte[] result = classWriter.toByteArray();

            try {
                if (showTransformedClassFolder != null) {
                    // generate .asm file that shows the transformed class
                    ClassReader cr = new ClassReader(result);
                    traceAfterPrintWriter =
                        new PrintWriter(new File(showTransformedClassFolder, className.replace('/', '_') + ".asm"));
                    ClassVisitor tv = new TraceClassVisitor(traceAfterPrintWriter);
                    cr.accept(tv, 0);
                }
            } catch (IOException ignored) {

            }
            return result;
        } finally {
            if (traceAfterPrintWriter != null) {
                traceAfterPrintWriter.close();
            }
        }
    }

    private static String getModuleName(ClassLoader loader) {
        if (loader == null) {
            return "(null)";
        }
        if (loader instanceof ModuleClassLoader) {
            return ((ModuleClassLoader) loader).getName();
        }
        return loader.toString();
    }

    private static String replaceSessionImplementor(String desc) {
        return desc.replace("Lorg/hibernate/engine/spi/SessionImplementor;",
                "Lorg/hibernate/engine/spi/SharedSessionContractImplementor;");
    }

    private boolean parentExpectsSharedSessionContractImplementor(Set parentClassesAndInterfaces) {
        return (parentClassesAndInterfaces.contains("org/hibernate/usertype/UserType")
                || parentClassesAndInterfaces.contains("org/hibernate/usertype/CompositeUserType")
                || parentClassesAndInterfaces.contains("org/hibernate/usertype/UserCollectionType")
                || parentClassesAndInterfaces.contains("org/hibernate/usertype/UserVersionType")
                || parentClassesAndInterfaces.contains("org/hibernate/type/Type")
                || parentClassesAndInterfaces.contains("org/hibernate/type/SingleColumnType")
                || parentClassesAndInterfaces.contains("org/hibernate/type/AbstractStandardBasicType")
                || parentClassesAndInterfaces.contains("org/hibernate/type/ProcedureParameterExtractionAware")
                || parentClassesAndInterfaces.contains("org/hibernate/type/ProcedureParameterNamedBinder")
                || parentClassesAndInterfaces.contains("org/hibernate/type/VersionType")
                || parentClassesAndInterfaces.contains("org/hibernate/collection/spi/PersistentCollection"));
    }

    private void collectClassesAndInterfaces(Set<String> classesAndInterfaces, ClassLoader classLoader, String className) {
        if (className == null || "java/lang/Object".equals(className) || classLoader == null) {
            return;
        }


        try (InputStream is = classLoader.getResourceAsStream(className.replace('.', '/') + ".class")) {
            ClassReader classReader = new ClassReader(is);
            classReader.accept(new ClassVisitor(useASM7 ? Opcodes.ASM7 : Opcodes.ASM6) {

                @Override
                public void visit(int version, int access, String name, String signature,
                                  String superName, String[] interfaces) {
                    if (interfaces != null) {
                        for (String interfaceName : interfaces) {
                            classesAndInterfaces.add(interfaceName);
                            collectClassesAndInterfaces(classesAndInterfaces, classLoader, interfaceName);
                        }
                    }

                    classesAndInterfaces.add(superName);
                    collectClassesAndInterfaces(classesAndInterfaces, classLoader, superName);
                }

                @Override
                public void visitInnerClass(String name, String outerName, String innerName, int access) {
                    if (innerName != null) {
                        classesAndInterfaces.add(innerName);
                    }
                }

            }, 0);
        } catch (IOException e) {
            logger.debugf("Unable to open class file %s, due to exception %s", className, e);
        }
    }

    private static int getMajorJavaVersion() {
        int major = 8;
        String version = WildFlySecurityManager.getPropertyPrivileged("java.specification.version", null);
        if (version != null) {
            Matcher matcher = Pattern.compile("^(?:1\\.)?(\\d+)$").matcher(version);
            if (matcher.find()) {
                major = Integer.valueOf(matcher.group(1));
            }
        }
        return major;
    }

}

