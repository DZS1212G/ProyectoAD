package servicios;

import modelo.Registro;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * RDF/XML con Apache Jena. Cada Registro es un recurso de tipo reto:Registro con las
 * propiedades reto:dato1..reto:dato6 y reto:orden (para conservar el orden de inserción).
 */
public class GestorRDF implements IGestorFicheros {

    private static final String NS = "http://gregoriofer.local/reto#";

    @Override
    public List<Registro> leer(String rutaArchivo) throws Exception {
        Model m = cargar(new File(rutaArchivo));
        Resource tipo = m.createResource(NS + "Registro");
        Property orden = m.createProperty(NS, "orden");

        List<Resource> sujetos = m.listSubjectsWithProperty(RDF.type, tipo).toList();
        sujetos.sort(Comparator.comparingLong(r -> {
            Statement s = r.getProperty(orden);
            return s == null ? 0L : s.getLong();
        }));

        List<Registro> registros = new ArrayList<>();
        for (Resource r : sujetos) {
            registros.add(new Registro(
                    valor(m, r, "dato1"), valor(m, r, "dato2"), valor(m, r, "dato3"),
                    valor(m, r, "dato4"), valor(m, r, "dato5"), valor(m, r, "dato6")));
        }
        return registros;
    }

    @Override
    public void escribir(String rutaArchivo, Registro reg) throws Exception {
        File f = new File(rutaArchivo);
        Model m = cargar(f);
        Resource tipo = m.createResource(NS + "Registro");

        long siguiente = m.listSubjectsWithProperty(RDF.type, tipo).toList().size() + 1;
        Resource r = m.createResource(NS + "registro" + siguiente, tipo);
        r.addLiteral(m.createProperty(NS, "orden"), siguiente);

        String[] datos = {
            reg.getDato1(), reg.getDato2(), reg.getDato3(),
            reg.getDato4(), reg.getDato5(), reg.getDato6()
        };
        for (int i = 0; i < datos.length; i++) {
            r.addProperty(m.createProperty(NS, "dato" + (i + 1)), datos[i]);
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

    private String valor(Model m, Resource r, String propiedad) {
        Statement s = r.getProperty(m.createProperty(NS, propiedad));
        return s == null ? "" : s.getString();
    }
}
