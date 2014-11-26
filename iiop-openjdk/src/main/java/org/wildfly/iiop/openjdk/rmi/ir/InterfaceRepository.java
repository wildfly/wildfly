/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.iiop.openjdk.rmi.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.omg.CORBA.Any;
import org.omg.CORBA.DefinitionKind;
import org.omg.CORBA.ExceptionDef;
import org.omg.CORBA.ExceptionDefHelper;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ParameterDescription;
import org.omg.CORBA.ParameterMode;
import org.omg.CORBA.Repository;
import org.omg.CORBA.RepositoryHelper;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.PortableServer.POA;
import org.wildfly.iiop.openjdk.logging.IIOPLogger;
import org.wildfly.iiop.openjdk.rmi.AttributeAnalysis;
import org.wildfly.iiop.openjdk.rmi.ConstantAnalysis;
import org.wildfly.iiop.openjdk.rmi.ContainerAnalysis;
import org.wildfly.iiop.openjdk.rmi.ExceptionAnalysis;
import org.wildfly.iiop.openjdk.rmi.InterfaceAnalysis;
import org.wildfly.iiop.openjdk.rmi.OperationAnalysis;
import org.wildfly.iiop.openjdk.rmi.ParameterAnalysis;
import org.wildfly.iiop.openjdk.rmi.RMIIIOPViolationException;
import org.wildfly.iiop.openjdk.rmi.RmiIdlUtil;
import org.wildfly.iiop.openjdk.rmi.Util;
import org.wildfly.iiop.openjdk.rmi.ValueAnalysis;
import org.wildfly.iiop.openjdk.rmi.ValueMemberAnalysis;

/**
 * An Interface Repository.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 * @version $Revision: 81018 $
 */
public class InterfaceRepository {


    /**
     * The repository implementation.
     */
    RepositoryImpl impl;

    /**
     * The ORB that I use.
     */
    private ORB orb = null;

    /**
     * The POA that I use.
     */
    private POA poa = null;

    /**
     * Maps java classes to IDL TypeCodes for parameter, result, attribute
     * and value member types.
     */
    private final Map<Class<?>, TypeCode> typeCodeMap;

    /**
     * Maps java classes to IDL TypeCodes for parameter, result, attribute
     * and value member types.
     */
    private final Map<Class<?>, TypeCode> constantTypeCodeMap;

    /**
     * Maps java classes to <code>InterfaceDefImpl</code>s for interfaces.
     */
    private Map interfaceMap = new HashMap();

    /**
     * Maps java classes to <code>ValueDefImpl</code>s for values.
     */
    private Map valueMap = new HashMap();

    /**
     * Maps java classes to <code>ExceptionDefImpl</code>s for exceptions.
     */
    private Map exceptionMap = new HashMap();

    /**
     * Maps java classes to <code>ValueBoxDefImpl</code>s for arrays.
     */
    private Map arrayMap = new HashMap();


    /**
     * java.io.Serializable special mapping, as per section 1.3.10.1.
     * Do not use this variable directly, use the
     * <code>getJavaIoSerializable()</code> method instead, as that will
     * create the typedef in the IR on demand.
     */
    private AliasDefImpl javaIoSerializable = null;

    /**
     * java.io.Externalizable special mapping, as per section 1.3.10.1.
     * Do not use this variable directly, use the
     * <code>getJavaIoExternalizable()</code> method instead, as that will
     * create the typedef in the IR on demand.
     */
    private AliasDefImpl javaIoExternalizable = null;

    /**
     * java.lang.Object special mapping, as per section 1.3.10.2.
     * Do not use this variable directly, use the
     * <code>getJavaLang_Object()</code> method instead, as that will
     * create the typedef in the IR on demand.
     */
    private AliasDefImpl javaLang_Object = null;

    /**
     * java.lang.String special mapping, as per section 1.3.5.10.
     * Do not use this variable directly, use the
     * <code>getJavaLangString()</code> method instead, as that will
     * create the value type in the IR on demand.
     */
    private ValueDefImpl javaLangString = null;

