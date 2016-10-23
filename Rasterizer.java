
import algebra.*;
import java.lang.Math.*;

/**
 * The Rasterizer class is responsible for the discretization of geometric primitives
 * (edges and faces) over the screen pixel grid and generates Fragment (pixels with
 * interpolated attributes). Those Fragment are then passed to a Shader object,
 * which will produce the final color of the fragment.
 *
 * @author morin, chambon, cdehais
 */
public class Rasterizer {
    
    Shader shader;

    public Rasterizer (Shader shader) {
        this.shader = shader;
    }

    public void setShader (Shader shader) {
        this.shader = shader;
    }

    /**
     * Linear interpolation of a Fragment f on the edge defined by Fragment's v1 and v2
     */ 
    private void interpolate2 (Fragment v1, Fragment v2, Fragment f) {
        int x1 = v1.getX ();
        int y1 = v1.getY ();
        int x2 = v2.getX ();
        int y2 = v2.getY ();       
        int x = f.getX ();
        int y = f.getX ();

        double alpha;
        if (Math.abs (x2 - x1) > Math.abs (y2 - y1)) {
            alpha = (double)(x - x1) / (double)(x2 - x1);
        } else {
            if (y2 != y1) {
                alpha = (double)(y - y1) / (double)(y2 - y1);
            } else {
                alpha = 0.5 ;
            }
        }
        int numAttributes = f.getNumAttributes ();
        for (int i = 0; i < numAttributes; i++) {
            f.setAttribute (i, (1.0 - alpha) * v1.getAttribute (i) + alpha * v2.getAttribute (i));
        }
    }


    /* Swaps x and y coordinates of the fragment. Used by the Bresenham algorithm. */
    private static void swapXAndY (Fragment f) {
        f.setPosition (f.getY(), f.getX());
    }

    /**
     * Rasterizes the edge between the projected vectors v1 and v2.
     * Generates Fragment's and calls the Shader::shade() metho on each of them.
     */ 
    public void rasterizeEdge (Fragment v1, Fragment v2) {


        /* Coordinates of V1 and V2 */
        int x1 = v1.getX ();
        int y1 = v1.getY ();
        int x2 = v2.getX ();
        int y2 = v2.getY ();

	/* For now : just display the vertices
	Fragment f = new Fragment (0,0);
	int size = 2;
        for (int i = 0; i < v1.getNumAttributes (); i++) {
            f.setAttribute (i, v1.getAttribute (i));
		}
	for (int i = -size; i <= size ; i++) {
		for (int j = -size; j <= size; j++) {
			f.setPosition(x1+i,y1+j);
			shader.shade (f);
			}
		}
	*/
  
        // tracé d'un segment avec l'algo de Bresenham 
	int numAttributes = v1.getNumAttributes ();
        Fragment fragment = new Fragment (0, 0); //, numAttributes);
        
        boolean sym = (Math.abs (y2 - y1) > Math.abs (x2 - x1));
        if (sym) {
            int temp;
            temp = x1; x1 = y1 ; y1 = temp; 
            temp = x2; x2 = y2 ; y2 = temp;
        }
        if (x1 > x2) {
            Fragment ftemp;
            int temp;
            temp = x1; x1 = x2; x2 = temp; 
            temp = y1; y1 = y2; y2 = temp; 
            ftemp = v1; v1 = v2; v2 = ftemp;
        }
        
        int ystep;
        if (y1 < y2) {
            ystep =  1;
        } else {
            ystep = -1;
        }
		
		int x = x1;
		float y_courant = y1;
		int y=y1;
		float delta_y = y2-y1;
		float delta_x = x2-x1;
		float m = delta_y/delta_x;	
	

		for (int i=1;i<=delta_x;i++) {
			x = x+1; 
			y_courant = y_courant + m;
			if ((ystep == 1)&&(y_courant < y+0.5)||((ystep == -1) && (y_courant > y -0.5))) {
				y = y;
			} else {
				y = y + ystep;
			}	

	    //envoi du fragment au shader
            fragment.setPosition (x, y);
            
            if (!shader.isClipped (fragment)) {

		//interpolation des attributs
                interpolate2 (v1, v2, fragment);
                if (sym) {
                    swapXAndY (fragment);
                }
                shader.shade (fragment);
            }
		}

    }

    static double triangleArea (Fragment v1, Fragment v2, Fragment v3) {
        return (double) v2.getX () * v3.getY () - v2.getY () * v3.getX () 
                      + v3.getX () * v1.getY () - v1.getX () * v3.getY ()
                      + v1.getX () * v2.getY () - v2.getX () * v1.getY ();
    }

