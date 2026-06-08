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

### Gradle (optional)

If you have Gradle installed, you can build and run with Gradle:

```bash
gradle build
gradle run --args='sample.pi'
```

## Files

- `ProjectStructure.java` - interpreter source and main entry point.
- `sample.pi` - sample Project Imperative program.
- `compile.bat` - compile the Java source on Windows.
- `run.bat` - run the interpreter on Windows.
- `run.sh` - compile and run the interpreter on macOS/Linux.
- `.github/workflows/ci.yml` - GitHub Actions workflow to verify compilation.
- `.gitignore` - ignores build artifacts and editor files.

## GitHub Publishing

When you publish this repository to GitHub, anyone can clone it and run it by following the above steps. The included scripts keep the workflow simple and portable.

## Notes

- If Java is not on your PATH, install a JDK and set `JAVA_HOME` or add `java`/`javac` to PATH.
- This project is compiled for Java 8 compatibility, so it can run on older machines with Java 8 or newer.
- The interpreter is intentionally kept in the repository root with no package declaration for easy execution.
