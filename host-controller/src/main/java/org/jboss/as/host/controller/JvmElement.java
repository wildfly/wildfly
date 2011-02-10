/**
 *
 */
package org.jboss.as.host.controller;

import org.jboss.as.controller.parsing.JvmType;

/**
 * A Java Virtual Machine configuration.
 *
 * @author Brian Stansberry
 */
public class JvmElement {

    private static final long serialVersionUID = 4963103173530602991L;

    //Attributes
    private final String name;
    private JvmType type;
    private String javaHome;
    private Boolean debugEnabled;
    private String debugOptions;
    private Boolean envClasspathIgnored;

    //Elements
    private String heapSize;
    private String maxHeap;
    private String permgenSize;
    private String maxPermgen;
    private String agentPath;
    private String agentLib;
    private String javaagent;
    private String stack;
    private final JvmOptionsElement jvmOptionsElement = new JvmOptionsElement();
    private PropertiesElement environmentVariables = new PropertiesElement(Element.VARIABLE, true);
    private PropertiesElement systemProperties = new PropertiesElement(Element.PROPERTY, true);


    /**
     */
    public JvmElement(final String name) {
        this.name = name;
    }

    public JvmElement(JvmElement ... toCombine) {
        // FIXME -- hack Location
        super();

        this.name = toCombine[0].getName();

        for (JvmElement element : toCombine) {
            if(element == null)
                continue;
            if (! this.name.equals(element.getName())) {
                throw new IllegalArgumentException("Jvm " + element.getName() + " has a different name from the other jvm elements; all must have the same name");
            }
            if (element.getJavaHome() != null) {
                this.javaHome = element.getJavaHome();
            }
            if (element.getJvmType() != null) {
                this.type = element.getJvmType();
            }
            if (element.getDebugOptions() != null) {
                this.debugOptions = element.getDebugOptions();
            }
            if (element.isDebugEnabled() != null) {
                this.debugEnabled = element.isDebugEnabled();
            }
            if (element.isEnvClasspathIgnored() != null) {
                this.envClasspathIgnored = element.isEnvClasspathIgnored();
            }
            if (element.getPermgenSize() != null) {
                this.permgenSize = element.getPermgenSize();
            }
            if (element.getMaxPermgen() != null) {
                this.maxPermgen = element.getMaxPermgen();
            }
            if (element.getHeapSize() != null) {
                this.heapSize = element.getHeapSize();
            }
            if (element.getMaxHeap() != null) {
                this.maxHeap = element.getMaxHeap();
            }
            if (element.getStack() != null) {
                this.stack = element.getStack();
            }
            if (element.getAgentLib() != null) {
                this.agentLib = element.getAgentLib();
            }
            if (element.getAgentPath() != null) {
                this.agentPath = element.getAgentPath();
            }
            if (element.getJavaagent() != null) {
                this.javaagent = element.getJavaagent();
            }
        }

        PropertiesElement[] combinedEnv = new PropertiesElement[toCombine.length];
        for (int i = 0; i < toCombine.length; i++) {
            if(toCombine[i] == null)
                continue;
            combinedEnv[i] = toCombine[i].getEnvironmentVariables();
        }
        this.environmentVariables = new PropertiesElement(Element.ENVIRONMENT_VARIABLES, true, combinedEnv);

        PropertiesElement[] combinedSysp = new PropertiesElement[toCombine.length];
        for (int i = 0; i < toCombine.length; i++) {
            if(toCombine[i] == null)
                continue;
            combinedSysp[i] = toCombine[i].getSystemProperties();
        }
        this.systemProperties = new PropertiesElement(Element.SYSTEM_PROPERTIES, true, combinedSysp);
    }

    public String getJavaHome() {
        return javaHome;
    }

    void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    public JvmType getJvmType() {
        return type;
    }

    void setJvmType(JvmType type) {
        this.type = type;
    }

    public String getPermgenSize() {
        return permgenSize;
    }

    void setPermgenSize(String permgenSize) {
        this.permgenSize = permgenSize;
    }

    public String getMaxPermgen() {
        return maxPermgen;
    }

    void setMaxPermgen(String maxPermgen) {
        this.maxPermgen = maxPermgen;
    }

    public String getHeapSize() {
        return heapSize;
    }

    void setHeapSize(String heapSize) {
        this.heapSize = heapSize;
    }

    public String getMaxHeap() {
        return maxHeap;
    }

    void setMaxHeap(String maxHeap) {
        this.maxHeap = maxHeap;
    }

    public String getName() {
        return name;
    }

    public Boolean isDebugEnabled() {
        return debugEnabled;
    }

    void setDebugEnabled(Boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public String getDebugOptions() {
        return debugOptions;
    }

    void setDebugOptions(String debugOptions) {
        this.debugOptions = debugOptions;
    }

    public String getStack() {
        return stack;
    }

    void setStack(String stack) {
        this.stack = stack;
    }

    public Boolean isEnvClasspathIgnored() {
        return envClasspathIgnored;
    }

    void setEnvClasspathIgnored(Boolean envClasspathIgnored) {
        this.envClasspathIgnored = envClasspathIgnored;
    }

    public JvmOptionsElement getJvmOptions() {
        return jvmOptionsElement;
    }

    public PropertiesElement getEnvironmentVariables() {
        return environmentVariables;
    }

    public PropertiesElement getSystemProperties() {
        return systemProperties;
    }

    public String getAgentPath() {
        return agentPath;
    }

    void setAgentPath(String agentPath) {
        if (agentLib != null) {
            throw new IllegalArgumentException("Attempting to set 'agent-path' when 'agent-lib' was already set");
        }
        this.agentPath = agentPath;
    }

    public String getAgentLib() {
        return agentLib;
    }

    void setAgentLib(String agentLib) {
        if (agentPath != null) {
            throw new IllegalArgumentException("Attempting to set 'agent-lib' when 'agent-path' was already set");
        }
        this.agentLib = agentLib;
    }

    public String getJavaagent() {
        return javaagent;
    }

    void setJavaagent(String javaagent) {
        this.javaagent = javaagent;
    }

    private interface MinMaxSetter {
        void setMinMax(String min, String max);
    }

    private class HeapSetter implements MinMaxSetter {

        @Override
        public void setMinMax(String min, String max) {
            heapSize = min;
            maxHeap = max;
        }
    }

    private class PermGenSetter implements MinMaxSetter {

        @Override
        public void setMinMax(String min, String max) {
            permgenSize = min;
            maxHeap = max;
        }
    }

}
