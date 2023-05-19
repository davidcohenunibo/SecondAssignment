package approccio_02_virtual_threads.source_analyser;

import approccio_02_virtual_threads.boundedbuffer.Distribution;
import approccio_02_virtual_threads.utils.PerformanceUtils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;

public class AnalyserGUI {

    private final JTextField directoryField;
    private final JTextField maxFilesField;
    private final JTextField numIntervalsField;
    private final JTextField maxLengthField;
    private final JButton startButton;
    private final JButton stopButton;
    private final JTextArea maxFilesArea;
    private final JTextArea distributionArea;
    private volatile boolean isStopped;
    private DirectoryWalkerParams params;
    private CompletableFuture<Boolean> future;


    /**
     * Creation of the GUI
     */
    public AnalyserGUI() {
        JFrame frame = new JFrame("Java Walker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(800, 600));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel directoryLabel = new JLabel("Directory:");
        directoryField = new JTextField();
        directoryField.setPreferredSize(new Dimension(300, 25));
        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> chooseDirectory());

        JLabel maxFilesLabel = new JLabel("Max Files:");
        maxFilesField = new JTextField("10");
        maxFilesField.setPreferredSize(new Dimension(100, 25));

        JLabel numIntervalsLabel = new JLabel("Num Intervals:");
        numIntervalsField = new JTextField("10");
        numIntervalsField.setPreferredSize(new Dimension(100, 25));

        JLabel maxLengthLabel = new JLabel("Max Length:");
        maxLengthField = new JTextField("100");
        maxLengthField.setPreferredSize(new Dimension(100, 25));

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(directoryLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        inputPanel.add(directoryField, gbc);
        gbc.gridx = 2;
        gbc.gridy = 0;
        inputPanel.add(browseButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(maxFilesLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        inputPanel.add(maxFilesField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(numIntervalsLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        inputPanel.add(numIntervalsField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        inputPanel.add(maxLengthLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        inputPanel.add(maxLengthField, gbc);

        startButton = new JButton("Start");
        startButton.addActionListener(e -> startWalker());

        stopButton = new JButton("Stop");
        stopButton.addActionListener(e -> stopWalker());
        stopButton.setEnabled(false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        maxFilesArea = new JTextArea();
        distributionArea = new JTextArea();

        JPanel maxFilesPanel = createTextAreaPanel("Max Files:", maxFilesArea);
        JPanel distributionPanel = createTextAreaPanel("Distribution:", distributionArea);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2));
        centerPanel.add(maxFilesPanel);
        centerPanel.add(distributionPanel);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    private JPanel createTextAreaPanel(String title, JTextArea textArea) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void chooseDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a directory");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            directoryField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void startWalker() {
        String directory = directoryField.getText().trim();
        if (directory.isEmpty()) {
            showErrorDialog("Please specify a directory.");
            return;
        }

        int maxLength;
        try {
            maxLength = Integer.parseInt(maxLengthField.getText().trim());
        } catch (NumberFormatException e) {
            showErrorDialog("Invalid value for max length: " + maxLengthField.getText().trim());
            return;
        }

        int maxFiles;
        try {
            maxFiles = Integer.parseInt(maxFilesField.getText().trim());
            if(maxFiles >= maxLength) {
                JOptionPane.showMessageDialog
                        (null, "Invalid value for max file: " + maxLengthField.getText().trim()
                               +"!!\nValue need to be <= Max Files","Bad value", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            showErrorDialog("Invalid value for max files: " + maxFilesField.getText().trim());
            return;
        }

        int numIntervals;
        try {
            numIntervals = Integer.parseInt(numIntervalsField.getText().trim());
            if(numIntervals >= maxLength) {
                JOptionPane.showMessageDialog
                        (null, "Invalid value for num intervals: " + numIntervalsField.getText().trim()
                                +"!!\nValue need to be <= Max Files","Bad value", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            showErrorDialog("Invalid value for num intervals: " + numIntervalsField.getText().trim());
            return;
        }

        Path dirPath = Paths.get(directory);

        Distribution<Integer, Path> distribution = new Distribution<>();
        this.params = DirectoryWalkerParams.builder()
                                            .directory(dirPath)
                                            .maxFiles(maxFiles)
                                            .numIntervals(numIntervals)
                                            .maxLines(maxLength)
                                            .distribution(distribution)
                                            .build();
        int poolSize = PerformanceUtils.getDefaultNumThread();

        isStopped = false;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        SourceAnalyser sourceAnalyser = new SourceAnalyserImpl(this.params);
        this.future = sourceAnalyser.analyzeSources();

        ThreadFactory factory = Thread.ofVirtual().factory();
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(poolSize, factory);
        ses.scheduleAtFixedRate(() -> {
            if (!isStopped) {
                printResults();
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
        future.thenAccept(completed -> stopWalker());
    }

    private void stopWalker() {
        this.future.cancel(true);
        this.isStopped = true;
        this.startButton.setEnabled(true);
        this.stopButton.setEnabled(false);
    }

    private void printResults() {
        PrinterTask printer = new PrinterTask(this.params);
        SwingUtilities.invokeLater(() -> distributionArea.setText(printer.getDistributionString()));
        SwingUtilities.invokeLater(() -> maxFilesArea.setText(printer.getMaxFilesString()));
    }

    private void showErrorDialog(String message) {
        SwingUtilities.invokeLater(() -> {
            JLabel label = new JLabel(message);
            JOptionPane.showMessageDialog(null, label, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }
}