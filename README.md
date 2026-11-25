# RetoDatos: Manejo de ficheros con JEE

Proyecto del módulo **Acceso a Datos (2º DAM, UT2 – Reto grupal)**, Centro Gregorio Fernández.

Aplicación web JEE que permite **leer y escribir datos en cinco formatos de fichero** (XLS/XLSX, CSV, JSON, XML y RDF) desde un formulario, mostrando el resultado en una tabla HTML y gestionando los errores en una página específica.

---

## 1. Qué hace la aplicación

El usuario, desde el navegador:

1. Elige el **formato** del fichero: XLS, CSV, JSON, XML o RDF.
2. Elige la **acción**: *Lectura* o *Escritura*.
3. Opcionalmente, **sube un fichero** propio para leerlo.
4. En *Escritura*, rellena **6 campos** (`DATO1`…`DATO6`), que se añaden como un nuevo registro al final del fichero.

Resultado:

- **Lectura** → tabla con todos los registros del fichero.
- **Escritura** → se guarda el registro, se vuelve a leer el fichero y se muestra la tabla actualizada, con un enlace para abrir el fichero.
- **Cualquier error** (campos vacíos, formato no soportado, fichero corrupto, error de E/S…) → página `Error.jsp` con el tipo de error y su mensaje.

---

## 2. Tecnologías utilizadas

| Tecnología | Versión | Para qué se usa |
|---|---|---|
| **Java** | 17 (`maven.compiler.release`) | Lenguaje de toda la lógica |
| **Jakarta EE (Servlets 6.0)** | `jakarta.servlet-api` 6.0.0 | Controlador web (`HttpServlet`) |
| **JSP** | La incluida en Tomcat | Vistas dinámicas (formulario, tabla, error) |
| **Apache Tomcat** | 10.1 o superior | Contenedor de servlets donde se despliega el `.war` |
| **Maven** | Plugins fijados en el `pom.xml` | Gestión de dependencias y empaquetado como `war` |
| **Apache POI** | 5.3.0 | Leer/escribir Excel (`.xlsx`) |
| **OpenCSV** | 5.9 | Leer/escribir CSV |
| **Jackson Databind** | 2.18.1 | Leer/escribir JSON |
| **DOM (`javax.xml.parsers`)** | Incluido en el JDK | Leer/escribir XML |
| **Apache Jena** | 5.1.0 (`apache-jena-libs`) | Leer/escribir RDF |
| **Eclipse IDE** | – | Desarrollo y *Run on Server* |
| **Git / GitHub** | – | Control de versiones y entrega |

> Tomcat 10.1+ es obligatorio porque el código usa el paquete `jakarta.servlet.*`. Con Tomcat 9 o anterior (que usa `javax.servlet.*`) daría errores de clase no encontrada.

---

## 3. Arquitectura: patrón MVC

La aplicación sigue el patrón **Modelo–Vista–Controlador**, con una capa extra de **servicios** para aislar la entrada/salida de cada formato.

```
┌─────────────────────┐   POST    ┌──────────────────┐   forward   ┌──────────────────┐
│ TratamientoFich.jsp │ ────────▶ │ ServletFichero   │ ──────────▶ │ AccesoDatosA.jsp │
│   (Vista: form)     │           │  (Controlador)   │             │ (Vista: tabla)   │
└─────────────────────┘           └───────┬──────────┘             └──────────────────┘
                                          │  │ forward (si hay fallo)
                                          │  └────────────────────▶ Error.jsp
                                          ▼
                                ┌────────────────────┐
                                │  IGestorFicheros   │  ◀── interfaz común
                                └─────────┬──────────┘
          ┌────────────┬─────────────┬────┴───────┬─────────────┐
          ▼            ▼             ▼            ▼             ▼
     GestorCSV    GestorJSON    GestorXLS    GestorXML     GestorRDF
          └────────────┴─────────────┴────────────┴─────────────┘
                                          │
                                   ficheros/datos.*
                                          ▲
                                   modelo.Registro (POJO)
```

### Estructura de directorios

```
RetoDatos/
├── pom.xml
└── src/main/
    ├── java/
    │   ├── controlador/
    │   │   └── ServletFichero.java
    │   ├── modelo/
    │   │   └── Registro.java
    │   └── servicios/
    │       ├── IGestorFicheros.java
    │       ├── GestorCSV.java
    │       ├── GestorJSON.java
    │       ├── GestorXLS.java
    │       ├── GestorXML.java
    │       └── GestorRDF.java
    └── webapp/
        ├── TratamientoFich.jsp
        ├── AccesoDatosA.jsp
        ├── Error.jsp
        └── ficheros/          ← aquí se crean datos.csv, datos.json, ...
```

