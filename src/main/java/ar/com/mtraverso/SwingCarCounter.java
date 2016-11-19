package ar.com.mtraverso;

import ar.com.mtraverso.gui.ImageList;
import ar.com.mtraverso.observable.Observer;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.BorderUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.Properties;

/**
 * Created by mtraverso on 11/2/16.
 */
public class SwingCarCounter implements Observer {

    public static void main(String[] args) throws IOException {

        SwingCarCounter carCounter = new SwingCarCounter();
        carCounter.initGui();

    }

    ImageList list;

    private void initGui() throws IOException{
        final Properties prop = new Properties();
        InputStream input = new FileInputStream("app.properties");

        prop.load(input);


        final String videoName = prop.getProperty("video.filename");
        String position = prop.getProperty("line.position");
        String focalLength = prop.getProperty("camera.focal.length");
        String fpsStr = prop.getProperty("camera.frames.per.second");


        if(position == null){
            position = "0";
        }
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

        final CarCounter counter = CarCounter.getInstance(repeat);

        counter.registerObserver(this);

        JPanel northPanel = new JPanel();
        northPanel.setBorder(new BorderUIResource.TitledBorderUIResource("Properties"));

        final JTextField textName = new JTextField();
        textName.setText(videoName);
        final JSpinner textPosition = new JSpinner(new SpinnerNumberModel(Integer.valueOf(position).intValue(),0,2000,1));
        final JComboBox<String> textOrientation = new JComboBox<String>();
        textOrientation.addItem("Horizontal");
        textOrientation.addItem("Vertical");
        JButton start = new JButton(new AbstractAction("Start") {
            public void actionPerformed(ActionEvent e) {
                Thread image = new Thread(iconThread);
                Thread thread = new Thread() {
                    public void run() {

                        counter.setRunning(true);

                        runningIcon = true;

                        counter.setCarCount(0);
                        try {
                            counter.initCapture(textName.getText(), textOrientation.getSelectedItem().toString().toLowerCase(),textPosition.getValue().toString(), Double.valueOf(focalLength), Integer.valueOf(fpsStr));
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }

                    }
                };

                thread.start();
                image.start();
            };


        });
        JButton stop = new JButton(new AbstractAction("Stop") {
            public void actionPerformed(ActionEvent e) {
                counter.setRunning(false);

                runningIcon = false;
            }
        });
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int button = e.getButton();
                if(textOrientation.getSelectedItem().equals("Horizontal")) {

                    int y = (int) e.getPoint().getY();
                    counter.setCrossingLine(y, button);
                    if (button == 1) {
                        textPosition.setValue(y);
                    }
                }
                if(textOrientation.getSelectedItem().equals("Vertical")) {
                    int x = (int)e.getPoint().getX();
                    counter.setCrossingLine(x,button);
                    if (button == 1) {
                        textPosition.setValue(x);
                    }

                }
            }});

        textOrientation.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                counter.setOrientation(((JComboBox<String>)e.getSource()).getSelectedItem().toString().toLowerCase());
            }
        });

        textPosition.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if(textOrientation.getSelectedItem().equals("Horizontal")) {
                    int y = (Integer)textPosition.getValue();
                    counter.setCrossingLine(y,1);
                }
                if(textOrientation.getSelectedItem().equals("Vertical")) {
                    int x = (Integer)textPosition.getValue();
                    counter.setCrossingLine(x,1);
                }
            }
        });

        JButton saveProperties = new JButton(new AbstractAction("Save properties"){
            public void actionPerformed(ActionEvent e) {
                prop.put("video.filename", textName.getText());
                prop.put("line.position",textPosition.getValue().toString());
                prop.put("line.orientation",textOrientation.getSelectedItem().toString().toLowerCase());
                File f = new File("app.properties");
                try {
                    OutputStream o = new FileOutputStream(f);
                    prop.store(o,"Saved");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        northPanel.add(textName);

        northPanel.add(textPosition);
        northPanel.add(textOrientation);
        northPanel.add(saveProperties);


        JPanel southPanel = new JPanel();
        southPanel.add(start);
        southPanel.add(stop);

        panel.add(northPanel,BorderLayout.NORTH);
        panel.add(southPanel,BorderLayout.SOUTH);




        JPanel eastPanel = new JPanel();
        list = new ImageList(5);
        eastPanel.add(list);

        panel.add(eastPanel,BorderLayout.EAST);
        eastPanel.setVisible(true);
        list.setVisible(true);


        frame.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize());
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        dim.setSize(dim.getWidth()-500,dim.getHeight());
        panel.setPreferredSize(dim);

        frame.setVisible(true);
        frame.pack();

        iconThread = new Runnable(){
            public void run() {

                while (runningIcon){
                    if(counter != null && counter.getImage()!= null) {
                        Image image = counter.getImage();
                        label.setIcon(new ImageIcon(image));
                    }
                }
            }
        };
    }

    static Runnable iconThread;
    private static boolean runningIcon = true;

    @Override
    public void update(Image i) {
        ((ImageList.ImageListModel)list.getModel()).addElement(i);
        list.repaint();
    }
}
