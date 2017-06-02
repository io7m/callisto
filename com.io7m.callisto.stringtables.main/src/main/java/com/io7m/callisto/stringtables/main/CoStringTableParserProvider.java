/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.callisto.stringtables.main;

import com.io7m.callisto.schemas.CoSchema;
import com.io7m.callisto.schemas.CoSchemas;
import com.io7m.callisto.stringtables.api.CoStringTableParserError;
import com.io7m.callisto.stringtables.api.CoStringTableParserProviderType;
import com.io7m.callisto.stringtables.api.CoStringTableParserResult;
import com.io7m.callisto.stringtables.api.CoStringTableParserType;
import com.io7m.callisto.stringtables.api.CoStringTableParserWarning;
import com.io7m.callisto.stringtables.api.CoStringTableType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * The default implementation of the {@link CoStringTableParserProviderType}
 * interface.
 */

@Component
public final class CoStringTableParserProvider
  implements CoStringTableParserProviderType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CoStringTableParserProvider.class);
  }

  private volatile SAXParserFactory factory;
  private volatile SchemaFactory schema_factory;
  private volatile Schema schema;

  /**
   * Construct a string table parser provider.
   */

  public CoStringTableParserProvider()
  {

  }

  /**
   * Activate the parser provider.
   *
   * @throws IOException  On I/O errors
   * @throws SAXException On XML parser configuration errors
   */

  @Activate
  public void onActivate()
    throws IOException, SAXException
  {
    this.schema_factory =
      SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

    final List<CoSchema> schemas =
      CoSchemas.stringTableSchemas();

    final Source[] sources = new Source[schemas.size()];
    for (int index = 0; index < schemas.size(); ++index) {
      final CoSchema bundle_schema = schemas.get(index);
      final URL url = bundle_schema.fileURL();
      sources[index] = new StreamSource(url.toString());
    }

    this.schema = this.schema_factory.newSchema(sources);
    this.factory = SAXParserFactory.newInstance();
    this.factory.setValidating(false);
    this.factory.setNamespaceAware(true);
    this.factory.setSchema(this.schema);
  }

  @Override
  public CoStringTableParserType createParserFromStream(
    final InputStream stream,
    final URI uri,
    final String language)
  {
    NullCheck.notNull(stream, "Stream");
    NullCheck.notNull(uri, "URI");
    NullCheck.notNull(language, "Language");

    final Callable<SAXParser> supplier = () -> {
      synchronized (this.factory) {
        return this.factory.newSAXParser();
      }
    };

    return new Parser(supplier, stream, uri, language);
  }

  private static final class StringTable implements CoStringTableType
  {
    private final Object2ReferenceOpenHashMap<String, String> strings;

    StringTable(
      final Object2ReferenceOpenHashMap<String, String> in_strings)
    {
      this.strings = NullCheck.notNull(in_strings, "Strings");
    }

    @Override
    public String get(
      final String name)
    {
      NullCheck.notNull(name, "Name");

      final String result = this.strings.get(name);
      if (result == null) {
        throw new NoSuchElementException("No such string: " + name);
      }
      return result;
    }
  }

  private static final class Parser
    implements CoStringTableParserType, ContentHandler, ErrorHandler
  {
    private final InputStream stream;
    private final URI uri;
    private final CoStringTableParserResult.Builder result;
    private final String language;
    private boolean closed;
    private final Callable<SAXParser> parser_supplier;
    private ContentHandler format_handler;
    private Locator locator;

    Parser(
      final Callable<SAXParser> in_parser_supplier,
      final InputStream in_stream,
      final URI in_uri,
      final String in_language)
    {
      this.parser_supplier =
        NullCheck.notNull(in_parser_supplier, "Parser supplier");
      this.stream =
        NullCheck.notNull(in_stream, "Stream");
      this.uri =
        NullCheck.notNull(in_uri, "URI");
      this.language =
        NullCheck.notNull(in_language, "Language");

      this.result = CoStringTableParserResult.builder();
    }

    @Override
    public CoStringTableParserResult parse()
    {
      try {
        final SAXParser parser = this.parser_supplier.call();
        final XMLReader reader = parser.getXMLReader();
        reader.setContentHandler(this);
        reader.setErrorHandler(this);

        final InputSource source = new InputSource(this.stream);
        source.setPublicId(this.uri.toString());
        reader.parse(source);

        return this.result.build();
      } catch (final Exception e) {
        this.result.addErrors(CoStringTableParserError.of(
          this.uri,
          0,
          e.getMessage(),
          Optional.of(e)
        ));
        this.result.setTable(
          new StringTable(new Object2ReferenceOpenHashMap<>()));
        return this.result.build();
      }
    }

    @Override
    public void close()
      throws IOException
    {
      try {
        if (!this.closed) {
          this.stream.close();
        }
      } finally {
        this.closed = true;
      }
    }

    @Override
    public void setDocumentLocator(
      final Locator in_locator)
    {
      this.locator = NullCheck.notNull(in_locator, "Locator");
    }

    @Override
    public void startDocument()
      throws SAXException
    {

    }

    @Override
    public void endDocument()
      throws SAXException
    {

    }

    @Override
    public void startPrefixMapping(
      final String prefix,
      final String in_uri)
      throws SAXException
    {
      final List<CoSchema> schemas = CoSchemas.stringTableSchemas();
      for (int index = 0; index < schemas.size(); ++index) {
        final CoSchema sch = schemas.get(index);
        if (Objects.equals(sch.namespaceURI().toString(), in_uri)) {
          LOG.debug(
            "instantiating format {}.{} parser",
            Integer.valueOf(sch.major()),
            Integer.valueOf(sch.minor()));
          switch (sch.major()) {
            case 1: {
              this.format_handler =
                new V1Handler(
                  this.uri,
                  this.locator,
                  this.result,
                  this.language);
              return;
            }
            default: {
              throw new UnreachableCodeException();
            }
          }
        }
      }

      throw new SAXException("Unsupported format: " + in_uri);
    }

    @Override
    public void endPrefixMapping(
      final String prefix)
      throws SAXException
    {

    }

    @Override
    public void startElement(
      final String in_uri,
      final String local_name,
      final String qualified_name,
      final Attributes attributes)
      throws SAXException
    {
      this.format_handler.startElement(
        in_uri, local_name, qualified_name, attributes);
    }

    @Override
    public void endElement(
      final String in_uri,
      final String local_name,
      final String qualified_name)
      throws SAXException
    {
      this.format_handler.endElement(in_uri, local_name, qualified_name);
    }

    @Override
    public void characters(
      final char[] ch,
      final int start,
      final int length)
      throws SAXException
    {
      this.format_handler.characters(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(
      final char[] ch,
      final int start,
      final int length)
      throws SAXException
    {
      this.format_handler.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void processingInstruction(
      final String target,
      final String data)
      throws SAXException
    {
      this.format_handler.processingInstruction(target, data);
    }

    @Override
    public void skippedEntity(
      final String name)
      throws SAXException
    {
      this.format_handler.skippedEntity(name);
    }

    @Override
    public void warning(
      final SAXParseException exception)
      throws SAXException
    {
      this.result.addWarnings(CoStringTableParserWarning.of(
        this.uri,
        exception.getLineNumber(),
        exception.getMessage()));
    }

    @Override
    public void error(
      final SAXParseException exception)
      throws SAXException
    {
      this.result.addErrors(CoStringTableParserError.of(
        this.uri,
        exception.getLineNumber(),
        exception.getMessage(),
        Optional.of(exception)));
    }

    @Override
    public void fatalError(
      final SAXParseException exception)
      throws SAXException
    {
      this.result.addErrors(CoStringTableParserError.of(
        this.uri,
        exception.getLineNumber(),
        exception.getMessage(),
        Optional.of(exception)));
    }
  }

  private static final class V1Handler implements ContentHandler
  {
    private final URI uri;
    private final Locator locator;
    private final CoStringTableParserResult.Builder result;
    private final String language;
    private final Object2ReferenceOpenHashMap<String, String> strings;
    private boolean string_table;
    private String string_name;
    private boolean content_found;
    private boolean content_reading;

    V1Handler(
      final URI in_uri,
      final Locator in_locator,
      final CoStringTableParserResult.Builder in_result,
      final String in_language)
    {
      this.uri =
        NullCheck.notNull(in_uri, "URI");
      this.locator =
        NullCheck.notNull(in_locator, "Locator");
      this.result =
        NullCheck.notNull(in_result, "Result");
      this.language =
        NullCheck.notNull(in_language, "Language");

      this.strings =
        new Object2ReferenceOpenHashMap<>();

      this.string_table = false;
    }

    @Override
    public void setDocumentLocator(
      final Locator in_locator)
    {
      throw new UnreachableCodeException();
    }

    @Override
    public void startDocument()
      throws SAXException
    {
      throw new UnreachableCodeException();
    }

    @Override
    public void endDocument()
      throws SAXException
    {
      throw new UnreachableCodeException();
    }

    @Override
    public void startPrefixMapping(
      final String prefix,
      final String in_uri)
      throws SAXException
    {
      throw new UnreachableCodeException();
    }

    @Override
    public void endPrefixMapping(
      final String prefix)
      throws SAXException
    {
      throw new UnreachableCodeException();
    }

    @Override
    public void startElement(
      final String in_uri,
      final String local_name,
      final String qualified_name,
      final Attributes attributes)
      throws SAXException
    {
      switch (local_name) {

        case "string-table": {
          this.string_table = true;
          return;
        }

        case "string": {
          if (!this.string_table) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("String declared outside of string table.");
            sb.append(System.lineSeparator());
            throw new SAXParseException(sb.toString(), this.locator);
          }

          this.string_name = attributes.getValue("name");
          LOG.trace("string: {}", this.string_name);

          if (this.strings.containsKey(this.string_name)) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("Duplicate string.");
            sb.append(System.lineSeparator());
            sb.append("  String:   ");
            sb.append(this.string_name);
            sb.append(System.lineSeparator());
            sb.append("  Language: ");
            sb.append(this.language);
            sb.append(System.lineSeparator());
            throw new SAXParseException(sb.toString(), this.locator);
          }

          return;
        }

        case "content": {
          if (this.string_name == null) {
            throw new SAXParseException(
              "Content specified outside of string declaration.",
              this.locator);
          }

          if (!this.content_found) {
            final String element_language =
              attributes.getValue("language");
            this.content_found =
              Objects.equals(this.language, element_language);
            this.content_reading = true;
          }

          return;
        }

        default: {
          this.result.addWarnings(CoStringTableParserWarning.of(
            this.uri,
            this.locator.getLineNumber(),
            "Unrecognized element: " + local_name
          ));
        }
      }
    }

    @Override
    public void endElement(
      final String in_uri,
      final String local_name,
      final String qualified_name)
      throws SAXException
    {
      switch (local_name) {

        case "string-table": {
          this.result.setTable(new StringTable(this.strings));
          return;
        }

        case "string": {
          try {
            if (!this.content_found) {
              final StringBuilder sb = new StringBuilder(128);
              sb.append("Missing language for string.");
              sb.append(System.lineSeparator());
              sb.append("  String:   ");
              sb.append(this.string_name);
              sb.append(System.lineSeparator());
              sb.append("  Language: ");
              sb.append(this.language);
              sb.append(System.lineSeparator());
              throw new SAXParseException(sb.toString(), this.locator);
            }

            this.string_name = null;
          } finally {
            this.content_found = false;
            this.content_reading = false;
          }
          return;
        }

        case "content": {
          this.content_reading = false;
          return;
        }

        default: {
          this.result.addWarnings(CoStringTableParserWarning.of(
            this.uri,
            this.locator.getLineNumber(),
            "Unrecognized element: " + local_name
          ));
        }
      }
    }

    @Override
    public void characters(
      final char[] ch,
      final int start,
      final int length)
      throws SAXException
    {
      if (this.content_reading) {
        LOG.trace(
          "text: {} at {}",
          Integer.valueOf(length),
          Integer.valueOf(start));

        this.strings.put(this.string_name, new String(ch, start, length));
      }
    }

    @Override
    public void ignorableWhitespace(
      final char[] ch,
      final int start,
      final int length)
      throws SAXException
    {

    }

    @Override
    public void processingInstruction(
      final String target,
      final String data)
      throws SAXException
    {

    }

    @Override
    public void skippedEntity(
      final String name)
      throws SAXException
    {

    }
  }
}
