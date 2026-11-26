package servicios;

import modelo.Registro;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GestorRDF implements IGestorFicheros {

    private static final String NS = "http://gregoriofer.local/reto#";

    @Override
    public List<Registro> leer(String rutaArchivo) throws Exception {
        Model m = cargar(new File(rutaArchivo));
        Resource tipoRegistro = m.createResource(NS + "Registro");
        Property propOrden = m.createProperty(NS, "orden");
        Property propCampo = m.createProperty(NS, "campo");
        Property propNombre = m.createProperty(NS, "nombre");
        Property propItem = m.createProperty(NS, "item");
        Property propOrdenItem = m.createProperty(NS, "ordenItem");

        List<Resource> sujetos = m.listSubjectsWithProperty(RDF.type, tipoRegistro).toList();
        sujetos.sort(Comparator.comparingLong(r -> {
            Statement s = r.getProperty(propOrden);
            return s == null ? 0L : s.getLong();
        }));

        List<Registro> registros = new ArrayList<>();
        for (Resource r : sujetos) {
            Map<String, List<String>> camposMap = new LinkedHashMap<>();

            List<Resource> camposRecursos = new ArrayList<>();
            StmtIterator itCampos = r.listProperties(propCampo);
            while (itCampos.hasNext()) {
                RDFNode nodo = itCampos.nextStatement().getObject();
                if (nodo.isResource()) {
                    camposRecursos.add(nodo.asResource());
                }
            }

            for (Resource campoRes : camposRecursos) {
                Statement stNombre = campoRes.getProperty(propNombre);
                if (stNombre == null) continue;
                String nombreCampo = stNombre.getString();

                List<Statement> itemsSt = new ArrayList<>();
                StmtIterator itItems = campoRes.listProperties(propItem);
                while (itItems.hasNext()) {
                    itemsSt.add(itItems.nextStatement());
                }

                // Ordenamos los elementos del array si tienen reto:ordenItem
                itemsSt.sort(Comparator.comparingLong(st -> {
                    if (!st.getObject().isResource()) return 0L;
                    Statement ord = st.getObject().asResource().getProperty(propOrdenItem);
                    return ord == null ? 0L : ord.getLong();
                }));

                List<String> valores = new ArrayList<>();
                for (Statement st : itemsSt) {
                    if (st.getObject().isResource()) {
                        Statement valSt = st.getObject().asResource().getProperty(RDF.value);
                        valores.add(valSt != null ? valSt.getString() : "");
                    } else if (st.getObject().isLiteral()) {
                        valores.add(st.getObject().asLiteral().getString());
                    }
                }
                camposMap.put(nombreCampo, valores);
            }

            Registro reg = new Registro();
            reg.setCampos(camposMap);
            registros.add(reg);
        }
        return registros;
    }

    @Override
    public void escribir(String rutaArchivo, Registro reg) throws Exception {
        File f = new File(rutaArchivo);
        if (f.getParentFile() != null) {
            f.getParentFile().mkdirs();
        }

        Model m = cargar(f);
        Resource tipoRegistro = m.createResource(NS + "Registro");
        Resource tipoCampo = m.createResource(NS + "Campo");
        Resource tipoItem = m.createResource(NS + "Item");

        Property propOrden = m.createProperty(NS, "orden");
        Property propCampo = m.createProperty(NS, "campo");
        Property propNombre = m.createProperty(NS, "nombre");
        Property propItem = m.createProperty(NS, "item");
        Property propOrdenItem = m.createProperty(NS, "ordenItem");

        long siguiente = m.listSubjectsWithProperty(RDF.type, tipoRegistro).toList().size() + 1;
        Resource regRes = m.createResource(NS + "registro" + siguiente, tipoRegistro);
        regRes.addLiteral(propOrden, siguiente);

        int idxCampo = 0;
        for (Map.Entry<String, List<String>> entry : reg.getCampos().entrySet()) {
            idxCampo++;
            Resource campoRes = m.createResource(NS + "registro" + siguiente + "_campo" + idxCampo, tipoCampo);
            campoRes.addProperty(propNombre, entry.getKey());

            List<String> valores = entry.getValue();
            for (int i = 0; i < valores.size(); i++) {
                String val = valores.get(i) != null ? valores.get(i) : "";
                Resource itemRes = m.createResource(
                        NS + "registro" + siguiente + "_campo" + idxCampo + "_item" + (i + 1),
                        tipoItem
                );
                itemRes.addLiteral(propOrdenItem, (long) (i + 1));
                itemRes.addProperty(RDF.value, val);
                campoRes.addProperty(propItem, itemRes);
            }

            regRes.addProperty(propCampo, campoRes);
        }

        try (OutputStream out = new FileOutputStream(f)) {
            m.write(out, "RDF/XML");
        }
    }

    private Model cargar(File f) throws Exception {
        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefix("reto", NS);
        if (f.exists() && f.length() > 0) {
            try (InputStream in = new FileInputStream(f)) {
                m.read(in, NS, "RDF/XML");
            }
        }
        return m;
    }
}