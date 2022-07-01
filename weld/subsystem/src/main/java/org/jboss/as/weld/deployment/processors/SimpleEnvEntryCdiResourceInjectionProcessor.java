/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.weld.deployment.processors;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Resource;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.configurator.AnnotatedFieldConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import javax.enterprise.util.AnnotationLiteral;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.metadata.javaee.spec.EnvironmentEntriesMetaData;
import org.jboss.metadata.javaee.spec.EnvironmentEntryMetaData;
import org.jboss.metadata.javaee.spec.ResourceInjectionTargetMetaData;
import org.jboss.modules.Module;

/**
 * Processes deployment descriptor env-entry configurations looking for cases where an injection target
 * is specified and the env-entry-type is one of the 'Simple Environment Entry' Java types listed in Section 5.4
 * of the Jakarta EE 9.1 Platform specification. If any are found, adds a CDI @{link Extension} that observes CDI
 * {@link ProcessAnnotatedType} events in order to ensure that the annotated type's resource injection configuration
 * reflects any env-entry that was found. Specifically, if the class name for the annotated type matches the
 * {@code injection-target-class} of a found env-entry, and the name of a field or name of a property with a setter
 * method matches the {@code injection-target-name} of that env-entry, and the type of the field or setter parameter
 * {@link Class#isAssignableFrom(Class) is assignable from} the specified {@code env-entry-type} of that env-entry:
 * <ol>
 *     <li>
 *         If the {@code env-entry-value} for the env-entry is not specified, but the field or method is annotated
 *         with an {@code @Resource} annotation with no {@link Resource#lookup() lookup()} or
 *         {@link Resource#mappedName() mappedName()}, configured, CDI is instructed to remove the @Resource annotation,
 *         thus disabling injection, as discussed in section 5.4.1.3 of the Jakarta EE 9.1 Platform specification.
 *     </li>
 *     <li>
 *         If the {@code env-entry-value} for the env-entry is specified,, but the field or method is not annotated
 *         with an {@code @Resource} annotation, CDI is instructed to add the @Resource annotation,
 *         thus enabling injection, as discussed in section 5.4.1.3 of the Jakarta EE 9.1 Platform specification.
 *     </li>
 * </ol>
 */
public class SimpleEnvEntryCdiResourceInjectionProcessor implements DeploymentUnitProcessor {

    private static final Map<String, Class<?>> SIMPLE_ENTRY_TYPES;
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TYPES;

    static {
        Map<Class<?>, Class<?>> primitives = new HashMap<>();
        primitives.put(byte.class, Byte.class);
        primitives.put(short.class, Short.class);
        primitives.put(int.class, Integer.class);
        primitives.put(long.class, Long.class);
        primitives.put(char.class, Character.class);
        primitives.put(boolean.class, Boolean.class);
        primitives.put(float.class, Float.class);
        primitives.put(double.class, Double.class);
        PRIMITIVE_TYPES = Collections.unmodifiableMap(primitives);

        Map<String, Class<?>> types = new HashMap<>();
        store(String.class, types);
        store(Character.class, types);
        store(Byte.class, types);
        store(Short.class, types);
        store(Integer.class, types);
        store(Long.class, types);
        store(Boolean.class, types);
        store(Double.class, types);
        store(Float.class, types);
        store(Class.class, types);
        primitives.forEach((k, v) -> types.put(k.getName(), v));
        SIMPLE_ENTRY_TYPES = Collections.unmodifiableMap(types);
    }

