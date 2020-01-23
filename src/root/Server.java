package root;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message) {
        for (Map.Entry<String, Connection> con : connectionMap.entrySet()) {
            try {
                con.getValue().send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Error sending message!");
            }
        }
    }

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Enter port:");
        try (ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt())) {
            System.out.println("Server is running...");
            while (true) {
                new Handler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler (Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            Message msg;
            while (true) {
            connection.send(new Message(MessageType.NAME_REQUEST));
            msg = connection.receive();
            if ((msg.getType() == MessageType.USER_NAME) && (!msg.getData().equals("")) && (!connectionMap.containsKey(msg.getData()))) {
                connectionMap.put(msg.getData(), connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return msg.getData();
                }
                else return serverHandshake(connection);
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (Map.Entry<String, Connection> con : connectionMap.entrySet()) {
                if (!con.getKey().equals(userName)) connection.send(new Message(MessageType.USER_ADDED, con.getKey()));
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message msg = connection.receive();
                if (msg.getType() == MessageType.TEXT) sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + msg.getData()));
                else ConsoleHelper.writeMessage("Error!");
            }
        }

        public void run() {
            String userName = null;
            ConsoleHelper.writeMessage("New connection established with remote address: " + socket.getRemoteSocketAddress());
            try (Connection connection = new Connection(socket)) {
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection, userName);
                serverMainLoop(connection, userName);
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Remote address data transfer error");
            } finally {
                if (userName != null) {
                    connectionMap.remove(userName);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
                }
            }
            ConsoleHelper.writeMessage("Connection with remote address closed");
        }
    }
}