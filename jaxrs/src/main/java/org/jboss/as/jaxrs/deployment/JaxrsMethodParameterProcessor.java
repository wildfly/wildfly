package org.jboss.as.jaxrs.deployment;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.jaxrs.JaxrsAnnotations;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Indexer;
import org.jboss.modules.Module;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.as.jaxrs.logging.JaxrsLogger.JAXRS_LOGGER;

/**
 * This class addresses the spec requirement of pre-processing resource
 * method parameters with DefaultValue annotations at application deployment
 * time.
 * (section 3.2 of the Jakarta RESTful Web Services 2.1 specification)
 */
public class JaxrsMethodParameterProcessor implements DeploymentUnitProcessor {
    private final DotName PARAM_CONVERTER_PROVIDER_DOTNAME =
            DotName.createSimple("javax.ws.rs.ext.ParamConverterProvider");
    private final DotName PARAM_CONVERTER_DOTNAME =
            DotName.createSimple("javax.ws.rs.ext.ParamConverter");
    private final DotName PARAM_CONVERTER_LAZY_DOTNAME =
            DotName.createSimple("javax.ws.rs.ext.ParamConverter$Lazy");
    private final DotName DEFAULT_VALUE_DOTNAME =
            DotName.createSimple("javax.ws.rs.DefaultValue");
    @Override
    public void deploy(DeploymentPhaseContext phaseContext)
            throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!JaxrsDeploymentMarker.isJaxrsDeployment(deploymentUnit)) {
            return;
        }

        if (!DeploymentTypeMarker.isType(DeploymentType.WAR, deploymentUnit)) {
            return;
        }

        final ResteasyDeploymentData resteasy = deploymentUnit.getAttachment(
                JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA);
        if (resteasy == null) {
            return;
        }
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final CompositeIndex index = deploymentUnit.getAttachment(
                Attachments.COMPOSITE_ANNOTATION_INDEX);

        processData(index, module.getClassLoader(), resteasy, false);
    }

    /**
     *
     * @param index
     * @param classLoader
     * @param resteasy
     * @param isFromUnitTest
     * @throws DeploymentUnitProcessingException
     */
    private void processData(final CompositeIndex index, final ClassLoader classLoader,
                             ResteasyDeploymentData resteasy, boolean isFromUnitTest)
            throws DeploymentUnitProcessingException {

        List<ParamDetail> detailList = getResouceClasses(index, classLoader,
                resteasy.getScannedResourceClasses(), isFromUnitTest);

        if (!detailList.isEmpty()) {
            HashMap<String, List<Validator>> paramConverterMap =
                    getParamConverters(index, classLoader,
                            resteasy.getScannedProviderClasses(), isFromUnitTest);

            validateDefaultValues(detailList, paramConverterMap);
        }

    }

    /**
     * Process all parameter DefaulValue objects.  Flag all parameters with
     * missing and invalid converters.
     *
     * @param detailList
     * @param paramConverterMap
     */
    private void validateDefaultValues(List<ParamDetail> detailList,
                                       HashMap<String, List<Validator>> paramConverterMap)
            throws DeploymentUnitProcessingException {

        for(ParamDetail detail : detailList) {

            // check param converter for specific return type
            List<Validator> validators = paramConverterMap.get(
                    detail.parameter.getName());

            if (validators == null) {
                // check for paramConverterProvider
                validators = paramConverterMap.get(Object.class.getName());
            }

            boolean isCheckClazzMethods = true;
            if (validators != null) {
                for (Validator v : validators) {
                    if (!v.isLazyLoad()) {
                        try {
                            Object obj = v.verify(detail);
                            if (obj != null) {
                                isCheckClazzMethods = false;
                                break;
                            }
                        } catch (Exception e) {
                            JAXRS_LOGGER.paramConverterFailed(detail.defaultValue.value(),
                                    detail.parameter.getSimpleName(),
                                    detail.method.toString(),
                                    v.toString(), e.getClass().getName(),
                                    e.getMessage());
                        }
                    }
                }
            }

            if (isCheckClazzMethods) {
                Class baseType = detail.parameter;
                Method valueOf = null;

                // constructor rule
                try {
                    Constructor<?> ctor = baseType.getConstructor(String.class);
                    if (Modifier.isPublic(ctor.getModifiers())) {
                        continue; // success move to next detail
                    }
                } catch (NoSuchMethodException ignored) { }

                // method fromValue(String.class) rule
                try {
                    Method fromValue = baseType.getDeclaredMethod("fromValue", String.class);
                    if (Modifier.isPublic(fromValue.getModifiers())) {
                        for (Annotation ann : baseType.getAnnotations()) {
                            if (ann.annotationType().getName()
                                    .equals("javax.xml.bind.annotation.XmlEnum")) {
                                valueOf = fromValue;
                            }
                        }
                        validateBaseType(fromValue, detail.defaultValue.value(), detail);
                        continue; // success move to next detail
                    }
                } catch (NoSuchMethodException ignoredA) { }

                // method fromString(String.class) rule
                Method fromString = null;
                try {
                    fromString = baseType.getDeclaredMethod("fromString", String.class);
                    if (Modifier.isStatic(fromString.getModifiers())) {
                        validateBaseType(fromString, detail.defaultValue.value(), detail);
                        continue; // success move to next detail
                    }
                } catch (NoSuchMethodException ignoredB) {
                }

                // method valueof(String.class) rule
                try {
                    valueOf = baseType.getDeclaredMethod("valueOf", String.class);
                    if (Modifier.isStatic(valueOf.getModifiers())) {
                        validateBaseType(valueOf, detail.defaultValue.value(), detail);
                        continue; // success move to next detail
                    }
                } catch (NoSuchMethodException ignored) {
                }

            }
        }

    }

    /**
     * Create a list of ParamConverters and ParamConverterProviders present
     * in the application.
     *
     * When running unitTest the classes must be indexed.  In normal deployment
     * the indexing is already done.
     *
     * @param index
     * @param classLoader
     * @return
     */
    private HashMap<String, List<Validator>>  getParamConverters(
            final CompositeIndex index, final ClassLoader classLoader,
            Set<String> knownProviderClasses, boolean isFromUnitTest) {

        HashMap<String, List<Validator>> paramConverterMap = new HashMap<>();
        List<Validator> converterProviderList = new ArrayList<>();
        paramConverterMap.put(Object.class.getName(), converterProviderList);

        Set<ClassInfo> paramConverterSet = new HashSet<ClassInfo>();
        if(isFromUnitTest) {
            Indexer indexer = new Indexer();
            for (String className : knownProviderClasses) {
                try {
                    String pathName = className.replace(".", File.separator);
                    InputStream stream = classLoader.getResourceAsStream(
                            pathName + ".class");
                    indexer.index(stream);
                    stream.close();
                } catch (IOException e) {
                    JAXRS_LOGGER.classIntrospectionFailure(e.getClass().getName(),
                            e.getMessage());
                }
            }

            List<ClassInfo> paramConverterList =
                    indexer.complete().getKnownDirectImplementors(PARAM_CONVERTER_DOTNAME);
            List<ClassInfo> paramConverterProviderList =
                    indexer.complete().getKnownDirectImplementors(PARAM_CONVERTER_PROVIDER_DOTNAME);
            paramConverterSet.addAll(paramConverterList);
            paramConverterSet.addAll(paramConverterProviderList);

        } else {

            for (String clazzName : knownProviderClasses) {
                ClassInfo classInfo = index.getClassByName(DotName.createSimple(clazzName));
                if (classInfo != null) {
                    List<DotName> intfNamesList = classInfo.interfaceNames();
                    for (DotName dotName : intfNamesList) {
                        if (dotName.compareTo(PARAM_CONVERTER_DOTNAME) == 0
                                || dotName.compareTo(PARAM_CONVERTER_PROVIDER_DOTNAME) == 0) {
                            paramConverterSet.add(classInfo);
                            break;
                        }
                    }
                }
            }
        }

        for (ClassInfo classInfo : paramConverterSet) {

            Class<?> clazz = null;
            Method method = null;
            try {
                String clazzName = classInfo.name().toString();
                if (clazzName.endsWith("$1")) {
                    clazzName = clazzName.substring(0, clazzName.length()-2);
                }

                clazz = classLoader.loadClass(clazzName);
                Constructor<?> ctor = clazz.getConstructor();
                Object object = ctor.newInstance();

                List<AnnotationInstance> lazyLoadAnnotations =classInfo
                        .annotations().get(PARAM_CONVERTER_LAZY_DOTNAME);

                if (object instanceof ParamConverterProvider) {
                    ParamConverterProvider pcpObj = (ParamConverterProvider) object;
                    method = pcpObj.getClass().getMethod(
                            "getConverter",
                            Class.class,
                            Type.class,
                            Annotation[].class);
                    converterProviderList.add(new ConverterProvider(pcpObj, method, lazyLoadAnnotations));
                }

                if (object instanceof ParamConverter) {
                    ParamConverter pc = (ParamConverter) object;
                    method = getFromStringMethod(pc.getClass());
                    Class<?> returnClazz = method.getReturnType();
                    List<Validator> verifiers = paramConverterMap.get(returnClazz.getName());
                    PConverter pConverter = new PConverter(pc, method, lazyLoadAnnotations);
                    if (verifiers == null){
                        List<Validator> vList = new ArrayList<>();
                        vList.add(pConverter);
                        paramConverterMap.put(returnClazz.getName(), vList);
                    } else {
                        verifiers.add(pConverter);
                    }
                }

            } catch(NoSuchMethodException nsne) {
                JAXRS_LOGGER.classIntrospectionFailure(nsne.getClass().getName(),
                        nsne.getMessage());
            } catch (Exception e) {
                JAXRS_LOGGER.classIntrospectionFailure(e.getClass().getName(),
                        e.getMessage());
            }
        }

        return paramConverterMap;
    }

    private Method getFromStringMethod(final Class clazz) {
        Method method = null;
        try {
            method = clazz.getMethod("fromString", String.class);
        }catch(NoSuchMethodException nsme) {
            JAXRS_LOGGER.classIntrospectionFailure(nsme.getClass().getName(),
                    nsme.getMessage());
        }
        return method;
    }

    /**
     * Create list of objects that represents resource method parameters with a
     * DefaultValue annontation assigned to it.
     *
     * When running unitTest the classes must be indexed.  In normal deployment
     * the indexing is already done.
     *
     * @param index
     * @param classLoader
     * @return
     */
    private ArrayList<ParamDetail> getResouceClasses(final CompositeIndex index,
                                   final ClassLoader classLoader,
                                   Set<String> knownResourceClasses,
                                   boolean isFromUnitTest) {

        ArrayList<ParamDetail> detailList = new ArrayList<>();
        ArrayList<String> classNameArr = new ArrayList<>();

        if (isFromUnitTest) {
            Indexer indexer = new Indexer();
            for (String className : knownResourceClasses) {
                try {
                    String pathName = className.replace(".", File.separator);
                    InputStream stream = classLoader.getResourceAsStream(pathName + ".class");
                    ClassInfo classInfo = indexer.index(stream);

                    List<AnnotationInstance> defaultValuesList =
                            classInfo.annotations().get(DEFAULT_VALUE_DOTNAME);

                    if (!defaultValuesList.isEmpty()) {
                        classNameArr.add((classInfo).name().toString());
                    }

                    stream.close();

                } catch (IOException e) {
                    JAXRS_LOGGER.classIntrospectionFailure(e.getClass().getName(),
                            e.getMessage());
                }
            }
        } else {

            for (String clazzName : knownResourceClasses) {

                ClassInfo classInfo = index.getClassByName(DotName.createSimple(clazzName));
                if (classInfo != null) {
                    Map<DotName, List<AnnotationInstance>> annotationsMap =
                            classInfo.annotations();

                    if (annotationsMap != null && !annotationsMap.isEmpty()) {
                        List<AnnotationInstance> xInstance = annotationsMap.get(
                                JaxrsAnnotations.PATH.getDotName());
                        List<AnnotationInstance> xdefaultValuesList =
                                annotationsMap.get(DEFAULT_VALUE_DOTNAME);
                        if ((xInstance != null && !xInstance.isEmpty()) &&
                             (xdefaultValuesList != null && !xdefaultValuesList.isEmpty())) {
                            classNameArr.add((classInfo).name().toString());
                        }
                    }
                }
            }
        }

        // resource classes with @DefaultValue
        // find methods and method params with @DefaultValue
        for (String className : classNameArr) {
            Class<?> clazz = null;
            try {
                clazz = classLoader.loadClass(className);
                for (Method method : clazz.getMethods()) {
                    if (clazz == method.getDeclaringClass()) {
                        Type[] genParamTypeArr = method.getGenericParameterTypes();
                        Annotation[][] annotationMatrix = method.getParameterAnnotations();

                        for (int j = 0; j < genParamTypeArr.length; j++) {
                            DefaultValue defaultValue = lookupDefaultValueAnn(annotationMatrix[j]);

                            if (defaultValue != null) {
                                Class paramClazz = checkParamType(genParamTypeArr[j],
                                        method, j, classLoader);

                                if (paramClazz != null) {
                                    detailList.add(new ParamDetail(method,
                                            defaultValue, paramClazz, annotationMatrix[j]));
                                }
                            }
                        }
                    }
                }

            } catch (ClassNotFoundException e) {
                JAXRS_LOGGER.classIntrospectionFailure(e.getClass().getName(),
                        e.getMessage());
            }
        }
        return detailList;
    }

    /**
     * Take steps to properly identify the parameter's data type
     * @param genParamType
     * @param method
     * @param paramPos
     * @param classLoader
     * @return
     */
    private Class checkParamType(Type genParamType, final Method method,
                                 final int paramPos, final ClassLoader classLoader){

        Class paramClazz = null;

        if (genParamType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genParamType;
            Type[] actualTypeArgs = pType.getActualTypeArguments();
            // skip Map types. Don't know how to set default value for these
            if (actualTypeArgs.length == 1) {
                try {
                    paramClazz = classLoader.loadClass(actualTypeArgs[0].getTypeName());
                } catch (Exception ee) {
                    JAXRS_LOGGER.classIntrospectionFailure(ee.getClass().getName(),
                            ee.getMessage());
                }
            }
        } else {
            Class<?>[] paramArr = method.getParameterTypes();
            if (paramArr[paramPos].isArray()) {
                Class compClazz = paramArr[paramPos].getComponentType();
                if (!compClazz.isPrimitive()) {
                    paramClazz = compClazz;
                }
            } else {
                if (!paramArr[paramPos].isPrimitive()) {
                    paramClazz = paramArr[paramPos];
                }
            }
        }
        return paramClazz;
    }

    /**
     * Extract a DefaultValue annotation from the list of parameter annotations
     * @param annotationArr
     * @return
     */
    private DefaultValue lookupDefaultValueAnn(Annotation[] annotationArr) {
        for (Annotation ann :  annotationArr) {

            if (ann instanceof DefaultValue) {
                return (DefaultValue)ann;
            }
        }
        return null;
    }

    /**
     * Data structure for passing around related parameter information
     */
    private class ParamDetail {
        public Method method;
        public DefaultValue defaultValue;
        public Class parameter;
        public Annotation[] annotations;

        public ParamDetail(Method method, DefaultValue defaultValue, Class parameter,
                           Annotation[] annotations) {
            this.method = method;
            this.defaultValue = defaultValue;
            this.parameter = parameter;
            this.annotations = annotations;
        }
    }

    private interface Validator {
      public Object verify(ParamDetail detail) throws Exception;
      public boolean isLazyLoad();
    }

    /**
     * ParamConverterProvider's getConverter method used for validation
     */
    private class ConverterProvider implements Validator {
        private ParamConverterProvider pcp;
        private ParamConverter pc = null;
        private Method method;
        private boolean isLazyLoad = false;

        public ConverterProvider(ParamConverterProvider pcp, Method method,
                                 List<AnnotationInstance> lazyAnnotations) {
           this.pcp = pcp;
           this.method = method;
           if (lazyAnnotations != null && !lazyAnnotations.isEmpty()) {
               isLazyLoad = true;
           }
        }

        public boolean isLazyLoad() {
            return isLazyLoad;
        }

        public Object verify(ParamDetail detail) throws Exception {

            Object obj = method.invoke(pcp,
                    detail.parameter,
                    detail.parameter.getComponentType(),
                    detail.annotations);

            if (obj instanceof ParamConverter) {
                this.pc = (ParamConverter) obj;
                return pc.fromString(detail.defaultValue.value());
            }

            return obj;
        }

        @Override
        public String toString() {
            if (pc == null) {
                return pcp.getClass().getName();
            }
            return pc.getClass().getName();
        }
    }

    /**
     * ParamConverter's method fromString  used for validation
     */
    private class PConverter implements Validator {
        private ParamConverter pc;
        private Method method;
        private boolean isLazyLoad = false;

        public PConverter(ParamConverter pc, Method method,
                          List<AnnotationInstance> lazyAnnotations) {
            this.pc = pc;
            this.method = method;
            if (lazyAnnotations != null && !lazyAnnotations.isEmpty()) {
                isLazyLoad = true;
            }
        }

        public boolean isLazyLoad() {
            return isLazyLoad;
        }

        public Object verify(ParamDetail detail) throws Exception {
                Object obj = method.invoke(pc, detail.defaultValue.value());
                return obj;
        }

        @Override
        public String toString() {
            return pc.getClass().getName();
        }
    }

    /**
     * Confirm the method can handle the default value without throwing
     * and exception.
     *
     * @param method
     * @param defaultValue
     */
    private void validateBaseType(Method method, String defaultValue, ParamDetail detail)
        throws DeploymentUnitProcessingException {
        if (defaultValue != null) {
            try {
                method.invoke(method.getDeclaringClass(), defaultValue);
            } catch (Exception e) {
                JAXRS_LOGGER.baseTypeMethodFailed(defaultValue,
                        detail.parameter.getSimpleName(), detail.method.toString(),
                        method.toString(), e.getClass().getName(),
                        e.getMessage());
            }
        }
    }

    /**
     * Method allows unit-test to provide processing data.
     * @param resteasyDeploymentData
     */
    public void testProcessor(final ClassLoader classLoader,
                              final ResteasyDeploymentData resteasyDeploymentData)
            throws DeploymentUnitProcessingException {
        processData(null, classLoader, resteasyDeploymentData, true);
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
