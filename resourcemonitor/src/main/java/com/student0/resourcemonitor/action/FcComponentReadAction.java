package com.student0.resourcemonitor.action;

import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.internal.scope.VariantScope;
import com.student0.resourcemonitor.config.NodeManager;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.jdom2.Attribute;
import org.jdom2.AttributeType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import groovy.util.Node;
import groovy.util.NodeList;
import groovy.util.XmlParser;

import static com.student0.resourcemonitor.config.ResourceMonitorExtension.DEFAULT_APPLICATION;

/**
 * @createDate: 6/27/21
 * @author: 飞彻
 * @description:
 */
public class FcComponentReadAction implements Action<Project> {

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
//                        NodeManager.Instance.setAllContext(findContextClass(manifestFile));
                        NodeManager.Instance.setAllContext(modifyXMLByJDOM(manifestFile));
                    }
                }
            });
        }
    }

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public Set<String> findContextClass(File file) {
        Set<String> contextSet = new HashSet<>();
        try {
            Node parse = new XmlParser(false, false).parse(file);
            Object application = parse.get("application");
            if (application instanceof NodeList) {
                for (Object o : ((NodeList) application)) {
                    if (o instanceof Node) {
                        Object attribute = ((Node) o).attribute("android:name");
                        if (attribute != null) {
                            contextSet.add(attribute.toString());
                        } else {
                            contextSet.add(DEFAULT_APPLICATION);
                        }
                        if (((Node) o).value() instanceof NodeList) {
                            NodeList componentList = (NodeList) ((Node) o).value();
                            for (Object c : componentList) {
                                Node component = (Node) c;
                                String compName = component.name().toString();
                                if ("activity".equals(compName) || "service".equals(compName)) {
                                    String compClassName = component.attribute("android:name").toString();
                                    contextSet.add(compClassName);
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return contextSet;
    }

    public Set<String> modifyXMLByJDOM(File path) {
        Set<String> contextSet = new HashSet<>();
        try {
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(path);
            Element application = doc.getRootElement().getChild("application");
            Namespace androidNamespace = Namespace.getNamespace("android", "http://schemas.android.com/apk/res/android");
            if (application.getAttribute("android:name") == null) {
                Attribute attrName = new Attribute(
                        "name",
                        DEFAULT_APPLICATION,
                        AttributeType.CDATA,
                        androidNamespace
                );
                application.setAttribute(attrName);
                contextSet.add(DEFAULT_APPLICATION);
            } else {
                String applicationName = application.getAttribute("name", androidNamespace).getValue();
                contextSet.add(applicationName);
            }
            List<Element> activityList = application.getChildren("activity");
            List<Element> servicesList = application.getChildren("services");
            activityList.addAll(servicesList);
            for (Element context : activityList) {
                String compClassName = context.getAttribute("name", androidNamespace).getValue();
                contextSet.add(compClassName);
            }
            FileWriter fileWriter = new FileWriter(path);
            XMLOutputter xmlOutputter = new XMLOutputter();
            xmlOutputter.setFormat(Format.getPrettyFormat());
            xmlOutputter.output(doc, fileWriter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contextSet;
    }
}
