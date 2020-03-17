package behavior;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JFrame;

import graphics.group.Group;
import graphics.object.BoundaryRectangle;
import graphics.object.GraphicalObject;

public class InteractiveWindowGroup extends JFrame implements Group {
    private static final long serialVersionUID = 1L;

    protected Image buffer;
    private JComponent canvas;
    private Insets insets;

    private List<GraphicalObject> children = new ArrayList<>();
    private List<Behavior> behaviors = new ArrayList<>();

    // make a top-level window with specified title, width and height
    public InteractiveWindowGroup(String title, int width, int height) {
        super(title);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        WindowMouseListener mouseListener = new WindowMouseListener();
        this.addMouseListener(mouseListener);
        this.addMouseMotionListener(mouseListener);
        this.addKeyListener(new WindowKeyListener());

        canvas = new JComponent() {
            private static final long serialVersionUID = 1L;
            public void paintComponent(Graphics graphics) {
                if (buffer != null) {
                    graphics.drawImage(buffer, 0, 0, null);
                }
            }
        };
        canvas.setBackground(Color.white);
        canvas.setPreferredSize(new Dimension(width, height));

        this.add(canvas);
        this.pack();
        this.setVisible(true);

        this.makeBuffer(width, height); // must be after setVisible
        this.insets = getInsets();
    }

    private void handleBehaviorEvent(BehaviorEvent behaviorEvent) {
        for (Behavior behavior : behaviors) {
            behavior.check(behaviorEvent);
        }
        redraw(children.get(0));
    }

