import java.nio.file.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Paths;
import java.awt.List;
import java.util.ArrayList;

public class FileMover {
    private Path downloadDir = Paths.get(System.getProperty("user.home"), "Downloads");
    private WatchService watchService;
    private ArrayList<Path> presetPaths;
    private ImageIcon icon;

    public FileMover() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        downloadDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        presetPaths = readPresetPathList();
        try (InputStream is = getClass().getResourceAsStream("/movefile_logo.png")) {
            if (is != null) {
                icon = new ImageIcon(ImageIO.read(is));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            ArrayList<Object> options = generateOptions();

            String fileName = file.getFileName().toString();

            Integer n = JOptionPane.showOptionDialog(null,
                    "「 " + fileName + "」 の移動先のディレクトリを選択してください",
                    "ファイルの移動",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    icon,
                    options.toArray(),
                    options.get(0));

            Path targetDirectory;
            if (n < options.size()-1) {
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
                JOptionPane.showMessageDialog(null, "ファイルの移動が完了しました. 「 " + file.getFileName() + " 」 -> " + targetDirectory.resolve(file.getFileName()));
                openFinder(targetDirectory);
            } catch (FileAlreadyExistsException e) {
                FileAlreadyExistsDialog(file, targetDirectory);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "An error occurred while moving the file: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public ArrayList<Object> generateOptions() {
        ArrayList<Object> options = new ArrayList<>();
        for (Path aPath : presetPaths) {
            options.add(String.valueOf(aPath.getFileName()));
        }
        options.add(String.valueOf("その他"));

        return options;
    }

    public ArrayList<Path> readPresetPathList() {
        ArrayList<Path> paths = new ArrayList<>();
        try (InputStream is = FileMover.class.getResourceAsStream("/paths.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is))){
            String line;
            while((line = reader.readLine()) != null) {
                paths.add(Paths.get(line));
            }          
            

        } catch (IOException e) {
            e.printStackTrace();
        }
        return paths;
    }

    public void FileAlreadyExistsDialog(Path file, Path targetDirectory) {
        Object[] options = {"名前を変更して移動", "移動先のファイルを削除", "他のディレクトリに移動"};
    int n = JOptionPane.showOptionDialog(null,
            "ディレクトリに同名のファイルがすでに存在しています。3つの選択肢から選んでください。",
            "同名ファイルの重複",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);

    switch (n) {
        case 0: // Rename and move
            String newFileName = JOptionPane.showInputDialog(null, "Enter new filename:", file.getFileName().toString());
            if (newFileName == null || newFileName.trim().isEmpty()) {
            // User did not enter a valid filename, do not proceed
            JOptionPane.showMessageDialog(null, "無効なファイル名です。操作はキャンセルされました。");
            moveFile(file);
            }
            Path renamedFile = targetDirectory.resolve(newFileName);
            try {
                Files.move(file, renamedFile);
                JOptionPane.showMessageDialog(null, "ファイルの移動とリネームが正常に行われました。");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "An error occurred while renaming and moving the file: " + ex.getMessage());
                ex.printStackTrace();
            }
            break;
        case 1: // Delete existing and move
            try {
                Files.delete(targetDirectory.resolve(file.getFileName()));
                Files.move(file, targetDirectory.resolve(file.getFileName()));
                JOptionPane.showMessageDialog(null, "既存のファイルが削除され、新しいファイルが正常に移動しました。");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "An error occurred while deleting the existing file and moving the new file: " + ex.getMessage());
                ex.printStackTrace();
            }
            break;
        case 2: // Move to another directory
            moveFile(file); // Recursively call the moveFile method
            break;
        default:
            return;
        }
    
    }

    public void openFinder(Path targetDirectory) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            JOptionPane.showMessageDialog(null, "The current platform does not support opening directories.");
        } else {
            try {
                Desktop.getDesktop().open(targetDirectory.toFile());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "An error occurred while opening the directory: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        try {
            Desktop.getDesktop().open(targetDirectory.toFile());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "An error occurred while opening the directory: " + e.getMessage());
            e.printStackTrace();
        }
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
