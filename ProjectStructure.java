import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import javax.swing.*;

/**
 * Project Imperative: core language architecture outline.
 *
 * This file defines the basic structure for the compiler/interpreter.
 * It includes the token model, lexer, parser, interpreter, environment,
 * symbol table, and error handler for typed variables.
 */
public class ProjectStructure {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java ProjectStructure <source-file>");
            System.out.println("Example: java ProjectStructure sample.pi");
            return;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(args[0]));
            String source = new String(fileBytes);
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            List<Stmt> program = parser.parseFile();
            Interpreter interpreter = new Interpreter();
            interpreter.interpret(program);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (ParseException e) {
            System.err.println(e.getMessage());
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
    }
}

/**
 * Token kinds for Project Imperative source text.
 */
enum TokenType {
    // structural markers
    FILE, NAME,
    LEFT_BRACE, RIGHT_BRACE,
    LEFT_SQUARE, RIGHT_SQUARE,
    LEFT_PAREN, RIGHT_PAREN,
    COMMA, COLON, SEMICOLON,

    // operators
    ASSIGN, EQUAL, NOT_EQUAL,
    PLUS, MINUS, STAR, SLASH,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // language keywords
    ATTACH_VARIABLE, IF_, THEN_, ELSE_, REPEAT, TIMES, PRINT,
    LOOP_WINDOW_WHILE, CREATED, IMPORT_LIB, CREATE, CREATE_WIND,
    ASK, FUNCTIONTEXT, DICTIONARYTEXT, NUMBERTEXT, STRINGTEXT, WINDOWTEXT, DEFAULTSIZE,
    CREATE_CALC, CREATE_ADVANCEDCALC,

    // literals and names
    IDENTIFIER, STRING_LITERAL, NUMBER_LITERAL,

    // special
    EOF
}

/**
 * The lexical token produced by the lexer.
 */
class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        return String.format("%s '%s' %s", type, lexeme, literal);
    }
}

/**
 * Lexer for Project Imperative source code.
 * This is the entry point for tokenizing explicit bracket syntax and keywords.
 */
