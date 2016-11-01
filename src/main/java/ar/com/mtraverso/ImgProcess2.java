package ar.com.mtraverso;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;

/**
 * Created by mtraverso on 10/21/16.
 */
public class ImgProcess2 {
    static Mat imag = null;

    static {
        OpenCV.loadLocally();
    }

    public static void main(String[] args) {
        JFrame jframe = new JFrame("HUMAN MOTION DETECTOR FPS");
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JLabel labelBack = new JLabel();
        panel.add(labelBack,BorderLayout.WEST);
        JLabel labelUp = new JLabel();
        panel.add(labelUp,BorderLayout.EAST);
        JLabel vidpanel = new JLabel();
        panel.add(vidpanel,BorderLayout.CENTER);
        jframe.getContentPane().add(panel);
        jframe.setSize(640, 480);
        jframe.setVisible(true);

        Mat frame = new Mat();
        Mat outerBox = new Mat();
        Mat diff_frame = null;
        Mat tempon_frame = null;
        ArrayList<Rect> array = new ArrayList<Rect>();
        VideoCapture camera = new VideoCapture(
                "video2.mp4");
        Size sz = new Size(640, 480);
        int i = 0;
        int quantityUp = 0;
        int quantityBack = 0;

        while (true) {
            if (camera.read(frame)) {
                Imgproc.resize(frame, frame, sz);
                imag = frame.clone();
                outerBox = new Mat(frame.size(), CvType.CV_8UC1);
                Imgproc.cvtColor(frame, outerBox, Imgproc.COLOR_BGR2GRAY);
                Imgproc.GaussianBlur(outerBox, outerBox, new Size(3, 3), 0);

                if (i == 0) {
                    jframe.setSize(frame.width(), frame.height());
                    diff_frame = new Mat(outerBox.size(), CvType.CV_8UC1);
                    tempon_frame = new Mat(outerBox.size(), CvType.CV_8UC1);
                    diff_frame = outerBox.clone();
                }

                if (i == 1) {
                    Core.subtract(outerBox, tempon_frame, diff_frame);
                    Imgproc.adaptiveThreshold(diff_frame, diff_frame, 255,
                            Imgproc.ADAPTIVE_THRESH_MEAN_C,
                            Imgproc.THRESH_BINARY_INV, 5, 2);
                    array = detection_contours(diff_frame);
                    if (array.size() > 0) {

                        Iterator<Rect> it2 = array.iterator();
                        while (it2.hasNext()) {
                            Rect obj = it2.next();
                            //Core.rectangle(imag, obj.br(), obj.tl(), new Scalar(0, 255, 0), 1);

                            int minY = obj.y;
                            int maxY = obj.y+obj.height;
                            int midY = (minY+maxY)/2;

                            int minX = obj.x;
                            int maxX = obj.x+obj.width;
                            int midX = (minX+maxX)/2;


                            Core.rectangle(imag,new Point (midX,midY),new Point (midX+2,midY+2),new Scalar(255,0,0));

                            if(imag.height()-200 <= midY && imag.height()-200 >= midY+5 && obj.x < imag.width()/2){
                                labelBack.setText("" + quantityBack++);
                                labelBack.repaint();

                                vidpanel.setIcon(new ImageIcon(drawLineOnImage(imag,Color.BLACK)));
                                vidpanel.repaint();
                            }
                            if(imag.height()-200 <= midY && imag.height()-200 >= midY+5 && obj.x > imag.width()/2){
                                labelUp.setText("" + quantityUp++);
                                labelUp.repaint();

                                vidpanel.setIcon(new ImageIcon(drawLineOnImage(imag,Color.BLACK)));
                                vidpanel.repaint();
                            }
                        }

                    }
                }

                i = 1;

                ImageIcon image = new ImageIcon(drawLineOnImage(imag,Color.WHITE));
                vidpanel.setIcon(image);
                vidpanel.repaint();
                tempon_frame = outerBox.clone();

            }
        }
    }

    public static BufferedImage drawLineOnImage(Mat frame,Color color){
        BufferedImage image = toBufferedImage(frame);
        Graphics2D g = image.createGraphics();
        g.setStroke(new BasicStroke(1));
        g.drawLine(0,image.getHeight()-200,image.getWidth(),image.getHeight()-200);

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

    /*public static BufferedImage Mat2bufferedImage(Mat image) {
        MatOfByte bytemat = new MatOfByte();
        Highgui.imencode(".jpg", image, bytemat);
        byte[] bytes = bytemat.toArray();
        InputStream in = new ByteArrayInputStream(bytes);
        BufferedImage img = null;
        try {
            img = ImageIO.read(in);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return img;
    }*/

    public static ArrayList<Rect> detection_contours(Mat outmat) {
        Mat v = new Mat();
        Mat vv = outmat.clone();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(vv, contours, v, Imgproc.RETR_LIST,
                Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = 1000;
        int maxAreaIdx = -1;
        Rect r = null;
        ArrayList<Rect> rect_array = new ArrayList<Rect>();

        for (int idx = 0; idx < contours.size(); idx++) {
            Mat contour = contours.get(idx);
            double contourarea = Imgproc.contourArea(contour);
            if (contourarea > maxArea) {
            // maxArea = contourarea;
            maxAreaIdx = idx;
            r = Imgproc.boundingRect(contours.get(maxAreaIdx));
            rect_array.add(r);

            Imgproc.drawContours(imag, contours, maxAreaIdx, new Scalar(0,0, 255));
        }

        }

        v.release();

        return rect_array;

    }
}
