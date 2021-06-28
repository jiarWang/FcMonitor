package com.student0.resourcemonitor.action;

import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.internal.scope.VariantScope;
import com.student0.resourcemonitor.config.NodeManager;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import groovy.util.Node;
import groovy.util.NodeList;
import groovy.util.XmlParser;

/**
 * @createDate: 6/27/21
 * @author: 飞彻
 * @description:
 */
public class FcComponentReadAction implements Action<Project>{

    @Override
    public void execute(Project project) {
        AppPlugin appPlugin = project.getPlugins().findPlugin(AppPlugin.class);

        for (VariantScope variantScope : appPlugin.getVariantManager().getVariantScopes()) {
            String variantName = capitalize(variantScope.getFullVariantName());
            if (variantName.toLowerCase().contains("test")) continue;

            final FileCollection files = project.getTasks().findByName("process" + variantName + "Manifest").getOutputs().getFiles();
            project.getTasks().findByName("compile" + variantName + "JavaWithJavac").doFirst(new Action<Task>() {
                @Override
                public void execute(Task task) {
                    for (File file : files) {
                        String manifest = file.getPath() + "/AndroidManifest.xml";
                        File manifestFile = new File(manifest);
                        if (!manifestFile.exists()) continue;
                        NodeManager.Instance.setAllContext(findContextClass(manifestFile));
                    }
                }
            });
        }
    }

    public static String capitalize(String str){
        if(str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public Set<String> findContextClass(File file){
        Set<String> contextSet = new HashSet<>();
        try {
            Node parse = new XmlParser(false, false).parse(file);
            Object application = parse.get("application");
            if (application instanceof NodeList){
                for (Object o : ((NodeList) application)) {
                    if (o instanceof Node){
                        Object attribute = ((Node) o).attribute("android:name");
                        if (attribute!= null){
                            contextSet.add(attribute.toString());
                        }
                        if (((Node) o).value() instanceof NodeList){
                            NodeList componentList = (NodeList) ((Node) o).value();
                            for (Object c : componentList) {
                                Node component = (Node) c;
                                String compName = component.name().toString();
                                if ("activity".equals(compName) || "service".equals(compName)){
                                    String compClassName = component.attribute("android:name").toString();
                                    contextSet.add(compClassName);
                                }
                            }
                        }
                    }
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return contextSet;
    }

}
