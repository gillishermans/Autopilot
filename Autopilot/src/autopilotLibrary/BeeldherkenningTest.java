package autopilotLibrary;

import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import interfaces.AutopilotConfig;

public class BeeldherkenningTest {

	//Grootte van de afbeelding
		private static int imageWidth = 200;
		private static int imageHeight = 200;
		//Focal length van de camera
		private static float focalLength = 0.01f;
		//Size of the object -> de diagonaal van de kubus
		// -> in een ideale situatie komt de diagonaal overeen met de diameter van de min enclosing circle
		private static float objectSize = 1f;
		//min enclosing circle parameters
	    private static float[] radius = new float[10];
		
		//Het centrum van de afbeelding
		private static Point screenCenter = new Point(imageWidth/2,imageHeight/2);
		private static Point center = new Point(screenCenter.x,screenCenter.y);
		
		private static ArrayList<Point> centerArray = new ArrayList<Point>();
		private static ArrayList<Float> radiusArray = new ArrayList<Float>();
		private static ArrayList<double[]> colorArray = new ArrayList<double[]>();
		
		//De lengte van de helft van het scherm in mm idpv pixels
		//Om de groottes op het projectievlak in mm om te zetten
		private static double halfScreenLength = Math.tan(1.0471975512)*focalLength; 
		
		public float[] getRadius(){
			return this.radius;
		}
		
		public Point getCenter(){
			return this.center;
		}
		
		public ArrayList<Point> getCenterArray(){
			return this.centerArray;
		}
		
		public ArrayList<Float> getRadiusArray(){
			return this.radiusArray;
		}
		
		public ArrayList<double[]> getColorArray(){
			return this.colorArray;
		}
		

		public static void addCircle(Point center,float radius){
			centerArray.add(center);
			radiusArray.add(radius);
		}
		
		public BeeldherkenningTest(AutopilotConfig config){
			this.imageWidth = config.getNbColumns();
			this.imageHeight = config.getNbRows();
			//this.centerArray.add(new Point(screenCenter.x,screenCenter.y));
		}
	
	//MAIN OM TE TESTEN
		public static void main(String[] args) throws Exception{
			imageRecognitionTest(null);
	        
		}
		
		//STATIC OM TE TESTEN
		public static void imageRecognitionTest(byte[] data) throws Exception{
		  //Laad de openCV library in
	        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
	        
	      //Lees image in (om te testen, anders komt data als input binnen)
	        data = Files.readAllBytes(new File("C:\\Image\\pixels.txt").toPath());
	        
	        
	      //Zet data = byte[] om in Mat
	        Mat flipped = new Mat(imageWidth, imageHeight, CvType.CV_8UC3);
	        flipped.put(0, 0, data);
	        Mat image = new Mat();
	        Core.flip(flipped, image, 0);
	        
	      //Maak images aan
	        Mat hsvImage = new Mat();
	        Mat mask = new Mat();
	        
	        
	      //convert the frame to HSV
	        Imgproc.cvtColor(image, hsvImage, Imgproc.COLOR_RGB2HSV);
	        
	      //Zoek kleur tussen deze ranges (rood)
//	        Scalar minValues = new Scalar(0,255,38);
//	        Scalar maxValues = new Scalar(0,255,255);
//	        Core.inRange(hsvImage, minValues, maxValues, mask);
	        
	      //Zoek kleuren die niet wit zijn - kubussen
	        Scalar minValues = new Scalar(0,1,0);
	        Scalar maxValues = new Scalar(255,255,255);
	        Core.inRange(hsvImage, minValues, maxValues, mask);
	        
	        
	        List<MatOfPoint> contours = new ArrayList<>();
	        Mat hierarchy = new Mat();
	
	      //Zoek de contouren van de mask
	        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
	        if (hierarchy.size().height > 0 && hierarchy.size().width > 0)
	        {
	                
	                for (int i = 0; i >= 0; i = (int) hierarchy.get(0, i)[0])
	                {
	                		//Teken de contouren in blauw
	                        Imgproc.drawContours(image, contours, i, new Scalar(0, 0, 255));
	                        //Bepaal minimal enclosing circle en teken hem op blurredImage
	                        MatOfPoint2f contour2f = new MatOfPoint2f( contours.get(i).toArray() ); 
	                        float[] radius = new float[1];
	    					Point center = new Point();
	                        Imgproc.minEnclosingCircle(contour2f, center, radius);
	                        int radiusInt = Math.round(radius[0]);
	                        Imgproc.circle(image, center, radiusInt, new Scalar( 0,0 , 255 ));
	                        
	                        addCircle(center,radius[0]);  
	                        colorArray.add(hsvImage.get((int) center.y,(int) center.x));
	                        
	                }
	        }
	    
	        displayImage( Mat2BufferedImage(mask));
	        Mat RGB_img = new Mat();
	        Imgproc.cvtColor(image,RGB_img, Imgproc.COLOR_BGR2RGB);
	        displayImage( Mat2BufferedImage(RGB_img));
	        System.out.println(distanceToObject(centerArray.get(0),radiusArray.get(0)));
	        System.out.println(horizontalAngle(centerArray.get(0)));
	        System.out.println(verticalAngle(centerArray.get(0)));
	        
	        
	         //System.out.println(centerArray);
	         //System.out.println(radiusArray);
	         //System.out.println(colorArray);
	        
	    }
		
		//Bereken afstand van een object tot de camera
		public static float distanceToObject(Point Center,float radius){
			
			float sizeOnSensorMM = (float) ((radius/100)*2* halfScreenLength);
			return (float) ((objectSize * focalLength )/sizeOnSensorMM );
			
		}
		
		//Bereken de horizontale hoek tussen middelpunt van de afbeelding en het object
		//Negatief is links en positief is rechts
		public static double horizontalAngle(Point center){
			double distancePointCenter = center.x - screenCenter.x;
			double distance = (distancePointCenter/100) *  halfScreenLength;
			return Math.atan(distance/focalLength);
		} 
		
		//Bereken de verticale hoek tussen middelpunt van de afbeelding en het object
		//Negatief is boven en positief is onder
		public static double verticalAngle(Point center){
			double distancePointCenter = center.y - screenCenter.y;
			double distance = (distancePointCenter/100) * halfScreenLength;
			return -Math.atan(distance/focalLength);
		}
		
		public static BufferedImage Mat2BufferedImage(Mat m){
			// source: http://answers.opencv.org/question/10344/opencv-java-load-image-to-gui/
			// The output can be assigned either to a BufferedImage or to an Image

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
		//Display the window
		public static void displayImage(Image img2){   
		    ImageIcon icon=new ImageIcon(img2);
		    JFrame frame=new JFrame();
		    frame.setLayout(new FlowLayout());        
		    frame.setSize(img2.getWidth(null)+50, img2.getHeight(null)+50);     
		    JLabel lbl=new JLabel();
		    lbl.setIcon(icon);
		    frame.add(lbl);
		    frame.setVisible(true);
		    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		}
}