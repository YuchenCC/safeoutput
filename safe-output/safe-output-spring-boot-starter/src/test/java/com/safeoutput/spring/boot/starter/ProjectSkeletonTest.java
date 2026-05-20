package com.safeoutput.spring.boot.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class ProjectSkeletonTest {

    private static final List<String> MODULES = Arrays.asList(
            "safe-output-core",
            "safe-output-log4j2",
            "safe-output-report",
            "safe-output-spring-boot-starter",
            "safe-output-dashboard-spring-boot-starter",
            "safe-output-demo");

    @Test
    void sourceRootContainsExpectedMavenModulesUnderSourceEntrypoint() throws Exception {
        Path sourceRoot = sourceRoot();
        assertEquals("safe-output", sourceRoot.getFileName().toString());
        assertTrue(Files.exists(sourceRoot.resolve("pom.xml")));

        Set<String> declaredModules = childText(sourceRoot.resolve("pom.xml"), "module");
        assertEquals(new HashSet<String>(MODULES), declaredModules);
        for (String module : MODULES) {
            assertTrue(Files.exists(sourceRoot.resolve(module).resolve("pom.xml")), module);
        }
    }

    @Test
    void starterKeepsPublicCoordinatesAndAggregatesInternalModules() throws Exception {
        Path starterPom = sourceRoot().resolve("safe-output-spring-boot-starter/pom.xml");
        Document document = document(starterPom);

        assertEquals("safe-output-spring-boot-starter",
                childText(document.getDocumentElement(), "artifactId"));
        assertEquals(new HashSet<String>(Arrays.asList(
                "safe-output-core",
                "safe-output-log4j2",
                "safe-output-report",
                "spring-boot-autoconfigure",
                "spring-webmvc")), productionDependencyArtifactIds(document));
        assertFalse(hasDirectSpringBootDependencyVersion(document));
    }

    @Test
    void demoReferencesOnlyStarterFromSafeOutputModules() throws Exception {
        Path demoPom = sourceRoot().resolve("safe-output-demo/pom.xml");
        Document document = document(demoPom);

        Set<String> safeOutputDependencies = productionDependencyArtifactIds(document, "com.safeoutput");
        assertEquals(new HashSet<String>(Arrays.asList("safe-output-spring-boot-starter")),
                safeOutputDependencies);
    }

    private static boolean hasDirectSpringBootDependencyVersion(Document document) {
        NodeList dependencies = document.getElementsByTagName("dependency");
        for (int i = 0; i < dependencies.getLength(); i++) {
            Node dependency = dependencies.item(i);
            if ("org.springframework.boot".equals(childText(dependency, "groupId"))
                    && childText(dependency, "version") != null) {
                return true;
            }
        }
        return false;
    }

    private static Path sourceRoot() {
        return Paths.get("").toAbsolutePath().getParent();
    }

    private static Set<String> childText(Path pom, String tagName) throws Exception {
        Document document = document(pom);
        NodeList nodes = document.getElementsByTagName(tagName);
        Set<String> values = new HashSet<String>();
        for (int i = 0; i < nodes.getLength(); i++) {
            values.add(nodes.item(i).getTextContent().trim());
        }
        return values;
    }

    private static Set<String> productionDependencyArtifactIds(Document document) {
        return productionDependencyArtifactIds(document, null);
    }

    private static Set<String> productionDependencyArtifactIds(Document document, String groupId) {
        NodeList dependencies = document.getElementsByTagName("dependency");
        Set<String> values = new HashSet<String>();
        for (int i = 0; i < dependencies.getLength(); i++) {
            Node dependency = dependencies.item(i);
            if (!"test".equals(childText(dependency, "scope"))
                    && (groupId == null || groupId.equals(childText(dependency, "groupId")))) {
                values.add(childText(dependency, "artifactId"));
            }
        }
        return values;
    }

    private static String childText(Node node, String tagName) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (tagName.equals(child.getNodeName())) {
                return child.getTextContent().trim();
            }
        }
        return null;
    }

    private static Document document(Path pom) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(pom.toFile());
    }
}
