package ar.com.mtraverso;

import ar.com.mtraverso.observable.*;
import ar.com.mtraverso.observable.Observable;
import ar.com.mtraverso.observable.Observer;
import ar.com.mtraverso.utils.Blob;
import jersey.repackaged.com.google.common.base.Stopwatch;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

/**
 * Created by matias on 31/10/16.
 */
public class CarCounter implements Observable {

    static
    {
        OpenCV.loadLocally();
    }
    private static Scalar SCALAR_BLACK = new Scalar(0.0, 0.0, 0.0);
    private static Scalar SCALAR_WHITE = new Scalar(255.0, 255.0, 255.0);
    private static Scalar SCALAR_YELLOW = new Scalar(0.0, 255.0, 255.0);
    private static Scalar SCALAR_GREEN = new Scalar(0.0, 200.0, 0.0);
    private static Scalar SCALAR_RED = new Scalar(0.0, 0.0, 255.0);

    private static CarCounter instance;

    public static CarCounter getInstance(boolean repeat){
        if(instance == null){
            instance = new CarCounter(repeat);
        }
        return instance;
    }

    private CarCounter(boolean repeat){
        this.repeat = repeat;
    }

    private boolean repeat;

    private int carCount = 0;

    private Image image;

    private boolean running;

    private String orientation="horizontal";

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

    Point[] crossingLine = new Point[2];
    //Point[] crossingLine2 = new Point[2];
    double linePosition;
    //double linePosition2;
    double frameCols;
    double frameRows;

    int fps;

    double avgRealWidth;
    double focalLength;
    private List<BufferedImage> images = new ArrayList<BufferedImage>();

