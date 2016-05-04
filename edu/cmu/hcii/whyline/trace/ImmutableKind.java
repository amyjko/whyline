/**
 * 
 */
package edu.cmu.hcii.whyline.trace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public enum ImmutableKind {
	
	STRING { 
		public Object createObject(Trace trace, DataInputStream data) throws IOException { 
			
			return data.readUTF(); 
			
		}
		public void writeObject(Object o, DataOutputStream data) throws IOException { 
			
			data.writeUTF((String)o); 
			
		}
	},
	COLOR { 	
		public Object createObject(Trace trace, DataInputStream data) throws IOException { 
			
			return new java.awt.Color(data.readInt(), true); 
			
		}
		public void writeObject(Object o, DataOutputStream data) throws IOException { 
			
			data.writeInt(((java.awt.Color)o).getRGB()); 
			
		}
	}, 
	FONT { 
		public Object createObject(Trace trace, DataInputStream data) throws IOException { 
			
			return new java.awt.Font(data.readUTF(), data.readInt(), data.readInt()); 
			
		}
		public void writeObject(Object o, DataOutputStream data) throws IOException { 

			java.awt.Font f = (java.awt.Font)o;
			data.writeUTF(f.getFamily());
			data.writeInt(f.getStyle());
			data.writeInt(f.getSize());
		
		}
	},
	GRADIENT { 
		public Object createObject(Trace trace, DataInputStream data) throws IOException { 
			
			return new java.awt.GradientPaint(
				data.readFloat(), data.readFloat(), new java.awt.Color(data.readInt(), true),
				data.readFloat(), data.readFloat(), new java.awt.Color(data.readInt(), true),
				data.readBoolean());					
			
		}
		public void writeObject(Object o, DataOutputStream data) throws IOException { 

			java.awt.GradientPaint p = (java.awt.GradientPaint)o;
			java.awt.geom.Point2D p1 = p.getPoint1(), p2 = p.getPoint2();
			data.writeFloat((float)p1.getX());
			data.writeFloat((float)p1.getY());
			data.writeInt(p.getColor1().getRGB());
			data.writeFloat((float)p2.getX());
			data.writeFloat((float)p2.getY());
			data.writeInt(p.getColor2().getRGB());
			data.writeBoolean(p.isCyclic());
			
		}
	},
	BASICSTROKE { 
		public Object createObject(Trace trace, DataInputStream data) throws IOException { 
			
			return new java.awt.BasicStroke(data.readFloat(), data.readInt(), data.readInt(), data.readFloat());
			
		}
		public void writeObject(Object o, DataOutputStream data) throws IOException { 

			java.awt.BasicStroke s = (java.awt.BasicStroke)o;
			data.writeFloat(s.getLineWidth());
			data.writeInt(s.getEndCap());
			data.writeInt(s.getLineJoin());
			data.writeFloat(s.getMiterLimit());
			
		}
	},
	
	RECTANGLE { 
		public Object createObject(Trace trace, DataInputStream data) throws IOException { 
			
			return new java.awt.Rectangle(data.readInt(), data.readInt(), data.readInt(), data.readInt());
			
		}
		public void writeObject(Object o, DataOutputStream data) throws IOException { 

			java.awt.geom.Rectangle2D r = (java.awt.geom.Rectangle2D)o;
			data.writeInt((int)r.getX());
			data.writeInt((int)r.getY());
			data.writeInt((int)r.getWidth());
			data.writeInt((int)r.getHeight());
			
		}
	},

	TRANSFORM { 
		public Object createObject(Trace trace, DataInputStream data) throws IOException { 
			
			return new java.awt.geom.AffineTransform(data.readFloat(), data.readFloat(), data.readFloat(), data.readFloat(), data.readFloat(), data.readFloat());
			
		}
		public void writeObject(Object o, DataOutputStream data) throws IOException { 

			java.awt.geom.AffineTransform t = (java.awt.geom.AffineTransform)o;
			data.writeFloat((float)t.getScaleX());
			data.writeFloat((float)t.getShearY());
			data.writeFloat((float)t.getShearX());
			data.writeFloat((float)t.getScaleY());
			data.writeFloat((float)t.getTranslateX());
			data.writeFloat((float)t.getTranslateY());

		}
	},

	;

	public abstract Object createObject(Trace trace, DataInputStream data) throws IOException; 
	public abstract void writeObject(Object o, DataOutputStream data) throws IOException; 
	
	private static ImmutableKind[] values = values();

	public static ImmutableKind intToType(int event) { return values[event]; }
	
	public static ImmutableKind classToType(Object o) {
		
		if(o instanceof java.lang.String) return STRING; 
		else if(o instanceof java.awt.BasicStroke) return BASICSTROKE;
		else if(o instanceof java.awt.GradientPaint) return GRADIENT;
		else if(o instanceof java.awt.Color) return COLOR;
		else if(o instanceof java.awt.Font) return FONT;
		else if(o instanceof java.awt.geom.Rectangle2D) return RECTANGLE;
		else if(o instanceof java.awt.geom.AffineTransform) return TRANSFORM;
		else return null;
		
	}
		
}