    private static void store(Class<?> clazz, Map<String, Class<?>> map) {
        map.put(clazz.getName(), clazz);
    }

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        final CapabilityServiceSupport support = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CAPABILITY_SERVICE_SUPPORT);
        final Optional<WeldCapability> optional = support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
        if (optional.isPresent() && optional.get().isPartOfWeldDeployment(deploymentUnit)) {
            // See if any of the env-entry entries for this deployment require our CDI extension
            final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
            EnvEntryCdiExtension extension = getEnvEntryCdiExtension(deploymentUnit, module.getClassLoader());
            if (extension != null) {
                optional.get().registerExtensionInstance(extension, deploymentUnit);
                WeldLogger.DEPLOYMENT_LOGGER.debugf("Registered CDI Extension %s", extension);
            }
        }

    }

    private EnvEntryCdiExtension getEnvEntryCdiExtension(DeploymentUnit deploymentUnit, ClassLoader classLoader) {

        EnvEntryCdiExtension extension = null;

        final DeploymentDescriptorEnvironment environment = deploymentUnit.getAttachment(Attachments.MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT);
        final EnvironmentEntriesMetaData envEntries = environment == null ? null : environment.getEnvironment().getEnvironmentEntries();
        if (envEntries != null) {
            Map<String, Map<String, InjectionData>> entriesWithoutValues = new HashMap<>();
            Map<String, Map<String, InjectionData>> envEntryInjections = new HashMap<>();

            for (EnvironmentEntryMetaData eemd : envEntries) {

                Set<ResourceInjectionTargetMetaData> rimds = eemd.isDependencyIgnored() ? null : eemd.getInjectionTargets();
                if (rimds == null) {
                    continue;
                }


                Class<?> entryType = null;
                if (eemd.getType() != null) {
                    entryType = getSimpleEntryType(eemd.getType(), classLoader);
                    if (entryType == null) {
                        // A type was configured but it is not one of the specified simple env-entry types,
                        // so not relevant here.
                        continue;
                    }
                }

                for (ResourceInjectionTargetMetaData rimd : rimds) {
                    Map<String, InjectionData> toPut;
                    String value = eemd.getValue();
                    String lookup = eemd.getLookupName();
                    if ((value == null || value.isEmpty()) && (lookup == null || lookup.isEmpty())) {
                        toPut = entriesWithoutValues.computeIfAbsent(rimd.getInjectionTargetClass(), k -> new HashMap<>());
                        WeldLogger.DEPLOYMENT_LOGGER.debugf("Adding %s/%s to injection disabled map", eemd.getEnvEntryName(), entryType);
                    } else {
                        toPut = envEntryInjections.computeIfAbsent(rimd.getInjectionTargetClass(), k -> new HashMap<>());
                        WeldLogger.DEPLOYMENT_LOGGER.debugf("Adding %s/%s to injection added map", eemd.getEnvEntryName(), entryType);
                    }
                    toPut.put(rimd.getInjectionTargetName(), new InjectionData(eemd, rimd, entryType));
                }
            }

            if (!entriesWithoutValues.isEmpty() || !envEntryInjections.isEmpty()) {
                extension = new EnvEntryCdiExtension(
                        entriesWithoutValues.isEmpty() ? Collections.emptyMap() : entriesWithoutValues,
                        envEntryInjections.isEmpty() ? Collections.emptyMap() : envEntryInjections
                );
            }
        }
        return extension;
    }

    private static Class<?> getSimpleEntryType(String type, ClassLoader classLoader) {
        assert type != null;
        Class<?> result = SIMPLE_ENTRY_TYPES.get(type);
        if (result == null && !type.equals(void.class.getName())) {
            try {
                Class<?> clazz = Class.forName(type, false, classLoader);
                if (clazz.isEnum()) {
                    result = clazz;
                }
            } catch (ClassNotFoundException e) {
                throw WeldLogger.ROOT_LOGGER.cannotLoadClass(type, e);
            }
        }
        return result;
    }

    private static class InjectionData {
        private final EnvironmentEntryMetaData eemd;
        private final ResourceInjectionTargetMetaData rimd;
        private final Class<?> envType;

        private InjectionData(EnvironmentEntryMetaData eemd, ResourceInjectionTargetMetaData rimd, Class<?> envType) {
            this.eemd = eemd;
            this.rimd = rimd;
            this.envType = envType;
        }

        private boolean isEntryNameTargetName() {
            String concat = rimd.getInjectionTargetClass() + "/" + rimd.getInjectionTargetName();
            return concat.equals(eemd.getEnvEntryName());
        }
    }

    private static class EnvEntryCdiExtension implements Extension {

        private final Map<String, Map<String, InjectionData>> entriesWithoutValues;
        private final Map<String, Map<String, InjectionData>> envEntryInjections;

        private EnvEntryCdiExtension(Map<String, Map<String, InjectionData>> entriesWithoutValues, Map<String, Map<String, InjectionData>> envEntryInjections) {
            this.entriesWithoutValues = entriesWithoutValues;
            this.envEntryInjections = envEntryInjections;
        }

        @SuppressWarnings("unused")
        public <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> event) {
            processEntriesWithoutValues(event);
            processEnvEntryInjections(event);
        }

        /**
         * Look for fields or setters annotated with @Resource where neither mappedName nor lookup are set.
         * If found see if there was an env-entry without a value; if so remove the annotation to disable injection
         * @param event the event to process
         * @param <X> the class being annotated
         */
        private <X> void processEntriesWithoutValues(ProcessAnnotatedType<X> event) {
            AnnotatedType<X> annotatedType = event.getAnnotatedType();
            String typeName = annotatedType.getBaseType().getTypeName();
            Map<String, InjectionData> membersWithoutValues = entriesWithoutValues.get(typeName);
            if (membersWithoutValues != null) {

                AnnotatedTypeConfigurator<X> typeConfigurator = event.configureAnnotatedType();

                Set<AnnotatedFieldConfigurator<? super X>> annotatedFields = typeConfigurator.fields();
                for (AnnotatedFieldConfigurator<? super X> annotatedFieldCfg : annotatedFields) {
                    AnnotatedField<? super X> annotatedField = annotatedFieldCfg.getAnnotated();
                    Field field = annotatedField.getJavaMember();
                    InjectionData injectionData = membersWithoutValues.get(field.getName());
                    // Remove the annotation if it exists, an env-entry does too and they match
                    annotatedFieldCfg.remove(a -> testNoValueAnnotation(a, injectionData, field.getType()));
                }

                Set<AnnotatedMethodConfigurator<? super X>> annotatedMethods = typeConfigurator.methods();
                for (AnnotatedMethodConfigurator<? super X> annotatedMethodCfg : annotatedMethods) {
                    AnnotatedMethod<? super X> annotatedMethod = annotatedMethodCfg.getAnnotated();
                    Method method = annotatedMethod.getJavaMember();
                    Class<?>[] params = method.getParameterTypes();
                    if (params.length == 1) {
                        String asField = methodNameAsField(method);
                        if (asField != null) {
                            InjectionData injectionData  = membersWithoutValues.get(asField);
                            // Remove the annotation if it exists, an env-entry does too and they match
                            annotatedMethodCfg.remove(a -> testNoValueAnnotation(a, injectionData, params[0]));
                        }
                    }
                }
            } else {
                WeldLogger.DEPLOYMENT_LOGGER.tracef("%s is not in the injection disabled map", typeName);
            }
        }

        /**
         * Look for fields or setters not annotated with @Resource whose name matches an env-entry injection target.
         * If found, add the annotation so injection will be performed
         * @param event the event to process
         * @param <X>  the class being annotated
         */
        private <X> void processEnvEntryInjections(ProcessAnnotatedType<X> event) {
            AnnotatedType<X> annotatedType = event.getAnnotatedType();
            String typeName = annotatedType.getBaseType().getTypeName();
            Map<String, InjectionData> envEntryInjection = envEntryInjections.get(typeName);
            if (envEntryInjection != null) {

                AnnotatedTypeConfigurator<X> typeConfigurator = event.configureAnnotatedType();

                Set<AnnotatedFieldConfigurator<? super X>> annotatedFields = typeConfigurator.fields();
                for (AnnotatedFieldConfigurator<? super X> annotatedFieldCfg : annotatedFields) {
                    AnnotatedField<? super X> annotatedField = annotatedFieldCfg.getAnnotated();
                    if (annotatedField.getAnnotations(Resource.class).isEmpty()) {
                        Field field = annotatedField.getJavaMember();
                        InjectionData injectionData  = envEntryInjection.get(field.getName());
                        if (injectionData != null && isMatchingType(field.getType(), injectionData.envType)) {
                            annotatedFieldCfg.add(new UnmappedResourceLiteral(injectionData));
                            WeldLogger.DEPLOYMENT_LOGGER.debugf("Added injection into %s", field);
                        } else if (injectionData != null && injectionData.envType != null) {
                             WeldLogger.DEPLOYMENT_LOGGER.debugf("Entry type %s cannot be assigned to %s", injectionData.envType, field);
                        }
                    }
                }

                Set<AnnotatedMethodConfigurator<? super X>> annotatedMethods = typeConfigurator.methods();
                for (AnnotatedMethodConfigurator<? super X> annotatedMethodCfg : annotatedMethods) {
                    AnnotatedMethod<? super X> annotatedMethod = annotatedMethodCfg.getAnnotated();
                    if (annotatedMethod.getAnnotations(Resource.class).isEmpty()) {
                        Method method = annotatedMethod.getJavaMember();
                        Class<?>[] params = method.getParameterTypes();
                        if (params.length == 1) {
                            String asField = methodNameAsField(method);
                            if (asField != null) {
                                InjectionData injectionData = envEntryInjection.get(asField);
                                if (injectionData != null && isMatchingType(params[0], injectionData.envType)) {
                                    annotatedMethodCfg.add(new UnmappedResourceLiteral(injectionData));
                                    WeldLogger.DEPLOYMENT_LOGGER.debugf("Added injection into %s", method);
                                } else if (injectionData != null && injectionData.envType != null) {
                                    WeldLogger.DEPLOYMENT_LOGGER.debugf("Entry type %s cannot be passed to %s", injectionData.envType, method);
                                }
                            }
                        }
                    }
                }
            } else {
                WeldLogger.DEPLOYMENT_LOGGER.tracef("%s is not in the env-entry injection map", typeName);
            }
        }
    }

    private static String methodNameAsField(Method method) {
        String result = null;
        String methodName = method.getName();
        if (methodName.startsWith("set") && methodName.length() > 3) {
            String withoutSet = methodName.substring(3);
            char initial = Character.toLowerCase(withoutSet.charAt(0));
            result = withoutSet.length() > 1 ? initial + withoutSet.substring(1) : String.valueOf(initial);
        }
        return result;
    }

    private static boolean isMatchingType(Class<?> memberType, Class<?> entryType) {
        assert memberType != null;
        // If no env-entry-type was provided, just confirm the target member's type is a legal simple env entry target.
        // If it was provided, confirm it is compatible with the target member's type
        return (entryType == null && (SIMPLE_ENTRY_TYPES.containsKey(memberType.getName()) || memberType.isEnum()))
            || (entryType != null && (memberType.isAssignableFrom(entryType) || entryType.equals(PRIMITIVE_TYPES.get(memberType))));
    }

    private static boolean testNoValueAnnotation(Annotation a, InjectionData injectionData, Class<?> targetType) {
        return injectionData != null && a instanceof Resource && testNoValueResource((Resource) a, injectionData, targetType);
    }

    private static boolean testNoValueResource(Resource resource, InjectionData injectionData, Class<?> targetType) {
        boolean result = injectionData != null
                && (resource.lookup().isEmpty() && resource.mappedName().isEmpty())
                && isMatchingType(targetType, injectionData.envType)
                && (resource.name().equals(injectionData.eemd.getEnvEntryName())
                    || (resource.name().isEmpty() && injectionData.isEntryNameTargetName()));
        WeldLogger.DEPLOYMENT_LOGGER.debugf("Disable injection into %s? %s", resource, result);
        return result;
    }

    private static class UnmappedResourceLiteral extends AnnotationLiteral<Resource> implements Resource {

        private static final long serialVersionUID = 1L;

        private final InjectionData injectionData;

        private UnmappedResourceLiteral(InjectionData injectionData) {
            this.injectionData = injectionData;
        }

        @Override
        public String name() {
            String name = injectionData.eemd.getEnvEntryName();
            return name == null ? "" : name;
        }

        @Override
        public String lookup() {
            String lookup = injectionData.eemd.getLookupName();
            return lookup == null ? "" : lookup;
        }

        @Override
        public Class<?> type() {
            Class<?> type = injectionData.envType;
            return type == null ? Object.class : type;
        }

        @Override
        public AuthenticationType authenticationType() {
            return AuthenticationType.CONTAINER;
        }

        @Override
        public boolean shareable() {
            return true;
        }

        @Override
        public String mappedName() {
            return "";
        }

        @Override
        public String description() {
            return "";
        }
    }
}
