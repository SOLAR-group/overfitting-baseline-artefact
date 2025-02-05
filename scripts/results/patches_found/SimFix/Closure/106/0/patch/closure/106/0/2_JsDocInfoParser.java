/*
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.javascript.jscomp.parsing;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMapBuilder;
import com.google.common.collect.Sets;
import com.google.javascript.jscomp.mozilla.rhino.ErrorReporter;
import com.google.javascript.rhino.JSDocInfo;
import com.google.javascript.rhino.JSDocInfo.Visibility;
import com.google.javascript.rhino.JSDocInfoBuilder;
import com.google.javascript.rhino.JSTypeExpression;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.ScriptRuntime;
import com.google.javascript.rhino.Token;
import com.google.javascript.rhino.jstype.JSType;
import com.google.javascript.rhino.jstype.JSTypeNative;
import com.google.javascript.rhino.jstype.JSTypeRegistry;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A parser for JSDoc comments.
 *
*
 */
// TODO(nicksantos): Unify all the JSDocInfo stuff into one package, instead of
// spreading it across mutliple packages.
public final class JsDocInfoParser {

  private final JsDocTokenStream stream;
  private final JSTypeRegistry typeRegistry;
  private final JSDocInfoBuilder jsdocBuilder;
  private final String sourceName;
  private final ErrorReporter errorReporter;
  private final ErrorReporterParser parser = new ErrorReporterParser();

  private class ErrorReporterParser {
    void addWarning(String messageId, String messageArg, int lineno,
                    int charno) {
      errorReporter.warning(ScriptRuntime.getMessage1(messageId, messageArg),
          sourceName, lineno, null, charno);
    }
    void addWarning(String messageId, int lineno, int charno) {
      errorReporter.warning(ScriptRuntime.getMessage0(messageId),
          sourceName, lineno, null, charno);
    }
  }

  // The DocInfo with the fileoverview tag for the whole file.
  private JSDocInfo fileOverviewJSDocInfo = null;
  private State state;

  private static final Map<String, Annotation> recognizedAnnotations =
      new ImmutableMapBuilder<String, Annotation>().
      put("argument", Annotation.PARAM).
      put("author", Annotation.AUTHOR).
      put("const", Annotation.CONSTANT).
      put("constant", Annotation.CONSTANT).
      put("constructor", Annotation.CONSTRUCTOR).
      put("define", Annotation.DEFINE).
      put("deprecated", Annotation.DEPRECATED).
      put("desc", Annotation.DESC).
      put("enum", Annotation.ENUM).
      put("export", Annotation.EXPORT).
      put("extends", Annotation.EXTENDS).
      put("fileoverview", Annotation.FILE_OVERVIEW).
      put("final", Annotation.CONSTANT).
      put("hidden", Annotation.HIDDEN).
      put("implements", Annotation.IMPLEMENTS).
      put("implicitCast", Annotation.IMPLICIT_CAST).
      put("inheritDoc", Annotation.INHERIT_DOC).
      put("interface", Annotation.INTERFACE).
      put("license", Annotation.LICENSE).
      put("noalias", Annotation.NO_ALIAS).
      put("noshadow", Annotation.NO_SHADOW).
      put("nosideeffects", Annotation.NO_SIDE_EFFECTS).
      put("notypecheck", Annotation.NO_TYPE_CHECK).
      put("override", Annotation.OVERRIDE).
      put("owner", Annotation.AUTHOR).
      put("param", Annotation.PARAM).
      put("preserve", Annotation.PRESERVE).
      put("preserveTry", Annotation.PRESERVE_TRY).
      put("private", Annotation.PRIVATE).
      put("protected", Annotation.PROTECTED).
      put("public", Annotation.PUBLIC).
      put("return", Annotation.RETURN).
      put("returns", Annotation.RETURN).
      put("see", Annotation.SEE).
      put("suppress", Annotation.SUPPRESS).
      put("template", Annotation.TEMPLATE).
      put("this", Annotation.THIS).
      put("throws", Annotation.THROWS).
      put("type", Annotation.TYPE).
      put("typedef", Annotation.TYPEDEF).
      put("version", Annotation.VERSION).
      getMap();

  private final Map<String, Annotation> annotationNames;

  private Node.FileLevelJsDocBuilder fileLevelJsDocBuilder;

  /**
   * Sets the JsDocBuilder for the file-level (root) node of this parse. The
   * parser uses the builder to append any preserve annotations it encounters
   * in jsdoc comments.
   *
   * @param fileLevelJsDocBuilder
   */
  void setFileLevelJsDocBuilder(
      Node.FileLevelJsDocBuilder fileLevelJsDocBuilder) {
    this.fileLevelJsDocBuilder = fileLevelJsDocBuilder;
  }

  /**
   * Sets the file overview JSDocInfo, in order to warn about multiple uses of
   * the @fileoverview tag in a file.
   */
  void setFileOverviewJSDocInfo(JSDocInfo fileOverviewJSDocInfo) {
    this.fileOverviewJSDocInfo = fileOverviewJSDocInfo;
  }

  private enum State {
    SEARCHING_ANNOTATION,
    SEARCHING_NEWLINE,
    NEXT_IS_ANNOTATION
  }

  private enum Annotation {
    AUTHOR,
    CONSTANT,
    CONSTRUCTOR,
    DEFINE,
    DEPRECATED,
    DESC,
    ENUM,
    EXTENDS,
    EXPORT,
    FILE_OVERVIEW,
    HIDDEN,
    IMPLEMENTS,
    IMPLICIT_CAST,
    INHERIT_DOC,
    INTERFACE,
    LICENSE, // same as preserve
    NO_ALIAS,
    NO_SHADOW,
    NO_SIDE_EFFECTS,
    NO_TYPE_CHECK,
    NOT_IMPLEMENTED,
    OVERRIDE,
    PARAM,
    PRESERVE, // same as license
    PRESERVE_TRY,
    PRIVATE,
    PROTECTED,
    PUBLIC,
    RETURN,
    SEE,
    SUPPRESS,
    TEMPLATE,
    THIS,
    THROWS,
    TYPE,
    TYPEDEF,
    VERSION
  }

  JsDocInfoParser(JsDocTokenStream stream,
                  String sourceName,
                  Config config,
                  ErrorReporter errorReporter) {
    this.stream = stream;
    this.sourceName = sourceName;
    this.typeRegistry = config.registry;
    this.jsdocBuilder = new JSDocInfoBuilder(config.parseJsDocDocumentation);
    this.annotationNames = buildAnnotationNames(config);

    this.errorReporter = errorReporter;
  }

  /** Create the annotation names from the user-specified config. */
  private static Map<String, Annotation> buildAnnotationNames(Config config) {
    ImmutableMapBuilder<String, Annotation> annotationBuilder =
        ImmutableMapBuilder.fromMap(recognizedAnnotations);
    for (String unrecognizedAnnotation : config.annotationWhitelist) {
      if (!recognizedAnnotations.containsKey(unrecognizedAnnotation)) {
        annotationBuilder.put(
            unrecognizedAnnotation, Annotation.NOT_IMPLEMENTED);
      }
    }
    return annotationBuilder.getMap();
  }

