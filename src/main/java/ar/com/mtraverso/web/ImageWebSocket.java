package ar.com.mtraverso.web;

import ar.com.mtraverso.CarCounter;
import ar.com.mtraverso.WebCarCounter;

import javax.imageio.ImageIO;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Properties;

/**
 * Created by mtraverso on 11/2/16.
 */
@ServerEndpoint(value = "/image")
public class ImageWebSocket {


    BufferedImage image;
    Thread thread;

    CarCounter counter;

    public ImageWebSocket() throws  IOException, InterruptedException{
        counter = WebCarCounter.getInstance();
        image = null;
        thread = new Thread(runnable);
        thread.start();
    }



    Runnable runnable = new Runnable() {
        public void run() {
            System.out.println("To run");
                while(counter.isRunning()) {
                    System.out.println("Running");
                    image = (BufferedImage) counter.getImage();
                    try {
                        if(image != null && session != null && session.isOpen()) {
                            session.getBasicRemote().sendText(getBase64Image(image));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        }
    };


    Session session;

    @OnOpen
    public void onSessionOpened(Session session) {
        this.session = session;
        System.out.println("onSessionOpened: " + session);
    }

    public String getBase64Image(BufferedImage image ) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (image != null) {
            try {
                ImageIO.write(image, "jpg", baos);
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            } catch( IOException ioe ) {
                ioe.printStackTrace();
            }
        }
        return null;
    }

    @OnMessage
    public void onMessageReceived(String message, Session session) throws IOException {

        if ("img".equals(message)) {


        }
    }
    @OnClose
    public void onClose(Session session, CloseReason closeReason) throws IOException {
        System.out.println("onClose");
        session.close();
    }
    @OnError
    public void onErrorReceived(Throwable t) {
        System.out.println("onErrorReceived: " + t);
    }
}