class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    Lexer(String source) {
        this.source = source;
    }

    List<Token> tokenize() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case '[': typeAnnotation(); break;
            case ']': addToken(TokenType.RIGHT_SQUARE); break;
            case ',': addToken(TokenType.COMMA); break;
            case ':': addToken(TokenType.COLON); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '+': addToken(TokenType.PLUS); break;
            case '-': addToken(TokenType.MINUS); break;
            case '*': addToken(TokenType.STAR); break;
            case '/': addToken(TokenType.SLASH); break;
            case '>':
                blockComment();
                break;
            case '=':
                addToken(match('=') ? TokenType.EQUAL : TokenType.ASSIGN);
                break;
            case '!':
                addToken(match('=') ? TokenType.NOT_EQUAL : null);
                break;
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;
            case '"':
            case '\'':
                stringLiteral(c);
                break;
            default:
                if (isDigit(c)) {
                    numberLiteral();
                } else if (isAlpha(c)) {
                    identifierOrKeyword();
                } else {
                    // ignore or raise lexical error
                }
                break;
        }
    }

    private void identifierOrKeyword() {
        while (isAlphaNumeric(peek())) advance();
        String value = source.substring(start, current);
        String lowerValue = value.toLowerCase();
        TokenType type = TokenType.IDENTIFIER;

        switch (lowerValue) {
            case "attach_variable":
                type = TokenType.ATTACH_VARIABLE;
                break;
            case "if_":
                type = TokenType.IF_;
                break;
            case "then":
                type = TokenType.THEN_;
                break;
            case "else":
                type = TokenType.ELSE_;
                break;
            case "repeat":
                type = TokenType.REPEAT;
                break;
            case "times":
                type = TokenType.TIMES;
                break;
            case "loop_window_while":
                type = TokenType.LOOP_WINDOW_WHILE;
                break;
            case "created":
                type = TokenType.CREATED;
                break;
            case "import_lib":
                type = TokenType.IMPORT_LIB;
                break;
            case "create":
                type = TokenType.CREATE;
                break;
            case "create_wind":
                type = TokenType.CREATE_WIND;
                break;
            case "create_calc":
                type = TokenType.CREATE_CALC;
                break;
            case "create_advancedcalc":
                type = TokenType.CREATE_ADVANCEDCALC;
                break;
            case "ask":
                type = TokenType.ASK;
                break;
            default:
                type = TokenType.IDENTIFIER;
        }

        addToken(type, value);
    }

    private void numberLiteral() {
        while (isDigit(peek())) advance();
        addToken(TokenType.NUMBER_LITERAL, Double.parseDouble(source.substring(start, current)));
    }

    private void stringLiteral(char quote) {
        while (peek() != quote && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            return; // unterminated string: real lexer should throw error
        }
        advance();
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING_LITERAL, value);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void typeAnnotation() {
        int scanStart = current;
        while (peek() != ']' && !isAtEnd()) advance();
        if (isAtEnd()) {
            addToken(TokenType.LEFT_SQUARE);
            return;
        }
        String text = source.substring(start, current + 1);
        String lowerText = text.toLowerCase();
        TokenType type = TokenType.IDENTIFIER;
        switch (lowerText) {
            case "[stringtext]":
                type = TokenType.STRINGTEXT;
                break;
            case "[numbertext]":
                type = TokenType.NUMBERTEXT;
                break;
            case "[dictetext]":
            case "[dictionarytext]":
                type = TokenType.DICTIONARYTEXT;
                break;
            case "[functiontext]":
                type = TokenType.FUNCTIONTEXT;
                break;
            case "[window]":
                type = TokenType.WINDOWTEXT;
                break;
            case "[defaultsize]":
                type = TokenType.DEFAULTSIZE;
                break;
            default:
                current = scanStart;
                addToken(TokenType.LEFT_SQUARE);
                return;
        }
        advance();
        addToken(type, text);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '.';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void blockComment() {
        while (!isAtEnd() && peek() != '<') {
            if (peek() == '\n') line++;
            advance();
        }
        if (!isAtEnd()) advance();
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        if (type == null) return;
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}

/**
 * A symbol table for typed variables in Project Imperative.
 */
class SymbolTable {
    private final Map<String, VariableEntry> symbols = new HashMap<>();

    void declare(String name, ValueType type, Object value) {
        String key = name.toLowerCase();
        if (symbols.containsKey(key)) {
            throw new LanguageRuntimeException("Variable already declared: " + name);
        }
        symbols.put(key, new VariableEntry(name, type, value));
    }

    VariableEntry lookup(String name) {
        String key = name.toLowerCase();
        VariableEntry entry = symbols.get(key);
        if (entry == null) {
            throw new LanguageRuntimeException("Undefined variable: " + name);
        }
        return entry;
    }

    void assign(String name, Object value, ValueType expectedType) {
        VariableEntry entry = lookup(name);
        if (entry.type != expectedType) {
            throw new TypeMismatchException(name, entry.type, expectedType);
        }
        entry.value = value;
    }
}

/**
 * Represents a typed variable in the symbol table.
 */
class VariableEntry {
    final String name;
    final ValueType type;
    Object value;

    VariableEntry(String name, ValueType type, Object value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }
}

/**
 * Supported language value types.
 */
enum ValueType {
    STRINGTEXT,
    NUMBERTEXT,
    DICTIONARYTEXT,
    FUNCTIONTEXT,
    WINDOWTEXT,
    DEFAULTSIZE
}

/**
 * Error handler for compile/runtime type mismatches.
 */
class ErrorHandler {
    static void throwTypeMismatch(String variableName, ValueType actual, ValueType expected) {
        throw new TypeMismatchException(variableName, actual, expected);
    }
}

class TypeMismatchException extends RuntimeException {
    TypeMismatchException(String variableName, ValueType actual, ValueType expected) {
        super(String.format("[Error: Inmatching text types to (%s)] actual=%s expected=%s",
                variableName, actual, expected));
    }
}

class LanguageRuntimeException extends RuntimeException {
    LanguageRuntimeException(String message) {
        super(message);
    }
}

interface NativeFunction {
    Object call(List<Object> arguments);
}

class FunctionValue {
    final String name;
    final NativeFunction function;

    FunctionValue(String name, NativeFunction function) {
        this.name = name;
        this.function = function;
    }

    Object call(List<Object> arguments) {
        return function.call(arguments);
    }

    @Override
    public String toString() {
        return "<native function " + name + ">";
    }
}

/**
 * Parser and AST support for Project Imperative.
 */
class ParseException extends RuntimeException {
    ParseException(String message) {
        super(message);
    }
}

abstract class Stmt {
    interface Visitor<R> {
        R visitVarStmt(Var stmt);
        R visitIfStmt(If stmt);
        R visitRepeatStmt(Repeat stmt);
        R visitLoopWindowWhileStmt(LoopWindowWhile stmt);
        R visitImportStmt(Import stmt);
        R visitCreateWindowStmt(CreateWindow stmt);
        R visitCreateAppStmt(CreateApp stmt);
        R visitDisplayStmt(Display stmt);
        R visitRenameWindowStmt(RenameWindow stmt);
        R visitOSCreateNewFileStmt(OSCreateNewFile stmt);
        R visitPrintStmt(Print stmt);
        R visitExpressionStmt(Expression stmt);
        R visitBlockStmt(Block stmt);
    }

    abstract <R> R accept(Visitor<R> visitor);

    static class Var extends Stmt {
        final String name;
        final ValueType type;
        final Expr initializer;

        Var(String name, ValueType type, Expr initializer) {
            this.name = name;
            this.type = type;
            this.initializer = initializer;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }
    }

    static class If extends Stmt {
        final String variableName;
        final ValueType expectedType;
        final Token operator;
        final Expr right;
        final Stmt thenBranch;
        final Stmt elseBranch;

        If(String variableName, ValueType expectedType, Token operator, Expr right, Stmt thenBranch, Stmt elseBranch) {
            this.variableName = variableName;
            this.expectedType = expectedType;
            this.operator = operator;
            this.right = right;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitIfStmt(this);
        }
    }

    static class Repeat extends Stmt {
        final String variableName;
        final Expr count;
        final Stmt body;

        Repeat(String variableName, Expr count, Stmt body) {
            this.variableName = variableName;
            this.count = count;
            this.body = body;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitRepeatStmt(this);
        }
    }

    static class LoopWindowWhile extends Stmt {
        final String windowName;
        final Stmt body;

        LoopWindowWhile(String windowName, Stmt body) {
            this.windowName = windowName;
            this.body = body;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLoopWindowWhileStmt(this);
        }
    }

    static class Import extends Stmt {
        final String libraryName;

        Import(String libraryName) {
            this.libraryName = libraryName;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitImportStmt(this);
        }
    }

    static class Print extends Stmt {
        final Expr expression;

        Print(Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitPrintStmt(this);
        }
    }

    static class CreateWindow extends Stmt {
        final Expr widthExpression;
        final Expr heightExpression;

        CreateWindow(Expr widthExpression, Expr heightExpression) {
            this.widthExpression = widthExpression;
            this.heightExpression = heightExpression;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitCreateWindowStmt(this);
        }
    }

    static class CreateApp extends Stmt {
        final boolean advanced;

        CreateApp(boolean advanced) {
            this.advanced = advanced;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitCreateAppStmt(this);
        }
    }

    static class Display extends Stmt {
        final String windowName;
        final String text;

        Display(String windowName, String text) {
            this.windowName = windowName;
            this.text = text;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitDisplayStmt(this);
        }
    }

    static class RenameWindow extends Stmt {
        final String oldName;
        final String newName;

        RenameWindow(String oldName, String newName) {
            this.oldName = oldName;
            this.newName = newName;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitRenameWindowStmt(this);
        }
    }

    static class OSCreateNewFile extends Stmt {
        final String fileName;
        final String path;

        OSCreateNewFile(String fileName, String path) {
            this.fileName = fileName;
            this.path = path;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitOSCreateNewFileStmt(this);
        }
    }

    static class Expression extends Stmt {
        final Expr expression;

        Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
    }

    static class Block extends Stmt {
        final List<Stmt> statements;

        Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBlockStmt(this);
        }
    }
}

