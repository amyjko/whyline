package edu.cmu.hcii.whyline.ui.views;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * The base class for all custom dynamic views. Inserted into DynamicJComponents, in order to interface with swing.
 * 
 * @author Andrew J. Ko
 *
 */
public class View {

	private boolean notifyContainerOfSizeChanges = false;
	
	private double preferredWidth, preferredHeight;
	
	private final VisibleProperty left = new VisibleProperty(0f);
	private final VisibleProperty top = new VisibleProperty(0f);
	private final VisibleProperty width = new VisibleProperty(0f);
	private final VisibleProperty height = new VisibleProperty(0f);

	private final VisibleProperty percentToScaleChildren = new VisibleProperty(1.0f);
	private final VisibleProperty percentToFadeChildren = new VisibleProperty(1.0f);
	
	private AffineTransform localTransform;
	private AffineTransform visibleTransform;

	private boolean clipsChildren = false;

	private ViewContainer container = null;
	private View parent;
	private final ArrayList<View> children = new ArrayList<View>(0);

	private class AnimationInfo {
		
		private ViewContainer root;
		private final View view;
		private long startTime;
		private final int duration;
		private long elapsedTime;
		private final boolean animateChildren;

		public AnimationInfo(View view, int ms, boolean animateChildren) {
			
			this.view = view;
			startTime = System.currentTimeMillis();
			duration = ms;
			this.animateChildren = animateChildren;
			root = view.getContainer();
			
		}
		
		public boolean update() {
			
			long current = System.currentTimeMillis(); 
			elapsedTime = current - startTime;
			final double percent = Math.min((float) elapsedTime / duration, 1.0);
			
			if(root != null) {
				root.invokeRunnableLater(new Runnable() {
					public void run() {
						view.animatePositions(percent, animateChildren);
						view.repaint();
					}
				});
			}
			view.repaint();
			return elapsedTime > duration;

		}
		
	}
	
	private static Vector<AnimationInfo> animationInfo = new Vector<AnimationInfo>();
	
	private static final Timer animationTimer;
	static {
		
		// Animate these position changes over time.
		animationTimer = new Timer(true);
		animationTimer.scheduleAtFixedRate(new TimerTask() {

			public void run() {

				synchronized(animationInfo) {
				
					if(animationInfo.size() > 0) {

						Iterator<AnimationInfo> infoIterator = animationInfo.iterator();
						while(infoIterator.hasNext()) {
							
							AnimationInfo info = infoIterator.next();
							if(info.update()) infoIterator.remove();
	
						}
						
					}
					
				}

			}
		}, 0, 30);
		
	}
	
	public static void addRoot(ViewContainer c) { roots.add(c); }

	public static void removeRoot(ViewContainer c) { roots.remove(c); }
	
	public void animate(int duration, boolean alsoAnimateChildren) { 
		
		synchronized(animationInfo) {

			animationInfo.add(new AnimationInfo(this, duration, alsoAnimateChildren));
			repaint();
			
		}
		
	}
	
	private static ArrayList<ViewContainer> roots = new ArrayList<ViewContainer>();
	
	public View(int left, int top, int width, int height) {
		
		visibleTransform = new AffineTransform();
		localTransform = new AffineTransform();

		this.left.set(left);
		this.top.set(top);
		this.width.set(width);
		this.height.set(height);
		
	}

	public View() {
		
		this(0, 0, 5, 5);
		
	}
	
	public final double getLocalLeft() { return left.get(); }
	public final double getVisibleLocalLeft() { return left.getVisible(); }
	public final double getGlobalLeft() {

		if(getParent() == null) return getLocalLeft();
		else return (int)getParent().localToGlobal(new Point2D.Double(getLocalLeft(), getLocalTop())).getX();

	}
	public final double getVisibleGlobalLeft() {

		if(getParent() == null) return getVisibleLocalLeft();
		else return (int)getParent().localToGlobal(new Point2D.Double(getVisibleLocalLeft(), getVisibleLocalTop())).getX();

	}
	
