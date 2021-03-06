package io.protostuff.jetbrains.plugin;

import static io.protostuff.compiler.parser.ProtoLexer.COMMENT;
import static io.protostuff.compiler.parser.ProtoLexer.LINE_COMMENT;
import static io.protostuff.compiler.parser.ProtoLexer.NL;
import static io.protostuff.compiler.parser.ProtoLexer.STRING_VALUE;
import static io.protostuff.compiler.parser.ProtoLexer.WS;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import io.protostuff.compiler.parser.ProtoLexer;
import io.protostuff.compiler.parser.ProtoParser;
import io.protostuff.jetbrains.plugin.psi.CustomFieldReferenceNode;
import io.protostuff.jetbrains.plugin.psi.EnumConstantNode;
import io.protostuff.jetbrains.plugin.psi.EnumNode;
import io.protostuff.jetbrains.plugin.psi.ExtendEntryNode;
import io.protostuff.jetbrains.plugin.psi.ExtendNode;
import io.protostuff.jetbrains.plugin.psi.ExtensionsNode;
import io.protostuff.jetbrains.plugin.psi.FieldNode;
import io.protostuff.jetbrains.plugin.psi.FieldReferenceNode;
import io.protostuff.jetbrains.plugin.psi.FileReferenceNode;
import io.protostuff.jetbrains.plugin.psi.GroupNode;
import io.protostuff.jetbrains.plugin.psi.ImportNode;
import io.protostuff.jetbrains.plugin.psi.MapKeyNode;
import io.protostuff.jetbrains.plugin.psi.MapNode;
import io.protostuff.jetbrains.plugin.psi.MessageNameNode;
import io.protostuff.jetbrains.plugin.psi.MessageNode;
import io.protostuff.jetbrains.plugin.psi.OneOfNode;
import io.protostuff.jetbrains.plugin.psi.OneofFieldNode;
import io.protostuff.jetbrains.plugin.psi.OptionEntryNode;
import io.protostuff.jetbrains.plugin.psi.OptionNode;
import io.protostuff.jetbrains.plugin.psi.OptionValueNode;
import io.protostuff.jetbrains.plugin.psi.PackageStatement;
import io.protostuff.jetbrains.plugin.psi.ProtoPsiFileRoot;
import io.protostuff.jetbrains.plugin.psi.ProtoRootNode;
import io.protostuff.jetbrains.plugin.psi.RangeNode;
import io.protostuff.jetbrains.plugin.psi.ReservedFieldNamesNode;
import io.protostuff.jetbrains.plugin.psi.ReservedFieldRangesNode;
import io.protostuff.jetbrains.plugin.psi.RpcMethodNode;
import io.protostuff.jetbrains.plugin.psi.RpcMethodTypeNode;
import io.protostuff.jetbrains.plugin.psi.ServiceNode;
import io.protostuff.jetbrains.plugin.psi.StandardFieldReferenceNode;
import io.protostuff.jetbrains.plugin.psi.SyntaxNode;
import io.protostuff.jetbrains.plugin.psi.TypeReferenceNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.antlr.jetbrains.adapter.lexer.AntlrLexerAdapter;
import org.antlr.jetbrains.adapter.lexer.PsiElementTypeFactory;
import org.antlr.jetbrains.adapter.lexer.RuleIElementType;
import org.antlr.jetbrains.adapter.lexer.TokenIElementType;
import org.antlr.jetbrains.adapter.parser.AntlrParserAdapter;
import org.antlr.jetbrains.adapter.psi.AntlrPsiNode;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;

/**
 * Parser definition for Protobuf.
 *
 * @author Kostiantyn Shchepanovskyi
 */
public class ProtoParserDefinition implements ParserDefinition {

