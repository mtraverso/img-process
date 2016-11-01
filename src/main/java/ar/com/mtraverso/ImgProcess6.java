package ar.com.mtraverso;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.video.BackgroundSubtractorMOG;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;

/**
 * Created by mtraverso on 10/25/16.
 */
public class ImgProcess6 {
    static
    {
        OpenCV.loadLocally();
    }

    Mat image;
    Mat foregroundMask;
    Mat maskedImage;
    BackgroundSubtractorMOG backgroundSubtractor;

    public ImgProcess6()
    {
        image = new Mat();
        foregroundMask = new Mat();
        maskedImage = new Mat();
        backgroundSubtractor = new BackgroundSubtractorMOG();
        initGui();
    }

    JFrame jframe;
    JPanel panel;
    JLabel labelBack;
    JLabel labelUp;
    JLabel vidpanel;

    public void initGui(){

        jframe = new JFrame("Traffic Control");
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        labelBack = new JLabel();
        panel.add(labelBack, BorderLayout.WEST);
        labelUp = new JLabel();
        panel.add(labelUp, BorderLayout.EAST);
        vidpanel = new JLabel();
        panel.add(vidpanel, BorderLayout.CENTER);
        jframe.getContentPane().add(panel);
        jframe.setSize(640, 480);
        jframe.setVisible(true);
    }

    public Collection<String> processVideo()
    {
        CascadeClassifier carDetector = new CascadeClassifier("cars.xml");


        VideoCapture videoCapture = new VideoCapture();
        videoCapture.open("CarsDrivingUnderBridge.mp4");

        int index = 0;

        while (true)
        {
            if (!videoCapture.read(image))
            {
                break;
            }


            MatOfRect carDetections = new MatOfRect();
            carDetector.detectMultiScale(image, carDetections);

            // Draw a bounding box around each hit
            for (Rect rect : carDetections.toArray())
            {
                Core.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(0, 255, 0));
            }

            vidpanel.setIcon(new ImageIcon(toBufferedImage(image)));
        }

        return null;
    }

    protected void processFrame()
    {
        backgroundSubtractor.apply(image, foregroundMask, 0.1);

        //Imgproc.cvtColor(foregroundMask, foregroundMask, Imgproc.COLOR_, 4);
        //Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2GRAY, 4);
        //Imgproc.cvtColor(maskedImage, maskedImage, Imgproc.COLOR_RGBA2GRAY, 4);

        Core.bitwise_and(image, image, maskedImage, foregroundMask);
    }

    public static void main(String[] args) {
        ImgProcess6 proc = new ImgProcess6();
        proc.processVideo();
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