    /**
     * java.lang.Class special mapping, as per section 1.3.5.11.
     * Do not use this variable directly, use the
     * <code>getJavaxRmiCORBAClassDesc()</code> method instead, as that will
     * create the value type in the IR on demand.
     */
    private ValueDefImpl javaxRmiCORBAClassDesc = null;

    public InterfaceRepository(ORB orb, POA poa, String name) {
        this.orb = orb;
        this.poa = poa;
        impl = new RepositoryImpl(orb, poa, name);

        // TypeCodes for primitive types
        final HashMap<Class<?>, TypeCode> primitiveTypeCodeMap = new HashMap<Class<?>, TypeCode>();
        primitiveTypeCodeMap.put(Void.TYPE, orb.get_primitive_tc(TCKind.tk_void));
        primitiveTypeCodeMap.put(Boolean.TYPE,  orb.get_primitive_tc(TCKind.tk_boolean));
        primitiveTypeCodeMap.put(Character.TYPE,  orb.get_primitive_tc(TCKind.tk_wchar));
        primitiveTypeCodeMap.put(Byte.TYPE, orb.get_primitive_tc(TCKind.tk_octet));
        primitiveTypeCodeMap.put(Short.TYPE, orb.get_primitive_tc(TCKind.tk_short));
        primitiveTypeCodeMap.put(Integer.TYPE, orb.get_primitive_tc(TCKind.tk_long));
        primitiveTypeCodeMap.put(Long.TYPE, orb.get_primitive_tc(TCKind.tk_longlong));
        primitiveTypeCodeMap.put(Float.TYPE, orb.get_primitive_tc(TCKind.tk_float));
        primitiveTypeCodeMap.put(Double.TYPE, orb.get_primitive_tc(TCKind.tk_double));
        this.typeCodeMap = primitiveTypeCodeMap;

        final HashMap<Class<?>, TypeCode> typeCodes = new HashMap<Class<?>, TypeCode>(primitiveTypeCodeMap);
        typeCodes.put(String.class, orb.create_wstring_tc(0));
        constantTypeCodeMap = typeCodes;
    }

    /**
     * Add mapping for a class.
     */
    public void mapClass(Class cls)
            throws RMIIIOPViolationException, IRConstructionException {
        // Just lookup a TypeCode for the class: That will provoke
        // mapping the class and adding it to the IR.
        getTypeCode(cls);
    }


    /**
     * Finish the build.
     */
    public void finishBuild()
            throws IRConstructionException {
        impl.allDone();
    }

    /**
     * Return a CORBA reference to this IR.
     */
    public Repository getReference() {
        return RepositoryHelper.narrow(impl.getReference());
    }

    /**
     * Deactivate all CORBA objects in this IR.
     */
    public void shutdown() {
        impl.shutdown();
    }



    /**
     * Returns the TypeCode suitable for an IDL constant.
     *
     * @param cls The Java class denoting the type of the constant.
     */
    private TypeCode getConstantTypeCode(Class cls)
            throws IRConstructionException {
        if (cls == null)
            throw IIOPLogger.ROOT_LOGGER.invalidNullClass();

        TypeCode ret = constantTypeCodeMap.get(cls);

        if (ret == null)
            throw IIOPLogger.ROOT_LOGGER.badClassForConstant(cls.getName());
        return ret;
    }

