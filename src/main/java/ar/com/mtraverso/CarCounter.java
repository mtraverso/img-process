package ar.com.mtraverso;

import ar.com.mtraverso.utils.Blob;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by matias on 31/10/16.
 */
public class CarCounter {

    static
    {
        OpenCV.loadLocally();
    }
    private static Scalar SCALAR_BLACK = new Scalar(0.0, 0.0, 0.0);
    private static Scalar SCALAR_WHITE = new Scalar(255.0, 255.0, 255.0);
    private static Scalar SCALAR_YELLOW = new Scalar(0.0, 255.0, 255.0);
    private static Scalar SCALAR_GREEN = new Scalar(0.0, 200.0, 0.0);
    private static Scalar SCALAR_RED = new Scalar(0.0, 0.0, 255.0);

    private static int carCount = 0;

    public static void main(String[] args) throws IOException{

        Properties prop = new Properties();
        InputStream input = new FileInputStream("app.properties");

        // load a properties file
        prop.load(input);


        String videoName = prop.getProperty("video.filename");
        String position = prop.getProperty("line.position");
        String orientation = prop.getProperty("line.orientation");
        String movement = prop.getProperty("movement");

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        JLabel label = new JLabel();
        frame.getContentPane().add(panel);
        panel.add(label);

        VideoCapture cap = new VideoCapture();

        try{
            cap.open(Integer.valueOf(videoName));
        }catch(NumberFormatException nfe){
            cap.open(videoName);
        }
        Mat imgFrame1 = new Mat();
        Mat imgFrame2 = new Mat();

        List<Blob> blobs = new ArrayList<Blob>();

        Point[] crossingLine = new Point[2];

        cap.read(imgFrame1);
        cap.read(imgFrame2);

        frame.setPreferredSize(new Dimension(imgFrame1.width(),imgFrame1.height()));
        frame.setVisible(true);
        frame.pack();

        Long intHorizontalLinePosition = Math.round((double)Double.valueOf(position));

        Point p1 = null,p2= null ;
        if(orientation.equals("horizontal")){
            p1 = new Point(0,Double.valueOf(position));
            p2 = new Point(imgFrame1.cols()-1,Double.valueOf(position));
        }else if (orientation.equals("vertical")){
            p1 = new Point(Double.valueOf(position),0);
            p2 = new Point(Double.valueOf(position),imgFrame1.rows()-1);
        }

        crossingLine[0] = p1;
        crossingLine[1] = p2;

        int frameCount = 2;

        boolean blnFirstFrame = true;

        while(cap.isOpened()){
            List<Blob> currentFrameBlobs= new ArrayList<Blob>();

            Mat imgFrame1Copy = imgFrame1.clone();
            Mat imgFrame2Copy = imgFrame2.clone();

            Mat imgDifference = new Mat();
            Mat imgThresh = new Mat();

            Imgproc.cvtColor(imgFrame1Copy,imgFrame1Copy,Imgproc.COLOR_BGR2GRAY);
            Imgproc.cvtColor(imgFrame2Copy,imgFrame2Copy,Imgproc.COLOR_BGR2GRAY);

            Imgproc.GaussianBlur(imgFrame1Copy,imgFrame1Copy,new Size(5,5),0);
            Imgproc.GaussianBlur(imgFrame2Copy,imgFrame2Copy,new Size(5,5),0);

            Core.absdiff(imgFrame1Copy,imgFrame2Copy,imgDifference);

            Imgproc.threshold(imgDifference,imgThresh,30,255,Imgproc.THRESH_BINARY);


            Mat structuringElement3x3 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
            Mat structuringElement5x5 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
            Mat structuringElement7x7 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7));
            Mat structuringElement15x15 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15, 15));

            for(int i = 0; i < 2 ; i++){
                Imgproc.dilate(imgThresh, imgThresh, structuringElement5x5);
                Imgproc.dilate(imgThresh, imgThresh, structuringElement5x5);
                Imgproc.erode(imgThresh, imgThresh, structuringElement5x5);
            }

            Mat imgThreshCopy =  imgThresh.clone();
            Mat hierarchy = new Mat();

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();


            Imgproc.findContours(imgThreshCopy,contours,hierarchy,Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);


            List<MatOfInt> convexHulls = new ArrayList<MatOfInt>();


            for(MatOfPoint ct : contours){
                MatOfInt hull = new MatOfInt();
                Imgproc.convexHull(ct,hull);
                convexHulls.add(hull);
            }


            for (MatOfPoint ch : contours) {
                Blob possibleBlob = new Blob(ch);

                if (possibleBlob.currentBoundingRect.area() > 400 &&
                        possibleBlob.dblCurrentAspectRatio > 0.2 &&
                        possibleBlob.dblCurrentAspectRatio < 4.0 &&
                        possibleBlob.currentBoundingRect.width > 30 &&
                        possibleBlob.currentBoundingRect.height > 30 &&
                        possibleBlob.dblCurrentDiagonalSize > 60.0 &&
                        (Imgproc.contourArea(possibleBlob.currentContour) / (double)possibleBlob.currentBoundingRect.area()) > 0.50) {
                    currentFrameBlobs.add(possibleBlob);
                }
            }

            if (blnFirstFrame == true) {
                for (Blob currentFrameBlob : currentFrameBlobs) {
                    blobs.add(currentFrameBlob);
                }
            } else {
                matchCurrentFrameBlobsToExistingBlobs(blobs, currentFrameBlobs);
            }


            imgFrame2Copy = imgFrame2.clone();          // get another copy of frame 2 since we changed the previous frame 2 copy in the processing above

            drawBlobInfoOnImage(blobs, imgFrame2Copy);

            boolean blnAtLeastOneBlobCrossedTheLine = checkIfBlobsCrossedTheLine(blobs, intHorizontalLinePosition.intValue(),orientation,movement);

            if (blnAtLeastOneBlobCrossedTheLine == true) {
                Core.line(imgFrame2Copy, crossingLine[0], crossingLine[1], SCALAR_GREEN, 2);
            }
            else {
                Core.line(imgFrame2Copy, crossingLine[0], crossingLine[1], SCALAR_RED, 2);
            }

            drawCarCountOnImage(carCount, imgFrame2Copy);

            label.setIcon(new ImageIcon(toBufferedImage(imgFrame2Copy)));

            //cv::waitKey(0);                 // uncomment this line to go frame by frame for debugging

            // now we prepare for the next iteration

            currentFrameBlobs.clear();

            imgFrame1 = imgFrame2.clone();           // move frame 1 up to where frame 2 is

            cap.read(imgFrame2);

            blnFirstFrame = false;
            frameCount++;


        }

    }

    public static Image toBufferedImage(Mat m){
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

    static void matchCurrentFrameBlobsToExistingBlobs(List<Blob> existingBlobs, List<Blob> currentFrameBlobs) {

        for (Blob existingBlob : existingBlobs) {

            existingBlob.blnCurrentMatchFoundOrNewBlob = false;

            existingBlob.predictNextPosition();
        }

        for (Blob currentFrameBlob : currentFrameBlobs) {

            int intIndexOfLeastDistance = 0;
            double dblLeastDistance = 100000.0;

            for (int i = 0; i < existingBlobs.size(); i++) {

                if (existingBlobs.get(i).blnStillBeingTracked == true) {

                    double dblDistance = distanceBetweenPoints(currentFrameBlob.centerPositions.get(currentFrameBlob.centerPositions.size()-1), existingBlobs.get(i).predictedNextPosition);

                    if (dblDistance < dblLeastDistance) {
                        dblLeastDistance = dblDistance;
                        intIndexOfLeastDistance = i;
                    }
                }
            }

            if (dblLeastDistance < currentFrameBlob.dblCurrentDiagonalSize * 0.5) {
                addBlobToExistingBlobs(currentFrameBlob, existingBlobs, intIndexOfLeastDistance);
            }
            else {
                currentFrameBlob.predictNextPosition();
                addNewBlob(currentFrameBlob, existingBlobs);
            }

        }

        for (Blob existingBlob : existingBlobs) {

            if (existingBlob.blnCurrentMatchFoundOrNewBlob == false) {
                existingBlob.intNumOfConsecutiveFramesWithoutAMatch++;
            }

            if (existingBlob.intNumOfConsecutiveFramesWithoutAMatch >= 5) {
                existingBlob.blnStillBeingTracked = false;
            }

        }

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    static void addBlobToExistingBlobs(Blob currentFrameBlob, List<Blob> existingBlobs, int intIndex) {

        existingBlobs.get(intIndex).currentContour = currentFrameBlob.currentContour;
        existingBlobs.get(intIndex).currentBoundingRect = currentFrameBlob.currentBoundingRect;

        existingBlobs.get(intIndex).centerPositions.add(currentFrameBlob.centerPositions.get(currentFrameBlob.centerPositions.size() - 1));

        existingBlobs.get(intIndex).dblCurrentDiagonalSize = currentFrameBlob.dblCurrentDiagonalSize;
        existingBlobs.get(intIndex).dblCurrentAspectRatio = currentFrameBlob.dblCurrentAspectRatio;

        existingBlobs.get(intIndex).blnStillBeingTracked = true;
        existingBlobs.get(intIndex).blnCurrentMatchFoundOrNewBlob = true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    static void addNewBlob(Blob currentFrameBlob, List<Blob> existingBlobs) {

        currentFrameBlob.blnCurrentMatchFoundOrNewBlob = true;

        existingBlobs.add(currentFrameBlob);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    static double distanceBetweenPoints(Point point1, Point point2) {

        double intX = Math.abs(point1.x - point2.x);
        double intY = Math.abs(point1.y - point2.y);

        return(Math.sqrt(Math.pow(intX, 2) + Math.pow(intY, 2)));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    static void drawAndShowContours(Size imageSize, List<MatOfPoint > contours, String strImageName) {
        Mat image = new Mat(imageSize, CvType.CV_8UC3, SCALAR_BLACK);

        Imgproc.drawContours(image, contours, -1, SCALAR_WHITE, -1);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        JLabel label = new JLabel();
        frame.getContentPane().add(panel);
        panel.add(label);
        frame.setVisible(true);
        frame.pack();

        label.setIcon(new ImageIcon(toBufferedImage(image)));

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    static void drawAndShowContoursBlob(Size imageSize, List<Blob> blobs, String strImageName) {

        Mat image = new Mat(imageSize, CvType.CV_8UC3, SCALAR_BLACK);

        List<MatOfPoint>  contours = new ArrayList<MatOfPoint>();

        for (Blob blob : blobs) {
            if (blob.blnStillBeingTracked == true) {
                contours.add(blob.currentContour);
            }
        }

        Imgproc.drawContours(image, contours, -1, SCALAR_WHITE, -1);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        JLabel label = new JLabel();
        frame.getContentPane().add(panel);
        panel.add(label);
        frame.setVisible(true);
        frame.pack();

        label.setIcon(new ImageIcon(toBufferedImage(image)));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    static boolean checkIfBlobsCrossedTheLine(List<Blob> blobs, int intHorizontalLinePosition,String orientation,String movement) {
        boolean blnAtLeastOneBlobCrossedTheLine = false;

        for (Blob blob : blobs) {

            if (blob.blnStillBeingTracked == true && blob.centerPositions.size() >= 2) {
                int prevFrameIndex = (int)blob.centerPositions.size() - 2;
                int currFrameIndex = (int)blob.centerPositions.size() - 1;

                if(orientation.equals("vertical")){
                    if(movement.equals("increasing")) {
                        if (blob.centerPositions.get(prevFrameIndex).x > intHorizontalLinePosition && blob.centerPositions.get(currFrameIndex).x <= intHorizontalLinePosition) {
                            carCount++;
                            blnAtLeastOneBlobCrossedTheLine = true;
                        }
                    }else if (movement.equals("decreasing")){
                        if (blob.centerPositions.get(prevFrameIndex).x < intHorizontalLinePosition && blob.centerPositions.get(currFrameIndex).x >= intHorizontalLinePosition) {
                            carCount++;
                            blnAtLeastOneBlobCrossedTheLine = true;
                        }
                    }
                }else if(orientation.equals("horizontal")) {
                    if(movement.equals("increasing")) {
                        if (blob.centerPositions.get(prevFrameIndex).y > intHorizontalLinePosition && blob.centerPositions.get(currFrameIndex).y <= intHorizontalLinePosition) {
                            carCount++;
                            blnAtLeastOneBlobCrossedTheLine = true;
                        }
                    }else if (movement.equals("decreasing")){
                        if (blob.centerPositions.get(prevFrameIndex).y < intHorizontalLinePosition && blob.centerPositions.get(currFrameIndex).y >= intHorizontalLinePosition) {
                            carCount++;
                            blnAtLeastOneBlobCrossedTheLine = true;
                        }
                    }
                }
            }

        }

        return blnAtLeastOneBlobCrossedTheLine;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    static void drawBlobInfoOnImage(List<Blob> blobs, Mat imgFrame2Copy) {

        for (int i = 0; i < blobs.size(); i++) {

            if (blobs.get(i).blnStillBeingTracked == true) {
                //Core.rectangle(imgFrame2Copy, blobs.get(i).currentBoundingRect.br(), blobs.get(i).currentBoundingRect.tl(), SCALAR_RED, 2);

                int intFontFace = Core.FONT_HERSHEY_SIMPLEX;
                double dblFontScale = blobs.get(i).dblCurrentDiagonalSize / 60.0;
                int intFontThickness = (int)Math.round(dblFontScale * 1.0);

                //Core.putText(imgFrame2Copy, String.valueOf(i), blobs.get(i).centerPositions.get(blobs.get(i).centerPositions.size()-1), intFontFace, dblFontScale, SCALAR_GREEN, intFontThickness);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    static void drawCarCountOnImage(int carCount, Mat imgFrame2Copy) {

        int intFontFace = Core.FONT_HERSHEY_SIMPLEX;
        double dblFontScale = (imgFrame2Copy.rows() * imgFrame2Copy.cols()) / 300000.0;
        int intFontThickness = (int)Math.round(dblFontScale * 1.5);

        Size textSize = Core.getTextSize(String.valueOf(carCount), intFontFace, dblFontScale, intFontThickness, new int[]{0});

        Point ptTextBottomLeftPosition = new Point();

        ptTextBottomLeftPosition.x = imgFrame2Copy.cols() - 1 - (int)((double)textSize.width * 1.25);
        ptTextBottomLeftPosition.y = (int)((double)textSize.height * 1.25);

        Core.putText(imgFrame2Copy, String.valueOf(carCount), ptTextBottomLeftPosition, intFontFace, dblFontScale, SCALAR_GREEN, intFontThickness);

    }
}