    public static final PsiElementTypeFactory ELEMENT_FACTORY = PsiElementTypeFactory.create(ProtoLanguage.INSTANCE, new ProtoParser(null));
    public static final TokenSet KEYWORDS = ELEMENT_FACTORY.createTokenSet(
            ProtoLexer.PACKAGE,
            ProtoLexer.SYNTAX,
            ProtoLexer.IMPORT,
            ProtoLexer.PUBLIC,
            ProtoLexer.OPTION,
            ProtoLexer.MESSAGE,
            ProtoLexer.GROUP,
            ProtoLexer.OPTIONAL,
            ProtoLexer.REQUIRED,
            ProtoLexer.REPEATED,
            ProtoLexer.ONEOF,
            ProtoLexer.EXTEND,
            ProtoLexer.EXTENSIONS,
            ProtoLexer.RESERVED,
            ProtoLexer.TO,
            ProtoLexer.MAX,
            ProtoLexer.ENUM,
            ProtoLexer.SERVICE,
            ProtoLexer.RPC,
            ProtoLexer.STREAM,
            ProtoLexer.RETURNS,
            ProtoLexer.MAP,
            ProtoLexer.BOOLEAN_VALUE,
            ProtoLexer.DOUBLE,
            ProtoLexer.FLOAT,
            ProtoLexer.INT32,
            ProtoLexer.INT64,
            ProtoLexer.UINT32,
            ProtoLexer.UINT64,
            ProtoLexer.SINT32,
            ProtoLexer.SINT64,
            ProtoLexer.FIXED32,
            ProtoLexer.FIXED64,
            ProtoLexer.SFIXED32,
            ProtoLexer.SFIXED64,
            ProtoLexer.BOOL,
            ProtoLexer.STRING,
            ProtoLexer.BYTES
    );
    // keywords also can be identifiers
    public static final TokenSet IDENTIFIER_TOKEN_SET = ELEMENT_FACTORY.createTokenSet(
            ProtoLexer.IDENT,
            ProtoLexer.PACKAGE,
            ProtoLexer.SYNTAX,
            ProtoLexer.IMPORT,
            ProtoLexer.PUBLIC,
            ProtoLexer.OPTION,
            ProtoLexer.MESSAGE,
            ProtoLexer.GROUP,
            ProtoLexer.OPTIONAL,
            ProtoLexer.REQUIRED,
            ProtoLexer.REPEATED,
            ProtoLexer.ONEOF,
            ProtoLexer.EXTEND,
            ProtoLexer.EXTENSIONS,
            ProtoLexer.RESERVED,
            ProtoLexer.TO,
            ProtoLexer.MAX,
            ProtoLexer.ENUM,
            ProtoLexer.SERVICE,
            ProtoLexer.RPC,
            ProtoLexer.STREAM,
            ProtoLexer.RETURNS,
            ProtoLexer.MAP,
            ProtoLexer.BOOLEAN_VALUE,
            ProtoLexer.DOUBLE,
            ProtoLexer.FLOAT,
            ProtoLexer.INT32,
            ProtoLexer.INT64,
            ProtoLexer.UINT32,
            ProtoLexer.UINT64,
            ProtoLexer.SINT32,
            ProtoLexer.SINT64,
            ProtoLexer.FIXED32,
            ProtoLexer.FIXED64,
            ProtoLexer.SFIXED32,
            ProtoLexer.SFIXED64,
            ProtoLexer.BOOL,
            ProtoLexer.STRING,
            ProtoLexer.BYTES
    );
    public static final TokenSet COMMENT_TOKEN_SET = ELEMENT_FACTORY.createTokenSet(
            ProtoLexer.COMMENT,
            ProtoLexer.LINE_COMMENT
    );
    public static final TokenSet LITERAL_TOKEN_SET = ELEMENT_FACTORY.createTokenSet(
            ProtoLexer.STRING_VALUE,
            ProtoLexer.FLOAT_VALUE,
            ProtoLexer.INTEGER_VALUE,
            ProtoLexer.IDENT,
            ProtoLexer.PACKAGE,
            ProtoLexer.SYNTAX,
            ProtoLexer.IMPORT,
            ProtoLexer.PUBLIC,
            ProtoLexer.OPTION,
            ProtoLexer.MESSAGE,
            ProtoLexer.GROUP,
            ProtoLexer.OPTIONAL,
            ProtoLexer.REQUIRED,
            ProtoLexer.REPEATED,
            ProtoLexer.ONEOF,
            ProtoLexer.EXTEND,
            ProtoLexer.EXTENSIONS,
            ProtoLexer.RESERVED,
            ProtoLexer.TO,
            ProtoLexer.MAX,
            ProtoLexer.ENUM,
            ProtoLexer.SERVICE,
            ProtoLexer.RPC,
            ProtoLexer.STREAM,
            ProtoLexer.RETURNS,
            ProtoLexer.MAP,
            ProtoLexer.BOOLEAN_VALUE,
            ProtoLexer.DOUBLE,
            ProtoLexer.FLOAT,
            ProtoLexer.INT32,
            ProtoLexer.INT64,
            ProtoLexer.UINT32,
            ProtoLexer.UINT64,
            ProtoLexer.SINT32,
            ProtoLexer.SINT64,
            ProtoLexer.FIXED32,
            ProtoLexer.FIXED64,
            ProtoLexer.SFIXED32,
            ProtoLexer.SFIXED64,
            ProtoLexer.BOOL,
            ProtoLexer.STRING,
            ProtoLexer.BYTES
    );
    public static final TokenSet WHITESPACE = ELEMENT_FACTORY.createTokenSet(WS, NL);
    private static final List<TokenIElementType> TOKEN_TYPES = ELEMENT_FACTORY.getTokenIElementTypes();
    public static final TokenIElementType ID = TOKEN_TYPES.get(ProtoLexer.IDENT);