    /**
     * Returns the TypeCode IDL TypeCodes for parameter, result, attribute
     * and value member types.
     * This may provoke a mapping of the class argument.
     * <p/>
     * Exception classes map to both values and exceptions. For these, this
     * method returns the typecode for the value, and you can use the
     * <code>getExceptionTypeCode</code> TODO method to get the typecode for the
     * mapping to exception.
     *
     * @param cls The Java class denoting the java type.
     */
    private TypeCode getTypeCode(Class cls)
            throws IRConstructionException, RMIIIOPViolationException {
        if (cls == null)
            throw IIOPLogger.ROOT_LOGGER.invalidNullClass();

        TypeCode ret = (TypeCode) typeCodeMap.get(cls);

        if (ret == null) {
            if (cls == java.lang.String.class)
                ret = getJavaLangString().type();
            else if (cls == java.lang.Object.class)
                ret = getJavaLang_Object().type();
            else if (cls == java.lang.Class.class)
                ret = getJavaxRmiCORBAClassDesc().type();
            else if (cls == java.io.Serializable.class)
                ret = getJavaIoSerializable().type();
            else if (cls == java.io.Externalizable.class)
                ret = getJavaIoExternalizable().type();
            else {
                // Try adding a mapping of the the class to the IR
                addClass(cls);

                // Lookup again, it should be there now.
                ret = (TypeCode) typeCodeMap.get(cls);

                if (ret == null)
                    throw IIOPLogger.ROOT_LOGGER.unknownTypeCodeForClass(cls.getName());
                else
                    return ret;
            }

            typeCodeMap.put(cls, ret);
        }

        return ret;
    }

    /**
     * Add a new IDL TypeCode for a mapped class.
     *
     * @param cls      The Java class denoting the java type.
     * @param typeCode The IDL type code of the mapped java class.
     */
    private void addTypeCode(Class cls, TypeCode typeCode)
            throws IRConstructionException {
        if (cls == null)
            throw IIOPLogger.ROOT_LOGGER.invalidNullClass();

        TypeCode tc = (TypeCode) typeCodeMap.get(cls);

        if (tc != null)
            throw IIOPLogger.ROOT_LOGGER.duplicateTypeCodeForClass(cls.getName());

        typeCodeMap.put(cls, typeCode);
    }

    /**
     * Get a reference to the special case mapping for java.io.Serializable.
     * This is according to "Java(TM) Language to IDL Mapping Specification",
     * section 1.3.10.1
     */
    private AliasDefImpl getJavaIoSerializable()
            throws IRConstructionException {
        if (javaIoSerializable == null) {
            final String id = "IDL:java/io/Serializable:1.0";
            final String name = "Serializable";
            final String version = "1.0";

            // Get module to add typedef to.
            ModuleDefImpl m = ensurePackageExists("java.io");

            TypeCode typeCode = orb.create_alias_tc(id, name,
                    orb.get_primitive_tc(TCKind.tk_any));
//         TypeCode typeCode = new TypeCodeImpl(TCKind._tk_alias, id, name,
//                                            new TypeCodeImpl(TCKind.tk_any));

            javaIoSerializable = new AliasDefImpl(id, name, version, m,
                    typeCode, impl);
            m.add(name, javaIoSerializable);
        }

        return javaIoSerializable;
    }

    /**
     * Get a reference to the special case mapping for java.io.Externalizable.
     * This is according to "Java(TM) Language to IDL Mapping Specification",
     * section 1.3.10.1
     */
    private AliasDefImpl getJavaIoExternalizable()
            throws IRConstructionException {
        if (javaIoExternalizable == null) {
            final String id = "IDL:java/io/Externalizable:1.0";
            final String name = "Externalizable";
            final String version = "1.0";

            // Get module to add typedef to.
            ModuleDefImpl m = ensurePackageExists("java.io");

            TypeCode typeCode = orb.create_alias_tc(id, name,
                    orb.get_primitive_tc(TCKind.tk_any));
//         TypeCode typeCode = new TypeCodeImpl(TCKind._tk_alias, id, name,
//                                            new TypeCodeImpl(TCKind.tk_any));

            javaIoExternalizable = new AliasDefImpl(id, name, version, m,
                    typeCode, impl);
            m.add(name, javaIoExternalizable);
        }

        return javaIoExternalizable;
    }

    /**
     * Get a reference to the special case mapping for java.lang.Object.
     * This is according to "Java(TM) Language to IDL Mapping Specification",
     * section 1.3.10.2
     */
    private AliasDefImpl getJavaLang_Object()
            throws IRConstructionException {
        if (javaLang_Object == null) {
            final String id = "IDL:java/lang/_Object:1.0";
            final String name = "_Object";
            final String version = "1.0";

            // Get module to add typedef to.
            ModuleDefImpl m = ensurePackageExists("java.lang");

            TypeCode typeCode = orb.create_alias_tc(id, name,
                    orb.get_primitive_tc(TCKind.tk_any));
//         TypeCode typeCode = new TypeCodeImpl(TCKind._tk_alias, id, name,
//                                            new TypeCodeImpl(TCKind.tk_any));

            javaLang_Object = new AliasDefImpl(id, name, version, m,
                    typeCode, impl);
            m.add(name, javaLang_Object);
        }

        return javaLang_Object;
    }

