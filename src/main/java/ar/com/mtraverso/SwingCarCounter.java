package ar.com.mtraverso;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by mtraverso on 11/2/16.
 */
public class SwingCarCounter{

    public static void main(String[] args) throws IOException {

        Properties prop = new Properties();
        InputStream input = new FileInputStream("app.properties");

        prop.load(input);


        String videoName = prop.getProperty("video.filename");
        String position = prop.getProperty("line.position");

        final JFrame frame = new JFrame();

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        final JPanel panel = new JPanel();

        panel.setLayout(new BorderLayout());
        final JLabel label = new JLabel();
        frame.getContentPane().add(panel);
        panel.add(label,BorderLayout.CENTER);


        boolean repeat;

        try{
            Integer.valueOf(videoName);
            repeat = false;
        }catch(NumberFormatException nfe){
            repeat = true;
        }

        final CarCounter counter = new CarCounter(repeat);


        JPanel northPanel = new JPanel();

        final JTextField textName = new JTextField();
        textName.setText(videoName);
        final JTextField textPosition = new JTextField();
        textPosition.setText(position);
        final JComboBox<String> textOrientation = new JComboBox<String>();
        textOrientation.addItem("Horizontal");
        textOrientation.addItem("Vertical");
        JButton start = new JButton(new AbstractAction("Start") {
            public void actionPerformed(ActionEvent e) {
                Thread thread = new Thread() {
                    public void run() {
                        Thread thread = new Thread(iconThread);
                        counter.setRunning(true);

                        runningIcon = true;
                        thread.start();
                        counter.setCarCount(0);
                        try {
                            counter.initCapture(textName.getText(), textOrientation.getSelectedItem().toString().toLowerCase(), textPosition.getText());
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }

                    }
                };
                thread.start();
            };


        });
        JButton stop = new JButton(new AbstractAction("Stop") {
            public void actionPerformed(ActionEvent e) {
                counter.setRunning(false);

                runningIcon = false;
            }
        });

        northPanel.add(textName);
        northPanel.add(textPosition);
        northPanel.add(textOrientation);
        JPanel southPanel = new JPanel();
        southPanel.add(start);
        southPanel.add(stop);

        panel.add(northPanel,BorderLayout.NORTH);
        panel.add(southPanel,BorderLayout.SOUTH);

        frame.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize());
        panel.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize());

        frame.setVisible(true);
        frame.pack();

        iconThread = new Runnable(){
            public void run() {

                while (runningIcon){
                    if(counter != null && counter.getImage()!= null) {
                        label.setIcon(new ImageIcon(counter.getImage()));
                    }
                }
            }
        };

    }

    static Runnable iconThread;
    private static boolean runningIcon = true;
}
