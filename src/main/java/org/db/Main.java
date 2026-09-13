package org.db;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.db.Main.Command.*;


public class Main {
    public enum Command {
        SET("Store a value"),
        GET("Retrieve a value"),
        DEL("Delete a value"),
        EXIT("Gracefully shutdown");

        private final String description;

        Command(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private static BufferedWriter clientWriter;
    private static BufferedReader clientReader;

    private static BufferedReader fileReader;
    private static BufferedWriter fileWriter;
    static void main() throws IOException {
        Path walPath = Path.of("pocketdynamo.wal");

        if (Files.notExists(walPath)) {
            try (ObjectOutputStream oos =
                         new ObjectOutputStream(Files.newOutputStream(walPath))) {
                oos.writeObject(new HashMap<String, String>());
            }
        }

        ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(walPath));
        ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(walPath));
        fileWriter = new BufferedWriter(new OutputStreamWriter(oos));
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap();

        System.out.println("DB Starting up ....");
        performStartup(ois, map);




        System.out.println("DB Started");

        try {
            handleOperations(map);
        }catch (Exception e){
            System.out.println("Error handling operations");
        }finally {
            System.out.println("Flushing and closing");
            oos.flush();
            oos.close();
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

    private static void performStartup(ObjectInputStream ois, ConcurrentHashMap<String, String> map) throws IOException {

        fileReader = new BufferedReader(new InputStreamReader(ois));
        String line;
        while((line = fileReader.readLine()) != null){
            loadMap(line, map);
        }
    }
    private static void loadMap(String line, ConcurrentHashMap<String, String> map) {
        Command operation = Command.valueOf(line.split(" ")[0]);
        if(SET.equals(operation)){
            map.put(line.split(" ")[1], line.split(" ")[2]);
        }else{
            map.remove(line.split(" ")[1]);
        }
    }

    private static void isValidCommand(String command) {
        boolean isValid = true;

        Command operation = null;
        try{
             operation = Command.valueOf(command.split(" ")[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid command");
        }
        String[] args = command.split(" ");




        if(SET.equals(operation)){
            isValid =  args.length == 3;
        }
        if(GET.equals(operation) || DEL.equals(operation)){
            isValid =  args.length == 2;
        }


        if(!isValid){
            throw new IllegalArgumentException("Invalid command / Wrong number of arguments");
        }
    }

    private static String executeCommand(String command, ConcurrentHashMap<String, String> map) throws IOException {
        isValidCommand(command);
        Command operation = Command.valueOf(command.split(" ")[0]);
        String[] args = command.split(" ");

        String response = "OK";
        switch (operation) {

                case EXIT:
                    break;
                case SET:

                    String value = args[2];
                    map.put(args[1], value);
                    fileWriter.write(command);
                    break;
                case GET:
                    response =  map.getOrDefault(args[1], "");
                    break;
                case DEL:
                    map.remove(args[1]);
                    fileWriter.write(command);
                    break;
            }
            return response;

    }


}
