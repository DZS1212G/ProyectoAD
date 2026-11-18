package controller;


import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.util.List;

@WebServlet("/ServletFich")
public class ServletFich extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Nombre base de los ficheros dentro de webapp/ficheros/
    private static final String NOMBRE_BASE = "atmosfera_inventario_emisiones";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        procesarPeticion(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        procesarPeticion(request, response);
    }

    private void procesarPeticion(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String formato = request.getParameter("formato");     // XLS, JSON, RDF
        String operacion = request.getParameter("operacion"); // lectura, escritura

        try {
            // 1. Validación básica de selección
            if (formato == null || formato.isBlank() || operacion == null || operacion.isBlank()) {
                throw new IllegalArgumentException("Debe seleccionar un formato de fichero y una operación (Lectura o Escritura).");
            }

            String formatoNorm = formato.trim().toUpperCase();
            String operacionNorm = operacion.trim().toLowerCase();

            // 2. Resolver ruta física del fichero en el servidor
            String extension = obtenerExtension(formatoNorm);
            String rutaRelativa = "/ficheros/" + NOMBRE_BASE + "." + extension;
            String rutaAbsoluta = getServletContext().getRealPath(rutaRelativa);

            if (rutaAbsoluta == null) {
                throw new IOException("No se pudo resolver la ruta física del directorio /ficheros en el servidor.");
            }

            // Asegurar que la carpeta /ficheros exista
            File carpetaFicheros = new File(rutaAbsoluta).getParentFile();
            if (carpetaFicheros != null && !carpetaFicheros.exists()) {
                carpetaFicheros.mkdirs();
            }

            // 3. Ejecutar la operación solicitada
            if ("escritura".equals(operacionNorm)) {
                DatosODS nuevoRegistro = validarYConstruirDato(request);

                switch (formatoNorm) {
                    case "XLS" -> GestionXLS.escribir(rutaAbsoluta, nuevoRegistro);
                    case "JSON" -> GestionJSON.escribir(rutaAbsoluta, nuevoRegistro);
                    case "RDF" -> GestionRDF.escribir(rutaAbsoluta, nuevoRegistro);
                    default -> throw new UnsupportedOperationException("Formato no soportado todavía: " + formatoNorm);
                }

                // Tras escribir, leemos el fichero actualizado para mostrar el resultado con el nuevo registro
                List<DatosODS> listaActualizada = leerPorFormato(formatoNorm, rutaAbsoluta);
                request.setAttribute("listaDatos", listaActualizada);
                request.setAttribute("formatoSeleccionado", formatoNorm);
                request.setAttribute("mensajeExito", "Registro añadido correctamente al fichero " + NOMBRE_BASE + "." + extension);

                request.getRequestDispatcher("AccesoDatosA.jsp").forward(request, response);

            } else if ("lectura".equals(operacionNorm)) {
                List<DatosODS> listaLeida = leerPorFormato(formatoNorm, rutaAbsoluta);

                request.setAttribute("listaDatos", listaLeida);
                request.setAttribute("formatoSeleccionado", formatoNorm);

                request.getRequestDispatcher("AccesoDatosA.jsp").forward(request, response);

            } else {
                throw new IllegalArgumentException("Operación no válida: " + operacion);
            }

        } catch (Exception e) {
            // Cualquier error de validación o E/S redirige a Error.jsp con el detalle
            request.setAttribute("tipoError", e.getClass().getSimpleName() + ": " + e.getMessage());
            request.getRequestDispatcher("Error.jsp").forward(request, response);
        }
    }

    private List<DatosODS> leerPorFormato(String formatoNorm, String rutaAbsoluta) throws IOException {
        File fichero = new File(rutaAbsoluta);
        if (!fichero.exists()) {
            throw new IOException("El fichero no existe en la ruta: " + rutaAbsoluta);
        }

        return switch (formatoNorm) {
            case "XLS" -> GestionXLS.leer(rutaAbsoluta);
            case "JSON" -> GestionJSON.leer(rutaAbsoluta);
            case "RDF" -> GestionRDF.leer(rutaAbsoluta);
            default -> throw new UnsupportedOperationException("Formato no soportado todavía: " + formatoNorm);
        };
    }

    private DatosODS validarYConstruirDato(HttpServletRequest request) {
        String d1 = limpiar(request.getParameter("dato1"));
        String d2 = limpiar(request.getParameter("dato2"));
        String d3 = limpiar(request.getParameter("dato3"));
        String d4 = limpiar(request.getParameter("dato4"));
        String d5 = limpiar(request.getParameter("dato5"));
        String d6 = limpiar(request.getParameter("dato6"));

        if (d1.isEmpty() || d2.isEmpty() || d3.isEmpty() || d4.isEmpty() || d5.isEmpty() || d6.isEmpty()) {
            throw new IllegalArgumentException("(*) Todos los campos (DATO1 a DATO6) son obligatorios para realizar una escritura.");
        }

        return new DatosODS(d1, d2, d3, d4, d5, d6);
    }

    private String obtenerExtension(String formatoNorm) {
        return switch (formatoNorm) {
            case "XLS" -> "xls";
            case "JSON" -> "json";
            case "RDF" -> "rdf";
            default -> throw new IllegalArgumentException("Formato desconocido: " + formatoNorm);
        };
    }

    private String limpiar(String valor) {
        return (valor == null) ? "" : valor.trim();
    }
}
}
