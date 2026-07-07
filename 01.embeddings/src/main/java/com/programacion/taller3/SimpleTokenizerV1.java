package com.programacion.taller3;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SimpleTokenizerV1 {

    private static final String REGEX = "(?=[,.:;?_!\"()']|--|\\s)|(?<=[,.:;?_!\"()']|--|\\s)";

    private Map<String, Integer> strtoInt;
    private Map<Integer, String> intToStr;

    public SimpleTokenizerV1 (List<Pair> vocab){
        strtoInt = vocab.stream()
                           .collect(Collectors.toMap(Pair::token, Pair::tokenId));
        intToStr = vocab.stream()
                        .collect(Collectors.toMap(Pair::tokenId, Pair::token));


    }

    public List<Integer> encode(String text) {
         return Arrays.stream( text.split(REGEX))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(token -> strtoInt.get(token))
                .filter(Objects::nonNull)
                .toList();

    }

    public String decode (List<Integer> ids){
        return  ids.stream()
                //.map(intToStr::get)
                .map(id -> intToStr.getOrDefault(id, "UNK"))
                .collect(Collectors.joining(" "));
    }
}
