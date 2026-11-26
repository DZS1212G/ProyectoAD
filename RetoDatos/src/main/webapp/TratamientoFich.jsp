<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8"
         import="java.util.ArrayList, java.util.List, modelo.Accion, modelo.Formato, modelo.FilaFormulario" %>
<%!
    // Escapa HTML para poder volver a pintar en los campos lo que escribió el usuario
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    // Estado del formulario: lo prepara ServletFichero al pulsar los botones de la tabla.
    // Si se entra directamente a esta página no hay atributos y se muestra una fila vacía.
    @SuppressWarnings("unchecked")
    List<FilaFormulario> filas = (List<FilaFormulario>) request.getAttribute("filas");
    if (filas == null || filas.isEmpty()) {
        filas = new ArrayList<>();
        filas.add(new FilaFormulario(1, "", null));
    }
    int siguienteId = 2;
    Object sigAttr = request.getAttribute("siguienteId");
    if (sigAttr instanceof Integer) {
        siguienteId = (Integer) sigAttr;
    }
    String formSel = (String) request.getAttribute("formFormato");
    String accSel = (String) request.getAttribute("formAccion");

    // Nº máximo de valores de una fila: sirve para alinear la tabla
    int maxValores = 1;
    for (FilaFormulario f : filas) {
        maxValores = Math.max(maxValores, f.getValores().size());
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Tratamiento de ficheros</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css">
  <style>
    /* Botón "por defecto" para la tecla Intro: existe pero no se ve */
    .oculto { position: absolute; left: -9999px; width: 1px; height: 1px; overflow: hidden; }

    /* Solo ESCRITURA usa la tabla: se oculta en LECTURA sin necesidad de JavaScript.
       (Si un navegador antiguo no entiende :has, la tabla simplemente queda siempre visible.) */
    form:has(#accion option[value="LECTURA"]:checked) #editor { display: none; }

    #editor { min-width: 0; max-width: 100%; }
    .scroll { overflow-x: auto; max-width: 100%; }

    #tabla-datos { border-collapse: separate; border-spacing: 0; width: max-content; min-width: 100%; }
    #tabla-datos th, #tabla-datos td {
      border-right: 1px solid #999; border-bottom: 1px solid #999;
      padding: .4rem .5rem; vertical-align: middle; text-align: left; box-sizing: border-box;
    }
    #tabla-datos tbody tr, #tabla-datos tbody tr:hover { background: #fff; }
    #tabla-datos td { background: #fff; }

    /* Columnas fijas (Nº y Clave): al hacer scroll solo se mueven los valores */
    #tabla-datos .c-num   { position: sticky; left: 0; z-index: 2;
                            width: 7.5rem; min-width: 7.5rem; max-width: 7.5rem; }
    #tabla-datos .c-clave { position: sticky; left: 7.5rem; z-index: 2;
                            width: 13rem; min-width: 13rem; box-shadow: 2px 0 3px rgba(0,0,0,.12); }

    #tabla-datos td.val   { min-width: 12.5rem; }
    #tabla-datos td.mas   { white-space: nowrap; }

    #tabla-datos input[type=text] { display: block; width: 100%; box-sizing: border-box; margin: 0; }

    /* Botones pequeños y sin solaparse: la × va encima de cada valor */
    #tabla-datos button { width: auto; margin: 0; padding: .25rem .6rem; font-size: .85rem;
                          line-height: 1.3; box-shadow: none; }
    #tabla-datos .x { display: block; margin: 0 0 .3rem auto; padding: 0 .5rem; font-size: 1.05rem;
                      background: #b3261e; color: #fff; border: 0; border-radius: 6px; }
    #tabla-datos .fila-del { display: block; white-space: nowrap; }
    #tabla-datos .x:disabled, #tabla-datos .fila-del:disabled { opacity: .35; cursor: not-allowed; }
    #tabla-datos .etq { display: block; color: #666; font-size: .85rem; margin-bottom: .35rem; }
  </style>