    /**
     * Get a reference to the special case mapping for java.lang.String.
     * This is according to "Java(TM) Language to IDL Mapping Specification",
     * section 1.3.5.10
     */
    private ValueDefImpl getJavaLangString()
            throws IRConstructionException {
        if (javaLangString == null) {
            ModuleDefImpl m = ensurePackageExists("org.omg.CORBA");
            ValueDefImpl val =
                    new ValueDefImpl("IDL:omg.org/CORBA/WStringValue:1.0",
                            "WStringValue", "1.0",
                            m, false, false,
                            new String[0], new String[0],
                            orb.get_primitive_tc(TCKind.tk_null),
                            impl);
            ValueMemberDefImpl vmdi =
                    new ValueMemberDefImpl("IDL:omg.org/CORBA/WStringValue.data:1.0",
                            "data", "1.0", orb.create_wstring_tc(0),
                            true, val, impl);
            val.add("data", vmdi);
            m.add("WStringValue", val);

            javaLangString = val;
        }

        return javaLangString;
    }

    /**
     * Get a reference to the special case mapping for java.lang.Class.
     * This is according to "Java(TM) Language to IDL Mapping Specification",
     * section 1.3.5.11.
     */
    private ValueDefImpl getJavaxRmiCORBAClassDesc()
            throws IRConstructionException, RMIIIOPViolationException {
        if (javaxRmiCORBAClassDesc == null) {
            // Just map the right value class
            ValueAnalysis va = ValueAnalysis.getValueAnalysis(javax.rmi.CORBA.ClassDesc.class);
            ValueDefImpl val = addValue(va);

            // Warn if it does not conform to the specification.
            if (!"RMI:javax.rmi.CORBA.ClassDesc:B7C4E3FC9EBDC311:CFBF02CF5294176B".equals(val.id()))
                IIOPLogger.ROOT_LOGGER.warnClassDescDoesNotConformToSpec();

            javaxRmiCORBAClassDesc = val;
        }

        return javaxRmiCORBAClassDesc;
    }


    /**
     * Ensure that a package exists in the IR.
     * This will create modules in the IR as needed.
     *
     * @param pkgName The package that needs to be defined as a module in the IR.
     * @return A reference to the IR module that represents the package.
     */
    private ModuleDefImpl ensurePackageExists(String pkgName)
            throws IRConstructionException {
        return ensurePackageExists(impl, "", pkgName);
    }

    /**
     * Ensure that a package exists in the IR.
     * This will create modules in the IR as needed.
     *
     * @param c         The container that the remainder of modules should be defined in.
     * @param previous  The IDL module name, from root to <code>c</code>.
     * @param remainder The java package name, relative to <code>c</code>.
     * @return A reference to the IR module that represents the package.
     */
    private ModuleDefImpl ensurePackageExists(LocalContainer c,
                                              String previous,
                                              String remainder)
            throws IRConstructionException {
        if ("".equals(remainder))
            return (ModuleDefImpl) c; // done

        int idx = remainder.indexOf('.');
        String base;

        if (idx == -1)
            base = remainder;
        else
            base = remainder.substring(0, idx);
        base = Util.javaToIDLName(base);

        if (previous.equals(""))
            previous = base;
        else
            previous = previous + "/" + base;
        if (idx == -1)
            remainder = "";
        else
            remainder = remainder.substring(idx + 1);

        LocalContainer next = null;
        LocalContained contained = (LocalContained) c._lookup(base);

        if (contained instanceof LocalContainer)
            next = (LocalContainer) contained;
        else if (contained != null)
            throw IIOPLogger.ROOT_LOGGER.collisionWhileCreatingPackage();

        if (next == null) {
            String id = "IDL:" + previous + ":1.0";

            // Create module
            ModuleDefImpl m = new ModuleDefImpl(id, base, "1.0", c, impl);

            c.add(base, m);

            if (idx == -1)
                return m; // done

            next = (LocalContainer) c._lookup(base); // Better be there now...
        } else // Check that next _is_ a module
            if (next.def_kind() != DefinitionKind.dk_Module)
                throw IIOPLogger.ROOT_LOGGER.collisionWhileCreatingPackage();

        return ensurePackageExists(next, previous, remainder);
    }

