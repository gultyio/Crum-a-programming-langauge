import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class IDEGFGames extends JFrame {
    private static final String STARTING_PLATE = "> Welcome to IDE GF Games\n" +
            "> This sample shows how to use a multiline comment block. It starts with > and ends with <.\n" +
            "> You can write comments across several lines and they are ignored by the interpreter.\n" +
            "<\n\n" +
            "import_lib GFGames\n" +
            "Create_wind[defaultsize]\n" +
            "attach_variable count = [numbertext]10\n" +
            "print count\n" +
            "ask\n";

    private final JTextArea editorArea;
    private final JTextArea outputArea;
    private final JTextField inputField;
    private final JLabel statusLabel;
    private File currentFile;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            IDEGFGames ide = new IDEGFGames();
            ide.setVisible(true);
        });
    }

    public IDEGFGames() {
        super("IDE GF Games");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        JMenuBar menuBar = new JMenuBar();
        JMenu examplesMenu = new JMenu("Examples");
        JMenuItem helloExample = new JMenuItem("Hello Project");
        helloExample.addActionListener(e -> loadExample("hello.pi"));
        JMenuItem loopExample = new JMenuItem("Loop Project");
        loopExample.addActionListener(e -> loadExample("loop.pi"));
        JMenuItem calcExample = new JMenuItem("Calculator Starter");
        calcExample.addActionListener(e -> loadExample("calc.crum"));
        JMenuItem advancedCalcExample = new JMenuItem("Advanced Calculator Starter");
        advancedCalcExample.addActionListener(e -> loadExample("advancedcalc.crum"));
        examplesMenu.add(helloExample);
        examplesMenu.add(loopExample);
        examplesMenu.add(calcExample);
        examplesMenu.add(advancedCalcExample);
        menuBar.add(examplesMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpItem = new JMenuItem("Show Help");
        helpItem.addActionListener(e -> showHelp());
        helpMenu.add(helpItem);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        editorArea = new JTextArea();
        editorArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        configureEditorIndentation();
        outputArea = new JTextArea();
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        outputArea.setEditable(false);
        inputField = new JTextField();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(new JScrollPane(editorArea));
        splitPane.setBottomComponent(new JScrollPane(outputArea));
        splitPane.setResizeWeight(0.65);

        JPanel controls = new JPanel(new BorderLayout(8, 8));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(createButton("New", e -> newFile()));
        buttons.add(createButton("Open", e -> openFile()));
        buttons.add(createButton("Save", e -> saveFile()));
        buttons.add(createButton("Run", e -> runSource()));
        buttons.add(createButton("Help", e -> showHelp()));
        controls.add(buttons, BorderLayout.WEST);

        JPanel inputPanel = new JPanel(new BorderLayout(4, 4));
        inputPanel.add(new JLabel("Ask input:"), BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(createButton("Clear Output", e -> outputArea.setText("")), BorderLayout.EAST);
        controls.add(inputPanel, BorderLayout.CENTER);

        statusLabel = new JLabel();
        refreshAdminStatus();
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(createButton("Refresh Admin", e -> refreshAdminStatus()), BorderLayout.EAST);
        controls.add(statusPanel, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().add(controls, BorderLayout.NORTH);
        getContentPane().add(splitPane, BorderLayout.CENTER);

        editorArea.setText(STARTING_PLATE);
    }

    private JButton createButton(String label, ActionListener listener) {
        JButton button = new JButton(label);
        button.addActionListener(listener);
        return button;
    }

    private void configureEditorIndentation() {
        final int tabSize = 4;
        InputMap inputMap = editorArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = editorArea.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("TAB"), "insert-tab");
        actionMap.put("insert-tab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                char[] spaces = new char[tabSize];
                for (int i = 0; i < tabSize; i++) {
                    spaces[i] = ' ';
                }
                editorArea.replaceSelection(new String(spaces));
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("shift TAB"), "remove-tab");
        actionMap.put("remove-tab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int caret = editorArea.getCaretPosition();
                    int lineStart = Utilities.getRowStart(editorArea, caret);
                    int removeCount = 0;
                    for (int i = 0; i < tabSize && lineStart + i < caret; i++) {
                        if (editorArea.getText(lineStart + i, 1).equals(" ")) {
                            removeCount++;
                        } else {
                            break;
                        }
                    }
                    if (removeCount > 0) {
                        editorArea.replaceRange("", lineStart, lineStart + removeCount);
                    }
                } catch (BadLocationException ignored) {
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "insert-break");
        actionMap.put("insert-break", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int caret = editorArea.getCaretPosition();
                    int rowStart = Utilities.getRowStart(editorArea, caret);
                    String currentLine = editorArea.getText(rowStart, caret - rowStart);
                    String indent = currentLine.replaceAll("^(\\s*).*", "$1");
                    editorArea.replaceSelection(System.lineSeparator() + indent);
                } catch (BadLocationException ex) {
                    editorArea.replaceSelection(System.lineSeparator());
                }
            }
        });
    }

    private void newFile() {
        editorArea.setText(STARTING_PLATE);
        currentFile = null;
        setTitle("IDE GF Games");
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CRUM files", "crum", "cr", "pi", "txt"));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileInputStream inputStream = new FileInputStream(file)) {
                byte[] bytes = new byte[(int) file.length()];
                inputStream.read(bytes);
                editorArea.setText(new String(bytes, StandardCharsets.UTF_8));
                currentFile = file;
                setTitle("IDE GF Games - " + file.getName());
            } catch (Exception ex) {
                showError("Unable to open file: " + ex.getMessage());
            }
        }
    }

    private void loadExample(String filename) {
        File file = new File("examples", filename);
        if (!file.exists()) {
            file = new File(filename);
        }
        if (!file.exists()) {
            showError("Example file not found: " + file.getPath());
            return;
        }
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            inputStream.read(bytes);
            editorArea.setText(new String(bytes, StandardCharsets.UTF_8));
            currentFile = null;
            setTitle("IDE GF Games - " + filename);
        } catch (Exception ex) {
            showError("Unable to load example: " + ex.getMessage());
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("CRUM files", "crum", "cr", "pi", "txt"));
            int result = chooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            currentFile = chooser.getSelectedFile();
            if (!currentFile.getName().contains(".")) {
                currentFile = new File(currentFile.getPath() + ".crum");
            }
        }
        try (FileOutputStream outputStream = new FileOutputStream(currentFile)) {
            outputStream.write(editorArea.getText().getBytes(StandardCharsets.UTF_8));
            setTitle("IDE GF Games - " + currentFile.getName());
        } catch (Exception ex) {
            showError("Unable to save file: " + ex.getMessage());
        }
    }

    private void runSource() {
        String source = editorArea.getText();
        outputArea.setText("");
        launchDefaultWindowIfRequested(source);
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(buffer, true, "UTF-8");
            String askInput = inputField.getText();
            InputStream inputStream = new ByteArrayInputStream((askInput + "\n").getBytes(StandardCharsets.UTF_8));

            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            List<Stmt> program = parser.parseFile();

            Interpreter interpreter = new Interpreter(inputStream, printStream);
            interpreter.interpret(program);

            outputArea.setText(buffer.toString("UTF-8"));
        } catch (Exception ex) {
            outputArea.setText("Error: " + ex.getMessage());
        }
    }

    private void launchDefaultWindowIfRequested(String source) {
        String lowerSource = source.toLowerCase();
        boolean hasLibrary = lowerSource.contains("import_lib gf") || lowerSource.contains("import_lib gfgames");
        boolean hasCreateWind = lowerSource.contains("create_wind[defaultsize]");
        if (!hasLibrary || !hasCreateWind) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("GF Games Window");
            window.setSize(800, 600);
            window.setLocationRelativeTo(this);
            window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            window.setVisible(true);
        });
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showHelp() {
        String helpText = "IDE GF Games Help:\n" +
                "\n" +
                "Keywords and statements:\n" +
                "attach_variable name = [type]value\n" +
                "func.printline([stringtext]\"Hello world!\")\n" +
                "import_lib calc\n" +
                "create_calc\n" +
                "create_advancedcalc\n" +
                "ask\n" +
                "if_(name) -[type]- == <expr> -[ then -[ ... -] -]\n" +
                "repeat i = <count> times -{ ... -}\n" +
                "loop_window_while(window)created -{ ... -}\n" +
                "import_lib GF\n" +
                "import_lib GFGames\n" +
                "import_lib GKD\n" +
                "import_lib OS\n" +
                "os_createnewfile 'filename' path: c:/desktop/javafiles/\n" +
                "display[stringtext]in_wind1\"Hello\"\n" +
                "ren_wind1 to --wind2\n" +
                "create-[window]size{(<width>)}\n" +
                "create-[window]size{(<width>,<height>)}\n" +
                "Create_wind[defaultsize]\n" +
                "\n" +
                "Type annotations:\n" +
                "[stringtext], [numbertext], [dictetext], [functiontext], [window]\n" +
                "\n" +
                "Dictionary example:\n" +
                "attach_variable x = [dictetext] --{ key = [stringtext]\"value\" -}\n" +
                "\n" +
                "Examples:\n" +
                "attach_variable count = [numbertext]10\n" +
                "print count\n" +
                "ask\n" +
                "create-[window]size{(800,600)}\n" +
                "\n" +
                "Use the input field to provide text for ask statements.\n" +
                "\n" +
                "Example projects are available in the Examples menu.\n" +
                "Load Hello Project or Loop Project to get started.\n" +
                "\n" +
                "Comments:\n" +
                "> This is a multiline comment start\n" +
                "Any text here is skipped by the interpreter\n" +
                "<\n";
        JTextArea textArea = new JTextArea(helpText);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(textArea), "IDE GF Games Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshAdminStatus() {
        boolean admin = isAdministrator();
        statusLabel.setText("Administrator mode: " + (admin ? "Yes" : "No") + "    OS library requires administrator privileges.");
    }

    private boolean isAdministrator() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            return false;
        }
        try {
            Process process = new ProcessBuilder("cmd", "/c", "whoami /groups").redirectErrorStream(true).start();
            String output = new String(readAllBytes(process.getInputStream()), StandardCharsets.UTF_8);
            process.waitFor();
            return output.contains("S-1-5-32-544") || output.toLowerCase().contains("administrators");
        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
