package vul.tools.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;


public class Rulers extends KeyAdapter {
  private static final String TITLE = "vRulR";
  private static final String VERSION_NUMBER = "1.0.0";
  private static final boolean SNAPSHOT = true;
  public static final String VERSION = TITLE + " " + VERSION_NUMBER + (SNAPSHOT ? "-SNAPSHOT" : "") + " - 2026/09/29";

  private static final String HELP_TEXT = "<html><table border=0>" +
                                          " <tr><td colspan='2' style='text-align:center'><u>" + VERSION + "</u></td></tr>" +
                                          " <tr><td colspan='2' style='text-align:center'>(c) <i>very useless lessons</i> featuring <i>Horst Hacker & the Teer-100 Experience</i></td></tr>" +
                                          " <tr><td>'SPACE'</td><td>toggle horizontal/vertical ruler layout</td></tr>" +
                                          " <tr><td>'CTRL + SPACE'</td><td>toggle the scale layout (top/bottom for the horzontal" +
                                                                           " and left/right for the vertical ruler layout)</td></tr>" +
                                          " <tr><td>'+'/'-'</td><td>increase/decrease size by 50 pixels</td></tr>" +
                                          " <tr><td>'M'</td><td>create marker at the current mouse position</td></tr>" +
                                          " <tr><td>Left double click</td><td>create marker by keyboard input (apply: 'ENTER', cancel: 'ESC')</td></tr>" +
                                          " <tr><td>'DEL'</td><td>delete marker next to the current mouse position (fuzzy deletion, the<br>"
                                          +                       " related 'active' marker has a different color and a slightly different shape)</td></tr>" +
                                          " <tr><td>Right click</td><td>same as 'DEL'</td></tr>" +
                                          " <tr><td>'0' (Zero)</td><td>move ruler to the horizontal/vertical start position</td></tr>" +
                                          " <tr><td>'CTRL + 0' (Zero)</td><td>move ruler to the origin (which is the upper left corner of your monitor)</td></tr>" +
                                          " <tr><td>'UP'/'DOWN'/'LEFT'/'RIGHT'</td><td>move ruler by one pixel up/down/left/right (for the impatient<br>" +
                                                                                      "ones: additionally pressing 'CTRL' moves by 50 pixels)</td></tr>" +
                                          " <tr><td>'CTRL + S'</td><td>save current position, size, markers and scale layout for both, the horizontal<br>" +
                                                                      "and the vertical ruler in your home directory (these settings<br>" +
                                                                      "will be automatically applied at the next start up)</td></tr>" +
                                          " <tr><td>'CTRL + I'</td><td>iconify ruler (usually to the tray bar)</td></tr>" +
                                          " <tr><td>'F1'</td><td>show this stylish and handy help popup which contains an<br>" +
                                                                "enormous amount of detailed information on how to deal with this<br>" +
                                                                "complex and powerful piece of software</td></tr>" +
                                          " <tr><td>'ESC'</td><td>exit</td></tr>" +
  		                                    "</table></html>";
  
