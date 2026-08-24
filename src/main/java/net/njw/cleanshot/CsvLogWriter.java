package net.njw.cleanshot;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

public final class CsvLogWriter {

    private static final String FILE_NAME = "cleanshot.csv";

    private static final String HEADER =
            "File,X,Y,Z,Dimension,Yaw,Pitch";

    private CsvLogWriter() {
    }

    public static synchronized void append(
            Path screenshotDirectory,
            String screenshotFileName,
            double x,
            double y,
            double z,
            String dimension,
            float yaw,
            float pitch
    ) throws IOException {

        Path csvFile = screenshotDirectory.resolve(FILE_NAME);

        boolean writeHeader =
                !Files.exists(csvFile) || Files.size(csvFile) == 0;

        try (BufferedWriter writer = Files.newBufferedWriter(
                csvFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {

            if (writeHeader) {
                writer.write(HEADER);
                writer.newLine();
            }

            String row = String.format(
                    Locale.ROOT,
                    "%s,%.6f,%.6f,%.6f,%s,%.2f,%.2f",
                    escape(screenshotFileName),
                    x,
                    y,
                    z,
                    escape(dimension),
                    yaw,
                    pitch
            );

            writer.write(row);
            writer.newLine();
        }
    }

    private static String escape(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}