    /**
     * Add a set of constants to a container (interface or value class).
     */
    private void addConstants(LocalContainer container,
                              ContainerAnalysis ca)
            throws RMIIIOPViolationException, IRConstructionException {
        ConstantAnalysis[] consts = ca.getConstants();
        for (int i = 0; i < consts.length; ++i) {
            ConstantDefImpl cDef;
            String cid = ca.getMemberRepositoryId(consts[i].getJavaName());
            String cName = consts[i].getIDLName();

            Class cls = consts[i].getType();
            TypeCode typeCode = getConstantTypeCode(cls);

            Any value = orb.create_any();
            consts[i].insertValue(value);

            cDef = new ConstantDefImpl(cid, cName, "1.0",
                    typeCode, value, container, impl);
            container.add(cName, cDef);
        }
    }

    /**
     * Add a set of attributes to a container (interface or value class).
     */
    private void addAttributes(LocalContainer container,
                               ContainerAnalysis ca)
            throws RMIIIOPViolationException, IRConstructionException {
        AttributeAnalysis[] attrs = ca.getAttributes();
        for (int i = 0; i < attrs.length; ++i) {
            AttributeDefImpl aDef;
            String aid = ca.getMemberRepositoryId(attrs[i].getJavaName());
            String aName = attrs[i].getIDLName();

            Class cls = attrs[i].getCls();
            TypeCode typeCode = getTypeCode(cls);
            aDef = new AttributeDefImpl(aid, aName, "1.0", attrs[i].getMode(),
                    typeCode, container, impl);
            container.add(aName, aDef);
        }
    }

    /**
     * Add a set of operations to a container (interface or value class).
     */
    private void addOperations(LocalContainer container,
                               ContainerAnalysis ca)
            throws RMIIIOPViolationException, IRConstructionException {
        OperationAnalysis[] ops = ca.getOperations();
        for (int i = 0; i < ops.length; ++i) {
            OperationDefImpl oDef;
            String oName = ops[i].getIDLName();
            String oid = ca.getMemberRepositoryId(oName);

            Class cls = ops[i].getReturnType();
            TypeCode typeCode = getTypeCode(cls);

            ParameterAnalysis[] ps = ops[i].getParameters();
            ParameterDescription[] params = new ParameterDescription[ps.length];
            for (int j = 0; j < ps.length; ++j) {
                params[j] = new ParameterDescription(ps[j].getIDLName(),
                        getTypeCode(ps[j].getCls()),
                        null, // filled in later
                        ParameterMode.PARAM_IN);
            }

            ExceptionAnalysis[] exc = ops[i].getMappedExceptions();
            ExceptionDef[] exceptions = new ExceptionDef[exc.length];
            for (int j = 0; j < exc.length; ++j) {
                ExceptionDefImpl e = addException(exc[j]);
                exceptions[j] = ExceptionDefHelper.narrow(e.getReference());
            }

            oDef = new OperationDefImpl(oid, oName, "1.0", container,
                    typeCode, params, exceptions, impl);
            container.add(oName, oDef);
        }
    }

    /**
     * Add a set of interfaces to the IR.
     *
     * @return An array of the IR IDs of the interfaces.
     */
    private String[] addInterfaces(ContainerAnalysis ca)
            throws RMIIIOPViolationException, IRConstructionException {
        InterfaceAnalysis[] interfaces = ca.getInterfaces();
        List base_interfaces = new ArrayList();
        for (int i = 0; i < interfaces.length; ++i) {
            InterfaceDefImpl idi = addInterface(interfaces[i]);
            base_interfaces.add(idi.id());
        }
        String[] strArr = new String[base_interfaces.size()];
        return (String[]) base_interfaces.toArray(strArr);
    }

