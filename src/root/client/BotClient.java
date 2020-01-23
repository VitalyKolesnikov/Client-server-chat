package root.client;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

public class BotClient extends Client {

    @Override
    protected String getUserName() throws IOException {
        return "date_bot_" + (int)(Math.random() * 100);
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    public static void main(String[] args) {
        new BotClient().run();
    }

    public class BotSocketThread extends Client.SocketThread{
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            super.processIncomingMessage(message);
            if (!message.contains(":")) return;
            String[] arr = message.split(": ", 2);
            String userName = arr[0].trim();
            String text = arr[1].trim();
            String pattern = "";

            switch (text) {
                case "дата" : pattern = "d.MM.YYYY"; break;
                case "день" : pattern = "d"; break;
                case "месяц" : pattern = "MMMM"; break;
                case "год" : pattern = "YYYY"; break;
                case "время" : pattern = "H:mm:ss"; break;
                case "час" : pattern = "H"; break;
                case "минуты" : pattern = "m"; break;
                case "секунды" : pattern = "s"; break;
            }
            if (!pattern.equals("")) {
                DateFormat df = new SimpleDateFormat(pattern, Locale.ENGLISH);
                String answer = String.format("Информация для %s: %s", userName,
                        df.format(new GregorianCalendar().getTime()));
                sendTextMessage(answer);
            }
        }
    }
}