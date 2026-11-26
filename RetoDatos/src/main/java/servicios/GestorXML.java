package servicios;

import modelo.Registro;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GestorXML implements IGestorFicheros {

    @Override
    public List<Registro> leer(String rutaArchivo) throws Exception {
        List<Registro> registros = new ArrayList<>();
        Document doc = cargar(new File(rutaArchivo));
        NodeList nodosRegistro = doc.getElementsByTagName("registro");

        for (int i = 0; i < nodosRegistro.getLength(); i++) {
            Element regElem = (Element) nodosRegistro.item(i);
            Registro r = new Registro();

            NodeList camposNodes = regElem.getElementsByTagName("campo");
            for (int j = 0; j < camposNodes.getLength(); j++) {
                Element campoElem = (Element) camposNodes.item(j);
                String nombreCampo = campoElem.getAttribute("nombre");

                List<String> valores = new ArrayList<>();
                NodeList items = campoElem.getElementsByTagName("item");
                for (int k = 0; k < items.getLength(); k++) {
                    valores.add(items.item(k).getTextContent());
                }
                r.setCampo(nombreCampo, valores);
            }
            registros.add(r);
        }
        return registros;
    }

    @Override
    public void escribir(String rutaArchivo, Registro reg) throws Exception {
        File f = new File(rutaArchivo);
        Document doc = cargar(f);

        Element regElem = doc.createElement("registro");
        for (Map.Entry<String, List<String>> entry : reg.getCampos().entrySet()) {
            Element campoElem = doc.createElement("campo");
            campoElem.setAttribute("nombre", entry.getKey());

            for (String val : entry.getValue()) {
                Element itemElem = doc.createElement("item");
                itemElem.setTextContent(val != null ? val : "");
                campoElem.appendChild(itemElem);
            }
            regElem.appendChild(campoElem);
        }

        doc.getDocumentElement().appendChild(regElem);
        guardar(doc, f);
    }

    private Document cargar(File f) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder db = dbf.newDocumentBuilder();

        if (!f.exists() || f.length() == 0) {
            Document doc = db.newDocument();
            doc.appendChild(doc.createElement("registros"));
            return doc;
        }
        Document doc = db.parse(f);
        limpiarEspacios(doc.getDocumentElement());
        return doc;
    }

    private void guardar(Document doc, File f) throws Exception {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        tf.transform(new DOMSource(doc), new StreamResult(f));
    }

    private void limpiarEspacios(Node nodo) {
        NodeList hijos = nodo.getChildNodes();
        for (int i = hijos.getLength() - 1; i >= 0; i--) {
            Node h = hijos.item(i);
            if (h.getNodeType() == Node.TEXT_NODE && h.getTextContent().trim().isEmpty()) {
                nodo.removeChild(h);
            } else {
                limpiarEspacios(h);
            }
        }
    }
}