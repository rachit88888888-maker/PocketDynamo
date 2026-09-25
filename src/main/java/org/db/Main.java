package org.db;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.db.Command.*;


public class Main {




    private static BufferedWriter clientWriter;
    private static BufferedReader clientReader;

    private static BufferedReader fileReader;
    private static BufferedWriter fileWriter;
    static void main() throws IOException {
        Path walPath = Path.of("pocketdynamo.wal");

        if (Files.notExists(walPath)) {
            Files.createFile(walPath);
        }

        fileWriter = Files.newBufferedWriter(
                walPath,
                StandardOpenOption.APPEND
        );
        fileReader = Files.newBufferedReader(walPath);


        ConcurrentHashMap<String, String> map = new ConcurrentHashMap();

        System.out.println("DB Starting up ....");
        performStartup( map);




        System.out.println("DB Started");

        try {
            handleOperations(map);
        }catch (Exception e){
            System.out.println("Error handling operations");
        }finally {
            System.out.println("Flushing and closing");
            fileWriter.flush();
            fileWriter.close();
        }
    }

    private static void handleOperations(ConcurrentHashMap<String, String> map) throws IOException {

        try (ServerSocket serverSocket= new ServerSocket(2001)){
            while (true) {
                System.out.println("Waiting for connection");
                Socket socket = serverSocket.accept();
                System.out.println("Connected to "+ socket.getInetAddress());
                Thread t = new Thread(()->{
                    try{
                        BufferedReader bufferedReader = new BufferedReader(new
                                InputStreamReader(socket.getInputStream()));
                        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        while (true) {
                            try{
                                String commandLine = bufferedReader.readLine();

                                if(commandLine == null){
                                    break;
                                }
                                String response = executeCommand(commandLine, map);


                                bufferedWriter.write(response);
                                bufferedWriter.newLine();
                                bufferedWriter.flush();

                                if (commandLine.startsWith("EXIT")) {
                                    break;
                                }
                            }catch (IllegalArgumentException ie) {
                                sendToClient(bufferedWriter, ie.getMessage());
                            }catch (Exception e){
                                e.printStackTrace();
                                System.out.println("Error processing command");

                                sendToClient(bufferedWriter, "ERROR PROCESSING");
                            }
                        }
                    }catch (Exception e) {
                        e.printStackTrace();
                    }finally {
                        System.out.println("flushing file");
                        try {
                            fileWriter.flush();
                        }catch (IOException e){
                            System.out.println("Error flushing file");
                        }
                        System.out.println("Closing connection" + socket.getInetAddress());
                        try {
                            socket.close();

                        } catch (IOException e) {
                            System.out.println("Error closing socket");
                        }
                    }
                });
                t.start();

            }




        }
    }

    private static void sendToClient( BufferedWriter bufferedWriter, String message) throws IOException {

        bufferedWriter.write(message);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    private static void performStartup(ConcurrentHashMap<String, String> map) throws IOException {


        String line;
        while((line = fileReader.readLine()) != null){
            loadMap(line, map);
        }
    }
    private static void loadMap(String line, ConcurrentHashMap<String, String> map) {
        Operation op = Operation.parse(line);
        if(SET.equals(op.getCommand())){
            map.put(op.getKey(), op.getValue());
        }else{
            map.remove(op.getKey());
        }
    }



    private static String executeCommand(String command, ConcurrentHashMap<String, String> map) throws IOException {
        Operation op = Operation.parse(command);

        Command operation = op.getCommand();

        String response = "OK";
        switch (operation) {

                case EXIT:
                    break;
                case SET:


                    map.put(op.getKey(), op.getValue());
                    fileWriter.write(op.getLine());
                    fileWriter.newLine();
                    fileWriter.flush();
                    break;
                case GET:
                    response =  map.getOrDefault(op.getKey(), "");
                    break;
                case DEL:
                    map.remove(op.getKey());
                    fileWriter.write(op.getLine());
                    fileWriter.newLine();
                    fileWriter.flush();
                    break;
            }
            return response;

    }


}
