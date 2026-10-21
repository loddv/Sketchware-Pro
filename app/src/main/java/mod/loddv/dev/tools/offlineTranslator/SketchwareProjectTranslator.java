package mod.loddv.dev.tools.offlineTranslator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public final class SketchwareProjectTranslator {

    private static final Pattern ANDROID_LOCALE_QUALIFIER =
            Pattern.compile("[a-z]{2,3}(-r[A-Z]{2})?");

    private final BatchTranslatorManager batchTranslatorManager;

    public SketchwareProjectTranslator(BatchTranslatorManager batchTranslatorManager) {
        if (batchTranslatorManager == null) {
            throw new NullPointerException("batchTranslatorManager");
        }
        this.batchTranslatorManager = batchTranslatorManager;
    }

    /**
     * Translates a project's default strings into a locale-specific resource file.
     *
     * @param projectDirectory project directory containing {@code files/resource/values/strings.xml}
     * @param localeQualifier Android locale qualifier, for example {@code pt-rBR}
     * @return the generated locale-specific strings file
     */
    public File translateStrings(File projectDirectory, String localeQualifier) throws IOException {
        if (projectDirectory == null) {
            throw new NullPointerException("projectDirectory");
        }
        if (localeQualifier == null || !ANDROID_LOCALE_QUALIFIER.matcher(localeQualifier).matches()) {
            throw new IllegalArgumentException(
                    "localeQualifier must be a language or language-region qualifier, such as pt-rBR");
        }

        File sourceFile = new File(projectDirectory, "files/resource/values/strings.xml");
        if (!sourceFile.isFile()) {
            throw new IOException("Project strings file does not exist: " + sourceFile);
        }

        File outputFile = new File(projectDirectory,
                "files/resource/values-" + localeQualifier + "/strings.xml");
        File temporaryFile = null;
        try {
            Document document = parse(sourceFile);
            List<Element> translatableStrings = getTranslatableStrings(document);
            List<String> sourceTexts = new ArrayList<>(translatableStrings.size());
            for (Element stringElement : translatableStrings) {
                sourceTexts.add(stringElement.getTextContent());
            }

            List<String> translations = batchTranslatorManager.translate(sourceTexts);
            for (int i = 0; i < translatableStrings.size(); i++) {
                translatableStrings.get(i).setTextContent(translations.get(i));
            }

            File outputDirectory = outputFile.getParentFile();
            if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs()
                    && !outputDirectory.isDirectory()) {
                throw new IOException("Could not create translated resource directory: " + outputDirectory);
            }
            temporaryFile = Files.createTempFile(outputDirectory.toPath(), "strings-", ".xml").toFile();
            write(document, temporaryFile);
            replaceOutput(temporaryFile, outputFile);
            temporaryFile = null;
            return outputFile;
        } catch (ParserConfigurationException | SAXException | TransformerException e) {
            throw new IOException("Could not process project strings file: " + sourceFile, e);
        } catch (Exception e) {
            throw new IOException("Could not translate project strings file: " + sourceFile, e);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile.toPath());
                } catch (IOException ignored) {
                    // Keep the original translation error if cleanup also fails.
                }
            }
        }
    }

    private static Document parse(File sourceFile)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(sourceFile);
    }

    private static List<Element> getTranslatableStrings(Document document) {
        List<Element> translatableStrings = new ArrayList<>();
        Element resources = document.getDocumentElement();
        NodeList children = resources.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE
                    || !"string".equals(child.getNodeName())) {
                continue;
            }

            Element stringElement = (Element) child;
            if ("false".equalsIgnoreCase(stringElement.getAttribute("translatable"))
                    || !hasOnlyTextContent(stringElement)
                    || stringElement.getTextContent().trim().isEmpty()) {
                continue;
            }
            translatableStrings.add(stringElement);
        }
        return translatableStrings;
    }

    private static boolean hasOnlyTextContent(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            short nodeType = children.item(i).getNodeType();
            if (nodeType != Node.TEXT_NODE && nodeType != Node.CDATA_SECTION_NODE) {
                return false;
            }
        }
        return true;
    }

    private static void write(Document document, File destination) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(document), new StreamResult(destination));
    }

    private static void replaceOutput(File temporaryFile, File outputFile) throws IOException {
        try {
            Files.move(temporaryFile.toPath(), outputFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporaryFile.toPath(), outputFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
