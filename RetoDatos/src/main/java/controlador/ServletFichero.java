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

	
	/**
	 * Handles HTTP POST requests for dynamic form editing and file read/write operations.
	 * <p>
	 * Validates the {@code accion} and {@code formato} parameters, intercepts intermediate
	 * table editing actions (when {@code op} is present and not {@code "enviar"}), and
	 * delegates full file processing to {@link #processFileAction(HttpServletRequest, HttpServletResponse, String, String)}.
	 * </p>
	 *
	 * @param request  The {@link HttpServletRequest} containing form parameters and optional multipart file uploads.
	 * @param response The {@link HttpServletResponse} used to forward the request to the appropriate JSP view.
	 * @throws ServletException If a servlet-specific error occurs during request processing or forwarding.
	 * @throws IOException      If an I/O error occurs during request processing or forwarding.
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
	    request.setCharacterEncoding("UTF-8");

	    if (validateFormatAction(request, response)) {
	        
	    

	    String action = request.getParameter("accion").trim().toLowerCase(); // "lectura" or "escritura"
	    String format = request.getParameter("formato").trim();

	    // If a table editing button was clicked (not "enviar"), update the form state and re-render
	    String operation = request.getParameter("op");
	    if (operation != null && !"enviar".equals(operation)) {
	        handleFormEditing(request, response, operation, format.toUpperCase(), action.toUpperCase());
	        return;
	    }

	    // Delegate file handling (upload validation, synchronized read/write, and view forwarding)
	    processFileAction(request, response, format, action);
	    } 
	}

	
	private boolean validateFormatAction(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		// 1. Validar 'formato' y 'accion' (que lleguen, que sean un único valor y que existan)
				String msjError = "";
				if (!isValidSingleParameter(request, "accion", accionesPosibles)) {
					msjError += "El parámetro 'accion' debe ser un único valor entre: "
							+ Arrays.toString(accionesPosibles) + "\n";
				}
				if (!isValidSingleParameter(request, "formato", formatosPosibles)) {
					msjError += "El parámetro 'formato' debe ser un único valor entre: "
							+ Arrays.toString(formatosPosibles) + "\n";
				}
				if (!msjError.isBlank()) {
					forwardToErrorPage(request, response, "Petición no válida", msjError);
					return false;
				}
				return true;
	}
	
	
	
	/**
	 * Resolves and ensures the existence of the target storage directory for data files.
	 * Falls back to the system temporary directory if the servlet context real path is unavailable.
	 *
	 * @return The resolved {@link Path} pointing to the existing storage directory.
	 * @throws IOException If the target directory does not exist and cannot be created.
	 */
	private Path resolveStorageDirectory() throws IOException {
	    String webRealPath = getServletContext().getRealPath("/ficheros");
	    Path storageDir = (webRealPath != null)
	            ? Path.of(webRealPath)
	            : Path.of(System.getProperty("java.io.tmpdir"), "ficheros_reto");

	    if (Files.notExists(storageDir)) {
	        Files.createDirectories(storageDir);
	    }
	    return storageDir;
	}

	
	/**
	 * Extracts and validates an uploaded file from a multipart HTTP request if present.
	 *
	 * @param request        The incoming {@link HttpServletRequest}.
	 * @param targetFilename The expected target filename used to verify the file extension.
	 * @param format         The requested file format (used for error reporting).
	 * @return The uploaded {@link Part} if a non-empty file was submitted; {@code null} otherwise.
	 * @throws IOException              If an I/O error occurs while retrieving the multipart data.
	 * @throws ServletException         If the request is not multipart or part retrieval fails.
	 * @throws IllegalArgumentException If the uploaded file's extension does not match the expected format.
	 */
	private Part extractValidatedUploadedFile(HttpServletRequest request, String targetFilename, String format)
	        throws IOException, ServletException {
	    String contentType = request.getContentType();
	    boolean isMultipart = contentType != null && contentType.toLowerCase().startsWith("multipart/");
	    if (!isMultipart) {
	        return null;
	    }

	    Part uploadedPart = request.getPart("fichero");
	    if (uploadedPart == null || uploadedPart.getSize() == 0) {
	        return null;
	    }

	    String submittedFilename = Path.of(uploadedPart.getSubmittedFileName()).getFileName().toString().toLowerCase();
	    String expectedExtension = targetFilename.substring(targetFilename.lastIndexOf('.'));
	    if (!submittedFilename.endsWith(expectedExtension)) {
	        throw new IllegalArgumentException(
	                "El fichero subido debe ser " + expectedExtension + " para el formato " + format.toUpperCase());
	    }

	    return uploadedPart;
	}

	
	/**
	 * Performs thread-safe file operations: replaces the target file if an upload is provided,
	 * appends/writes the new record if present, and reads back the full list of records.
	 *
	 * @param fileHandler    The {@link IGestorFicheros} implementation for the target format.
	 * @param targetFilePath The absolute path to the target data file.
	 * @param uploadedPart   An optional uploaded file {@link Part} to overwrite the target file before processing.
	 * @param newRecord      An optional {@link Registro} to write to the file.
	 * @return The updated {@link List} of {@link Registro} entries read from the file.
	 * @throws Exception If an error occurs during file copying, writing, or reading.
	 */
	private List<Registro> executeSynchronizedFileOperation(IGestorFicheros fileHandler, String targetFilePath,
	        Part uploadedPart, Registro newRecord) throws Exception {
	    synchronized (LOCK) {
	        if (uploadedPart != null) {
	            try (InputStream inputStream = uploadedPart.getInputStream()) {
	                Files.copy(inputStream, Path.of(targetFilePath), StandardCopyOption.REPLACE_EXISTING);
	            }
	        }
	        if (newRecord != null) {
	            fileHandler.escribir(targetFilePath, newRecord);
	        }
	        return fileHandler.leer(targetFilePath);
	    }
	}

	
	/**
	 * Processes file read and write requests, handling optional multipart file uploads,
	 * persisting new form records when requested, and forwarding the resulting records to the view.
	 *
	 * @param request  The {@link HttpServletRequest} containing action parameters and optional multipart data.
	 * @param response The {@link HttpServletResponse} used to forward to the results or error view.
	 * @param format   The target file format (e.g., {@code "CSV"}, {@code "JSON"}, {@code "XLSX"}).
	 * @param action   The operation mode ({@code "escritura"} for write, or read-only otherwise).
	 * @throws ServletException If a servlet-specific error occurs during request forwarding.
	 * @throws IOException      If an I/O error occurs during request forwarding.
	 */
	private void processFileAction(HttpServletRequest request, HttpServletResponse response,
	        String format, String action) throws ServletException, IOException {
	    // 1. In WRITE mode, build and validate the new record from form fields
	    Registro newRecord = null;
	    if ("escritura".equals(action)) {
	        try {
	            newRecord = buildRecord(request);
	        } catch (IllegalArgumentException e) {
	            forwardToErrorPage(request, response, "Datos no válidos", e.getMessage());
	            return;
	        }
	    }

	    try {
	        IGestorFicheros fileHandler = createFileHandler(format);
	        String outputFilename = resolveOutputFilename(format);
	        Path storageDir = resolveStorageDirectory();
	        String targetFilePath = storageDir.resolve(outputFilename).toAbsolutePath().toString();

	        Part uploadedPart = extractValidatedUploadedFile(request, outputFilename, format);
	        List<Registro> records = executeSynchronizedFileOperation(
	                fileHandler, targetFilePath, uploadedPart, newRecord);

	        request.setAttribute("listaRegistros", records);
	        request.setAttribute("nombreFichero", outputFilename);
	        request.setAttribute("formato", format.toUpperCase());
	        request.setAttribute("accion", action);
	        if (getServletContext().getRealPath("/ficheros") != null) {
	            request.setAttribute("enlaceFichero", "ficheros/" + outputFilename);
	        }

	        request.getRequestDispatcher("AccesoDatosA.jsp").forward(request, response);

	    } catch (Exception e) {
	        log("Error al tratar el fichero", e);
	        forwardToErrorPage(request, response, e.getClass().getSimpleName(),
	                e.getMessage() != null ? e.getMessage() : "Error de lectura/escritura del fichero.");
	    }
	}

	
	/**
	 * Parses and validates dynamic form rows from the HTTP request, building a populated {@link Registro}
	 * instance mapping each unique key to its corresponding list of non-empty values.
	 *
	 * @param request The {@link HttpServletRequest} containing the submitted form fields ({@code fila},
	 *                {@code clave_<id>}, and {@code valor_<id>}).
	 * @return A validated {@link Registro} object populated with the submitted keys and values.
	 * @throws IllegalArgumentException If no rows are submitted, any key or value is blank,
	 *                                  a key has no associated values, or duplicate keys are detected.
	 */
	private Registro buildRecord(HttpServletRequest request) {
	    String[] rowIds = request.getParameterValues("fila");
	    if (rowIds == null || rowIds.length == 0) {
	        throw new IllegalArgumentException("Debe añadir al menos una clave con su valor.");
	    }

	    Registro record = new Registro();
	    Set<String> usedKeys = new HashSet<>();

	    for (int i = 0; i < rowIds.length; i++) {
	        String rowId = (rowIds[i] == null) ? "" : rowIds[i].trim();
	        String key = request.getParameter("clave_" + rowId);
	        if (esVacio(key)) {
	            throw new IllegalArgumentException("La clave de la fila " + (i + 1) + " está vacía.");
	        }
	        key = key.trim();
	        if (!usedKeys.add(key)) {
	            throw new IllegalArgumentException("La clave '" + key + "' está repetida.");
	        }

	        String[] rawValues = request.getParameterValues("valor_" + rowId);
	        if (rawValues == null || rawValues.length == 0) {
	            throw new IllegalArgumentException("La clave '" + key + "' no tiene ningún valor.");
	        }

	        List<String> sanitizedValues = new ArrayList<>();
	        for (String rawValue : rawValues) {
	            if (esVacio(rawValue)) {
	                throw new IllegalArgumentException("La clave '" + key + "' tiene algún valor vacío.");
	            }
	            sanitizedValues.add(rawValue.trim());
	        }
	        record.setCampo(key, sanitizedValues);
	    }
	    return record;
	}


	/**
	 * Handles dynamic form editing operations (adding or removing rows and values)
	 * and forwards the updated form state back to the view.
	 * <p>
	 * Supported operations in the {@code operation} parameter (formatted as {@code action[:rowId[:valueIndex]]}):
	 * <ul>
	 *   <li>{@code addFila}: Appends a new empty row with the next available ID.</li>
	 *   <li>{@code addValor}: Appends a new empty value field to the specified row.</li>
	 *   <li>{@code delValor}: Removes the value at the specified index, ensuring at least one value remains.</li>
	 *   <li>{@code delFila}: Removes the specified row, ensuring at least one row remains.</li>
	 *   <li>Any other value (e.g., {@code refrescar}): Preserves the current state without structural changes.</li>
	 * </ul>
	 * </p>
	 *
	 * @param request   The {@link HttpServletRequest} containing the current form data and parameters.
	 * @param response  The {@link HttpServletResponse} used to forward the request to the JSP view.
	 * @param operation The encoded editing command string (e.g., {@code "addFila"}, {@code "delValor:2:0"}).
	 * @param format    The selected file format to preserve across the request.
	 * @param action    The selected form action to preserve across the request.
	 * @throws ServletException If a servlet-specific error occurs during request forwarding.
	 * @throws IOException      If an I/O error occurs during request forwarding.
	 */
	private void handleFormEditing(HttpServletRequest request, HttpServletResponse response, String operation,
	        String format, String action) throws ServletException, IOException {
	    List<FilaFormulario> rows = parseFormRows(request);

	    int nextRowId = parseIntOrDefault(request.getParameter("siguienteId"), 1);
	    for (FilaFormulario row : rows) {
	        nextRowId = Math.max(nextRowId, row.getId() + 1);
	    }

	    String[] commandTokens = operation.split(":");
	    switch (commandTokens[0]) {
	        case "addFila":
	            rows.add(new FilaFormulario(nextRowId++, "", null));
	            break;
	        case "addValor": {
	            FilaFormulario targetRow = findRowById(rows, commandTokens);
	            if (targetRow != null) {
	                targetRow.getValores().add("");
	            }
	            break;
	        }
	        case "delValor": {
	            FilaFormulario targetRow = findRowById(rows, commandTokens);
	            int valueIndex = commandTokens.length > 2 ? parseIntOrDefault(commandTokens[2], -1) : -1;
	            // Ensure at least one value always remains in the row
	            if (targetRow != null && targetRow.getValores().size() > 1
	                    && valueIndex >= 0 && valueIndex < targetRow.getValores().size()) {
	                targetRow.getValores().remove(valueIndex);
	            }
	            break;
	        }
	        case "delFila": {
	            FilaFormulario targetRow = findRowById(rows, commandTokens);
	            // Ensure at least one row always remains in the form
	            if (targetRow != null && rows.size() > 1) {
	                rows.remove(targetRow);
	            }
	            break;
	        }
	        default: // "refrescar" (e.g., pressing Enter) or unknown operation: keep state unchanged
	            break;
	    }

	    request.setAttribute("filas", rows);
	    request.setAttribute("siguienteId", nextRowId);
	    request.setAttribute("formFormato", format);
	    request.setAttribute("formAccion", action);
	    request.getRequestDispatcher("TratamientoFich.jsp").forward(request, response);
	}


	/** Reconstruye las filas tal y como las dejó el usuario (sin validar ni recortar nada). */
	/**
	 * Extracts and parses dynamic form rows from the incoming HTTP servlet request.
	 * <p>
	 * Reads the {@code "fila"} request parameter to identify row IDs, then retrieves
	 * the corresponding key ({@code "clave_<id>"}) and values ({@code "valor_<id>"})
	 * for each valid row. If no valid rows are found in the request, a default empty
	 * row with ID 1 is initialized and returned.
	 * </p>
	 *
	 * @param request The {@link HttpServletRequest} containing the submitted form parameters.
	 * @return A non-empty {@link List} of {@link FilaFormulario} objects parsed from the request.
	 */
	private List<FilaFormulario> parseFormRows(HttpServletRequest request) {
	    List<FilaFormulario> rows = new ArrayList<>();
	    String[] rawRowIds = request.getParameterValues("fila");

	    if (rawRowIds != null) {
	        for (String rawId : rawRowIds) {
	            int rowId = parseIntOrDefault(rawId, -1);
	            if (rowId < 0) {
	                continue;
	            }
	            String key = request.getParameter("clave_" + rowId);
	            String[] rawValues = request.getParameterValues("valor_" + rowId);
	            List<String> values = (rawValues == null) ? null : Arrays.asList(rawValues);
	            
	            rows.add(new FilaFormulario(rowId, key, values));
	        }
	    }

	    if (rows.isEmpty()) {
	        rows.add(new FilaFormulario(1, "", null));
	    }

	    return rows;
	}


	/**
	 * Searches for a specific {@link FilaFormulario} within a list by extracting the target ID
	 * from the provided command tokens.
	 *
	 * @param rows          The list of {@link FilaFormulario} objects to search through.
	 * @param commandTokens An array of string tokens where the second element (index 1) 
	 *                      is expected to contain the target row ID.
	 * @return The matching {@link FilaFormulario} if found; {@code null} otherwise or if the ID is invalid.
	 */
	private FilaFormulario findRowById(List<FilaFormulario> rows, String[] commandTokens) {
	    int targetId = commandTokens.length > 1 ? parseIntOrDefault(commandTokens[1], -1) : -1;
	    for (FilaFormulario row : rows) {
	        if (row.getId() == targetId) {
	            return row;
	        }
	    }
	    return null;
	}

	/**
	 * @param text the text to be parsed
	 * @param default_ the dafult value to be returned
	 * @return returns the parsed text as a primitive int or the default value
	 * in case it could not cast succesfully 
	 */
	private int parseIntOrDefault(String text, int default_) {
		try {
			return Integer.parseInt(text.trim());
		} catch (Exception e) {
			return default_;
		}
	}

	/**
	 * Validates that the specified HTTP request parameter is present as a single non-null value
	 * and matches one of the allowed uppercase options (ignoring leading and trailing whitespace).
	 *
	 * @param request        The {@link HttpServletRequest} containing the parameters to validate.
	 * @param parameterName  The name of the request parameter to inspect.
	 * @param allowedValues  An array of valid uppercase string values to check against.
	 * @return {@code true} if the parameter has exactly one value that matches one of the allowed
	 *         options (case-insensitive after trimming); {@code false} otherwise.
	 */
	private boolean isValidSingleParameter(HttpServletRequest request, String parameterName, String[] allowedValues) {
	    String[] parameterValues = request.getParameterValues(parameterName);
	    return parameterValues != null
	            && parameterValues.length == 1
	            && parameterValues[0] != null
	            && Arrays.asList(allowedValues).contains(parameterValues[0].trim().toUpperCase());
	}

	
	/**
	 * Forwards the current request to the error view ({@code Error.jsp}), populating
	 * the request scope with the specified error category and detailed message.
	 *
	 * @param request      The {@link HttpServletRequest} in which the error attributes will be stored.
	 * @param response     The {@link HttpServletResponse} used to forward the request.
	 * @param errorType    The high-level category or title of the error (stored as {@code "tipoError"}).
	 * @param errorDetails A descriptive message explaining the cause of the error (stored as {@code "detalleError"}).
	 * @throws ServletException If a servlet-specific error occurs while forwarding to the error view.
	 * @throws IOException      If an I/O error occurs while forwarding to the error view.
	 */
	private void forwardToErrorPage(HttpServletRequest request, HttpServletResponse response,
	        String errorType, String errorDetails) throws ServletException, IOException {
	    request.setAttribute("tipoError", errorType);
	    request.setAttribute("detalleError", errorDetails);
	    request.getRequestDispatcher("Error.jsp").forward(request, response);
	}

	

	private boolean esVacio(String str) {
		return str == null || str.trim().isEmpty();
	}

	/**
	 * Instantiates the appropriate {@link IGestorFicheros} implementation for the requested file format.
	 *
	 * @param format The target file format identifier (case-insensitive, e.g., {@code "CSV"},
	 *               {@code "JSON"}, {@code "XLS"}, {@code "XLSX"}, {@code "XML"}, {@code "RDF"}).
	 * @return A concrete {@link IGestorFicheros} instance capable of handling the specified format.
	 * @throws IllegalArgumentException If the provided format is {@code null} or unsupported.
	 */
	private IGestorFicheros createFileHandler(String format) {
	    switch (format != null ? format.toUpperCase() : "") {
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
	            throw new IllegalArgumentException("Formato no soportado: " + format);
	    }
	}


	/**
	 * Resolves the default output filename corresponding to the specified export format.
	 *
	 * @param format The target file format identifier (case-insensitive, e.g., {@code "CSV"},
	 *               {@code "JSON"}, {@code "XLS"}, {@code "XLSX"}, {@code "XML"}, {@code "RDF"}).
	 * @return The standardized filename with its appropriate extension (e.g., {@code "datos.csv"}).
	 * @throws IllegalArgumentException If the provided format is unsupported.
	 */
	private String resolveOutputFilename(String format) {
	    switch (format.toUpperCase()) {
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
	            throw new IllegalArgumentException("Formato no soportado: " + format);
	    }
	}

}
