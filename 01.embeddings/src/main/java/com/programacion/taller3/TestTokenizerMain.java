package com.programacion.taller3;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestTokenizerMain {

    // Este es la carcaga del archivo
    public static final String PATH = "C://Users//fing.labcom//IdeaProjects//Taller//01.embeddings//the-verdict.txt";

    static List<Pair> vocabulary (String PATH) throws  Exception {

        String raw_text = Files.lines(Paths.get(PATH))
                .reduce(String::concat)
                .orElse("");


        String regex = "(?=[,.:;?_!\"()']|--|\\s)|(?<=[,.:;?_!\"()']|--|\\s)";

        var tokens = raw_text.split(regex);

        var preprocessed = Stream.of(tokens)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();


        //--

        var allWords = preprocessed.stream()
                .distinct()
                .sorted()
                .toList();

        var vocabsize = allWords.size();


        AtomicInteger counter = new AtomicInteger(0);
        var vocab = allWords.stream()
                .map(it -> new Pair(counter.getAndIncrement(), it))
                .toList();

        return vocab;
    }

    static List<Pair> vocabularyEx (String PATH) throws  Exception {

        String raw_text = Files.lines(Paths.get(PATH))
                .reduce(String::concat)
                .orElse("");


        String regex = "(?=[,.:;?_!\"()']|--|\\s)|(?<=[,.:;?_!\"()']|--|\\s)";

        var tokens = raw_text.split(regex);

        var preprocessed = Stream.of(tokens)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();


        //--

        var allWords = preprocessed.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        allWords.add("<|endoftext|>");
        allWords.add("<|unk|>");

        AtomicInteger counter = new AtomicInteger(0);
        var vocab = allWords.stream()
                .map(it -> new Pair(counter.getAndIncrement(), it))
                .toList();

        return vocab;
    }

    static void main(String [] args) throws Exception  {

        var vocab = vocabulary(PATH);

        vocab.stream()
                .takeWhile(it -> it.tokenId() < 51)

                .forEach(System.out::println);

        //--

        var text = "\"It's the last he painted, you know,\" Mrs. Gisburn said with pardonable pride.";
        SimpleTokenizerV1 tokenizerV1 = new SimpleTokenizerV1(vocab);

        var ids = tokenizerV1.encode(text);
        System.out.println(ids);
        System.out.println("Decode" + tokenizerV1.decode(ids));

        var vocabex = vocabularyEx(PATH);

        vocabex.stream()
                .skip(vocabex.size() - 5)
                .forEach(System.out::println);


        var text1 = "Hello, do you like tea?";
        var text2 = "In the sunlit terraces of the palace.";
        text = text1 + " <|endoftext|> " + text2;

        SimpleTokenizerV2 tokenizer2 = new SimpleTokenizerV2(vocabex);

        var ids2 = tokenizer2.encode(text);
        System.out.println(ids2);
        System.out.println("Decode" + tokenizer2.decode(ids2));

    }
}
