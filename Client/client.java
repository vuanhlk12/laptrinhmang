import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class client {
    private static String name;

    private static String clientFolder = "D:/BuiVuAnh_11/Client/ClientFile";
    private static int bufferSize = 1024;

    public static void overrideLine() {
        // int count = 1;
        // System.out.print("> "+String.format("\033[%dA", count)); // Move up
        // System.out.print("> "+"\033[2K");
        // System.out.print("> "+"> ");
    }

    public static void receiveFile(String msg, DataInputStream dataInputStream) throws IOException {
        String fileName = "";
        String[] arrCommand = msg.split(": ", 2);
        if (arrCommand.length < 2 || arrCommand[1].equals("")) {
            System.out.println("> Error command!");
            return;
        } else {
            fileName = arrCommand[1];
        }

        long fileSize = dataInputStream.readLong();
        if (fileSize == -1L) {
            System.out.println(" Error from Server!");
        } else {
            FileOutputStream fileOutputStream = new FileOutputStream(clientFolder + "/" + fileName);
            byte[] receiveBytes = new byte[bufferSize];
            int n;
            System.out.println(" Receiving " + fileSize + " Bytes...");
            for (long doneSize = 0L; doneSize < fileSize; doneSize += (long) n) {
                n = dataInputStream.read(receiveBytes);
                fileOutputStream.write(receiveBytes, 0, n);
            }
            System.out.println(" Received!");
            fileOutputStream.close();
            return;
        }
    }

    public static void receiveHandler(Socket soc) {
        DataInputStream dataInputStream = null;
        try {
            dataInputStream = new DataInputStream(soc.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                Scanner in = new Scanner(soc.getInputStream());
                while (in.hasNextLine()) {
                    String msg = in.nextLine();
                    if (msg.startsWith("FILE OK")) {
                        receiveFile(msg, dataInputStream);
                    } else {
                        System.out.println("> " + msg);
                    }
                    overrideLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void showLocalList() {
        File sharedFolder = new File(clientFolder);
        File[] files = sharedFolder.listFiles();
        int numOfFiles = files.length;
        System.out.println("> " + name + " have " + numOfFiles + " files:");
        for (int i = 0; i < numOfFiles; i++) {
            File file = files[i];
            System.out.println("> " + (i + 1) + ". " + file.getName());
        }
    }

    public static void upFile(Socket soc, String msg, PrintWriter out, DataOutputStream dataOutputStream) throws IOException {
        //dataOutputStream = new DataOutputStream(soc.getOutputStream());
        String fileName = "";
        String[] arrCommand = msg.split(" ", 2);
        if (arrCommand.length < 2 || arrCommand[1].equals("")) {
            System.out.println("> Error command!");
            return;
        } else {
            fileName = arrCommand[1];
        }

        System.out.println("Server needs file: " + fileName);
        File file = new File(clientFolder + "/" + fileName);
        if (!file.exists()) {
            System.out.println(" File not found!");
            out = new PrintWriter(soc.getOutputStream(), true);
            out.println("FILE NOT FOUND: " + fileName);//send file not found
        } else {
            out = new PrintWriter(soc.getOutputStream(), true);
            out.println("FILE OK: " + fileName);//send file ok

            FileInputStream fileInputStream = new FileInputStream(file);
            long fileSize = file.length();
            dataOutputStream.writeLong(fileSize);
            //out = new PrintWriter(soc.getOutputStream(), true);
            //out.println(fileSize);//send file not found
            byte[] sendBytes = new byte[bufferSize];
            int n;
            System.out.println(" Sending " + fileSize + " Bytes...");
            while ((n = fileInputStream.read(sendBytes)) != -1) {
                dataOutputStream.write(sendBytes, 0, n);
            }
            System.out.println(" Sent!");

            //dataOutputStream.close();
            fileInputStream.close();
        }
    }

    public static void sendHandler(Socket soc) {
        DataOutputStream dataOutputStream = null;
        try {
            dataOutputStream = new DataOutputStream(soc.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            overrideLine();
            Scanner sc = new Scanner(System.in);
            String msg = sc.nextLine();

            try {
                OutputStream outputStream = soc.getOutputStream();
                PrintWriter out = new PrintWriter(outputStream, true);
                out.println(msg);

                if (msg.equals("/locallist")) {
                    showLocalList();
                }

                if (msg.startsWith("/up")) {
                    upFile(soc, msg, out, dataOutputStream);
                }

                if (msg.equals("/exit")) {
                    System.out.print("Left chatroom...");
                    System.exit(1);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static boolean isAlpha(String name) {
        return name.matches("[a-zA-Z]+");
    }

    public static void main(String[] args) {
        try {
            //final Socket soc = new Socket("192.168.0.103", 9000);
            final Socket soc = new Socket("localhost", 9000);

            // validate name
            boolean isNameValid = false;
            while (!isNameValid) {
                System.out.print("> " + "Enter your name: ");
                Scanner scanner = new Scanner(System.in);
                name = scanner.nextLine().toUpperCase();
                if (isAlpha(name)) {
                    isNameValid = true;
                } else {
                    System.out.println("> " + "Your name can only contain alphabet, try again...");
                    continue;
                }
                System.out.println("> " + "Your name is: " + name);
            }

            // Send name
            try {
                OutputStream outputNameStream = soc.getOutputStream();
                PrintWriter outName = new PrintWriter(outputNameStream, true);
                outName.println(name);
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Connected to chatroom...");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    receiveHandler(soc);
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    sendHandler(soc);
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}