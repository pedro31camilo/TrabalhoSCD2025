import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileHelper
{
    public static boolean appendToFile(String conteudo, String nomeArquivo) {
        if (conteudo == null || nomeArquivo == null || nomeArquivo.trim().isEmpty()) {
            System.err.println("Parâmetros inválidos");
            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nomeArquivo, true))) {
            writer.write(conteudo);
            writer.newLine();
            return true;
        } catch (IOException e) {
            System.err.println("Erro ao escrever no arquivo '" + nomeArquivo + "': " + e.getMessage());
            return false;
        }
    }
}

