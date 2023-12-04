package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class RussianMorphology {
    LuceneMorphology luceneMorph = new RussianLuceneMorphology();

    public RussianMorphology() throws IOException {
    }

    public Boolean wordCheck(String word) {
        List<String> wordBaseForms = luceneMorph.getMorphInfo(word);

        if ((!wordBaseForms.get(0).endsWith("ПРЕДЛ") && (!wordBaseForms.get(0).endsWith("СОЮЗ")) &&
                (!wordBaseForms.get(0).endsWith("ЧАСТ")) && (!wordBaseForms.get(0).endsWith("МЕЖД")))) {
            return true;
        }
        return false;
    }

    public List<String> russianLemma(String word) {
        List<String> wordBaseForms = new ArrayList<>();
        if (wordCheck(word)) {
            wordBaseForms = luceneMorph.getNormalForms(word);
        }
        return wordBaseForms;
    }


}
