package edu.cmu.hcii.whyline.io;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.trace.*;

/**
 * @author Andrew J. Ko
 *
 */
public class GraphicalOutputParser extends ExecutionEventParser {

	public GraphicalOutputParser(Trace trace) {
		super(trace);
	}

	public static boolean handles(Instruction inst) {

		if(!(inst instanceof INVOKEVIRTUAL)) return false;
		
		// We've detected an output event!
		QualifiedClassName classname = ((INVOKEVIRTUAL)inst).getMethodInvoked().getClassName(); 

		if(!(classname.equals(QualifiedClassName.get("java/awt/Graphics")) || classname.equals(QualifiedClassName.get("java/awt/Graphics2D")))) return false;

		MethodrefInfo methodRef = ((INVOKEVIRTUAL)inst).getMethodInvoked();

		GraphicalOutputEvent output = null;
		
		// Methods we can ignore because they don't change state. We optimize a bit on the first three letters of getters :)
		return 
			!(
				methodRef.getMethodName().startsWith("get") ||
				methodRef.getMethodName().equals("toString") ||
				methodRef.getMethodName().equals("hitClip") ||
				methodRef.getMethodName().equals("hit") ||
				methodRef.getMethodName().equals("finalize") ||
				methodRef.getMethodName().equals("dispose") ||
				methodRef.getMethodName().equals("addRenderingHints") ||
				methodRef.getMethodName().equals("setRenderingHint") ||
				methodRef.getMethodName().equals("setRenderingHints")
			);
		
	}
	
	// Distinguishes between output that actually draws something and output that just affects what's drawn.
	public static boolean invokesOutput(Instruction inst) {
		
		if(!(inst instanceof INVOKEVIRTUAL)) return false;

		MethodrefInfo methodRef = ((INVOKEVIRTUAL)inst).getMethodInvoked();

		String methodName = methodRef.getMethodName();
		
		return methodName.startsWith("draw") || methodName.startsWith("fill") || methodName.startsWith("copy") || methodName.startsWith("clear");
		
	}
	
	public boolean handle(int id) {

		// If it's an invocation of a java/awt/Graphics method, parse the output
		if(trace.getKind(id) != EventKind.INVOKE_VIRTUAL) return false;

		INVOKEVIRTUAL invoke = (INVOKEVIRTUAL) trace.getInstruction(id);
		
		if(!handles(invoke)) return false;

		MethodrefInfo methodRef = invoke.getMethodInvoked();

		GraphicalOutputEvent output = null;
		
		// Methods we can ignore because they don't change state. We optimize a bit on the first three letters :)
		if(methodRef.getMethodName().startsWith("get")) {}
		else if(methodRef.matchesNameAndDescriptor("toString", "()Ljava/lang/String;")) {}
		else if(methodRef.matchesNameAndDescriptor("hitClip", "(IIII)Z")) {}
		else if(methodRef.matchesNameAndDescriptor("hit", "(Ljava/awt/Rectangle;Ljava/awt/Shape;Z)Z")) {}
		else if(methodRef.matchesNameAndDescriptor("finalize", "()V")) {}

		else if(methodRef.matchesNameAndDescriptor("dispose", "()V")) {}

		// Cloning. We ignore these because they're a special case in the tracing engine, since we need to capture extra details.
		else if(methodRef.matchesNameAndDescriptor("create", "()Ljava/awt/Graphics;")) {}
		else if(methodRef.matchesNameAndDescriptor("create", "(IIII)Ljava/awt/Graphics;")) {}

		// Area operations
		else if(methodRef.matchesNameAndDescriptor("clearRect", "(IIII)V")) output = new ClearRectEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("copyArea", "(IIIIII)V")) output = new CopyAreaEvent(trace, id);

