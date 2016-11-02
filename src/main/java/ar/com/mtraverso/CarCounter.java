package ar.com.mtraverso;

import ar.com.mtraverso.utils.Blob;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

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

    public CarCounter(boolean repeat){
        this.repeat = repeat;
    }

    private boolean repeat;

    private int carCount = 0;

    private Image image;

    private boolean running;


    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void setCarCount(int carCount){
        this.carCountByMovement.put("increasing",carCount);
        this.carCountByMovement.put("decreasing",carCount);
    }

    public int getCarCount(String direction){
        return this.carCountByMovement.get(direction);
    }

    Map<String,Integer> carCountByMovement = new HashMap<String, Integer>();

    public void initCapture(String videoName,String orientation, String position) throws InterruptedException {

        carCountByMovement.put("increasing",0);
        carCountByMovement.put("decreasing",0);

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



        Long intHorizontalLinePosition = Math.round((double)Double.valueOf(position));

        Point p11 = null,p12= null ;
        if(orientation.equals("horizontal")){
            p11 = new Point(0,Double.valueOf(position));
            p12 = new Point(imgFrame1.cols()-1,Double.valueOf(position));
        }else if (orientation.equals("vertical")){
            p11 = new Point(Double.valueOf(position),0);
            p12 = new Point(Double.valueOf(position),imgFrame1.rows()-1);
        }

        crossingLine[0] = p11;
        crossingLine[1] = p12;


        int frameCount = 2;

        boolean blnFirstFrame = true;
        while(running){
            List<Blob> currentFrameBlobs= new ArrayList<Blob>();

            Mat imgFrame1Copy = imgFrame1.clone();
            Mat imgFrame2Copy = imgFrame2.clone();
            if(repeat) {
                if (frameCount==(int)cap.get(7)) {
                    frameCount = 2;
                    cap.set(1, 2);
                    Thread.sleep(500);
                    continue;
                }
            }

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


            int id = 0;

            for (MatOfPoint ch : contours) {
                Blob possibleBlob = new Blob(ch,id++);

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

            boolean blnAtLeastOneBlobCrossedTheLine = checkIfBlobsCrossedTheLine(blobs, intHorizontalLinePosition.intValue(),orientation,true);
            if (blnAtLeastOneBlobCrossedTheLine == true) {
                Core.line(imgFrame2Copy, crossingLine[0], crossingLine[1], SCALAR_GREEN, 2);
            }
            else {
                Core.line(imgFrame2Copy, crossingLine[0], crossingLine[1], SCALAR_RED, 2);
            }

            drawCarCountOnImage(carCount, imgFrame2Copy);

            image = toBufferedImage(imgFrame2Copy);

            //cv::waitKey(0);                 // uncomment this line to go frame by frame for debugging

            // now we prepare for the next iteration

            currentFrameBlobs.clear();

            imgFrame1 = imgFrame2.clone();           // move frame 1 up to where frame 2 is

            cap.read(imgFrame2);

            blnFirstFrame = false;
            frameCount++;
        }

    }


    private Image toBufferedImage(Mat m){
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

    private void matchCurrentFrameBlobsToExistingBlobs(List<Blob> existingBlobs, List<Blob> currentFrameBlobs) {

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
    private void addBlobToExistingBlobs(Blob currentFrameBlob, List<Blob> existingBlobs, int intIndex) {

        existingBlobs.get(intIndex).currentContour = currentFrameBlob.currentContour;
        existingBlobs.get(intIndex).currentBoundingRect = currentFrameBlob.currentBoundingRect;

        existingBlobs.get(intIndex).centerPositions.add(currentFrameBlob.centerPositions.get(currentFrameBlob.centerPositions.size() - 1));

        existingBlobs.get(intIndex).dblCurrentDiagonalSize = currentFrameBlob.dblCurrentDiagonalSize;
        existingBlobs.get(intIndex).dblCurrentAspectRatio = currentFrameBlob.dblCurrentAspectRatio;

        existingBlobs.get(intIndex).blnStillBeingTracked = true;
        existingBlobs.get(intIndex).blnCurrentMatchFoundOrNewBlob = true;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    private void addNewBlob(Blob currentFrameBlob, List<Blob> existingBlobs) {

        currentFrameBlob.blnCurrentMatchFoundOrNewBlob = true;

        existingBlobs.add(currentFrameBlob);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    private double distanceBetweenPoints(Point point1, Point point2) {

        double intX = Math.abs(point1.x - point2.x);
        double intY = Math.abs(point1.y - point2.y);

        return(Math.sqrt(Math.pow(intX, 2) + Math.pow(intY, 2)));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    private void drawAndShowContours(Size imageSize, List<MatOfPoint > contours, String strImageName) {
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
    private void drawAndShowContoursBlob(Size imageSize, List<Blob> blobs, String strImageName) {

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
    private boolean checkIfBlobsCrossedTheLine(List<Blob> blobs, int intHorizontalLinePosition,String orientation,boolean doCount) {
        boolean blnAtLeastOneBlobCrossedTheLine = false;

        for (Blob blob : blobs) {

            if (blob.blnStillBeingTracked == true && blob.centerPositions.size() >= 2) {
                int prevFrameIndex = (int)blob.centerPositions.size() - 2;
                int currFrameIndex = (int)blob.centerPositions.size() - 1;

                if(orientation.equals("vertical")){
                    //if(movement.equals("decreasing")) {
                        if (blob.centerPositions.get(prevFrameIndex).x > intHorizontalLinePosition && blob.centerPositions.get(currFrameIndex).x <= intHorizontalLinePosition) {
                            blnAtLeastOneBlobCrossedTheLine = count(blob.id,doCount);
                            carCountByMovement.put("decreasing",carCountByMovement.get("decreasing")+1);
                        }
                    //}else if (movement.equals("increasing")){
                        if (blob.centerPositions.get(prevFrameIndex).x < intHorizontalLinePosition && blob.centerPositions.get(currFrameIndex).x >= intHorizontalLinePosition) {
                            blnAtLeastOneBlobCrossedTheLine = count(blob.id,doCount);
                            carCountByMovement.put("increasing",carCountByMovement.get("increasing")+1);
                        }
                    //}
                }else if(orientation.equals("horizontal")) {
                    //if(movement.equals("increasing")) {
                        if (blob.centerPositions.get(prevFrameIndex).y > intHorizontalLinePosition && blob.centerPositions.get(currFrameIndex).y <= intHorizontalLinePosition) {
                            blnAtLeastOneBlobCrossedTheLine = count(blob.id,doCount);
                            carCountByMovement.put("increasing",carCountByMovement.get("increasing")+1);
                        }
                    //}else if (movement.equals("decreasing")){
                        if (blob.centerPositions.get(prevFrameIndex).y < intHorizontalLinePosition && blob.centerPositions.get(currFrameIndex).y >= intHorizontalLinePosition) {
                            blnAtLeastOneBlobCrossedTheLine = count(blob.id,doCount);
                            carCountByMovement.put("decreasing",carCountByMovement.get("decreasing")+1);
                        }
                    //}
                }
            }

        }

        return blnAtLeastOneBlobCrossedTheLine;
    }

    private Map<Integer,Long> timesPerBlob = new HashMap<Integer,Long>();
    private long time = 1l;

    private boolean count(int blobId,boolean doCount) {
        boolean blnAtLeastOneBlobCrossedTheLine = true;;
        if(doCount) {
            carCount++;
            long start = System.currentTimeMillis();
            timesPerBlob.put(blobId,start);
        }else{
            long end = System.currentTimeMillis();
            time = end - timesPerBlob.get(blobId);
        }

        return blnAtLeastOneBlobCrossedTheLine;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
     void drawBlobInfoOnImage(List<Blob> blobs, Mat imgFrame2Copy) {

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
     void drawCarCountOnImage(int carCount, Mat imgFrame2Copy) {

        int intFontFace = Core.FONT_HERSHEY_SIMPLEX;
        double dblFontScale = (imgFrame2Copy.rows() * imgFrame2Copy.cols()) / 300000.0;
        int intFontThickness = (int)Math.round(dblFontScale * 1.5);

        //Size textSize = Core.getTextSize(String.valueOf(carCount), intFontFace, dblFontScale, intFontThickness, new int[]{0});

         Size textSizeDec = Core.getTextSize(String.valueOf(carCountByMovement.get("decreasing")), intFontFace, dblFontScale, intFontThickness, new int[]{0});
         Size textSizeInc = Core.getTextSize(String.valueOf(carCountByMovement.get("increasing")), intFontFace, dblFontScale, intFontThickness, new int[]{0});

        Point ptTextBottomLeftPositionInc = new Point();

        ptTextBottomLeftPositionInc.x = imgFrame2Copy.cols() - 1 - (int)((double)textSizeInc.width * 1.25);
        ptTextBottomLeftPositionInc.y = (int)((double)textSizeInc.height * 1.25);

         Point ptTextBottomLeftPositionDec = new Point();

         ptTextBottomLeftPositionDec.x = 1;
         ptTextBottomLeftPositionDec.y = (int)((double)textSizeDec.height * 1.25);


        Core.putText(imgFrame2Copy, String.valueOf(carCountByMovement.get("increasing")), ptTextBottomLeftPositionInc, intFontFace, dblFontScale, SCALAR_GREEN, intFontThickness);
         Core.putText(imgFrame2Copy, String.valueOf(carCountByMovement.get("decreasing")), ptTextBottomLeftPositionDec, intFontFace, dblFontScale, SCALAR_RED, intFontThickness);

    }

}
