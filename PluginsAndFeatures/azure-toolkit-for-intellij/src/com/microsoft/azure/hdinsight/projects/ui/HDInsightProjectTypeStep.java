/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.projects.ui;

import com.intellij.ide.projectWizard.ProjectTemplateList;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.Disposable;
import com.microsoft.azure.hdinsight.projects.HDInsightExternalSystem;
import com.microsoft.azure.hdinsight.projects.HDInsightModuleBuilder;
import com.microsoft.azure.hdinsight.projects.HDInsightProjectTemplate;

import javax.swing.*;

public class HDInsightProjectTypeStep extends ModuleWizardStep implements Disposable {
    private HDInsightModuleBuilder moduleBuilder;
    private JPanel mainPanel;
    private ProjectTemplateList templateList;
    private JComboBox externalSystems;

    public HDInsightProjectTypeStep(HDInsightModuleBuilder moduleBuilder) {
        this.moduleBuilder = moduleBuilder;
        this.templateList.setTemplates(moduleBuilder.getTemplates(), false);
        this.externalSystems.addItem(HDInsightExternalSystem.MAVEN);
        this.externalSystems.addItem(HDInsightExternalSystem.SBT);
        this.externalSystems.setSelectedItem(HDInsightExternalSystem.MAVEN);
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public void updateDataModel() {
        this.moduleBuilder.setSelectedTemplate((HDInsightProjectTemplate) this.templateList.getSelectedTemplate());
        this.moduleBuilder.setSelectedExternalSystem((HDInsightExternalSystem) this.externalSystems.getSelectedItem());
    }

    @Override
    public void dispose() {
    }
}