abstract class Expr {
    interface Visitor<R> {
        R visitLiteralExpr(Literal expr);
        R visitVariableExpr(Variable expr);
        R visitDictionaryExpr(Dictionary expr);
        R visitBinaryExpr(Binary expr);
        R visitUnaryExpr(Unary expr);
        R visitCallExpr(Call expr);
        R visitAskExpr(Ask expr);
    }

    abstract <R> R accept(Visitor<R> visitor);

    static class Literal extends Expr {
        final Object value;

        Literal(Object value) {
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }

    static class Variable extends Expr {
        final Token name;

        Variable(Token name) {
            this.name = name;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }
    }

    static class Dictionary extends Expr {
        final Map<String, Expr> entries;

        Dictionary(Map<String, Expr> entries) {
            this.entries = entries;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitDictionaryExpr(this);
        }
    }

    static class Call extends Expr {
        final Expr callee;
        final List<Expr> arguments;

        Call(Expr callee, List<Expr> arguments) {
            this.callee = callee;
            this.arguments = arguments;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitCallExpr(this);
        }
    }

    static class Binary extends Expr {
        final Expr left;
        final Token operator;
        final Expr right;

        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }

    static class Unary extends Expr {
        final Token operator;
        final Expr right;

        Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }

    static class Ask extends Expr {
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitAskExpr(this);
        }
    }
}

