package ch.admin.bit.jeap.messaging.annotations.processor.util;

import org.reflections.vfs.Vfs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CustomUrlType implements Vfs.UrlType {
    @Override
    public boolean matches(URL url) {
        return url.getProtocol().equals("file") && url.getPath().endsWith(".jar");
    }

    @Override
    public Vfs.Dir createDir(URL url) {
        try {
            return new ZipDir(new ZipFile(new File(url.getPath())));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create ZipDir", e);
        }
    }

    public static class ZipDir implements Vfs.Dir {
        private final ZipFile zipFile;

        public ZipDir(ZipFile zipFile) {
            this.zipFile = zipFile;
        }

        @Override
        public String getPath() {
            return zipFile.getName();
        }

        @Override
        public Iterable<Vfs.File> getFiles() {
            return () -> {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                return new Iterator<Vfs.File>() {
                    @Override
                    public boolean hasNext() {
                        return entries.hasMoreElements();
                    }

                    @Override
                    public Vfs.File next() {
                        ZipEntry entry = entries.nextElement();
                        return new ZipFileVfsFile(entry, zipFile);
                    }
                };
            };
        }

        @Override
        public void close() {
            try {
                zipFile.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to close ZipFile", e);
            }
        }
    }

    public static class ZipFileVfsFile implements Vfs.File {
        private final ZipEntry entry;
        private final ZipFile zipFile;

        public ZipFileVfsFile(ZipEntry entry, ZipFile zipFile) {
            this.entry = entry;
            this.zipFile = zipFile;
        }

        @Override
        public String getName() {
            return entry.getName();
        }

        @Override
        public String getRelativePath() {
            return entry.getName();
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return zipFile.getInputStream(entry);
        }
    }
}
