package client;

import server.Connection;
import server.ConsoleHelper;
import server.Message;
import server.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
    protected String getServerAddress(){
        ConsoleHelper.writeMessage("Введите адрес сервера:");
        return ConsoleHelper.readString();
    }
    protected int getServerPort(){
            ConsoleHelper.writeMessage("Введите порт сервера:");
        return ConsoleHelper.readInt();
    }
    protected String getUserName(){
        ConsoleHelper.writeMessage("Введите ваше имя:");
        return ConsoleHelper.readString();
    }
    protected boolean shouldSendTextFromConsole(){
        return true;
    }
    protected SocketThread getSocketThread(){
        return new SocketThread();
    }
    protected void sendTextMessage(String text){
        try{
            connection.send(new Message(MessageType.TEXT,text));
        }catch (IOException e){
            ConsoleHelper.writeMessage("Не удалось отправить сообщение");
            clientConnected = false;
        }
    }
    public void run(){
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        try{
            synchronized (this){
                wait();
            }
        }catch (InterruptedException e){
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
            return;
        }
        if(clientConnected)
            ConsoleHelper.writeMessage("Соединение установлено.Для выхода наберите команду 'exit'.");
        else
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        while (clientConnected){
            String text = ConsoleHelper.readString();
            if(text.equalsIgnoreCase("exit"))
                break;
            if(shouldSendTextFromConsole())
                sendTextMessage(text);
        }
    }
    public class SocketThread extends Thread {
        @Override
        public void run() {
          try{
              //соединение с сервером
              connection = new Connection(new Socket(getServerAddress(),getServerPort()));
              clientHandshake();
              clientMainLoop();
          }catch(IOException | ClassNotFoundException e){
              notifyConnectionStatusChanged(false);
          }
        }

        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage("Участник '" + userName + "' присоединился к чату.");
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage("Участник '" + userName + "' покинул чат.");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }
        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true){
                Message message = connection.receive();
                 if(message.getType() == MessageType.NAME_REQUEST){ // сервер запросил имя
                     // запрос имени с консоли
                     String name = getUserName();
                     //отправка имени серверу
                     connection.send(new Message(MessageType.USER_NAME,name));
                     //сервер принял имя
            }else if(message.getType() == MessageType.NAME_ACCEPTED){
                     //сообщаем потоку что он может продолжить
                     notifyConnectionStatusChanged(true);
                     return;
                 }else{
                     throw new IOException("Unexpected server.MessageType");
                 }
                }
        }
       protected void clientMainLoop() throws IOException, ClassNotFoundException{
            // реализация главного цикла обработки сообщений сервера
           while (true){
               Message message = connection.receive();
               //Если это текстовое сообщение
               if(message.getType() == MessageType.TEXT){
                   processIncomingMessage(message.getData());
               }else if(MessageType.USER_ADDED == message.getType()){
                   informAboutAddingNewUser(message.getData());
               }else if(MessageType.USER_REMOVED == message.getType()){
                   informAboutDeletingNewUser(message.getData());
               }else{
                   throw new IOException("Unexpected server.MessageType");
               }
           }
       }
    }
}
