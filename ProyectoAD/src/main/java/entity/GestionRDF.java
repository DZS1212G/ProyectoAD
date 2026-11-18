package entity;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GestionRDF {

	private static final String NS = "http://www.gregoriofer.com/ods#";

	public static List<DatosODS> leer(String rutaFichero) throws IOException {
		List<DatosODS> lista = new ArrayList<>();
		File archivo = new File(rutaFichero);
		if (!archivo.exists() || archivo.length() == 0) {
			return lista;
		}

		Model model = ModelFactory.createDefaultModel();
		try (InputStream in = new FileInputStream(archivo)) {
			RDFDataMgr.read(model, in, Lang.RDFXML);
		}

		Property p1 = model.createProperty(NS, "dato1");
		Property p2 = model.createProperty(NS, "dato2");
		Property p3 = model.createProperty(NS, "dato3");
		Property p4 = model.createProperty(NS, "dato4");
		Property p5 = model.createProperty(NS, "dato5");
		Property p6 = model.createProperty(NS, "dato6");

		ResIterator iter = model.listResourcesWithProperty(p1);
		while (iter.hasNext()) {
			Resource res = iter.nextResource();
			String d1 = getValorPropiedad(res, p1);
			String d2 = getValorPropiedad(res, p2);
			String d3 = getValorPropiedad(res, p3);
			String d4 = getValorPropiedad(res, p4);
			String d5 = getValorPropiedad(res, p5);
			String d6 = getValorPropiedad(res, p6);

			lista.add(new DatosODS(d1, d2, d3, d4, d5, d6));
		}
		return lista;
	}

	public static void escribir(String rutaFichero, DatosODS nuevoDato) throws IOException {
		Model model = ModelFactory.createDefaultModel();
		model.setNsPrefix("ods", NS);

		File archivo = new File(rutaFichero);
		if (archivo.exists() && archivo.length() > 0) {
			try (InputStream in = new FileInputStream(archivo)) {
				RDFDataMgr.read(model, in, Lang.RDFXML);
			}
		}

		Property p1 = model.createProperty(NS, "dato1");
		Property p2 = model.createProperty(NS, "dato2");
		Property p3 = model.createProperty(NS, "dato3");
		Property p4 = model.createProperty(NS, "dato4");
		Property p5 = model.createProperty(NS, "dato5");
		Property p6 = model.createProperty(NS, "dato6");

		Resource registro = model.createResource(NS + "registro_" + UUID.randomUUID());
		registro.addProperty(p1, nuevoDato.getDato1()).addProperty(p2, nuevoDato.getDato2())
				.addProperty(p3, nuevoDato.getDato3()).addProperty(p4, nuevoDato.getDato4())
				.addProperty(p5, nuevoDato.getDato5()).addProperty(p6, nuevoDato.getDato6());

		try (OutputStream out = new FileOutputStream(archivo)) {
			RDFDataMgr.write(out, model, Lang.RDFXML);
		}
	}

	private static String getValorPropiedad(Resource res, Property prop) {
		Statement stmt = res.getProperty(prop);
		return (stmt != null) ? stmt.getString() : "";
	}
}