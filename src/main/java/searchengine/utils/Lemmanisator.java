package searchengine.utils;

import lombok.Data;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

@Component
@Data
public class Lemmanisator {
    private HashMap<String, Integer> wordsMap;
    private static final String REGEXP_WORD = "[а-яА-ЯёЁ]+";
    private static final String REGEXP_TEXT = "\\s*(\\s|\\?|\\||»|«|\\*|,|!|\\.)\\s*";
    LuceneMorphology luceneMorph = new RussianLuceneMorphology();

    public Lemmanisator() throws IOException {
    }

    public String htmlClearing(String document) {
        String clearingText = Jsoup.parse(document).text();
        return clearingText;
    }

    public boolean wordCheck(String word) {
        if (word.matches(REGEXP_WORD)) {
            List<String> wordBaseForms =
                    luceneMorph.getMorphInfo(word);
            return Stream.of("ПРЕДЛ", "СОЮЗ", "ЧАСТ", "МЕЖД").noneMatch(s -> wordBaseForms.get(0).endsWith(s));
        }
        return false;
    }

    public HashMap<String, Integer> textToLemma(String content) {
        wordsMap = new HashMap<>();
        String text = htmlClearing(content);
        text = content.trim();
        String[] words = text.toLowerCase().split(REGEXP_TEXT);
        for (String word : words) {
            if (wordCheck(word)) {
                List<String> wordBaseForms = luceneMorph.getNormalForms(word);

                wordBaseForms.forEach((String w) -> {
                    wordsMap.put(w, wordsMap.getOrDefault(w, 0) + 1);
                });
            }
        }
        return wordsMap;
    }

    public Collection<Integer> findLemmaIndexInWord(String content, String lemma) {
        List<Integer> listLemmaIndex = new ArrayList<>();
        String[] elements = content.toLowerCase().split("\\p{Punct}|\\s");
        int index = 0;
        List<String> lemmas;
        for (String el : elements) {
            lemmas = getLemma(el);
            for (String lem : lemmas) {
                if (lem.equals(lemma)) {
                    listLemmaIndex.add(index);
                }
            }
            index += el.length() + 1;
        }
        Collections.sort(listLemmaIndex);
        return listLemmaIndex;
    }

    public List<String> getLemma(String word) {
        List<String> lemmaList = new ArrayList<>();
        if (wordCheck(word)) {
            lemmaList = luceneMorph.getNormalForms(word);
        }
        return lemmaList;
    }
}