</head>
<body>
  <h1>TRATAMIENTO FICHEROS</h1>
  <form id="formulario" action="ServletFichero" method="POST" enctype="multipart/form-data">
    <!-- Primer botón del formulario = el que se activa al pulsar Intro. Solo repinta, no envía nada. -->
    <button type="submit" name="op" value="refrescar" class="oculto" tabindex="-1" aria-hidden="true">Refrescar</button>

    <div class="fila">
      <div>
        <label for="formato">Formato del fichero:
          <select name="formato" id="formato">
            <% for (Formato fmt : Formato.values()) { %>
              <option value="<%= fmt.name() %>" <%= fmt.name().equals(formSel) ? "selected" : "" %>><%= fmt.name() %></option>
            <% } %>
          </select>
        </label>
        <hr>
        <p>¿Qué quiere hacer con el fichero?</p>
        <select name="accion" id="accion">
          <% for (Accion acc : Accion.values()) { %>
            <option value="<%= acc.name() %>" <%= acc.name().equals(accSel) ? "selected" : "" %>><%= acc.name() %></option>
          <% } %>
        </select>
      </div>
    </div>

    <!-- Editor de claves y valores (solo se usa en ESCRITURA) -->
    <div id="editor">
      <div class="scroll">
        <table id="tabla-datos">
          <thead>
            <tr>
              <th class="c-num">Nº</th>
              <th class="c-clave">Clave</th>
              <th colspan="<%= maxValores + 1 %>">Valores</th>
            </tr>
          </thead>
          <tbody>
            <% int numero = 0; %>
            <% for (FilaFormulario fila : filas) {
                 numero++;
                 List<String> valores = fila.getValores();
                 int n = valores.size();
            %>
              <tr>
                <td class="c-num">
                  <span class="etq">Dato <%= numero %></span>
                  <button type="submit" name="op" value="delFila:<%= fila.getId() %>" class="fila-del"
                          title="Eliminar esta fila" <%= filas.size() == 1 ? "disabled" : "" %>>Quitar fila</button>
                </td>
                <td class="c-clave">
                  <input type="hidden" name="fila" value="<%= fila.getId() %>">
                  <input type="text" name="clave_<%= fila.getId() %>" value="<%= esc(fila.getClave()) %>" placeholder="Clave">
                </td>
                <% for (int i = 0; i < n; i++) { %>
                  <td class="val">
                    <button type="submit" name="op" value="delValor:<%= fila.getId() %>:<%= i %>" class="x"
                            title="Quitar este valor" <%= n == 1 ? "disabled" : "" %>>×</button>
                    <input type="text" name="valor_<%= fila.getId() %>" value="<%= esc(valores.get(i)) %>" placeholder="Valor">
                  </td>
                <% } %>
                <td class="mas">
                  <button type="submit" name="op" value="addValor:<%= fila.getId() %>">+ valor</button>
                </td>
                <% if (n < maxValores) { %>
                  <td class="relleno" colspan="<%= maxValores - n %>"></td>
                <% } %>
              </tr>
            <% } %>
          </tbody>
        </table>
      </div>
      <input type="hidden" name="siguienteId" value="<%= siguienteId %>">
      <p><button type="submit" name="op" value="addFila">+ Añadir clave</button></p>
      <p class="aviso">(*) Claves y valores no pueden estar vacíos; las claves no pueden repetirse.</p>
    </div>

    <p><button type="submit" name="op" value="enviar">Enviar</button></p>
    <hr>
    <label for="fichero">Subir fichero (opcional):
      <input type="file" name="fichero" id="fichero" accept=".csv,.json,.xlsx,.xml,.rdf">
    </label>
    <p><small>Si pulsas un botón de la tabla la página se recarga y el fichero elegido se pierde:
      selecciónalo justo antes de pulsar «Enviar».</small></p>
  </form>
</body>
</html>
