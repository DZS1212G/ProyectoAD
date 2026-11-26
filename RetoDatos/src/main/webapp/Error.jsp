<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>


<%!
private String esc(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .replace("\n", "<br>"); 
}
%>

<%
    String tipoError = (String) request.getAttribute("tipoError");
    String detalleError = (String) request.getAttribute("detalleError");
    if (tipoError == null || tipoError.isEmpty()) {
        tipoError = "Error desconocido";
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Error</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css">
  <!-- 
   <style>
    body { font-family: Georgia, serif; margin: 2rem auto; max-width: 720px; text-align: center; }
    h1 { color: red; }
  </style>
   -->
 
</head>
<body>
  <h1>ERROR</h1>
  <h2> <%=esc(tipoError) %> </h2>
  <p><%=esc(detalleError) %></p>
  <p><a href="TratamientoFich.jsp">Volver al formulario</a></p>
</body>
</html>
