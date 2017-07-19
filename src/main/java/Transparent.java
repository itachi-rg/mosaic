/**
 * Created by rg on 26-Dec-16.
 */

import org.bytedeco.javacpp.opencv_core;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.bytedeco.javacpp.indexer.UByteRawIndexer;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvSaveImage;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;
import static org.bytedeco.javacpp.opencv_imgproc.CV_RGB2RGBA;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.cvResize;

public class Transparent {
    public static void main(String[] args) throws IOException {
        File file1 = new File("C:\\googs\\mosaic\\src\\main\\resources\\transparent\\mosaic.jpg");
        File file2 = new File("C:\\googs\\mosaic\\src\\main\\resources\\transparent\\PM2_0059.JPG");
//        IplImage orig = cvLoadImage(origFile.getCanonicalPath());
/*
        Mat some = imread(origFile.getCanonicalPath(), CV_LOAD_IMAGE_UNCHANGED);

        Mat some1 = new Mat(some.rows(), some.cols(), CV_8UC4, cvScalar(0,0,0,0.5));

        some.copyTo(some1);

        System.out.println("Channels " + some.channels());
*/

        IplImage img1 = cvLoadImage(file1.getCanonicalPath());
        IplImage img2 = cvLoadImage(file2.getCanonicalPath());

        IplImage img3 = IplImage.create(img1.width(), img1.height(), img1.depth(), img1.nChannels());

        Mat im1 = cvarrToMat(img1);
        Mat im2 = cvarrToMat(img2);
        Mat im3 = cvarrToMat(img3);

        addWeighted(im1, 0.5, im2, 0.5, 0.0, im3);

        imwrite("C:\\googs\\mosaic\\src\\main\\resources\\transparent\\z.jpg", im3);

        System.out.println("Done");
    }
}
