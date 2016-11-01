package ar.com.mtraverso;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by mtraverso on 10/21/16.
 */
public class ImgProcess3 {
    static {
        OpenCV.loadLocally();
    }
    public static void main(String[] args) {

        JFrame jframe = new JFrame("Traffic Control");
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JLabel labelBack = new JLabel();
        panel.add(labelBack, BorderLayout.WEST);
        JLabel labelUp = new JLabel();
        panel.add(labelUp, BorderLayout.EAST);
        JLabel vidpanel = new JLabel();
        panel.add(vidpanel, BorderLayout.CENTER);
        jframe.getContentPane().add(panel);
        jframe.setSize(640, 480);
        jframe.setVisible(true);


        VideoCapture camera = new VideoCapture(
                "video2.mp4");

        Mat frame = new Mat();
        Mat gray = new Mat();
        int quantity = 0;

        while (true) {
            camera.read(frame);

            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGB2GRAY);

            MatOfRect rects = new MatOfRect();


            for (Rect obj : rects.toList()) {
                Core.rectangle(frame, obj.br(), obj.tl(), new Scalar(250,0,0));

                int minY = obj.y;
                int maxY = obj.y + obj.height;

                int midY = (minY + maxY)/2;

                if (frame.height() - 100 <= midY-5 && frame.height() - 100 >= midY+5 /*&& obj.x < imag.width()/2*/) {
                    labelBack.setText(""+quantity++);
                    vidpanel.setIcon(new ImageIcon(drawLineOnImage(frame,Color.GREEN)));
                    labelBack.repaint();
                }
            }

                vidpanel.setIcon(new ImageIcon(drawLineOnImage(frame,Color.WHITE)));

        }
    }

    public static BufferedImage drawLineOnImage(Mat frame,Color color){
        BufferedImage image = toBufferedImage(frame);
        Graphics2D g = image.createGraphics();
        g.setStroke(new BasicStroke(1));
        g.drawLine(0,image.getHeight()-150,image.getWidth(),image.getHeight()-150);

        return image;
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
}