  /**
   * Parses a string containing a JsDoc type declaration, returning the
   * type if the parsing succeeded or {@code null} if it failed.
   */
  public static Node parseTypeString(String typeString) {
    Config config = new Config(
        new JSTypeRegistry(NullErrorReporter.forOldRhino()),
        Sets.<String>newHashSet(),
        false);
    JsDocInfoParser parser = new JsDocInfoParser(
        new JsDocTokenStream(typeString),
        "typeparsing",
        config,
        NullErrorReporter.forNewRhino());

    return parser.parseTopLevelTypeExpression(parser.next());
  }

  /**
   * Parses a {@link JSDocInfo} object. This parsing method reads all tokens
   * returned by the {@link JsDocTokenStream#getJsDocToken()} method until the
   * {@link JsDocToken#EOC} is returned.
   *
   * @return {@code true} if JSDoc information was correctly parsed,
   *     {@code false} otherwise
   */
  boolean parse() {
    int lineno;
    int charno;

    // JSTypes are represented as Rhino AST nodes, and then resolved later.
    JSTypeExpression type;

    state = State.SEARCHING_ANNOTATION;
    JsDocToken token = next();

    ExtractionInfo blockInfo = extractBlockComment(token);
    token = blockInfo.token;

    // If we have a block level comment, record it.
    if (blockInfo.string.length() > 0) {
      jsdocBuilder.recordBlockDescription(blockInfo.string);
    }

    // Parse the actual JsDoc.
    retry: for (;;) {
      switch (token) {
        case ANNOTATION:
          if (state == State.SEARCHING_ANNOTATION) {
            state = State.SEARCHING_NEWLINE;
            lineno = stream.getLineno();
            charno = stream.getCharno();

            String annotationName = stream.getString();
            Annotation annotation = annotationNames.get(annotationName);
            if (annotation == null) {
              parser.addWarning("msg.bad.jsdoc.tag", annotationName,
                  stream.getLineno(), stream.getCharno());
            } else {
              // Mark the beginning of the annotation.
              jsdocBuilder.markAnnotation(annotationName, lineno, charno);

              switch (annotation) {
                case AUTHOR:
                  ExtractionInfo authorInfo = extractSingleLineBlock();
                  String author = authorInfo.string;

                  if (author.length() == 0) {
                    parser.addWarning("msg.jsdoc.authormissing",
                          stream.getLineno(), stream.getCharno());
                  } else {
                    jsdocBuilder.addAuthor(author);
                  }

                  token = authorInfo.token;
                  continue retry;

                case CONSTANT:
                  if (!jsdocBuilder.recordConstancy()) {
                    parser.addWarning("msg.jsdoc.const",
                        stream.getLineno(), stream.getCharno());
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case CONSTRUCTOR:
                  if (!jsdocBuilder.recordConstructor()) {
                    if (jsdocBuilder.isInterfaceRecorded()) {
                      parser.addWarning("msg.jsdoc.interface.constructor",
                          stream.getLineno(), stream.getCharno());
                    } else {
                      parser.addWarning("msg.jsdoc.incompat.type",
                          stream.getLineno(), stream.getCharno());
                    }
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case DEPRECATED:
                  if (!jsdocBuilder.recordDeprecated()) {
                    parser.addWarning("msg.jsdoc.deprecated",
                        stream.getLineno(), stream.getCharno());
                  }

                  // Find the reason/description, if any.
                  ExtractionInfo reasonInfo =
                      extractMultilineTextualBlock(token);

                  String reason = reasonInfo.string;

                  if (reason.length() > 0) {
                    jsdocBuilder.recordDeprecationReason(reason);
                  }

                  token = reasonInfo.token;
                  continue retry;

                case INTERFACE:
                  if (!jsdocBuilder.recordInterface()) {
                    if (jsdocBuilder.isConstructorRecorded()) {
                      parser.addWarning("msg.jsdoc.interface.constructor",
                          stream.getLineno(), stream.getCharno());
                    } else {
                      parser.addWarning("msg.jsdoc.incompat.type",
                          stream.getLineno(), stream.getCharno());
                    }
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case DESC:
                  if (jsdocBuilder.isDescriptionRecorded()) {
                    parser.addWarning("msg.jsdoc.desc.extra",
                        stream.getLineno(), stream.getCharno());
                    token = eatTokensUntilEOL();
                    continue retry;
                  } else {
                    ExtractionInfo descriptionInfo =
                        extractMultilineTextualBlock(token);

                    String description = descriptionInfo.string;

                    jsdocBuilder.recordDescription(description);
                    token = descriptionInfo.token;
                    continue retry;
                  }

                case FILE_OVERVIEW:
                  ExtractionInfo fileOverviewInfo =
                      extractMultilineTextualBlock(token,
                                                   WhitespaceOption.TRIM);

                  String fileOverview = fileOverviewInfo.string;

                  if (!jsdocBuilder.recordFileOverview(fileOverview) ||
                      fileOverviewJSDocInfo != null) {
                    parser.addWarning("msg.jsdoc.fileoverview.extra",
                        stream.getLineno(), stream.getCharno());
                  }
                  token = fileOverviewInfo.token;
                  continue retry;

                case LICENSE:
                case PRESERVE:
                  ExtractionInfo preserveInfo =
                      extractMultilineTextualBlock(token,
                                                   WhitespaceOption.PRESERVE);

                  String preserve = preserveInfo.string;

                  if (preserve.length() > 0) {
                    if (fileLevelJsDocBuilder != null) {
                      fileLevelJsDocBuilder.append(preserve);
                    }
                  }

                  token = preserveInfo.token;
                  continue retry;

                case ENUM:
                  token = next();
                  lineno = stream.getLineno();
                  charno = stream.getCharno();

                  type = null;
                  if (token != JsDocToken.EOL && token != JsDocToken.EOC) {
                    type = createJSTypeExpression(
                        parseAndRecordTypeNode(token));
                  }

                  if (type == null) {
                    type = createJSTypeExpression(newStringNode("number"));
                  }
                  if (!jsdocBuilder.recordEnumParameterType(type)) {
                    parser.addWarning("msg.jsdoc.incompat.type", lineno, charno);
                  }
                  token = eatTokensUntilEOL(token);
                  continue retry;

                case EXPORT:
                  if (!jsdocBuilder.recordExport()) {
                    parser.addWarning("msg.jsdoc.export",
                        stream.getLineno(), stream.getCharno());
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case EXTENDS:
                case IMPLEMENTS:
                  skipEOLs();
                  token = next();
                  lineno = stream.getLineno();
                  charno = stream.getCharno();
                  boolean matchingRc = false;

                  if (token == JsDocToken.LC) {
                    token = next();
                    matchingRc = true;
                  }

                  if (token == JsDocToken.STRING) {
                    Node typeNode = parseAndRecordTypeNameNode(
                        token, lineno, charno, matchingRc);

                    lineno = stream.getLineno();
                    charno = stream.getCharno();

                    typeNode = wrapNode(Token.BANG, typeNode);
                    if (typeNode != null && !matchingRc) {
                      typeNode.putBooleanProp(Node.BRACELESS_TYPE, true);
                    }
                    type = createJSTypeExpression(typeNode);

                    if (annotation == Annotation.EXTENDS) {
                      if (!jsdocBuilder.recordBaseType(type)) {
                        parser.addWarning(
                            "msg.jsdoc.incompat.type", lineno, charno);
                      }
                    } else {
                      Preconditions.checkState(
                          annotation == Annotation.IMPLEMENTS);
                      if (!jsdocBuilder.recordImplementedInterface(type)) {
                        parser.addWarning("msg.jsdoc.implements.duplicate",
                            lineno, charno);
                      }
                    }
                    token = next();
                    if (matchingRc) {
                      if (token != JsDocToken.RC) {
                        parser.addWarning("msg.jsdoc.missing.rc",
                            stream.getLineno(), stream.getCharno());
                      }
                    } else if (token != JsDocToken.EOL &&
                        token != JsDocToken.EOF && token != JsDocToken.EOC) {
                      parser.addWarning("msg.end.annotation.expected",
                          stream.getLineno(), stream.getCharno());
                    }
                  } else {
                    parser.addWarning("msg.no.type.name", lineno, charno);
                  }
                  token = eatTokensUntilEOL(token);
                  continue retry;

                case HIDDEN:
                  if (!jsdocBuilder.recordHiddenness()) {
                    parser.addWarning("msg.jsdoc.hidden",
                        stream.getLineno(), stream.getCharno());
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case NO_ALIAS:
                  if (!jsdocBuilder.recordNoAlias()) {
                    parser.addWarning("msg.jsdoc.noalias",
                        stream.getLineno(), stream.getCharno());
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case NO_TYPE_CHECK:
                  if (!jsdocBuilder.recordNoTypeCheck()) {
                    parser.addWarning("msg.jsdoc.nocheck",
                        stream.getLineno(), stream.getCharno());
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case NOT_IMPLEMENTED:
                  token = eatTokensUntilEOL();
                  continue retry;

                case INHERIT_DOC:
                case OVERRIDE:
                  if (!jsdocBuilder.recordOverride()) {
                    parser.addWarning("msg.jsdoc.override",
                        stream.getLineno(), stream.getCharno());
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case THROWS:
                  skipEOLs();
                  token = next();
                  lineno = stream.getLineno();
                  charno = stream.getCharno();
                  type = null;

                  if (token == JsDocToken.LC) {
                    type = createJSTypeExpression(
                        parseAndRecordTypeNode(token));

                    if (type == null) {
                      // parsing error reported during recursive descent
                      // recovering parsing
                      token = eatTokensUntilEOL();
                      continue retry;
                    }
                  }

                  // *Update* the token to that after the type annotation.
                  token = current();

                  // Save the throw type.
                  jsdocBuilder.recordThrowType(type);

                  // Find the throw's description (if applicable).
                  ExtractionInfo descriptionInfo =
                      extractMultilineTextualBlock(token);

                  String description = descriptionInfo.string;

                  if (description.length() > 0) {
                    jsdocBuilder.recordThrowDescription(type, description);
                  }

                  token = descriptionInfo.token;
                  continue retry;

                case PARAM:
                  skipEOLs();
                  token = next();
                  lineno = stream.getLineno();
                  charno = stream.getCharno();
                  type = null;

                  if (token == JsDocToken.LC) {
                    type = createJSTypeExpression(
                        parseAndRecordParamTypeNode(token));

                    if (type == null) {
                      // parsing error reported during recursive descent
                      // recovering parsing
                      token = eatTokensUntilEOL();
                      continue retry;
                    }
                    skipEOLs();
                    token = next();
                    lineno = stream.getLineno();
                    charno = stream.getCharno();
                  }

                  String name = null;

                  if (JsDocToken.STRING != token) {
                    parser.addWarning("msg.missing.variable.name",
                        lineno, charno);
                  } else {
                    name = stream.getString();
                    if (!jsdocBuilder.recordParameter(name, type)) {
                      if (jsdocBuilder.hasParameter(name)) {
                        parser.addWarning("msg.dup.variable.name", name,
                            lineno, charno);
                      } else {
                        parser.addWarning("msg.jsdoc.incompat.type", name,
                            lineno, charno);
                      }
                    }
                  }

                  if (name == null) {
                    token = eatTokensUntilEOL(token);
                    continue retry;
                  }

                  jsdocBuilder.markName(name, lineno, charno);

                  // Find the parameter's description (if applicable).
                  ExtractionInfo paramDescriptionInfo =
                      extractMultilineTextualBlock(token);

                  String paramDescription = paramDescriptionInfo.string;

                  if (paramDescription.length() > 0) {
                    jsdocBuilder.recordParameterDescription(name,
                                                            paramDescription);
                  }

                  token = paramDescriptionInfo.token;
                  continue retry;

                case PRESERVE_TRY:
                  if (!jsdocBuilder.recordPreserveTry()) {
                    parser.addWarning("msg.jsdoc.preservertry",
                        stream.getLineno(), stream.getCharno());
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case PRIVATE:
                  if (!jsdocBuilder.recordVisibility(Visibility.PRIVATE)) {
                    parser.addWarning("msg.jsdoc.visibility.private",
                        stream.getLineno(), stream.getCharno());
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case PROTECTED:
                  if (!jsdocBuilder.recordVisibility(Visibility.PROTECTED)) {
                    parser.addWarning("msg.jsdoc.visibility.protected",
                        stream.getLineno(), stream.getCharno());
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case PUBLIC:
                  if (!jsdocBuilder.recordVisibility(Visibility.PUBLIC)) {
                    parser.addWarning("msg.jsdoc.visibility.public",
                        stream.getLineno(), stream.getCharno());
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case NO_SHADOW:
                  if (!jsdocBuilder.recordNoShadow()) {
                    parser.addWarning("msg.jsdoc.noshadow",
                        stream.getLineno(), stream.getCharno());
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case NO_SIDE_EFFECTS:
                  if (!jsdocBuilder.recordNoSideEffects()) {
                    parser.addWarning("msg.jsdoc.nosideeffects",
                        stream.getLineno(), stream.getCharno());
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case IMPLICIT_CAST:
                  if (!jsdocBuilder.recordImplicitCast()) {
                    parser.addWarning("msg.jsdoc.implicitcast",
                        stream.getLineno(), stream.getCharno());
                  }
                  token = eatTokensUntilEOL();
                  continue retry;

                case SEE:
                  ExtractionInfo referenceInfo = extractSingleLineBlock();
                  String reference = referenceInfo.string;

                  if (reference.length() == 0) {
                    parser.addWarning("msg.jsdoc.seemissing",
                          stream.getLineno(), stream.getCharno());
                  } else {
                    jsdocBuilder.addReference(reference);
                  }

                  token = referenceInfo.token;
                  continue retry;

                case SUPPRESS:
                  token = parseSuppressTag(next());
                  continue retry;

                case TEMPLATE:
                  ExtractionInfo templateInfo = extractSingleLineBlock();
                  String templateTypeName = templateInfo.string;

                  if (templateTypeName.length() == 0) {
                    parser.addWarning("msg.jsdoc.templatemissing",
                          stream.getLineno(), stream.getCharno());
                  } else if (!jsdocBuilder.recordTemplateTypeName(
                      templateTypeName)) {
                    parser.addWarning("msg.jsdoc.template.at.most.once",
                        stream.getLineno(), stream.getCharno());
                  }

                  token = templateInfo.token;
                  continue retry;

                case VERSION:
                  ExtractionInfo versionInfo = extractSingleLineBlock();
                  String version = versionInfo.string;

                  if (version.length() == 0) {
                    parser.addWarning("msg.jsdoc.versionmissing",
                          stream.getLineno(), stream.getCharno());
                  } else {
                    if (!jsdocBuilder.recordVersion(version)) {
                       parser.addWarning("msg.jsdoc.extraversion",
                          stream.getLineno(), stream.getCharno());
                    }
                  }

                  token = versionInfo.token;
                  continue retry;

                case DEFINE:
                case RETURN:
                case THIS:
                case TYPE:
                case TYPEDEF:
                  skipEOLs();
                  lineno = stream.getLineno();
                  charno = stream.getCharno();

                  token = next();

                  Node typeNode = parseAndRecordTypeNode(token, lineno, charno);

                  if (annotation == Annotation.THIS) {
                    typeNode = wrapNode(Token.BANG, typeNode);
                    if (typeNode != null && token != JsDocToken.LC) {
                      typeNode.putBooleanProp(Node.BRACELESS_TYPE, true);
                    }
                  }
                  type = createJSTypeExpression(typeNode);

                  if (type == null) {
                    // error reported during recursive descent
                    // recovering parsing
                  } else {
                    switch (annotation) {
                      case DEFINE:
                        if (!isValidDefineType(typeNode)) {
                          parser.addWarning("msg.jsdoc.define.badtype",
                              lineno, charno);
                        } else if (!jsdocBuilder.recordDefineType(type)) {
                          parser.addWarning("msg.jsdoc.define",
                              lineno, charno);
                        }
                        break;

                      case RETURN:
                        if (!jsdocBuilder.recordReturnType(type)) {
                          parser.addWarning(
                              "msg.jsdoc.incompat.type", lineno, charno);
                          break;
                        }

                        // *Update* the token to that after the type annotation.
                        token = current();

                        // Find the return's description (if applicable).
                        ExtractionInfo returnDescriptionInfo =
                            extractMultilineTextualBlock(token);

                        String returnDescription = returnDescriptionInfo.string;

                        if (returnDescription.length() > 0) {
                          jsdocBuilder.recordReturnDescription(
                              returnDescription);
                        }

                        token = returnDescriptionInfo.token;
                        continue retry;

                      case THIS:
                        if (!jsdocBuilder.recordThisType(type)) {
                          parser.addWarning(
                              "msg.jsdoc.incompat.type", lineno, charno);
                        }
                        break;

                      case TYPE:
                        if (!jsdocBuilder.recordType(type)) {
                          parser.addWarning(
                              "msg.jsdoc.incompat.type", lineno, charno);
                        }
                        break;

                      case TYPEDEF:
                        if (!jsdocBuilder.recordTypedef(type)) {
                          parser.addWarning(
                              "msg.jsdoc.incompat.type", lineno, charno);
                        }
                        break;
                    }

                    token = eatTokensUntilEOL();
                  }
                  continue retry;
              }
            }
          }
          break;

        case EOC:
          if (hasParsedFileOverviewDocInfo()) {
            fileOverviewJSDocInfo = retrieveAndResetParsedJSDocInfo();
          }

          return true;

        case EOF:
          // discard any accumulated information
          jsdocBuilder.build(null);
          parser.addWarning("msg.unexpected.eof",
              stream.getLineno(), stream.getCharno());
          return false;

        case EOL:
          if (state == State.SEARCHING_NEWLINE) {
            state = State.SEARCHING_ANNOTATION;
          }
          token = next();
          continue retry;

        default:
          if (token == JsDocToken.STAR && state == State.SEARCHING_ANNOTATION) {
            token = next();
            continue retry;
          } else {
            state = State.SEARCHING_NEWLINE;
            token = eatTokensUntilEOL();
            continue retry;
          }
      }

      // next token
      token = next();
    }
  }

  /**
   * Parse a {@code @suppress} tag of the form
   * {@code @suppress&#123;warning1|warning2&#125;}.
   *
   * @param token The current token.
   */
  private JsDocToken parseSuppressTag(JsDocToken token) {
    if (token == JsDocToken.LC) {
      Set<String> suppressions = new HashSet<String>();
      while (true) {
        if (match(JsDocToken.STRING)) {
          suppressions.add(stream.getString());
          token = next();
        } else {
          parser.addWarning("msg.jsdoc.suppress",
              stream.getLineno(), stream.getCharno());
          return token;
        }

        if (match(JsDocToken.PIPE)) {
          token = next();
        } else {
          break;
        }
      }

      if (!match(JsDocToken.RC)) {
        parser.addWarning("msg.jsdoc.suppress",
            stream.getLineno(), stream.getCharno());
      } else {
        token = next();
        if (!jsdocBuilder.recordSuppressions(suppressions)) {
          parser.addWarning("msg.jsdoc.suppress.duplicate",
              stream.getLineno(), stream.getCharno());
        }
      }
    }
    return token;
  }

  /**
   * Looks for a type expression at the current token and if found,
   * returns it. Note that this method consumes input.
   *
   * @param token The current token.
   * @return The type expression found or null if none.
   */
  private Node parseAndRecordTypeNode(JsDocToken token) {
    return parseAndRecordTypeNode(token, token == JsDocToken.LC);
  }

  /**
   * Looks for a type expression at the current token and if found,
   * returns it. Note that this method consumes input.
   *
   * @param token The current token.
   * @param matchingLC Whether the type expression starts with a "{".
   * @return The type expression found or null if none.
   */
  private Node parseAndRecordTypeNode(JsDocToken token, boolean matchingLC) {
    return parseAndRecordTypeNode(token, stream.getLineno(), stream.getCharno(),
        matchingLC, false);
  }

  /**
   * Looks for a type expression at the current token and if found,
   * returns it. Note that this method consumes input.
   *
   * @param token The current token.
   * @param lineno The line of the type expression.
   * @param startCharno The starting character position of the type expression.
   * @return The type expression found or null if none.
   */
  private Node parseAndRecordTypeNode(JsDocToken token, int lineno,
      int startCharno) {
    return parseAndRecordTypeNode(token, lineno, startCharno,
        token == JsDocToken.LC, false);
  }

  /**
   * Looks for a type expression at the current token and if found,
   * returns it. Note that this method consumes input.
   *
   * @param token The current token.
   * @param lineno The line of the type expression.
   * @param startCharno The starting character position of the type expression.
   * @param matchingLC Whether the type expression starts with a "{".
   * @return The type expression found or null if none.
   */
  private Node parseAndRecordTypeNameNode(JsDocToken token, int lineno,
                                          int startCharno, boolean matchingLC) {
    return parseAndRecordTypeNode(token, lineno, startCharno, matchingLC, true);
  }

  /**
   * Looks for a type expression at the current token and if found,
   * returns it. Note that this method consumes input.
   *
   * Parameter type expressions are special for two reasons:
   * <ol>
   *   <li>They must begin with '{', to distinguish type names from param names.
   *   <li>They may end in '=', to denote optionality.
   * </ol>
   *
   * @param token The current token.
   * @return The type expression found or null if none.
   */
  private Node parseAndRecordParamTypeNode(JsDocToken token) {
    Preconditions.checkArgument(token == JsDocToken.LC);
    int lineno = stream.getLineno();
    int startCharno = stream.getCharno();

    Node typeNode = parseParamTypeExpressionAnnotation(token);
    int endCharno = stream.getCharno();

    jsdocBuilder.markTypeNode(typeNode, lineno, startCharno, endCharno,
        true);
    return typeNode;
  }

  /**
   * Looks for a parameter type expression at the current token and if found,
   * returns it. Note that this method consumes input.
   *
   * @param token The current token.
   * @param lineno The line of the type expression.
   * @param startCharno The starting character position of the type expression.
   * @param matchingLC Whether the type expression starts with a "{".
   * @param onlyParseSimpleNames If true, only simple type names are parsed
   *     (via a call to parseTypeNameAnnotation instead of
   *     parseTypeExpressionAnnotation).
   * @return The type expression found or null if none.
   */
  private Node parseAndRecordTypeNode(JsDocToken token, int lineno,
                                      int startCharno,
                                      boolean matchingLC,
                                      boolean onlyParseSimpleNames) {
    Node typeNode = null;

    if (onlyParseSimpleNames) {
      typeNode = parseTypeNameAnnotation(token);
    } else {
      typeNode = parseTypeExpressionAnnotation(token);
    }

    if (typeNode != null && !matchingLC) {
      typeNode.putBooleanProp(Node.BRACELESS_TYPE, true);
    }

    int endCharno = stream.getCharno();

    jsdocBuilder.markTypeNode(typeNode, lineno, startCharno, endCharno,
        matchingLC);

    return typeNode;
  }

  /**
   * Determines whether the given type is a valid {@code @define} type.
   */
  // TODO(nicksantos): Move this into a check pass.
  private boolean isValidDefineType(Node typeNode) {
    JSType type = typeRegistry.createFromTypeNodes(typeNode, "", null);
    return !type.isUnknownType() && type.isSubtype(
        typeRegistry.getNativeType(JSTypeNative.NUMBER_STRING_BOOLEAN));
  }

  /**
   * Converts a JSDoc token to its string representation.
   */
  private String toString(JsDocToken token) {
    switch (token) {
      case ANNOTATION:
        return "@" + stream.getString();

      case BANG:
        return "!";

      case COMMA:
        return ",";

      case COLON:
        return ":";

      case GT:
        return ">";

      case LB:
        return "[";

      case LC:
        return "{";

      case LP:
        return "(";

      case LT:
        return ".<";

      case QMARK:
        return "?";

      case PIPE:
        return "|";

      case RB:
        return "]";

      case RC:
        return "}";

      case RP:
        return ")";

      case STAR:
        return "*";

      case ELLIPSIS:
        return "...";

      case EQUALS:
        return "=";

      case STRING:
        return stream.getString();

      default:
        throw new IllegalStateException(token.toString());
    }
  }

  /**
   * Constructs a new {@code JSTypeExpression}.
   * @param n A node. May be null.
   */
  private JSTypeExpression createJSTypeExpression(Node n) {
    return n == null ? null :
        new JSTypeExpression(n, sourceName, typeRegistry);
  }

  /**
   * Tuple for returning both the string extracted and the
   * new token following a call to any of the extract*Block
   * methods.
   */
  private static class ExtractionInfo {
    private final String string;
    private final JsDocToken token;

    public ExtractionInfo(String string, JsDocToken token) {
      this.string = string;
      this.token = token;
    }

    public String getString() {
      return string;
    }

    public JsDocToken getToken() {
      return token;
    }
  }

  /**
   * Extracts the text found on the current line starting at token. Note that
   * token = token.info; should be called after this method is used to update
   * the token properly in the parser.
   *
   * @return The extraction information.
   */
  private ExtractionInfo extractSingleLineBlock() {

    // Get the current starting point.
    stream.update();
    int lineno = stream.getLineno();
    int charno = stream.getCharno() + 1;

    String line = stream.getRemainingJSDocLine().trim();

    // Record the textual description.
    if (line.length() > 0) {
      jsdocBuilder.markText(line, lineno, charno, lineno,
                            charno + line.length());
    }

    return new ExtractionInfo(line, next());
  }

  private ExtractionInfo extractMultilineTextualBlock(JsDocToken token) {
    return extractMultilineTextualBlock(token, WhitespaceOption.SINGLE_LINE);
  }

  private enum WhitespaceOption {
    /**
     * Preserves all whitespace and formatting. Needed for licenses and
     * purposely formatted text.
     */
    PRESERVE,

    /** Preserves newlines but trims the output. */
    TRIM,

    /** Removes newlines and turns the output into a single line string. */
    SINGLE_LINE
  }

  /**
   * Extracts the text found on the current line and all subsequent
   * until either an annotation, end of comment or end of file is reached.
   * Note that if this method detects an end of line as the first token, it
   * will quit immediately (indicating that there is no text where it was
   * expected).  Note that token = info.token; should be called after this
   * method is used to update the token properly in the parser.
   *
   * @param token The start token.
   * @param option How to handle whitespace.
   *
   * @return The extraction information.
   */
  private ExtractionInfo extractMultilineTextualBlock(JsDocToken token,
                                                      WhitespaceOption option) {

    if (token == JsDocToken.EOC || token == JsDocToken.EOL ||
        token == JsDocToken.EOF) {
      return new ExtractionInfo("", token);
    }

    stream.update();
    int startLineno = stream.getLineno();
    int startCharno = stream.getCharno() + 1;

    // Read the content from the first line.
    String line = stream.getRemainingJSDocLine();

    if (option != WhitespaceOption.PRESERVE) {
      line = line.trim();
    }

    StringBuilder builder = new StringBuilder();
    builder.append(line);

    state = State.SEARCHING_ANNOTATION;
    token = next();

    boolean ignoreStar = false;

    do {
      switch (token) {
        case ANNOTATION:
        case EOC:
        case EOF:
          String multilineText = builder.toString();

          if (option != WhitespaceOption.PRESERVE) {
            multilineText = multilineText.trim();
          }

          int endLineno = stream.getLineno();
          int endCharno = stream.getCharno();

          if (multilineText.length() > 0) {
            jsdocBuilder.markText(multilineText, startLineno, startCharno,
                                  endLineno, endCharno);
          }

          return new ExtractionInfo(multilineText, token);

        case STAR:
          if (!ignoreStar) {
            if (builder.length() > 0) {
              builder.append(' ');
            }

            builder.append('*');
          }

          token = next();
          continue;

        case EOL:
          if (option != WhitespaceOption.SINGLE_LINE) {
            builder.append("\n");
          }

          ignoreStar = true;
          token = next();
          continue;

        default:
          ignoreStar = false;
          state = State.SEARCHING_ANNOTATION;

          if (builder.length() > 0) {
            builder.append(' ');
          }

          builder.append(toString(token));

          line = stream.getRemainingJSDocLine();

          if (option != WhitespaceOption.PRESERVE) {
            line = trimEnd(line);
          }

          builder.append(line);
          token = next();
      }
    } while (true);
  }


  /**
   * Extracts the top-level block comment from the JsDoc comment, if any.
   * This method differs from the extractMultilineTextualBlock in that it
   * terminates under different conditions (it doesn't have the same
   * prechecks), it does not first read in the remaining of the current
   * line and its conditions for ignoring the "*" (STAR) are different.
   *
   * @param token The starting token.
   *
   * @return The extraction information.
   */
  private ExtractionInfo extractBlockComment(JsDocToken token) {
    StringBuilder builder = new StringBuilder();

    boolean ignoreStar = true;

    do {
      switch (token) {
        case ANNOTATION:
        case EOC:
        case EOF:
          return new ExtractionInfo(builder.toString().trim(), token);

        case STAR:
          if (!ignoreStar) {
            if (builder.length() > 0) {
              builder.append(' ');
            }

            builder.append('*');
          }

          token = next();
          continue;

        case EOL:
          ignoreStar = true;
          builder.append('\n');
          token = next();
          continue;

// start of generated patch
default :
if(!ignoreStar&&builder.length()>0){
builder.append(' ');
}
ignoreStar=false;
builder.append(toString(token));
String line=stream.getRemainingJSDocLine();
line=trimEnd(line);
builder.append(line);
if(line.length()>0){
jsdocBuilder.recordDeprecationReason(line);
}
token=next();


// end of generated patch
/* start of original code
        default:
          if (!ignoreStar && builder.length() > 0) {
            builder.append(' ');
          }

          ignoreStar = false;

          builder.append(toString(token));

          String line = stream.getRemainingJSDocLine();
          line = trimEnd(line);
          builder.append(line);
          token = next();
 end of original code*/
      }
    } while (true);
  }

  /**
   * Trim characters from only the end of a string.
   * This method will remove all whitespace characters
   * (defined by Character.isWhitespace(char), in addition to the characters
   * provided, from the end of the provided string.
   *
   * @param s String to be trimmed
   * @return String with whitespace and characters in extraChars removed
   *                   from the end.
   */
  private static String trimEnd(String s) {
    int trimCount = 0;
    while (trimCount < s.length()) {
      char ch = s.charAt(s.length() - trimCount - 1);
      if (Character.isWhitespace(ch)) {
        trimCount++;
      } else {
        break;
      }
    }

    if (trimCount == 0) {
      return s;
    }
    return s.substring(0, s.length() - trimCount);
  }

  // Based on ES4 grammar proposed on July 10, 2008.
  // http://wiki.ecmascript.org/doku.php?id=spec:spec
  // Deliberately written to line up with the actual grammar rules,
  // for maximum flexibility.

  // TODO(nicksantos): The current implementation tries to maintain backwards
  // compatibility with previous versions of the spec whenever we can.
  // We should try to gradually withdraw support for these.

  /**
   * TypeExpressionAnnotation := TypeExpression |
   *     '{' TopLevelTypeExpression '}'
   */
  private Node parseTypeExpressionAnnotation(JsDocToken token) {
    if (token == JsDocToken.LC) {
      skipEOLs();
      Node typeNode = parseTopLevelTypeExpression(next());
      if (typeNode != null) {
        skipEOLs();
        if (!match(JsDocToken.RC)) {
          reportTypeSyntaxWarning("msg.jsdoc.missing.rc");
        } else {
          next();
        }
      }

      return typeNode;
    } else {
      return parseTypeExpression(token);
    }
  }

  /**
   * ParamTypeExpressionAnnotation :=
   *     '{' OptionalParameterType '}' |
   *     '{' TopLevelTypeExpression '}' |
   *     '{' '...' TopLevelTypeExpression '}'
   *
   * OptionalParameterType :=
   *     TopLevelTypeExpression '='
   */
  private Node parseParamTypeExpressionAnnotation(JsDocToken token) {
    Preconditions.checkArgument(token == JsDocToken.LC);

    skipEOLs();

    boolean restArg = false;
    token = next();
    if (token == JsDocToken.ELLIPSIS) {
      token = next();
      restArg = true;
    }

    Node typeNode = parseTopLevelTypeExpression(token);
    if (typeNode != null) {
      skipEOLs();
      if (restArg) {
        typeNode = wrapNode(Token.ELLIPSIS, typeNode);
      } else if (match(JsDocToken.EQUALS)) {
        next();
        skipEOLs();
        typeNode = wrapNode(Token.EQUALS, typeNode);
      }

      if (!match(JsDocToken.RC)) {
        reportTypeSyntaxWarning("msg.jsdoc.missing.rc");
      } else {
        next();
      }
    }

    return typeNode;
  }

  /**
   * TypeNameAnnotation := TypeName | '{' TypeName '}'
   */
  private Node parseTypeNameAnnotation(JsDocToken token) {
    if (token == JsDocToken.LC) {
      skipEOLs();
      Node typeNode = parseTypeName(next());
      if (typeNode != null) {
        skipEOLs();
        if (!match(JsDocToken.RC)) {
          reportTypeSyntaxWarning("msg.jsdoc.missing.rc");
        } else {
          next();
        }
      }

      return typeNode;
    } else {
      return parseTypeName(token);
    }
  }

  /**
   * TopLevelTypeExpression := TypeExpression
   *     | TypeUnionList
   *
   * We made this rule up, for the sake of backwards compatibility.
   */
  private Node parseTopLevelTypeExpression(JsDocToken token) {
    Node typeExpr = parseTypeExpression(token);
    if (typeExpr != null) {
      // top-level unions are allowed
      if (match(JsDocToken.PIPE)) {
        next();
        if (match(JsDocToken.PIPE)) {
          // We support double pipes for backwards-compatibility.
          next();
        }
        skipEOLs();
        token = next();
        return parseUnionTypeWithAlternate(token, typeExpr);
      }
    }
    return typeExpr;
  }

  /**
   * TypeExpressionList := TopLevelTypeExpression
   *     | TopLevelTypeExpression ',' TypeExpressionList
   */
  private Node parseTypeExpressionList(JsDocToken token) {
    Node typeExpr = parseTopLevelTypeExpression(token);
    if (typeExpr == null) {
      return null;
    }
    Node typeList = new Node(Token.BLOCK);
    typeList.addChildToBack(typeExpr);
    while (match(JsDocToken.COMMA)) {
      next();
      skipEOLs();
      typeExpr = parseTopLevelTypeExpression(next());
      if (typeExpr == null) {
        return null;
      }
      typeList.addChildToBack(typeExpr);
    }
    return typeList;
  }

  /**
   * TypeExpression := BasicTypeExpression
   *     | '?' BasicTypeExpression
   *     | '!' BasicTypeExpression
   *     | BasicTypeExpression '?'
   *     | BasicTypeExpression '!'
   */
  private Node parseTypeExpression(JsDocToken token) {
    if (token == JsDocToken.QMARK) {
      return wrapNode(Token.QMARK, parseBasicTypeExpression(next()));
    } else if (token == JsDocToken.BANG) {
      return wrapNode(Token.BANG, parseBasicTypeExpression(next()));
    } else {
      Node basicTypeExpr = parseBasicTypeExpression(token);
      if (basicTypeExpr != null) {
        if (match(JsDocToken.QMARK)) {
          next();
          return wrapNode(Token.QMARK, basicTypeExpr);
        } else if (match(JsDocToken.BANG)) {
          next();
          return wrapNode(Token.BANG, basicTypeExpr);
        }
      }

      return basicTypeExpr;
    }
  }

  /**
   * BasicTypeExpression := '*' | 'null' | 'undefined' | TypeName
   *     | FunctionType | UnionType | RecordType | ArrayType
   */
  private Node parseBasicTypeExpression(JsDocToken token) {
    if (token == JsDocToken.STAR) {
      return newNode(Token.STAR);
    } else if (token == JsDocToken.LB) {
      skipEOLs();
      return parseArrayType(next());
    } else if (token == JsDocToken.LC) {
      skipEOLs();
      return parseRecordType(next());
    } else if (token == JsDocToken.LP) {
      skipEOLs();
      return parseUnionType(next());
    } else if (token == JsDocToken.STRING) {
      String string = stream.getString();
      if ("function".equals(string)) {
        skipEOLs();
        return parseFunctionType(next());
      } else if ("null".equals(string) || "undefined".equals(string)) {
        return newStringNode(string);
      } else {
        return parseTypeName(token);
      }
    }

    return reportGenericTypeSyntaxWarning();
  }

  /**
   * TypeName := NameExpression | NameExpression TypeApplication
   * TypeApplication := '.<' TypeExpressionList '>'
   * TypeExpressionList := TypeExpression // a white lie
   */
  private Node parseTypeName(JsDocToken token) {
    if (token != JsDocToken.STRING) {
      return reportGenericTypeSyntaxWarning();
    }

    Node typeName = newStringNode(stream.getString());
    if (match(JsDocToken.LT)) {
      next();
      skipEOLs();
      Node memberType = parseTypeExpressionList(next());
      if (memberType != null) {
        typeName.addChildToFront(memberType);

        skipEOLs();
        if (!match(JsDocToken.GT)) {
          return reportTypeSyntaxWarning("msg.jsdoc.missing.gt");
        }

        next();
      }
    }
    return typeName;
  }

  /**
   * FunctionType := 'function' FunctionSignatureType
   * FunctionSignatureType :=
   *    TypeParameters '(' 'this' ':' TypeName, ParametersType ')' ResultType
   */
  private Node parseFunctionType(JsDocToken token) {
    // NOTE(nicksantos): We're not implementing generics at the moment, so
    // just throw out TypeParameters.
    if (token != JsDocToken.LP) {
      return reportTypeSyntaxWarning("msg.jsdoc.missing.lp");
    }

    Node functionType = newNode(Token.FUNCTION);
    Node parameters = null;
    skipEOLs();
    if (!match(JsDocToken.RP)) {
      token = next();

      boolean hasParams = true;
      if (token == JsDocToken.STRING && "this".equals(stream.getString())) {
        if (match(JsDocToken.COLON)) {
          next();
          skipEOLs();
          Node thisType = wrapNode(Token.THIS, parseTypeName(next()));
          if (thisType == null) {
            return null;
          }

          functionType.addChildToFront(thisType);
        } else {
          return reportTypeSyntaxWarning("msg.jsdoc.missing.colon");
        }

        if (match(JsDocToken.COMMA)) {
          next();
          skipEOLs();
          token = next();
        } else {
          hasParams = false;
        }
      }

      if (hasParams) {
        parameters = parseParametersType(token);
        if (parameters == null) {
          return null;
        }
      }
    }

    if (parameters != null) {
      functionType.addChildToBack(parameters);
    }

    skipEOLs();
    if (!match(JsDocToken.RP)) {
      return reportTypeSyntaxWarning("msg.jsdoc.missing.rp");
    }

    skipEOLs();
    Node resultType = parseResultType(next());
    if (resultType == null) {
      return null;
    } else {
      functionType.addChildToBack(resultType);
    }
    return functionType;
  }

  /**
   * ParametersType := RestParameterType | NonRestParametersType
   *     | NonRestParametersType ',' RestParameterType
   * RestParameterType := '...' Identifier
   * NonRestParametersType := ParameterType ',' NonRestParametersType
   *     | ParameterType
   *     | OptionalParametersType
   * OptionalParametersType := OptionalParameterType
   *     | OptionalParameterType, OptionalParametersType
   * OptionalParameterType := ParameterType=
   * ParameterType := TypeExpression | Identifier ':' TypeExpression
   */
  // NOTE(nicksantos): The official ES4 grammar forces optional and rest
  // arguments to come after the required arguments. Our parser does not
  // enforce this. Instead we allow them anywhere in the function at parse-time,
  // and then warn about them during type resolution.
  //
  // In theory, it might be mathematically nicer to do the order-checking here.
  // But in practice, the order-checking for structural functions is exactly
  // the same as the order-checking for @param annotations. And the latter
  // has to happen during type resolution. Rather than duplicate the
  // order-checking in two places, we just do all of it in type resolution.
  private Node parseParametersType(JsDocToken token) {
    Node paramsType = newNode(Token.LP);
    boolean isVarArgs = false;
    Node paramType = null;
    if (token != JsDocToken.RP) {
      do {
        if (paramType != null) {
          // skip past the comma
          next();
          skipEOLs();
          token = next();
        }

        if (token == JsDocToken.ELLIPSIS) {
          // In the latest ES4 proposal, there are no type constraints allowed
          // on variable arguments. We support the old syntax for backwards
          // compatibility, but we should gradually tear it out.
          skipEOLs();
          if (match(JsDocToken.RP)) {
            paramType = newNode(Token.ELLIPSIS);
          } else {
            skipEOLs();
            if (!match(JsDocToken.LB)) {
              return reportTypeSyntaxWarning("msg.jsdoc.missing.lb");
            }

            next();
            skipEOLs();
            paramType = wrapNode(Token.ELLIPSIS, parseTypeExpression(next()));
            skipEOLs();
            if (!match(JsDocToken.RB)) {
              return reportTypeSyntaxWarning("msg.jsdoc.missing.rb");
            }
            skipEOLs();
            next();
          }

          isVarArgs = true;
        } else {
          paramType = parseTypeExpression(token);
          if (match(JsDocToken.EQUALS)) {
            skipEOLs();
            next();
            paramType = wrapNode(Token.EQUALS, paramType);
          }
        }

        if (paramType == null) {
          return null;
        }
        paramsType.addChildToBack(paramType);
        if (isVarArgs) {
          break;
        }
      } while (match(JsDocToken.COMMA));
    }

    if (isVarArgs && match(JsDocToken.COMMA)) {
      return reportTypeSyntaxWarning("msg.jsdoc.function.varargs");
    }

    // The right paren will be checked by parseFunctionType

    return paramsType;
  }

  /**
   * ResultType := <empty> | ':' void | ':' TypeExpression
   */
  private Node parseResultType(JsDocToken token) {
    skipEOLs();
    if (!match(JsDocToken.COLON)) {
      return newNode(Token.EMPTY);
    }

    token = next();
    skipEOLs();
    if (match(JsDocToken.STRING) && "void".equals(stream.getString())) {
      next();
      return newNode(Token.VOID);
    } else {
      return parseTypeExpression(next());
    }
  }

  /**
   * UnionType := '(' TypeUnionList ')'
   * TypeUnionList := TypeExpression | TypeExpression '|' TypeUnionList
   *
   * We've removed the empty union type.
   */
  private Node parseUnionType(JsDocToken token) {
    return parseUnionTypeWithAlternate(token, null);
  }

  /**
   * Create a new union type, with an alternate that has already been
   * parsed. The alternate may be null.
   */
  private Node parseUnionTypeWithAlternate(JsDocToken token, Node alternate) {
    Node union = newNode(Token.PIPE);
    if (alternate != null) {
      union.addChildToBack(alternate);
    }

    Node expr = null;
    do {
      if (expr != null) {
        skipEOLs();
        token = next();
        Preconditions.checkState(
            token == JsDocToken.PIPE || token == JsDocToken.COMMA);

        boolean isPipe = token == JsDocToken.PIPE;
        if (isPipe && match(JsDocToken.PIPE)) {
          // We support double pipes for backwards compatiblity.
          next();
        }
        skipEOLs();
        token = next();
      }
      expr = parseTypeExpression(token);
      if (expr == null) {
        return null;
      }

      union.addChildToBack(expr);
      // We support commas for backwards compatiblity.
    } while (match(JsDocToken.PIPE, JsDocToken.COMMA));

    if (alternate == null) {
      skipEOLs();
      if (!match(JsDocToken.RP)) {
        return reportTypeSyntaxWarning("msg.jsdoc.missing.rp");
      }
      next();
    }
    return union;
  }

  /**
   * ArrayType := '[' ElementTypeList ']'
   * ElementTypeList := <empty> | TypeExpression | '...' TypeExpression
   *     | TypeExpression ',' ElementTypeList
   */
  private Node parseArrayType(JsDocToken token) {
    Node array = newNode(Token.LB);
    Node arg = null;
    boolean hasVarArgs = false;

    do {
      if (arg != null) {
        next();
        skipEOLs();
        token = next();
      }
      if (token == JsDocToken.ELLIPSIS) {
        arg = wrapNode(Token.ELLIPSIS, parseTypeExpression(next()));
        hasVarArgs = true;
      } else {
        arg = parseTypeExpression(token);
      }

      if (arg == null) {
        return null;
      }

      array.addChildToBack(arg);
      if (hasVarArgs) {
        break;
      }
      skipEOLs();
    } while (match(JsDocToken.COMMA));

    if (!match(JsDocToken.RB)) {
      return reportTypeSyntaxWarning("msg.jsdoc.missing.rb");
    }
    next();
    return array;
  }

  /**
   * RecordType := '{' FieldTypeList '}'
   */
  private Node parseRecordType(JsDocToken token) {
    Node recordType = newNode(Token.LC);
    Node fieldTypeList = parseFieldTypeList(token);

    if (fieldTypeList == null) {
      return reportGenericTypeSyntaxWarning();
    }

    skipEOLs();
    if (!match(JsDocToken.RC)) {
      return reportTypeSyntaxWarning("msg.jsdoc.missing.rc");
    }

    next();

    recordType.addChildToBack(fieldTypeList);
    return recordType;
  }

  /**
   * FieldTypeList := FieldType | FieldType ',' FieldTypeList
   */
  private Node parseFieldTypeList(JsDocToken token) {
    Node fieldTypeList = newNode(Token.LB);

    do {
      Node fieldType = parseFieldType(token);

      if (fieldType == null) {
        return null;
      }

      fieldTypeList.addChildToBack(fieldType);

      skipEOLs();
      if (!match(JsDocToken.COMMA)) {
        break;
      }

      // Move to the comma token.
      next();

      // Move to the token passed the comma.
      skipEOLs();
      token = next();
    } while (true);

    return fieldTypeList;
  }

  /**
   * FieldType := FieldName | FieldName ':' TypeExpression
   */
  private Node parseFieldType(JsDocToken token) {
    Node fieldName = parseFieldName(token);

    if (fieldName == null) {
      return null;
    }

    skipEOLs();
    if (!match(JsDocToken.COLON)) {
      return fieldName;
    }

    // Move to the colon.
    next();

    // Move to the token after the colon and parse
    // the type expression.
    skipEOLs();
    Node typeExpression = parseTypeExpression(next());

    if (typeExpression == null) {
      return null;
    }

    Node fieldType = newNode(Token.COLON);
    fieldType.addChildToBack(fieldName);
    fieldType.addChildToBack(typeExpression);
    return fieldType;
  }

  /**
   * FieldName := NameExpression | StringLiteral | NumberLiteral |
   * ReservedIdentifier
   */
  private Node parseFieldName(JsDocToken token) {
    switch (token) {
      case STRING:
        String string = stream.getString();
        return newStringNode(string);

      default:
        return null;
    }
  }

  private Node wrapNode(int type, Node n) {
    return n == null ? null :
        new Node(type, n, stream.getLineno(), stream.getCharno());
  }

  private Node newNode(int type) {
    return new Node(type, stream.getLineno(), stream.getCharno());
  }

  private Node newStringNode(String s) {
    return Node.newString(s, stream.getLineno(), stream.getCharno());
  }

  private Node reportTypeSyntaxWarning(String warning) {
    parser.addWarning(warning, stream.getLineno(), stream.getCharno());
    return null;
  }

  private Node reportGenericTypeSyntaxWarning() {
    return reportTypeSyntaxWarning("msg.jsdoc.type.syntax");
  }

  /**
   * Eats tokens until {@link JsDocToken#EOL} included, and switches back the
   * state to {@link State#SEARCHING_ANNOTATION}.
   */
  private JsDocToken eatTokensUntilEOL() {
    return eatTokensUntilEOL(next());
  }

  /**
   * Eats tokens until {@link JsDocToken#EOL} included, and switches back the
   * state to {@link State#SEARCHING_ANNOTATION}.
   */
  private JsDocToken eatTokensUntilEOL(JsDocToken token) {
    do {
      if (token == JsDocToken.EOL || token == JsDocToken.EOC ||
          token == JsDocToken.EOF) {
        state = State.SEARCHING_ANNOTATION;
        return token;
      }
      token = next();
    } while (true);
  }

  /**
   * Specific value indicating that the {@link #unreadToken} contains no token.
   */
  private static final JsDocToken NO_UNREAD_TOKEN = null;

  /**
   * One token buffer.
   */
  private JsDocToken unreadToken = NO_UNREAD_TOKEN;

  /**
   * Tests whether the next symbol of the token stream matches the specific
   * token.
   */
  private boolean match(JsDocToken token) {
    unreadToken = next();
    return unreadToken == token;
  }

  /**
   * Tests that the next symbol of the token stream matches one of the specified
   * tokens.
   */
  private boolean match(JsDocToken token1, JsDocToken token2) {
    unreadToken = next();
    return unreadToken == token1 || unreadToken == token2;
  }

  /**
   * Gets the next token of the token stream or the buffered token if a matching
   * was previously made.
   */
  private JsDocToken next() {
    if (unreadToken == NO_UNREAD_TOKEN) {
      return stream.getJsDocToken();
    } else {
      return current();
    }
  }

  /**
   * Gets the current token, invalidating it in the process.
   */
  private JsDocToken current() {
    JsDocToken t = unreadToken;
    unreadToken = NO_UNREAD_TOKEN;
    return t;
  }

  /**
   * Skips all EOLs and all empty lines in the JSDoc. Call this method if you
   * want the JSDoc entry to span multiple lines.
   */
  private void skipEOLs() {
    while (match(JsDocToken.EOL)) {
      next();
      if (match(JsDocToken.STAR)) {
        next();
      }
    }
  }

  /**
   * Determines whether the parser has been populated with docinfo with a
   * fileoverview tag.
   */
  private boolean hasParsedFileOverviewDocInfo() {
    return jsdocBuilder.isPopulatedWithFileOverview();
  }

  boolean hasParsedJSDocInfo() {
    return jsdocBuilder.isPopulated();
  }

  JSDocInfo retrieveAndResetParsedJSDocInfo() {
    return jsdocBuilder.build(sourceName);
  }

  /**
   * Gets the fileoverview JSDocInfo, if any.
   */
  JSDocInfo getFileOverviewJSDocInfo() {
    return fileOverviewJSDocInfo;
  }
}
