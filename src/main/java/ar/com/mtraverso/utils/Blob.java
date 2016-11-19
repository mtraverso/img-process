package ar.com.mtraverso.utils;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by matias on 31/10/16.
 */
public class Blob {

    public MatOfPoint currentContour;
    public double prevDistance;

    public Rect currentBoundingRect;

    public List<Point> centerPositions= new ArrayList<Point>();

    public double dblCurrentDiagonalSize;
    public double dblCurrentAspectRatio;

    public boolean blnCurrentMatchFoundOrNewBlob;

    public boolean blnStillBeingTracked;

    public int intNumOfConsecutiveFramesWithoutAMatch;

    public Point predictedNextPosition;

    public int id;

    public BigDecimal speed;

    public int frameFirstLine;
    public int frameSecondLine;


    public int getFrameFirstLine() {
        return frameFirstLine;
    }

    public void setFrameFirstLine(int frameFirstLine) {
        this.frameFirstLine = frameFirstLine;
    }

    public int getFrameSecondLine() {
        return frameSecondLine;
    }

    public void setFrameSecondLine(int frameSecondLine) {
        this.frameSecondLine = frameSecondLine;
    }

    public BigDecimal getSpeed() {
        return speed;
    }

    public void setSpeed(BigDecimal speed) {
        this.speed = speed;
    }

    public Blob(MatOfPoint contour, int id){
        this.id = id;
        currentContour = contour;
        currentBoundingRect = Imgproc.boundingRect(contour);
        Point currentCenter = new Point();

        currentCenter.x = (currentBoundingRect.x+ currentBoundingRect.x + currentBoundingRect.width)/2;
        currentCenter.y = (currentBoundingRect.y+ currentBoundingRect.y + currentBoundingRect.height)/2;

        centerPositions.add(currentCenter);

        dblCurrentDiagonalSize = Math.sqrt(Math.pow(currentBoundingRect.width, 2) + Math.pow(currentBoundingRect.height, 2));

        dblCurrentAspectRatio = (float)currentBoundingRect.width / (float)currentBoundingRect.height;


        blnStillBeingTracked = true;
        blnCurrentMatchFoundOrNewBlob = true;

        intNumOfConsecutiveFramesWithoutAMatch = 0;
    }

    public void predictNextPosition(){
        int numPositions = centerPositions.size();

        if(predictedNextPosition == null){
            predictedNextPosition = new Point();
        }

        if (numPositions == 1) {

            predictedNextPosition.x = centerPositions.get(0).x;
            predictedNextPosition.y = centerPositions.get(0).y;

        }
        else if (numPositions == 2) {

            double deltaX = centerPositions.get(1).x - centerPositions.get(0).x;
            double deltaY = centerPositions.get(1).y - centerPositions.get(0).y;

            predictedNextPosition.x = centerPositions.get(numPositions-1).x + deltaX;
            predictedNextPosition.y = centerPositions.get(numPositions-1).y + deltaY;

        }
        else if (numPositions == 3) {

            double sumOfXChanges = ((centerPositions.get(2).x - centerPositions.get(1).x) * 2) +
                    ((centerPositions.get(1).x - centerPositions.get(0).x) * 1);

            long deltaX = Math.round((float) sumOfXChanges / 3.0);

            double sumOfYChanges = ((centerPositions.get(2).y - centerPositions.get(1).y) * 2) +
                    ((centerPositions.get(1).y - centerPositions.get(0).y) * 1);

            long deltaY = Math.round((float) sumOfYChanges / 3.0);

            predictedNextPosition.x = centerPositions.get(numPositions-1).x + deltaX;
            predictedNextPosition.y = centerPositions.get(numPositions - 1).y + deltaY;

        }
        else if (numPositions == 4) {

            double sumOfXChanges = ((centerPositions.get(3).x - centerPositions.get(2).x) * 3) +
                    ((centerPositions.get(2).x - centerPositions.get(1).x) * 2) +
                    ((centerPositions.get(1).x - centerPositions.get(0).x) * 1);

            long deltaX = Math.round((float)sumOfXChanges / 6.0);

            double sumOfYChanges = ((centerPositions.get(3).y - centerPositions.get(2).y) * 3) +
                    ((centerPositions.get(2).y - centerPositions.get(1).y) * 2) +
                    ((centerPositions.get(1).y - centerPositions.get(0).y) * 1);

            long deltaY = Math.round((float) sumOfYChanges / 6.0);

            predictedNextPosition.x = centerPositions.get(numPositions-1).x + deltaX;
            predictedNextPosition.y = centerPositions.get(numPositions-1).y + deltaY;

        }
        else if (numPositions >= 5) {

            double sumOfXChanges = ((centerPositions.get(numPositions - 1).x - centerPositions.get(numPositions - 2).x) * 4) +
                    ((centerPositions.get(numPositions - 2).x - centerPositions.get(numPositions - 3).x) * 3) +
                    ((centerPositions.get(numPositions - 3).x - centerPositions.get(numPositions - 4).x) * 2) +
                    ((centerPositions.get(numPositions - 4).x - centerPositions.get(numPositions - 5).x) * 1);

            long deltaX = Math.round((float)sumOfXChanges / 10.0);

            double sumOfYChanges = ((centerPositions.get(numPositions - 1).y - centerPositions.get(numPositions - 2).y) * 4) +
                    ((centerPositions.get(numPositions - 2).y - centerPositions.get(numPositions - 3).y) * 3) +
                    ((centerPositions.get(numPositions - 3).y - centerPositions.get(numPositions - 4).y) * 2) +
                    ((centerPositions.get(numPositions - 4).y - centerPositions.get(numPositions - 5).y) * 1);

            long deltaY = Math.round((float)sumOfYChanges / 10.0);

            predictedNextPosition.x = centerPositions.get(numPositions-1).x + deltaX;
            predictedNextPosition.y = centerPositions.get(numPositions-1).y + deltaY;

        }
        else {
            // should never get here
        }
    }
}
