import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

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
    LOOP_WINDOW_WHILE, CREATED, IMPORT_LIB,
    ASK, FUNCTIONTEXT, DICTIONARYTEXT, NUMBERTEXT, STRINGTEXT,

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
                stringLiteral();
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
        TokenType type = TokenType.IDENTIFIER;

        switch (value) {
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
            case "ask":
                type = TokenType.ASK;
                break;
            case "print":
                type = TokenType.PRINT;
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

    private void stringLiteral() {
        while (peek() != '"' && !isAtEnd()) {
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
        TokenType type = TokenType.IDENTIFIER;
        switch (text) {
            case "[stringtext]":
                type = TokenType.STRINGTEXT;
                break;
            case "[numbertext]":
                type = TokenType.NUMBERTEXT;
                break;
            case "[dictionarytext]":
                type = TokenType.DICTIONARYTEXT;
                break;
            case "[functiontext]":
                type = TokenType.FUNCTIONTEXT;
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
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
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
        if (symbols.containsKey(name)) {
            throw new LanguageRuntimeException("Variable already declared: " + name);
        }
        symbols.put(name, new VariableEntry(name, type, value));
    }

    VariableEntry lookup(String name) {
        VariableEntry entry = symbols.get(name);
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
    FUNCTIONTEXT
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
        R visitBinaryExpr(Binary expr);
        R visitUnaryExpr(Unary expr);
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
        if (match(TokenType.PRINT)) return printStatement();
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
        Expr initializer = expression();
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
            List<Stmt> statements = new ArrayList<>();
            while (!check(TokenType.RIGHT_SQUARE) && !isAtEnd()) {
                statements.add(statement());
            }
            consume(TokenType.RIGHT_SQUARE, "Expect ']' after block.");
            return new Stmt.Block(statements);
        }
        return new Stmt.Block(List.of(statement()));
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
        return primary();
    }

    private Expr primary() {
        if (match(TokenType.STRINGTEXT, TokenType.NUMBERTEXT, TokenType.DICTIONARYTEXT, TokenType.FUNCTIONTEXT)) {
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
        throw new ParseException("Expect type annotation like [stringtext] or [numbertext].");
    }

    private void skipNoise() {
        while (check(TokenType.MINUS) || check(TokenType.LEFT_SQUARE) || check(TokenType.RIGHT_SQUARE)) {
            advance();
        }
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
    private final SymbolTable globals = new SymbolTable();
    private Environment environment = new Environment(globals);
    private final Scanner scanner = new Scanner(System.in);

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
        System.out.println("loop_window_while(" + stmt.windowName + ") created");
        executeBlock(stmt.body, new Environment(environment));
        return null;
    }

    @Override
    public Object visitImportStmt(Stmt.Import stmt) {
        ImportLib.loadLibrary(stmt.libraryName);
        return null;
    }

    @Override
    public Object visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
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
        System.out.print("ask> ");
        return scanner.nextLine();
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
        if (expected == ValueType.FUNCTIONTEXT) return value instanceof String;
        return false;
    }

    private ValueType typeOf(Object value) {
        if (value instanceof String) return ValueType.STRINGTEXT;
        if (value instanceof Double) return ValueType.NUMBERTEXT;
        if (value instanceof Map) return ValueType.DICTIONARYTEXT;
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
    static void loadLibrary(String name) {
        switch (name) {
            case "GF":
                GraphicsLibrary.load();
                break;
            case "GKD":
                KeyDetectionLibrary.load();
                break;
            default:
                throw new LanguageRuntimeException("Unknown library: " + name);
        }
    }
}

class GraphicsLibrary {
    static void load() {
        // stub: register graphics functions with interpreter
    }
}

class KeyDetectionLibrary {
    static void load() {
        // stub: register key-detection functions with interpreter
    }
}
