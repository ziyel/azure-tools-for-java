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
package com.microsoft.azure.hdinsight.projects;

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.ProjectTemplate;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HDInsightProjectTemplate implements ProjectTemplate {
    private HDInsightTemplatesType templateType;

    public HDInsightProjectTemplate(HDInsightTemplatesType templatesType) {
        this.templateType = templatesType;
    }

    @NotNull
    @Override
    public String getName() {
        switch (this.templateType) {
            case Java:
                return "Spark on HDInsight (Java)";
            case Scala:
                return "Spark on HDInsight (Scala)";
            case JavaLocalSample:
                return "Spark on HDInsight Local Run Sample (Java)";
            case ScalaLocalSample:
                return "Spark on HDInsight Local Run Sample (Scala)";
            case ScalaClusterSample:
                return "Spark on HDInsight Cluster Run Sample (Scala)";
            default:
                return "HDInsight Tools";
        }
    }

    @Nullable
    @Override
    public String getDescription() {
        switch (this.templateType) {
            case Java:
            case Scala:
                return "HDInsight Spark blank module.";
            case JavaLocalSample:
                return "HDInsight Spark samples written in Java; This code sample should be executed locally.";
            case ScalaLocalSample:
                return "HDInsight Spark samples written in Scala; This code sample should be executed locally.";
            case ScalaClusterSample:
                return "HDInsight Spark samples written in Scala; This code sample should be submitted to HDInsight cluster.";
            default:
                return "HDInsight Tools";
        }
    }

    @Override
    public Icon getIcon() {
        switch (this.templateType) {
            case Java:
            case JavaLocalSample:
                return StreamUtil.getImageResourceFile(CommonConst.JavaProjectIconPath);
            case Scala:
            case ScalaLocalSample:
            case ScalaClusterSample:
                return StreamUtil.getImageResourceFile(CommonConst.ScalaProjectIconPath);
            default:
                return StreamUtil.getImageResourceFile(CommonConst.JavaProjectIconPath);
        }
    }

    @NotNull
    @Override
    public AbstractModuleBuilder createModuleBuilder() {
        return null;
    }

    @Nullable
    @Override
    public ValidationInfo validateSettings() {
        return null;
    }

    public HDInsightTemplatesType getTemplateType() {
        return templateType;
    }
}
