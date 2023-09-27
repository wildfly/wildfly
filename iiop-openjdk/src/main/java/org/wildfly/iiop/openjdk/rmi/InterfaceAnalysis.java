/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.rmi;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.wildfly.iiop.openjdk.logging.IIOPLogger;


/**
 * Interface analysis.
 * <p/>
 * Routines here are conforming to the "Java(TM) Language to IDL Mapping
 * Specification", version 1.1 (01-06-07).
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 */
public class InterfaceAnalysis extends ContainerAnalysis {

    private boolean abstractInterface;

    private String[] allTypeIds;

    /**
     * Map of IDL operation names to operation analyses.
     */
    Map<String, OperationAnalysis> operationAnalysisMap;

    private static WorkCacheManager cache = new WorkCacheManager(InterfaceAnalysis.class);

    public static InterfaceAnalysis getInterfaceAnalysis(Class cls) throws RMIIIOPViolationException {
        return (InterfaceAnalysis) cache.getAnalysis(cls);
    }

    public static void clearCache(final ClassLoader classLoader) {
        cache.clearClassLoader(classLoader);
    }

    protected InterfaceAnalysis(Class cls) {
        super(cls);
    }

    protected void doAnalyze() throws RMIIIOPViolationException {
        super.doAnalyze();

        calculateOperationAnalysisMap();
        fixupCaseNames();
    }

    public boolean isAbstractInterface() {
        return abstractInterface;
    }

    public boolean isRmiIdlRemoteInterface() {
        return (!abstractInterface);
    }

    public String[] getAllTypeIds() {
        return (String[]) allTypeIds.clone();
    }

    /**
     * Return a list of all the entries contained here.
     */
    protected ArrayList getContainedEntries() {
        final ArrayList ret = new ArrayList(constants.length + attributes.length + operations.length);

        for (int i = 0; i < constants.length; ++i)
            ret.add(constants[i]);
        for (int i = 0; i < attributes.length; ++i)
            ret.add(attributes[i]);
        for (int i = 0; i < operations.length; ++i)
            ret.add(operations[i]);

        return ret;
    }

    /**
     * Analyse operations.
     * This will fill in the <code>operations</code> array.
     */
    protected void analyzeOperations() throws RMIIIOPViolationException {

        if (!cls.isInterface())
            throw IIOPLogger.ROOT_LOGGER.notAnInterface(cls.getName());

        abstractInterface = RmiIdlUtil.isAbstractInterface(cls);
        calculateAllTypeIds();

        int operationCount = 0;
        for (int i = 0; i < methods.length; ++i)
            if ((m_flags[i] & (M_READ | M_WRITE | M_READONLY)) == 0)
                ++operationCount;
        operations = new OperationAnalysis[operationCount];
        operationCount = 0;
        for (int i = 0; i < methods.length; ++i) {
            if ((m_flags[i] & (M_READ | M_WRITE | M_READONLY)) == 0) {
                operations[operationCount] = new OperationAnalysis(methods[i]);
                ++operationCount;
            }
        }

    }

    /**
     * Calculate the map that maps IDL operation names to operation analyses.
     * Besides mapped operations, this map also contains the attribute
     * accessor and mutator operations.
     */
    protected void calculateOperationAnalysisMap() {
        operationAnalysisMap = new HashMap();
        OperationAnalysis oa;

        // Map the operations
        for (int i = 0; i < operations.length; ++i) {
            oa = operations[i];
            operationAnalysisMap.put(oa.getIDLName(), oa);
        }

        // Map the attributes
        for (int i = 0; i < attributes.length; ++i) {
            AttributeAnalysis attr = attributes[i];

            oa = attr.getAccessorAnalysis();

            // Not having an accessor analysis means that
            // the attribute is not in a remote interface
            if (oa != null) {
                operationAnalysisMap.put(oa.getIDLName(), oa);

                oa = attr.getMutatorAnalysis();
                if (oa != null)
                    operationAnalysisMap.put(oa.getIDLName(), oa);
            }
        }
    }

    /**
     * Calculate the array containing all type ids of this interface,
     * in the format that org.omg.CORBA.portable.Servant._all_interfaces()
     * is expected to return.
     */
    protected void calculateAllTypeIds() {
        if (!isRmiIdlRemoteInterface()) {
            allTypeIds = new String[0];
        } else {
            ArrayList a = new ArrayList();
            InterfaceAnalysis[] intfs = getInterfaces();
            for (int i = 0; i < intfs.length; ++i) {
                String[] ss = intfs[i].getAllTypeIds();

                for (int j = 0; j < ss.length; ++j)
                    if (!a.contains(ss[j]))
                        a.add(ss[j]);
            }
            allTypeIds = new String[a.size() + 1];
            allTypeIds[0] = getRepositoryId();
            for (int i = 1; i <= a.size(); ++i)
                allTypeIds[i] = (String) a.get(a.size() - i);
        }
    }

}

