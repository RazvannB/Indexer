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
import java.util.Arrays;
import java.util.Comparator;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author razvan
 */
public class Retriever {

    private final String index = Constants.indexPath;
    private final String contentsField = Constants.contentsField;
    private final String abstractField = Constants.abstractField;
    private final String documentField = Constants.documentField;
    private final String queries = Constants.queriesPath;

    Retriever() {
    }

    public void startRetriever() throws IOException, ParseException {
        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)))) {
            IndexSearcher searcher = new IndexSearcher(reader);

            BufferedReader in = Files.newBufferedReader(Paths.get(queries), StandardCharsets.UTF_8);
            
            String line = in.readLine().trim();
            line = normalizeSearchQuery(line);
            
            QueryParser parser = new QueryParser(documentField, new CustomAnalyzer());
            Query debugQuery = parser.parse(line);
            System.out.println("Searching for: " + debugQuery.toString(documentField));
            
            Builder booleanQueryBuilder = new BooleanQuery.Builder();
            Query query1 = new TermQuery(new Term(abstractField, debugQuery.toString(documentField)));
            Query query2 = new TermQuery(new Term(contentsField, debugQuery.toString(documentField)));
            query1 = new BoostQuery(query1, 3);
            booleanQueryBuilder.add(query1, BooleanClause.Occur.SHOULD);
            booleanQueryBuilder.add(query2, BooleanClause.Occur.SHOULD);
            BooleanQuery booleanQuery = booleanQueryBuilder.build();

            TopDocs results = search(reader, searcher, booleanQuery);
            
            calculateIFIDF(reader, searcher, debugQuery.toString(documentField), results);
            
        } catch (IOException e) {
            System.out.println("Exception opening directory: " + e.getMessage());
        }
    }

    private TopDocs search(IndexReader reader, IndexSearcher searcher, Query query) throws IOException {
        TopDocs results = searcher.search(query, 100);
        ScoreDoc[] hits = results.scoreDocs;
        
        Comparator<ScoreDoc> scoreDescComparator = (ScoreDoc sd1, ScoreDoc sd2) -> Float.compare(sd2.score, sd1.score);
        Arrays.sort(hits, scoreDescComparator);
        
        return results;
    }
    
    private void calculateIFIDF(IndexReader reader, IndexSearcher searcher, String words, TopDocs results) throws IOException {
        ScoreDoc[] hits = results.scoreDocs;
        System.out.println("\nIDF");
        for (String word: words.split(" ")) {
            System.out.println(getIDF(reader, word));
        }
        System.out.println();
        
        int counter = 1;
        for (ScoreDoc hit: hits) {
            Document doc = searcher.doc(hit.doc);
            System.out.println(counter + ". " + doc.get(Constants.titleField) + " (score: " + hit.score + ")");
            for (String word: words.split(" ")) {
                System.out.println(getTF(reader, hit.doc, word));
            }
            counter++;
        }
    }
    
    private String normalizeSearchQuery(String query) {
        query = Normalizer.normalize(query, Normalizer.Form.NFD);
        query = query.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return query;
    }
    
    private String getTF(IndexReader reader, int docID, String word) throws IOException {
        ClassicSimilarity similarity = new ClassicSimilarity();
        int postingsFreq = 0;
        float wordFreq = 0;

        Term term = new Term(documentField, word);
        BytesRef bytesRef = term.bytes();
        PostingsEnum docsEnum = MultiFields.getTermDocsEnum(reader, documentField, bytesRef);
        int currentDocID;
        while ((currentDocID = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
            if (currentDocID == docID) {
                int _postingsFreq = docsEnum.freq();
                wordFreq += similarity.tf(_postingsFreq);
                postingsFreq += _postingsFreq;
            }
        }
        
        String printString = "\t" + word + ": TF = " + wordFreq + " (" + postingsFreq + " times in this document)";
        return printString;
    }
    
    private String getIDF(IndexReader reader, String word) throws IOException {
        ClassicSimilarity similarity = new ClassicSimilarity();
        int documentsFreq = 0;
        float idf = 0;

        Term term = new Term(documentField, word);
        int _documentsFreq = reader.docFreq(term);
        int documentsCount = reader.getDocCount(documentField);
        idf += similarity.idf(_documentsFreq, documentsCount);
        documentsFreq += _documentsFreq;

        String printString = word + ": " + idf + " (in " + documentsFreq + " documents)";
        return printString;
    }
}
