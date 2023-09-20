package utils;

import lombok.Data;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
@Data
public class Lemmanisator {
    private HashMap<String, Integer> wordsMap;
    private static final String REGEXP_WORD = "[а-яА-ЯёЁ]+";
    private static final String REGEXP_TEXT = "\\s*(\\s|\\?|\\||»|«|\\*|,|!|\\.)\\s*";
    LuceneMorphology luceneMorph = new RussianLuceneMorphology();

    public Lemmanisator() throws IOException {

    }

    public String htmlClearing (String document)
    {
        String clearingText = Jsoup.parse(document).text();
        return clearingText;
    }
    public boolean wordCheck(String word) {
        if (word.matches(REGEXP_WORD)) {
            List<String> wordBaseForms =
                    luceneMorph.getMorphInfo(word);
            if ((!wordBaseForms.get(0).endsWith("ПРЕДЛ") && (!wordBaseForms.get(0).endsWith("СОЮЗ")) &&
                    (!wordBaseForms.get(0).endsWith("ЧАСТ")) && (!wordBaseForms.get(0).endsWith("МЕЖД")))) {
                return true;
            }
        }
        return false;
    }
    public HashMap<String, Integer> textToLemma (String content)
    {
        wordsMap = new HashMap<>();
        String text = htmlClearing(content);
        text = content.trim();
        String[] words = text.toLowerCase().split(REGEXP_TEXT);
        for (String word : words)
        {
            if (wordCheck(word))
            {
                List<String> wordBaseForms = luceneMorph.getNormalForms(word);

                wordBaseForms.forEach(w -> { wordsMap.put(w, wordsMap.getOrDefault(w, 0) + 1);
                });
            }
        }
        return wordsMap;
    }
}
