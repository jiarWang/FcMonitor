package com.student0.resourcemonitor;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.internal.tasks.factory.TaskFactory;
import com.android.build.gradle.internal.tasks.factory.TaskFactoryImpl;
import com.student0.resourcemonitor.action.FcComponentReadAction;
import com.student0.resourcemonitor.config.ResourceMonitorExtension;
import com.student0.resourcemonitor.transform.FcGetResourceTransform;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ResourceProxyPlugin implements Plugin<Project> {

    private TaskFactory mTaskFactory;

    @Override
    public void apply(Project project) {
        mTaskFactory = new TaskFactoryImpl(project.getTasks());

        project.getExtensions().create("resourceMonitor", ResourceMonitorExtension.class);
        AppExtension appExtension = project.getExtensions().findByType(AppExtension.class);
        project.afterEvaluate(new FcComponentReadAction());
        appExtension.registerTransform(new FcGetResourceTransform(project));
    }
}