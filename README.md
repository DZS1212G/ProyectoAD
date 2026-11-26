# 📊 RetoDatos — Gestión Multi-formato de Ficheros (Acceso a Datos)

Aplicación web empresarial desarrollada en **Java 22** bajo la especificación **Jakarta EE 11** y desplegada sobre **Apache Tomcat 11**. Su objetivo funcional es gestionar la **lectura** y **escritura persistente** de registros de datos estructurados de manera unificada e interoperable sobre cinco formatos estándar distintos: **CSV, JSON, XML, XLS y RDF**.

Arquitectónicamente, el sistema combina el patrón **MVC (Modelo-Vista-Controlador)** clásico en Servlets/JSP con el **Patrón Estrategia (*Strategy Pattern*)** en la capa de servicios y un **Listener de ciclo de vida** para la inicialización automática del entorno.

---

## 🛠️ Stack Tecnológico y Dependencias

* **Lenguaje:** Java 22 (`<maven.compiler.release>22</maven.compiler.release>`)
* **Especificación Web:** Jakarta EE 11 (Servlet 6.1, JSP 4.0 y JSTL 3.0)
* **Servidor de Aplicaciones:** Apache Tomcat 11.0
* **Construcción y Dependencias:** Apache Maven (`pom.xml` con empaquetado `.war`)
* **Librerías de Procesamiento de Datos:**
  * **Apache POI (`poi` y `poi-ooxml`):** Lectura y escritura binaria de hojas de cálculo Excel (`.xls`).
  * **Google Gson (`gson`):** Serialización y deserialización de colecciones de objetos en formato `.json`.
  * **Apache Jena (`apache-jena-libs`):** Gestión de grafos semánticos, recursos y tripletas en formato `.rdf` (RDF/XML).
  * **Java XML / DOM & Java NIO/IO:** Procesamiento nativo del árbol DOM para `.xml` y flujos con codificación `UTF-8` para `.csv`.
* **Frontend:** JSP, HTML5 y CSS3 (`estilos.css`).

---

## 🏗️ Arquitectura y Desglose Detallado por Capas

### 1. Capa de Modelo (`src/main/java/modelo`)
Define las estructuras de datos del dominio, los tipos enumerados de control y los objetos de transferencia entre la vista y el controlador:

* **`Registro.java` (Entidad principal / POJO):**
  Representa una fila o unidad lógica de información dentro de cualquiera de los ficheros. Encapsula los atributos correspondientes a las columnas del dataset, sus constructores (vacío para serialización con librerías como Gson y parametrizado para instanciación directa), métodos *getters* y *setters*, e implementa `Serializable` para permitir su transporte seguro entre capas y ámbitos de petición.
* **`Formato.java` (Enumerado de tipos de archivo):**
  Define de forma tipada y segura los cinco formatos soportados por la aplicación (`CSV`, `JSON`, `XML`, `XLS`, `RDF`). Evita el uso de cadenas de texto dispersas (*magic strings*), asociando cada constante con su extensión correspondiente y facilitando la selección polimórfica del gestor adecuado.
* **`Accion.java` (Enumerado de operaciones):**
  Modela las dos operaciones permitidas en el flujo de negocio (`LECTURA` y `ESCRITURA`), permitiendo que el controlador enrute la petición sin ambigüedades.
* **`FilaFormulario.java` (DTO de interfaz):**
  Actúa como objeto auxiliar de transferencia (*Data Transfer Object*) para estructurar y transportar los valores enviados desde los campos dinámicos del formulario web antes de validarlos y convertirlos en una entidad `Registro` persistible.

---

### 2. Capa de Servicios y Persistencia (`src/main/java/servicios`)
Implementa el **Patrón Estrategia** para desacoplar el controlador de las particularidades técnicas de cada formato de archivo. Todas las clases comparten un contrato común:

* **`IGestorFicheros.java` (Interfaz):**
  Establece la firma estándar que deben cumplir todos los gestores de almacenamiento (operaciones de lectura que devuelven `List<Registro>` y operaciones de escritura incremental de nuevos objetos `Registro`). Gracias a esta abstracción, el servlet opera sin acoplarse al formato físico subyacente.
* **`GestorCSV.java`:**
  Gestiona ficheros de texto plano delimitados mediante flujos estándar (`BufferedReader`, `BufferedWriter`, `StandardCharsets.UTF_8`). En lectura, omite la fila de cabecera y mapea cada línea a objetos `Registro`; en escritura, abre el archivo en modo *append* (`true`) para añadir nuevas líneas al final sin sobrescribir los registros existentes.
* **`GestorJSON.java`:**
  Utiliza **Google Gson** (`GsonBuilder`, `TypeToken`) para deserializar arreglos de objetos JSON directamente en colecciones `List<Registro>` y serializar los nuevos registros con formato legible (*pretty printing*) preservando la codificación `UTF-8`.
