package servicios;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import modelo.Registro;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** El fichero contiene un array JSON: [ {"dato1": "...", ...}, ... ] */
public class GestorJSON implements IGestorFicheros {

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public List<Registro> leer(String rutaArchivo) throws Exception {
        File f = new File(rutaArchivo);
        if (!f.exists() || f.length() == 0) {
            return new ArrayList<>();
        }
        return mapper.readValue(f, new TypeReference<List<Registro>>() {});
    }

    @Override
    public void escribir(String rutaArchivo, Registro nuevoRegistro) throws Exception {
        List<Registro> registros = leer(rutaArchivo);
        registros.add(nuevoRegistro);
        mapper.writeValue(new File(rutaArchivo), registros); // reescribe el array completo (UTF-8)
    }
}
