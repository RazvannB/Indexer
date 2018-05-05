/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.IOException;
import org.apache.lucene.queryparser.classic.ParseException;

/**
 *
 * @author razvan
 */
public class Configuration {
    
    private static final boolean CREATED = true;
    private static final boolean UPDATED = false;
    
    public static void main(String args[]) throws ParseException, IOException {
        
        // TODO: Ordonare doc dupa scor, TF si IDF
        // TODO: 20 cuv de la inceput. x3 importanta la scor in primele 20
        
        if (CREATED == false || UPDATED == true) {
            Indexer indexer = new Indexer(UPDATED);
            indexer.startIndexing();
        } else { 
            // Retrieve 
            Retriever retriever = new Retriever();
            retriever.startRetriever();
        }
    }
}
