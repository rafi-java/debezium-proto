/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 * 
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.debezium.message;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.debezium.annotation.ThreadSafe;

/**
 * Reads {@link Document} instances from a variety of input forms.
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public interface DocumentReader {

    /**
     * Get the default {@link DocumentReader} instance.
     * 
     * @return the shared default reader instance; never null
     */
    static DocumentReader defaultReader() {
        return JacksonReader.INSTANCE;
    }

    /**
     * Read a document from the supplied stream.
     * 
     * @param jsonStream the input stream to be read; may not be null
     * @return the document instance; never null
     * @throws IOException if a document could not be read from the supplied stream
     */
    Document read(InputStream jsonStream) throws IOException;

    /**
     * Read a document from the supplied {@link Reader}.
     * 
     * @param jsonReader the reader to be read; may not be null
     * @return the document instance; never null
     * @throws IOException if a document could not be read from the supplied reader
     */
    Document read(Reader jsonReader) throws IOException;

    /**
     * Read a document from the supplied JSON-formatted string.
     * 
     * @param json the JSON string representation to be read; may not be null
     * @return the document instance; never null
     * @throws IOException if a document could not be read from the supplied string
     */
    Document read(String json) throws IOException;

    /**
     * Read a document from the content at the given URL.
     * 
     * @param jsonUrl the URL to the content that is to be read; may not be null
     * @return the document instance; never null
     * @throws IOException if a document could not be read from the supplied content
     */
    default Document read(URL jsonUrl) throws IOException {
        return read(jsonUrl.openStream());
    }

    /**
     * Read a document from the supplied file.
     * 
     * @param jsonFile the file to be read; may not be null
     * @return the document instance; never null
     * @throws IOException if a document could not be read from the supplied file
     */
    default Document read(File jsonFile) throws IOException {
        return read( new BufferedInputStream(new FileInputStream(jsonFile)) );
    }

    /**
     * Read a document from the supplied bytes.
     * 
     * @param rawBytes the UTF-8 bytes to be read; may not be null
     * @return the document instance; never null
     * @throws IOException if a document could not be read from the supplied bytes
     */
    default Document read(byte[] rawBytes) throws IOException {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(rawBytes)) {
            return DocumentReader.defaultReader().read(stream);
        }
    }

}