		// Colors
		else if(methodRef.matchesNameAndDescriptor("setColor", "(Ljava/awt/Color;)V")) output = new SetColorEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("setBackground", "(Ljava/awt/Color;)V")) output = new SetBackgroundEvent(trace, id);

		// Clipping
		else if(methodRef.matchesNameAndDescriptor("clipRect", "(IIII)V")) output = new ClipRectEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("clip", "(Ljava/awt/Shape;)V")) output = new ClipWithShapeEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("setClip", "(IIII)V")) output = new SetClipEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("setClip", "(Ljava/awt/Shape;)V")) output = new SetClipWithShapeEvent(trace, id);

		// Rectangles
		else if(methodRef.matchesNameAndDescriptor("drawRect", "(IIII)V")) output = new DrawRectEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("drawRoundRect", "(IIIIII)V")) output = new DrawRoundRectEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("draw3DRect", "(IIIIZ)V")) output = new Draw3DRectEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("fillRect", "(IIII)V")) output = new FillRectEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("fillRoundRect", "(IIIIII)V")) output = new FillRoundRectEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("fill3DRect", "(IIIIZ)V")) output = new Fill3DRectEvent(trace, id);

		// Characters
		else if(methodRef.matchesNameAndDescriptor("setFont", "(Ljava/awt/Font;)V")) output = new SetFontEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("drawBytes", "([BIIII)V")) output = new DrawCharsEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("drawChars", "([CIIII)V")) output = new DrawCharsEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("drawString", "(Ljava/lang/String;II)V")) output = new DrawStringEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("drawString", "(Ljava/lang/String;FF)V")) output = new DrawStringEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("drawGlyphVector", "(Ljava/awt/font/GlyphVector;FF)V")) output = new DrawCharacterSequenceEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("drawString", "(Ljava/text/AttributedCharacterIterator;II)V")) output = new DrawCharacterSequenceEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("drawString", "(Ljava/text/AttributedCharacterIterator;FF)V")) output = new DrawCharacterSequenceEvent(trace, id);

		// Lines
//		else if(methodRef.matchesNameAndDescriptor("drawPolyline", "([I[II)V")) throw new UnsupportedOperationException("drawPolyline");
		else if(methodRef.matchesNameAndDescriptor("drawLine", "(IIII)V")) output = new DrawLineEvent(trace, id);

		// Transformations
		else if(methodRef.matchesNameAndDescriptor("rotate", "(D)V")) output = new RotateEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("rotate", "(DDD)V")) output = new RotateAroundOriginEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("scale", "(DD)V")) output = new ScaleEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("setTransform", "(Ljava/awt/geom/AffineTransform;)V")) output = new SetTransformEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("shear", "(DD)V")) output = new ShearEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("transform", "(Ljava/awt/geom/AffineTransform;)V")) output = new TransformEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("translate", "(DD)V")) output = new TranslateEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("translate", "(II)V")) output = new TranslateEvent(trace, id);

		// Painting modes
		else if(methodRef.matchesNameAndDescriptor("setPaintMode", "()V")) output = new SetPaintModeEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("setXORMode", "(Ljava/awt/Color;)V")) output = new SetXORModeEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("setComposite", "(Ljava/awt/Composite;)V")) output = new SetCompositeEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("setPaint", "(Ljava/awt/Paint;)V")) output = new SetPaintEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("setStroke", "(Ljava/awt/Stroke;)V")) output = new SetStrokeEvent(trace, id);

		// Images
		else if(methodRef.matchesNameAndDescriptor("drawImage", "(Ljava/awt/Image;IILjava/awt/image/ImageObserver;)Z")) output = new DrawImageWithLocationAndObserverEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("drawImage", "(Ljava/awt/Image;IILjava/awt/Color;Ljava/awt/image/ImageObserver;)Z")) output = new DrawImageWithLocationColorAndObserverEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("drawImage", "(Ljava/awt/Image;IIIILjava/awt/Color;Ljava/awt/image/ImageObserver;)Z")) output = new DrawImageWithLocationSizeColorAndObserverEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("drawImage", "(Ljava/awt/Image;IIIILjava/awt/image/ImageObserver;)Z")) output = new DrawImageWithLocationSizeAndObserverEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("drawImage", "(Ljava/awt/Image;IIIIIIIILjava/awt/image/ImageObserver;)Z")) output = new DrawImageWithSizeWithinAreaEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("drawImage", "(Ljava/awt/Image;IIIIIIIILjava/awt/Color;Ljava/awt/image/ImageObserver;)Z")) output = new DrawImageWithSizeAndColorWithinAreaEvent(trace, id);
		