  private static final int FONTSIZE = 10;
  private static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, FONTSIZE);

  private static final int BROADNESS = 46;
  private static final int MIN_HOR_WIDTH = 61;
  private static final int MIN_VERT_HEIGHT = 61;
  
  private static final int DISTANCE_MARKER_SENSITIVITY = 10;
  
  private static final Color BGRD_COLOR = Color.ORANGE;
  private static final Color BGRD_COLOR_NOFOCUS = new Color(223, 223, 223);
  private static final Color FGRD_COLOR = new Color(120, 120, 140);
  private static final Color FGRD_COLOR_STRONG = Color.BLACK;
  private static final Color MARKER_COLOR = new Color(180, 0, 56);
  private static final Color ACTIVE_MARKER_COLOR = new Color(80, 60, 255);
    
  private static Dimension mainScreenSize; 
  private static int maxHorWidth = Integer.MIN_VALUE; 
  private static int maxVertHeight = Integer.MIN_VALUE;  
  
  private AbstractRuler activeRuler;
  private final HorRuler horRuler;
  private final VertRuler vertRuler;
  
  private final S11n s11n;
  
  
  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() { public void run() { init(); } });
  }
  
  private static void init() {
    try { new Rulers(); }
    catch (Exception ex) { ex.printStackTrace(); }
  }
  
  
  static Dimension mainScreenSize() {
    if (mainScreenSize == null) mainScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
    
    return mainScreenSize;
  }
  
  static int maxHorWidth() {
    if (maxHorWidth == Integer.MIN_VALUE) maxHorWidth = mainScreenSize().width;
    
    return maxHorWidth; 
  }
  static int maxVertHeight() {
    if (maxVertHeight == Integer.MIN_VALUE) maxVertHeight = mainScreenSize().height;
    
    return maxVertHeight;  
  }
  
  static int dfltLength(int maxLength) { return Math.round(0.65f * maxLength); }
  
  
  Rulers() throws IOException {
    s11n = new S11n();
    s11n.load();

    final MouseHandler mh = new MouseHandler();
    final KeyHandler kh = new KeyHandler();
    final CloseHandler ch = new CloseHandler();
    
    horRuler = new HorRuler(s11n.getHorSettings());
    horRuler.addMouseListener(mh);
    horRuler.addMouseMotionListener(mh);
    horRuler.addKeyListener(kh);
    horRuler.addKeyListener(this);
    horRuler.addWindowListener(ch);
    horRuler.setIconImages(GraFix.appIcons());
    
    vertRuler = new VertRuler(s11n.getVertSettings());
    vertRuler.addMouseListener(mh);
    vertRuler.addMouseMotionListener(mh);
    vertRuler.addKeyListener(kh);
    vertRuler.addKeyListener(this);
    vertRuler.addWindowListener(ch);
    vertRuler.setIconImages(GraFix.appIcons());
    
    activeRuler = horRuler;
    activeRuler.setVisible(true);
  }
  
  @Override 
  public void keyPressed(KeyEvent evt) {
    if (evt.isControlDown() && (evt.getKeyCode() == KeyEvent.VK_S)) {
      evt.consume();
      
      Settings s = s11n.getHorSettings();
      s.setX(horRuler.getX());
      s.setY(horRuler.getY());
      s.setLength(horRuler.getWidth());
      
      s = s11n.getVertSettings();
      s.setX(vertRuler.getX());
      s.setY(vertRuler.getY());
      s.setLength(vertRuler.getHeight());
      
      try { s11n.write(); }
      catch (IOException ex) { ex.printStackTrace(); }
    }
  }

  private void onToggleDirection() {
    if (activeRuler == horRuler) {
      horRuler.setVisible(false);
      activeRuler = vertRuler;
    } else {
      vertRuler.setVisible(false);
      activeRuler = horRuler;
    }
    activeRuler.setVisible(true);
  }
  
  
  private void setRulerLocation(int value) {
    if (activeRuler == horRuler) horRuler.setLocation(value, horRuler.getY());
    else vertRuler.setLocation(vertRuler.getX(), value);
  }
  
  private void setRulerLocation(int x, int y) {
    if (activeRuler == horRuler) horRuler.setLocation(x, y);
    else vertRuler.setLocation(x, y);
  }

  void showMoveCursor() { activeRuler.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)); }
  
  void moveRuler(int xOffset, int yOffset) { activeRuler.setLocation(activeRuler.getX() + xOffset, activeRuler.getY() + yOffset); }
  
  void updateRulerValue(int mouseX, int mouseY) { activeRuler.updateValue(mouseX, mouseY); }
  
  void dispose() { horRuler.dispose(); vertRuler.dispose(); if (helpDlg != null) helpDlg.dispose(); }
  
  private JDialog helpDlg;
  @SuppressWarnings("serial")
  void showHelp() {
    if (helpDlg == null) {
      helpDlg = new JOptionPane(HELP_TEXT, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION) {
        @Override
        public void paintComponent(Graphics g) {
          ((Graphics2D)g).setRenderingHint(
              RenderingHints.KEY_TEXT_ANTIALIASING,
              RenderingHints.VALUE_TEXT_ANTIALIAS_GASP
          );
          super.paintComponent(g);
        }
      }.createDialog(TITLE + " - help");
      helpDlg.setIconImages(GraFix.APP_ICONS);
      helpDlg.setModal(false);
      helpDlg.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
    }
    helpDlg.setVisible(true);
  }

  /////////////////////////////////////
  
  private final class KeyHandler extends KeyAdapter {
    @Override 
    public void keyPressed(KeyEvent evt) { 
      switch (evt.getKeyCode()) {
        case KeyEvent.VK_ESCAPE: dispose(); break;
        
        case KeyEvent.VK_SPACE: if (!(evt.isAltDown() || evt.isControlDown())) onToggleDirection(); else if (evt.isControlDown()) activeRuler.flipScale(); break;
        
        case KeyEvent.VK_I: if (evt.isControlDown()) activeRuler.setState(Frame.ICONIFIED); break;
        
        case KeyEvent.VK_PLUS: activeRuler.increaseSize(); break;
        case KeyEvent.VK_MINUS: activeRuler.decreaseSize(); break;
        
        case KeyEvent.VK_M: activeRuler.toggleMarker(false); break;
        case KeyEvent.VK_DELETE: activeRuler.toggleMarker(true); break;
        
        case KeyEvent.VK_0: if (evt.isControlDown()) setRulerLocation(0, 0); else setRulerLocation(0); break;
        case KeyEvent.VK_UP: moveRuler(0, -1 * calcMoveFactor(evt)); break;
        case KeyEvent.VK_RIGHT: moveRuler(1 * calcMoveFactor(evt), 0); break;
        case KeyEvent.VK_DOWN: moveRuler(0, 1 * calcMoveFactor(evt)); break;
        case KeyEvent.VK_LEFT: moveRuler(-1 * calcMoveFactor(evt), 0); break;
        
        case KeyEvent.VK_F1: 
        case KeyEvent.VK_HELP: showHelp(); break;
      }
    }
    
    private int calcMoveFactor(KeyEvent evt) { return (evt.isControlDown() ? 50 : 1); }
  }
  
  /////////////////////////////////////
  
  private final class MouseHandler extends MouseMotionAdapter implements MouseListener {
    private final int modeNone = 0;
    private final int modeMove = 1;
    private final int modeResize = 2;
    private int dragMode = modeNone;
    private int startX = 0, startY = 0;
    private int startW = 0, startH = 0;
    private int dragStartX = 0, dragStartY = 0;

    @Override 
    public void mouseMoved(MouseEvent evt) {
      int evtX = evt.getX();
      int evtY = evt.getY();
      if (isResizePickerArea(rulerComp(), evtX, evtY)) {
        rulerComp().showResizeCursor();
      } else {
        rulerComp().resetCursor();
        updateRulerValue(evtX, evtY);
      }
    }

    @Override
    public void mousePressed(MouseEvent evt) {
      if (SwingUtilities.isLeftMouseButton(evt)) {
        AbstractRuler rulerComp = rulerComp(); 
        startX = rulerComp.getX();
        startY = rulerComp.getY();
        startW = rulerComp.getWidth();
        startH = rulerComp.getHeight();
        dragStartX = evt.getXOnScreen(); 
        dragStartY = evt.getYOnScreen();
        
        if (isResizePickerArea(rulerComp, evt.getX(), evt.getY())) {
          dragMode = modeResize;
          rulerComp.showResizeCursor();
        } else {
          dragMode = modeMove;
          rulerComp.showMoveCursor();
        }
      }
    }
    
    private boolean isMoveMode() { return dragMode == modeMove; }
    
    private boolean isResizePickerArea(Component rulerComp, int x, int y) {
      int w = rulerComp.getWidth();
      int h = rulerComp.getHeight();
      return 
          x <= w 
          && x >= w - 12 
          && y <= h 
          && y >= h - 12;
    }

    @Override 
    public void mouseDragged(MouseEvent evt) {
      if (SwingUtilities.isLeftMouseButton(evt) && dragMode != modeNone) {
        int xOffset = evt.getXOnScreen() - dragStartX;
        int yOffset = evt.getYOnScreen() - dragStartY;
        
        AbstractRuler rulerComp = rulerComp();
        if (isMoveMode()) rulerComp.setLocation(startX + xOffset, startY + yOffset);
        else {
          if (rulerComp.direction() == SwingConstants.HORIZONTAL) rulerComp.setSize(startW + xOffset, startH);
          else rulerComp.setSize(startW, startH + yOffset);
        }
      }
    }
    
    @Override public void mouseReleased(MouseEvent evt) { 
      if (SwingUtilities.isLeftMouseButton(evt)) rulerComp().resetCursor();
      
      dragMode = modeNone;
    }

    private AbstractRuler rulerComp() { return Rulers.this.activeRuler; }

    @Override public void mouseClicked(MouseEvent evt) { }
    @Override public void mouseEntered(MouseEvent evt) { }
    @Override public void mouseExited(MouseEvent evt) { }
  }  
  /////////////////////////////////////
  
  private final class CloseHandler extends WindowAdapter { 
    @Override public void windowClosing(WindowEvent e) { dispose(); } 
  }
  
  ///////////////////////////////////////////////////////
  
  @SuppressWarnings("serial")
  private static abstract class AbstractRuler extends JFrame {
    protected int value = Integer.MIN_VALUE;
    protected int activeMarkerPos = Integer.MIN_VALUE;
    protected final JLabel valueLbl = new JLabel("0px", JLabel.CENTER);
    protected final Settings settings;
    
    
    protected AbstractRuler(Settings settings) {
      super(TITLE);
      
      this.settings = settings;
    }
  
    protected void createUI() {
      setUndecorated(true);
      setLocation(settings.x, settings.y);
      setSize(getPreferredSize());
      setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      if (isAlwaysOnTopSupported()) setAlwaysOnTop(true);      

      JPanel p = new JPanel(new BorderLayout(4, 4));
      p.add(rulerPanel(), panelLayoutPosition());
            
      valueLbl.setFont(FONT);
      valueLbl.setForeground(FGRD_COLOR_STRONG);
      valueLbl.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      p.add(valueLbl, labelLayoutPosition());
      
      setContentPane(p);
      
      addFocusListener(new FocusListener() {        
        @Override public void focusLost(FocusEvent e) { handleFocusEvent(false); }
        @Override public void focusGained(FocusEvent e) { handleFocusEvent(true); }        
        private void handleFocusEvent(boolean hasFocus) { getContentPane().setBackground(hasFocus ? BGRD_COLOR : BGRD_COLOR_NOFOCUS); }
      });
      
      addMouseListener(new MouseAdapter() {
        @Override 
        public void mouseClicked(MouseEvent evt) {
          int clickCnt = evt.getClickCount();
          if (SwingUtilities.isLeftMouseButton(evt) && (clickCnt == 2)) showInputDlg(AbstractRuler.this, evt.getX(), evt.getY()); // Marker anlegen
          else if (SwingUtilities.isRightMouseButton(evt)) toggleMarker(true); // remove marker (if there's one)
        }
      });

      resetCursor();
    }
    
    private void showInputDlg(AbstractRuler r, int posX, int posY) {
      InputDlg inputDlg = new InputDlg(r);
      inputDlg.inputField.setRange(0, Math.max(0, (r.direction() == SwingConstants.HORIZONTAL) ? r.getWidth() : r.getHeight()));
      inputDlg.setValue((r.direction() == SwingConstants.HORIZONTAL) ? posX : posY);      
      inputDlg.setLocation(r.getX() + posX, r.getY() + posY);
      inputDlg.setVisible(true);
      inputDlg.requestFocusInWindow();
    }
        
    void resetCursor() { setCursor(GraFix.crosshairCursor()); }
   
    void showMoveCursor() { setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)); }
    void showResizeCursor() { setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)); }

    
    void flipScale() {
      settings.scaleFlipped = !settings.scaleFlipped;
      repaint();
    }
    
    
    void toggleMarker(boolean off) { 
      int markerValue = value;
      if (off) delActiveMarker();
      else addMarkerAt(markerValue);
    }
    
    void addMarkerAt(int pos) {
      settings.markers.add(pos);
      activeMarkerPos = pos;
      rulerPanel().repaint();
    }
    
    void delActiveMarker() {
      if (activeMarkerPos != Integer.MAX_VALUE) {
        settings.markers.remove(activeMarkerPos);
        if (!updateActiveMarker(value)) rulerPanel().repaint();
      }
    }
    
    protected boolean updateActiveMarker(int pos) {
      if (settings.markers.isEmpty()) return false; // nothing to compare
      
      int nearestMarkerPos = Integer.MAX_VALUE;
      int dist;
      for (int markerPos : settings.markers) {
        dist = Math.abs(pos - markerPos);
        if (dist > DISTANCE_MARKER_SENSITIVITY) continue; // distance too large -> ignore marker
        else if (dist < Math.abs(pos - nearestMarkerPos))  nearestMarkerPos = markerPos; // new nearest marker
      }
      
      if (activeMarkerPos != nearestMarkerPos) { 
//        System.out.println("setting active marker to " + nearestMarkerPos);
        activeMarkerPos = nearestMarkerPos;
        rulerPanel().repaint();
        return true;
      }
      // no change
      return false;
    }    

    
    /** @return SwingConstants.HORIZONTAL or SwingConstants.VERTICAL */
    protected abstract int direction(); 
    protected abstract void changeSizeBy(int px);
    protected abstract AbstractRulerPanel rulerPanel();   
    protected abstract void updateValue(int mouseX, int mouseY);


    protected void increaseSize() { changeSizeBy(50); }
    protected void decreaseSize() { changeSizeBy(-50); }
    
    protected String panelLayoutPosition() { return (direction() == SwingConstants.HORIZONTAL) ? BorderLayout.CENTER : BorderLayout.WEST; }    
    protected String labelLayoutPosition() { return (direction() == SwingConstants.HORIZONTAL) ? BorderLayout.EAST : BorderLayout.SOUTH; }
    
    
    /////////////////////////////////////
    
    static abstract class AbstractRulerPanel extends JPanel {
      AbstractRulerPanel(Dimension prefSize) {
        super();
        
        setOpaque(false); // Panel nicht opaque
        setDoubleBuffered(true);
        
        setPreferredSize(prefSize);
      } 
      
      protected void preparePainting(Graphics g, int w, int h) {
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
//        g.fillRect(0, 0, w, h); // panel is transparent, the dialog background shines through 
        g.setColor(FGRD_COLOR);
        g.setFont(FONT);
      }
    }
  }
  
  ///////////////////////////////////////////////////////
  
  @SuppressWarnings("serial")
  private static final class HorRuler extends AbstractRuler {
    private final HorRulerPanel rulerPanel = new HorRulerPanel();
    
    HorRuler(Settings s) { 
      super(s);
      
      setPreferredSize(new Dimension(s.getLength(), BROADNESS));
      createUI();
    }
    
    @Override
    protected void changeSizeBy(int px) {
      int w = Math.min(getWidth() + px, maxHorWidth());
      if (w < MIN_HOR_WIDTH) w = MIN_HOR_WIDTH;
      
      setSize(w, BROADNESS);
      repaint();
    }
    
    @Override
    protected void updateValue(int x, int y) {
      if (this.value != x) {
        this.value = x;
        valueLbl.setText(String.valueOf(x) + "px");
        // markers:
        updateActiveMarker(x);
      }
    }
        
    @Override protected int direction() { return SwingUtilities.HORIZONTAL; }
    
    @Override protected AbstractRulerPanel rulerPanel() { return rulerPanel; }
    
    ///////////////////////////////////
    
    private final class HorRulerPanel extends AbstractRulerPanel {
      HorRulerPanel() { super(new Dimension(1, BROADNESS - 20)); }
      
      @Override
      public void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        preparePainting(g, w, h);

        int step = 5;
        int x;
        int y1 = (settings.scaleFlipped ? h : 0), y2 = y1 + (settings.scaleFlipped ? -5 : 5);
        for (x = 0; x <= w; x += step) {
          if (x % 50 == 0) {
            g.setColor(FGRD_COLOR_STRONG);
            g.drawLine(x, y1, x, y2 - (settings.scaleFlipped ? 6 : -6));
            g.setColor(FGRD_COLOR);
            g.drawString(String.valueOf(x), x, y2 - (settings.scaleFlipped ? 8 : -16));
          } else if (x % 10 == 0) {
            g.drawLine(x, y1, x, y2 - (settings.scaleFlipped ? 3 : -3));
          } else {
            g.drawLine(x, y1, x, y2);
          }
        }
        
        y2 += (settings.scaleFlipped ? - 20 : 20);
        for (Iterator<Integer> it = settings.markers.iterator(); it.hasNext(); ) {
          x = it.next();
          if (x == activeMarkerPos) {
            g.setColor(ACTIVE_MARKER_COLOR);
            g.drawLine(x, y1, x, y2);
            g.fillRect(x - 2, y2 - (settings.scaleFlipped ? 5 : 0), 5, 5);
          } else {
            g.setColor(MARKER_COLOR);
            g.drawLine(x, y1, x, y2);
            g.drawLine(x - 2, y2, x + 2, y2);
          }
        } 
      }
    }
  }
  
  /////////////////////////////////////////////////////////
  
  @SuppressWarnings("serial")
  private static final class VertRuler extends AbstractRuler {
    private final VertRulerPanel rulerPanel = new VertRulerPanel();
    
    VertRuler(Settings s) { 
      super(s);
      
      setPreferredSize(new Dimension(BROADNESS, s.length));
      createUI();
    }
    
    @Override
    protected void changeSizeBy(int px) {
      int h = Math.min(getHeight() + px, maxVertHeight());
      if (h < MIN_VERT_HEIGHT) h = MIN_VERT_HEIGHT;
      
      setSize(BROADNESS, h);
      repaint();
    }
    
    @Override
    protected void updateValue(int x, int y) {
      if (this.value != y) {
        this.value = y;
        valueLbl.setText(String.valueOf(y) + "px");
        // Marker:
        updateActiveMarker(y);
      }
    }

    @Override protected int direction() { return SwingUtilities.VERTICAL; }
    
    @Override protected AbstractRulerPanel rulerPanel() { return rulerPanel; }
    
    ///////////////////////////////////
    
    private final class VertRulerPanel extends AbstractRulerPanel {
      VertRulerPanel() { super(new Dimension(BROADNESS, 1)); }
      
      @Override
      public void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        preparePainting(g, w, h);
        
        int step = 5;
        int x1 = (settings.scaleFlipped ? w : 0), x2 = (settings.scaleFlipped ? w - 5 : 5);
        int y;
        for (y = 0; y <= h; y += step) {
          if (y % 50 == 0) {
            g.setColor(FGRD_COLOR_STRONG);
            g.drawLine(x1, y, x2 + (settings.scaleFlipped ? -6 : 6), y);
            g.setColor(FGRD_COLOR);
            // numbers of the scale:
            String s = String.valueOf(y);
            if (settings.scaleFlipped) g.drawString(s, x2 - (8 + GraFix.stringWidth(s, g)), y + FONTSIZE); 
            else g.drawString(s, x2 + 8, y + FONTSIZE);
          } else if (y % 10 == 0) {
            g.drawLine(x1, y, x2 + (settings.scaleFlipped ? -3 : 3), y);
          } else {
            g.drawLine(x1, y, x2, y);
          }
        }
                
        x2 += (settings.scaleFlipped ? -20 : 20);
        for (Iterator<Integer> it = settings.markers.iterator(); it.hasNext(); ) {
          y = it.next();
          if (y == activeMarkerPos) {
            g.setColor(ACTIVE_MARKER_COLOR);
            g.drawLine(x1, y, x2, y);
            g.fillRect(x2 - (settings.scaleFlipped ? 5 : 0), y - 2, 5, 5);
          } else {
            g.setColor(MARKER_COLOR);
            g.drawLine(x1, y, x2, y);
            g.drawLine(x2, y - 2, x2, y + 2);
          }
        }
      }
    }
  }
  
  ///////////////////////////////////////////////
  
  private static final class GraFix {
    private static Cursor crosshairCursor;
    
    static Cursor crosshairCursor() {
      if (crosshairCursor != null) return crosshairCursor;
      
      Toolkit kit = Toolkit.getDefaultToolkit();
      Dimension dim = kit.getBestCursorSize(24, 24);
      
      BufferedImage bi = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = GraphicsEnvironment.getLocalGraphicsEnvironment().createGraphics(bi);

      g.setColor(Color.DARK_GRAY);
      int centerX = (dim.width - 1) /2;
      int centerY = (dim.height - 1) / 2;
      g.drawLine(centerX, 0, centerX, dim.height - 1);
      g.drawLine(0, centerY, dim.height - 1, centerY);
      g.dispose();
      
      crosshairCursor = kit.createCustomCursor(bi, new Point(centerX, centerY), "vul-crosshair-cursor");
      return crosshairCursor;
    }
    
    
    static int stringWidth(String s, Graphics g) { return g.getFontMetrics().stringWidth(s); }
    
    
    private static final int[] ICON_SIZES = { 12, 24, 32 };
    private static final ArrayList<Image> APP_ICONS = new ArrayList<Image>(ICON_SIZES.length);
    static ArrayList<Image> appIcons() {
      if (APP_ICONS.isEmpty()) {
        // paint icons:
        for (int size : ICON_SIZES) APP_ICONS.add(drawIcon(size));
      }
      return APP_ICONS;
    }
    
    private static Image drawIcon(int size) {
      final int w = size, h = size; 

      BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = bi.createGraphics();
//      g.setComposite(AlphaComposite.Src);
//      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      final int halfSize = size / 2;
      final int quartSize = size / 4;
      g.setColor(BGRD_COLOR);
      g.fillRect(0, quartSize, w, halfSize);
      
      int yStart;
      g.setColor(FGRD_COLOR);
      for (int i = 0; i < w; i += 4) {
        yStart = ((i % 8) == 0) ? halfSize : halfSize + 4;
        g.drawLine(i, yStart, i, halfSize + quartSize);
      }
      
      final int offs = 3;
      g.setColor(FGRD_COLOR_STRONG);
      g.drawLine(offs + quartSize, offs, offs + quartSize, offs + halfSize);
      g.drawLine(offs, offs + quartSize, offs + halfSize, offs + quartSize);
      
      g.dispose();
      
      return bi;      
    }
  }
  
  ///////////////////////////////////////////////
  // Serialization, Settings:  
  public static final class S11n {
    private static final File SETTINGS_DIR = new File(System.getProperty("user.home"), ".vrulers");
    private static final String SETTINGS_FILE = "rulers.settings";
    
    private Settings horSettings, vertSettings;

    public Settings getHorSettings() { return horSettings; }
    public void setHorSettings(Settings horSettings) { this.horSettings = horSettings; }

    public Settings getVertSettings() { return vertSettings; }
    public void setVertSettings(Settings vertSettings) { this.vertSettings = vertSettings; }
    
    
    synchronized void load() throws IOException {
      File f = new File(SETTINGS_DIR, SETTINGS_FILE);
      if (! f.isFile()) {
        loadDefaults();
        return;
      }
      
      InputStream is = new BufferedInputStream(new FileInputStream(f));
      XMLDecoder dec = new XMLDecoder(is);
      try { 
        S11n s = (S11n)dec.readObject();
        setHorSettings(s.getHorSettings());
        setVertSettings(s.getVertSettings());
      } finally { 
        dec.close(); 
      }
    }
    
    private synchronized void loadDefaults() {
      int length = dfltLength(maxHorWidth());
      int x = (mainScreenSize().width - length) / 2;
      int y = (mainScreenSize().height - BROADNESS) / 2;
      horSettings = new Settings(x, y, length);

      length = dfltLength(maxVertHeight());
      x = (mainScreenSize().width - BROADNESS) / 2;
      y = (mainScreenSize().height - length) / 2;
      vertSettings = new Settings(x, y, length);
    }
    
    synchronized void write() throws IOException {
      SETTINGS_DIR.mkdirs();
      
      OutputStream os = new BufferedOutputStream(new FileOutputStream(new File(SETTINGS_DIR, SETTINGS_FILE)));
      XMLEncoder enc = new XMLEncoder(os);
      try { enc.writeObject(this); } 
      finally { enc.close(); }    
    }
  }
  
  
  /** Persistable settings bean */
  public static final class Settings {
    private int x, y;
    private int length;
    private Set<Integer> markers;
    private boolean scaleFlipped; 
    
    public Settings() { }
    Settings(int x, int y, int length) {
      this.x = x;
      this.y = y;
      this.length = length;
      markers = new HashSet<Integer>();
    }
    
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
        
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    
    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }
    
    public Set<Integer> getMarkers() { return markers; }
    public void setMarkers(Set<Integer> markers) { this.markers = markers; }
    
    public boolean isScaleFlipped() { return scaleFlipped; }
    public void setScaleFlipped(boolean scaleSwitched) { this.scaleFlipped = scaleSwitched; }
  }
  
  /////////////////////////////////////////////////////////
  // marker position input field:
  @SuppressWarnings("serial")
  private static final class InputDlg extends JDialog {
    final LongField inputField;
    
    InputDlg(AbstractRuler ruler) {
      super(ruler, true);
      
      inputField = new LongField();
      inputField.setColumns(5);
      inputField.setRange(0, maxHorWidth());
      inputField.setToolTipText("input marker position");
      inputField.addKeyListener(new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent evt) {
          switch (evt.getKeyCode()) {
            case KeyEvent.VK_ESCAPE: setVisible(false); break;
            case KeyEvent.VK_ENTER: ruler.addMarkerAt(getValue()); setVisible(false); dispose(); break;
            default: break;
          }
        }
      });
      
      getContentPane().add(inputField);
      
      setUndecorated(true);
      setResizable(false);
      pack();
    }

    int getValue() { return (int)inputField.getLong(); }
    void setValue(int val) { inputField.setLong(val); }
    
    
    ///////////////////////////////////
    /** Textfield for Integers */
    public static final class LongField extends JTextField {
      private static final long serialVersionUID = 1L;
      private long maxVal = Long.MAX_VALUE;
      private long minVal = Long.MIN_VALUE;
      private static final long DEFAULT_VALUE = 0;
      private static final int DEFAULT_COLUMNS = String.valueOf(Long.MIN_VALUE).length();

      
      public LongField() { this(DEFAULT_VALUE, DEFAULT_COLUMNS); }
      public LongField(long initialValue, int cols) {
        super(cols);
        
        try { getDocument().insertString(0, String.valueOf(initialValue), null); } 
        catch (BadLocationException ex) { ex.printStackTrace(); }
        
        addKeyListener(new KeyAdapter() {
          @Override 
          public void keyPressed(KeyEvent evt) {
            switch (evt.getKeyCode()) {
              case KeyEvent.VK_UP: setLong(getLong() + 1); break;
              case KeyEvent.VK_DOWN: setLong(getLong() - 1); break;
              default: break;
            }
          }
        });
      }
      
      protected Document createDefaultModel() { return new LongDocument(); }
     
      public void setRange(long minVal, long maxVal) {
        if (minVal > maxVal) throw new RuntimeException("min value is greater then max value");
        
        this.maxVal = maxVal;
        this.minVal = minVal;
      }
     
      public void setLong(long l) { setText(String.valueOf(l)); }
      public long getLong() {
        String curText = getText();
        if (curText.length() == 0) return minVal;
        else return Integer.parseInt(curText);
      }
      
      /////////////////////////////////////

      /** <code>Document</code> implementation for {@link LongField}. */
      class LongDocument extends PlainDocument {
        private static final long serialVersionUID = 1L;

        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
          if (str == null) return;

          if (isInRange(offs, str)) super.insertString(offs, str, a);
        }

        private boolean isInRange(int offs, String str) throws BadLocationException {
          StringBuffer curVal = new StringBuffer(this.getText(0, this.getLength()));
          curVal.insert(offs, str);
          
          try {
            long l = Long.parseLong(curVal.toString());
            return ((l <= maxVal) && (l >= minVal));
          } catch (NumberFormatException ex) {
            return false;
          }
        }
      }
    }
  }
}
