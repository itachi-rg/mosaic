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
import static org.bytedeco.javacpp.opencv_imgproc.cvResize;
import static org.bytedeco.javacpp.opencv_imgproc.cvSmooth;

/**
 * Created by rg on 25-Nov-16.
 */
public class SampleCode {
    public static void smooth(String filename) {
        IplImage image = cvLoadImage(filename);
        if (image != null) {
            cvSmooth(image, image);
            cvSaveImage(filename, image);
            cvReleaseImage(image);
        }
    }

    public static void main(String[] args) throws Exception {
        String compLoc = args[0];
        String sourceLoc = args[1];
        int compWidth = args[2]!=null ? Integer.parseUnsignedInt(args[2]) : 30;
        int compHeight = args[3]!=null ? Integer.parseUnsignedInt(args[3]) : 20;

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
            componentImages.add(imageDetails);

            cvReleaseImage(comImageresized);
            cvReleaseImage(componentImage);
        }



        IplImage srcOrigImage = null;
        System.out.println("Getting all files in " + sourceLoc + " including those in subdirectories");
        List<File> files1 = (List<File>) FileUtils.listFiles(new File(sourceLoc), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        for (File file : files1) {
            System.out.println("file: " + file.getCanonicalPath());
            srcOrigImage = cvLoadImage(file.getCanonicalPath());
        }


        int origWidth = srcOrigImage.width();
        int origHeight = srcOrigImage.height();

        IplImage srcImage = IplImage.create(srcOrigImage.width()*3, srcOrigImage.height()*3, srcOrigImage.depth(), srcOrigImage.nChannels());

        cvSmooth(srcOrigImage, srcOrigImage);

        cvResize(srcOrigImage, srcImage);



        int modifiedSrcWidth = (srcImage.width()/compWidth)*compWidth;
        int modifiedSrcHeight = (srcImage.height()/compHeight)*compHeight;

        int regionCols = modifiedSrcWidth/compWidth;
        int regionRows =  modifiedSrcHeight/compHeight;
        double[][][] regions = new double[regionCols][regionRows][3];



/*        IplImage compImage2 = cvLoadImage("C:\\googs\\mosaic\\src\\main\\resources\\grey.jpg");
        IplImage comImage2resized = IplImage.create(compWidth, compHeight, compImage2.depth(), compImage2.nChannels());
        cvResize(compImage2, comImage2resized);*/

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
//                        System.out.println("i : " + i + " : j : " + j + " : x+compWidth " + (x+compWidth) + " : y+compHeigth " + (y+compHeight)) ;
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

/*        for (int i=0;i<regionRows;i++) {
            for (int j=0;j<regionCols;j++) {
                int val = (int) regions[j][i];
//                System.out.print(val+" ");
                if (val < 130) {
                    System.out.print("1 ");
                } else {
                    System.out.print("0 ");
                }
            }
            System.out.println();
        }*/
        System.out.println("image " + imageCount);

        IplImage targetImage = IplImage.create(modifiedSrcWidth, modifiedSrcHeight, srcImage.depth(), srcImage.nChannels());

        for (int i=0;i<regionRows;i++) {
            for (int j=0;j<regionCols;j++) {

                IplImage suitableImage = getSuitableImage(regions[j][i], componentImages);
                cvSetImageROI(targetImage, cvRect((j*compWidth),(i*compHeight),compWidth,compHeight));
                System.out.println(" Target depth " + targetImage.depth() + " width " + compWidth + " height " + compHeight);
                System.out.println(" Suitable image depth " + suitableImage.depth() + " width " + suitableImage.width() + " height " + suitableImage.height());
                cvCopy(suitableImage, targetImage);
                cvReleaseImage(suitableImage);


/*                int val = (int) regions[j][i];
//                System.out.print(val+" ");
                if (val < 130) {
                    System.out.print("1 ");
                    cvSetImageROI(targetImage, cvRect(j*compWidth,i*compHeight,compWidth,compHeight));
                    cvCopy(componentImages.get(0).getModifiedImage(), targetImage);
                } else {
                    System.out.print("0 ");
                    cvSetImageROI(targetImage, cvRect(j*compWidth,i*compHeight,compWidth,compHeight));
                    cvCopy(componentImages.get(2).getModifiedImage(), targetImage);
                }*/
            }
            System.out.println();
        }

        cvResetImageROI(targetImage);

        cvSaveImage("C:\\googs\\mosaic\\src\\main\\resources\\modifiedImage.jpg", targetImage);
        cvSaveImage("C:\\googs\\mosaic\\src\\main\\resources\\modifiedImage_1.jpg", targetImage);
        /*cvSetImageROI(image, cvRect(0,0,120,80));
        cvCopy(resizeImage, image);

        cvResetImageROI(image);

        if (resizeImage != null) {
            cvSaveImage("C:\\googs\\mosaic\\src\\main\\resources\\resized.jpg", resizeImage);
            cvSaveImage("C:\\googs\\mosaic\\src\\main\\resources\\modifiedImage.jpg", image);
            cvReleaseImage(image);
            cvReleaseImage(resizeImage);

        }*/
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

        if (suitableImageDetails == null) {
            throw new AssertionError();
        }

        IplImage componentImage = cvLoadImage(suitableImageDetails.getImageFile().getCanonicalPath());
        IplImage comImageresized = IplImage.create(suitableImageDetails.getResizedWidth(), suitableImageDetails.getResizedHeight(), componentImage.depth(), componentImage.nChannels());
        cvResize(componentImage, comImageresized);
        cvReleaseImage(componentImage);

        return comImageresized;
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