class Parser {
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parseFile() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            skipNoise();
            if (isAtEnd()) break;
            statements.add(statement());
        }
        return statements;
    }

    private Stmt statement() {
        if (match(TokenType.ATTACH_VARIABLE)) return varDeclaration();
        if (match(TokenType.IF_)) return ifStatement();
        if (match(TokenType.REPEAT)) return repeatStatement();
        if (match(TokenType.LOOP_WINDOW_WHILE)) return loopWindowWhileStatement();
        if (match(TokenType.IMPORT_LIB)) return importStatement();
        if (match(TokenType.CREATE)) return createWindowStatement();
        if (match(TokenType.CREATE_WIND)) return createWindStatement();
        if (check(TokenType.IDENTIFIER) && peek().lexeme.equalsIgnoreCase("display")) return displayStatement();
        if (check(TokenType.IDENTIFIER) && peek().lexeme.toLowerCase().startsWith("ren_wind")) return renameWindowStatement();
        if (check(TokenType.IDENTIFIER) && peek().lexeme.equalsIgnoreCase("os_createnewfile")) return osCreateNewFileStatement();
        if (match(TokenType.CREATE_CALC)) return createCalcStatement(false);
        if (match(TokenType.CREATE_ADVANCEDCALC)) return createCalcStatement(true);
        return expressionStatement();
    }

    private Stmt printStatement() {
        Expr value = expression();
        return new Stmt.Print(value);
    }

    private Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name after attach_variable.");
        consume(TokenType.ASSIGN, "Expect '=' after variable name.");
        ValueType type = parseTypeAnnotation();
        Expr initializer;
        if (type == ValueType.DICTIONARYTEXT && check(TokenType.MINUS) && peekNext().type == TokenType.MINUS && peekNext(2).type == TokenType.LEFT_BRACE) {
            initializer = parseDictionaryLiteral();
        } else {
            initializer = expression();
        }
        return new Stmt.Var(name.lexeme, type, initializer);
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after if_.");
        Token name = consume(TokenType.IDENTIFIER, "Expect condition variable name.");
        consume(TokenType.RIGHT_PAREN, "Expect ')' after condition variable.");
        skipNoise();
        ValueType expectedType = parseTypeAnnotation();
        skipNoise();
        Token operator = consume(TokenType.EQUAL, "Expect '==' after type annotation.");
        Expr right = expression();
        skipNoise();
        consume(TokenType.THEN_, "Expect then after if condition.");
        skipNoise();
        Stmt thenBranch = parseBlock();
        skipNoise();
        Stmt elseBranch = null;
        if (match(TokenType.ELSE_)) {
            skipNoise();
            elseBranch = parseBlock();
        }
        return new Stmt.If(name.lexeme, expectedType, operator, right, thenBranch, elseBranch);
    }

    private Stmt repeatStatement() {
        Token name = consume(TokenType.IDENTIFIER, "Expect loop variable name.");
        consume(TokenType.ASSIGN, "Expect '=' after repeat variable.");
        Expr count = expression();
        consume(TokenType.TIMES, "Expect times after repeat count.");
        skipNoise();
        Stmt body = parseBlock();
        return new Stmt.Repeat(name.lexeme, count, body);
    }

    private Stmt loopWindowWhileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after loop_window_while.");
        Token window = consume(TokenType.IDENTIFIER, "Expect window name.");
        consume(TokenType.RIGHT_PAREN, "Expect ')' after window name.");
        consume(TokenType.CREATED, "Expect created after loop window condition.");
        skipNoise();
        Stmt body = parseBlock();
        return new Stmt.LoopWindowWhile(window.lexeme, body);
    }

    private Stmt importStatement() {
        Token name = consume(TokenType.IDENTIFIER, "Expect library name after import_lib.");
        return new Stmt.Import(name.lexeme);
    }

    private Stmt osCreateNewFileStatement() {
        Token command = consume(TokenType.IDENTIFIER, "Expect os_createnewfile command.");
        String fileName;
        if (match(TokenType.STRING_LITERAL)) {
            fileName = (String) previous().literal;
        } else if (match(TokenType.IDENTIFIER)) {
            fileName = previous().lexeme;
        } else {
            throw new ParseException("Expect file name after os_createnewfile.");
        }

        String path = null;
        if (check(TokenType.IDENTIFIER) && peek().lexeme.equalsIgnoreCase("path")) {
            advance();
            consume(TokenType.COLON, "Expect ':' after path.");
            path = collectPathString();
        }
        return new Stmt.OSCreateNewFile(fileName, path);
    }

    private String collectPathString() {
        StringBuilder builder = new StringBuilder();
        int pathLine = previous().line;
        while (!isAtEnd() && peek().line == pathLine && !check(TokenType.SEMICOLON)) {
            builder.append(peek().lexeme);
            advance();
        }
        return builder.toString().trim();
    }

    private Stmt createWindowStatement() {
        ValueType type = parseTypeAnnotation();
        if (type != ValueType.WINDOWTEXT) {
            throw new ParseException("Expect [window] after create.");
        }
        Token keyword = consume(TokenType.IDENTIFIER, "Expect size keyword after create-[window].");
        if (!"size".equals(keyword.lexeme)) {
            throw new ParseException("Expect 'size' after create-[window]. Found '" + keyword.lexeme + "'.");
        }
        consume(TokenType.LEFT_BRACE, "Expect '{' after create-[window]size.");
        consume(TokenType.LEFT_PAREN, "Expect '(' before size expression.");
        Expr widthExpr = expression();
        Expr heightExpr = null;
        if (match(TokenType.COMMA)) {
            heightExpr = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after size expression.");
        consume(TokenType.RIGHT_BRACE, "Expect '}' after create-[window]size block.");
        return new Stmt.CreateWindow(widthExpr, heightExpr);
    }

    private Stmt createWindStatement() {
        ValueType type = parseTypeAnnotation();
        if (type != ValueType.DEFAULTSIZE) {
            throw new ParseException("Expect [defaultsize] after Create_wind.");
        }
        Expr widthExpr = new Expr.Literal("defaultsize");
        return new Stmt.CreateWindow(widthExpr, null);
    }

    private Stmt displayStatement() {
        consume(TokenType.IDENTIFIER, "Expect display command.");
        ValueType type = parseTypeAnnotation();
        if (type != ValueType.STRINGTEXT) {
            throw new ParseException("Expect [stringtext] after display.");
        }
        Token target = consume(TokenType.IDENTIFIER, "Expect target window after display.");
        String targetName = target.lexeme;
        if (!targetName.toLowerCase().startsWith("in_")) {
            throw new ParseException("Expect window target in format in_<windowName>.");
        }
        String windowName = targetName.substring(3);
        Token textToken = consume(TokenType.STRING_LITERAL, "Expect string literal after display target.");
        return new Stmt.Display(windowName, (String) textToken.literal);
    }

    private Stmt renameWindowStatement() {
        Token command = consume(TokenType.IDENTIFIER, "Expect rename command.");
        String lowerCommand = command.lexeme.toLowerCase();
        if (!lowerCommand.startsWith("ren_wind")) {
            throw new ParseException("Unknown rename command: " + command.lexeme);
        }
        String oldName = command.lexeme.substring("ren_".length());
        Token toToken = consume(TokenType.IDENTIFIER, "Expect 'to' after rename command.");
        if (!toToken.lexeme.equalsIgnoreCase("to")) {
            throw new ParseException("Expect 'to' after rename command.");
        }
        consume(TokenType.MINUS, "Expect '-' before new window name.");
        consume(TokenType.MINUS, "Expect second '-' before new window name.");
        Token newNameToken = consume(TokenType.IDENTIFIER, "Expect new window name after --.");
        return new Stmt.RenameWindow(oldName, newNameToken.lexeme);
    }

    private Stmt createCalcStatement(boolean advanced) {
        return new Stmt.CreateApp(advanced);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        return new Stmt.Expression(expr);
    }

    private Stmt parseBlock() {
        if (match(TokenType.LEFT_BRACE)) {
            List<Stmt> statements = new ArrayList<>();
            while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
                statements.add(statement());
            }
            consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
            return new Stmt.Block(statements);
        }
        if (match(TokenType.LEFT_SQUARE)) {
            List<Stmt> statements = new ArrayList<Stmt>();
            while (!check(TokenType.RIGHT_SQUARE) && !isAtEnd()) {
                statements.add(statement());
            }
            consume(TokenType.RIGHT_SQUARE, "Expect ']' after block.");
            return new Stmt.Block(statements);
        }
        List<Stmt> statements = new ArrayList<Stmt>();
        statements.add(statement());
        return new Stmt.Block(statements);
    }

    private Expr expression() {
        return equality();
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(TokenType.EQUAL, TokenType.NOT_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, arguments);
    }

    private Expr primary() {
        if (match(TokenType.STRINGTEXT, TokenType.NUMBERTEXT, TokenType.FUNCTIONTEXT)) {
            if (match(TokenType.STRING_LITERAL)) {
                return new Expr.Literal(previous().literal);
            }
            if (match(TokenType.NUMBER_LITERAL)) {
                return new Expr.Literal(previous().literal);
            }
            if (match(TokenType.ASK)) {
                return new Expr.Ask();
            }
            if (match(TokenType.IDENTIFIER)) {
                return new Expr.Variable(previous());
            }
            throw new ParseException("Expect a value after type annotation.");
        }
        if (match(TokenType.DICTIONARYTEXT)) {
            if (check(TokenType.MINUS) && peekNext().type == TokenType.MINUS && peekNext(2).type == TokenType.LEFT_BRACE) {
                return parseDictionaryLiteral();
            }
            if (match(TokenType.ASK)) {
                return new Expr.Ask();
            }
            if (match(TokenType.IDENTIFIER)) {
                return new Expr.Variable(previous());
            }
            throw new ParseException("Expect a value after dictionary type annotation.");
        }
        if (match(TokenType.NUMBER_LITERAL)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(TokenType.STRING_LITERAL)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(TokenType.ASK)) {
            return new Expr.Ask();
        }
        if (match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return expr;
        }
        throw new ParseException("Expect expression.");
    }

    private ValueType parseTypeAnnotation() {
        if (match(TokenType.STRINGTEXT)) return ValueType.STRINGTEXT;
        if (match(TokenType.NUMBERTEXT)) return ValueType.NUMBERTEXT;
        if (match(TokenType.DICTIONARYTEXT)) return ValueType.DICTIONARYTEXT;
        if (match(TokenType.FUNCTIONTEXT)) return ValueType.FUNCTIONTEXT;
        if (match(TokenType.DEFAULTSIZE)) return ValueType.DEFAULTSIZE;
        throw new ParseException("Expect type annotation like [stringtext], [numbertext], [dictetext], or [defaultsize].");
    }

    private void skipNoise() {
        while (check(TokenType.MINUS) || check(TokenType.LEFT_SQUARE) || check(TokenType.RIGHT_SQUARE)) {
            advance();
        }
    }

    private Token peekNext() {
        return peekNext(1);
    }

    private Token peekNext(int offset) {
        int index = current + offset;
        if (index >= tokens.size()) {
            return tokens.get(tokens.size() - 1);
        }
        return tokens.get(index);
    }

    private Expr parseDictionaryLiteral() {
        consume(TokenType.MINUS, "Expect '-' before dictionary literal start.");
        consume(TokenType.MINUS, "Expect second '-' before dictionary literal start.");
        consume(TokenType.LEFT_BRACE, "Expect '{' after dictionary literal start.");
        Map<String, Expr> entries = new HashMap<>();
        while (!(check(TokenType.MINUS) && peekNext().type == TokenType.RIGHT_BRACE) && !isAtEnd()) {
            skipNoise();
            if (check(TokenType.MINUS) && peekNext().type == TokenType.RIGHT_BRACE) {
                break;
            }
            String key;
            if (match(TokenType.IDENTIFIER)) {
                key = previous().lexeme;
            } else if (match(TokenType.STRING_LITERAL)) {
                key = (String) previous().literal;
            } else {
                throw new ParseException("Expect dictionary key name.");
            }
            consume(TokenType.ASSIGN, "Expect '=' after dictionary key.");
            parseTypeAnnotation();
            Expr value = expression();
            entries.put(key, value);
            skipNoise();
        }
        consume(TokenType.MINUS, "Expect '-' before dictionary literal end.");
        consume(TokenType.RIGHT_BRACE, "Expect '}' after dictionary literal end.");
        return new Expr.Dictionary(entries);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw new ParseException(message + " Found '" + peek().lexeme + "' at line " + peek().line + ".");
    }

    private boolean check(TokenType type) {
        return !isAtEnd() && peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}

