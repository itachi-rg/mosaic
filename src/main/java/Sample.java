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

/**
 * Created by rg on 25-Nov-16.
 */
public class Sample {

    public static void main(String[] args) throws Exception {
        String compLoc = args[0];
        String sourceLoc = args[1];
        int compWidth = args[2]!=null ? Integer.parseUnsignedInt(args[2]) : 30;
        int compHeight = args[3]!=null ? Integer.parseUnsignedInt(args[3]) : 20;
        int scale = args[4]!=null ? Integer.parseUnsignedInt(args[4]) : 1;

        List<ImageDetails> componentImages = new LinkedList<ImageDetails>();

        System.out.println("Getting all files in " + compLoc + " including those in subdirectories");
        List<File> files = (List<File>) FileUtils.listFiles(new File(compLoc), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        for (File file : files) {
            System.out.println("file: " + file.getCanonicalPath());

            ImageDetails imageDetails = new ImageDetails(file);

            IplImage componentImage = cvLoadImage(file.getCanonicalPath());
            IplImage comImageresized = IplImage.create(compWidth, compHeight, componentImage.depth(), componentImage.nChannels());


            imageDetails.setResizedWidth(compWidth);
            imageDetails.setResizedHeight(compHeight);
            imageDetails.setResizedDepth(componentImage.depth());
            imageDetails.setResizedChannels(componentImage.nChannels());

            cvResize(componentImage, comImageresized);

            double[] pixelSum = new double[3];
            double[] pixelAverage = new double[3];
            UByteRawIndexer indexer = comImageresized.createIndexer();
            for (int i = 0; i < compWidth; i++) {
                for (int j = 0; j < compHeight; j++) {
                    pixelSum[0] += indexer.get(j,i,0);
                    pixelSum[1] += indexer.get(j,i,1);
                    pixelSum[2] += indexer.get(j,i,2);
                }
            }

            pixelAverage[0] = pixelSum[0]/(compHeight*compWidth);
            pixelAverage[1] = pixelSum[1]/(compHeight*compWidth);
            pixelAverage[2] = pixelSum[2]/(compHeight*compWidth);

            imageDetails.setPixelAverage(pixelAverage);
            imageDetails.setImage(comImageresized);

            componentImages.add(imageDetails);

            cvReleaseImage(componentImage);
        }



        IplImage srcOrigImage = null;
        System.out.println("Getting all files in " + sourceLoc + " including those in subdirectories");
        List<File> files1 = (List<File>) FileUtils.listFiles(new File(sourceLoc), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        for (File file : files1) {
            System.out.println("file: " + file.getCanonicalPath());
            srcOrigImage = cvLoadImage(file.getCanonicalPath());
        }
/*

        IplImage transparentImage = IplImage.create(srcOrigImage.width(), srcOrigImage.height(), srcOrigImage.depth(), 4);
        cvCvtColor(srcOrigImage, transparentImage, CV_RGB2RGBA);
        transparentImage.alphaChannel(200);
        cvSaveImage("C:\\googs\\mosaic\\src\\main\\resources\\target4\\trans1.jpg", transparentImage);

        transparentImage.alphaChannel();


        IplImage transparentImage1 = IplImage.create(srcOrigImage.width(), srcOrigImage.height(), srcOrigImage.depth(), 4);
        cvCopy(transparentImage, transparentImage1);

        transparentImage1.alphaChannel(0);

*/



//        cvSaveImage("C:\\googs\\mosaic\\src\\main\\resources\\target4\\trans2.jpg", transparentImage1);
        int origWidth = srcOrigImage.width();
        int origHeight = srcOrigImage.height();

        IplImage srcImage = IplImage.create(srcOrigImage.width()*scale, srcOrigImage.height()*scale, srcOrigImage.depth(), srcOrigImage.nChannels());
        cvResize(srcOrigImage, srcImage);

        int modifiedSrcWidth = (srcImage.width()/compWidth)*compWidth; // Done for rounding off, otherwise placing images till the end will get hit
        int modifiedSrcHeight = (srcImage.height()/compHeight)*compHeight;

        int regionCols = modifiedSrcWidth/compWidth;
        int regionRows =  modifiedSrcHeight/compHeight;
        double[][][] regions = new double[regionCols][regionRows][3];


        int x = 0;
        int y = 0;
        int imageCount = 0;
        int regionXindex = 0;
        int regionYindex = 0;

        UByteRawIndexer indexer = srcImage.createIndexer();

        do {
            do {
                double[] pixelSum = new double[3];
                double[] pixelAverage = new double[3];

                for (int i = x; i < x + compWidth; i++) {
                    for (int j = y; j < y + compHeight; j++) {
                        pixelSum[0] += indexer.get(j,i,0);
                        pixelSum[1] += indexer.get(j,i,1);
                        pixelSum[2] += indexer.get(j,i,2);

                    }
                }

                pixelAverage[0] = pixelSum[0]/(compHeight*compWidth);
                pixelAverage[1] = pixelSum[1]/(compHeight*compWidth);
                pixelAverage[2] = pixelSum[2]/(compHeight*compWidth);

                System.out.println("regionXindex : " + regionXindex + " : regionYindex : " + regionYindex + " : regionRows " + regionRows + " : regioncols " + regionCols) ;
                regions[regionXindex][regionYindex][0] = pixelAverage[0];
                regions[regionXindex][regionYindex][1] = pixelAverage[1];
                regions[regionXindex][regionYindex][2] = pixelAverage[2];

                y += compHeight;
                regionYindex+=1;
                System.out.println("image " + imageCount++);
                System.out.println("regionAverage " + pixelAverage);

            } while (y < modifiedSrcHeight);
            x += compWidth;
            y = 0;
            regionYindex=0;
            regionXindex+=1;
        } while (x < modifiedSrcWidth);

        System.out.println("image " + imageCount);

        IplImage targetImage = IplImage.create(modifiedSrcWidth, modifiedSrcHeight, srcImage.depth(), srcImage.nChannels());

        for (int i=0;i<regionRows;i++) {
            for (int j=0;j<regionCols;j++) {

                IplImage suitableImage = getSuitableImage2(regions[j][i], componentImages);
                cvSetImageROI(targetImage, cvRect((j*compWidth),(i*compHeight),compWidth,compHeight));
                System.out.println(" Target depth " + targetImage.depth() + " width " + compWidth + " height " + compHeight);
                System.out.println(" Suitable image depth " + suitableImage.depth() + " width " + suitableImage.width() + " height " + suitableImage.height());
                cvCopy(suitableImage, targetImage);

            }
            System.out.println();
        }

        cvResetImageROI(targetImage);

        cvSaveImage("C:\\googs\\mosaic\\src\\main\\resources\\result\\1.jpg", targetImage);

        IplImage img3 = IplImage.create(modifiedSrcWidth, modifiedSrcHeight, targetImage.depth(), targetImage.nChannels());

        Mat im1 = cvarrToMat(srcImage);


        Mat im2 = cvarrToMat(targetImage);
        Mat im3 = cvarrToMat(img3);

        Mat im = im1.apply(new Rect(0,0,im2.cols(), im2.rows()));

        addWeighted(im, 0.5, im2, 0.3, 0.0, im3);


        imwrite("C:\\googs\\mosaic\\src\\main\\resources\\result\\2.jpg", im3);
    }

    static IplImage getSuitableImage(double[] currentAverage, List<ImageDetails> imageDetailsList) throws IOException {
        ImageDetails suitableImageDetails = null;

        double lowestAverage = 10000;
        for (ImageDetails imageDetails: imageDetailsList) {
            int[] diff = new int[3];
            diff[0] = (int) ((imageDetails.getPixelAverage())[0] - currentAverage[0]);
            diff[1] = (int) ((imageDetails.getPixelAverage())[1] - currentAverage[1]);
            diff[2] = (int) ((imageDetails.getPixelAverage())[2] - currentAverage[2]);

            int absoluteDiff = Math.abs(diff[0]) + Math.abs(diff[1]) + Math.abs(diff[2]);
            if (absoluteDiff < lowestAverage) {
                lowestAverage = absoluteDiff;
                suitableImageDetails = imageDetails;
            }
        }

        IplImage componentImage = cvLoadImage(suitableImageDetails.getImageFile().getCanonicalPath());
        IplImage comImageresized = IplImage.create(suitableImageDetails.getResizedWidth(), suitableImageDetails.getResizedHeight(), componentImage.depth(), componentImage.nChannels());
        cvResize(componentImage, comImageresized);
        cvReleaseImage(componentImage);

        return comImageresized;
    }

    static IplImage getSuitableImage2(double[] currentAverage, List<ImageDetails> imageDetailsList) throws IOException {
        ImageDetails suitableImageDetails = null;

        double bestScore = -1;
        for (ImageDetails imageDetails: imageDetailsList) {
            int[] diff = new int[3];
            diff[0] = (int) ((imageDetails.getPixelAverage())[0] - currentAverage[0]);
            diff[1] = (int) ((imageDetails.getPixelAverage())[1] - currentAverage[1]);
            diff[2] = (int) ((imageDetails.getPixelAverage())[2] - currentAverage[2]);

            int absoluteDiff = 255*3 - (Math.abs(diff[0]) + Math.abs(diff[1]) + Math.abs(diff[2]));
            if (absoluteDiff > bestScore) {
                bestScore = absoluteDiff;
                suitableImageDetails = imageDetails;
            }
        }
        return suitableImageDetails.getImage();
    }

    static IplImage getSuitableImage1(double[] currentAverage, List<ImageDetails> imageDetailsList) {
        IplImage suitableImage = null;

        double bestScore = -1;
        for (ImageDetails imageDetails: imageDetailsList) {
            int[] diff = new int[3];
            diff[0] = (int) ((imageDetails.getPixelAverage())[0] - currentAverage[0]);
            diff[1] = (int) ((imageDetails.getPixelAverage())[1] - currentAverage[1]);
            diff[2] = (int) ((imageDetails.getPixelAverage())[2] - currentAverage[2]);

            int absoluteDiff = 255*3 - (Math.abs(diff[0]) + Math.abs(diff[1]) + Math.abs(diff[2]));
            if (absoluteDiff > bestScore) {
                bestScore = absoluteDiff;
            }
        }
        return suitableImage;
    }

}