	public final double getLocalRight() { return (int)(left.get() + getLocalWidth()); }
	public final double getVisibleLocalRight() { return (int)(left.getVisible() + getVisibleLocalWidth()); }
	public final double getGlobalRight() {

		if(getParent() == null) return getLocalRight();
		else return (int)getParent().localToGlobal(new Point2D.Double(getLocalRight(), getLocalBottom())).getX();

	}
	public final double getVisibleGlobalRight() {

		if(getParent() == null) return getVisibleLocalRight();
		else return (int)getParent().localToGlobal(new Point2D.Double(getVisibleLocalRight(), getVisibleLocalBottom())).getX();

	}
	
	public final double getLocalTop() { return (int)top.get(); }
	
	public final double getLocalHorizontalCenter() { return getLocalLeft() + getLocalWidth() / 2; }
	public final double getLocalVerticalCenter() { return getLocalTop() + getLocalHeight() / 2; }
	
	public final double getVisibleLocalTop() { return (int)top.getVisible(); }
	public final double getGlobalTop() {

		if(getParent() == null) return getLocalTop();
		else return (int)getParent().localToGlobal(new Point2D.Double(getLocalLeft(), getLocalTop())).getY();

	}
	public final double getVisibleGlobalTop() {

		if(getParent() == null) return getVisibleLocalTop();
		else return (int)getParent().localToGlobal(new Point2D.Double(getVisibleLocalLeft(), getVisibleLocalTop())).getY();

	}

	public final double getLocalBottom() { return (int)(top.get() + getLocalHeight()); }
	public final double getVisibleLocalBottom() { return (int)(top.getVisible() + getVisibleLocalHeight()); }
	public final double getGlobalBottom() {

		if(getParent() == null) return getLocalBottom();
		else return (int)getParent().localToGlobal(new Point2D.Double(getLocalRight(), getLocalBottom())).getY();

	}
	public final double getVisibleGlobalBottom() {

		if(getParent() == null) return getVisibleLocalBottom();
		else return (int)getParent().localToGlobal(new Point2D.Double(getVisibleLocalRight(), getVisibleLocalBottom())).getY();

	}
	
	public final double getLocalWidth() { return (int)width.get(); }
	public final double getVisibleLocalWidth() { return (int)width.getVisible(); }
	public final double getGlobalWidth() { return getGlobalRight() - getGlobalLeft(); }
	public final double getVisibleGlobalWidth() { return getVisibleGlobalRight() - getVisibleGlobalLeft(); }

	public final double getLocalHeight() { return (int)height.get(); }
	public final double getVisibleLocalHeight() { return (int)height.getVisible(); }
	public final double getGlobalHeight() { return getGlobalBottom() - getGlobalTop(); }
	public final double getVisibleGlobalHeight() { return getVisibleGlobalBottom() - getVisibleGlobalTop(); }

	public final double getPercentToScaleChildren() { return percentToScaleChildren.get(); }
	public final double getVisiblePercentToScaleChildren() { return percentToScaleChildren.getVisible(); }

	public final double getPercentToFadeChildren() { return percentToFadeChildren.get(); }
	public final double getVisiblePercentToFadeChildren() { return percentToFadeChildren.getVisible(); }

	public final View getParent() { return parent; }
	private final void setParent(View parent) { this.parent = parent; this.container = parent == null ? null : parent.container; }
	
	public synchronized final View getRoot() {
		
		if(parent == null) return this;
		else return parent.getRoot();
		
	}
	
	public final int getDepth() {
		
		return parent == null ? 0 : 1 + parent.getDepth();
		
	}
	
	public final ViewContainer getContainer() {
		
		if(container == null) {

			View root = getRoot();
			for(ViewContainer container : View.roots)
				if(container.getView() == root)
					this.container = container;
			
		}

		return container;
		
	}

