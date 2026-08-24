package net.njw.cleanshot;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.CRC32;

public final class PngMetadataWriter {

    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89,
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A,
            0x0A
    };

    private PngMetadataWriter() {
    }

    public static void addMetadata(
            File file,
            Map<String, String> metadata
    ) throws IOException {

        byte[] png = Files.readAllBytes(file.toPath());

        validatePng(png);

        ByteArrayOutputStream output =
                new ByteArrayOutputStream(png.length + 1024);

        output.write(PNG_SIGNATURE);

        int offset = PNG_SIGNATURE.length;
        boolean metadataWritten = false;

        while (offset < png.length) {
            if (offset + 12 > png.length) {
                throw new IOException("Invalid PNG chunk.");
            }

            int dataLength = readInt(png, offset);

            if (dataLength < 0) {
                throw new IOException("Invalid PNG chunk length.");
            }

            int chunkLength = 12 + dataLength;

            if (offset + chunkLength > png.length) {
                throw new IOException("Invalid PNG chunk size.");
            }

            String chunkType = new String(
                    png,
                    offset + 4,
                    4,
                    StandardCharsets.US_ASCII
            );

            // IEND 직전에 CleanShot metadata 삽입
            if ("IEND".equals(chunkType) && !metadataWritten) {

                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    writeTextChunk(
                            output,
                            entry.getKey(),
                            entry.getValue()
                    );
                }

                metadataWritten = true;
            }

            output.write(
                    png,
                    offset,
                    chunkLength
            );

            offset += chunkLength;
        }

        if (!metadataWritten) {
            throw new IOException(
                    "PNG does not contain an IEND chunk."
            );
        }

        replaceFile(
                file.toPath(),
                output.toByteArray()
        );
    }

    private static void writeTextChunk(
            ByteArrayOutputStream output,
            String keyword,
            String value
    ) throws IOException {

        byte[] keywordBytes =
                keyword.getBytes(StandardCharsets.ISO_8859_1);

        byte[] valueBytes =
                value.getBytes(StandardCharsets.ISO_8859_1);

        if (keywordBytes.length < 1 || keywordBytes.length > 79) {
            throw new IOException(
                    "PNG text keyword must be between 1 and 79 bytes."
            );
        }

        ByteArrayOutputStream data =
                new ByteArrayOutputStream();

        data.write(keywordBytes);
        data.write(0);
        data.write(valueBytes);

        byte[] dataBytes = data.toByteArray();

        byte[] type =
                "tEXt".getBytes(StandardCharsets.US_ASCII);

        writeInt(output, dataBytes.length);

        output.write(type);
        output.write(dataBytes);

        CRC32 crc = new CRC32();

        crc.update(type);
        crc.update(dataBytes);

        writeInt(
                output,
                (int) crc.getValue()
        );
    }

    private static void validatePng(byte[] png)
            throws IOException {

        if (png.length < PNG_SIGNATURE.length) {
            throw new IOException("Invalid PNG file.");
        }

        byte[] signature = Arrays.copyOfRange(
                png,
                0,
                PNG_SIGNATURE.length
        );

        if (!Arrays.equals(signature, PNG_SIGNATURE)) {
            throw new IOException("Invalid PNG signature.");
        }
    }

    private static int readInt(
            byte[] data,
            int offset
    ) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private static void writeInt(
            ByteArrayOutputStream output,
            int value
    ) {
        output.write((value >>> 24) & 0xFF);
        output.write((value >>> 16) & 0xFF);
        output.write((value >>> 8) & 0xFF);
        output.write(value & 0xFF);
    }

    private static void replaceFile(
            Path target,
            byte[] data
    ) throws IOException {

        Path directory = target.getParent();

        Path temporaryFile = Files.createTempFile(
                directory,
                ".cleanshot-",
                ".png"
        );

        try {
            Files.write(temporaryFile, data);

            try {
                Files.move(
                        temporaryFile,
                        target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(
                        temporaryFile,
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
}