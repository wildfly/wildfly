package org.jboss.as.test.integration.jsf.duplicateid.deployment;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Kari
 */
@ManagedBean(name = "includeBean")
@ViewScoped
public class IncludeBean implements Serializable {
    private Set<Integer> visibleComponentIndexes = new HashSet<Integer>();

    public void show(int index) {
        visibleComponentIndexes.add(index);
    }

    public void hide(int index) {
        visibleComponentIndexes.remove(index);
    }

    public boolean isVisible(int index) {
        return visibleComponentIndexes.contains(index);
    }
}
