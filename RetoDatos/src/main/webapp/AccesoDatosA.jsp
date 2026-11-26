<%@page import="java.util.Set"%>
<%@page import="java.util.LinkedHashSet"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8"
         import="java.util.List, modelo.Registro" %>
<%!
    // Escapa HTML para que lo escrito por el usuario se muestre como texto y no se ejecute
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    @SuppressWarnings("unchecked")
    List<Registro> lista = (List<Registro>) request.getAttribute("listaRegistros");
    String nombreFichero = (String) request.getAttribute("nombreFichero");
    String formato = (String) request.getAttribute("formato");
    String accion = (String) request.getAttribute("accion");
    String enlaceFichero = (String) request.getAttribute("enlaceFichero");
	
    Set<String> claves = new LinkedHashSet<>();
    if (lista != null) {
        for (Registro r : lista) {
            claves.addAll(r.getCampos().keySet());
        }
    }

%>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Datos</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css">
</head>
<body>
  <h1>DATOS</h1>
  <p>Fichero: <strong><%= esc(nombreFichero) %></strong> (formato <%= esc(formato) %>)</p>

  <% if ("escritura".equals(accion)) { %>
    <p class="ok">Registro guardado correctamente. Contenido actualizado del fichero:</p>
  <% } %>

  <% if (lista == null || lista.isEmpty()) { %>
    <p>El fichero todavía no contiene registros.</p>
  <% } else { %>
    <table>
      <tr>
      	<th>Nº</th>
        <% for (String clave : claves) { %>
          <th><%= esc(clave) %></th>
        <% } %>     
      </tr>
      
      <% int numero = 0; %>
      <% for (Registro r : lista) { numero++; %>
        <tr>
          <td><%= numero %></td>
          <% for (String clave : claves) { %>
            <% List<String> valores = r.getValores(clave); %>
            <td>
              <% if (valores.size() == 1) { %>
                <%= esc(valores.get(0)) %>
              <% } else if (valores.size() > 1) { %>
                <ul style="margin:0;padding-left:1.2rem">
                  <% for (String valor : valores) { %>
                    <li><%= esc(valor) %></li>
                  <% } %>
                </ul>
              <% } %>
            </td>
          <% } %>
        </tr>
      <% } %>
    </table>
  <% } %>

  <p>
    <% if (enlaceFichero != null) { %>
      <a class="btn-secundario" href="<%= esc(enlaceFichero) %>" target="_blank">Abrir / descargar el fichero</a> |
    <% } %>
    <a class="btn-secundario" href="TratamientoFich.jsp">Volver</a>
  </p>
</body>
</html>