    /**
     * Add a set of abstract valuetypes to the IR.
     *
     * @return An array of the IR IDs of the abstract valuetypes.
     */
    private String[] addAbstractBaseValuetypes(ContainerAnalysis ca)
            throws RMIIIOPViolationException, IRConstructionException {
        ValueAnalysis[] abstractValuetypes = ca.getAbstractBaseValuetypes();
        List abstract_base_valuetypes = new ArrayList();
        for (int i = 0; i < abstractValuetypes.length; ++i) {
            ValueDefImpl vdi = addValue(abstractValuetypes[i]);
            abstract_base_valuetypes.add(vdi.id());
        }
        String[] strArr = new String[abstract_base_valuetypes.size()];
        return (String[]) abstract_base_valuetypes.toArray(strArr);
    }

    /**
     * Map the class and add its IIOP mapping to the repository.
     */
    private void addClass(Class cls)
            throws RMIIIOPViolationException, IRConstructionException {
        if (cls.isPrimitive())
            return; // No need to add primitives.

        if (cls.isArray()) {
            // Add array mapping
            addArray(cls);
        } else if (cls.isInterface()) {
            if (!RmiIdlUtil.isAbstractValueType(cls)) {
                // Analyse the interface
                InterfaceAnalysis ia = InterfaceAnalysis.getInterfaceAnalysis(cls);

                // Add analyzed interface (which may be abstract)
                addInterface(ia);
            } else {
                // Analyse the value
                ValueAnalysis va = ValueAnalysis.getValueAnalysis(cls);

                // Add analyzed value
                addValue(va);
            }
        } else if (Exception.class.isAssignableFrom(cls)) { // Exception type.
            // Analyse the exception
            ExceptionAnalysis ea = ExceptionAnalysis.getExceptionAnalysis(cls);

            // Add analyzed exception
            addException(ea);
        } else { // Got to be a value type.
            // Analyse the value
            ValueAnalysis va = ValueAnalysis.getValueAnalysis(cls);

            // Add analyzed value
            addValue(va);
        }
    }

