/*
 * Copyright (c) 2010-2018, sikuli.org, sikulix.com - MIT license
 */
package org.sikuli.util;

import org.sikuli.basics.Debug;
import org.sikuli.script.IScreen;
import org.sikuli.script.RunTime;
import org.sikuli.script.Screen;
import org.sikuli.script.ScreenImage;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

/**
 * INTERNAL USE implements the screen overlay used with the capture feature
 */
public class OverlayCapturePrompt extends JFrame implements EventSubject {

  final static float MIN_DARKER_FACTOR = 0.6f;
  final static long MSG_DISPLAY_TIME = 2000;
  final static long WIN_FADE_IN_TIME = 200;

  protected static final Font fontMsg = new Font("Arial", Font.PLAIN, 60);
  protected static final Color selFrameColor = new Color(1.0f, 1.0f, 1.0f, 1.0f);
  protected static final Color selCrossColor = new Color(1.0f, 0.0f, 0.0f, 0.6f);
  protected static final Color screenFrameColor = new Color(1.0f, 0.0f, 0.0f, 0.6f);
  protected Rectangle screenFrame = null;
  protected static final BasicStroke strokeScreenFrame = new BasicStroke(5);
  protected static final BasicStroke _StrokeMeasurement = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 1, new float[]{10,10}, 0);
  protected static final BasicStroke bs = new BasicStroke(1);

  protected EventObserver captureObserver = null;
  protected IScreen scrOCP;
  protected BufferedImage scr_img = null;
  protected BufferedImage scr_img_darker = null;
  protected BufferedImage bi = null;
  protected float darker_factor;

  protected int srcScreenId = -1;

  protected boolean canceled = false;
  protected String promptMsg = "";
  protected boolean dragging = false;
  protected boolean hasFinished = false;
  protected boolean hasStarted = false;

  protected int scr_img_type = BufferedImage.TYPE_INT_RGB;
  protected double scr_img_scale = 1;
  protected Rectangle scr_img_rect = null;
  protected ScreenImage scr_img_original = null;

  protected boolean isLocalScreen = true;

  public OverlayCapturePrompt(IScreen scr) {
    Debug.log(3, "TRACE: OverlayCapturePrompt: init: S(%d)", scr.getID());
    scrOCP = scr;
    canceled = false;

    setUndecorated(true);
    setAlwaysOnTop(true);

    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

    if (scr.isOtherScreen()) {
      isLocalScreen = false;
    }

    bindListeners();
  }

  protected void bindListeners(){}

  public int getScrID() {
    return srcScreenId;
  }

  public void close() {
    Debug.log(4, "CapturePrompt.close: S(%d) freeing resources", scrOCP.getID());
    setVisible(false);
    dispose();
    scr_img = null;
    scr_img_darker = null;
    bi = null;
  }

  public void prompt(String msg, int delayMS) {
    try {
      Thread.sleep(delayMS);
    } catch (InterruptedException ie) {
    }
    prompt(msg);
  }

  public void prompt(int delayMS) {
    prompt(null, delayMS);
  }

  public void prompt() {
    prompt(null);
  }

  public void prompt(String msg) {
    scr_img_original = scrOCP.capture();
    if (Debug.getDebugLevel() > 2) {
      scr_img_original.getFile(RunTime.get().fSikulixStore.getAbsolutePath(), "lastScreenShot");
    }
    scr_img = scr_img_original.getImage();
    scr_img_darker = scr_img;
    scr_img_type = scr_img.getType();
    scr_img_rect = new Rectangle(scrOCP.getBounds());
    promptMsg = msg;
    if (isLocalScreen) {
      darker_factor = 0.6f;
      RescaleOp op = new RescaleOp(darker_factor, 0, null);
      scr_img_darker = op.filter(scr_img, null);
    } else {
      promptMsg = null;
      if (scr_img_rect.height > Screen.getPrimaryScreen().getBounds().getHeight()) {
        scr_img_scale = Screen.getPrimaryScreen().getBounds().getHeight() / scr_img_rect.height;
      }
      if (scr_img_rect.width > Screen.getPrimaryScreen().getBounds().getWidth()) {
        scr_img_scale = Math.min(Screen.getPrimaryScreen().getBounds().getWidth() / scr_img_rect.width, scr_img_scale);
      }
      if (1 != scr_img_scale) {
        scr_img_rect.width = (int) (scr_img_rect.width * scr_img_scale);
        scr_img_rect.height = (int) (scr_img_rect.height * scr_img_scale);
        Image tmp = scr_img.getScaledInstance(scr_img_rect.width, scr_img_rect.height, Image.SCALE_SMOOTH);
        scr_img = new BufferedImage(scr_img_rect.width, scr_img_rect.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scr_img.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        scr_img_darker = scr_img;
      }
    }
    this.setBounds(scr_img_rect);
    this.setVisible(true);
  }

  public boolean isComplete() {
    return hasFinished;
  }

  @Override
  public void addObserver(EventObserver obs) {
    Debug.log(3, "TRACE: OverlayCapturePrompt: addObserver: %s", obs != null);
    captureObserver = obs;
  }

  @Override
  public void notifyObserver() {
    Debug.log(3, "TRACE: OverlayCapturePrompt: notifyObserver: %s", captureObserver != null);
    if (null != captureObserver) {
      captureObserver.update(this);
    }
  }

  @Override
  public void paint(Graphics g) {
    if (scr_img != null) {
      Graphics2D g2dWin = (Graphics2D) g;
      if (bi == null) {
        bi = new BufferedImage(scr_img_rect.width, scr_img_rect.height, scr_img_type);
      }
      Graphics2D bfG2 = bi.createGraphics();
      bfG2.drawImage(scr_img_darker, 0, 0, this);
      drawMessage(bfG2);
      drawCustom(g2dWin, bfG2);
      g2dWin.drawImage(bi, 0, 0, this);
      setVisible(true);
    } else {
      setVisible(false);
    }
  }

  protected void drawCustom(Graphics2D g2dWin, Graphics2D bfG2) {
  }

  protected void drawMessage(Graphics2D g2d) {
    if (promptMsg == null) {
      return;
    }
    g2d.setFont(fontMsg);
    g2d.setColor(new Color(1f, 1f, 1f, 1));
    int sw = g2d.getFontMetrics().stringWidth(promptMsg);
    int sh = g2d.getFontMetrics().getMaxAscent();
    Rectangle ubound = scrOCP.getBounds();
    for (int i = 0; i < Screen.getNumberScreens(); i++) {
      if (!Screen.getScreen(i).hasPrompt()) {
        continue;
      }
      Rectangle bound = Screen.getBounds(i);
      int cx = bound.x + (bound.width - sw) / 2 - ubound.x;
      int cy = bound.y + (bound.height - sh) / 2 - ubound.y;
      g2d.drawString(promptMsg, cx, cy);
    }
  }

  protected void drawScreenFrame(Graphics2D g2d, int scrId) {
    if (!isLocalScreen) {
      return;
    }
    g2d.setColor(screenFrameColor);
    g2d.setStroke(strokeScreenFrame);
    if (screenFrame == null) {
      screenFrame = Screen.getBounds(scrId);
      Rectangle ubound = scrOCP.getBounds();
      screenFrame.x -= ubound.x;
      screenFrame.y -= ubound.y;
      int sw = (int) (strokeScreenFrame.getLineWidth() / 2);
      screenFrame.x += sw;
      screenFrame.y += sw;
      screenFrame.width -= sw * 2;
      screenFrame.height -= sw * 2;
    }
    g2d.draw(screenFrame);
  }
}