class Interpreter implements Stmt.Visitor<Object>, Expr.Visitor<Object> {
    private Environment environment = new Environment(null);
    private final Scanner scanner;
    private final PrintStream output;
    private final java.util.Set<String> loadedLibraries = new java.util.HashSet<>();

    Interpreter() {
        this(System.in, System.out);
    }

    Interpreter(InputStream input, PrintStream output) {
        this.scanner = new Scanner(input);
        this.output = output;
        registerNativeFunction("func.printline", arguments -> {
            if (arguments.size() != 1 || !(arguments.get(0) instanceof String)) {
                throw new LanguageRuntimeException("func.printline([stringtext]message) requires one string argument.");
            }
            output.println(arguments.get(0));
            return null;
        });
    }

    void registerNativeFunction(String name, NativeFunction function) {
        environment.define(name, ValueType.FUNCTIONTEXT, new FunctionValue(name, function));
    }

    void markLibraryLoaded(String name) {
        loadedLibraries.add(name.toLowerCase());
    }

    boolean isLibraryLoaded(String name) {
        return loadedLibraries.contains(name.toLowerCase());
    }

    void interpret(List<Stmt> statements) {
        for (Stmt statement : statements) {
            execute(statement);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitVarStmt(Stmt.Var stmt) {
        Object value = evaluate(stmt.initializer);
        if (!typeMatches(stmt.type, value)) {
            throw new TypeMismatchException(stmt.name, typeOf(value), stmt.type);
        }
        environment.define(stmt.name, stmt.type, value);
        return null;
    }

    @Override
    public Object visitIfStmt(Stmt.If stmt) {
        VariableEntry variable = environment.resolve(stmt.variableName);
        if (variable.type != stmt.expectedType) {
            throw new TypeMismatchException(stmt.variableName, variable.type, stmt.expectedType);
        }
        Object rightValue = evaluate(stmt.right);
        boolean condition = evaluateCondition(variable.value, stmt.operator, rightValue);
        if (condition) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Object visitRepeatStmt(Stmt.Repeat stmt) {
        Object countValue = evaluate(stmt.count);
        int times = (int) toNumber(countValue);
        for (int i = 1; i <= times; i++) {
            Environment loopEnv = new Environment(environment);
            loopEnv.define(stmt.variableName, ValueType.NUMBERTEXT, (double) i);
            executeBlock(stmt.body, loopEnv);
        }
        return null;
    }

    @Override
    public Object visitLoopWindowWhileStmt(Stmt.LoopWindowWhile stmt) {
        output.println("loop_window_while(" + stmt.windowName + ") created");
        executeBlock(stmt.body, new Environment(environment));
        return null;
    }

    @Override
    public Object visitCreateWindowStmt(Stmt.CreateWindow stmt) {
        Object widthValue = evaluate(stmt.widthExpression);
        String result;
        if (stmt.heightExpression != null) {
            Object heightValue = evaluate(stmt.heightExpression);
            result = String.format("create window size: %sx%s", stringify(widthValue), stringify(heightValue));
        } else if ("defaultsize".equals(widthValue)) {
            result = "create window default size";
            if (isLibraryLoaded("gf") || isLibraryLoaded("gfgames")) {
                GraphicsLibrary.openWindow("wind1", 800, 600);
            }
        } else {
            result = "create window size: " + stringify(widthValue);
        }
        output.println(result);
        return null;
    }

    @Override
    public Object visitCreateAppStmt(Stmt.CreateApp stmt) {
        if (!isLibraryLoaded("calc")) {
            throw new LanguageRuntimeException("Calc library is not loaded. Use import_lib calc first.");
        }
        String title = stmt.advanced ? "Advanced Calculator" : "Calculator";
        launchCalculatorWindow(title);
        output.println("create app: " + title);
        return null;
    }

    @Override
    public Object visitDisplayStmt(Stmt.Display stmt) {
        if (!isLibraryLoaded("gf") && !isLibraryLoaded("gfgames")) {
            throw new LanguageRuntimeException("GF Games library is not loaded. Use import_lib GF or import_lib GFGames first.");
        }
        GraphicsLibrary.displayText(stmt.windowName, stmt.text);
        output.println("displayed text in " + stmt.windowName);
        return null;
    }

    @Override
    public Object visitRenameWindowStmt(Stmt.RenameWindow stmt) {
        if (!isLibraryLoaded("gf") && !isLibraryLoaded("gfgames")) {
            throw new LanguageRuntimeException("GF Games library is not loaded. Use import_lib GF or import_lib GFGames first.");
        }
        GraphicsLibrary.renameWindow(stmt.oldName, stmt.newName);
        output.println("renamed window " + stmt.oldName + " to " + stmt.newName);
        return null;
    }

    @Override
    public Object visitOSCreateNewFileStmt(Stmt.OSCreateNewFile stmt) {
        if (!isLibraryLoaded("os")) {
            throw new LanguageRuntimeException("OS library is not loaded. Use import_lib OS first.");
        }
        try {
            String directory = stmt.path != null ? stmt.path : ".";
            Path targetPath = Paths.get(directory);
            Path filePath;
            if (directory.endsWith("/") || directory.endsWith("\\") || targetPath.getFileName() == null || !targetPath.getFileName().toString().contains(".")) {
                filePath = targetPath.resolve(stmt.fileName);
            } else {
                filePath = targetPath;
            }
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
            output.println("created file " + filePath.toString());
            return filePath.toString();
        } catch (Exception ex) {
            throw new LanguageRuntimeException("os_createnewfile failed: " + ex.getMessage());
        }
    }

    @Override
    public Object visitImportStmt(Stmt.Import stmt) {
        ImportLib.loadLibrary(stmt.libraryName, this);
        return null;
    }

    private void launchCalculatorWindow(String title) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(title);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(320, 460);
            frame.setLocationRelativeTo(null);
            frame.setLayout(new BorderLayout(4, 4));

            JTextField display = new JTextField();
            display.setEditable(false);
            display.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
            display.setHorizontalAlignment(JTextField.RIGHT);
            frame.add(display, BorderLayout.NORTH);

            JPanel buttons = new JPanel(new GridLayout(5, 4, 4, 4));
            String[] labels = {"7", "8", "9", "/", "4", "5", "6", "*", "1", "2", "3", "-", "0", ".", "=", "+", "C", "CE", "(", ")"};
            for (String label : labels) {
                JButton button = new JButton(label);
                button.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
                button.addActionListener(e -> {
                    String text = display.getText();
                    if ("C".equals(label)) {
                        display.setText("");
                    } else if ("CE".equals(label)) {
                        if (!text.isEmpty()) {
                            display.setText(text.substring(0, text.length() - 1));
                        }
                    } else if ("=".equals(label)) {
                        try {
                            Object result = new ScriptEngineManager().getEngineByName("JavaScript").eval(text);
                            display.setText(result == null ? "" : result.toString());
                        } catch (Exception ex) {
                            display.setText("ERROR");
                        }
                    } else {
                        display.setText(text + label);
                    }
                });
                buttons.add(button);
            }
            frame.add(buttons, BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }

    @Override
    public Object visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        output.println(stringify(value));
        return null;
    }

    @Override
    public Object visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Object visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt, new Environment(environment));
        return null;
    }

