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

package com.io7m.callisto.resources.main;

import com.io7m.callisto.resources.api.CoResource;
import com.io7m.callisto.resources.api.CoResourceBundleParserError;
import com.io7m.callisto.resources.api.CoResourceBundleParserFileResolverType;
import com.io7m.callisto.resources.api.CoResourceBundleParserProviderType;
import com.io7m.callisto.resources.api.CoResourceBundleParserResult;
import com.io7m.callisto.resources.api.CoResourceBundleParserType;
import com.io7m.callisto.resources.api.CoResourceBundleParserWarning;
import com.io7m.callisto.resources.api.CoResourceID;
import com.io7m.callisto.resources.api.CoResourcePackageDeclaration;
import com.io7m.callisto.schemas.CoSchema;
import com.io7m.callisto.schemas.CoSchemas;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * The default implementation of the {@link CoResourceBundleParserProviderType}
 * interface.
 */

@Component(
  immediate = true,
  service = CoResourceBundleParserProviderType.class)
public final class CoResourceBundleParserProvider
  implements CoResourceBundleParserProviderType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CoResourceBundleParserProvider.class);
  }

  private volatile SAXParserFactory factory;
  private volatile SchemaFactory schema_factory;
  private volatile Schema schema;

  /**
   * Construct a parser provider.
   */

  public CoResourceBundleParserProvider()
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
      CoSchemas.bundleSchemas();

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
  public CoResourceBundleParserType createFromInputStream(
    final InputStream stream,
    final URI uri,
    final CoResourceBundleParserFileResolverType resolver)
  {
    NullCheck.notNull(stream, "Stream");
    NullCheck.notNull(uri, "URI");
    NullCheck.notNull(resolver, "Resolver");

    final Callable<SAXParser> supplier = () -> {
      synchronized (this.factory) {
        return this.factory.newSAXParser();
      }
    };

    return new Parser(supplier, stream, uri, resolver);
  }

  private static final class Parser implements CoResourceBundleParserType,
    ContentHandler, ErrorHandler
  {
    private final InputStream stream;
    private final URI uri;
    private final CoResourceBundleParserResult.Builder result;
    private final CoResourceBundleParserFileResolverType resolver;
    private boolean closed;
    private final Callable<SAXParser> parser_supplier;
    private ContentHandler format_handler;
    private Locator locator;

    Parser(
      final Callable<SAXParser> in_parser_supplier,
      final InputStream in_stream,
      final URI in_uri,
      final CoResourceBundleParserFileResolverType in_resolver)
    {
      this.parser_supplier =
        NullCheck.notNull(in_parser_supplier, "Parser supplier");
      this.stream =
        NullCheck.notNull(in_stream, "Stream");
      this.uri =
        NullCheck.notNull(in_uri, "URI");
      this.resolver =
        NullCheck.notNull(in_resolver, "Resolver");

      this.result = CoResourceBundleParserResult.builder();
    }

    @Override
    public CoResourceBundleParserResult parse()
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
        this.result.addErrors(CoResourceBundleParserError.of(
          this.uri,
          0,
          e.getMessage(),
          Optional.of(e)
        ));
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
      final List<CoSchema> schemas = CoSchemas.bundleSchemas();
      for (int index = 0; index < schemas.size(); ++index) {
        final CoSchema sch = schemas.get(index);
        if (Objects.equals(sch.namespaceURI().toString(), in_uri)) {
          if (LOG.isDebugEnabled()) {
            LOG.debug(
              "instantiating format {}.{} parser for {}",
              Integer.valueOf(sch.major()),
              Integer.valueOf(sch.minor()),
              this.uri);
          }

          switch (sch.major()) {
            case 1: {
              this.format_handler =
                new V1Handler(
                  this.uri,
                  this.locator,
                  this.resolver,
                  this.result);
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
      final String element_uri,
      final String local_name,
      final String qualified_name,
      final Attributes atts)
      throws SAXException
    {
      this.format_handler.startElement(
        element_uri, local_name, qualified_name, atts);
    }

    @Override
    public void endElement(
      final String element_uri,
      final String local_name,
      final String qualified_name)
      throws SAXException
    {
      this.format_handler.endElement(element_uri, local_name, qualified_name);
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
      this.result.addWarnings(CoResourceBundleParserWarning.of(
        this.uri,
        exception.getLineNumber(),
        exception.getMessage()));
    }

    @Override
    public void error(
      final SAXParseException exception)
      throws SAXException
    {
      this.result.addErrors(CoResourceBundleParserError.of(
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
      this.result.addErrors(CoResourceBundleParserError.of(
        this.uri,
        exception.getLineNumber(),
        exception.getMessage(),
        Optional.of(exception)));
    }
  }

  private static final class V1Handler implements ContentHandler
  {
    private final CoResourceBundleParserFileResolverType resolver;
    private final CoResourceBundleParserResult.Builder result;
    private final URI uri;
    private Object2ReferenceOpenHashMap<CoResourceID, CoResource> resources;
    private CoResourcePackageDeclaration.Builder package_builder;
    private String package_name;
    private Locator locator;

    V1Handler(
      final URI in_uri,
      final Locator in_locator,
      final CoResourceBundleParserFileResolverType in_resolver,
      final CoResourceBundleParserResult.Builder in_result)
    {
      this.uri =
        NullCheck.notNull(in_uri, "URI");
      this.locator =
        NullCheck.notNull(in_locator, "Locator");
      this.resolver =
        NullCheck.notNull(in_resolver, "Resolver");
      this.result =
        NullCheck.notNull(in_result, "Result");

      this.resources = new Object2ReferenceOpenHashMap<>();
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
      final String element_uri,
      final String local_name,
      final String qualified_name,
      final Attributes atts)
      throws SAXException
    {
      switch (local_name) {
        case "bundle": {
          break;
        }

        case "package": {
          this.finishPackage();
          this.package_builder = CoResourcePackageDeclaration.builder();
          this.package_name = atts.getValue("name");
          this.package_builder.setName(this.package_name);
          break;
        }

        case "resource": {
          final String file = atts.getValue("file");
          final String name = atts.getValue("name");
          final String type = atts.getValue("type");

          if (this.package_name == null) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("No package has been defined.");
            sb.append(System.lineSeparator());
            sb.append("  Resource: ");
            sb.append(name);
            sb.append(System.lineSeparator());
            this.result.addErrors(CoResourceBundleParserError.of(
              this.uri,
              this.locator.getLineNumber(),
              sb.toString(),
              Optional.empty()));
            return;
          }

          final URI file_uri;
          try {
            file_uri = this.resolver.resolve(file);
          } catch (final FileNotFoundException e) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("An exported file is missing.");
            sb.append(System.lineSeparator());
            sb.append("  File: ");
            sb.append(file);
            sb.append(System.lineSeparator());
            this.result.addErrors(CoResourceBundleParserError.of(
              this.uri,
              this.locator.getLineNumber(),
              sb.toString(),
              Optional.of(e)));
            return;
          }

          final CoResourceID resource_id =
            CoResourceID.of(this.package_name, name);
          final CoResource resource =
            CoResource.of(resource_id, type, file_uri);

          if (this.resources.containsKey(resource_id)) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("A resource is already exported.");
            sb.append(System.lineSeparator());
            sb.append("  Resource: ");
            sb.append(resource_id.qualifiedName());
            sb.append(System.lineSeparator());
            this.result.addErrors(CoResourceBundleParserError.of(
              this.uri,
              this.locator.getLineNumber(),
              sb.toString(),
              Optional.empty()));
            return;
          }

          this.resources.put(resource_id, resource);
          break;
        }

        default: {
          this.result.addWarnings(CoResourceBundleParserWarning.of(
            this.uri,
            this.locator.getLineNumber(),
            "Unrecognized element: " + local_name
          ));
        }
      }
    }

    private void finishPackage()
    {
      if (this.package_builder != null) {
        this.package_builder.putAllResources(this.resources);
        this.result.putPackages(
          this.package_name,
          this.package_builder.build());
        this.resources = new Object2ReferenceOpenHashMap<>();
        this.package_builder = null;
        this.package_name = null;
      }
    }

    @Override
    public void endElement(
      final String element_uri,
      final String local_name,
      final String qualified_name)
      throws SAXException
    {
      switch (local_name) {
        case "bundle": {
          this.finishPackage();
          break;
        }

        default: {
          this.result.addWarnings(CoResourceBundleParserWarning.of(
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
