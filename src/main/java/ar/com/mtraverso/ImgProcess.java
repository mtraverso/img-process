package ar.com.mtraverso;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by mtraverso on 10/21/16.
 */
public class ImgProcess {
    ImageIcon icon;
    JFrame frame;
    JPanel panel;
    public static void main(String[] args) {
        OpenCV.loadLocally();

        /*VideoCapture capt = new VideoCapture();
        capt.open(0);
        Mat frame = new Mat();
        capt.read(frame);
        System.out.println(frame);
        capt.retrieve(frame);*/

        /*for(;;){
            capt.read(frame);
            icon.setImage(toBufferedImage(frame));
        }*/
        ImgProcess test = new ImgProcess();
        test.initFrame();

    }

    public void initFrame(){
        frame = new JFrame();
        panel = new JPanel();
        icon = new ImageIcon();
        panel.setPreferredSize(new Dimension(500,500));
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel(icon),BorderLayout.CENTER);
        frame.setPreferredSize(new Dimension((int)(Toolkit.getDefaultToolkit().getScreenSize().getWidth()*0.5),(int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight()*0.5)));
        frame.setMinimumSize(new Dimension((int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() * 0.5), (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() * 0.5)));
        frame.setVisible(true);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(panel);
        initCapture();

    }

    public void initCapture(){
        VideoCapture capt = new VideoCapture();
        capt.open(0);
        Mat frame = new Mat();
        capt.read(frame);
        Mat smaller = new Mat();
        Mat gray = convertFrame(frame);
        Mat prevGray = convertFrame(frame);
        for(;;){
            capt.read(frame);
            gray = convertFrame(frame);




            icon.setImage(toBufferedImage(gray));
            panel.revalidate();
            panel.repaint();
            prevGray = gray;
        }
    }

    public Mat convertFrame(Mat frame){
        Mat smaller= new Mat();
        Mat gray = new Mat();
        Imgproc.resize(frame,smaller,new Size(frame.width()*0.5,frame.height()*0.5));
        Imgproc.cvtColor(smaller,gray, Imgproc.COLOR_RGBA2GRAY);
        return gray;
    }

    public static BufferedImage toBufferedImage(Mat m){
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if ( m.channels() > 1 ) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels()*m.cols()*m.rows();
        byte [] b = new byte[bufferSize];
        m.get(0,0,b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;

    }



    public Image drawLineOnImage(Mat frame){
        BufferedImage image = toBufferedImage(frame);
        Graphics2D g = image.createGraphics();
        g.setStroke(new BasicStroke(2));
        //g.drawLine(0,image.getHeight()/2,image.getWidth(),image.getHeight()/2);
        g.drawLine(image.getWidth()/ 2, 0, image.getWidth()/2,image.getHeight());

        return image;
    }

}
