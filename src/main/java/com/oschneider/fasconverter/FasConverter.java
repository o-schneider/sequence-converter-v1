package com.oschneider.fasconverter;

import io.vavr.collection.List;
import io.vavr.control.Try;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FasConverter {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Le chemin vers les fichiers n'a pas été renseigné");
            return;
        }

        FasConverter fasConverter = new FasConverter();
        fasConverter.run(args[0]);
    }

    public void run(String dirName) {
        Try.of(() -> new File(dirName))
                .filter(File::exists, () -> new IllegalStateException("Le chemin " + dirName + " n'existe pas."))
                .mapTry(directory -> Files.list(directory.toPath()))
                .map(files -> {
                    List<Path> collect = List.ofAll(files);
                    return collect
                            .map(Path::toFile)
                            .peek(f -> System.out.println(f.getAbsolutePath()))
                            .filter(f -> f.getAbsolutePath().endsWith("fasta"))
                            .map(file -> convert(file)
                                    .mapTry(lines -> Files.write(
                                            new File(file.getAbsolutePath() + ".converted").toPath(),
                                            lines.toJavaList(),
                                            StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING)));
                })
                .onFailure(e -> System.out.println(e.getMessage()));
    }

    private Try<List<String>> convert(File file) {
        System.out.println("Converti " + file.getAbsolutePath());
        return Try.of(() -> Files.readAllLines(file.toPath()))
                .map(List::ofAll)
                .map(lines -> lines.foldLeft(List.empty(), (newLines, currentLine) ->
                        currentLine.startsWith(">") ?
                                mapSequenceName(newLines, currentLine) :
                                newLines.append(currentLine)));

    }

    private List<String> mapSequenceName(List<String> newLines, String currentLine) {
        String first3Chars = currentLine.substring(1, 4);
        String chars5to8 = currentLine.substring(5, 8);

        String suffix = chars5to8.equals("110") ?
                currentLine.substring(4, 5) :
                currentLine.substring(4, 6);

        return newLines.append(">" + first3Chars + "-" + suffix);
    }


}