//		else if(methodRef.matchesNameAndDescriptor("drawImage", "(Ljava/awt/image/BufferedImage;Ljava/awt/image/BufferedImageOp;II)V")) throw new UnsupportedOperationException("drawImage(Ljava/awt/image/BufferedImage;Ljava/awt/image/BufferedImageOp;II)V");
//		else if(methodRef.matchesNameAndDescriptor("drawImage", "(Ljava/awt/Image;Ljava/awt/geom/AffineTransform;Ljava/awt/image/ImageObserver;)V")) throw new UnsupportedOperationException("drawImage(Ljava/awt/Image;Ljava/awt/geom/AffineTransform;Ljava/awt/image/ImageObserver;)V");

//		else if(methodRef.matchesNameAndDescriptor("drawRenderableImage", "(Ljava/awt/image/renderable/RenderableImage;Ljava/awt/geom/AffineTransform;)V")) throw new UnsupportedOperationException("drawRenderableImage(Ljava/awt/image/renderable/RenderableImage;Ljava/awt/geom/AffineTransform;)V");
//		else if(methodRef.matchesNameAndDescriptor("drawRenderedImage", "(Ljava/awt/image/RenderedImage;Ljava/awt/geom/AffineTransform;)V")) throw new UnsupportedOperationException("drawRenderedImage(Ljava/awt/image/RenderedImage;Ljava/awt/geom/AffineTransform;)V");

		// Arcs and ovals
		else if(methodRef.matchesNameAndDescriptor("drawArc", "(IIIIII)V")) output = new DrawArcEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("fillArc", "(IIIIII)V")) output = new FillArcEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("drawOval", "(IIII)V")) output = new DrawOvalEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("fillOval", "(IIII)V")) output = new FillOvalEvent(trace, id);

		// Polygons
//		else if(methodRef.matchesNameAndDescriptor("drawPolygon", "([I[II)V")) throw new UnsupportedOperationException("drawPolygon");
//		else if(methodRef.matchesNameAndDescriptor("drawPolygon", "(Ljava/awt/Polygon;)V")) throw new UnsupportedOperationException("drawPolygon");
//		else if(methodRef.matchesNameAndDescriptor("fillPolygon", "([I[II)V")) throw new UnsupportedOperationException("fillPolygon");
//		else if(methodRef.matchesNameAndDescriptor("fillPolygon", "(Ljava/awt/Polygon;)V")) throw new UnsupportedOperationException("fillPolygon");
		
		// Shapes
		else if(methodRef.matchesNameAndDescriptor("draw", "(Ljava/awt/Shape;)V")) output = new DrawEvent(trace, id);
		else if(methodRef.matchesNameAndDescriptor("fill", "(Ljava/awt/Shape;)V")) output = new FillEvent(trace, id);
		
		// Rendering hints
		else if(methodRef.matchesNameAndDescriptor("addRenderingHints", "(Ljava/util/Map;)V")) {}
		else if(methodRef.matchesNameAndDescriptor("setRenderingHint", "(Ljava/awt/RenderingHints$Key;Ljava/lang/Object;)V")) {}
		else if(methodRef.matchesNameAndDescriptor("setRenderingHints", "(Ljava/awt/RenderingHints;)V")) {}

//		else Whyline.debug("Not handling " + invoke);		
		
		if(output != null) {
			
			if(output instanceof RenderEvent) trace.getRenderHistory().add((RenderEvent)output);
			else trace.getGraphicsHistory().add(output);
			return true;

		}
		else return false;
	
	}
	
}