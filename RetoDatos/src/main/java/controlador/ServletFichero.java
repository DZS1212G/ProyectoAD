package controlador;

import modelo.Accion;
import modelo.FilaFormulario;
import modelo.Formato;
import modelo.Registro;
import servicios.GestorCSV;
import servicios.GestorJSON;
import servicios.GestorRDF;
import servicios.GestorXLS;
import servicios.GestorXML;
import servicios.IGestorFicheros;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controlador central: TratamientoFich.jsp -> ServletFichero -> AccesoDatosA.jsp (o Error.jsp)
 *
 * Protocolo de parámetros del formulario dinámico (una fila = una clave con uno o más valores):
 *   fila         -> un campo oculto por fila, con el identificador de la fila (en orden)
 *   clave_ID     -> la clave de la fila ID
 *   valor_ID     -> un campo por cada valor de la fila ID (puede haber varios con el mismo nombre)
 *   siguienteId  -> contador para dar identificadores nuevos a las filas
 *   op           -> botón pulsado: "enviar" (guardar/leer) o una operación sobre la tabla:
 *                   addFila | addValor:ID | delValor:ID:POSICION | delFila:ID | refrescar
 *
 * Sin JavaScript, cada botón de la tabla envía el formulario aquí; el servlet aplica la
 * operación y vuelve a mostrar TratamientoFich.jsp conservando lo que el usuario ya había escrito.
 */