    private class WindowMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent event) {
            int id = BehaviorEvent.MOUSE_DOWN_ID;
            handleBehaviorEvent(getBehaviorEvent(event, id));
        }

        public void mouseReleased(MouseEvent event) {
            int id = BehaviorEvent.MOUSE_UP_ID;
            handleBehaviorEvent(getBehaviorEvent(event, id));
        }

        public void mouseMoved(MouseEvent event) {
            System.out.println("mouse moved");
            int id = BehaviorEvent.MOUSE_MOVE_ID;
            handleBehaviorEvent(getBehaviorEvent(event, id));
        }

        public void mouseDragged(MouseEvent event) {
            System.out.println("mouse dragged");
            int id = BehaviorEvent.MOUSE_DRAGGED_ID;
            handleBehaviorEvent(getBehaviorEvent(event, id));
        }

        // public void mouseExited(MouseEvent event) {
        //     System.out.println("mouse exited " + event.getX() + " " + event.getY());
        //     int id = BehaviorEvent.MOUSE_MOVE_ID;
        //     handleBehaviorEvent(getBehaviorEvent(event, id));
        // }

        // public void mouseEntered(MouseEvent event) {
        //     System.out.println("mouse entered");
        //     int id = BehaviorEvent.MOUSE_MOVE_ID;
        //     handleBehaviorEvent(getBehaviorEvent(event, id));
        // }

        public void mouseWheelMoved(MouseWheelEvent event) {
            int id = BehaviorEvent.SCROLLWHEEL_ID;
            handleBehaviorEvent(getBehaviorEvent(event, id));
        }

        private BehaviorEvent getBehaviorEvent(MouseEvent event, int id) {
            return new BehaviorEvent(id,
                getModifiers(event),
                getKey(event, id),
                event.getX() - insets.left,
                event.getY() - insets.top - 1  // at least this works on MacOS
            );
        }

        // mouse event keys
        private int getKey(MouseEvent event, int id) {
            if (BehaviorEvent.isMouseEvent(id)) {
                int button = event.getButton();
                if (button == MouseEvent.BUTTON1) {
                    return BehaviorEvent.LEFT_MOUSE_KEY;
                } else if (button == MouseEvent.BUTTON2) {
                    return BehaviorEvent.MIDDLE_MOUSE_KEY;
                } else if (button == MouseEvent.BUTTON3) {
                    return BehaviorEvent.RIGHT_MOUSE_KEY;
                }
            }
            if (BehaviorEvent.isMouseWheelEvent(id)) {
                int scroll = ((MouseWheelEvent) event).getWheelRotation();
                if (scroll < 0) {
                    return BehaviorEvent.SCROLLWHEEL_UP_KEY;
                } else if (scroll > 0) {
                    return BehaviorEvent.SCROLLWHEEL_DOWN_KEY;
                }
            }
            return BehaviorEvent.NO_KEY;
        }
    }

    // applicable to all input events
    private int getModifiers(InputEvent event) {
        int modifiers = BehaviorEvent.NO_MODIFIER;
        modifiers |= event.isAltDown() ? BehaviorEvent.ALT_MODIFIER : 0;
        modifiers |= event.isControlDown() ? BehaviorEvent.CONTROL_MODIFIER : 0;
        modifiers |= event.isShiftDown() ? BehaviorEvent.SHIFT_MODIFIER : 0;
        modifiers |= event.isMetaDown() ? BehaviorEvent.COMMAND_KEY_MODIFIER : 0;
        // TODO: check meta and other modifiers
        return modifiers;
    }

    private class WindowKeyListener extends KeyAdapter {
        private Point getCursor() {
            Point cursor = getMousePosition();
            return (cursor != null) ? cursor : new Point(0, 0);
        }

        public void keyPressed(KeyEvent event) {
            Point cursor = getCursor();
            handleBehaviorEvent(new BehaviorEvent(
                BehaviorEvent.KEY_DOWN_ID,
                getModifiers(event),
                event.getKeyCode(),
                cursor.x - insets.left,
                cursor.y - insets.top
            ));
        }

        public void keyReleased(KeyEvent event) {
            Point cursor = getCursor();
            handleBehaviorEvent(new BehaviorEvent(
                BehaviorEvent.KEY_UP_ID,
                getModifiers(event),
                event.getKeyCode(),
                cursor.x - insets.left,
                cursor.y - insets.top
            ));
        }

        public void keyTyped(KeyEvent event) {
        }
    }

    private void makeBuffer(int width, int height) {
        buffer = createImage(width, height);
        Graphics2D graphics = (Graphics2D) buffer.getGraphics();
        graphics.setColor(canvas.getBackground());
        graphics.fillRect(0, 0, width, height);
    }
    
    public InteractiveWindowGroup() {
        this("Interactive Window", 400, 400);
    }

    public void redraw(GraphicalObject object) {
        Graphics2D graphics = (Graphics2D) buffer.getGraphics();
        BoundaryRectangle area = new BoundaryRectangle(0, 0, getWidth(), getHeight());
        graphics.setColor(canvas.getBackground());
        graphics.fill(area);
        object.draw(graphics, area);
        canvas.repaint();
    }

    public void addBehavior(Behavior behavior) {
        behaviors.add(behavior);
    }

    public void removeBehavior(Behavior behavior) {
        behaviors.remove(behavior);
    }

    /**
     * Methods defined in the Group and GraphicalObject interface
     */
    public void addChild(GraphicalObject child) {
        if (!children.isEmpty()) {
            String message = "top level window only supports one child";
            throw new IllegalArgumentException(message);
        } else {
            children.add(child);
            child.setGroup(this);
            redraw(child);
        }
    }

    public void removeChild(GraphicalObject child) {
        children.remove(child);
        child.setGroup(null);
    }

    public void bringChildToFront(GraphicalObject child) {
    }

    public void resizeToChildren() {
    }

    public List<GraphicalObject> getChildren() {
        return new ArrayList<GraphicalObject>(children);
    }

    public Point parentToChild(Point pt) {
        return pt;
    }

    public Point childToParent(Point pt) {
        return pt;
    }

    public void draw(Graphics2D graphics, Shape clipRect) {
    }

    public BoundaryRectangle getBoundingBox() {
        return new BoundaryRectangle(canvas.getBounds());
    }

    public void moveTo(int x, int y) {
    }

    public Group getGroup() {
        return null;
    }

    public void setGroup(Group group) {
    }

    public boolean contains(int x, int y) {
        return getBoundingBox().contains(x, y);
    }
}