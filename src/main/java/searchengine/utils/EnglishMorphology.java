package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class EnglishMorphology {
    LuceneMorphology luceneMorph = new EnglishLuceneMorphology();

    public EnglishMorphology() throws IOException {
    }

    public Boolean wordCheck(String word) {
        List<String> wordBaseForms = luceneMorph.getMorphInfo(word);
        if ((!wordBaseForms.get(0).endsWith("PREP") && (!wordBaseForms.get(0).endsWith("CONJ")) &&
                (!wordBaseForms.get(0).endsWith("PART")) && (!wordBaseForms.get(0).endsWith("ADJECTIVE"))
                && (!wordBaseForms.get(0).endsWith("ADVERB")))) {
            return true;
        }
        return false;
    }

    public List<String> englishLemma(String word) {
        List<String> wordBaseForms = new ArrayList<>();
        if (wordCheck(word)) {
            wordBaseForms = luceneMorph.getNormalForms(word);
        }
        return wordBaseForms;
    }
}
