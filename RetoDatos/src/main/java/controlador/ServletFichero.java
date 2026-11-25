package controlador;

import modelo.Registro;
import servicios.GestorCSV;
import servicios.GestorJSON;
import servicios.GestorRDF;
import servicios.GestorXLS;
import servicios.GestorXML;
import servicios.IGestorFicheros;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Controlador central: TratamientoFich.jsp -> ServletFichero -> AccesoDatosA.jsp (o Error.jsp)
 */
import jakarta.servlet.annotation.MultipartConfig;

@WebServlet("/ServletFichero")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024) // máx. 10 MB
public class ServletFichero extends HttpServlet {
    private static final long serialVersionUID = 1L;

	// Evita que dos peticiones escriban el mismo fichero a la vez
	private static final Object LOCK = new Object();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.sendRedirect("TratamientoFich.jsp");
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		request.setCharacterEncoding("UTF-8");

		String formato = request.getParameter("formato"); // CSV, JSON, RDF, XLS, XML
		String accion = request.getParameter("accion"); // "lectura" o "escritura"
		
		System.out.println("ServletFichero.doPost()");
		String dato1 = request.getParameter("dato1");
		String dato2 = request.getParameter("dato2");
		String dato3 = request.getParameter("dato3");
		String dato4 = request.getParameter("dato4");
		String dato5 = request.getParameter("dato5");
		String dato6 = request.getParameter("dato6");

		if (!"lectura".equals(accion) && !"escritura".equals(accion)) {
			irAError(request, response, "Acción no válida", "Debe elegir lectura o escritura.");
			return;
		}

		// Validación solo en ESCRITURA
		if ("escritura".equals(accion) && (esVacio(dato1) || esVacio(dato2) || esVacio(dato3) || esVacio(dato4)
				|| esVacio(dato5) || esVacio(dato6))) {
			irAError(request, response, "Campos vacíos",
					"En modo escritura ninguno de los 6 campos puede quedar vacío.");
			return;
		}

		try {
			IGestorFicheros gestor = obtenerGestor(formato);
			String nombreFichero = obtenerNombreFichero(formato);

			String rutaWeb = getServletContext().getRealPath("/ficheros");
			System.out.println(rutaWeb);
			File carpeta = (rutaWeb != null) ? new File(rutaWeb)
					: new File(System.getProperty("java.io.tmpdir"), "ficheros_reto");
			if (!carpeta.exists() && !carpeta.mkdirs()) {
				throw new IOException("No se pudo crear la carpeta de ficheros: " + carpeta);
			}
			String rutaAbsoluta = new File(carpeta, nombreFichero).getAbsolutePath();

			Part fichero = request.getPart("fichero");
			boolean hayFicheroSubido = fichero != null && fichero.getSize() > 0;
			InputStream inputStreamSubido = null;

			if (hayFicheroSubido) {
				String subido = Paths.get(fichero.getSubmittedFileName()).getFileName().toString().toLowerCase();
				String extension = nombreFichero.substring(nombreFichero.lastIndexOf('.')); // ".csv", ".xlsx"...
				if (!subido.endsWith(extension)) {
					throw new IllegalArgumentException(
							"El fichero subido debe ser " + extension + " para el formato " + formato.toUpperCase());
				}
				inputStreamSubido = fichero.getInputStream();
			}

			List<Registro> lista;
			synchronized (LOCK) {
				if (hayFicheroSubido) {
					try (InputStream in = inputStreamSubido) {
						Files.copy(in, Path.of(rutaAbsoluta), StandardCopyOption.REPLACE_EXISTING);
					}
				}
				if ("escritura".equals(accion)) {
					gestor.escribir(rutaAbsoluta, new Registro(dato1, dato2, dato3, dato4, dato5, dato6));
				}
				lista = gestor.leer(rutaAbsoluta); // tras escribir, releemos para mostrar el nuevo registro
			}

			request.setAttribute("listaRegistros", lista);
			request.setAttribute("nombreFichero", nombreFichero);
			request.setAttribute("formato", formato.toUpperCase());
			request.setAttribute("accion", accion);
			if (rutaWeb != null) {
				request.setAttribute("enlaceFichero", "ficheros/" + nombreFichero);
			}
			request.getRequestDispatcher("AccesoDatosA.jsp").forward(request, response);

		} catch (Exception e) {
			log("Error al tratar el fichero", e);
			irAError(request, response, e.getClass().getSimpleName(),
					e.getMessage() != null ? e.getMessage() : "Error de lectura/escritura del fichero.");
		}
	}

	private void irAError(HttpServletRequest request, HttpServletResponse response, String tipo, String detalle)
			throws ServletException, IOException {
		request.setAttribute("tipoError", tipo);
		request.setAttribute("detalleError", detalle);
		request.getRequestDispatcher("Error.jsp").forward(request, response);
	}

	private boolean esVacio(String str) {
		return str == null || str.trim().isEmpty();
	}

	private IGestorFicheros obtenerGestor(String formato) {
		switch (formato != null ? formato.toUpperCase() : "") {
		case "CSV":
			return new GestorCSV();
		case "JSON":
			return new GestorJSON();
		case "XLS":
		case "XLSX":
			return new GestorXLS();
		case "XML":
			return new GestorXML();
		case "RDF":
			return new GestorRDF();
		default:
			throw new IllegalArgumentException("Formato no soportado: " + formato);
		}
	}

	private String obtenerNombreFichero(String formato) {
		switch (formato.toUpperCase()) {
		case "CSV":
			return "datos.csv";
		case "JSON":
			return "datos.json";
		case "XLS":
		case "XLSX":
			return "datos.xlsx";
		case "XML":
			return "datos.xml";
		case "RDF":
			return "datos.rdf";
		default:
			throw new IllegalArgumentException("Formato no soportado: " + formato);
		}
	}
}