    /**
     * Add an array.
     */
    private ValueBoxDefImpl addArray(Class cls)
            throws RMIIIOPViolationException, IRConstructionException {
        if (!cls.isArray())
            throw IIOPLogger.ROOT_LOGGER.classIsNotArray(cls.getName());

        ValueBoxDefImpl vbDef;

        // Lookup: Has it already been added?
        vbDef = (ValueBoxDefImpl) arrayMap.get(cls);
        if (vbDef != null)
            return vbDef; // Yes, just return it.

        int dimensions = 0;
        Class compType = cls;

        do {
            compType = compType.getComponentType();
            ++dimensions;
        } while (compType.isArray());

        String typeName;
        String moduleName;
        TypeCode typeCode;

        if (compType.isPrimitive()) {
            if (compType == Boolean.TYPE) {
                typeName = "boolean";
                typeCode = orb.get_primitive_tc(TCKind.tk_boolean);
            } else if (compType == Character.TYPE) {
                typeName = "wchar";
                typeCode = orb.get_primitive_tc(TCKind.tk_wchar);
            } else if (compType == Byte.TYPE) {
                typeName = "octet";
                typeCode = orb.get_primitive_tc(TCKind.tk_octet);
            } else if (compType == Short.TYPE) {
                typeName = "short";
                typeCode = orb.get_primitive_tc(TCKind.tk_short);
            } else if (compType == Integer.TYPE) {
                typeName = "long";
                typeCode = orb.get_primitive_tc(TCKind.tk_long);
            } else if (compType == Long.TYPE) {
                typeName = "long_long";
                typeCode = orb.get_primitive_tc(TCKind.tk_longlong);
            } else if (compType == Float.TYPE) {
                typeName = "float";
                typeCode = orb.get_primitive_tc(TCKind.tk_float);
            } else if (compType == Double.TYPE) {
                typeName = "double";
                typeCode = orb.get_primitive_tc(TCKind.tk_double);
            } else {
                throw IIOPLogger.ROOT_LOGGER.unknownPrimitiveType(compType.getName());
            }

            moduleName = "org.omg.boxedRMI";
        } else {
            typeCode = getTypeCode(compType); // map the component type.

            if (compType == java.lang.String.class)
                typeName = getJavaLangString().name();
            else if (compType == java.lang.Object.class)
                typeName = getJavaLang_Object().name();
            else if (compType == java.lang.Class.class)
                typeName = getJavaxRmiCORBAClassDesc().name();
            else if (compType == java.io.Serializable.class)
                typeName = getJavaIoSerializable().name();
            else if (compType == java.io.Externalizable.class)
                typeName = getJavaIoExternalizable().name();
            else if (compType.isInterface() &&
                    !RmiIdlUtil.isAbstractValueType(compType))
                typeName = ((InterfaceDefImpl) interfaceMap.get(compType)).name();
            else if (Exception.class.isAssignableFrom(compType)) // exception type
                typeName = ((ExceptionDefImpl) exceptionMap.get(compType)).name();
            else // must be value type
                typeName = ((ValueDefImpl) valueMap.get(compType)).name();

            moduleName = "org.omg.boxedRMI." + compType.getPackage().getName();
        }

        // Get module to add array to.
        ModuleDefImpl m = ensurePackageExists(moduleName);

        // Create an array of the types for the dimensions
        Class[] types = new Class[dimensions];
        types[dimensions - 1] = cls;
        for (int i = dimensions - 2; i >= 0; --i)
            types[i] = types[i + 1].getComponentType();

        // Create boxed sequences for all dimensions.
        for (int i = 0; i < dimensions; ++i) {
            Class type = types[i];

            typeCode = orb.create_sequence_tc(0, typeCode);
            vbDef = (ValueBoxDefImpl) arrayMap.get(type);
            if (vbDef == null) {
                String id = Util.getIRIdentifierOfClass(type);

                SequenceDefImpl sdi = new SequenceDefImpl(typeCode, impl);

                String name = "seq" + (i + 1) + "_" + typeName;
//            TypeCode boxTypeCode = new TypeCodeImpl(TCKind._tk_value_box,
//                                                    id, name, typeCode);
                TypeCode boxTypeCode = orb.create_value_box_tc(id, name, typeCode);
                vbDef = new ValueBoxDefImpl(id, name, "1.0", m, boxTypeCode, impl);

                addTypeCode(type, vbDef.type());
                m.add(name, vbDef);
                impl.putSequenceImpl(id, typeCode, sdi, vbDef);

                arrayMap.put(type, vbDef); // Remember we mapped this.

                typeCode = boxTypeCode;
            } else
                typeCode = vbDef.type();
        }

        // Return the box of highest dimension.
        return vbDef;
    }

    /**
     * Add an interface.
     */
    private InterfaceDefImpl addInterface(InterfaceAnalysis ia)
            throws RMIIIOPViolationException, IRConstructionException {
        InterfaceDefImpl iDef;
        Class cls = ia.getCls();

        // Lookup: Has it already been added?
        iDef = (InterfaceDefImpl) interfaceMap.get(cls);
        if (iDef != null)
            return iDef; // Yes, just return it.

        // Get module to add interface to.
        ModuleDefImpl m = ensurePackageExists(cls.getPackage().getName());

        // Add superinterfaces
        String[] base_interfaces = addInterfaces(ia);

        // Create the interface
        String base = cls.getName();
        base = base.substring(base.lastIndexOf('.') + 1);
        base = Util.javaToIDLName(base);

        iDef = new InterfaceDefImpl(ia.getRepositoryId(),
                base, "1.0", m,
                base_interfaces, impl);
        addTypeCode(cls, iDef.type());
        m.add(base, iDef);
        interfaceMap.put(cls, iDef); // Remember we mapped this.

        // Fill in constants
        addConstants(iDef, ia);

        // Add attributes
        addAttributes(iDef, ia);

        // Fill in operations
        addOperations(iDef, ia);

        return iDef;
    }

