package ar.com.mtraverso.javacv;

import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_videoio.VideoCapture;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;


import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/**
 * Created by mtraverso on 10/24/16.
 */
public class ImgProcess {

    JFrame frame;
    JPanel panel;
    JLabel label;

    JSlider r1,r2,g1,g2,b1,b2,a1,a2;


    public static void main(String[] args) {
        ImgProcess process = new ImgProcess();
        process.initFrame();
    }

    public ImgProcess (){

    }

    public void initFrame(){
        r1= new JSlider(0,255);
        g1= new JSlider(0,255);
        b1= new JSlider(0,255);
        a1= new JSlider(0,255);
        r2= new JSlider(0,255);
        g2= new JSlider(0,255);
        b2= new JSlider(0,255);
        a2= new JSlider(0,255);
        frame = new JFrame();
        panel = new JPanel();
        label = new JLabel();
        panel.setLayout(new BorderLayout());
        panel.add(label,BorderLayout.CENTER);
        frame.getContentPane().add(panel);
        frame.setPreferredSize(new Dimension(640,480));
        frame.pack();
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initCapture();
    }

    private void initCapture(){
        VideoCapture cap = new VideoCapture();
        cap.open("video2.mp4");

        Mat original = new Mat();
        Mat prevFrame = new Mat();
        Mat gray = new Mat();
        Mat frameDelta = new Mat();
        Mat threshold = new Mat();
        Mat hierarchy = new Mat();

        cap.read(original);
        cvtColor(original,prevFrame,COLOR_BGR2GRAY);

        while(!original.empty()) {
            cap.read(original);

            cvtColor(original,gray,COLOR_BGR2GRAY);

            GaussianBlur(gray,gray,new Size(3,3),0);

            absdiff(prevFrame,gray,frameDelta);

            threshold(frameDelta,threshold,10,255,THRESH_BINARY);

            dilate(threshold,threshold,hierarchy);

            MatVector vector = new MatVector();
            findContours(threshold,vector,RETR_EXTERNAL,CHAIN_APPROX_SIMPLE);

            drawContours(original, vector,
                    -1, // draw all contours
                    new Scalar(0, 0, 255, 0));



            label.setIcon(new ImageIcon(toBufferedImage(new IplImage(original))));
            label.repaint();

            gray.copyTo(prevFrame);

        }
    }



    public static BufferedImage toBufferedImage(IplImage src) {
        OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
        Java2DFrameConverter paintConverter = new Java2DFrameConverter();
        Frame frame = grabberConverter.convert(src);
        return paintConverter.getBufferedImage(frame,1);
    }

}
