import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class EventPanel extends JPanel implements ActionListener, MouseListener {

    public static final Font STANDARD_FONT = new Font("Monospaced", Font.PLAIN, 12);
    public static final Color NORMAL_BACKGROUND = Color.WHITE;
    public static final Color SELECTED_BACKGROUND = new Color(210, 230, 250);
    public static final Color SEPARATOR_COLOR = new Color(200, 200, 200);

    private Component parent;
    private Event event;
    private JPanel topPanel;
    private JPanel topButtonPanel;
    private JLabel captionLabel;
    private JButton snapshotButton;
    private JButton subEventsButton;
    private JButton expandButton;
    private JTextArea detailsArea;
    private JLabel imageLabel;
    private JPanel expandPanel;
    private boolean expanded;
    private boolean selected;
    private boolean canExpand;

    public EventPanel(Event evt, Component parent) {
        this.event = evt;
        this.parent = parent;
        this.canExpand = false;

        captionLabel = new JLabel();
        captionLabel.setFont(STANDARD_FONT);
        captionLabel.setOpaque(true);

        snapshotButton = new JButton("SNAPSHOT");
        snapshotButton.setFont(STANDARD_FONT);
        snapshotButton.addActionListener(this);
        subEventsButton = new JButton("SUBEVENTS");
        subEventsButton.setFont(STANDARD_FONT);
        subEventsButton.addActionListener(this);
        expandButton = new JButton("+");
        expandButton.setFont(STANDARD_FONT);
        expandButton.addActionListener(this);

        topButtonPanel = new JPanel();

        detailsArea = new JTextArea();
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setEditable(false);
        detailsArea.setFont(STANDARD_FONT);

        imageLabel = new JLabel();

        topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        topPanel.add(captionLabel, BorderLayout.CENTER);
        topPanel.add(topButtonPanel, BorderLayout.EAST);

        if (event.hasAttachment("snapshot")) {
            topButtonPanel.add(snapshotButton);
        }

        expandPanel = new JPanel();
        expandPanel.setLayout(new BoxLayout(expandPanel, BoxLayout.PAGE_AXIS));

        String details = null;
        if (event.getDetails() != null && !event.getDetails().equals("")) {
            details = event.getDetails();
        } else if (event.hasAttachment("diff")) {
            details = event.getAttachment("diff").getTextData();
        }
        if (details != null) {
            detailsArea.setText(details);
            expandPanel.add(detailsArea);
            canExpand = true;
        }

        if (event.hasAttachment("image")) {
            Attachment image = event.getAttachment("image");
            imageLabel.setIcon(new ImageIcon(image.getImageData()));
            expandPanel.add(imageLabel);
            canExpand = true;
        }

        java.util.List<Event> subEvents = event.getSubEvents();
        if (subEvents != null && subEvents.size() > 0) {
            topButtonPanel.add(subEventsButton);
        }

        captionLabel.addMouseListener(this);
        detailsArea.addMouseListener(this);
        addMouseListener(this);

        setAlignmentX(Component.CENTER_ALIGNMENT);
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);

        if (canExpand && isAlwaysExpanded()) {
            expanded = true;
            add(expandPanel, BorderLayout.CENTER);
        } else if (canExpand) {
            expanded = false;
            topButtonPanel.add(expandButton);
        }
        setSelected(false);

        // for layout debugging
        //topPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
        //detailsArea.setBorder(BorderFactory.createLineBorder(Color.BLUE));

        if (expanded) {
            setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0,0,1,0, SEPARATOR_COLOR),
                        BorderFactory.createEmptyBorder(5,2,5,2)));
        } else {
            setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0,0,1,0, SEPARATOR_COLOR),
                        BorderFactory.createEmptyBorder(0,2,0,2)));
        }

        refreshLabel();
        refreshSize();
    }

    public Event getEvent() {
        return event;
    }

    private boolean isAlwaysExpanded() {
        if (event.hasTag("comment")) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean status) {
        selected = status;
        if (status) {
            setBackground(SELECTED_BACKGROUND);
            topPanel.setBackground(SELECTED_BACKGROUND);
            topButtonPanel.setBackground(SELECTED_BACKGROUND);
            captionLabel.setBackground(SELECTED_BACKGROUND);
            detailsArea.setBackground(SELECTED_BACKGROUND);
            imageLabel.setBackground(SELECTED_BACKGROUND);
            expandPanel.setBackground(SELECTED_BACKGROUND);
        } else {
            setBackground(NORMAL_BACKGROUND);
            topPanel.setBackground(NORMAL_BACKGROUND);
            topButtonPanel.setBackground(NORMAL_BACKGROUND);
            captionLabel.setBackground(NORMAL_BACKGROUND);
            detailsArea.setBackground(NORMAL_BACKGROUND);
            imageLabel.setBackground(NORMAL_BACKGROUND);
            expandPanel.setBackground(NORMAL_BACKGROUND);
        }
        invalidate();
        repaint();
    }

    public String getLabel() {
        StringBuffer label = new StringBuffer();
        label.append(event.getFormattedTimestamp());
        label.append(": ");
        for (String tag : event.getTags()) {
            if (!tag.equals("text")) {
                label.append("[");
                label.append(tag.toUpperCase());
                label.append("] ");
            }
        }
        //if (event.hasAttachment("snapshot")) {
            //label.append("[*] ");
        //}
        if (event.hasProperty("path")) {
            label.append(event.getProperty("path"));
        } else if (!expanded) {
            label.append(event.getDetailPreview());
        }
        //return label.toString();
        String desc = label.toString();
        if (desc.length() > 80) {
            desc = desc.substring(0,76) + " ...";
        }
        return desc;
    }

    public void refreshLabel() {
        captionLabel.setText(getLabel());
    }

    public void refreshSize() {
        if (expanded) {
            setMaximumSize(new Dimension(Short.MAX_VALUE,
                        topPanel.getPreferredSize().height + 
                        expandPanel.getPreferredSize().height));
        } else {
            setMaximumSize(new Dimension(Short.MAX_VALUE,
                        topPanel.getPreferredSize().height));
            setPreferredSize(null);
        }
    }
    
    public void showSubEvents() {
        java.util.List<Event> subEvents = event.getSubEvents();
        if (subEvents != null && subEvents.size() > 0) {
            JPanel subEventsPanel = new JPanel();
            subEventsPanel.setLayout(new BoxLayout(subEventsPanel, BoxLayout.PAGE_AXIS));
            subEventsPanel.setBackground(NORMAL_BACKGROUND);
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout());
            mainPanel.add(new JScrollPane(subEventsPanel), BorderLayout.CENTER);
            JDialog popupDialog = new JDialog();
            popupDialog.setTitle(getLabel());
            popupDialog.getContentPane().add(mainPanel);
            popupDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            popupDialog.setSize(800,500);
            popupDialog.setLocationRelativeTo(null);
            popupDialog.setVisible(true);

            for (Event e : subEvents) {
                subEventsPanel.add(new EventPanel(e, popupDialog));
            }
            subEventsPanel.add(Box.createVerticalGlue());
        }
    }

    public void showSnapshot() {
        String path = event.getProperty("path");
        String fulltext = (event.hasAttachment("snapshot") ? 
                event.getAttachment("snapshot").getTextData() : null);
        if (path != null && fulltext != null) {
            JTextArea textArea = new JTextArea();
            textArea.setText(fulltext);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setEditable(false);
            textArea.setFont(STANDARD_FONT);
            textArea.setSelectionStart(0);
            textArea.setSelectionEnd(0);
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout());
            mainPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
            JDialog popupDialog = new JDialog();
            popupDialog.setTitle(path);
            popupDialog.getContentPane().add(mainPanel);
            popupDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            popupDialog.setSize(700,500);
            popupDialog.setLocationRelativeTo(null);
            popupDialog.setVisible(true);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == expandButton) {
            if (!expanded) {
                expandButton.setText("-");
                add(expandPanel, BorderLayout.CENTER);
                expanded = true;
            } else {
                expandButton.setText("+");
                remove(expandPanel);
                expanded = false;
            }
            refreshLabel();
            refreshSize();
        } else if (e.getSource() == snapshotButton) {
            showSnapshot();
        } else if (e.getSource() == subEventsButton) {
            showSubEvents();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            showSnapshot();
        }
        setSelected(!selected);
    }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }

}

