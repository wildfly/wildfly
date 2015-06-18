package com.redhat.gss.redhat_support_lib.helpers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

public class FilterHelper {
    private static final Logger LOGGER = Logger.getLogger(FilterHelper.class
            .getName());

    public static List<?> filterResults(List<?> list, String[] kwargs) {
        ArrayList<Object> matches = new ArrayList<Object>();
        if (kwargs != null && kwargs.length > 0) {
            if (list.size() > 0) {
                for (String kwarg : kwargs) {
                    String[] split = kwarg.split("=", 5);
                    for (Object obj : list) {
                        for (Method method : obj.getClass().getMethods()) {
                            if (method.getName().equalsIgnoreCase(
                                    "get" + split[0])) {
                                String value = null;
                                try {
                                    value = (String) method.invoke(obj,
                                            new Object[] {});
                                } catch (IllegalArgumentException e) {
                                    LOGGER.warn(e.getMessage());
                                    continue;
                                } catch (IllegalAccessException e) {
                                    LOGGER.warn(e.getMessage());
                                    continue;
                                } catch (InvocationTargetException e) {
                                    LOGGER.warn(e.getMessage());
                                    continue;
                                }
                                if (value.equalsIgnoreCase(split[1])) {
                                    matches.add(obj);
                                }
                                break;
                            }
                        }
                    }
                }
            }
            return matches;
        } else {
            return list;
        }
    }
}