* **`GestorXML.java`:**
  Emplea las APIs estándar de procesamiento XML de Java (**DOM** mediante `DocumentBuilderFactory` y `TransformerFactory`). Al leer, recorre el árbol de nodos (`NodeList` / `Element`) extrayendo las etiquetas de cada registro; al escribir, carga el documento DOM existente, construye un nuevo nodo hijo con sus subelementos, lo adjunta al elemento raíz y vuelca el árbol actualizado al disco.
* **`GestorXLS.java`:**
  Integra **Apache POI** (`HSSFWorkbook` para `.xls`) junto con `DataFormatter` para extraer valores de celda de forma segura independientemente de su tipo interno en Excel (numérico, texto o fecha). Al escribir, abre el libro existente, localiza el índice siguiente a `getLastRowNum()` y crea una nueva fila (`Row`) con sus celdas sin alterar los datos previos.
* **`GestorRDF.java`:**
  Utiliza **Apache Jena** (`Model`, `ModelFactory`, `RDFDataMgr`) para manipular grafos semánticos en formato RDF/XML. Define un espacio de nombres (*namespace*) y propiedades RDF para cada campo del registro: en lectura consulta los recursos mediante `ResIterator`, y en escritura instancia un nuevo `Resource` con identificador único (URI/UUID), le asocia sus tripletas (sujeto-predicado-objeto) y serializa el modelo completo.

---

### 3. Capa de Controlador y Ciclo de Vida (`src/main/java/controlador`)
Orquesta el arranque de la aplicación, la recepción de peticiones HTTP, la validación de negocio y la navegación entre vistas:

* **`InicializadorFicheros.java` (`ServletContextListener`):**
  Se ejecuta automáticamente una única vez cuando Apache Tomcat arranca o despliega la aplicación web (`contextInitialized`). Verifica que el directorio físico `/ficheros` dentro del contexto desplegado exista y prepara los archivos base de datos para evitar errores de archivo no encontrado (`FileNotFoundException`) en la primera petición.
* **`ServletFichero.java` (`HttpServlet` — Front Controller):**
  Controlador principal mapeado bajo `jakarta.servlet.*`. Sus responsabilidades son:
  1. Configurar la codificación `UTF-8` en las peticiones para preservar tildes y caracteres especiales.
  2. Recoger y normalizar los parámetros enviados desde la vista (`formato`, `accion` y los campos del formulario).
  3. Resolver la ruta física en el servidor mediante `getServletContext().getRealPath("/ficheros/...")`.
  4. Instanciar dinámicamente la implementación de `IGestorFicheros` correspondiente al `Formato` seleccionado.
  5. **En Lectura:** Invocar al gestor, almacenar la `List<Registro>` resultante como atributo en el `request` y delegar mediante `RequestDispatcher.forward()` hacia `AccesoDatosA.jsp`.
  6. **En Escritura:** Validar en servidor que ningún campo obligatorio llegue vacío o nulo, persistir el nuevo `Registro`, releer el fichero actualizado para reflejar los cambios en tiempo real y reenviar hacia `AccesoDatosA.jsp`.
  7. **Gestión de Excepciones:** Capturar cualquier error de validación, E/S o parseo, adjuntar su mensaje descriptivo en el `request` y redirigir hacia `Error.jsp`.

---

### 4. Capa de Vista y Recursos Web (`src/main/webapp`)
Constituye la interfaz de usuario renderizada en el servidor mediante **JSP (JavaServer Pages)** y hojas de estilo:

* **`TratamientoFich.jsp` (Vista principal de entrada):**
  Página de inicio configurada en el descriptor de despliegue. Presenta el formulario interactivo donde el usuario elige el formato (`CSV`, `JSON`, `XML`, `XLS`, `RDF`), la operación (`Lectura` o `Escritura`) y rellena los campos del nuevo registro cuando desea insertar información.
* **`AccesoDatosA.jsp` (Vista de visualización de resultados):**
  Recibe desde `ServletFichero` la colección de objetos `Registro` y los metadatos de la operación realizada, renderizando dinámicamente una tabla HTML (`<table>`) con todas las filas almacenadas en el fichero seleccionado, además de notificaciones de confirmación tras una escritura.
* **`Error.jsp` (Vista de gestión de excepciones):**
  Página dedicada a mostrar de forma controlada y legible cualquier error de validación o excepción técnica capturada por el controlador, ofreciendo un enlace de retorno seguro al formulario principal sin exponer trazas crudas del servidor.
* **`css/estilos.css`:**
  Hoja de estilos centralizada que define la maquetación, tipografía, paleta de colores, formularios, botones y tablas responsivas para mantener coherencia visual en toda la aplicación.
* **`WEB-INF/web.xml`:**
  Descriptor de despliegue conforme al esquema **Jakarta EE 11 (`web-app_6_1.xsd`)** que establece `TratamientoFich.jsp` como página de bienvenida (`<welcome-file-list>`).
* **`ficheros/`:**
  Directorio de almacenamiento físico en el servidor donde residen los archivos de datos manipulados por las clases `Gestor*`.

---

## 📂 Estructura de Directorios

