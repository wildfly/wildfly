package org.jboss.as.test.integration.jsf.duplicateid.deployment;

import jakarta.faces.annotation.FacesConfig;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Kari
 */
@Named("includeBean")
@ViewScoped
// TODO remove once standard WildFly moves to Faces 4
@FacesConfig
public class IncludeBean implements Serializable {
    private final Set<Integer> visibleComponentIndexes = new HashSet<Integer>();

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