    // tokens
    public static final TokenIElementType LCURLY = TOKEN_TYPES.get(ProtoLexer.LCURLY);
    public static final TokenIElementType RCURLY = TOKEN_TYPES.get(ProtoLexer.RCURLY);
    public static final TokenIElementType LPAREN = TOKEN_TYPES.get(ProtoLexer.LPAREN);
    public static final TokenIElementType RPAREN = TOKEN_TYPES.get(ProtoLexer.RPAREN);
    public static final TokenIElementType LSQUARE = TOKEN_TYPES.get(ProtoLexer.LSQUARE);
    public static final TokenIElementType RSQUARE = TOKEN_TYPES.get(ProtoLexer.RSQUARE);
    public static final TokenIElementType LT = TOKEN_TYPES.get(ProtoLexer.LT);
    public static final TokenIElementType GT = TOKEN_TYPES.get(ProtoLexer.GT);
    public static final TokenIElementType ASSIGN = TOKEN_TYPES.get(ProtoLexer.ASSIGN);
    private static final List<RuleIElementType> RULE_TYPES = ELEMENT_FACTORY.getRuleIElementTypes();
    // Rules
    public static final IElementType R_TYPE_REFERENCE = RULE_TYPES.get(ProtoParser.RULE_typeReference);
    public static final IElementType R_NAME = RULE_TYPES.get(ProtoParser.RULE_ident);
    public static final IElementType R_FIELD_MODIFIER = RULE_TYPES.get(ProtoParser.RULE_fieldModifier);
    public static final IElementType R_FIELD_NAME = RULE_TYPES.get(ProtoParser.RULE_fieldName);
    public static final IElementType R_TAG = RULE_TYPES.get(ProtoParser.RULE_tag);
    private static final IFileElementType FILE = new IFileElementType(ProtoLanguage.INSTANCE);
    private static final TokenSet COMMENTS = ELEMENT_FACTORY.createTokenSet(COMMENT, LINE_COMMENT);
    private static final TokenSet STRING = ELEMENT_FACTORY.createTokenSet(STRING_VALUE);
    private final Map<Integer, Function<ASTNode, AntlrPsiNode>> elementFactories = new HashMap<>();