    static protected Matrix makeBarycentricCoordsMatrix (Fragment v1, Fragment v2, Fragment v3) {
        Matrix C = null;
        try {
            C = new Matrix (3, 3);
        } catch (InstantiationException e) {
            /* unreached */
        }

        double area = triangleArea (v1, v2, v3);
        int x1 = v1.getX ();
        int y1 = v1.getY ();
        int x2 = v2.getX ();
        int y2 = v2.getY ();
        int x3 = v3.getX ();
        int y3 = v3.getY ();
        C.set (0, 0, (x2 * y3 - x3 * y2) / area);
        C.set (0, 1, (y2 - y3) / area);
        C.set (0, 2, (x3 - x2) / area);
        C.set (1, 0, (x3 * y1 - x1 * y3) / area);
        C.set (1, 1, (y3 - y1) / area);
        C.set (1, 2, (x1 - x3) / area);
        C.set (2, 0, (x1 * y2 - x2 * y1) / area);
        C.set (2, 1, (y1 - y2) / area);
        C.set (2, 2, (x2 - x1) / area);

        return C;
    }

    /**
     * Rasterizes the triangular face made of the Fragment v1, v2 and v3
     */
    public void rasterizeFace (Fragment v1, Fragment v2, Fragment v3) {
	try {
		Matrix C = makeBarycentricCoordsMatrix (v1, v2, v3);

		/* iterate over the triangle's bounding box */
		
		int xmin = Math.min(Math.min(v1.getX(), v2.getX()), v3.getX());
		int ymin = Math.min(Math.min(v1.getY(), v2.getY()), v3.getY());
		int xmax = Math.max(Math.max(v1.getX(), v2.getX()), v3.getX());
		int ymax = Math.max(Math.max(v1.getY(), v2.getY()), v3.getY());
		
		Vector v = new Vector(3);
		for (int x = xmin ; x <= xmax ; x++) {
			for (int y = ymin ; y <= ymax ; y++) {
				v.set(0, 1);
				v.set(1, x);
				v.set(2, y);
				
				v = C.multiply(v);
				double alpha = v.get(0);
				double beta = v.get(1);
				double gamma = v.get(2);
				
				boolean in = (alpha >= 0 && beta >= 0 && gamma >= 0);
				
				if (in) {
					Fragment f = new Fragment(x, y);
					// Compute the attributes
					for (int i = 0 ; i < v1.getNumAttributes() ; i++) {
						double attribute = alpha*v1.getAttribute(i) + beta*v2.getAttribute(i) + gamma*v3.getAttribute(i);
						f.setAttribute(i, attribute);
					}
                    
					shader.shade(f);
				}
			}
		}
        } catch (Exception e) { /* unreached */ }
    }

    /**
     * Rasterizes the triangular face made of the Fragment v1, v2 and v3
     */
    public void rasterizeFacePhong (Fragment v1, Fragment v2, Fragment v3, Vector3 pos1, Vector3 pos2, Vector3 pos3, Scene scene, Lighting lighting) {
	try {
		Matrix C = makeBarycentricCoordsMatrix (v1, v2, v3);

		/* iterate over the triangle's bounding box */
		
		int xmin = Math.min(Math.min(v1.getX(), v2.getX()), v3.getX());
		int ymin = Math.min(Math.min(v1.getY(), v2.getY()), v3.getY());
		int xmax = Math.max(Math.max(v1.getX(), v2.getX()), v3.getX());
		int ymax = Math.max(Math.max(v1.getY(), v2.getY()), v3.getY());
		
		Vector v = new Vector(3);
		for (int x = xmin ; x <= xmax ; x++) {
			for (int y = ymin ; y <= ymax ; y++) {
				v.set(0, 1);
				v.set(1, x);
				v.set(2, y);
				
				v = C.multiply(v);
				double alpha = v.get(0);
				double beta = v.get(1);
				double gamma = v.get(2);
				
				boolean in = (alpha >= 0 && beta >= 0 && gamma >= 0);
				
				if (in) {
					Fragment f = new Fragment(x, y);
					// Compute the attributes
					for (int i = 0 ; i < v1.getNumAttributes() ; i++) {
						double attribute = alpha*v1.getAttribute(i) + beta*v2.getAttribute(i) + gamma*v3.getAttribute(i);
						f.setAttribute(i, attribute);
					}
					pos1.scale(alpha);
					pos2.scale(beta);
					pos3.scale(gamma);
					pos1.add(pos2);
					pos1.add(pos3);
					
					Vector3 normal = new Vector3(f.getAttribute(4),f.getAttribute(5),f.getAttribute(6));
					normal.normalize();
					double[] oldColor = new double[3];
					oldColor[0] = f.getAttribute(1);
					oldColor[1] = f.getAttribute(2);
					oldColor[2] = f.getAttribute(3);
					double[] material = scene.getMaterial();
					double[] color = lighting.applyLights(pos1, normal, oldColor, scene.getCameraPosition(), material[0], material[1], material[2], material[3]);

					f.setAttribute(1, color[0]);
					f.setAttribute(2, color[1]);
					f.setAttribute(3, color[2]);
					
					shader.shade(f);
				}
			}
		}
        } catch (Exception e) { /* unreached */ }
    }  
}