    public void initCapture(String videoName,String orientation, String position, double focalLength, int fps) throws InterruptedException {
        this.orientation = orientation;
        this.focalLength = focalLength;
        this.fps = fps;

        Properties props = new Properties();
        try {
            props.load(new FileInputStream("app.properties"));
            avgRealWidth = Double.valueOf(props.getProperty("avg.object.width"));
        }catch (IOException ioe){
            ioe.printStackTrace();
        }

        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

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



        cap.read(imgFrame1);
        cap.read(imgFrame2);





        Long intHorizontalLinePosition = Math.round((double)Double.valueOf(position));
        this.linePosition = intHorizontalLinePosition;
        /*Long intHorizontalLinePosition2 = Math.round((double)Double.valueOf(position2));
        this.linePosition2 = intHorizontalLinePosition2;*/

        frameCols = imgFrame1.cols();
        frameRows = imgFrame1.rows();

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


            boolean blnAtLeastOneBlobCrossedTheLine = checkIfBlobsCrossedTheLine(blobs, linePosition, true,imgFrame2Copy);
            if (blnAtLeastOneBlobCrossedTheLine == true) {
                Core.line(imgFrame2Copy, crossingLine[0], crossingLine[1], SCALAR_GREEN, 2);

            }
            else {
                Core.line(imgFrame2Copy, crossingLine[0], crossingLine[1], SCALAR_RED, 2);
            }

            drawCarCountOnImage(carCount, imgFrame2Copy);
            int intFontFace = Core.FONT_HERSHEY_SIMPLEX;
            double dblFontScale = 1;
            int intFontThickness = (int)Math.round(dblFontScale * 2.0);
            Core.putText(imgFrame2Copy,stopwatch.toString(),new Point(50,70),intFontFace,dblFontScale,SCALAR_GREEN,intFontThickness);
            Core.putText(imgFrame2Copy,Integer.toString(frameCount),new Point(200,70),intFontFace,dblFontScale,SCALAR_GREEN,intFontThickness);

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
    private void calculateSpeed(Blob blob, Mat frame){
        int width = blob.currentBoundingRect.width;

        //(WxF)/P
        double distance = (avgRealWidth * focalLength ) / width;
        if(blob.prevDistance != 0) {
            double delta = Math.abs(distance - blob.prevDistance);
            double speed = delta  * 3.6 * fps ;
            blob.setSpeed(new BigDecimal(speed));
            blob.prevDistance = distance;
        }else{
            blob.prevDistance = distance;
        }
        //drawBlobInfoOnImage(blob, frame);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean checkIfBlobsCrossedTheLine(List<Blob> blobs,double linePosition, boolean doCount, Mat frame) {
        boolean blnAtLeastOneBlobCrossedTheLine = false;

        for (Blob blob : blobs) {

            if (blob.blnStillBeingTracked == true && blob.centerPositions.size() >= 2) {
                int prevFrameIndex = (int)blob.centerPositions.size() - 2;
                int currFrameIndex = (int)blob.centerPositions.size() - 1;

                calculateSpeed(blob,frame);

                if(orientation.equals("vertical")){
                    //if(movement.equals("decreasing")) {
                        if (blob.centerPositions.get(prevFrameIndex).x > linePosition && blob.centerPositions.get(currFrameIndex).x <= linePosition) {
                            String movement = "decreasing";
                            blnAtLeastOneBlobCrossedTheLine = handleLineCrossing(doCount, blob, currFrameIndex, movement,frame);
                        }
                    //}else if (movement.equals("increasing")){
                        if (blob.centerPositions.get(prevFrameIndex).x < linePosition && blob.centerPositions.get(currFrameIndex).x >= linePosition) {
                            String movement = "increasing";
                            blnAtLeastOneBlobCrossedTheLine = handleLineCrossing(doCount, blob, currFrameIndex, movement,frame);
                        }
                    //}
                }else if(orientation.equals("horizontal")) {
                    //if(movement.equals("increasing")) {
                        if (blob.centerPositions.get(prevFrameIndex).y > linePosition && blob.centerPositions.get(currFrameIndex).y <= linePosition) {
                            String movement = "increasing";
                            blnAtLeastOneBlobCrossedTheLine = handleLineCrossing(doCount, blob, currFrameIndex, movement,frame);
                        }
                    //}else if (movement.equals("decreasing")){
                        if (blob.centerPositions.get(prevFrameIndex).y < linePosition && blob.centerPositions.get(currFrameIndex).y >= linePosition) {
                            String movement = "decreasing";
                            blnAtLeastOneBlobCrossedTheLine = handleLineCrossing(doCount, blob, currFrameIndex, movement,frame);
                        }
                    //}

                }
            }

        }

        return blnAtLeastOneBlobCrossedTheLine;
    }

    private boolean handleLineCrossing(boolean doCount, Blob blob, int currFrameIndex, String movement, Mat frame) {
        boolean blnAtLeastOneBlobCrossedTheLine;
        blnAtLeastOneBlobCrossedTheLine = count(blob.id,doCount);
        carCountByMovement.put(movement,carCountByMovement.get(movement)+1);
        writeImage(blob,frame);
        return blnAtLeastOneBlobCrossedTheLine;
    }

    private void writeImage(Blob blob,Mat frame){
        if(blob != null && blob.currentContour != null){
            Rect rect = blob.currentBoundingRect;
            Mat roi = frame.submat(rect);
            notifyObservers((BufferedImage)toBufferedImage(roi));
        }

    }

    private Map<Integer,Long> timesPerBlob = new HashMap<Integer,Long>();
    private long time = 1l;

    private boolean count(int blobId,boolean doCount) {
        boolean blnAtLeastOneBlobCrossedTheLine = true;;
        if(!doCount) {
            carCount++;
            long start = System.currentTimeMillis();
            timesPerBlob.put(blobId,start);
        }else{
            long end = System.currentTimeMillis();
            if(timesPerBlob.get(blobId) != null){
                time = end - timesPerBlob.get(blobId);
                timesPerBlob.put(blobId,time);
            }

        }

        return blnAtLeastOneBlobCrossedTheLine;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
     void drawBlobInfoOnImage(Blob blob, Mat imgFrame2Copy) {


            if (blob.blnStillBeingTracked == true) {
                //Core.rectangle(imgFrame2Copy, blobs.get(i).currentBoundingRect.br(), blobs.get(i).currentBoundingRect.tl(), SCALAR_RED, 2);

                int intFontFace = Core.FONT_HERSHEY_SIMPLEX;
                double dblFontScale = blob.dblCurrentDiagonalSize / 200.0;
                int intFontThickness = (int)Math.round(dblFontScale * 2.0);

                if(blob.getSpeed() != null) {
                    Core.putText(imgFrame2Copy, blob.getSpeed().setScale(2,BigDecimal.ROUND_HALF_UP).toString()+" km/h", blob.currentBoundingRect.tl(), intFontFace, dblFontScale, SCALAR_GREEN, intFontThickness);
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

        ptTextBottomLeftPositionInc.x = imgFrame2Copy.cols() - 250 - (int)((double)textSizeInc.width );
        ptTextBottomLeftPositionInc.y = (int)((double)textSizeInc.height * 1.50);

         Point ptTextBottomLeftPositionDec = new Point();

         ptTextBottomLeftPositionDec.x = 1;
         ptTextBottomLeftPositionDec.y = (int)((double)textSizeDec.height * 1.50);


        Core.putText(imgFrame2Copy, String.valueOf(carCountByMovement.get("increasing")), ptTextBottomLeftPositionInc, intFontFace, dblFontScale, SCALAR_GREEN, intFontThickness);
         Core.putText(imgFrame2Copy, String.valueOf(carCountByMovement.get("decreasing")), ptTextBottomLeftPositionDec, intFontFace, dblFontScale, SCALAR_RED, intFontThickness);

    }

    public void setCrossingLine(int y, int id){
        if(id == 1) {
            Point p11 = crossingLine[0];
            Point p12 = crossingLine[1];
            linePosition = y;
            if (orientation.equals("horizontal")) {
                p11 = new Point(0, linePosition);
                p12 = new Point(frameCols, linePosition);
            } else if (orientation.equals("vertical")) {
                p11 = new Point(linePosition, 0);
                p12 = new Point(linePosition, frameRows);
            }
            crossingLine[0] = p11;
            crossingLine[1] = p12;
        }
    }

    public void setOrientation(String orientation){
        this.orientation = orientation;
        Point p11 = crossingLine[0];
        Point p12 = crossingLine[1];

        if(orientation.equals("horizontal")){
            p11 = new Point(0, linePosition);
            p12 = new Point(frameCols, linePosition);
        }else if (orientation.equals("vertical")){
            p11 = new Point(linePosition,0);
            p12 = new Point(linePosition,frameRows);
        }
        crossingLine[0] = p11;
        crossingLine[1] = p12;
    }

    List<Observer> observers = new ArrayList<Observer>();

    @Override
    public void registerObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void notifyObservers(Image i) {
        for(Observer o : observers){
            o.update(i);
        }
    }
}
