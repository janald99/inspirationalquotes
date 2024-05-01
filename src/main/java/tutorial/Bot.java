package tutorial;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Bot extends TelegramLongPollingBot {

    Dotenv dotenv = Dotenv.load();
    String botToken = dotenv.get("TELEGRAM_BOT_TOKEN");
    String quoteAPI = dotenv.get("QUOTE_API");

    private boolean screaming = false;

    @Override
    public String getBotUsername() {
        return "InspiringYouBot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        var msg = update.getMessage();
        var user = msg.getFrom();
        var id = user.getId();

        if(msg.isCommand()){
            if (msg.getText().equals("/start")) {
                String startMessage = "Hey! I give out daily inspirational quotes when you ask for them! Simple type /quote.";
                sendText(id, startMessage);
            } else if(msg.getText().equals("/help")) {
                String helpMessage = "Available commands:\n"
                        + "/start - Start the bot\n"
                        + "/help - Show this help message\n"
                        + "/quote - Ask for an inspirational quote\n"
                        + "/scream - Switch to screaming mode - Responses will be in Caps!\n"
                        + "/whisper - Switch to whispering mode - toggle off screaming mode";
                sendText(id, helpMessage);
            } else if(msg.getText().equals("/quote")) {
                String quoteBody = getQuote();
                String quoteMessage = "", author="", category="";

                Gson gson = new Gson();
                Type listType = new TypeToken<List<Map<String, String>>>(){}.getType();
                List<Map<String, String>> quoteList = gson.fromJson(quoteBody, listType);

                for (Map<String, String> quote : quoteList) {
                    quoteMessage = quote.get("quote");
                    author = quote.get("author");
                    category = quote.get("category");
                }

                String resultMessage = "\"" + quoteMessage + "\"" + '\n' + "Author: " + author;
                if (screaming) {
                    resultMessage = resultMessage.toUpperCase();
                }
                sendText(id,resultMessage);
            } else if(msg.getText().equals("/scream")) { //If the command was /scream, we switch gears
                screaming = true;
            } else if (msg.getText().equals("/whisper")) { //Otherwise, we return to normal
                screaming = false;
            }
            return;                                     //We don't want to echo commands, so we exit
        }

//        System.out.println(user.getFirstName() + " wrote " + msg.getText());
        if(screaming)                            //If we are screaming
            scream(id, update.getMessage());     //Call a custom method
        else
            copyMessage(id, msg.getMessageId()); //Else proceed normally
    }

    public void sendText(Long who, String what){
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString()) //Who are we sending a message to
                .text(what).build();    //Message content
        try {
            execute(sm);                        //Actually sending the message
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);      //Any error will be printed here
        }
    }

    public void copyMessage(Long who, Integer msgId){
        CopyMessage cm = CopyMessage.builder()
                .fromChatId(who.toString())  //We copy from the user
                .chatId(who.toString())      //And send it back to him
                .messageId(msgId)            //Specifying what message
                .build();
        try {
            execute(cm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void scream(Long id, Message msg) {
        if(msg.hasText())
            sendText(id, msg.getText().toUpperCase());
        else
            copyMessage(id, msg.getMessageId());  //We can't really scream a sticker
    }

    private String getQuote() {
        String apiUrl = "https://api.api-ninjas.com/v1/quotes?category=happiness";
        String result;

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Api-Key", quoteAPI);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                result = response.toString();
            } else {
                result = "Failed to fetch quote. Response code: " + responseCode;
            }
            connection.disconnect();
            return result;
        } catch (IOException e) {
            result = "An error occurred while fetching the quote: " + e.getMessage();
            return result;
        }
    }

}
