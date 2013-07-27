package org.jboss.as.test.integration.deployment.resourcelisting;

import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.Resource;
import org.junit.Assert;

import java.util.*;

/**
 * @author: rhatlapa@redhat.com
 */
public class ResourceListingUtils {

    /**
     * Lists resources in deployment using provided API for iterating resources
     * @param classLoader the deployment class loader
     * @param rootDir directory which should be considered as root
     * @param recursive if true also a recursive resources are taken into account, otherwise only resources in rootDir are considered
     * @return list of resources returned by the API for iterating over deployment resources
     */
    public static List<String> listResources(ModuleClassLoader classLoader, String rootDir, boolean recursive) {
        List<String> resourceList = new ArrayList<>();
        Iterator<Resource> it = classLoader.iterateResources(rootDir, recursive);

        while (it.hasNext()) {
            resourceList.add(it.next().getName());
        }
        return resourceList;
    }

    /**
     * translates path to class in package notation to standard path notation with addition of .class suffix
     * @param clazz class in package notation
     * @return path representation of class
     */
    public static String classToPath(Class clazz) {
        return clazz.getName().replaceAll("\\.", "/") + ".class";
    }

    /**
     * Filters resources in collection using specified parameters
     * @param resources collection to be filtered
     * @param rootDir what is the root directory of resources which should be taken into account
     * @param removeRecursive if recursive resources should be removed or not (true means recursive resources are removed, false means that they are preserved)
     */
    public static void filterResources(Collection<String> resources, String rootDir, boolean removeRecursive) {
        String rootDirPrefix = "";
        if (rootDir.startsWith("/")) {
            rootDirPrefix = rootDir.substring(1);
        }
        Iterator<String> it = resources.iterator();
        while (it.hasNext()) {
            String resource = it.next();
            if (resource.startsWith(rootDirPrefix)) {
                // the rootDir needs to be removed from name for deciding if it is an recursive resource or not
                if (removeRecursive) {
                    String resourceWithoutPrefix = resource.substring(rootDirPrefix.length());
                    if (resourceWithoutPrefix.startsWith("/")) {
                        resourceWithoutPrefix = resourceWithoutPrefix.substring(1);
                    }
                    System.err.println("Original resource to check = " + resource);
                    System.err.println("Resource without its rootDir = " + resourceWithoutPrefix);
                    if (resourceWithoutPrefix.contains("/")) {
                        it.remove();
                    }
                }
            } else {
                it.remove();
            }

        }
    }



}
