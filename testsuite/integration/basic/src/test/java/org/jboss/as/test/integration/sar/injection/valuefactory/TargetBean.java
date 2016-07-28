/*
Copyright 2016 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.test.integration.sar.injection.valuefactory;

public class TargetBean implements TargetBeanMBean {
    private int sourceCount;
    private int countWithArgument;

    @Override
    public int getSourceCount() {
        return sourceCount;
    }

    public void setSourceCount(int sourceCount) {
        this.sourceCount = sourceCount;
    }

    @Override
    public int getCountWithArgument() {
        return countWithArgument;
    }

    public void setCountWithArgument(int countWithArgument) {
        this.countWithArgument = countWithArgument;
    }
}