```text
ProyectoAD/
├── RetoDatos/
│   ├── pom.xml                                      # Configuración Maven (Java 22, Jakarta EE 11, POI, Gson, Jena)
│   └── src/
│       └── main/
│           ├── java/
│           │   ├── controlador/
│           │   │   ├── InicializadorFicheros.java   # Listener de arranque (ServletContextListener)
│           │   │   └── ServletFichero.java          # Controlador frontal (HttpServlet)
│           │   ├── modelo/
│           │   │   ├── Accion.java                  # Enum de operaciones (LECTURA / ESCRITURA)
│           │   │   ├── FilaFormulario.java          # DTO de captura de datos del formulario
│           │   │   ├── Formato.java                 # Enum de formatos (CSV, JSON, XML, XLS, RDF)
│           │   │   └── Registro.java                # Entidad POJO del dominio
│           │   └── servicios/
│           │       ├── IGestorFicheros.java         # Interfaz común (Patrón Estrategia)
│           │       ├── GestorCSV.java               # Estrategia de lectura/escritura CSV
│           │       ├── GestorJSON.java              # Estrategia de lectura/escritura JSON (Gson)
│           │       ├── GestorRDF.java               # Estrategia de lectura/escritura RDF (Apache Jena)
│           │       ├── GestorXLS.java               # Estrategia de lectura/escritura XLS (Apache POI)
│           │       └── GestorXML.java               # Estrategia de lectura/escritura XML (DOM)
│           └── webapp/
│               ├── TratamientoFich.jsp              # Vista principal (Formulario)
│               ├── AccesoDatosA.jsp                 # Vista de resultados (Tabla dinámica)
│               ├── Error.jsp                        # Vista de notificación de errores
│               ├── css/
│               │   └── estilos.css                  # Hoja de estilos global
│               ├── ficheros/                        # Almacenamiento de ficheros de datos
│               └── WEB-INF/
│                   └── web.xml                      # Descriptor de despliegue Jakarta EE 11
└── Servers/
    └── Tomcat v11.0 Server at localhost-config/     # Configuración del servidor Tomcat 11 en Eclipse
```

---

## 🔄 Flujo de Ejecución del Sistema

```text
[Arranque Tomcat 11] ──▶ InicializadorFicheros (Prepara carpeta /ficheros)
                                │
                                ▼
[Usuario] ──▶ TratamientoFich.jsp (Selecciona Formato + Acción + Datos)
                                │
                        HTTP GET / POST
                                ▼
                       ServletFichero.java
                       ├── Valida parámetros y datos
                       ├── Selecciona IGestorFicheros (CSV | JSON | XML | XLS | RDF)
                       │        │
                       │        ▼
                       │   Gestor*.java ◀──▶ [webapp/ficheros/*.*]
                       │
                       ├── (Éxito) ──▶ RequestDispatcher ──▶ AccesoDatosA.jsp (Tabla HTML)
                       └── (Error) ──▶ RequestDispatcher ──▶ Error.jsp (Mensaje controlado)
```

1. **Despliegue:** Tomcat 11 inicializa el contexto y dispara `InicializadorFicheros`, verificando que el entorno de almacenamiento en `webapp/ficheros/` esté listo.
2. **Interacción:** El usuario accede a `TratamientoFich.jsp`, selecciona un formato y una operación, y envía el formulario hacia `ServletFichero`.
3. **Procesamiento:** `ServletFichero` valida la entrada, selecciona dinámicamente la implementación adecuada de `IGestorFicheros` y ejecuta la operación física sobre el disco.
4. **Renderizado:** Si la operación finaliza con éxito, el controlador transfiere la colección `List<Registro>` a `AccesoDatosA.jsp` para pintar la tabla actualizada; si ocurre cualquier fallo, deriva el flujo a `Error.jsp` con el detalle de la excepción.

---

## ⚙️ Instalación y Despliegue en Local

### Requisitos Previos
* **JDK 22** instalado y configurado en las variables de entorno.
* **Apache Tomcat 11.0** (compatible con Jakarta EE 11 / Servlet 6.1).
* **Apache Maven** (integrado en Eclipse IDE mediante M2E o vía terminal).

### Pasos de Configuración
1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/dzs1212g/proyectoad.git
   ```
2. **Importar en Eclipse IDE:**
   * Ir a `File > Import > Maven > Existing Maven Projects` y seleccionar el directorio `RetoDatos`.
   * Hacer clic derecho sobre el proyecto > `Maven > Update Project...` para descargar todas las dependencias definidas en `pom.xml`.
   * Verificar en `Properties > Project Facets` que estén activos **Java 22** y **Dynamic Web Module 6.1**.
3. **Desplegar y ejecutar en Tomcat 11:**
   * Añadir el proyecto `RetoDatos` al servidor **Tomcat v11.0** en la pestaña *Servers*.
   * Arrancar el servidor y abrir en el navegador:
     ```text
     http://localhost:8080/RetoDatos/TratamientoFich.jsp
     ```