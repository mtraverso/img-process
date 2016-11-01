package ar.com.mtraverso;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;

/**
 * Created by mtraverso on 10/21/16.
 */
public class ImgProcess4 {
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
                "video.mp4");
        Mat nativeFrame = new Mat();
        Mat frame = new Mat();
        Mat gray = new Mat();
        Mat blurred = new Mat();
        Mat firstMat = new Mat();
        Mat diff = new Mat();
        Mat threshold = new Mat();
        Mat hierarchy = new Mat();
        Mat kernel = new Mat();
        int quantity = 0;


        Color prevColor=null;
        Color before = Color.WHITE;
        boolean first = true;

        camera.read(nativeFrame);
        while (!nativeFrame.empty()) {
            camera.read(nativeFrame);


            nativeFrame.copyTo(frame);
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGB2GRAY);
            Imgproc.GaussianBlur(gray,blurred,new Size(5,5),100);
            if(first){
                blurred.copyTo(firstMat);
                first = false;
                continue;
            }


            Core.absdiff(blurred,firstMat,diff);
            Imgproc.threshold(diff,threshold,50,255,Imgproc.THRESH_BINARY);
            Imgproc.dilate(threshold,threshold,kernel,new Point(1,1),2);
            ArrayList<MatOfPoint> points = new ArrayList<MatOfPoint>();
            Imgproc.findContours(threshold.clone(),points,hierarchy,Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);

            for(MatOfPoint point : points) {
                if(Imgproc.contourArea(point) < 1000){
                    continue;
                }
                Rect rect = Imgproc.boundingRect(point);

                Core.rectangle(frame,rect.br(),rect.tl(),new Scalar(0,255,0));
                Point bottomRight = rect.br();
                Point topLeft = rect.tl();

                Point middlePoint = new Point((topLeft.x+bottomRight.x)/2,(topLeft.y+bottomRight.y)/2);

                Core.rectangle(frame,middlePoint,new Point(middlePoint.x+5,middlePoint.y+5),new Scalar(255,0,0));

                if(middlePoint.y == frame.height()-150){
                    labelUp.setText(""+quantity++);
                    labelUp.repaint();
                }
            }

            vidpanel.setIcon(new ImageIcon(toBufferedImage(diff)));
            gray.copyTo(firstMat);
        }

    }

    public static double colourDistance(Color c1, Color c2)
    {
        double rmean = ( c1.getRed() + c2.getRed() )/2;
        int r = c1.getRed() - c2.getRed();
        int g = c1.getGreen() - c2.getGreen();
        int b = c1.getBlue() - c2.getBlue();
        double weightR = 2 + rmean/256;
        double weightG = 4.0;
        double weightB = 2 + (255-rmean)/256;
        return Math.sqrt(weightR*r*r + weightG*g*g + weightB*b*b);
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

    public static BufferedImage drawLineOnImage(Mat frame,Color color, int xStart, int width){
        BufferedImage image = toBufferedImage(frame);
        Graphics2D g = image.createGraphics();
        g.setStroke(new BasicStroke(1));
        g.setColor(color);
        g.drawLine(xStart,image.getHeight()-150,xStart+width,image.getHeight()-150);

        return image;
    }
}
