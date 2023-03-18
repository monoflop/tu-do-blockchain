package com.philippkutsch.tuchain.dss;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Copied from https://www.baeldung.com/java-csv
 */
public class CsvHelper {
    final File output;

    public CsvHelper(@Nonnull File output) {
        this.output = output;
    }

    public void writeData(@Nonnull List<String[]> data) throws IOException {
        try (PrintWriter pw = new PrintWriter(output)) {
            data.stream()
                    .map(this::convertToCSV)
                    .forEach(pw::println);
        }
    }

    @Nonnull
    private String convertToCSV(@Nonnull String[] data) {
        return Stream.of(data)
                .map(this::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

    @Nonnull
    private String escapeSpecialCharacters(@Nonnull String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }
}
