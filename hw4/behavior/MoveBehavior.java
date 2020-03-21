package behavior;

import java.awt.Point;
import java.util.List;

import graphics.group.Group;
import graphics.object.BoundaryRectangle;
import graphics.object.GraphicalObject;

public class MoveBehavior implements Behavior {
    private Group group = null;
    private int state = IDLE;

    private int gridSize = 1;

    private int startX, startY;     // location of start event (wrt window)
    private int prevX, prevY;       // location of previous move (wrt window)
    private GraphicalObject movingObject;

    private BehaviorEvent startEvent = BehaviorEvent.DEFAULT_START_EVENT;
    private BehaviorEvent stopEvent = BehaviorEvent.DEFAULT_STOP_EVENT;
    private BehaviorEvent cancelEvent = BehaviorEvent.DEFAULT_CANCEL_EVENT;

    public MoveBehavior() {}

    public MoveBehavior(int gridSize) {
        if (gridSize < 1) {
            throw new RuntimeException("Grid size must be a positive integer");
        }
        this.gridSize = gridSize;
    }

    /**
     * Methods defined in the Behavior interface
     */
    public Group getGroup() {
        return this.group;
    }

    public Behavior setGroup(Group group) {
        this.group = group;
        return this;
    }

    public int getState() {
        return this.state;
    }

    public BehaviorEvent getStartEvent() {
        return this.startEvent;
    }

    public Behavior setStartEvent(BehaviorEvent startEvent) {
        this.startEvent = startEvent;
        return this;
    }

    public BehaviorEvent getStopEvent() {
        return this.stopEvent;
    }

    public Behavior setStopEvent(BehaviorEvent stopEvent) {
        this.stopEvent = stopEvent;
        return this;
    }

    public BehaviorEvent getCancelEvent() {
        return this.cancelEvent;
    }

    public Behavior setCancelEvent(BehaviorEvent cancelEvent) {
        this.cancelEvent = cancelEvent;
        return this;
    }

    // fix new position to grid coordinates
    private int fixToGrid(int now, int start) {
        int fixedNow = start + (now - start) / gridSize * gridSize;
        int diffInGrid = (now - start) % gridSize;
        if (diffInGrid <= gridSize - diffInGrid) {
            return fixedNow;
        } else {
            return fixedNow + gridSize;
        }
    }

    // Convert event coordinates from absolute to relative to group
    // TODO: do we really need two options?
    private Point findCoordinates(Group group, int x, int y, String option) {
        assert (option == "inside") || (option == "beside");
        if (option == "beside") {
            return group.childToParent(findCoordinates(group, x, y, "inside"));
        } else {
            Group parentGroup = group.getGroup();
            if (parentGroup == null) {
                return new Point(x, y);
            }
            return group.parentToChild(findCoordinates(parentGroup, x, y, option));
        }
    }

    /**
     * start
     */
    public boolean start(BehaviorEvent event) {
        if (event.matches(this.startEvent)
                && this.state == IDLE
                && this.group != null) {
            int eventX = event.getX(), eventY = event.getY();
            Point eventInGroup = findCoordinates(group, eventX, eventY, "inside");
            Point eventBesideGroup = group.childToParent(eventInGroup);
            if (!group.contains(eventBesideGroup)) {
                return false;
            }

            // find the object to be moved
            List<GraphicalObject> children = group.getChildren();
            for (int idx = children.size() - 1; idx >= 0; --idx) { // front to back
                GraphicalObject child = children.get(idx);
                if (child.contains(eventInGroup)) {
                    System.out.println("Move starts!");
                    BoundaryRectangle r = child.getBoundingBox();
                    this.startX = this.prevX = eventX;
                    this.startY = this.prevY = eventY;
                    this.movingObject = child;
                    this.state = RUNNING_INSIDE;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * running
     */
    public boolean running(BehaviorEvent event) {
        if (event.matches(this.stopEvent) || event.matches(this.cancelEvent)) {
            return false;
        }

        if (this.state != IDLE && event.isMouseMoved()) {
            int eventX = event.getX(), eventY = event.getY();
            Point eventBesideGroup = findCoordinates(group, eventX, eventY, "beside");
            if (!group.contains(eventBesideGroup)) {
                System.out.println("outside!");
                this.state = RUNNING_OUTSIDE;
                return true;
            }

            // move the object with mouse
            this.state = RUNNING_INSIDE;
            BoundaryRectangle r = movingObject.getBoundingBox();
            int newX = r.x - prevX + eventX;
            int newY = r.y - prevY + eventY;
            int fixedNewX = fixToGrid(newX, r.x);
            int fixedNewY = fixToGrid(newY, r.y);
            if (fixedNewX != r.x || fixedNewY != r.y) {
                prevX += fixedNewX - r.x;
                prevY += fixedNewY - r.y;
                movingObject.moveTo(fixedNewX, fixedNewY);
            }
            return true;
        }
        return false;
    }

    /**
     * stop
     */
    public boolean stop(BehaviorEvent event) {
        if (event.matches(this.stopEvent) && this.state != IDLE) {
            this.state = IDLE;
            return true;
        }
        return false;
    }

    /**
     * cancel
     */
    public boolean cancel(BehaviorEvent event) {
        if (event.matches(this.cancelEvent) && this.state != IDLE) {
            BoundaryRectangle r = movingObject.getBoundingBox();
            int originalX = r.x - prevX + startX;
            int originalY = r.y - prevY + startY;
            movingObject.moveTo(originalX, originalY);
            this.state = IDLE;
            return true;
        }
        return false;
    }

    public boolean check(BehaviorEvent event) {
        return start(event) || running(event) || stop(event) || cancel(event);
    }
}