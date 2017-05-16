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
package com.microsoft.intellij;

import com.intellij.openapi.project.Project;

public class ToolWindowKey {
    private Project project;
    private String toolWindowFactoryId;

    public ToolWindowKey(Project project, String toolWindowFactoryId) {
        this.project = project;
        this.toolWindowFactoryId = toolWindowFactoryId;
    }

    @Override
    public boolean equals(Object obj) {
        ToolWindowKey otherToolWindowKey = (ToolWindowKey) obj;
        if (otherToolWindowKey == null) {
            return false;
        }

        return (this.project == otherToolWindowKey.project) &&
                (this.toolWindowFactoryId == otherToolWindowKey.toolWindowFactoryId);
    }

    @Override
    public int hashCode() {
        return this.project.hashCode() * 5 + this.toolWindowFactoryId.hashCode();
    }
}