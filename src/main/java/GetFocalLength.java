import ar.com.mtraverso.utils.Blob;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * Created by mtraverso on 11/4/16.
 */
public class GetFocalLength {

    private static Scalar SCALAR_RED = new Scalar(0.0, 0.0, 255.0);
    private static Scalar SCALAR_GREEN = new Scalar(0.0, 200.0, 0.0);

    static
    {
        OpenCV.loadLocally();
    }

    public static void main(String[] args) throws  IOException{
        Properties prop = new Properties();
        File propertiesFile = new File("app.properties");
        InputStream input = new FileInputStream(propertiesFile);
        prop.load(input);
        input.close();

        String videoName = prop.getProperty("video.filename");

        double d = Double.valueOf(prop.getProperty("object.distance"));
        double w = Double.valueOf(prop.getProperty("object.width"));

        VideoCapture cap = new VideoCapture(videoName);

        Mat image = new Mat();
        cap.read(image);

        Mat gray = new Mat();
        Mat edged = new Mat();

        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, gray , new Size(5, 5), 0);
        Imgproc.Canny(gray,edged, 35, 125);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(edged.clone(), contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        MatOfPoint max =  contours.stream().max(Comparator.comparing(p -> Imgproc.boundingRect(p).area())).get();

        MatOfPoint2f  maxMat = new MatOfPoint2f( max.toArray() );

        Imgproc.minAreaRect(maxMat);



        MatOfPoint pt = new MatOfPoint();

        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(maxMat,approx,0,true);
        approx.convertTo(pt,CvType.CV_32S);
        List<MatOfPoint> contourTemp = new ArrayList<MatOfPoint>();
        contourTemp.add(pt);

        Rect rect = Imgproc.boundingRect(pt);

        Core.rectangle(image, rect.br(),rect.tl(),SCALAR_GREEN);

        Highgui.imwrite("test1.jpg",image);

        System.out.println(rect.width);


        double p = rect.width;

        Double focalLength = (d*p)/w;

        OutputStream op = new FileOutputStream(propertiesFile);
        prop.put("camera.focal.length",focalLength.toString());

        prop.store(op,"Saved");

    }
}
