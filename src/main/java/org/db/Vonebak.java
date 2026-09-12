package org.db;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;


public class Main {
    static void main() throws IOException {

        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

        try (ServerSocket serverSocket= new ServerSocket(1997)){
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
                                String line = bufferedReader.readLine();
                                if(line == null){
                                    break;
                                }
                                try{
                                String[] commands = line.split(" ");

                                String command = commands[0];
                                String toReturn = "OK";
                                boolean isValid = true;
                                switch (command) {
                                    case "SET":
                                        if(commands.length != 3){
                                            isValid = false;

                                            break;
                                        }
                                        map.put(commands[1], commands[2]);
                                        break;
                                    case "GET":
                                        if(commands.length != 2){
                                            isValid = false;
                                            break;
                                        }
                                        toReturn = map.getOrDefault(commands[1], "");
                                        break;
                                    case "DEL":
                                        if(commands.length != 2){
                                            isValid = false;
                                            break;
                                        }
                                        map.remove(commands[1]);
                                        break;
                                    case "EXIT":
                                        toReturn = "BYE.....";
                                        break;
                                    default:
                                        toReturn = "Unknown command";
                                }

                                if(!isValid){
                                    toReturn = "Wrong number of arguments";
                                }


                                bufferedWriter.write(toReturn);
                                bufferedWriter.newLine();
                                bufferedWriter.flush();

                                if (command.equals("EXIT")) {
                                    socket.close();
                                    break;
                                }
                                }catch (Exception e){
                                    System.out.println("Error processing command");
                                    bufferedWriter.write("ERROR PROCESSING");
                                    bufferedWriter.newLine();
                                    bufferedWriter.flush();
                                }
                            }
                        }catch (Exception e) {
                            e.printStackTrace();
                        }finally {
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


}
