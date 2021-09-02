import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class OnibusHttpServer {
    private static class Assento {
        int lugar;
        boolean reservado;
        String nomeDoPassageiro;
        LocalDateTime dataHora;
    }

    ServerSocket serverSocket = new ServerSocket(8080);
    ArrayList<Assento> assentos = new ArrayList<>();
    final int qtdDeAssentos = 32;
    final File logFile = new File("logs", "log.txt");

    public static void main(String[] args) throws IOException { new OnibusHttpServer(); }

    public OnibusHttpServer() throws IOException {
        for (int i = 0; i < qtdDeAssentos; i++) {
            Assento assento = new Assento();
            assento.lugar = i + 1;
            assento.reservado = false;
            assentos.add(assento);
        }

        while (true) {
            try {
                Socket socket = serverSocket.accept();
                new Thread(new Guiche(socket)).start();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private class Guiche implements Runnable {
        Socket socket;
        String status = "ok";

        private Guiche(Socket socket) throws IOException {
            this.socket = socket;
            InputStream input = socket.getInputStream();
            byte[] buffer = new byte[2048];
            int len = input.read(buffer);
            String req = new String(buffer, 0, len);
            String[] line = req.split("\n");
            String[] line0 = line[0].split(" ");

            String htmlBody =
                "<header>" +
                    "<a href='/'>Assentos</a><br>" +
                    "<a href='/reserva'>Reservar assento</a>" +
                "</header>";

            if (line0[1].equals("/")) {
                String htmlTable = "<table style='border: 1px solid black;'>";

                for (int i = 0; i < qtdDeAssentos; i += 4) {
                    htmlTable += "<tr>";

                    for (int j = 0; j < 4; j++) {
                        Assento assento = assentos.get(i + j);
                        htmlTable += "<td style='border: 1px solid black;'>Assento " + assento.lugar + "<br>";

                        if (assento.reservado) {
                            htmlTable += "Reservado<br>" + assento.nomeDoPassageiro + "<br>" +
                                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(assento.dataHora);
                        } else { htmlTable += "Vago<br>"; }

                        htmlTable += "</td>";

                        if (j == 1) { htmlTable += "<td style='padding: 30px;'></td>"; }
                    }
                    htmlTable += "</tr>";
                }
                htmlTable += "</table>";

                htmlBody += "<h1>Assentos</h1>" + htmlTable;
            } else if (line0[1].equals("/reserva")) {
                String htmlForm =
                    "<form action='/reserva' method='get'>" +
                        "<label for='nome'>Nome</label><input type='text' id='nome' name='nome'><br>" +
                        "<label for='lugar'>Lugar</label><select id='lugar' name='lugar'>";

                for (int i = 0; i < qtdDeAssentos; i++) {
                    Assento assento = assentos.get(i);

                    if (!assento.reservado) {
                        htmlForm += "<option value='" + assento.lugar + "'>Assento " + assento.lugar + "</option>";
                    }
                }

                htmlForm +=
                        "</select><br>" +
                        "<input type='submit' value='Reservar'>" +
                    "</form>";

                htmlBody += "<h1>Reservar assento</h1>" + htmlForm;
            } else {
                String[] get = line0[1].split("[?&]");

                if (get.length > 1) {
                    String[] nome = get[1].split("=");
                    String[] lugar = get[2].split("=");

                    synchronized (assentos.get(Integer.parseInt(lugar[1]) - 1)) {
                        Assento assento = assentos.get(Integer.parseInt(lugar[1]) - 1);
                        if (assento.reservado) {
                            status = "erro";
                        } else {
                            assento.reservado = true;
                            assento.nomeDoPassageiro = nome[1].replace("+"," ");
                            assento.dataHora = LocalDateTime.now();
                            status = "sucesso";
							FileWriter fileWriter = new FileWriter(logFile);
							fileWriter.append(assento.dataHora + " " + assento.lugar + " " + assento.nomeDoPassageiro + "\n");
							fileWriter.flush();
                        }
                    }
                }
            }

            if (!status.equals("ok")) {
                if (status.equals("sucesso")) { htmlBody += "<h1>Operação realizada com sucesso.</h1>"; }
                else { htmlBody += "<h1>Erro durante a operação. Por favor, tente novamente.</h1>"; }
            }

            String html =
                "<html lang='pt-br'>" +
                    "<head>" +
                        "<meta charset='utf-8'>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
                        "<title>Reservas de assento em ônibus</title>" +
                    "</head>"+
                    "<body>" + htmlBody + "</body>" +
                "</html>";

            OutputStream output = socket.getOutputStream();

            if (
                line0[1].equals("/") ||
                line0[1].equals("/reserva") ||
                line0[1].matches("^/reserva\\?nome=[0-z]+&lugar=[0-9]+$")
            ) {
                output.write((
                    "HTTP/1.1 200 OK\nContent-Type: text/html; charset=UTF-8\n\n"
                ).getBytes(StandardCharsets.UTF_8));

                output.write(html.getBytes(StandardCharsets.UTF_8));
            } else {
                if (!status.equals("ok")) {
                    output.write((
                        "HTTP/1.1 200 OK\nContent-Type: text/html; charset=UTF-8\n\n"
                    ).getBytes(StandardCharsets.UTF_8));

                    output.write(html.getBytes(StandardCharsets.UTF_8));
                    output.flush();
                } else {
                    output.write((
                        "HTTP/1.1 404 Not found\n\nError 404\nNot found"
                    ).getBytes(StandardCharsets.UTF_8));
                }
            }
            output.flush();
            socket.close();
        }

        @Override
        public void run() { }
    }
}
