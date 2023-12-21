package searchengine.utils;

import lombok.Data;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
@Data
public class Lemmanisator {


    RussianMorphology russianMorphology = new RussianMorphology();
    EnglishMorphology englishMorphology = new EnglishMorphology();
    private static final String REGEXP_RUSSIAN_WORD = "[а-яА-ЯёЁ]+";
    private static final String REGEXP_ENGLISH_WORD = "[a-zA-Z]+";
    private static final String REGEXP_TEXT = "\\s*(\\s|\\?|\\||»|«|\\*|,|!|\\.)\\s*";

    public Lemmanisator() throws IOException {
    }

    public String htmlClearing(String document) {
        return Jsoup.parse(document).text();
    }

    public Boolean wordsRussian(String word) {
        return word.matches(REGEXP_RUSSIAN_WORD);
    }

    public Boolean wordsEnglish(String word) {
        return word.matches(REGEXP_ENGLISH_WORD);
    }

    public HashMap<String, Integer> textToLemma(String content) {
        HashMap<String, Integer> lemmaMap = new HashMap<>();
        List<String> lemma;
        String text = htmlClearing(content).trim();
        String[] words = text.toLowerCase().split(REGEXP_TEXT);
        for (String word : words) {
            if (wordsRussian(word)) {
                lemma = russianMorphology.russianLemma(word);
                lemmaMap = lemmaMap(lemma, lemmaMap);
            }
            if (wordsEnglish(word)) {
                lemma = englishMorphology.englishLemma(word);
                lemmaMap = lemmaMap(lemma, lemmaMap);
            }
        }
        return lemmaMap;
    }

    public HashMap<String, Integer> lemmaMap(List<String> lemmaList, HashMap<String, Integer> lemmaMap) {
        lemmaList.forEach((String w) -> lemmaMap.put(w, lemmaMap.getOrDefault(w, 0) + 1));
        return lemmaMap;
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
        if (wordsRussian(word)) {
            lemmaList = russianMorphology.luceneMorph.getNormalForms(word);
        }
        if (wordsEnglish(word)) {
            lemmaList = englishMorphology.luceneMorph.getNormalForms(word);
        }
        return lemmaList;
    }
}