	public final boolean clippingBoundariesContain(double localX, double localY) {
		
		return localX >= getLocalLeft() && localY >= getLocalTop() && localX < getLocalRight() + 1 && localY < getLocalBottom() + 1;

	}
	
	public final Rectangle getLocalBoundaries() {

		return new java.awt.Rectangle((int)getLocalLeft(), (int)getLocalTop(), (int)getLocalWidth(), (int)getLocalHeight());

	}
	
	public final Rectangle getGlobalBoundaries() {
		
		return new Rectangle((int)getGlobalLeft(), (int)getGlobalTop(), (int)getGlobalWidth(), (int)getGlobalHeight());
		
	}

	public final Rectangle getVisibleGlobalBoundaries() {
		
		return new Rectangle((int)getVisibleGlobalLeft(), (int)getVisibleGlobalTop(), (int)getVisibleGlobalWidth(), (int)getVisibleGlobalHeight());
		
	}

	public final Rectangle2D getBoundariesRelativeTo(View view) {
		
		Point2D topLeft = new Point2D.Double(getGlobalLeft(), getGlobalTop());
		Point2D bottomRight = new Point2D.Double(getGlobalRight(), getGlobalBottom());
		if(view.getParent() != null) {
			topLeft = view.getParent().globalToLocal(topLeft);
			bottomRight = view.getParent().globalToLocal(bottomRight);
		}
		return new Rectangle2D.Double(topLeft.getX(), topLeft.getY(), bottomRight.getX() - topLeft.getX(), bottomRight.getY() - topLeft.getY());
		
	}

	public final Rectangle2D getVisibleBoundariesRelativeTo(View view) {
		
		Point2D topLeft = new Point2D.Double(getVisibleGlobalLeft(), getVisibleGlobalTop());
		Point2D bottomRight = new Point2D.Double(getVisibleGlobalRight(), getVisibleGlobalBottom());
		if(view.getParent() != null) {
			topLeft = view.getParent().globalToLocal(topLeft);
			bottomRight = view.getParent().globalToLocal(bottomRight);
		}
		return new Rectangle2D.Double(topLeft.getX(), topLeft.getY(), bottomRight.getX() - topLeft.getX(), bottomRight.getY() - topLeft.getY());
		
	}

	public final void addChild(View view) {
		
		assert view.getParent() == null : "" + view + " is already a child of " + view.getParent();
		children.add(view);
		view.setParent(this);
		
	}

	public final void addChildBeforeChildAtIndex(View view, int index) {
		
		assert view.getParent() == null : "Can't add child that's already added.";
		children.add(index, view);
		view.setParent(this);
		
	}
	
	public final void bringToFront() {

		if(parent == null) return;

		View temp = parent;
		
		temp.removeChild(this);
		temp.addChild(this);
		repaint();
		
	}
	
	public final void sendToBack() {
		
		if(parent == null) return;

		parent.children.remove(this);
		parent.children.add(0, this);
		repaint();
		
	}
	
	public final boolean hasChild(View view) {
		
		return children.contains(view);
		
	}
	
	public final View getChildAtIndex(int index) {
		
		if(index < 0 || index >= children.size()) return null;
		else return children.get(index);
		
	}
	
	public final View getChildBefore() {

		if(parent == null) return null;
		else return parent.getChildAtIndex(parent.children.indexOf(this) - 1);
		
	}

	public final View getChildAfter() {

		if(parent == null) return null;
		else return parent.getChildAtIndex(parent.children.indexOf(this) + 1);
		
	}

	public final View getFirstChild() { return children.isEmpty() ? null : children.get(0); }
	public final View getLastChild() { return children.isEmpty() ? null : children.get(children.size() - 1); }
	
	public final Iterable<View> getChildren() { return children; }

	public final int getNumberOfChildren() { return children.size(); }
	
	public final void removeChild(View child) {

		if(children.contains(child)) {
			
			children.remove(child);
			child.setParent(null);
			
		}
		
	}

	public final void removeChildren() {
		
		while(children.size() > 0) removeChild(getFirstChild());
		
	}

