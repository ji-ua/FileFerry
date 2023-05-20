import java.nio.file.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;

public class FileMover {
    private Path downloadDir = Paths.get(System.getProperty("user.home"), "Downloads");
    private WatchService watchService;
    private ArrayList<Path> presetPaths;

    public FileMover() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        downloadDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
    }

    public void startListening() {
        while (true) {
            try {
                WatchKey key = watchService.take(); // Blocks until a file is created in the download directory
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path filename = (Path) event.context();
                    moveFile(downloadDir.resolve(filename));
                }
                key.reset();
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void moveFile(Path file) {
        SwingUtilities.invokeLater(() -> {
            ArrayList<Object> options = new ArrayList<>();
            for (Path aPath : presetPaths) {
                options.add(String.valueOf(aPath));
            }
            options.add(String.valueOf("その他"));

            Integer n = JOptionPane.showOptionDialog(null,
                    "ファイルの移動先のディレクトリを選択してください",
                    "ファイルの移動",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options.toArray(),
                    options.get(0));

            Path targetDirectory;
            if (n < options.size()) {
                targetDirectory = presetPaths.get(n);
            } else {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int userSelection = fileChooser.showSaveDialog(null);
                if (userSelection != JFileChooser.APPROVE_OPTION) return;
                targetDirectory = fileChooser.getSelectedFile().toPath();
            }
            try {
                Files.move(file, targetDirectory.resolve(file.getFileName()));
            } catch (FileAlreadyExistsException e) {
                JOptionPane.showMessageDialog(null, "保存先ディレクトリに同じ名前のファイルが既に存在します: " + e.getMessage());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "An error occurred while moving the file: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        try {
            new FileMover().startListening();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred while setting up the file watcher: " + e.getMessage());
        }
    }
}
