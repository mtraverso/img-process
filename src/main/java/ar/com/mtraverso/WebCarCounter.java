package ar.com.mtraverso;

import ar.com.mtraverso.web.ImageWebSocket;
import ar.com.mtraverso.web.RestServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.websocket.server.ServerContainer;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by mtraverso on 11/2/16.
 */
public class WebCarCounter {

    Thread captThread;
    boolean runningIcon;
    String videoName;
    String position;
    String orientation;
    CarCounter counter;

    Runnable capture = new Runnable() {
        public void run() {
            try {
                System.out.println("Start capture");
                counter.initCapture(videoName, orientation, position);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    private static WebCarCounter instance = new WebCarCounter();

    public static CarCounter getInstance() {
        return instance.counter;
    }



    private WebCarCounter() {
        Properties prop = new Properties();
        try {
            InputStream input = new FileInputStream("app.properties");

            prop.load(input);
        }catch(IOException e){
            e.printStackTrace();
        }
        videoName = prop.getProperty("video.filename");
        position = prop.getProperty("line.position");
        orientation = prop.getProperty("line.orientation");
        boolean repeat;

        try{
            Integer.valueOf(videoName);
            repeat = false;
        }catch(NumberFormatException nfe){
            repeat = true;
        }

        counter = new CarCounter(repeat);
        counter.setRunning(true);

        counter.setCarCount(0);

        runningIcon = true;

        captThread = new Thread(capture);
        captThread.start();
    }

    public static void main(String[] args) throws Exception {

        int port = 8080;

        Server server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setResourceBase(".");
        context.setWelcomeFiles(new String[]{"index.html"});
        server.setHandler(context);


        ServletHolder holderHome = new ServletHolder("/", DefaultServlet.class);
        holderHome.setInitParameter("dirAllowed","true");
        holderHome.setInitParameter("pathInfoOnly","true");
        context.addServlet(holderHome, "/*");
        holderHome.setInitOrder(0);

        ServletHolder rest = context.addServlet(ServletContainer.class,"/rest/*");
        rest.setInitOrder(1);

        rest.setInitParameter("jersey.config.server.provider.classnames", RestServlet.class.getCanonicalName());

        try {
            ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
            wscontainer.addEndpoint(ImageWebSocket.class);


            server.start();
            System.out.println("Listening port : " + port );

            server.join();
        } catch (Exception e) {
            System.out.println("Error.");
            e.printStackTrace();
        }


    }

}
