# 📊 RetoDatos — Gestión Multi-formato de Ficheros (Acceso a Datos)

Aplicación web desarrollada en **Java (Jakarta EE)** bajo el patrón arquitectónico **Modelo-Vista-Controlador (MVC)** para el módulo de **Acceso a Datos**. El sistema permite realizar operaciones de **lectura** y **escritura** sobre conjuntos de datos estructurados en cinco formatos estándar diferentes: **CSV, JSON, XML, XLS y RDF**.

---

## 🚀 Funcionalidades Principales

* **Soporte Multi-formato Unificado:** Procesamiento homogéneo de registros mediante una interfaz común (`IGestorFicheros`) implementada para cada tecnología de almacenamiento:
  * **CSV** (`.csv`): Lectura y escritura secuencial de texto delimitado.
  * **JSON** (`.json`): Serialización y deserialización de colecciones de objetos.
  * **XML** (`.xml`): Parseo y manipulación del árbol de nodos DOM.
  * **Excel** (`.xls`): Lectura de hojas de cálculo y adición de filas mediante Apache POI.
  * **RDF** (`.rdf`): Gestión de grafos semánticos y tripletas mediante Apache Jena.
* **Operaciones de Lectura y Escritura Incremental:**
  * **Lectura:** Extrae todos los registros almacenados en el fichero del formato elegido y los presenta dinámicamente en una tabla HTML.
  * **Escritura:** Valida los datos introducidos en el formulario web y añade nuevos registros al fichero correspondiente **sin sobrescribir** la información previa.
* **Inicialización Automática de Datos:** Al arrancar la aplicación en el servidor, un listener (`InicializadorFicheros`) prepara y verifica el entorno de ficheros base en el directorio desplegado.
* **Gestión Centralizada de Errores:** Captura excepciones de validación, entrada/salida (E/S) o parseo, redirigiendo al usuario a una vista informativa con el detalle del error ocurrido.

---

## 🛠️ Tecnologías Utilizadas

* **Lenguaje:** Java (JDK 22)
* **Arquitectura Web:** Jakarta EE (Servlets & JSP)
* **Servidor de Aplicaciones:** Apache Tomcat 11.0
* **Gestión de Dependencias:** Apache Maven (`pom.xml`)
* **Procesamiento de Datos:**
  * *Apache POI* (gestión de hojas de cálculo `.xls`)
  * *Gson* (procesamiento de `.json`)
  * *Apache Jena* (lectura y escritura de modelos `.rdf`)
  * *Java XML / DOM & Java I/O* (procesamiento de `.xml` y `.csv`)
* **Frontend:** JSP, HTML5 y CSS3 (`estilos.css`)

---

## 📂 Estructura del Proyecto

El código fuente dentro de `RetoDatos/` está organizado siguiendo la separación de responsabilidades del patrón MVC:

```text
RetoDatos/
├── pom.xml                                  # Configuración de Maven y dependencias
└── src/
    └── main/
        ├── java/
        │   ├── controlador/
        │   │   ├── InicializadorFicheros.java  # Listener de arranque para preparar los ficheros
        │   │   └── ServletFichero.java         # Controlador principal de peticiones (doGet/doPost)
        │   ├── modelo/
        │   │   ├── Accion.java                 # Enumerado/modelo de operaciones (Lectura / Escritura)
        │   │   ├── FilaFormulario.java         # Modelo de apoyo para los campos del formulario
        │   │   ├── Formato.java                # Enumerado/modelo de formatos (CSV, JSON, XML, XLS, RDF)
        │   │   └── Registro.java               # Entidad principal que representa cada fila de datos
        │   └── servicios/
        │       ├── IGestorFicheros.java        # Interfaz común para operaciones de lectura y escritura
        │       ├── GestorCSV.java              # Implementación para ficheros CSV
        │       ├── GestorJSON.java             # Implementación para ficheros JSON
        │       ├── GestorRDF.java              # Implementación para ficheros RDF
        │       ├── GestorXLS.java              # Implementación para ficheros Excel (.xls)
        │       └── GestorXML.java              # Implementación para ficheros XML
        └── webapp/
            ├── TratamientoFich.jsp             # Vista principal (selección de formato, acción y formulario)
            ├── AccesoDatosA.jsp                # Vista de resultados (tabla con los registros leídos)
            ├── Error.jsp                       # Vista de notificación de errores y excepciones
            ├── css/
            │   └── estilos.css                 # Hoja de estilos de la aplicación
            ├── ficheros/                       # Directorio de almacenamiento de los archivos de datos
            └── WEB-INF/
                └── web.xml                     # Descriptor de despliegue web
```

---

## 🔄 Flujo de Ejecución

1. **Petición Inicial (`TratamientoFich.jsp`):** El usuario selecciona el formato de fichero deseado (`CSV`, `JSON`, `XML`, `XLS` o `RDF`) y la operación a realizar (`Lectura` o `Escritura`), rellenando los campos del registro en caso de escritura.
2. **Controlador (`ServletFichero.java`):** Recibe los parámetros HTTP, valida la entrada e instancia el gestor adecuado a través de la interfaz `IGestorFicheros`.
3. **Capa de Servicios (`servicios/Gestor*.java`):** Ejecuta la operación física sobre el fichero alojado en `webapp/ficheros/` transformando los datos entre el formato origen y objetos `Registro`.
4. **Respuesta (`AccesoDatosA.jsp` / `Error.jsp`):**
   * Si la operación tiene éxito, se envía la lista de objetos `Registro` a `AccesoDatosA.jsp` para renderizar la tabla actualizada.
   * Si ocurre algún fallo, se captura la excepción y se muestra el mensaje descriptivo en `Error.jsp`.

---

## ⚙️ Instalación y Despliegue

### Requisitos Previos
* **JDK 22** instalado y configurado.
* **Apache Tomcat 11.0** configurado en tu entorno (Eclipse IDE o instalación local).
* **Apache Maven** (integrado en el IDE o vía consola).

### Pasos para ejecutar el proyecto
1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/dzs1212g/proyectoad.git
   ```
2. **Importar en el IDE (Eclipse):**
   * Ir a `File > Import > Maven > Existing Maven Projects` y seleccionar la carpeta `RetoDatos`.
   * Ejecutar `Maven > Update Project` para descargar todas las dependencias definidas en el `pom.xml`.
3. **Desplegar en Tomcat 11:**
   * Añadir el proyecto `RetoDatos` al servidor **Tomcat v11.0** configurado en el IDE.
   * Iniciar el servidor y acceder desde el navegador a:
     ```text
     http://localhost:8080/RetoDatos/TratamientoFich.jsp
     ```