    private void executeBlock(Stmt block, Environment env) {
        Environment previous = environment;
        try {
            environment = env;
            if (block instanceof Stmt.Block) {
                for (Stmt statement : ((Stmt.Block) block).statements) {
                    execute(statement);
                }
            }
        } finally {
            environment = previous;
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.resolve(expr.name.lexeme).value;
    }

    @Override
    public Object visitDictionaryExpr(Expr.Dictionary expr) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Expr> entry : expr.entries.entrySet()) {
            result.put(entry.getKey(), evaluate(entry.getValue()));
        }
        return result;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case PLUS:
                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }
                return toNumber(left) + toNumber(right);
            case MINUS:
                return toNumber(left) - toNumber(right);
            case STAR:
                return toNumber(left) * toNumber(right);
            case SLASH:
                return toNumber(left) / toNumber(right);
            case EQUAL:
                return isEqual(left, right);
            case NOT_EQUAL:
                return !isEqual(left, right);
            case GREATER:
                return toNumber(left) > toNumber(right);
            case GREATER_EQUAL:
                return toNumber(left) >= toNumber(right);
            case LESS:
                return toNumber(left) < toNumber(right);
            case LESS_EQUAL:
                return toNumber(left) <= toNumber(right);
            default:
                throw new LanguageRuntimeException("Unknown binary operator: " + expr.operator.lexeme);
        }
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case MINUS:
                return -toNumber(right);
            default:
                throw new LanguageRuntimeException("Unknown unary operator: " + expr.operator.lexeme);
        }
    }

    @Override
    public Object visitAskExpr(Expr.Ask expr) {
        output.print("ask> ");
        return scanner.nextLine();
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);
        if (!(callee instanceof FunctionValue)) {
            throw new LanguageRuntimeException("Can only call functions.");
        }
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }
        return ((FunctionValue) callee).call(arguments);
    }

    private boolean evaluateCondition(Object left, Token operator, Object right) {
        switch (operator.type) {
            case EQUAL:
                return isEqual(left, right);
            case NOT_EQUAL:
                return !isEqual(left, right);
            default:
                throw new LanguageRuntimeException("Unsupported condition operator: " + operator.lexeme);
        }
    }

    private boolean typeMatches(ValueType expected, Object value) {
        if (expected == ValueType.STRINGTEXT) return value instanceof String;
        if (expected == ValueType.NUMBERTEXT) return value instanceof Double;
        if (expected == ValueType.DICTIONARYTEXT) return value instanceof Map;
        if (expected == ValueType.FUNCTIONTEXT) return value instanceof String || value instanceof FunctionValue;
        if (expected == ValueType.WINDOWTEXT) return value instanceof String;
        if (expected == ValueType.DEFAULTSIZE) return value instanceof String;
        return false;
    }

    private ValueType typeOf(Object value) {
        if (value instanceof String) return ValueType.STRINGTEXT;
        if (value instanceof Double) return ValueType.NUMBERTEXT;
        if (value instanceof Map) return ValueType.DICTIONARYTEXT;
        if (value instanceof FunctionValue) return ValueType.FUNCTIONTEXT;
        return ValueType.FUNCTIONTEXT;
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (Boolean) object;
        if (object instanceof Double) return (Double) object != 0;
        if (object instanceof String) return !((String) object).isEmpty();
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private double toNumber(Object value) {
        if (value instanceof Double) return (Double) value;
        throw new LanguageRuntimeException("Value must be a number: " + stringify(value));
    }

    private String stringify(Object object) {
        if (object == null) return "null";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                return text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }
}

