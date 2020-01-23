package root.client;

import root.Connection;
import root.ConsoleHelper;
import root.Message;
import root.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    protected String getServerAddress() throws IOException {
        return ConsoleHelper.readString();
    }

    protected int getServerPort() throws IOException {
        return ConsoleHelper.readInt();
    }

    protected String getUserName() throws IOException {
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Error!");
            clientConnected = false;
        }
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Работа программы остановлена.");
            }
        }

        if (clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
            String msg;
            while (clientConnected) {
                msg = ConsoleHelper.readString();
                if (msg.equals("exit")) break;
                else {
                    if (shouldSendTextFromConsole()) sendTextMessage(msg);
                }
            }
        } else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }
    }

    public class SocketThread extends Thread {
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " присоединился к чату");
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(userName + " покинул чат");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            Message msg;
            while (true) {
                msg = connection.receive();
                if (msg.getType() == MessageType.NAME_REQUEST) {
                    connection.send(new Message(MessageType.USER_NAME, getUserName()));
                } else if (msg.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    return;
                } else throw new IOException("Unexpected MessageType");
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            Message msg;
            while (true) {
                msg = connection.receive();
                if (msg.getType() == MessageType.TEXT) {
                    processIncomingMessage(msg.getData());
                } else if (msg.getType() == MessageType.USER_ADDED) {
                    informAboutAddingNewUser(msg.getData());
                } else if (msg.getType() == MessageType.USER_REMOVED) {
                    informAboutDeletingNewUser(msg.getData());
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        public void run() {
            try {
                String host = getServerAddress();
                int port = getServerPort();
                Socket socket = new Socket(host, port);
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }
    }

    public static void main(String[] args) {
        new Client().run();
    }
}