---

## 4. Componentes en detalle

### 4.1 Modelo: `Registro`

POJO con seis atributos `String` (`dato1`…`dato6`), constructor vacío, constructor completo y getters/setters. Es el **único tipo de dato** que circula por la aplicación: los gestores lo producen al leer y lo consumen al escribir, y las JSP lo recorren para pintar la tabla.

El constructor vacío y los getters/setters no son decorativos: **Jackson** los necesita para convertir un `Registro` a JSON y viceversa.

### 4.2 Contrato: `IGestorFicheros`

```java
List<Registro> leer(String rutaArchivo) throws Exception;
void escribir(String rutaArchivo, Registro nuevoRegistro) throws Exception;
```

Dos operaciones, mismas para todos los formatos. El servlet solo conoce esta interfaz, nunca una librería concreta.

### 4.3 Gestores (uno por formato)

Todos comparten el mismo comportamiento:

- **Si el fichero no existe o está vacío**, `leer` devuelve una lista vacía (no lanza excepción).
- **`escribir` añade** un registro sin perder los anteriores.
- Todo se lee y escribe en **UTF-8**.

| Gestor | Librería | Cómo funciona |
|---|---|---|
| `GestorCSV` | OpenCSV | `CSVReader` recorre línea a línea y crea un `Registro` por cada línea con al menos 6 columnas. Para escribir abre el fichero en modo *append* y usa `CSVWriter.writeNext`. Sin cabecera. |
| `GestorJSON` | Jackson | Lee el fichero como `List<Registro>` con `TypeReference`. Para escribir lee la lista, añade el registro y **reescribe el array completo** con indentación. |
| `GestorXLS` | Apache POI | Usa `XSSFWorkbook` (`.xlsx`). La fila 1 es la cabecera `DATO1…DATO6`; los datos empiezan en la fila 2. `DataFormatter` convierte cualquier celda a texto. Al escribir añade una fila tras la última. |
| `GestorXML` | DOM nativo | Carga el documento, añade un nodo `<registro>` con seis hijos y lo guarda con `Transformer` indentado. |
| `GestorRDF` | Apache Jena | Carga el modelo RDF/XML, crea un recurso por registro con sus propiedades y reescribe el modelo completo. |

#### Cómo queda cada fichero en disco

**CSV** (`datos.csv`)
```csv
"a","b","c","d","e","f"
```

**JSON** (`datos.json`)
```json
[ {
  "dato1" : "a", "dato2" : "b", "dato3" : "c",
  "dato4" : "d", "dato5" : "e", "dato6" : "f"
} ]
```

**XML** (`datos.xml`)
```xml
<registros>
  <registro>
    <dato1>a</dato1> <dato2>b</dato2> <dato3>c</dato3>
    <dato4>d</dato4> <dato5>e</dato5> <dato6>f</dato6>
  </registro>
</registros>
```

**RDF/XML** (`datos.rdf`): cada registro es un recurso de tipo `reto:Registro`
```
reto:registro1  rdf:type  reto:Registro ;
                reto:orden 1 ;
                reto:dato1 "a" ; reto:dato2 "b" ; ... reto:dato6 "f" .
```
con el espacio de nombres `http://gregoriofer.local/reto#`.

**XLSX** (`datos.xlsx`)

| DATO1 | DATO2 | DATO3 | DATO4 | DATO5 | DATO6 |
|---|---|---|---|---|---|
| a | b | c | d | e | f |

### 4.4 Controlador: `ServletFichero`

Mapeado en `/ServletFichero`. Es el **único punto de entrada** de la lógica. En `doPost`:

1. Fija la codificación a UTF-8 (para tildes y eñes).
2. Recoge `formato`, `accion` y `dato1…dato6`.
3. **Valida** la acción (solo `lectura` o `escritura`).
4. En escritura, **valida que ningún campo esté vacío** (`null` o solo espacios).
5. Obtiene el **gestor** y el **nombre de fichero** según el formato.
6. Resuelve la **carpeta** `ficheros/` y construye la ruta absoluta.
7. Si hay un fichero subido, lo guarda sustituyendo el existente.
8. Si es escritura, crea el `Registro` y llama a `gestor.escribir`.
9. En ambos casos llama a `gestor.leer` y guarda la lista como atributo de la petición.
10. Hace `forward` a `AccesoDatosA.jsp`.
11. Cualquier excepción se captura en un `try/catch` global y se hace `forward` a `Error.jsp` con el tipo y el mensaje.

