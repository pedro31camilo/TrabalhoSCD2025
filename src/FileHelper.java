import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.time.LocalDateTime;

public class FileHelper
{
    /*
    Exemplos de uso
    public static void main(String[] args) {
        LocalDateTime agora = LocalDateTime.now();
        appendToFile(agora + " Operacao de Request", "dados.txt");

        LocalDateTime depois = LocalDateTime.now();
        appendToFile(depois + " Uma operação de Grant.", "dados.txt");
    }
    */

    public static boolean appendToFile(String conteudo, String nomeArquivo) {
        // Validação básica dos parâmetros
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

