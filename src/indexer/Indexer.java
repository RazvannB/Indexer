/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

/**
 *
 * @author razvan
 */
public class Indexer {

    private final Path indexPath = Paths.get(Constants.indexPath);
    private final Path docsPath = Paths.get(Constants.dataPath);
    private final boolean create;

    Indexer(boolean isCreated) {
        this.create = !isCreated;
    }

    public void startIndexing() throws ParseException, IOException {
        try {
            System.out.println("Indexing...");

            Directory dir = FSDirectory.open(indexPath);
            IndexWriterConfig config = new IndexWriterConfig(new CustomAnalyzer());
            config = create ? config.setOpenMode(OpenMode.CREATE) : config.setOpenMode(OpenMode.CREATE_OR_APPEND);
            config.setRAMBufferSizeMB(256.0);
            
            try (IndexWriter writer = new IndexWriter(dir, config)) {
                indexDocs(writer, docsPath);
            } catch (Exception e) {
                System.out.println("Exception creating index writer: " + e.getMessage());
            }
        } catch (IOException e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    private void indexDocs(final IndexWriter writer, Path path) throws IOException, TikaException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
                    } catch (IOException e) {
                        System.out.println("Exception indexing document: " + e.getMessage());
                    } catch (TikaException ex) {
                        Logger.getLogger(Indexer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }

    private void indexDoc(IndexWriter writer, Path filePath, long lastModified) throws IOException, TikaException {
        try (InputStream stream = Files.newInputStream(filePath)) {

            Document doc = new Document();

            // Title
            Field titleField;
            titleField = new StringField(Constants.titleField, filePath.getFileName().toString(), Field.Store.YES);
            doc.add(titleField);

            // Path
            Field pathField;
            pathField = new StringField(Constants.pathField, filePath.toString(), Field.Store.YES);
            doc.add(pathField);
            
            // Last modified
            doc.add(new LongPoint(Constants.modifiedField, lastModified));
            
            File file = new File(filePath.toString());
            Tika tika = new Tika();
            String fileContent = tika.parseToString(file);
            
            Field documentField;
            documentField = new TextField(Constants.documentField, fileContent, Field.Store.YES);
            doc.add(documentField);
            
            String[] wordsArray = fileContent.split(" ");
            String abstractString = "";
            String contentString = "";
            for (int i = 0; i < wordsArray.length; i++) {
                if (i < Constants.abstractLength) {
                    abstractString += wordsArray[i] + " ";
                } else {
                    contentString += wordsArray[i] + " ";
                }
            }
            
            Field abstractField;
            abstractField = new TextField(Constants.abstractField, abstractString, Field.Store.YES);
            doc.add(abstractField);
            abstractField.setBoost(Constants.abstractBoost);
            
            Field contentField;
            contentField = new TextField(Constants.contentsField, contentString, Field.Store.YES);
            doc.add(contentField);

            if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
                System.out.println("creating " + filePath);
                try {
                    writer.addDocument(doc);
                } catch (IOException e) {
                    System.out.println("Exception writing document: " + e.getMessage());
                }
            } else {
                System.out.println("updating " + filePath);
                Term term = new Term(Constants.pathField, filePath.toString());
                long updateDocument = writer.updateDocument(term, doc);
            }
        } catch (IOException e) {
            System.out.println("Exception creating input stream: " + e.getMessage());
        }
    }
}
