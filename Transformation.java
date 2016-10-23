
import algebra.*;

/**
 * author: cdehais
 */
public class Transformation  {

    Matrix worldToCamera;
    Matrix projection;
    Matrix calibration;

    public Transformation () {
	try {
	    worldToCamera = new Matrix ("W2C", 4, 4);
	    projection = new Matrix ("P", 3, 4);
	    calibration = Matrix.createIdentity (3);
	    calibration.setName ("K");
	} catch (InstantiationException e) {
	    /* should not reach */
	}
    }

    public void setLookAt (Vector3 cam, Vector3 lookAt, Vector3 up) {
	try {
	    // Compute rotation
	    Vector3 e3c = new Vector3(lookAt);
	    e3c.subtract(cam);
	    e3c.normalize();
	    Vector3 e1c = up.cross(e3c);
	    e1c.normalize();
	    Vector e2c = e3c.cross(e1c);

	    Matrix P = Matrix.horConcat(Matrix.vecToMat(e1c), Matrix.vecToMat(e2c));
	    P = Matrix.horConcat(P, Matrix.vecToMat(e3c));
	    P = P.transpose();

	    // Compute translation
	    Vector3 t = new Vector3(P.multiply(cam));
	    t.scale(-1);

	    worldToCamera = Matrix.horConcat(P, Matrix.vecToMat(t));
	    Matrix v_aux = new Matrix(1, 4);
	    v_aux.set(0,0,0);
	    v_aux.set(0,1,0);
	    v_aux.set(0,2,0);
	    v_aux.set(0,3,1);
	    worldToCamera = Matrix.verConcat(worldToCamera, v_aux);
	    worldToCamera.setName("W2C");

	} catch (Exception e) { /* unreached */ };
	System.out.println ("Modelview matrix:\n" + worldToCamera);
    }

    public void setProjection () {
	// Construct projection matrix
	try {
	    projection = Matrix.horConcat(Matrix.createIdentity(3), Matrix.createZeros(3,1));
	    projection.setName("P");
	} catch (Exception e) { /* unreached */ };

	System.out.println ("Projection matrix:\n" + projection);
    }

    public void setCalibration (double focal, double width, double height) {
	// Construct calibration matrix
	try {
	    calibration.scalarMultiply(focal);
	    calibration.set(0, 2, width/2);
	    calibration.set(1, 2, height/2);
	    calibration.set(2, 2, 1);
	} catch (Exception e) { /* unreached */ }

	System.out.println ("Calibration matrix:\n" + calibration);
    }

    /**
     * Projects the given homogeneous, 4 dimensional point onto the screen.
     * The resulting Vector as its (x,y) coordinates in pixel, and its z coordinate
     * is the depth of the point in the camera coordinate system.
     */  
    public Vector3 projectPoint (Vector p)
	throws SizeMismatchException, InstantiationException {
	Vector ps = new Vector(3);

	ps = worldToCamera.multiply(p);
	ps = projection.multiply(ps);
	double z = ps.get(2);
	ps = calibration.multiply(ps);
	ps.scale(1/z);
	ps.set(2, z);

	return new Vector3 (ps);
    }

    /**
     * Transform a vector from world to camera coordinates.
     */
    public Vector3 transformVector (Vector3 v)
	throws SizeMismatchException, InstantiationException {
	/* Doing nothing special here because there is no scaling */
	Matrix R = worldToCamera.getSubMatrix (0, 0, 3, 3);
	Vector tv = R.multiply (v);
	return new Vector3 (tv);
    }

}