`doGet` simplemente redirige al formulario, para que acceder a `/ServletFichero` desde la barra de direcciones no dé error.

### 4.5 Vistas (JSP)

| Vista | Función |
|---|---|
| `TratamientoFich.jsp` | Formulario: `<select>` de formato, radios de acción, 6 campos de texto y botón de subida de fichero. |
| `AccesoDatosA.jsp` | Recupera la `List<Registro>` de la petición y pinta una `<table>` con cabeceras y una fila por registro. Muestra aviso de éxito en escritura y enlace al fichero. |
| `Error.jsp` | Muestra el tipo de error y el detalle recibidos del servlet. |

Las JSP usan **scriptlets** (`<% %>`, `<%= %>`, `<%! %>`) y un método `esc()` propio para escapar HTML.

---

## 5. Librerías: por qué cada una

- **Apache POI (`poi-ooxml`)**: la librería estándar de Java para Office. `poi-ooxml` cubre `.xlsx`.
- **OpenCSV**: gestiona bien comillas, comas dentro de campos y saltos de línea, que son los casos que rompen un `split(",")` casero.
- **Jackson Databind**: estándar de facto para JSON. Convierte directamente entre `Registro` y JSON gracias a los getters/setters.
- **DOM (JDK)**: sin dependencias externas y es una de las dos opciones que pide el enunciado (DOM o SAX).
- **Apache Jena (`apache-jena-libs`)**: implementación de referencia de RDF en Java. Permite crear modelos, recursos y propiedades sin escribir el XML a mano.
- **`jakarta.servlet-api` (scope `provided`)**: se necesita para compilar, pero Tomcat ya la trae en tiempo de ejecución; incluirla en el `.war` provocaría conflictos.

---

## 6. Decisiones de diseño

| # | Decisión | Alternativa descartada | Motivo |
|---|---|---|---|
| 1 | **Interfaz `IGestorFicheros` + un gestor por formato** | Un único `if/else` gigante en el servlet | Cada formato queda aislado. Añadir uno nuevo es crear una clase y una línea en el `switch`. El servlet no conoce POI, Jena ni Jackson. |
| 2 | **Un único modelo `Registro` con 6 campos** | Mapas dinámicos o un modelo por formato | Simple, tipado y reutilizado por todos los formatos y vistas. |
| 3 | **Validación en Java (servidor)**, no solo en JavaScript | Validar solo en el navegador | El enunciado la sitúa en el servlet, y la validación del cliente se puede saltar. Además, en lectura los campos pueden estar vacíos, así que depende de la acción. |
| 4 | **`forward` a `Error.jsp` con atributos** | `sendRedirect("Error.jsp")` | El enunciado exige indicar el *tipo de error*. Con `forward` se conservan `tipoError` y `detalleError`; con `sendRedirect` se perderían. |
| 5 | **Tras escribir, se vuelve a leer el fichero** | Mostrar solo el registro recién creado | Se muestra lo que realmente quedó guardado. Sirve de comprobación y cumple "ver el nuevo registro guardado". |
| 6 | **XML con DOM nativo** | Jackson XML o SAX | DOM permite leer **y escribir** con facilidad; SAX solo sirve bien para leer. Se evita una dependencia extra (se quitó `jackson-dataformat-xml`). |
| 7 | **RDF en formato RDF/XML** | Turtle | Coincide con la extensión `.rdf` y es el formato por defecto de Jena. |
| 8 | **Propiedad `reto:orden` en RDF** | Confiar en el orden de Jena | Un grafo RDF no tiene orden. Con `orden` se garantiza el orden de inserción al leer. |
| 9 | **`.xlsx` con `XSSFWorkbook`** | `.xls` antiguo (`HSSFWorkbook`) | Es el formato Excel actual y usa una sola dependencia (`poi-ooxml`). |
| 10 | **`leer` devuelve lista vacía si no hay fichero** | Dejar que lance `FileNotFoundException` | La primera lectura de un formato nuevo no debe acabar en página de error. |
| 11 | **Un fichero fijo por formato** (`datos.csv`, `datos.json`…) | Nombre elegido por el usuario | Sencillez y evitar que el usuario escriba rutas arbitrarias en el servidor. |
| 12 | **`synchronized (LOCK)` en acceso al fichero** | Sin sincronización | Un servlet atiende varias peticiones a la vez. Sin bloqueo, dos escrituras simultáneas podrían corromper el fichero. |
| 13 | **JSP con scriptlets** | JSTL/EL | Menos dependencias y más cercano a lo visto en clase. Se compensa con un método `esc()` propio. |
| 14 | **`esc()` en todo dato del usuario** | Imprimir directamente | Evita **XSS**: lo escrito en un campo se muestra como texto, no se ejecuta como HTML. |
| 15 | **Un solo servlet para lectura y escritura** | Un servlet por acción | Lo exige el flujo del enunciado (formulario → servlet → vista). |
| 16 | **Nombres: `ServletFichero` y vistas `.jsp`** | `ServletFich` y `.html` (handover) | El enunciado usa `.jsp` y se mantuvo el nombre de la clase ya existente. Hay que usar el mismo nombre en el `action` del formulario. |
| 17 | **`maven.compiler.release` 17** | Java 22 | Compila con cualquier JDK ≥ 17 y es compatible con Tomcat 10.1. |
| 18 | **`jakarta.servlet-api` con `provided`** | Scope compile | Tomcat ya aporta la API. |

