# Project Imperative

A simple Java interpreter for a custom imperative language.

## Requirements

- Java 17 or newer installed and available on your PATH.
- A shell or command prompt to run the included scripts.

## Quick start

### Windows

1. Open a command prompt in the repository root.
2. Run:
   ```bat
   compile.bat
   run.bat sample.pi
   ```

### macOS / Linux

1. Open a terminal in the repository root.
2. Run:
   ```bash
   chmod +x run.sh
   ./run.sh sample.pi
   ```

### IDE

- Windows: `run-ide.bat`
- macOS/Linux: `chmod +x run-ide.sh && ./run-ide.sh`

This opens a Swing-based CRUM IDE with source editing, output console, and `ask`-input support. It can open and save files with `.crum`, `.cr`, `.pi`, and `.txt` extensions.

### Native Windows executable

To package the IDE as a native Windows app image, run:

```bat
package-ide.bat
```

The built executable will appear in:

```bat
dist\ProjectImperativeIDE\ProjectImperativeIDE.exe
```

This folder also contains the runtime image used by the IDE.

### Gradle (optional)

If you have Gradle installed, you can build and run with Gradle:

```bash
gradle build
gradle run --args='sample.pi'
```

### Maven (recommended for publishing)

If you have Maven installed, you can build and package with:

```bash
mvn clean package
```

Run the interpreter using Maven:

```bash
mvn exec:java -Dexec.args="sample.pi"
```

The packaged jar will be available in `target/project-imperative-0.1.0.jar`.

For publishing to a Maven repository, configure `<distributionManagement>` in `pom.xml` and use `mvn deploy` with the repository credentials.

## CRUM language reference

- `attach_variable name = [type]value`
  - Types: `[stringtext]`, `[numbertext]`, `[dictionarytext]`, `[functiontext]`, `[window]`
- `print <expression>` prints values.
- `ask` reads a line from input.
- `if_(name)-[type]- == <expr> -[ then -[ ... -] -]` for conditional execution.
- `repeat i = <count> times -{ ... -}` for loops.
- `loop_window_while(window)created -{ ... -}` for window loop scaffolding.
- `import_lib GF` or `import_lib GFGames` loads the graphics stub.
- `create-[window]size{(<size>)}` creates a window with the given size expression.

## Files

- `ProjectStructure.java` - interpreter source and main entry point.
- `sample.pi` - sample CRUM program.
- `compile.bat` - compile the Java source on Windows.
- `run.bat` - run the interpreter on Windows.
- `ProjectImperativeIDE.java` - a Swing-based IDE for editing and running CRUM programs.
- `run-ide.bat` - compile and launch the IDE on Windows.
- `run-ide.sh` - compile and launch the IDE on macOS/Linux.
- `run.sh` - compile and run the interpreter on macOS/Linux.
- `.github/workflows/ci.yml` - GitHub Actions workflow to verify compilation.
- `.gitignore` - ignores build artifacts and editor files.

## GitHub Publishing

When you publish this repository to GitHub, anyone can clone it and run it by following the above steps. The included scripts keep the workflow simple and portable.

## Notes

- If Java is not on your PATH, install a JDK and set `JAVA_HOME` or add `java`/`javac` to PATH.
- This project is compiled for Java 8 compatibility, so it can run on older machines with Java 8 or newer.
- The interpreter is intentionally kept in the repository root with no package declaration for easy execution.
