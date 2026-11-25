<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="es">
<head>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css">
  <meta charset="UTF-8">
  <title>Tratamiento de ficheros</title>
  
  <!-- 
  <style>
    body { font-family: Georgia, serif; margin: 2rem auto; max-width: 720px; }
    h1 { text-align: center; }
    .fila { display: flex; gap: 3rem; }
    label { display: block; margin: .3rem 0; }
    .aviso { color: red; font-weight: bold; margin-top: 1rem; }
    hr { margin: .8rem 0; }
  </style>
   -->
  
</head>
<body>
  <h1>TRATAMIENTO FICHEROS</h1>
  <form action="ServletFichero" method="POST" enctype="multipart/form-data">
    <div class="fila">
      <div>
        <label for="formato">Formato del fichero:
          <select name="formato" id="formato">
            <option value="XLS">XLS</option>
            <option value="CSV">CSV</option>
            <option value="JSON">JSON</option>
            <option value="XML">XML</option>
            <option value="RDF">RDF</option>
          </select>
        </label>
        <hr>
        <p>¿Qué quiere hacer con el fichero?</p>
        <label><input type="radio" name="accion" value="lectura" checked> Lectura</label>
        <label><input type="radio" name="accion" value="escritura"> Escritura</label>
      </div>
      <div>
        <label>DATO1: <input type="text" name="dato1"></label>
        <label>DATO2: <input type="text" name="dato2"></label>
        <label>DATO3: <input type="text" name="dato3"></label>
        <label>DATO4: <input type="text" name="dato4"></label>
        <label>DATO5: <input type="text" name="dato5"></label>
        <label>DATO6: <input type="text" name="dato6"></label>
        <p class="aviso">(*) Los campos no pueden estar vacíos (en escritura)</p>
      </div>
    </div>
    <p><input type="submit" value="Enviar"></p>
    <hr>
<label for="fichero">Subir fichero (opcional):
  <input type="file" name="fichero" id="fichero"
         accept=".csv,.json,.xlsx,.xml,.rdf">
</label>
  </form>
</body>
</html>