	// To save memory. Useful if a view has a constant number of children.
	public void trim() { children.trimToSize(); }
	
	public final void setLocalLeft(double newLeft, boolean animate) { 
		
		if(animate) left.animate(newLeft); 
		else left.set(newLeft); 
		invalidateTransform(); 
		
	}
	
	public final void setLocalTop(double newTop, boolean animate) { 
		
		if(animate) top.animate(newTop); 
		else top.set(newTop); 
		invalidateTransform(); 
		
	}
	
	public final void setLocalWidth(double newWidth, boolean animate) { 

		boolean changed = animate ? width.animate(newWidth) : width.set(newWidth);
		if(changed) { 
			invalidateTransform(); 
			notifyContainerOfSizeChange(); 
		} 
		
	}
	public final void setLocalHeight(double newHeight, boolean animate) { 
		
		boolean changed = animate ? height.animate(newHeight) : height.set(newHeight);
		if(changed) { 
			invalidateTransform(); 
			notifyContainerOfSizeChange();
		}
		
	}

	public final void setPercentToScaleChildren(double newScale) { 
	
		if(newScale == 0.0) newScale = .01;
		percentToScaleChildren.set(newScale); invalidateTransform(); 
		
	}

	public void setNotifyContainerOfSizeChanges(boolean yes) {
		
		this.notifyContainerOfSizeChanges = yes;
		
	}
	
	// Only notifies if this is a root view.
	private void notifyContainerOfSizeChange() {
		
		if(notifyContainerOfSizeChanges && parent == null) {
			ViewContainer container = getContainer();
			if(container != null)
				container.rootViewChangedSize();
		}
		
	}
	
	public final void setPreferredSize(double width, double height) {
		
		this.preferredWidth = width;
		this.preferredHeight = height;
		notifyContainerOfSizeChange();
		
	}
	
	public final double getPreferredWidth() { return preferredWidth; }
	public final double getPreferredHeight() { return preferredHeight; }

	public final void animatePercentToScaleChildren(double newScale) { 
		
		if(newScale == 0.0) newScale = .01;
		percentToScaleChildren.animate(newScale); invalidateTransform(); 
		
	}
	
	public final void setPercentToFadeChildren(double newTransparency) { percentToFadeChildren.set(newTransparency); }

	private void invalidateTransform() { 
		
		if(!localTransform.isIdentity()) localTransform.setToIdentity(); 
		
	}
		
	public AffineTransform getInverseOfLocalTransform() {
		
		try {
			return getLocalTransform().createInverse();
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
			return null;
		}
		
	}

	private AffineTransform getLocalTransform() {

		if(localTransform.isIdentity()) {

			localTransform.translate(getLocalLeft(), getLocalTop());
			double visibleScale = percentToScaleChildren.get();
			if(visibleScale != 1.0) localTransform.scale(visibleScale, visibleScale);
			
		}
		return localTransform;
	}

	// Returns the given point, interpreted in this view's parent's coordinate system, as global coordinates.
	public final Point2D localToGlobal(Point2D point) {
		
		getLocalTransform().transform(point, point);
		if(parent == null) return point;
		else return parent.localToGlobal(point);
		
	}

	public final Point2D globalToLocal(Point2D point) {
		
		if(parent == null) {

			getInverseOfLocalTransform().transform(point, point);
			return point;
		
		}
		else {
		
			parent.globalToLocal(point);
			getInverseOfLocalTransform().transform(point, point);
			return point;
		
		}
		
	}
	
	// Returns the given point, interpreted in the coordinate system of the given view's parent, in terms of *this* view's parent's coordinate system.
	// Use this to help draw between points on the given view and things that this view draws.
	public final Point2D localToLocal(View view, Point2D point) {

		return getParent().globalToLocal(view.getParent().localToGlobal(point));
		
	}

	public final double globalLeftToLocal(double left) { return globalToLocal(new Point2D.Double(left, 0)).getX(); }
	public final double globalTopToLocal(double top) { return globalToLocal(new Point2D.Double(0, top)).getY(); }
	
