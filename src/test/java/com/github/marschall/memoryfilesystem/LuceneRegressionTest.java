package com.github.marschall.memoryfilesystem;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSLockFactory;
import org.apache.lucene.store.NIOFSDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class LuceneRegressionTest {

  @RegisterExtension
  final PosixFileSystemExtension extension = new PosixFileSystemExtension();

  @Test
  void issue113NIOFSDirectory() throws IOException {
    FileSystem fileSystem = this.extension.getFileSystem();
    Path indexPath = fileSystem.getPath("/index");

    FSLockFactory lockFactory = FSLockFactory.getDefault();
    Analyzer analyzer = new StandardAnalyzer();
    IndexWriterConfig writerConfiguration = new IndexWriterConfig(analyzer);
    try (Directory directory = new NIOFSDirectory(indexPath, lockFactory);
            IndexWriter indexWriter = new IndexWriter(directory, writerConfiguration)) {
      indexWriter.addDocument(new Document());
    }
  }

}