/**
 * Represents scope chaining for nested blocks and function bodies.
 */
class Environment {
    private final Environment parent;
    private final SymbolTable symbols;

    Environment(Environment parent) {
        this.parent = parent;
        this.symbols = new SymbolTable();
    }

    void define(String name, ValueType type, Object value) {
        symbols.declare(name, type, value);
    }

    VariableEntry resolve(String name) {
        try {
            return symbols.lookup(name);
        } catch (LanguageRuntimeException e) {
            if (parent != null) return parent.resolve(name);
            throw e;
        }
    }
}

/**
 * Runtime loader for modules/libraries.
 */
class ImportLib {
    static void loadLibrary(String name, Interpreter interpreter) {
        String lowerName = name.toLowerCase();
        switch (lowerName) {
            case "gf":
            case "gfgames":
                GraphicsLibrary.load(interpreter);
                break;
            case "gkd":
                KeyDetectionLibrary.load(interpreter);
                break;
            case "os":
                OSLibrary.load(interpreter);
                break;
            case "calc":
            case "calculator":
                CalcLibrary.load(interpreter);
                break;
            default:
                throw new LanguageRuntimeException("Unknown library: " + name);
        }
    }
}

class GraphicsLibrary {
    private static final java.util.Map<String, GFWindow> windows = new java.util.HashMap<>();

    static void load(Interpreter interpreter) {
        interpreter.markLibraryLoaded("gf");
        interpreter.markLibraryLoaded("gfgames");
    }

    static void openWindow(String name, int width, int height) {
        synchronized (windows) {
            GFWindow window = windows.get(name);
            if (window == null) {
                window = new GFWindow(name, width, height);
                windows.put(name, window);
            } else {
                window.setSize(width, height);
                window.show();
            }
        }
    }

    static void displayText(String windowName, String text) {
        GFWindow window = findWindow(windowName);
        window.setTopLeftText(text);
        window.show();
    }

    static void renameWindow(String oldName, String newName) {
        synchronized (windows) {
            GFWindow window = windows.remove(oldName);
            if (window == null) {
                throw new LanguageRuntimeException("Window not found: " + oldName);
            }
            window.setName(newName);
            window.setTitle(newName);
            windows.put(newName, window);
        }
    }

    private static GFWindow findWindow(String name) {
        synchronized (windows) {
            GFWindow window = windows.get(name);
            if (window == null) {
                throw new LanguageRuntimeException("Window not found: " + name);
            }
            return window;
        }
    }

    private static class GFWindow {
        private JFrame frame;
        private JLabel label;
        private String name;

