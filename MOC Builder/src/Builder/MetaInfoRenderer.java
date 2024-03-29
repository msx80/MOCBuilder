package Builder;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

import Common.Vector2f;
import Common.Vector3f;
import Notification.ILDrawSubscriber;
import Notification.INotificationMessage;
import Notification.NotificationCenter;
import Notification.NotificationMessageT;
import Window.MOCBuilder;
import Window.GlobalMousePosition;

public class MetaInfoRenderer implements ILDrawSubscriber {

	private MainCamera camera;
	private Grid grid;
	private Baseplate baseplate;
	private MOCBuilder builder;

	private GLU glu;

	private boolean isVisible = true;

	private boolean showBaseplate = true;

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}

	public MetaInfoRenderer(MOCBuilder builder, MainCamera cam) {
		camera = cam;
		grid = new Grid(cam);
		baseplate = new Baseplate(cam);
		glu = new GLU();
		this.builder = builder;

		showBaseplate = BuilderConfigurationManager.getInstance()
				.isUseDefaultBaseplate();

		NotificationCenter.getInstance().addSubscriber(this,
				NotificationMessageT.BrickbuilderConfigurationChanged);
	}

	private void drawPointerPos(GL2 gl2) {
		Vector2f mousePoint = GlobalMousePosition.getInstance().getPos();

		Vector3f hitPos = builder.getHittedPos(mousePoint.getX(),
				mousePoint.getY(), true);

		gl2.glColor3d(0, 0, 0);

		// Draw sphere (possible styles: FILL, LINE, POINT).
		GLUquadric earth = glu.gluNewQuadric();
		glu.gluQuadricDrawStyle(earth, GLU.GLU_FILL);
		glu.gluQuadricNormals(earth, GLU.GLU_FLAT);
		glu.gluQuadricOrientation(earth, GLU.GLU_OUTSIDE);
		final float radius = 5;
		final int slices = 16;
		final int stacks = 16;
		gl2.glLoadMatrixf(camera.getModelView(), 0);
		gl2.glTranslatef(hitPos.getX(), hitPos.getY(), hitPos.getZ());
		glu.gluSphere(earth, radius, slices, stacks);
		glu.gluDeleteQuadric(earth);
	}

	public void draw(GL2 gl2) {
		if (isVisible == false)
			return;
		gl2.glDisable(GL2.GL_LIGHTING);

		gl2.glUseProgram(0);

		gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glPushMatrix();
		gl2.glLoadMatrixf(camera.getProjection(), 0);
		gl2.glMatrixMode(GL2.GL_MODELVIEW);
		gl2.glPushMatrix();
		gl2.glLoadMatrixf(camera.getModelView(), 0);

		if (BuilderConfigurationManager.getInstance()
				.isUseDefaultBaseplate() == false)
			grid.draw(gl2);
		else {
			if (showBaseplate)
				baseplate.draw(gl2);
			else
				grid.draw(gl2);
		}
		
		gl2.glMatrixMode(GL2.GL_MODELVIEW);
		gl2.glPopMatrix();

		gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glPopMatrix();

		gl2.glEnable(GL2.GL_LIGHTING);
	}

	public void setRange(float[] range) {
		grid.setRange(range);
		baseplate.setRange(range);
	}

	public void setShowBaseplate(boolean flag) {
		this.showBaseplate = flag;
	}

	@Override
	public void receiveNotification(NotificationMessageT messageType,
			INotificationMessage msg) {
		showBaseplate = BuilderConfigurationManager.getInstance()
				.isUseDefaultBaseplate();
	}
}
