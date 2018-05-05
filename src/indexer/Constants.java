/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

/**
 *
 * @author razvan
 */
public class Constants {
    //  Paths
    static String indexPath = "/Users/razvan/Local/Projects/Faculty/Indexer/output";
    static String dataPath = "/Users/razvan/Local/Projects/Faculty/Indexer/data";
    static String queriesPath = "/Users/razvan/Local/Projects/Faculty/Indexer/input/queries.txt";
    static String stopwordsPath = "/Users/razvan/Local/Projects/Faculty/Indexer/input/stopwords.txt";
    
    //  Fields
    static String contentsField = "contents";
    static String abstractField = "abstract";
    static String documentField = "document";
    static String pathField = "path";
    static String titleField = "title";   
    static String modifiedField = "modified";
    
    //  Utils
    static int abstractLength = 20;
    static int abstractBoost = 3;
}
