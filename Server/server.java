import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class server {

    private static String message = "";
    //private static String serverFolder = "Server/ServerFile";
    private static String serverFolder = "C:\\Users\\vuanh\\OneDrive\\Máy tính\\laptrinhmang\\chatroom\\Server\\ServerFile";

    private static int bufferSize = 1024;

    public static class user {
        public String name;
        public int port;
        public int id;
        public Socket soc;

        public user(String _name, int _port, Socket _soc) {
            this.name = _name;
            this.port = _port;
            this.soc = _soc;
        }
    }

    public static int handlerCommand(String userName, List<user> listUser, Socket soc, String command)
            throws IOException, InterruptedException {
        int removeFlag = 0;
        if (command.equals("/show")) {
            sendOnline(listUser, soc);
        } else if (command.startsWith("/chat")) {
            String name = "";
            String[] arrCommand = command.split(" ", 3);
            name = arrCommand[1].toUpperCase();
            message = "<private> " + arrCommand[2];
            sendToName(userName, listUser, name);
        } else if (command.equals("/exit")) {
            RemoveUser(userName, listUser, soc);
            removeFlag = 1;
        } else if (command.equals("/list")) {
            SendListFile(soc);
        } else if (command.startsWith("/down")) {
            String[] arrCommand = command.split(" ", 2);
            if (arrCommand.length < 2 || arrCommand[1].equals("")) {
                System.out.println("> Error command!");
            } else {
                String fileName = arrCommand[1];
                SendFile(soc, fileName, false);
            }
        } else if (command.startsWith("FILE OK")) {
            ReceiveFile(soc, command);
        } else if (command.startsWith("/up")) {
            //do nothing
        } else if (command.startsWith("/sendfile")) {
            //do nothing
        } else if (command.startsWith("START SEND FILE")) {
            String[] arrCommand = command.split(": ", 3);
            if (arrCommand.length < 3 || arrCommand[1].equals("") || arrCommand[2].equals("")) {
                System.out.println("> Error command!");
            } else {
                String name = arrCommand[1].toUpperCase();
                String fileName = arrCommand[2];
                SendFileTo(userName, listUser, name, fileName);
            }
        } else if (!command.startsWith("/")) {
            sendAll(userName, listUser, soc);
        }
        return removeFlag;
    }

    private static void SendFileTo(String userName, List<user> listUser, String name, String fileName) throws IOException, InterruptedException {

        for (user _user : listUser) {
            Socket s = _user.soc;
            if (s == null)
                listUser.remove(_user);
            else if (_user.name.equals(name)) {
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                out.println("[" + userName + "] -> Sending file " + fileName);
                message = "Send " + fileName + " to " + name;
                sendToName("Notification", listUser, userName);
                SendFile(_user.soc, fileName, true);
                return;
            }
        }

        if (!userName.equals("Notification")) {
            message = "User " + name + " not found";
            sendToName("Notification", listUser, userName);
        }
    }

    public static synchronized void ReceiveFile(Socket soc, String msg) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(soc.getInputStream());

        String fileName = "";
        String[] arrCommand = msg.split(": ", 2);
        if (arrCommand.length < 2 || arrCommand[1].equals("")) {
            System.out.println("> Error command!");
            return;
        } else {
            fileName = arrCommand[1];
        }
        long fileSize = dataInputStream.readLong();
        //long fileSize = Long.parseLong(in.nextLine());

        FileOutputStream fileOutputStream = new FileOutputStream(serverFolder + "/" + fileName);
        byte[] receiveBytes = new byte[bufferSize];
        int n;
        System.out.println(" Receiving " + fileSize + " Bytes...");
        for (long doneSize = 0L; doneSize < fileSize; doneSize += (long) n) {
            n = dataInputStream.read(receiveBytes);
            fileOutputStream.write(receiveBytes, 0, n);
        }
        System.out.println(" Received!");
        fileOutputStream.close();
        //dataInputStream.close();


    }

    public static void SendFile(Socket soc, String fileName, boolean delete) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(soc.getOutputStream());

        System.out.println("Client needs file: " + fileName);
        File file = new File(serverFolder + "/" + fileName);
        if (!file.exists()) {
            System.out.println(" File not found!");
            PrintWriter out = new PrintWriter(soc.getOutputStream(), true);
            out.println("FILE NOT FOUND: " + fileName);//send file not found
        } else {
            PrintWriter out = new PrintWriter(soc.getOutputStream(), true);
            out.println("FILE OK: " + fileName);//send file ok

            FileInputStream fileInputStream = new FileInputStream(file);
            long fileSize = file.length();
            dataOutputStream.writeLong(fileSize);
            byte[] sendBytes = new byte[bufferSize];
            int n;
            System.out.println(" Sending " + fileSize + " Bytes...");
            while ((n = fileInputStream.read(sendBytes)) != -1) {
                dataOutputStream.write(sendBytes, 0, n);
            }
            System.out.println(" Sent!");
            fileInputStream.close();
        }

        if(delete){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    file.delete();
                }
            }).start();
        }
    }

    public static void SendListFile(Socket soc) throws IOException {
        File sharedFolder = new File(serverFolder);
        File[] files = sharedFolder.listFiles();
        int numOfFiles = files.length;
        PrintWriter outNumberFile = new PrintWriter(soc.getOutputStream(), true);
        outNumberFile.println("Server have " + numOfFiles + " files:");
        for (int i = 0; i < numOfFiles; i++) {
            File file = files[i];
            PrintWriter out = new PrintWriter(soc.getOutputStream(), true);
            out.println((i + 1) + ". " + file.getName());
        }
    }

    public static void RemoveUser(String userName, List<user> listUser, Socket soc) throws IOException {
        message = "-> left chatroom...";
        sendAll(userName, listUser, soc);
        System.out.println("> " + userName + " left chatroom...");
        Iterator<user> it = listUser.iterator();

        for (Iterator<user> iter = it; iter.hasNext(); ) {
            user _user = iter.next();
            Socket s = _user.soc;
            try {
                if (s == null)
                    iter.remove();
                else if (_user.name.equals(userName)) {
                    iter.remove();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void sendOnline(List<user> listUser, Socket soc) throws IOException {
        for (user _user : listUser) {
            Socket s = _user.soc;
            String online = _user.name + " online, port: " + _user.soc.getPort();
            PrintWriter out = new PrintWriter(soc.getOutputStream(), true);
            out.println(online);
        }
    }

    private static void sendAll(String userName, List<user> listUser, Socket soc) throws IOException {
        for (user _user : listUser) {
            Socket s = _user.soc;
            if (s == null)
                listUser.remove(_user);
            else if (!s.equals(soc)) {
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                out.println("[" + userName + "]: " + message);
            }
        }
    }

    private static void sendToName(String userName, List<user> listUser, String name) throws IOException {

        for (user _user : listUser) {
            Socket s = _user.soc;
            if (s == null)
                listUser.remove(_user);
            else if (_user.name.equals(name)) {
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                out.println("[" + userName + "]: " + message);
                message = "Send message to " + name;
                sendToName("Notification", listUser, userName);
                return;
            }
        }

        if (!userName.equals("Notification")) {
            message = "User " + name + " not found";
            sendToName("Notification", listUser, userName);
        }
    }

    public static void main(String[] args) {
        try {
            ServerSocket svr = new ServerSocket(9000);
            System.out.println("> " + "Chatroom server starting...");
            ArrayList<user> arrListUser = new ArrayList<user>();

            while (true) {
                Socket soc = svr.accept();

                String _userName = "%noname%";
                String userName = _userName;

                new Thread(new Runnable() {
                    List<user> listUser = Collections.synchronizedList(arrListUser);
                    String finalUserName = userName;

                    @Override
                    public void run() {
                        try {
                            try {
                                Scanner inName = new Scanner(soc.getInputStream());
                                if (inName.hasNextLine()) {
                                    finalUserName = inName.nextLine();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            int port = soc.getPort();
                            listUser.add(new user(finalUserName, port, soc));
                            message = "Client connected, IP: " + soc.getInetAddress() + "; port: " + soc.getPort();
                            System.out.println("> " + "Received -> " + "[" + finalUserName + "]: " + message);

                            // Send connected
                            try {
                                sendAll(finalUserName, listUser, soc);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Scanner in = new Scanner(soc.getInputStream());
                            while (in.hasNextLine()) {
                                message = in.nextLine();
                                System.out.println("> " + "Received -> " + "[" + finalUserName + "]: " + message);
                                int flag = handlerCommand(finalUserName, listUser, soc, message);
                                if (flag == 1) {
                                    break;// đảm bảo ko có luồng zombie
                                }
                            }
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}