import java.nio.file.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class FileMover {
    private Path downloadDir = Paths.get(System.getProperty("user.home"), "Downloads");
    private WatchService watchService;

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
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select directory to move file to");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int userSelection = fileChooser.showSaveDialog(null);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                Path targetDirectory = fileChooser.getSelectedFile().toPath();
                Path targetPath = targetDirectory.resolve(file.getFileName());
                try {
                    if (Files.exists(targetPath)) {
                        throw new FileAlreadyExistsException(targetPath.toString());
                    }
                    Files.move(file, targetPath);
                } catch (FileAlreadyExistsException e) {
                    JOptionPane.showMessageDialog(null, "File already exists in the destination directory: " + e.getMessage());
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "An error occurred while moving the file: " + e.getMessage());
                    e.printStackTrace();
                }
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
