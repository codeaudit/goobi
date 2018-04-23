package de.sub.goobi.helper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.List;

public interface StorageProviderInterface {

    public Integer getNumberOfFiles(Path inDir);

    public Integer getNumberOfPaths(Path inDir);

    public Integer getNumberOfFiles(String inDir);

    public Integer getNumberOfFiles(Path dir, final String suffix);

    public List<Path> listFiles(String folder);

    public List<Path> listFiles(String folder, DirectoryStream.Filter<Path> filter);

    public List<String> list(String folder);

    public List<String> list(String folder, DirectoryStream.Filter<Path> filter);

    public void copyDirectory(final Path source, final Path target) throws IOException;

    public Path renameTo(Path oldName, String newNameString) throws IOException;

    public void copyFile(Path srcFile, Path destFile) throws IOException;

    public Long createChecksum(Path file) throws IOException;

    public Long start(Path srcFile, Path destFile) throws IOException;

    public long checksumMappedFile(String filepath) throws IOException;

    public boolean deleteDir(Path dir);

    public boolean deleteInDir(Path dir);

    public boolean deleteDataInDir(Path dir);
    
    public boolean isFileExists(Path path);
    
    public boolean isDirectory(Path path);
}
