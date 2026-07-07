package moira.util.docker;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public class FileContainerStream implements ContainerStream {
    private final OutputStream writer;

    public FileContainerStream(final Path path) {
        writer = getOutputStream(path);
    }

    private static OutputStream getOutputStream(final Path path) {
        try {
            return new FileOutputStream(path.toFile());
        } catch (final IOException e) {
            return new OutputStream() {
                @Override
                public void write(final int b) {}
            };
        }
    }

    public void append(final byte[] data) {
        try {
            writer.write(data);
        } catch (final IOException e) {
        }
    }
}

