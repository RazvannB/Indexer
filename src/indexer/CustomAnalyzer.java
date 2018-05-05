/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.tartarus.snowball.ext.RomanianStemmer;
/**
 *
 * @author razvan
 */
public class CustomAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String string) {
        Tokenizer tokenizer = new StandardTokenizer();
        TokenStream tokenStream = new StandardFilter(tokenizer);
        
        tokenStream = new LowerCaseFilter(tokenStream);
        try {
            tokenStream = this.filterStopWords(tokenStream);
        } catch (IOException ex) {
            Logger.getLogger(CustomAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
        tokenStream = new ASCIIFoldingFilter(tokenStream);
        tokenStream = new SnowballFilter(tokenStream, new RomanianStemmer());
        
        return new TokenStreamComponents(tokenizer, tokenStream);
    }
    
    private TokenStream filterStopWords(TokenStream tokenStream) throws IOException {
        tokenStream = new StopFilter(tokenStream, RomanianAnalyzer.getDefaultStopSet());
        String[] stopWords = getStopwords();
        CharArraySet stopSet = StopFilter.makeStopSet(stopWords);
        return new StopFilter(tokenStream, stopSet);
    }
    
    private String[] getStopwords() throws IOException {
        List<String> stopwords = new ArrayList<>();
        BufferedReader in = Files.newBufferedReader(Paths.get(Constants.stopwordsPath), StandardCharsets.UTF_8);
        String line;
        while ((line = in.readLine()) != null) {
           stopwords.add(normalizeWords(line));
        }
        String[] stopwordsArray = new String[stopwords.size()];
        stopwordsArray = stopwords.toArray(stopwordsArray);
        return stopwordsArray;
    }
    
    private String normalizeWords(String string) {
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        string = string.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return string;
    }
}
