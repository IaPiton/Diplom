import searchengine.utils.Lemmanisator;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Lemmanisator lemmanisator = new Lemmanisator();
        System.out.println(lemmanisator.textToLemma("if"));

    }
}