---

## 7. Seguridad

- **XSS**: toda salida de datos de usuario pasa por `esc()` en las JSP.
- **XXE** (XML External Entity): `GestorXML` desactiva los `DOCTYPE` y activa el procesamiento seguro.
- **Rutas controladas**: el usuario nunca elige el nombre ni la ubicación del fichero; solo el formato, que se traduce a un nombre fijo con `switch`.
- **Fichero subido**: se usa `Paths.get(...).getFileName()` para ignorar posibles rutas, se comprueba que la extensión corresponda al formato y se limita el tamaño con `@MultipartConfig(maxFileSize = 10 MB)`.
- **Validación en servidor**: no depende de JavaScript.

---

## 8. Subida de ficheros

Para poder leer un fichero propio se añadieron dos cosas:

- **Formulario**: `enctype="multipart/form-data"` y un `<input type="file" name="fichero">`.
- **Servlet**: `@MultipartConfig` y, tras calcular la ruta, un bloque que lee `request.getPart("fichero")`, valida la extensión y lo copia con `Files.copy(..., REPLACE_EXISTING)`.

El fichero subido **sustituye** al `datos.xxx` del formato elegido. Después el flujo es el normal: si la acción es lectura, se muestra su contenido; si es escritura, se añade el nuevo registro al final.

Para que el contenido sea legible debe tener **la estructura que espera el gestor** (por ejemplo, un CSV con 6 columnas o un JSON que sea un array de objetos con `dato1…dato6`).

---

## 9. Limitaciones conocidas

- **Persistencia**: los ficheros se guardan en `webapp/ficheros` del despliegue de Tomcat (o en una carpeta temporal si el servidor no da esa ruta), así que se pierden al redesplegar la aplicación.
- **Seis campos fijos**: los datasets reales de datos abiertos tienen otras columnas y habría que adaptarlos o generalizar el modelo.
- **XLS antiguo**: solo se admite `.xlsx`.
- **Escritura reescribe el fichero completo** en JSON, XML, RDF y XLSX: es correcto para pocos registros, pero poco eficiente con volúmenes grandes.
- **Sin pruebas automáticas**: el proyecto incluye JUnit en el `pom.xml`, pero no hay tests escritos.
- **Subida sobrescribe sin confirmar** los datos previos del formato.

---

## 10. Cómo ejecutarlo

1. Importar la carpeta como **Existing Maven Project** en Eclipse y hacer **Maven → Update Project** (Alt+F5).
2. Configurar un servidor **Tomcat v10.1** con JDK 17 o superior.
3. Clic derecho sobre `TratamientoFich.jsp` → **Run As → Run on Server**.
4. Abrir `http://localhost:8080/RetoDatos/TratamientoFich.jsp`.

Pruebas recomendadas para cada formato:

1. Escritura con los 6 campos rellenos → debe aparecer en la tabla.
2. Lectura → el registro debe persistir.
3. Escritura con un campo vacío → debe salir `Error.jsp`.
4. Subir un fichero con la extensión correcta → lectura de su contenido.
5. Subir un fichero con la extensión equivocada → `Error.jsp`.

---

## 11. Pendiente de entrega

- [ ] Diagrama de Gantt (reparto de tareas, duración, miembros, tareas completadas, dificultades).
- [ ] Subir el proyecto a GitHub y compartirlo con la profesora.
- [ ] Entregar el enlace en Moodle.