    /**
     * Create new parser definition.
     */
    public ProtoParserDefinition() {
        register(ProtoParser.RULE_syntax, SyntaxNode::new);
        register(ProtoParser.RULE_packageStatement, PackageStatement::new);
        register(ProtoParser.RULE_importStatement, ImportNode::new);
        register(ProtoParser.RULE_fileReference, FileReferenceNode::new);
        register(ProtoParser.RULE_messageBlock, MessageNode::new);
        register(ProtoParser.RULE_messageName, MessageNameNode::new);
        register(ProtoParser.RULE_field, FieldNode::new);
        register(ProtoParser.RULE_typeReference, TypeReferenceNode::new);
        register(ProtoParser.RULE_groupBlock, GroupNode::new);
        register(ProtoParser.RULE_enumBlock, EnumNode::new);
        register(ProtoParser.RULE_enumField, EnumConstantNode::new);
        register(ProtoParser.RULE_serviceBlock, ServiceNode::new);
        register(ProtoParser.RULE_rpcMethod, RpcMethodNode::new);
        register(ProtoParser.RULE_optionEntry, OptionEntryNode::new);
        register(ProtoParser.RULE_option, OptionNode::new);
        register(ProtoParser.RULE_fieldRerefence, FieldReferenceNode::new);
        register(ProtoParser.RULE_standardFieldRerefence, StandardFieldReferenceNode::new);
        register(ProtoParser.RULE_customFieldReference, CustomFieldReferenceNode::new);
        register(ProtoParser.RULE_optionValue, OptionValueNode::new);
        register(ProtoParser.RULE_oneof, OneOfNode::new);
        register(ProtoParser.RULE_oneofField, OneofFieldNode::new);
        register(ProtoParser.RULE_extendBlock, ExtendNode::new);
        register(ProtoParser.RULE_extendBlockEntry, ExtendEntryNode::new);
        register(ProtoParser.RULE_extensions, ExtensionsNode::new);
        register(ProtoParser.RULE_map, MapNode::new);
        register(ProtoParser.RULE_mapKey, MapKeyNode::new);
        register(ProtoParser.RULE_range, RangeNode::new);
        register(ProtoParser.RULE_reservedFieldRanges, ReservedFieldRangesNode::new);
        register(ProtoParser.RULE_reservedFieldNames, ReservedFieldNamesNode::new);
        register(ProtoParser.RULE_rpcType, RpcMethodTypeNode::new);
        register(ProtoParser.RULE_proto, ProtoRootNode::new);
    }

    public static TokenIElementType token(int token) {
        return TOKEN_TYPES.get(token);
    }

    public static RuleIElementType rule(int rule) {
        return RULE_TYPES.get(rule);
    }

    private void register(int rule, Function<ASTNode, AntlrPsiNode> factory) {
        if (elementFactories.containsKey(rule)) {
            throw new IllegalStateException("Duplicate rule");
        }
        elementFactories.put(rule, factory);
    }

    @NotNull
    @Override
    public Lexer createLexer(Project project) {
        ProtoLexer lexer = new ProtoLexer(null);
        return new AntlrLexerAdapter(ProtoLanguage.INSTANCE, lexer, ELEMENT_FACTORY);
    }

    @Override
    public PsiParser createParser(Project project) {
        final ProtoParser parser = new ProtoParser(null);
        return new AntlrParserAdapter(ProtoLanguage.INSTANCE, parser, ELEMENT_FACTORY) {
            @Override
            protected ParseTree parse(Parser parser, IElementType root) {
                // start rule depends on root passed in; sometimes we want to create an ID node etc...
                if (root instanceof IFileElementType) {
                    return ((ProtoParser) parser).proto();
                }
                if (root instanceof RuleIElementType) {
                    RuleIElementType type = (RuleIElementType) root;
                    if (ProtoParserDefinition.rule(ProtoParser.RULE_fileReference).equals(type)) {
                        return ((ProtoParser) parser).fileReference();
                    }
                }
                // let's hope it's an ID as needed by "rename function"
                throw new UnsupportedOperationException();
                // return ((ProtoParser) parser).name();
            }
        };
    }

    @Override
    public IFileElementType getFileNodeType() {
        return FILE;
    }

    @NotNull
    @Override
    public TokenSet getWhitespaceTokens() {
        return WHITESPACE;
    }

    @NotNull
    @Override
    public TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @NotNull
    @Override
    public TokenSet getStringLiteralElements() {
        return STRING;
    }

    @NotNull
    @Override
    public AntlrPsiNode createElement(ASTNode node) {
        IElementType elType = node.getElementType();
        if (elType instanceof TokenIElementType) {
            return new AntlrPsiNode(node);
        }
        if (!(elType instanceof RuleIElementType)) {
            return new AntlrPsiNode(node);
        }
        RuleIElementType ruleElType = (RuleIElementType) elType;
        int ruleIndex = ruleElType.getRuleIndex();
        if (elementFactories.containsKey(ruleIndex)) {
            Function<ASTNode, AntlrPsiNode> factory = elementFactories.get(ruleIndex);
            return factory.apply(node);
        }
        return new AntlrPsiNode(node);
    }

    @Override
    public PsiFile createFile(FileViewProvider viewProvider) {
        return new ProtoPsiFileRoot(viewProvider);
    }

    @Override
    public SpaceRequirements spaceExistanceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }
}