        GFWindow(String name, int width, int height) {
            this.name = name;
            SwingUtilities.invokeLater(() -> {
                frame = new JFrame(name);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setSize(width, height);
                frame.setLocationRelativeTo(null);
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
                label = new JLabel("");
                label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
                panel.add(label);
                frame.getContentPane().add(panel, BorderLayout.NORTH);
                frame.setVisible(true);
            });
        }

        void setName(String name) {
            this.name = name;
        }

        void setTitle(String title) {
            SwingUtilities.invokeLater(() -> {
                if (frame != null) {
                    frame.setTitle(title);
                }
            });
        }

        void setTopLeftText(String text) {
            SwingUtilities.invokeLater(() -> {
                if (label != null) {
                    label.setText(text);
                }
            });
        }

        void setSize(int width, int height) {
            SwingUtilities.invokeLater(() -> {
                if (frame != null) {
                    frame.setSize(width, height);
                }
            });
        }

        void show() {
            SwingUtilities.invokeLater(() -> {
                if (frame != null) {
                    frame.setVisible(true);
                }
            });
        }
    }
}

class CalcLibrary {
    static void load(Interpreter interpreter) {
        interpreter.markLibraryLoaded("calc");
        interpreter.registerNativeFunction("create_calc", arguments -> {
            throw new LanguageRuntimeException("Use the create_calc statement after importing calc, not as a function call.");
        });
        interpreter.registerNativeFunction("create_advancedcalc", arguments -> {
            throw new LanguageRuntimeException("Use the create_advancedcalc statement after importing calc, not as a function call.");
        });
    }
}

class KeyDetectionLibrary {
    static void load(Interpreter interpreter) {
        // stub: register key-detection functions with interpreter
    }
}

class OSLibrary {
    static void load(Interpreter interpreter) {
        if (!isAdministrator()) {
            throw new LanguageRuntimeException("OS library requires administrator privileges to load.");
        }

        interpreter.registerNativeFunction("os_read", arguments -> {
            if (arguments.size() != 1 || !(arguments.get(0) instanceof String)) {
                throw new LanguageRuntimeException("os_read(path) requires a string path argument.");
            }
            try {
                return new String(Files.readAllBytes(Paths.get((String) arguments.get(0))), StandardCharsets.UTF_8);
            } catch (Exception ex) {
                throw new LanguageRuntimeException("os_read failed: " + ex.getMessage());
            }
        });

        interpreter.registerNativeFunction("os_write", arguments -> {
            if (arguments.size() != 2 || !(arguments.get(0) instanceof String) || !(arguments.get(1) instanceof String)) {
                throw new LanguageRuntimeException("os_write(path, contents) requires two string arguments.");
            }
            try {
                Files.write(Paths.get((String) arguments.get(0)), ((String) arguments.get(1)).getBytes(StandardCharsets.UTF_8));
                return "OK";
            } catch (Exception ex) {
                throw new LanguageRuntimeException("os_write failed: " + ex.getMessage());
            }
        });

        interpreter.registerNativeFunction("os_createnewfile", arguments -> {
            if (arguments.size() < 1 || arguments.size() > 2 || !(arguments.get(0) instanceof String) || (arguments.size() == 2 && !(arguments.get(1) instanceof String))) {
                throw new LanguageRuntimeException("os_createnewfile(filename, [directory]) requires one or two string arguments.");
            }
            String fileName = (String) arguments.get(0);
            String directory = arguments.size() == 2 ? (String) arguments.get(1) : ".";
            try {
                Path targetPath = Paths.get(directory);
                Path filePath = (directory.endsWith("/") || directory.endsWith("\\") || targetPath.getFileName() == null || !targetPath.getFileName().toString().contains("."))
                        ? targetPath.resolve(fileName)
                        : targetPath;
                if (filePath.getParent() != null) {
                    Files.createDirectories(filePath.getParent());
                }
                if (!Files.exists(filePath)) {
                    Files.createFile(filePath);
                }
                return filePath.toString();
            } catch (Exception ex) {
                throw new LanguageRuntimeException("os_createnewfile failed: " + ex.getMessage());
            }
        });

        interpreter.registerNativeFunction("os_exists", arguments -> {
            if (arguments.size() != 1 || !(arguments.get(0) instanceof String)) {
                throw new LanguageRuntimeException("os_exists(path) requires a string path argument.");
            }
            return Files.exists(Paths.get((String) arguments.get(0))) ? 1.0 : 0.0;
        });

        interpreter.registerNativeFunction("os_delete", arguments -> {
            if (arguments.size() != 1 || !(arguments.get(0) instanceof String)) {
                throw new LanguageRuntimeException("os_delete(path) requires a string path argument.");
            }
            try {
                return Files.deleteIfExists(Paths.get((String) arguments.get(0))) ? 1.0 : 0.0;
            } catch (Exception ex) {
                throw new LanguageRuntimeException("os_delete failed: " + ex.getMessage());
            }
        });

        interpreter.registerNativeFunction("os_list", arguments -> {
            if (arguments.size() != 1 || !(arguments.get(0) instanceof String)) {
                throw new LanguageRuntimeException("os_list(path) requires a string path argument.");
            }
            try {
                return String.join(", ", Files.list(Paths.get((String) arguments.get(0))).map(path -> path.toString()).sorted().collect(Collectors.toList()));
            } catch (Exception ex) {
                throw new LanguageRuntimeException("os_list failed: " + ex.getMessage());
            }
        });
    }

    private static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private static boolean isAdministrator() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return false;
        }

        try {
            Process process = new ProcessBuilder("cmd", "/c", "whoami /groups").redirectErrorStream(true).start();
            String output = new String(toByteArray(process.getInputStream()), StandardCharsets.UTF_8);
            process.waitFor();
            return output.contains("S-1-5-32-544") || output.toLowerCase().contains("administrators");
        } catch (Exception ex) {
            return false;
        }
    }
}
