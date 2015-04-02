package ru.taximaxim.codekeeper.ui;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ru.taximaxim.codekeeper.ui.localizations.Messages;

/**
 * Serializes and deserializes a {@link List} of Strings in/from XML.<br><br>
 * Example XML:<br>
 * <code>
 * &lt;rootTagName&gt;<br>
 * &lt;elementTagName&gt;item1&lt;/elementTagName&gt;<br>
 * &lt;elementTagName&gt;item2&lt;/elementTagName&gt;<br>
 * &lt;!-- ... --&gt;<br>
 * &lt;/rootTagName&gt;<br>
 * </code>
 */
public class XmlStringList {
    
    private static final String PROPERTY_NAME = "name"; //$NON-NLS-1$
    
    private final String rootTagName;
    private final String elementTagName;
    private String elementSetTagName;
    
    public XmlStringList(String rootTagName, String elementTagName) {
        this.rootTagName = rootTagName;
        this.elementTagName = elementTagName;
    }
    
    public XmlStringList(String rootTagName, String elementTagName, 
            String elementSetTagName) {
        this(rootTagName, elementTagName);
        this.elementSetTagName = elementSetTagName;
    }
    
    public LinkedList<String> deserializeList(Reader reader) throws IOException, SAXException {
        return readList(readXml(reader));
    }
    
    public Map<String, List<String>> deserializeMap(Reader reader) 
            throws IOException, SAXException {
        return readMap(readXml(reader));
    }

    public <T> void serializeList(List<T> listToConvert, boolean noFormatting,
            Writer writer) throws TransformerException, IOException {
        Document xml;
        try {
            xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException ex) {
            throw new IOException(ex);
        }
        
        Element root = xml.createElement(rootTagName);
        xml.appendChild(root);
        
        for (T listElement : listToConvert) {
            Element newElement = xml.createElement(elementTagName);
            newElement.setTextContent(listElement.toString());
            root.appendChild(newElement);
        }
        
        Transformer tf =  TransformerFactory.newInstance().newTransformer();
        if (!noFormatting) {
            tf.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        tf.transform(new DOMSource(xml), new StreamResult(writer));
    }
    
    public void serializeMap(Map<String, List<String>> mapToConvert, boolean noFormatting,
            Writer writer) throws TransformerException, IOException {
        Document xml;
        try {
            xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException ex) {
            throw new IOException(ex);
        }
        
        Element root = xml.createElement(rootTagName);
        xml.appendChild(root);
        
        for (String key : mapToConvert.keySet()) {
            Element keyElement = xml.createElement(elementSetTagName);
            keyElement.setAttribute(PROPERTY_NAME, key);
            root.appendChild(keyElement);
            for (String listElement : mapToConvert.get(key)) {
                Element newElement = xml.createElement(elementTagName);
                newElement.setTextContent(listElement);
                keyElement.appendChild(newElement);
            }
        }
        
        Transformer tf =  TransformerFactory.newInstance().newTransformer();
        if (!noFormatting) {
            tf.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        tf.transform(new DOMSource(xml), new StreamResult(writer));
    }
    /**
     * Reads (well-formed) list XML and checks it for basic validity:
     * root node must be <code>&lt;rootTagName&gt;</code>
     */
    private Document readXml(Reader reader) throws IOException, SAXException {
        try {
            Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(reader));
            xml.normalize();
            
            if (!xml.getDocumentElement().getNodeName().equals(rootTagName)) {
                throw new IOException(Messages.XmlStringList_root_name_invalid);
            }
            
            return xml;
        } catch (ParserConfigurationException ex) {
            throw new IOException(ex);
        }
    }
    
    private LinkedList<String> readList(Document xml) {
        LinkedList<String> list = new LinkedList<>();
        
        Element root = (Element) xml.getElementsByTagName(rootTagName).item(0);
        
        NodeList nlItems = root.getElementsByTagName(elementTagName);
        for (int i = 0; i < nlItems.getLength(); ++i) {
            Node nListItem = nlItems.item(i);
            if (nListItem.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            list.add(nListItem.getTextContent());
        }
        
        return list;
    }
    
    private Map<String, List<String>> readMap(Document xml) {
        Map<String, List<String>> lists = new LinkedHashMap<>();
        
        Element root = (Element) xml.getElementsByTagName(rootTagName).item(0);
        NodeList nList = root.getChildNodes();
        for (int i = 0; i < nList.getLength(); i++) {
            List<String> list = new LinkedList<>();
            Node node = nList.item(i);
            
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                NodeList nElements = ((Element)node).getElementsByTagName(elementTagName);
                for (int j = 0; j < nElements.getLength(); j++) {
                    list.add(nElements.item(j).getTextContent());
                }
                if (!list.isEmpty()) {
                    lists.put(node.getAttributes().getNamedItem(PROPERTY_NAME)
                            .getNodeValue(), list);
                }
            }
        }
        
        return lists;
    }
}
