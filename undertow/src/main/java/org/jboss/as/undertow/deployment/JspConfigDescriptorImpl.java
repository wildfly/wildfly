package org.jboss.as.undertow.deployment;

import org.apache.jasper.deploy.JspPropertyGroup;
import org.apache.jasper.deploy.TagLibraryInfo;

import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Stuart Douglas
 */
public class JspConfigDescriptorImpl implements JspConfigDescriptor {

    private final Collection<TaglibDescriptor> taglibs;
    private final Collection<JspPropertyGroupDescriptor> jspPropertyGroups;

    public JspConfigDescriptorImpl(Collection<TagLibraryInfo> taglibs, Collection<JspPropertyGroup> jspPropertyGroups) {
        this.taglibs = new ArrayList<TaglibDescriptor>();
        for(TagLibraryInfo t : taglibs) {
            this.taglibs.add(new TaglibDescriptorImpl(t));
        }
        this.jspPropertyGroups = new ArrayList<JspPropertyGroupDescriptor>();
        for(JspPropertyGroup p : jspPropertyGroups) {
            this.jspPropertyGroups.add(new JspPropertyGroupDescriptorImpl(p));
        }
    }

    @Override
    public Collection<TaglibDescriptor> getTaglibs() {
        return new ArrayList<TaglibDescriptor>(taglibs);
    }

    @Override
    public Collection<JspPropertyGroupDescriptor> getJspPropertyGroups() {
        return new ArrayList<JspPropertyGroupDescriptor>(jspPropertyGroups);
    }
}