    /**
     * Add a value type.
     */
    private ValueDefImpl addValue(ValueAnalysis va)
            throws RMIIIOPViolationException, IRConstructionException {
        ValueDefImpl vDef;
        Class cls = va.getCls();

        // Lookup: Has it already been added?
        vDef = (ValueDefImpl) valueMap.get(cls);
        if (vDef != null)
            return vDef; // Yes, just return it.

        // Get module to add value to.
        ModuleDefImpl m = ensurePackageExists(cls.getPackage().getName());

        // Add implemented interfaces
        String[] supported_interfaces = addInterfaces(va);

        // Add abstract base valuetypes
        String[] abstract_base_valuetypes = addAbstractBaseValuetypes(va);

        // Add superclass
        ValueDefImpl superValue = null;
        ValueAnalysis superAnalysis = va.getSuperAnalysis();
        if (superAnalysis != null)
            superValue = addValue(superAnalysis);

        // Create the value
        String base = cls.getName();
        base = base.substring(base.lastIndexOf('.') + 1);
        base = Util.javaToIDLName(base);

        TypeCode baseTypeCode;
        if (superValue == null)
            baseTypeCode = orb.get_primitive_tc(TCKind.tk_null);
        else
            baseTypeCode = superValue.type();

        vDef = new ValueDefImpl(va.getRepositoryId(), base, "1.0",
                m,
                va.isAbstractValue(),
                va.isCustom(),
                supported_interfaces,
                abstract_base_valuetypes,
                baseTypeCode,
                impl);
        addTypeCode(cls, vDef.type());
        m.add(base, vDef);
        valueMap.put(cls, vDef); // Remember we mapped this.

        // Fill in constants.
        addConstants(vDef, va);

        // Add value members
        ValueMemberAnalysis[] vmas = va.getMembers();
        for (int i = 0; i < vmas.length; ++i) {
            ValueMemberDefImpl vmDef;
            String vmid = va.getMemberRepositoryId(vmas[i].getJavaName());
            String vmName = vmas[i].getIDLName();

            Class vmCls = vmas[i].getCls();
            TypeCode typeCode = getTypeCode(vmCls);

            boolean vmPublic = vmas[i].isPublic();

            vmDef = new ValueMemberDefImpl(vmid, vmName, "1.0",
                    typeCode, vmPublic, vDef, impl);
            vDef.add(vmName, vmDef);
        }

        // Add attributes
        addAttributes(vDef, va);

        // TODO: Fill in operations.

        return vDef;
    }

    /**
     * Add an exception type.
     */
    private ExceptionDefImpl addException(ExceptionAnalysis ea)
            throws RMIIIOPViolationException, IRConstructionException {
        ExceptionDefImpl eDef;
        Class cls = ea.getCls();

        // Lookup: Has it already been added?
        eDef = (ExceptionDefImpl) exceptionMap.get(cls);
        if (eDef != null)
            return eDef; // Yes, just return it.

        // 1.3.7.1: map to value
        ValueDefImpl vDef = addValue(ea);

        // 1.3.7.2: map to exception
        ModuleDefImpl m = ensurePackageExists(cls.getPackage().getName());
        String base = cls.getName();
        base = base.substring(base.lastIndexOf('.') + 1);
        if (base.endsWith("Exception"))
            base = base.substring(0, base.length() - 9);
        base = Util.javaToIDLName(base + "Ex");

        StructMember[] members = new StructMember[1];
        members[0] = new StructMember("value", vDef.type(), null/*ignored*/);
        TypeCode typeCode
                = orb.create_exception_tc(ea.getExceptionRepositoryId(),
                base, members);

        eDef = new ExceptionDefImpl(ea.getExceptionRepositoryId(), base, "1.0",
                typeCode, vDef, m, impl);
        m.add(base, eDef);
        exceptionMap.put(cls, eDef); // Remember we mapped this.

        return eDef;
    }


    // Inner classes -------------------------------------------------
}
