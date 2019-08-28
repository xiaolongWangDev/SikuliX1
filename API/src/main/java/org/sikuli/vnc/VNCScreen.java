/*
 * Copyright (c) 2010-2018, sikuli.org, sikulix.com - MIT license
 */
package org.sikuli.vnc;

import com.dragondawn.vncconnector.VncConnector;
import org.sikuli.basics.Debug;
import org.sikuli.script.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class VNCScreen extends Region implements IScreen {
    private VncConnector vncConnector;


    private IRobot robot;
    private ScreenImage lastScreenImage;

    private double START_UP_WAIT = 2.0d;

    private String ip = "";
    private int port = -1;
    private String id = "";

    private static Map<String, VNCScreen> screens = new HashMap<>();

    private VNCScreen() {
    }

    public VncConnector getVncConnector() {
        return vncConnector;
    }

    public static VNCScreen start(String theIP, int thePort, String password) {
        VNCScreen scr;

        String address = theIP + ":" + thePort;

        VNCScreen vncScreen = screens.get(address);
        if (null != vncScreen) {
            scr = vncScreen;
        } else {
            scr = new VNCScreen();
        }

        if (scr.id.isEmpty()) {
            scr.init(theIP, thePort, password);
            Debug.log(3, "VNCScreen: start: %s", scr);
        } else {
            Debug.log(3, "VNCScreen: start: using existing: %s", scr);
        }
        return scr;
    }

    private void init(String theIP, int thePort, String password) {
        ip = theIP;
        port = thePort;
        id = String.format("%s:%d", ip, port);
        this.vncConnector = new VncConnector();
        new Thread(() -> vncConnector.connect(ip, port, password)).start();
        this.robot = new VNCRobot(this);

        setOtherScreen(this);
        setRect(getBounds());
        initScreen(this);

        screens.put(id, this);
        while(!vncConnector.firstImageReady()) {
            this.wait(2d);
        }
    }

    public String getIDString() {
        return (isRunning() ? "VNC " : "VNC:INVALID ") + id;
    }

    public void stop() {
        close();
        screens.remove(this.id);
    }

    public static void stopAll() {
        if (screens.size() > 0) {
            Debug.log(3, "VNCScreen: stopping all");
            for (VNCScreen scr : screens.values()) {
                scr.close();
            }
            screens.clear();
        }
    }

    private void close() {
        if (isRunning()) {
            Debug.log(3, "VNCScreen: stopping: %s", this);
            this.vncConnector.disconnect();
            this.vncConnector = null;
            this.robot = null;
        }
    }

    public boolean isRunning() {
        return null != vncConnector;
    }

    @Override
    public IRobot getRobot() {
        return robot;
    }

    @Override
    public Rectangle getBounds() {
        if (isRunning()) {
            return vncConnector.getBounds();
        }
        return new Rectangle();
    }

    @Override
    public ScreenImage capture() {
        return capture(getBounds());
    }

    @Override
    public ScreenImage capture(Region reg) {
        return capture(reg.x, reg.y, reg.w, reg.h);
    }

    @Override
    public ScreenImage capture(Rectangle rect) {
        return capture(rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    public ScreenImage capture(int x, int y, int w, int h) {
        if (!isRunning()) {
            return null;
        }
    BufferedImage image = this.vncConnector.getLatestImage();
    ScreenImage img = new ScreenImage(
            new Rectangle(x, y, w, h),
            image
    );
    lastScreenImage = img;
        Debug.log(3, "VNCScreen: capture: (%d,%d) %dx%d on %s", x, y, w, h, this);
    return img;
    }

    @Override
    public int getID() {
        return 0;
    }

    @Override
    public int getIdFromPoint(int srcx, int srcy) {
        return 0;
    }

    @Override
    protected <PSIMRL> Location getLocationFromTarget(PSIMRL target) throws FindFailed {
        Location location = super.getLocationFromTarget(target);
        if (location != null) {
            location.setOtherScreen(this);
        }
        return location;
    }

    @Override
    public ScreenImage getLastScreenImageFromScreen() {
        return lastScreenImage;
    }

    @Override
    public ScreenImage userCapture(final String msg) {
        throw new RuntimeException("Not supported");
    }

    public Region set(Region element) {
        return setOther(element);
    }

    public Location set(Location element) {
        return setOther(element);
    }

    public Region setOther(Region element) {
        element.setOtherScreen(this);
        return element;
    }

    public Location setOther(Location element) {
        element.setOtherScreen(this);
        return element;
    }

    public Location newLocation(int x, int y) {
        Location loc = new Location(x, y);
        loc.setOtherScreen(this);
        return loc;
    }

    public Location newLocation(Location loc) {
        return newLocation(loc.x, loc.y);
    }

    public Region newRegion(int x, int y, int w, int h) {
        Region reg = Region.create(x, y, w, h, this);
        reg.setOtherScreen(this);
        return reg;
    }

    public Region newRegion(Location loc, int w, int h) {
        return newRegion(loc.x, loc.y, w, h);
    }

    public Region newRegion(Region reg) {
        return newRegion(reg.x, reg.y, reg.w, reg.h);
    }
}
