package intelite.recorder;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 *
 * @author jonhj
 */
public class TelegramNotifier extends TelegramLongPollingBot {
     
    private final String botToken;
    private final String chatId;

    
    public TelegramNotifier(String botToken, String chatId){
        this.botToken=botToken;
        this.chatId=chatId;
    }

    @Override
    public void onUpdateReceived(Update update) {
       //Este metodo se llama cuando se recibe un mensaje pero esta vez no lo utilizaremos  
    }

    public void sendMessage(String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    
    @Override
    public String getBotUsername() {
        return "RECNOTIBot";
    }
    
    
    @Override
    public String getBotToken() {
        return botToken;
    }
    
}
