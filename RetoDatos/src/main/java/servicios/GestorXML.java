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

/**
 * XML con DOM. Estructura:
 * <registros><registro><dato1>..</dato1>...<dato6>..</dato6></registro>...</registros>
 */
public class GestorXML implements IGestorFicheros {

    @Override
    public List<Registro> leer(String rutaArchivo) throws Exception {
        List<Registro> registros = new ArrayList<>();
        Document doc = cargar(new File(rutaArchivo));
        NodeList nodos = doc.getElementsByTagName("registro");
        for (int i = 0; i < nodos.getLength(); i++) {
            Element e = (Element) nodos.item(i);
            registros.add(new Registro(
                    texto(e, "dato1"), texto(e, "dato2"), texto(e, "dato3"),
                    texto(e, "dato4"), texto(e, "dato5"), texto(e, "dato6")));
        }
        return registros;
    }

    @Override
    public void escribir(String rutaArchivo, Registro reg) throws Exception {
        File f = new File(rutaArchivo);
        Document doc = cargar(f);
        Element nuevo = doc.createElement("registro");
        String[] datos = {
            reg.getDato1(), reg.getDato2(), reg.getDato3(),
            reg.getDato4(), reg.getDato5(), reg.getDato6()
        };
        for (int i = 0; i < datos.length; i++) {
            Element hijo = doc.createElement("dato" + (i + 1));
            hijo.setTextContent(datos[i]);
            nuevo.appendChild(hijo);
        }
        doc.getDocumentElement().appendChild(nuevo);
        guardar(doc, f);
    }

    /** Carga el XML; si no existe devuelve un documento nuevo con la raíz <registros>. */
    private Document cargar(File f) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // evita XXE
        DocumentBuilder db = dbf.newDocumentBuilder();

        if (!f.exists() || f.length() == 0) {
            Document doc = db.newDocument();
            doc.appendChild(doc.createElement("registros"));
            return doc;
        }
        Document doc = db.parse(f);
        limpiarEspacios(doc.getDocumentElement()); // evita que se acumulen líneas en blanco al re-indentar
        return doc;
    }

    private void guardar(Document doc, File f) throws Exception {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        tf.transform(new DOMSource(doc), new StreamResult(f));
    }

    private String texto(Element e, String etiqueta) {
        NodeList n = e.getElementsByTagName(etiqueta);
        return n.getLength() == 0 ? "" : n.item(0).getTextContent();
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
