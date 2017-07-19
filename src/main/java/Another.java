import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.bytedeco.javacpp.indexer.UByteRawIndexer;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;

/**
 * Created by rg on 25-Nov-16.
 */
public class Another {

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
            System.out.println("Component file: " + file.getCanonicalPath());
            ImageDetails imageDetails = new ImageDetails(file);

            Mat componentImage = imread(file.getCanonicalPath());
            Mat comImageresized = new Mat();

            imageDetails.setResizedWidth(compWidth);
            imageDetails.setResizedHeight(compHeight);
            imageDetails.setResizedDepth(componentImage.depth());

            resize(componentImage, comImageresized, new Size(compWidth, compHeight));

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
            imageDetails.setImg(comImageresized);

            componentImages.add(imageDetails);

            componentImage.release();
        }



        Mat srcOrigImage = null;
        System.out.println("Getting all files in " + sourceLoc + " including those in subdirectories");
        List<File> files1 = (List<File>) FileUtils.listFiles(new File(sourceLoc), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        for (File file : files1) {
            System.out.println("file: " + file.getCanonicalPath());
            srcOrigImage = imread(file.getCanonicalPath());
        }

        Mat srcImage = new Mat();
        resize(srcOrigImage, srcImage, new Size(srcOrigImage.cols()*scale, srcOrigImage.rows()*scale));

        int modifiedSrcWidth = (srcImage.cols()/compWidth)*compWidth; // Done for rounding off, otherwise placing images till the end will get hit
        int modifiedSrcHeight = (srcImage.rows()/compHeight)*compHeight;

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

                try {
                    for (int i = x; i < x + compWidth; i++) {
                        for (int j = y; j < y + compHeight; j++) {
                            pixelSum[0] += indexer.get(j, i, 0);
                            pixelSum[1] += indexer.get(j, i, 1);
                            pixelSum[2] += indexer.get(j, i, 2);

                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    System.out.println("Reduce the scale. The size of image is too big to allocate memory");
                    throw e;
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

        Mat targetImage = new Mat(modifiedSrcHeight, modifiedSrcWidth, CV_8UC3);
        Mat overlay = new Mat(modifiedSrcHeight, modifiedSrcWidth, CV_8UC3);

        for (int i=0;i<regionRows;i++) {
            for (int j=0;j<regionCols;j++) {

                Mat suitableImage = getSuitableImage(regions[j][i], componentImages);
                Mat subRegion = targetImage.apply(new Rect((j*compWidth),(i*compHeight),compWidth,compHeight));
                suitableImage.copyTo(subRegion);
            }
        }

        imwrite("C:\\googs\\mosaic\\src\\main\\resources\\result\\1.jpg", targetImage);

        srcImage = srcImage.apply(new Rect(0,0,targetImage.cols(), targetImage.rows()));

        addWeighted(srcImage, 0.5, targetImage, 0.4, 0.0, overlay);

        imwrite("C:\\googs\\mosaic\\src\\main\\resources\\result\\2.jpg", overlay);

        Mat bright = addPut(overlay, new Scalar(50,50,50,50));
        imwrite("C:\\googs\\mosaic\\src\\main\\resources\\result\\3.jpg", bright);
    }

    static Mat getSuitableImage(double[] currentAverage, List<ImageDetails> imageDetailsList) throws IOException {
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
        return suitableImageDetails.getImg();
    }

}