	public void setClipsChildren(boolean clipsChildren) {
		
		this.clipsChildren = clipsChildren;
		repaint();
				
	}

	public boolean clipsChildren() { return clipsChildren; }
	
	public final void paint(Graphics2D g) {

		Graphics2D og = (Graphics2D)g.create();
		
		AlphaComposite oldComposite = (AlphaComposite)g.getComposite();
		og.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)getVisiblePercentToFadeChildren() * oldComposite.getAlpha()));

		if(clipsChildren)
			og.clipRect((int)getVisibleLocalLeft(), (int)getVisibleLocalTop(), (int)getVisibleLocalWidth() + 2, (int)getVisibleLocalHeight() + 2);

		og.translate(getVisibleLocalLeft(), getVisibleLocalTop());
		double visibleScale = percentToScaleChildren.getVisible();
		if(visibleScale != 1.0) og.scale(visibleScale, visibleScale);		
		
		paintChildren(og);

	}
	
	public void paintChildren(Graphics2D g) {

		Rectangle clip = g.getClipBounds();
		for(View view : children) {
			if(clip.intersects((int)view.getLocalLeft(), (int)view.getLocalTop(), (int)view.getLocalWidth(), (int)view.getLocalHeight())) {

				view.paintBelowChildren(g);
				view.paint(g);
				view.paintAboveChildren(g);
			}
		}

	}

	public void paintBelowChildren(Graphics2D g) {}
	public void paintAboveChildren(Graphics2D g) {}
	
	public void drawBoundaries(Color color, Graphics2D g) {
		
		g.setColor(color);
		int left= (int)getVisibleLocalLeft(), top = (int)getVisibleLocalTop(), width = (int)getVisibleLocalWidth(), height = (int)getVisibleLocalHeight(); 
		g.drawRect(left, top, width - 1, height - 1);
		
	}
	
	public void fillBoundaries(Color color, Graphics2D g) {
	
		g.setColor(color);
		int left= (int)getVisibleLocalLeft(), top = (int)getVisibleLocalTop(), width = (int)getVisibleLocalWidth(), height = (int)getVisibleLocalHeight(); 
		g.fillRect(left, top, width + 1, height + 1);
	
	}
	
	public void drawRoundBoundaries(Color color, Graphics2D g, int roundWidth, int roundHeight) {
		
		g.setColor(color);
		int left= (int)getVisibleLocalLeft(), top = (int)getVisibleLocalTop(), width = (int)getVisibleLocalWidth(), height = (int)getVisibleLocalHeight(); 
		g.drawRoundRect(left, top, width, height, roundWidth, roundHeight); 

	}

	public void fillRoundBoundaries(Color color, Graphics2D g, int roundWidth, int roundHeight) {
		
		g.setColor(color);
		int left= (int)getVisibleLocalLeft(), top = (int)getVisibleLocalTop(), width = (int)getVisibleLocalWidth(), height = (int)getVisibleLocalHeight(); 
		g.fillRoundRect(left, top, width, height, roundWidth, roundHeight); 

	}

	private void animatePositions(double percentComplete, boolean animateChildren) {
		
		left.update(percentComplete);
		top.update(percentComplete);
		width.update(percentComplete);
		height.update(percentComplete);
		top.update(percentComplete);
		percentToScaleChildren.update(percentComplete);
		percentToFadeChildren.update(percentComplete);
		invalidateTransform();
		
		if(animateChildren)
			for(int i = 0; i < children.size(); i++)
				children.get(i).animatePositions(percentComplete, animateChildren);
		
	}
	
	public void handleContainerResize() {}
	public boolean handleMouseMove(int localX, int localY) { return false; }
	public boolean handleMouseDrag(int localX, int localY, int mouseButton) { return false; }
	public boolean handleMouseClick(int localX, int localY, int mouseButton) { return false; }
	public boolean handleMouseDoubleClick(int localX, int localY, int mouseButton) { return false; }
	public boolean handleMouseUp(int localX, int localY, int mouseButton) { return false; }
	public boolean handleMouseDown(int localX, int localY, int mouseButton) { return false; }
	public boolean handleWheelMove(int units) { return false; }
	public void handleMouseEnter() {}
	public void handleMouseExit() {}
	public void handleMouseNoLongerDirectlyOver(int x, int y) {}
	public void handleMouseDirectlyOver(int x, int y) {}
	
	public boolean handleKeyPressed(KeyEvent keyCode) { return false; }
	public boolean handleKeyReleased(KeyEvent keyCode) { return false; }
	public boolean handleKeyTyped(KeyEvent keyCode) { return false; }
	
	/**
	 * 
	 * @param viewsUnderMouse
	 * @param localX Should be coordinate in this view's coordinate system
	 * @param localY Should be coordinate in this view's coordinate system
	 * @param clip Should be coordinate in this view's coordinate system
	 */
	public final void collectViewsUnderMouseFromBottomToTop(ArrayList<View> viewsUnderMouse, int localX, int localY, Rectangle2D clip) {

		// If clipping boundaries intersect local boundaries
		//		add this to front of list of views under mouse
		//		clip clipping rectangle, if clipsChildren is true
		//		transform rectangle
		//		ask children
		// otherwise, return

		// Also, viewsUnderMouse should be a linked list. It'll be faster to add since we're adding to the front.

		// If this view isn't in the clip, then none of its views are under the mouse.
		Rectangle2D localBoundaries = getLocalBoundaries();

		if(clip.intersects(localBoundaries)) {

			if(clipsChildren)
				clip = clip.createIntersection(localBoundaries);

			// Does the clipped part of this view contain the mouse?
			if(clip.contains(localX, localY) && localBoundaries.contains(localX, localY))
				viewsUnderMouse.add(0, this);				
			
			
			try {
			
				// Transform the clip to the children's coordinate system.
				Point2D topLeft = getLocalTransform().inverseTransform(new Point2D.Double(clip.getMinX(), clip.getMinY()), null);
				Point2D bottomRight = getLocalTransform().inverseTransform(new Point2D.Double(clip.getMaxX(), clip.getMaxY()), null);
				
				clip = new Rectangle2D.Double(topLeft.getX(), topLeft.getY(), bottomRight.getX() - topLeft.getX() + 1, bottomRight.getY() - topLeft.getY() + 1);

				// Transform the mouse position to the children's local coordinate system.
				Point2D mouse = getLocalTransform().inverseTransform(new Point2D.Double(localX, localY), null);

				localX = (int)mouse.getX();
				localY = (int)mouse.getY();

			} catch(NoninvertibleTransformException e) {
				e.printStackTrace();
			}
			
			// Collect views in the children.
			for(int i = 0; i < children.size(); i++)
				children.get(i).collectViewsUnderMouseFromBottomToTop(viewsUnderMouse, localX, localY, clip);
			
		}
		
	}
	
	public final boolean containsOrChildrenContains(View view) {
		
		for(View child : children) {
			
			if(child == view) return true;
			if(child.containsOrChildrenContains(view)) return true;
			
		}
		return false;
		
	}

	public final void repaint() {

		ViewContainer container = getContainer();
		if(container != null) container.repaint();
		
	}

	public final double getLeftmostChildsLeft() {
		
		double minX = 0;
		for(View child : children)			
			if(child.getLocalLeft() < minX) 
				minX = child.getLocalLeft();
		return minX;
		
	}

	public final double getRightmostChildsRight() {
		
		double maxX = 0;
		for(View child : children)			
			if(child.getLocalRight() > maxX) 
				maxX = child.getLocalRight();
		return maxX;
		
	}

	public final double getBottommostChildsBottom() {
		
		double maxY = 0;
		for(View child : children)			
			if(child.getLocalBottom() > maxY) 
				maxY = child.getLocalBottom();
		return maxY;
		
	}

}