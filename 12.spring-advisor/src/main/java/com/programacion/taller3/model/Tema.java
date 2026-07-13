package com.programacion.taller3.model;

import java.util.List;

public record Tema(
        String titulo,
        List<String> autores,
        String director,
        String editores,
        String tipo_material,
        Integer fecha,
        List<String> palabras_clave,
        String resumen_es,
        String resumen_en,
        String uri,
        List<String> colecciones,
        Double similitud
) {}