@WebServlet("/ServletFichero")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024) // máx. 10 MB
public class ServletFichero extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String[] accionesPosibles = Accion.getValues();
	private static final String[] formatosPosibles = Formato.getValues();
	// Evita que dos peticiones escriban el mismo fichero a la vez
	private static final Object LOCK = new Object();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		doPost(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");

		// 1. Validar 'formato' y 'accion' (que lleguen, que sean un único valor y que existan)
		String msjError = "";
		if (!esValorUnicoValido(request, "accion", accionesPosibles)) {
			msjError += "El parámetro 'accion' debe ser un único valor entre: "
					+ Arrays.toString(accionesPosibles) + "\n";
		}
		if (!esValorUnicoValido(request, "formato", formatosPosibles)) {
			msjError += "El parámetro 'formato' debe ser un único valor entre: "
					+ Arrays.toString(formatosPosibles) + "\n";
		}
		if (!msjError.isBlank()) {
			irAError(request, response, "Petición no válida", msjError);
			return;
		}

		String accion = request.getParameter("accion").trim().toLowerCase(); // "lectura" o "escritura"
		String formato = request.getParameter("formato").trim();

		// 1b. Si se ha pulsado un botón de la tabla (no "Enviar"), solo editamos el formulario y lo repintamos
		String op = request.getParameter("op");
		if (op != null && !"enviar".equals(op)) {
			gestionarEdicion(request, response, op, formato.toUpperCase(), accion.toUpperCase());
			return;
		}

		// 2. En ESCRITURA, construir el registro a partir de las filas clave/valores del formulario
		Registro nuevoRegistro = null;
		if ("escritura".equals(accion)) {
			try {
				nuevoRegistro = construirRegistro(request);
			} catch (IllegalArgumentException e) {
				irAError(request, response, "Datos no válidos", e.getMessage());
				return;
			}
		}

		try {
			IGestorFicheros gestor = obtenerGestor(formato);
			String nombreFichero = obtenerNombreFichero(formato);

			String rutaWeb = getServletContext().getRealPath("/ficheros");
			File carpeta = (rutaWeb != null) ? new File(rutaWeb)
					: new File(System.getProperty("java.io.tmpdir"), "ficheros_reto");
			if (!carpeta.exists() && !carpeta.mkdirs()) {
				throw new IOException("No se pudo crear la carpeta de ficheros: " + carpeta);
			}
			String rutaAbsoluta = new File(carpeta, nombreFichero).getAbsolutePath();

			// Fichero subido (solo si la petición es multipart; un GET o un POST normal no lo es)
			String tipoContenido = request.getContentType();
			boolean esMultipart = tipoContenido != null && tipoContenido.toLowerCase().startsWith("multipart/");
			Part fichero = esMultipart ? request.getPart("fichero") : null;
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
				if (nuevoRegistro != null) {
					gestor.escribir(rutaAbsoluta, nuevoRegistro);
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

	/**
	 * Lee las filas dinámicas del formulario y las convierte en un Registro (clave -> lista de valores).
	 * Lanza IllegalArgumentException con un mensaje claro si algo no es válido.
	 */
	private Registro construirRegistro(HttpServletRequest request) {
		String[] ids = request.getParameterValues("fila");
		if (ids == null || ids.length == 0) {
			throw new IllegalArgumentException("Debe añadir al menos una clave con su valor.");
		}

		Registro registro = new Registro();
		Set<String> clavesUsadas = new HashSet<>();

		for (int i = 0; i < ids.length; i++) {
			String id = ids[i] == null ? "" : ids[i].trim();
			String clave = request.getParameter("clave_" + id);
			if (esVacio(clave)) {
				throw new IllegalArgumentException("La clave de la fila " + (i + 1) + " está vacía.");
			}
			clave = clave.trim();
			if (!clavesUsadas.add(clave)) {
				throw new IllegalArgumentException("La clave '" + clave + "' está repetida.");
			}

			String[] valores = request.getParameterValues("valor_" + id);
			if (valores == null || valores.length == 0) {
				throw new IllegalArgumentException("La clave '" + clave + "' no tiene ningún valor.");
			}
			List<String> lista = new ArrayList<>();
			for (String valor : valores) {
				if (esVacio(valor)) {
					throw new IllegalArgumentException("La clave '" + clave + "' tiene algún valor vacío.");
				}
				lista.add(valor.trim());
			}
			registro.setCampo(clave, lista);
		}
		return registro;
	}

	/**
	 * Aplica una operación de edición sobre las filas del formulario y vuelve a mostrar TratamientoFich.jsp.
	 */
	private void gestionarEdicion(HttpServletRequest request, HttpServletResponse response, String op,
			String formato, String accion) throws ServletException, IOException {
		List<FilaFormulario> filas = leerFilas(request);

		int siguiente = parsearEntero(request.getParameter("siguienteId"), 1);
		for (FilaFormulario f : filas) {
			siguiente = Math.max(siguiente, f.getId() + 1);
		}

		String[] partes = op.split(":");
		switch (partes[0]) {
		case "addFila":
			filas.add(new FilaFormulario(siguiente++, "", null));
			break;
		case "addValor": {
			FilaFormulario f = buscarFila(filas, partes);
			if (f != null) {
				f.getValores().add("");
			}
			break;
		}
		case "delValor": {
			FilaFormulario f = buscarFila(filas, partes);
			int pos = partes.length > 2 ? parsearEntero(partes[2], -1) : -1;
			// siempre debe quedar al menos un valor en la fila
			if (f != null && f.getValores().size() > 1 && pos >= 0 && pos < f.getValores().size()) {
				f.getValores().remove(pos);
			}
			break;
		}
		case "delFila": {
			FilaFormulario f = buscarFila(filas, partes);
			// siempre debe quedar al menos una fila
			if (f != null && filas.size() > 1) {
				filas.remove(f);
			}
			break;
		}
		default: // "refrescar" (p. ej. al pulsar Intro) u operación desconocida: no cambia nada
			break;
		}

		request.setAttribute("filas", filas);
		request.setAttribute("siguienteId", siguiente);
		request.setAttribute("formFormato", formato);
		request.setAttribute("formAccion", accion);
		request.getRequestDispatcher("TratamientoFich.jsp").forward(request, response);
	}

	/** Reconstruye las filas tal y como las dejó el usuario (sin validar ni recortar nada). */
	private List<FilaFormulario> leerFilas(HttpServletRequest request) {
		List<FilaFormulario> filas = new ArrayList<>();
		String[] ids = request.getParameterValues("fila");
		if (ids != null) {
			for (String idTexto : ids) {
				int id = parsearEntero(idTexto, -1);
				if (id < 0) {
					continue;
				}
				String clave = request.getParameter("clave_" + id);
				String[] valores = request.getParameterValues("valor_" + id);
				filas.add(new FilaFormulario(id, clave, valores == null ? null : Arrays.asList(valores)));
			}
		}
		if (filas.isEmpty()) {
			filas.add(new FilaFormulario(1, "", null));
		}
		return filas;
	}

	/** Busca la fila cuyo id viene en partes[1] (p. ej. "addValor:3"). */
	private FilaFormulario buscarFila(List<FilaFormulario> filas, String[] partes) {
		int id = partes.length > 1 ? parsearEntero(partes[1], -1) : -1;
		for (FilaFormulario f : filas) {
			if (f.getId() == id) {
				return f;
			}
		}
		return null;
	}

	private int parsearEntero(String texto, int porDefecto) {
		try {
			return Integer.parseInt(texto.trim());
		} catch (Exception e) {
			return porDefecto;
		}
	}

	private boolean esValorUnicoValido(HttpServletRequest request, String nombre, String[] posibles) {
		String[] valores = request.getParameterValues(nombre);
		return valores != null && valores.length == 1 && valores[0] != null
				&& Arrays.asList(posibles).contains(valores[0].trim().toUpperCase());
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
